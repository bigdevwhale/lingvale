package app.lingvale.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import app.lingvale.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Talks to the -ling apps through their VillageProvider (ContentResolver.call). Protocol v1 reads the pets and the days;
 * v2 also has an app write its own backup file into a file of ours, and load one back (see [Mover]).
 * A dev build of Lingvale prefers the dev builds of the apps, a demo build the demo ones.
 */
class Bridge(private val ctx: Context) {
    private val suffix = BuildConfig.APPLICATION_ID.removePrefix("app.lingvale")

    private fun candidates(id: String) = listOf("app.$id$suffix", "app.$id", "app.$id.dev", "app.$id.demo").distinct()

    private fun installed(pkg: String) = try { ctx.packageManager.getPackageInfo(pkg, 0); true } catch (_: PackageManager.NameNotFoundException) { false }

    fun find(home: Home): String? = candidates(home.id).firstOrNull(::installed)

    private fun uri(pkg: String) = Uri.parse("content://$pkg.village")

    /** True when [pkg] declares the village provider, i.e. a version that has the bridge, whether or not it's running. */
    private fun bridged(pkg: String) = try { ctx.packageManager.resolveContentProvider("$pkg.village", 0) != null } catch (_: Exception) { false }

    /** Asks [home] for its snapshot; the status says why when there is none. */
    fun ask(home: Home): Pair<Status, Pair<String?, String?>> {
        val pkg = find(home) ?: return Status.MISSING to (null to null)
        // A version with the bridge that still doesn't answer is asleep: some phones (MIUI/HyperOS autostart, battery savers)
        // won't start another app's process from the background, so it only answers while it's already running.
        val silent = { if (bridged(pkg)) Status.ASLEEP else Status.OLD }
        return try {
            val b = ctx.contentResolver.call(uri(pkg), "snapshot", null, null)
            val json = b?.getString("json") ?: return silent() to (pkg to null)
            Status.LIVE to (pkg to json)
        } catch (_: SecurityException) {
            Status.LOCKED to (pkg to null)
        } catch (_: Exception) {
            silent() to (pkg to null)             // «Unknown authority»: a version from before the village, or one that can't be woken
        }
    }

    fun art(pkg: String, name: String): ByteArray? = try {
        ctx.contentResolver.call(uri(pkg), "art", name, null)?.getByteArray("bytes")
    } catch (_: Exception) { null }

    /** The protocol [pkg] speaks: from 2 on it can save and load its backup. 0 when it doesn't answer. */
    fun speaks(pkg: String): Int = try {
        ctx.contentResolver.call(uri(pkg), "hello", null, null)?.getInt("v") ?: 0
    } catch (_: Exception) { 0 }

    /** Has [pkg] write the backup its own settings would save into [to]. The file's name as the app calls it, or null if it couldn't. */
    fun backup(pkg: String, to: File): String? = try {
        ParcelFileDescriptor.open(to, ParcelFileDescriptor.MODE_CREATE or ParcelFileDescriptor.MODE_TRUNCATE or ParcelFileDescriptor.MODE_WRITE_ONLY).use { fd ->
            val b = ctx.contentResolver.call(uri(pkg), "backup", null, Bundle().apply { putParcelable("fd", fd) })
            if (b?.getBoolean("ok") == true) b.getString("name") else null
        }
    } catch (_: Exception) { null }

    /** Has [pkg] load the backup [from], replacing what it holds. Null when it did, else why not: "newer", "busy" or "bad". */
    fun restore(pkg: String, from: File): String? = try {
        ParcelFileDescriptor.open(from, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
            val b = ctx.contentResolver.call(uri(pkg), "restore", null, Bundle().apply { putParcelable("fd", fd) })
            if (b?.getBoolean("ok") == true) null else b?.getString("error") ?: "bad"
        }
    } catch (_: Exception) { "bad" }

    fun launch(pkg: String): Intent? = ctx.packageManager.getLaunchIntentForPackage(pkg)
}

/** Pictures borrowed from the apps, kept on disk per app version so they come back instantly. */
class ArtStore(private val ctx: Context, private val bridge: Bridge, private val scope: CoroutineScope) {
    private val images = mutableStateMapOf<String, ImageBitmap>()
    private val asked = HashSet<String>()
    @Volatile var versions: Map<String, String> = emptyMap()
    /** Where the screen renders take pictures from instead of the apps. */
    @Volatile var source: ((pkg: String, name: String) -> ByteArray?)? = null

    /** The picture [name] of the app [pkg], or null while it's loading (the caller recomposes when it arrives). */
    fun get(pkg: String, name: String): ImageBitmap? {
        val key = "$pkg/$name"
        images[key]?.let { return it }
        source?.let { src ->
            val b = src(pkg, name) ?: return null
            return BitmapFactory.decodeByteArray(b, 0, b.size)?.asImageBitmap()?.also { images[key] = it }
        }
        synchronized(asked) { if (!asked.add(key)) return null }
        scope.launch(Dispatchers.IO) {
            val dir = File(ctx.cacheDir, "art/$pkg-${versions[pkg].orEmpty()}").apply { mkdirs() }
            val f = File(dir, "$name.webp")
            val bytes = if (f.exists()) f.readBytes() else bridge.art(pkg, name)?.also { runCatching { f.writeBytes(it) } }
            val bmp = bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }
            withContext(Dispatchers.Main) { if (bmp != null) images[key] = bmp else synchronized(asked) { asked.remove(key) } }
        }
        return null
    }
}

/** The village: every house of the family, refreshed from the apps whenever Lingvale comes to the front. */
class Village(private val ctx: Context, private val bridge: Bridge, private val art: ArtStore) {
    private val snapDir = File(ctx.filesDir, "snaps").apply { mkdirs() }
    private val _houses = MutableStateFlow(load())
    val houses: StateFlow<List<House>> = _houses
    private val _ready = MutableStateFlow(false)
    /** True once the apps have been asked at least once since the start. */
    val ready: StateFlow<Boolean> = _ready

    private fun cached(h: Home): Snap? = runCatching { Snap.parse(File(snapDir, "${h.id}.json").readText()) }.getOrNull()

    /** The last snapshots, shown at once before the apps answer. */
    private fun load(): List<House> = Family.all.map { h ->
        val pkg = bridge.find(h)
        val snap = if (pkg == null) null else cached(h)
        House(h, if (pkg == null) Status.MISSING else if (snap != null) Status.LIVE else Status.OLD, pkg, snap)
    }.also { l -> art.versions = l.mapNotNull { h -> h.pkg?.let { p -> h.snap?.let { p to it.version } } }.toMap() }

    /** Screen renders put their own houses in. */
    fun set(l: List<House>) { _houses.value = l; _ready.value = true }

    suspend fun refresh() = withContext(Dispatchers.IO) {
        val l = Family.all.map { h ->
            val (status, r) = bridge.ask(h)
            val (pkg, json) = r
            val snap = json?.let { j -> runCatching { Snap.parse(j) }.getOrNull() }
            if (json != null && snap != null) runCatching { File(snapDir, "${h.id}.json").writeText(json) }
            // An app that is asleep keeps its house as it was the last time it answered, instead of turning back into a plot.
            val last = if (status == Status.ASLEEP) cached(h) else null
            when {
                last != null -> House(h, Status.LIVE, pkg, last)
                status == Status.LIVE && snap == null -> House(h, Status.OLD, pkg, null)
                else -> House(h, status, pkg, snap)
            }
        }
        art.versions = l.mapNotNull { h -> h.pkg?.let { p -> h.snap?.let { p to it.version } } }.toMap()
        _houses.value = l
        _ready.value = true
    }
}
