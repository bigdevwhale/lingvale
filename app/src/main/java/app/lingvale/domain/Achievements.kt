package app.lingvale.domain

import app.lingvale.data.Family
import app.lingvale.data.House
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** An achievement: [have] of [goal], [reward] fireflies once claimed. [group] sorts them into sections. */
data class Ach(
    val id: String, val group: Int, val icon: String, val ru: String, val en: String, val aboutRu: String, val aboutEn: String,
    val goal: Int, val have: Int, val reward: Int,
) {
    val title get() = tr(ru, en)
    val about get() = tr(aboutRu, aboutEn)
    val done get() = have >= goal
}

object Achievements {
    val groups get() = listOf(tr("Деревня", "Village"), tr("Дела", "Things done"), tr("Постоянство", "Steadiness"), tr("Жители", "Residents"), tr("Особые случаи", "Special"))

    private val rewards = listOf(10, 20, 35, 60, 100, 150)

    /** Everything the village can reach, worked out from the houses' days, the fireflies gathered and what the shop sold. */
    fun all(live: List<House>, total: Int, decor: Int, quests: Int): List<Ach> {
        val out = mutableListOf<Ach>()
        fun tiers(id: String, group: Int, icon: String, have: Int, vararg t: Pair<Int, Array<String>>) = t.forEachIndexed { i, (goal, s) ->
            out += Ach("$id$i", group, icon, s[0], s[1], s[2], s[3], goal, have, rewards[minOf(i, rewards.lastIndex)])
        }
        fun one(id: String, group: Int, icon: String, have: Boolean, ru: String, en: String, aboutRu: String, aboutEn: String, reward: Int = 30) =
            run { out += Ach(id, group, icon, ru, en, aboutRu, aboutEn, 1, if (have) 1 else 0, reward) }

        val snaps = live.map { it.snap!! }
        val busy = HashMap<Long, Int>(); val things = HashMap<Long, Int>(); val mins = HashMap<Long, Int>()
        snaps.forEach { s -> s.days.forEach { d -> busy.merge(d.d, 1, Int::plus); things.merge(d.d, d.n, Int::plus); mins.merge(d.d, d.min, Int::plus) } }
        val days = busy.keys.sorted()
        val bestRun = run { var best = 0; var cur = 0; var prev = Long.MIN_VALUE; for (d in days) { cur = if (d == prev + 1) cur + 1 else 1; best = maxOf(best, cur); prev = d }; best }
        val houseRun = snaps.maxOfOrNull { s ->
            var best = 0; var cur = 0; var prev = Long.MIN_VALUE
            for (d in s.days.map { it.d }.sorted()) { cur = if (d == prev + 1) cur + 1 else 1; best = maxOf(best, cur); prev = d }
            best
        } ?: 0
        val fairs = if (live.size < 2) 0 else busy.values.count { it >= minOf(3, live.size) }
        val zone = ZoneId.systemDefault()
        val hours = snaps.flatMap { s -> s.events.map { Instant.ofEpochMilli(it.at).atZone(zone).hour } }
        val hatched = snaps.sumOf { it.hatched.size }
        val masters = snaps.sumOf { s -> s.pets.count { it.stage >= 4 } }
        val grown = snaps.sumOf { s -> s.pets.count { it.stage >= 3 } }
        val gaps = days.zipWithNext { a, b -> b - a }

        // The village itself.
        tiers("lvl", 0, "g_crown", total,
            Game.levels[1].from to arrayOf("Деревня", "Village", "Хутор вырос в деревню", "The hamlet grows into a village"),
            Game.levels[2].from to arrayOf("Село", "Large village", "Деревня стала селом", "The village becomes a large one"),
            Game.levels[3].from to arrayOf("Посёлок", "Township", "Село выросло в посёлок", "Grow into a township"),
            Game.levels[4].from to arrayOf("Городок", "Small town", "Высший уровень долины", "The valley's top level"))
        tiers("ff", 0, "icon_firefly", total,
            100 to arrayOf("Первая банка", "First jar", "Собрать 100 светлячков", "Gather 100 fireflies"),
            500 to arrayOf("Светлячковый луг", "Firefly meadow", "Собрать 500 светлячков", "Gather 500 fireflies"),
            1500 to arrayOf("Светлая долина", "Bright valley", "Собрать 1 500 светлячков", "Gather 1,500 fireflies"),
            4000 to arrayOf("Млечный путь", "Milky Way", "Собрать 4 000 светлячков", "Gather 4,000 fireflies"),
            10000 to arrayOf("Звёздная ночь", "Starry night", "Собрать 10 000 светлячков", "Gather 10,000 fireflies"))
        tiers("house", 0, "nav_village", live.size,
            1 to arrayOf("Новоселье", "Housewarming", "Построить первый дом", "Build the first house"),
            3 to arrayOf("Соседи", "Neighbours", "Три дома в деревне", "Three houses in the village"),
            5 to arrayOf("Улица", "A street", "Пять домов", "Five houses"),
            7 to arrayOf("Квартал", "A quarter", "Семь домов", "Seven houses"),
            Family.all.size to arrayOf("Вся семья в сборе", "The whole family", "Все дома семейства в долине", "Every house of the family in the valley"))
        tiers("decor", 0, "decor_well", decor,
            1 to arrayOf("Первая покупка", "First purchase", "Купить что-нибудь в лавке", "Buy something in the shop"),
            5 to arrayOf("Уютный двор", "Cosy yard", "Пять украшений", "Five decorations"),
            10 to arrayOf("Нарядная деревня", "Dressed-up village", "Десять украшений", "Ten decorations"),
            Game.decor.size to arrayOf("Вся лавка", "The whole shop", "Купить всё, что есть в лавке", "Buy everything in the shop"))
        tiers("quest", 0, "icon_scroll", quests,
            1 to arrayOf("Первое задание", "First task", "Выполнить задание недели", "Complete a weekly task"),
            10 to arrayOf("Исполнительный", "Reliable", "10 выполненных заданий", "10 tasks done"),
            30 to arrayOf("Мастер поручений", "Errand master", "30 выполненных заданий", "30 tasks done"),
            75 to arrayOf("Опора деревни", "Village pillar", "75 выполненных заданий", "75 tasks done"))

        // Things done.
        tiers("n", 1, "g_checklist", things.values.sum(),
            10 to arrayOf("Первые шаги", "First steps", "10 дел во всех домах", "10 things done in all houses"),
            50 to arrayOf("Разгон", "Picking up", "50 дел", "50 things done"),
            200 to arrayOf("Трудяга", "Hard worker", "200 дел", "200 things done"),
            600 to arrayOf("Неутомимый", "Tireless", "600 дел", "600 things done"),
            1500 to arrayOf("Легенда долины", "Valley legend", "1 500 дел", "1,500 things done"))
        tiers("min", 1, "g_hourglass", mins.values.sum(),
            300 to arrayOf("Пять часов", "Five hours", "300 минут в делах", "300 minutes of things"),
            1200 to arrayOf("Двадцать часов", "Twenty hours", "1 200 минут", "1,200 minutes"),
            3000 to arrayOf("Полсотни часов", "Fifty hours", "3 000 минут", "3,000 minutes"),
            6000 to arrayOf("Сотня часов", "A hundred hours", "6 000 минут", "6,000 minutes"),
            15000 to arrayOf("Двести пятьдесят часов", "250 hours", "15 000 минут", "15,000 minutes"))
        tiers("bigday", 1, "g_sun", things.values.maxOrNull() ?: 0,
            5 to arrayOf("Насыщенный день", "Full day", "5 дел за один день", "5 things in one day"),
            10 to arrayOf("Длинный день", "Long day", "10 дел за один день", "10 things in one day"),
            20 to arrayOf("День-рекорд", "Record day", "20 дел за один день", "20 things in one day"))
        tiers("long", 1, "g_hourglass", mins.values.maxOrNull() ?: 0,
            60 to arrayOf("Час в деле", "An hour at it", "60 минут за день", "60 minutes in a day"),
            180 to arrayOf("Марафон", "Marathon", "3 часа за день", "3 hours in a day"))
        tiers("multi", 1, "g_link", busy.values.maxOrNull() ?: 0,
            2 to arrayOf("Вдвоём веселее", "Better together", "Два дома в деле в один день", "Two houses busy on one day"),
            4 to arrayOf("Оживлённая улица", "Busy street", "Четыре дома в один день", "Four houses on one day"),
            6 to arrayOf("Вся деревня в деле", "Whole village at it", "Шесть домов в один день", "Six houses on one day"))
        tiers("fair", 1, "icon_flags", fairs,
            1 to arrayOf("Первая ярмарка", "First fair", "День, когда заняты три дома", "A day with three houses busy"),
            10 to arrayOf("Ярмарочный сезон", "Fair season", "10 ярмарок", "10 fairs"),
            40 to arrayOf("Вечный праздник", "Endless festival", "40 ярмарок", "40 fairs"))

        // Steadiness.
        tiers("days", 2, "g_calendar_small", days.size,
            7 to arrayOf("Неделя дел", "A week of things", "7 дней с делами", "7 busy days"),
            30 to arrayOf("Месяц дел", "A month of things", "30 дней с делами", "30 busy days"),
            100 to arrayOf("Сто дней", "A hundred days", "100 дней с делами", "100 busy days"),
            200 to arrayOf("Двести дней", "Two hundred days", "200 дней с делами", "200 busy days"),
            365 to arrayOf("Год в долине", "A year in the valley", "365 дней с делами", "365 busy days"))
        tiers("run", 2, "g_flame", bestRun,
            3 to arrayOf("Искра", "Spark", "3 дня подряд хоть что-то в деревне", "Something in the village 3 days running"),
            7 to arrayOf("Огонёк", "Flame", "7 дней подряд", "7 days running"),
            14 to arrayOf("Костёр", "Bonfire", "14 дней подряд", "14 days running"),
            30 to arrayOf("Очаг", "Hearth", "30 дней подряд", "30 days running"),
            100 to arrayOf("Вечный огонь", "Eternal flame", "100 дней подряд", "100 days running"))
        tiers("hrun", 2, "g_flame", houseRun,
            5 to arrayOf("Привычка", "A habit", "5 дней подряд в одном доме", "5 days running in one house"),
            21 to arrayOf("Три недели", "Three weeks", "21 день подряд в одном доме", "21 days running in one house"),
            60 to arrayOf("Образ жизни", "A way of life", "60 дней подряд в одном доме", "60 days running in one house"))
        tiers("vet", 2, "g_book", snaps.maxOfOrNull { it.days.size } ?: 0,
            50 to arrayOf("Обжитой дом", "Lived-in house", "50 дней с делами в одном доме", "50 busy days in one house"),
            150 to arrayOf("Старожил", "Old-timer", "150 дней с делами в одном доме", "150 busy days in one house"))

        // Residents.
        tiers("pets", 3, "g_egg", hatched,
            3 to arrayOf("Первые жители", "First residents", "3 жителя в деревне", "3 residents in the village"),
            10 to arrayOf("Шумная деревня", "Lively village", "10 жителей", "10 residents"),
            25 to arrayOf("Многолюдно", "Crowded", "25 жителей", "25 residents"),
            50 to arrayOf("Целый город", "A whole town", "50 жителей", "50 residents"))
        tiers("grown", 3, "g_sprout", grown,
            1 to arrayOf("Подросли", "All grown up", "Вырастить жителя до взрослого", "Raise a resident to grown"),
            10 to arrayOf("Взрослая компания", "Grown-up crowd", "10 взрослых жителей", "10 grown residents"))
        tiers("master", 3, "g_crown", masters,
            1 to arrayOf("Мастер", "Master", "Житель стал мастером", "A resident becomes a master"),
            5 to arrayOf("Гильдия мастеров", "Masters' guild", "5 мастеров", "5 masters"),
            15 to arrayOf("Академия мастеров", "Masters' academy", "15 мастеров", "15 masters"))
        one("shiny", 3, "star", snaps.any { s -> s.pets.any { it.shiny && !it.egg } }, "Блестящий", "Shiny", "Житель с особым окрасом", "A resident with a rare colouring", 50)
        one("rare", 3, "g_magnifier", snaps.any { s -> s.hatched.any { p -> s.species(p.species)?.rare == true } }, "Редкая находка", "Rare find", "Житель редкого вида", "A resident of a rare species", 50)
        one("album", 3, "nav_album", snaps.any { s -> s.species.isNotEmpty() && s.species.all { sp -> s.hatched.any { it.species == sp.id } } },
            "Полный альбом", "Full album", "Все виды одного дома", "Every species of one house", 100)
        one("egg", 3, "g_egg", snaps.any { s -> s.pets.any { it.egg } }, "Что-то зреет", "Something's brewing", "В деревне есть яйцо", "There's an egg in the village", 10)

        // Special.
        one("early", 4, "g_sunrise", hours.any { it in 4..6 }, "Ранняя пташка", "Early bird", "Дело до семи утра", "Something done before 7 am", 20)
        one("owl", 4, "g_moon", hours.any { it >= 23 || it < 3 }, "Ночная сова", "Night owl", "Дело после одиннадцати вечера", "Something done after 11 pm", 20)
        one("lunch", 4, "g_sun", hours.any { it in 12..13 }, "Обеденный перерыв", "Lunch break", "Дело в обед, с 12 до 14", "Something done at lunchtime, 12 to 2", 15)
        one("weekend", 4, "g_calendar_small", days.any { LocalDate.ofEpochDay(it).dayOfWeek == DayOfWeek.SATURDAY && busy.containsKey(it + 1) },
            "Выходные в деле", "Busy weekend", "И суббота, и воскресенье с делами", "Both Saturday and Sunday busy", 20)
        one("week", 4, "g_checklist", days.any { d -> LocalDate.ofEpochDay(d).dayOfWeek == DayOfWeek.MONDAY && (0..6).all { busy.containsKey(d + it) } },
            "Полная неделя", "Full week", "Все семь дней недели с понедельника по воскресенье", "Every day from Monday to Sunday", 40)
        one("back", 4, "g_up", gaps.any { it >= 14 }, "С возвращением", "Welcome back", "Вернуться к делам после двух недель тишины", "Get back to things after two quiet weeks", 30)
        one("newyear", 4, "star", days.any { LocalDate.ofEpochDay(it).let { d -> d.monthValue == 1 && d.dayOfMonth == 1 } }, "Новогоднее", "New Year's", "Дело 1 января", "Something done on January 1", 40)
        one("all", 4, "g_trophy", live.size >= 3 && busy.values.any { it >= live.size }, "Свет во всех окнах", "Every window lit", "Все дома деревни (от трёх) в деле в один день", "Every house (three or more) busy on one day", 60)
        return out
    }
}
