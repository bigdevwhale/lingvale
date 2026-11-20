package app.lingvale.domain

import app.lingvale.data.EventInfo
import app.lingvale.data.House
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** A piece about one house in the day's issue. */
data class Piece(val house: House, val title: String, val text: String, val quote: String)

/** The day's issue of the village gazette, written from what the apps did. */
data class Issue(
    val day: Long, val number: Long, val dateLine: String, val headline: String, val lead: String,
    val pieces: List<Piece>, val chronicle: List<Pair<EventInfo, House>>, val notices: List<String>,
)

object Gazette {
    private fun who(h: House): String {
        val s = h.snap!!; val p = s.team.firstOrNull() ?: return h.home.app
        return p.name.ifBlank { s.species(p.species)?.name ?: h.home.app }
    }

    private fun pick(list: List<String>, seed: Long) = list[((seed % list.size) + list.size).toInt() % list.size]

    private fun quote(h: House, n: Int, seed: Long): String {
        val w = who(h); val c = count(n, h.home.what)
        return pick(if (Lang.ru) listOf(
            "«$c – и это только начало», – говорит $w.",
            "«Мы считали: $c. Завтра посчитаем ещё», – делится $w.",
            "«После такого дня спится особенно сладко», – признаётся $w.",
            "«Заходите, у нас тепло и пахнет делом», – зовёт $w.",
            "«Главное – не останавливаться. Ну, или останавливаться, но ненадолго», – считает $w.",
        ) else listOf(
            "“$c – and that's just the start,” says $w.",
            "“We counted: $c. We'll count again tomorrow,” $w tells us.",
            "“After a day like this you sleep so well,” $w admits.",
            "“Drop by, it's warm and busy in here,” $w calls.",
            "“The trick is not to stop. Or to stop, but not for long,” $w reckons.",
        ), seed + n)
    }

    fun issue(live: List<House>, d: Long, start: Long, levelName: String, shopHint: String?, village: String = "Lingvale"): Issue {
        val busy = Game.busy(live, d)
        val gain = Game.dayGain(live, d)
        val loc = if (Lang.ru) Locale.forLanguageTag("ru") else Locale.ENGLISH
        val date = LocalDate.ofEpochDay(d).format(DateTimeFormatter.ofPattern(if (Lang.ru) "EEEE, d MMMM" else "EEEE, d MMMM", loc))
        val number = (d - start + 1).coerceAtLeast(1)
        val dateLine = tr("№ $number · $date · ${levelName.lowercase()} $village · цена – один светлячок", "No. $number · $date · $village ${levelName.lowercase()} · price: one firefly")
        val dayOf = { h: House -> h.snap!!.days.first { it.d == d } }
        val headline = when (busy.size) {
            0 -> tr("Тихий день в долине", "A quiet day in the valley")
            1 -> tr("${busy[0].home.house}: ${count(dayOf(busy[0]).n, busy[0].home.what)} за день", "${busy[0].home.house}: ${count(dayOf(busy[0]).n, busy[0].home.what)} in a day")
            2 -> tr("${busy[0].home.house} и ${busy[1].home.house.lowercase()} не сидели без дела", "${busy[0].home.house} and ${busy[1].home.house.lowercase()} kept busy")
            else -> tr("Ярмарка в долине: в деле ${count(busy.size, HOUSES)}", "Fair in the valley: ${busy.size} houses at work")
        }
        val minutes = busy.sumOf { dayOf(it).min }
        val lead = when {
            busy.isEmpty() -> tr("Жители отдыхали, светлячки спали в банке. Редакция тоже взяла выходной и советует последовать её примеру.",
                "The residents rested and the fireflies slept in their jar. The editors took the day off too and recommend it.")
            else -> tr("Деревня собрала $gain светлячков" + (if (minutes > 0) ", а жители провели в делах ${count(minutes, MINUTES)}." else ".") +
                (if (busy.size >= 2) " Когда заняты сразу несколько домов, светлячков слетается больше – так в долине было всегда." else ""),
                "The village gathered $gain fireflies" + (if (minutes > 0) " and the residents spent ${count(minutes, MINUTES)} at work." else ".") +
                (if (busy.size >= 2) " When several houses are busy at once, more fireflies come – it's always been so in the valley." else ""))
        }
        val pieces = busy.map { h ->
            val di = dayOf(h)
            val text = count(di.n, h.home.what) + (if (h.home.minutes && di.min > 0) tr(", всего ", ", ") + count(di.min, MINUTES) + tr("", " in all") else "") + "."
            Piece(h, h.home.house, text, quote(h, di.n, d))
        }
        val chronicle = live.flatMap { h -> h.snap!!.events.filter { dayOf(it.at) == d }.map { it to h } }.sortedBy { it.first.at }
        val notices = buildList {
            live.filter { it !in busy }.forEach { add(tr("В доме «${it.home.house}» сегодня выходной. Жители просили не беспокоить.", "The ${it.home.house} has a day off. The residents ask not to be disturbed.")) }
            shopHint?.let { add(it) }
            if (busy.size == 2 && live.size >= 3) add(tr("Ещё один занятый дом – и в долине будет ярмарка.", "One more busy house and there'll be a fair in the valley."))
        }
        return Issue(d, number, dateLine, headline, lead, pieces, chronicle, notices)
    }
}
