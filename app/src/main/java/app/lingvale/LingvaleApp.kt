package app.lingvale

import android.app.Application
import android.content.Context
import app.lingvale.data.ArtStore
import app.lingvale.data.Bridge
import app.lingvale.data.Village
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import app.lingvale.domain.today
import org.json.JSONArray
import org.json.JSONObject

/** The few settings: the theme ("", "light", "dark") and whether the welcome was read. */
/** How long the village's name may be. */
const val NAME_MAX = 24

/** What the village is called: the player's name for it or «Lingvale». */
fun villageName(n: String) = n.ifBlank { "Lingvale" }

class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("lingvale", Context.MODE_PRIVATE)
    private val _theme = MutableStateFlow(sp.getString("theme", "").orEmpty())
    val theme: StateFlow<String> = _theme
    fun setTheme(t: String) { sp.edit().putString("theme", t).apply(); _theme.value = t }
    private val _name = MutableStateFlow(sp.getString("name", "").orEmpty())
    /** The village's own name, given by the player (#7); blank keeps «Lingvale». */
    val name: StateFlow<String> = _name
    fun setName(n: String) { val t = n.trim().take(NAME_MAX); sp.edit().putString("name", t).apply(); _name.value = t }
    private val _built = MutableStateFlow(sp.getString("built", "").orEmpty().split(',').filter { it.isNotBlank() })
    /** The houses the player chose to build, in the order they went up; only these stand in the village. */
    val built: StateFlow<List<String>> = _built
    fun build(id: String) { if (id !in _built.value) setBuilt(_built.value + id) }
    fun demolish(id: String) = setBuilt(_built.value - id)
    private fun setBuilt(l: List<String>) { sp.edit().putString("built", l.joinToString(",")).apply(); _built.value = l }

    var askedToBuild: Boolean
        get() = sp.getBoolean("askedToBuild", false)
        set(v) = sp.edit().putBoolean("askedToBuild", v).apply()

    var welcomed: Boolean
        get() = sp.getBoolean("welcomed", false)
        set(v) = sp.edit().putBoolean("welcomed", v).apply()

    private fun readStyle() = Styling(
        bg = sp.getString("bg", "summer").orEmpty(), path = sp.getString("path", "sand").orEmpty(), weather = sp.getString("weather", "").orEmpty(),
        skins = sp.getString("skins", "").orEmpty().split(',').mapNotNull { e -> e.split(':').takeIf { it.size == 2 }?.let { it[0] to (it[1].toIntOrNull() ?: 0) } }.toMap(),
    )
    private val _style = MutableStateFlow(readStyle())
    /** How the village looks: the chosen season, path, weather and house designs. */
    val style: StateFlow<Styling> = _style
    fun setStyle(f: (Styling) -> Styling) {
        val s = f(_style.value)
        sp.edit().putString("bg", s.bg).putString("path", s.path).putString("weather", s.weather)
            .putString("skins", s.skins.filterValues { it > 0 }.entries.joinToString(",") { "${it.key}:${it.value}" }).apply()
        _style.value = s
    }

    private fun read() = Progress(
        start = sp.getLong("start", -1L).takeIf { it >= 0 } ?: (today() - 6).also { sp.edit().putLong("start", it).apply() },
        spent = sp.getInt("spent", 0), bonus = sp.getInt("bonus", 0),
        owned = sp.getStringSet("owned", emptySet())!!.toSet(), claimed = sp.getStringSet("claimed", emptySet())!!.toSet(),
        letter = sp.getLong("letter", 0L), got = sp.getStringSet("got", emptySet())!!.toSet(),
    )
    private val _progress = MutableStateFlow(read())
    /** The village game: what was spent, bought and claimed. Fireflies gathered are worked out from the apps' days. */
    val progress: StateFlow<Progress> = _progress
    fun update(f: (Progress) -> Progress) {
        val p = f(_progress.value)
        sp.edit().putInt("spent", p.spent).putInt("bonus", p.bonus).putStringSet("owned", p.owned).putStringSet("claimed", p.claimed).putLong("letter", p.letter).putStringSet("got", p.got).apply()
        _progress.value = p
    }

    /** The village's own things for a moving file: what is built, how it looks, what was bought and claimed. */
    fun export(): JSONObject = JSONObject()
        .put("theme", _theme.value).put("name", _name.value).put("built", JSONArray(_built.value))
        .put("bg", _style.value.bg).put("path", _style.value.path).put("weather", _style.value.weather)
        .put("skins", JSONObject(_style.value.skins.filterValues { it > 0 }))
        .put("start", _progress.value.start).put("spent", _progress.value.spent).put("bonus", _progress.value.bonus).put("letter", _progress.value.letter)
        .put("owned", JSONArray(_progress.value.owned)).put("claimed", JSONArray(_progress.value.claimed)).put("got", JSONArray(_progress.value.got))

    /** Puts a moving file's village in place of this one. Throws on a file that isn't right, before anything changes. */
    fun restore(o: JSONObject) {
        fun strings(k: String) = o.optJSONArray(k)?.let { a -> (0 until a.length()).map { a.getString(it) } }.orEmpty()
        val skins = o.optJSONObject("skins")?.let { s -> s.keys().asSequence().associateWith { s.getInt(it) } }.orEmpty()
        val style = Styling(o.optString("bg", "summer"), o.optString("path", "sand"), o.optString("weather"), skins)
        val p = Progress(o.getLong("start"), o.optInt("spent"), o.optInt("bonus"), strings("owned").toSet(), strings("claimed").toSet(), o.optLong("letter"), strings("got").toSet())
        sp.edit().putLong("start", p.start).putBoolean("welcomed", true).putBoolean("askedToBuild", true).apply()
        setTheme(o.optString("theme")); setName(o.optString("name")); setBuilt(strings("built")); setStyle { style }; update { p }
    }
}

/**
 * [start]: the first day the village counts (a week before it was founded, as a welcome). [bonus]: fireflies from quests and achievements.
 * [claimed]: "week:quest" for quests, "ach:id" for achievements. [got]: achievements ever reached, so they stay once the days behind them are gone.
 */
data class Progress(val start: Long, val spent: Int, val bonus: Int, val owned: Set<String>, val claimed: Set<String>, val letter: Long, val got: Set<String> = emptySet())

/** The look of the village: [bg] a season id (or "calendar"), [path] a path id, [weather] a weather id ("" for clear), [skins] house id to design. */
data class Styling(val bg: String = "summer", val path: String = "sand", val weather: String = "", val skins: Map<String, Int> = emptyMap())

class AppContainer(context: Context) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val prefs = Prefs(context)
    val bridge = Bridge(context)
    val art = ArtStore(context, bridge, scope)
    val village = Village(context, bridge, art)
    val mover = app.lingvale.data.Mover(context, bridge, prefs, village, scope)
}

class LingvaleApp : Application() {
    lateinit var container: AppContainer
    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

val Context.container: AppContainer get() = (applicationContext as LingvaleApp).container
