package app.lingvale.data

import app.lingvale.domain.tr
import org.json.JSONArray
import org.json.JSONObject

/**
 * A pet as its app describes it. [stage] 0 egg, 1 baby, 2 youngster, 3 grown, 4 master; [slot] 0 leader, 1–2 team, -1 resting.
 * [frame] is the picture pet_<species>_<frame> when the app names it (Roamling: a grown companion has one of several forms),
 * -1 to take it from the stage.
 */
data class PetInfo(
    val id: Long, val species: String, val name: String, val stage: Int, val progress: Double,
    val slot: Int, val shiny: Boolean, val found: Long, val hatched: Long, val frame: Int = -1,
) {
    val egg get() = stage == 0
}

data class SpeciesInfo(val id: String, val ru: String, val en: String, val color: Long, val aboutRu: String, val aboutEn: String, val rare: Boolean) {
    val name get() = tr(ru, en)
    val about get() = tr(aboutRu, aboutEn)
}

/** A day with something done: [n] things, [min] minutes (0 where minutes mean nothing, as with money). */
data class DayInfo(val d: Long, val n: Int, val min: Int)

/** One thing done in the last week. */
data class EventInfo(val at: Long, val min: Int, val ru: String, val en: String) {
    val title get() = tr(ru, en)
}

/** What an app told the village, protocol v1 (see VillageProvider in each app). */
data class Snap(
    val app: String, val version: String, val ru: Boolean, val scene: String,
    val pets: List<PetInfo>, val species: List<SpeciesInfo>, val days: List<DayInfo>, val events: List<EventInfo>,
) {
    val team get() = pets.filter { it.slot >= 0 && !it.egg }.sortedBy { it.slot }
    val hatched get() = pets.filter { !it.egg }
    /** Who walks in the village: the team, or the most grown residents when the team is all eggs. */
    val walkers get() = team.ifEmpty { hatched.sortedByDescending { it.stage }.take(2) }
    fun species(id: String) = species.firstOrNull { it.id == id }

    companion object {
        private inline fun <T> JSONArray?.list(f: (JSONObject) -> T): List<T> =
            if (this == null) emptyList() else (0 until length()).map { f(getJSONObject(it)) }

        private fun color(s: String) = runCatching { 0xFF000000 or s.removePrefix("#").toLong(16) }.getOrDefault(0xFF888888)

        fun parse(json: String): Snap {
            val o = JSONObject(json)
            return Snap(
                app = o.getString("app"), version = o.optString("version"), ru = o.optBoolean("ru", true), scene = o.optString("scene"),
                pets = o.optJSONArray("pets").list {
                    PetInfo(it.getLong("id"), it.getString("species"), it.optString("name"), it.optInt("stage"), it.optDouble("progress", 0.0),
                        it.optInt("slot", -1), it.optBoolean("shiny"), it.optLong("found"), it.optLong("hatched"), it.optInt("frame", -1))
                },
                species = o.optJSONArray("species").list {
                    SpeciesInfo(it.getString("id"), it.optString("ru"), it.optString("en"), color(it.optString("color")),
                        it.optString("aboutRu"), it.optString("aboutEn"), it.optBoolean("rare"))
                },
                days = o.optJSONArray("days").list { DayInfo(it.getLong("d"), it.optInt("n"), it.optInt("min")) },
                events = o.optJSONArray("events").list { EventInfo(it.getLong("at"), it.optInt("min"), it.optString("ru"), it.optString("en")) },
            )
        }
    }
}

/** Where a house stands: [LIVE] talks to us, [OLD] is installed but has no bridge yet, [LOCKED] refused (signed differently), [MISSING] isn't installed. */
enum class Status { LIVE, OLD, LOCKED, MISSING }

data class House(val home: Home, val status: Status, val pkg: String?, val snap: Snap?) {
    val live get() = snap != null && status == Status.LIVE
}
