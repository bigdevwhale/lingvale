package app.lingvale.data

import android.content.Context
import android.net.Uri
import app.lingvale.BuildConfig
import app.lingvale.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.OutputStream
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/** One app's backup inside a moving file: [file] is its entry, [version] the app version that wrote it. */
data class Part(val id: String, val file: String, val version: String)

/** What a moving file holds: when it was made, the village's own things (null when none) and the apps' backups. */
data class Manifest(val at: Long, val village: JSONObject?, val parts: List<Part>)

/** Thrown for a file that isn't a moving file, or ([newer]) one from a Lingvale newer than this. */
class NotMoving(val newer: Boolean = false) : Exception()

/**
 * The moving file: a zip with `lingvale.json` (the manifest and the village) and, under `apps/`, each app's own backup
 * exactly as its settings save it, so any one of them can also be taken out and loaded in its app by hand.
 */
object Moving {
    const val FORMAT = 1
    const val MANIFEST = "lingvale.json"
    const val VILLAGE = "village"
    fun name() = "lingvale-${LocalDate.now()}.zip"

    fun encode(m: Manifest, version: String): String = JSONObject()
        .put("app", "lingvale").put("format", FORMAT).put("version", version).put("at", m.at)
        .put("village", m.village ?: JSONObject.NULL)
        .put("apps", JSONArray().apply { m.parts.forEach { put(JSONObject().put("id", it.id).put("file", it.file).put("version", it.version)) } })
        .toString()

    fun decode(text: String): Manifest {
        val o = runCatching { JSONObject(text) }.getOrNull() ?: throw NotMoving()
        if (o.optString("app") != "lingvale") throw NotMoving()
        if (o.optInt("format") > FORMAT) throw NotMoving(newer = true)
        val apps = o.optJSONArray("apps") ?: JSONArray()
        return Manifest(o.optLong("at"), o.optJSONObject("village"),
            (0 until apps.length()).map { apps.getJSONObject(it) }.map { Part(it.getString("id"), it.getString("file"), it.optString("version")) })
    }

    /** Writes the zip: the manifest first, then each app's file under the name the manifest gives it. */
    fun pack(out: OutputStream, m: Manifest, version: String, files: Map<String, File>) {
        ZipOutputStream(out).use { z ->
            z.putNextEntry(ZipEntry(MANIFEST)); z.write(encode(m, version).toByteArray()); z.closeEntry()
            m.parts.forEach { p ->
                z.putNextEntry(ZipEntry(p.file)); files.getValue(p.id).inputStream().use { it.copyTo(z) }; z.closeEntry()
            }
        }
    }

    fun open(zip: File): Manifest = try {
        ZipFile(zip).use { z ->
            val e = z.getEntry(MANIFEST) ?: throw NotMoving()
            decode(z.getInputStream(e).use { it.readBytes().toString(Charsets.UTF_8) })
        }
    } catch (e: NotMoving) { throw e } catch (_: Exception) { throw NotMoving() }

    /** Takes one entry out into [to]; false when the zip doesn't have it. */
    fun extract(zip: File, entry: String, to: File): Boolean = ZipFile(zip).use { z ->
        val e = z.getEntry(entry) ?: return false
        z.getInputStream(e).use { i -> to.outputStream().use { i.copyTo(it) } }
        true
    }
}

/** How one line of a move stands. [OLD]: the app is installed but too old to save or load; [MISSING]: it isn't installed. */
enum class Mark { READY, WORKING, DONE, FAILED, OLD, MISSING, NEWER, BUSY }

/** One line of a move: an app's house ([id] of [Family]) or the village itself ([Moving.VILLAGE]); [on] when the player wants it loaded. */
data class Line(val id: String, val mark: Mark, val on: Boolean = true)

enum class Stage { IDLE, SAVING, SAVED, PICKED, LOADING, LOADED, ERROR }

/** Where a move stands. [at]: when the picked file was made. [error]: "notours", "newer" or "write" for [Stage.ERROR]. */
data class Move(val stage: Stage = Stage.IDLE, val lines: List<Line> = emptyList(), val at: Long = 0, val error: String = "")

/**
 * Moves the whole family to another phone: asks every installed app for its backup and packs them into one file with
 * the village's own things, and on the other phone hands each app its backup back. The work runs in the app's scope,
 * so it finishes even if the settings are closed meanwhile.
 */
class Mover(private val ctx: Context, private val bridge: Bridge, private val prefs: Prefs, private val village: Village, private val scope: CoroutineScope) {
    private val _state = MutableStateFlow(Move())
    val state: StateFlow<Move> = _state
    private val dir get() = File(ctx.cacheDir, "moving").apply { mkdirs() }
    private val picked get() = File(dir, "picked.zip")
    private var manifest: Manifest? = null

    private fun mark(id: String, m: Mark) = _state.update { s -> s.copy(lines = s.lines.map { if (it.id == id) it.copy(mark = m) else it }) }
    private val busy get() = _state.value.stage == Stage.SAVING || _state.value.stage == Stage.LOADING

    fun reset() { if (!busy) { manifest = null; _state.value = Move(); dir.deleteRecursively() } }

    /** Gathers every installed app's backup and writes the moving file to [uri]. */
    fun save(uri: Uri) {
        if (busy) return
        val homes = Family.all.mapNotNull { h -> bridge.find(h)?.let { h to it } }
        _state.value = Move(Stage.SAVING, homes.map { Line(it.first.id, Mark.WORKING) } + Line(Moving.VILLAGE, Mark.WORKING))
        scope.launch(Dispatchers.IO) {
            val files = LinkedHashMap<String, File>(); val parts = ArrayList<Part>()
            homes.forEach { (h, pkg) ->
                if (bridge.speaks(pkg) < 2) { mark(h.id, Mark.OLD); return@forEach }
                val f = File(dir, "${h.id}.part")
                val name = bridge.backup(pkg, f)
                if (name == null || f.length() == 0L) { mark(h.id, Mark.FAILED); return@forEach }
                files[h.id] = f
                parts += Part(h.id, "apps/${h.id}.${name.substringAfterLast('.', "json")}", version(pkg))
                mark(h.id, Mark.DONE)
            }
            val ok = runCatching {
                ctx.contentResolver.openOutputStream(uri, "wt")!!.use {
                    Moving.pack(it, Manifest(System.currentTimeMillis(), prefs.export(), parts), BuildConfig.VERSION_NAME, files)
                }
            }.isSuccess
            dir.deleteRecursively()
            if (ok) { mark(Moving.VILLAGE, Mark.DONE); _state.update { it.copy(stage = Stage.SAVED) } }
            else _state.value = Move(Stage.ERROR, error = "write")
        }
    }

    /** Reads the moving file at [uri] and shows what it can load here; nothing changes until [load]. */
    fun pick(uri: Uri) {
        if (busy) return
        scope.launch(Dispatchers.IO) {
            val m = try {
                ctx.contentResolver.openInputStream(uri)!!.use { i -> picked.outputStream().use { i.copyTo(it) } }
                Moving.open(picked)
            } catch (e: Exception) {
                _state.value = Move(Stage.ERROR, error = if ((e as? NotMoving)?.newer == true) "newer" else "notours"); return@launch
            }
            manifest = m
            val lines = m.parts.filter { it.id in Family.byId }.map { p ->
                val pkg = bridge.find(Family.byId.getValue(p.id))
                val mark = if (pkg == null) Mark.MISSING else if (bridge.speaks(pkg) < 2) Mark.OLD else Mark.READY
                Line(p.id, mark, on = mark == Mark.READY)
            } + listOfNotNull(m.village?.let { Line(Moving.VILLAGE, Mark.READY) })
            _state.value = Move(Stage.PICKED, lines, m.at)
        }
    }

    /** Ticks or unticks a line of the picked file. */
    fun toggle(id: String) = _state.update { s ->
        if (s.stage != Stage.PICKED) s else s.copy(lines = s.lines.map { if (it.id == id && it.mark == Mark.READY) it.copy(on = !it.on) else it })
    }

    /** Loads the ticked lines of the picked file: each app replaces what it holds with its backup. */
    fun load() {
        val m = manifest ?: return
        if (_state.value.stage != Stage.PICKED) return
        val todo = _state.value.lines.filter { it.on && it.mark == Mark.READY }.map { it.id }
        _state.update { s -> s.copy(stage = Stage.LOADING, lines = s.lines.map { if (it.id in todo) it.copy(mark = Mark.WORKING) else it.copy(on = false) }) }
        scope.launch(Dispatchers.IO) {
            m.parts.filter { it.id in todo }.forEach { p ->
                val pkg = bridge.find(Family.byId.getValue(p.id))
                val f = File(dir, "${p.id}.part")
                val error = if (pkg == null || !runCatching { Moving.extract(picked, p.file, f) }.getOrDefault(false)) "bad" else bridge.restore(pkg, f)
                f.delete()
                mark(p.id, when (error) { null -> Mark.DONE; "newer" -> Mark.NEWER; "busy" -> Mark.BUSY; else -> Mark.FAILED })
            }
            if (Moving.VILLAGE in todo) mark(Moving.VILLAGE, if (runCatching { prefs.restore(m.village!!) }.isSuccess) Mark.DONE else Mark.FAILED)
            runCatching { village.refresh() }
            // the picked file stays until the page is closed, so what failed can be seen; it holds nothing the apps don't
            _state.update { it.copy(stage = Stage.LOADED) }
        }
    }

    private fun version(pkg: String) = runCatching { ctx.packageManager.getPackageInfo(pkg, 0).versionName }.getOrNull().orEmpty()
}
