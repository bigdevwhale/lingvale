package app.lingvale.domain

import app.lingvale.data.House
import app.lingvale.data.PetInfo
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.min
import kotlin.math.roundToInt

/** Things done and minutes over a stretch of days. */
data class Tally(val days: Int, val n: Int, val min: Int)

/** What one house has done, worked out from its days. */
class HouseFacts(h: House) {
    private val s = h.snap!!
    private val by = s.days.associateBy { it.d }
    private val t = today()

    fun tally(from: Long, to: Long): Tally {
        val l = s.days.filter { it.d in from..to }
        return Tally(l.size, l.sumOf { it.n }, l.sumOf { it.min })
    }

    val mon = mondayOf(t)
    val week = tally(mon, t)
    /** Last week up to the same weekday, so the two are fair to compare. */
    val lastWeekSoFar = tally(mon - 7, t - 7)
    val lastWeek = tally(mon - 7, mon - 1)
    val month = tally(t - 29, t)
    val prevMonth = tally(t - 59, t - 30)
    val first = s.days.minOfOrNull { it.d }
    val all = tally(first ?: t, t)

    val streak: Int = run { var n = 0; var d = t; if (by[d] == null) d--; while (by[d] != null) { n++; d-- }; n }
    val bestStreak: Int = run {
        var best = 0; var cur = 0; var prev = Long.MIN_VALUE
        for (d in s.days.map { it.d }.sorted()) { cur = if (d == prev + 1) cur + 1 else 1; best = maxOf(best, cur); prev = d }
        best
    }

    /** The weekday most often busy over the last half year, or null with too little to go on. */
    val favDay: DayOfWeek? = run {
        val l = s.days.filter { it.d > t - 182 }
        if (l.size < 8) null else l.groupingBy { LocalDate.ofEpochDay(it.d).dayOfWeek }.eachCount().maxByOrNull { it.value }?.key
    }

    /** Things done per busy day, on average over the last three months. */
    val perDay: Double = run { val l = s.days.filter { it.d > t - 90 }; if (l.isEmpty()) 0.0 else l.sumOf { it.n }.toDouble() / l.size }
    val minPerDay: Int = run { val l = s.days.filter { it.d > t - 90 && it.min > 0 }; if (l.isEmpty()) 0 else l.sumOf { it.min } / l.size }

    /** The last twelve weeks, oldest first: things done and minutes per week. */
    val weeks: List<Tally> = (11 downTo 0).map { k -> tally(mon - k * 7L, mon - k * 7L + 6) }

    /** Fireflies this house has brought the village in all. */
    val fireflies = s.days.sumOf { min(it.n, 4) * 2 }

    /** When things are usually done, from the last week: share before noon, in the afternoon, in the evening. */
    val partOfDay: Triple<Int, Int, Int>? = run {
        val zone = ZoneId.systemDefault()
        val hs = s.events.map { Instant.ofEpochMilli(it.at).atZone(zone).hour }
        if (hs.size < 3) null else Triple(hs.count { it < 12 } * 100 / hs.size, hs.count { it in 12..17 } * 100 / hs.size, hs.count { it >= 18 } * 100 / hs.size)
    }

    /** The resident closest to growing up, and how far along it is. */
    val nextToGrow: PetInfo? = s.pets.filter { !it.egg && it.stage < 4 }.maxByOrNull { it.progress }
}

fun dayName(d: DayOfWeek, accusative: Boolean = false): String {
    if (!Lang.ru) return d.getDisplayName(TextStyle.FULL, Locale.ENGLISH) + "s"
    return when (d) {
        DayOfWeek.MONDAY -> "понедельник"; DayOfWeek.TUESDAY -> "вторник"; DayOfWeek.WEDNESDAY -> if (accusative) "среду" else "среда"
        DayOfWeek.THURSDAY -> "четверг"; DayOfWeek.FRIDAY -> if (accusative) "пятницу" else "пятница"
        DayOfWeek.SATURDAY -> if (accusative) "субботу" else "суббота"; DayOfWeek.SUNDAY -> if (accusative) "воскресенье" else "воскресенье"
    }
}

/** «+40%», «−15%», «столько же». */
fun change(now: Int, before: Int): String = when {
    before == 0 && now == 0 -> tr("как и было", "same as before")
    before == 0 -> tr("впервые", "new")
    else -> { val p = ((now - before) * 100.0 / before).roundToInt(); if (p == 0) tr("столько же", "the same") else if (p > 0) "+$p%" else "−${-p}%" }
}

/** How a figure moved, with what it is compared to: «+40% к прошлой»; «раньше не было» when there was nothing before. */
fun vs(now: Int, before: Int, against: String): String = when {
    before == 0 -> if (now == 0) tr("без изменений", "no change") else tr("раньше не было", "none before")
    now == before -> tr("без изменений", "no change")
    else -> change(now, before) + against
}

/** A pattern across houses, said in a sentence. */
data class Link(val a: House, val b: House, val text: String, val strength: Double)

object Insights {
    /**
     * How the houses go together over the last three months: on days house A was busy, how much more (or less)
     * often house B was busy than usual. Only clear patterns with enough days behind them.
     */
    fun links(live: List<House>): List<Link> {
        val t = today(); val from = t - 89
        val sets = live.associateWith { h -> h.snap!!.days.filter { it.d >= from }.map { it.d }.toSet() }
        val out = mutableListOf<Link>()
        for (a in live) for (b in live) {
            if (a == b) continue
            val da = sets[a]!!; val db = sets[b]!!
            if (da.size < 6 || db.size < 6) continue
            val base = db.size / 90.0
            val withA = da.count { it in db } / da.size.toDouble()
            val lift = withA / base
            val pct = (withA * 100).roundToInt()
            if (lift >= 1.3) out += Link(a, b, tr("В дни, когда занят дом «${a.home.house}», «${b.home.house}» тоже в деле в $pct% случаев – в ${fmt1(lift)} раза чаще обычного.",
                "On days the ${a.home.house} is busy, the ${b.home.house} is busy too $pct% of the time – ${fmt1(lift)}× more often than usual."), lift)
            else if (lift <= .7) out += Link(a, b, tr("Когда занят дом «${a.home.house}», до дома «${b.home.house}» руки доходят реже: $pct% таких дней против обычных ${(base * 100).roundToInt()}%.",
                "When the ${a.home.house} is busy, the ${b.home.house} gets less attention: $pct% of those days against the usual ${(base * 100).roundToInt()}%."), 1 / lift)
        }
        return out.sortedByDescending { it.strength }.distinctBy { it.text.take(40) }.take(4)
    }

    private fun fmt1(v: Double) = if (Lang.ru) "%.1f".format(Locale.US, v).replace('.', ',') else "%.1f".format(Locale.US, v)

    /** Days in the last 30 with something done, with two or more houses, and the busiest weekday across the village. */
    fun village(live: List<House>): List<String> {
        val t = today()
        val days = (t - 29..t).map { d -> Game.busy(live, d).size }
        val prev = (t - 59..t - 30).map { d -> Game.busy(live, d).size }
        val out = mutableListOf<String>()
        out += tr("За 30 дней дела были в ${count(days.count { it > 0 }, DAYS)} (до этого – ${prev.count { it > 0 }}).",
            "In the last 30 days something was done on ${count(days.count { it > 0 }, DAYS)} (the 30 before: ${prev.count { it > 0 }}).")
        if (live.size >= 2) out += tr("Два дома и больше были заняты в ${count(days.count { it >= 2 }, DAYS)}.", "Two or more houses were busy on ${count(days.count { it >= 2 }, DAYS)}.")
        val wd = (t - 181..t).filter { Game.busy(live, it).isNotEmpty() }.groupingBy { LocalDate.ofEpochDay(it).dayOfWeek }.eachCount()
        wd.maxByOrNull { it.value }?.let { best -> wd.minByOrNull { it.value }?.let { worst ->
            out += tr("Самый деятельный день – ${dayName(best.key)}, самый тихий – ${dayName(worst.key)}.", "The busiest weekday is ${dayName(best.key)}, the quietest ${dayName(worst.key)}.")
        } }
        val zone = ZoneId.systemDefault()
        val hours = live.flatMap { h -> h.snap!!.events.map { Instant.ofEpochMilli(it.at).atZone(zone).hour } }
        if (hours.size >= 5) {
            val m = hours.count { it < 12 } * 100 / hours.size; val e = hours.count { it >= 18 } * 100 / hours.size
            out += tr("За неделю $m% дел пришлось на утро, $e% – на вечер.", "This week $m% of things happened in the morning and $e% in the evening.")
        }
        return out
    }
}
