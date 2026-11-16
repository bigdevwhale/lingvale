package app.lingvale.domain

import app.lingvale.data.House
import java.time.Instant
import java.time.ZoneId
import kotlin.math.min
import kotlin.random.Random

/** A village level: the village grows with the fireflies it has ever gathered. */
data class Level(val n: Int, val ru: String, val en: String, val from: Int) {
    val name get() = tr(ru, en)
}

/** A decoration for the village. [unlocks] names a feature it brings: "board" news, "postbox" weekly letters, "campfire" evenings together. */
data class Decor(val id: String, val ru: String, val en: String, val aboutRu: String, val aboutEn: String, val cost: Int, val level: Int, val unlocks: String = "") {
    val name get() = tr(ru, en)
    val about get() = tr(aboutRu, aboutEn)
}

/** A weekly village task, made of what the apps did. */
data class Quest(val id: String, val ru: String, val en: String, val goal: Int, val reward: Int, val have: Int) {
    val title get() = tr(ru, en)
    val done get() = have >= goal
}

/** Something a resident says, above its house [house]. */
data class Chat(val house: String, val text: String)

object Game {
    val levels = listOf(
        Level(0, "Хутор", "Hamlet", 0), Level(1, "Деревня", "Village", 120), Level(2, "Село", "Large village", 350),
        Level(3, "Посёлок", "Township", 800), Level(4, "Городок", "Small town", 1600),
    )

    val decor = listOf(
        Decor("bench", "Скамейка", "Bench", "Посидеть у дорожки", "A seat by the path", 20, 0),
        Decor("flowers", "Клумба", "Flower bed", "Тюльпаны у дороги", "Tulips by the road", 25, 0),
        Decor("well", "Колодец", "Well", "Сердце любой деревни", "The heart of any village", 30, 0),
        Decor("board", "Доска объявлений", "Notice board", "Жители пишут на ней новости деревни", "The residents pin the village news on it", 45, 0, "board"),
        Decor("lamp", "Фонарь", "Street lamp", "Светит ночью", "Lights up at night", 40, 1),
        Decor("signpost", "Указатель", "Signpost", "Чтобы гости не заблудились", "So guests don't get lost", 30, 1),
        Decor("postbox", "Почтовый ящик", "Post box", "По понедельникам в нём письмо об ушедшей неделе", "Every Monday a letter about the week gone by", 60, 1, "postbox"),
        Decor("appletree", "Яблоня", "Apple tree", "Тень и яблоки", "Shade and apples", 60, 1),
        Decor("campfire", "Костёр", "Campfire", "Вечером жители собираются у огня", "In the evening the residents gather by the fire", 80, 2, "campfire"),
        Decor("beehives", "Ульи", "Beehives", "Деревенский мёд", "Village honey", 80, 2),
        Decor("haycart", "Телега с сеном", "Hay cart", "Урожай собран", "The harvest is in", 70, 2),
        Decor("fountain", "Фонтан", "Fountain", "Для площади", "For the square", 130, 3),
        Decor("gazebo", "Беседка", "Gazebo", "Для долгих разговоров", "For long talks", 150, 3),
        Decor("tent", "Ярмарочный шатёр", "Fair tent", "Ярмарки становятся пышнее", "Fairs get grander", 170, 3),
        Decor("windmill", "Мельница", "Windmill", "Машет крыльями над долиной", "Waves its sails over the valley", 240, 4),
        Decor("balloon", "Воздушный шар", "Hot air balloon", "Вершина деревенской жизни", "The height of village life", 320, 4),
    )

    fun level(earned: Int) = levels.last { earned >= it.from }
    fun next(earned: Int) = levels.firstOrNull { it.from > earned }

    /** The houses busy on day [d]. */
    fun busy(live: List<House>, d: Long) = live.filter { h -> h.snap!!.days.any { it.d == d } }

    /**
     * Fireflies a day brings: two for every thing done in a house (up to four things a house),
     * and more when several houses were busy that day – the village likes it when life is everywhere.
     */
    fun dayGain(live: List<House>, d: Long): Int {
        val act = live.mapNotNull { h -> h.snap!!.days.firstOrNull { it.d == d } }
        val base = act.sumOf { min(it.n, 4) * 2 }
        val bonus = when (act.size) { 0, 1 -> 0; 2 -> 6; else -> 6 + 8 * (act.size - 2) }
        return base + bonus
    }

    fun gathered(live: List<House>, from: Long, to: Long = today()) = (from..to).sumOf { dayGain(live, it) }

    /** A fair on days when three houses are busy (two, while only two have moved in). */
    fun fair(live: List<House>, d: Long = today()) = live.size >= 2 && busy(live, d).size >= min(3, live.size)

    fun quests(live: List<House>): List<Quest> {
        val mon = mondayOf(today())
        val week = (mon..today()).toList()
        val busy = week.map { busy(live, it) }
        val minutes = live.sumOf { h -> h.snap!!.days.filter { it.d >= mon }.sumOf { it.min } }
        val zone = ZoneId.systemDefault()
        val mornings = live.sumOf { h -> h.snap!!.events.count { e -> e.at.let { val t = Instant.ofEpochMilli(it).atZone(zone); t.toLocalDate().toEpochDay() >= mon && t.hour < 9 } } }
        val all = buildList {
            if (live.size >= 2) add(Quest("pair", "Два дома в один день – три раза", "Two houses busy on the same day, three times", 3, 30, busy.count { it.size >= 2 }))
            if (live.size >= 3) add(Quest("trio", "Три дома в один день", "Three houses busy on one day", 1, 50, busy.count { it.size >= 3 }))
            if (live.size >= 2) add(Quest("each", "Свет в каждом доме", "A light in every house", live.size, 40, live.count { h -> h.snap!!.days.any { it.d >= mon } }))
            add(Quest("five", "Пять дней с делами", "Five days with something done", 5, 25, busy.count { it.isNotEmpty() }))
            if (live.any { it.home.minutes }) add(Quest("hour", "Полтора часа на всё", "An hour and a half in all", 90, 25, minutes))
            add(Quest("morning", "Два утра в деле – до девяти", "Two things done before 9 am", 2, 20, mornings))
        }
        return all.shuffled(Random(mon)).take(3)
    }

    /** What the residents say today, made of their own days. Russian lines avoid gendered verbs: pets can be anyone. */
    fun chat(live: List<House>): List<Chat> {
        val t = today()
        val out = mutableListOf<Chat>()
        val busyToday = busy(live, t)
        for (h in live) {
            val s = h.snap!!
            val lead = s.walkers.firstOrNull() ?: continue
            val who = lead.name.ifBlank { s.species(lead.species)?.name.orEmpty() }
            val today = s.days.firstOrNull { it.d == t }
            var streak = 0; var d = t; if (s.days.none { it.d == d }) d--
            while (s.days.any { it.d == d }) { streak++; d-- }
            val last = s.days.maxOfOrNull { it.d }
            when {
                today != null -> out += Chat(h.home.id, tr("$who: сегодня уже ${count(today.n, h.home.what)}!", "$who: ${count(today.n, h.home.what)} today already!"))
                last != null && t - last >= 3 -> out += Chat(h.home.id, tr("$who: у нас тихо… можно вздремнуть", "$who: it's quiet here… nap time"))
                else -> out += Chat(h.home.id, tr("$who: заглядывайте в гости!", "$who: drop by any time!"))
            }
            if (streak >= 3) out += Chat(h.home.id, tr("$who: ${count(streak, DAYS)} подряд!", "$who: ${count(streak, DAYS)} in a row!"))
            s.pets.firstOrNull { !it.egg && it.hatched >= t - 3 }?.let { p ->
                out += Chat(h.home.id, tr("Знакомьтесь: ${p.name.ifBlank { s.species(p.species)?.name.orEmpty() }}, новый житель дома «${h.home.house}»", "Meet ${p.name.ifBlank { s.species(p.species)?.name.orEmpty() }}, new in the ${h.home.house}"))
            }
            if (s.pets.any { it.egg }) out += Chat(h.home.id, tr("$who: у нас яйцо, скоро кто-то вылупится", "$who: we've got an egg, someone's hatching soon"))
        }
        if (busyToday.size >= 2) {
            val a = busyToday[0]; val b = busyToday[1]
            val na = a.snap!!.team.firstOrNull()?.name.orEmpty(); val nb = b.snap!!.team.firstOrNull()?.name.orEmpty()
            if (na.isNotBlank() && nb.isNotBlank()) out += Chat(b.home.id, tr("$nb: $na, мы сегодня тоже в деле – вечером праздник!", "$nb: $na, we're busy today too – party tonight!"))
        }
        return out
    }

    /** The village news for the notice board: a few lines about the last days. */
    fun news(live: List<House>): List<String> {
        val t = today(); val mon = mondayOf(t)
        val out = mutableListOf<String>()
        val best = (mon..t).maxByOrNull { dayGain(live, it) }
        if (best != null && dayGain(live, best) > 0) out += tr("Лучший день недели – ${dayName(best).lowercase()}: ${count(busy(live, best).size, HOUSES)} в деле, +${dayGain(live, best)} светлячков.",
            "Best day of the week: ${dayName(best)}, ${busy(live, best).size} houses busy, +${dayGain(live, best)} fireflies.")
        for (h in live) {
            val s = h.snap!!
            val week = s.days.filter { it.d >= mon }
            if (week.isNotEmpty()) out += "${h.home.house}: " + count(week.sumOf { it.n }, h.home.what) + if (h.home.minutes && week.sumOf { it.min } > 0) ", " + count(week.sumOf { it.min }, MINUTES) else ""
            val masters = s.pets.count { it.stage >= 4 }
            if (masters > 0) out += tr("${h.home.house}: мастеров – $masters", "${h.home.house}: $masters masters")
        }
        val residents = live.sumOf { it.snap!!.hatched.size }
        out += tr("Жителей в деревне: $residents", "Residents in the village: $residents")
        return out
    }

    /** Last week in a letter, for the post box. */
    fun letter(live: List<House>): String {
        val mon = mondayOf(today()) - 7; val sun = mon + 6
        val days = (mon..sun).count { busy(live, it).isNotEmpty() }
        val gain = (mon..sun).sumOf { dayGain(live, it) }
        val most = live.maxByOrNull { h -> h.snap!!.days.count { it.d in mon..sun } }
        val parts = mutableListOf(tr("Дела нашлись в ${count(days, DAYS)} из семи, деревня собрала $gain светлячков.", "Something was done on ${count(days, DAYS)} of seven; the village gathered $gain fireflies."))
        if (most != null && most.snap!!.days.any { it.d in mon..sun }) parts += tr("Больше всех света было в доме «${most.home.house}».", "The ${most.home.house} shone the most.")
        val quiet = live.filter { h -> h.snap!!.days.none { it.d in mon..sun } }
        if (quiet.isNotEmpty()) parts += tr("В доме «${quiet.first().home.house}» отдыхали – это тоже нужно.", "The ${quiet.first().home.house} had a rest – that's needed too.")
        return parts.joinToString(" ")
    }
}
