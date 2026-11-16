package app.lingvale.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

/** The language the app speaks right now (set from the activity's locale). */
object Lang { @Volatile var ru = true }

fun tr(ru: String, en: String) = if (Lang.ru) ru else en

fun plural(n: Long, one: String, few: String, many: String): String {
    val a = abs(n) % 100; val b = a % 10
    return if (a in 11..14) many else when (b) { 1L -> one; 2L, 3L, 4L -> few; else -> many }
}

/** «3 практики» / «3 practices». */
fun count(n: Int, w: Words) =
    if (Lang.ru) "${fmt(n)} ${plural(n.toLong(), w.ru1, w.ru2, w.ru5)}" else "${fmt(n)} ${if (n == 1) w.en1 else w.enN}"

/** A countable word in both languages. */
data class Words(val ru1: String, val ru2: String, val ru5: String, val en1: String, val enN: String)

val MINUTES = Words("минута", "минуты", "минут", "minute", "minutes")
val PETS = Words("житель", "жителя", "жителей", "resident", "residents")
val HOUSES = Words("дом", "дома", "домов", "house", "houses")
val FIREFLIES = Words("светлячок", "светлячка", "светлячков", "firefly", "fireflies")
val DAYS = Words("день", "дня", "дней", "day", "days")

fun fmt(n: Int): String = "%,d".format(Locale.US, n).replace(",", if (Lang.ru) " " else ",")

fun today(): Long = LocalDate.now().toEpochDay()
fun dayOf(ms: Long): Long = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()
fun hourNow(): Int = java.time.LocalTime.now().hour

private fun loc() = if (Lang.ru) Locale.forLanguageTag("ru") else Locale.ENGLISH

/** «16 ноября» / «16 November». */
fun dayTitle(d: Long): String = LocalDate.ofEpochDay(d).format(DateTimeFormatter.ofPattern("d MMMM", loc()))

/** «пн» / «Mon». */
fun weekdayShort(d: Long): String = LocalDate.ofEpochDay(d).format(DateTimeFormatter.ofPattern("EE", loc())).replace(".", "")

/** «Сегодня», «Вчера» or the date. */
fun dayName(d: Long): String = when (today() - d) {
    0L -> tr("Сегодня", "Today"); 1L -> tr("Вчера", "Yesterday"); else -> dayTitle(d)
}

fun clock(ms: Long): String = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalTime().let { "%d:%02d".format(it.hour, it.minute) }

/** Monday of the week [d] falls in. */
fun mondayOf(d: Long): Long = d - LocalDate.ofEpochDay(d).dayOfWeek.value + 1
