package app.lingvale.domain

import app.lingvale.data.Look
import java.time.LocalDate

/**
 * Something in the shop that changes how the village looks rather than adding to it: a season for the valley, a path,
 * the weather, a house design. [key] is how it is kept among the owned things; the free ones (cost 0) are always owned.
 */
data class StyleItem(
    val key: String, val id: String, val ru: String, val en: String, val aboutRu: String, val aboutEn: String,
    val cost: Int, val level: Int, val art: String = "",
) {
    val name get() = tr(ru, en)
    val about get() = tr(aboutRu, aboutEn)
    val free get() = cost == 0
}

object Style {
    val seasons = listOf(
        StyleItem("bg:summer", "summer", "Лето", "Summer", "Зелёная долина, как в первый день", "The green valley of the first day", 0, 0, "valley_bg"),
        StyleItem("bg:spring", "spring", "Весна", "Spring", "Деревья в цвету", "Trees in blossom", 140, 1, "valley_spring"),
        StyleItem("bg:autumn", "autumn", "Осень", "Autumn", "Золото и багрянец", "Gold and crimson", 140, 1, "valley_autumn"),
        StyleItem("bg:winter", "winter", "Зима", "Winter", "Снег и замёрзший ручей", "Snow and a frozen stream", 180, 2, "valley_winter"),
        StyleItem("bg:dusk", "dusk", "Сиреневые сумерки", "Lilac dusk", "Долина из сказки", "A fairy-tale valley", 220, 3, "valley_dusk"),
        StyleItem("bg:calendar", "calendar", "По календарю", "By the calendar", "Весна, лето, осень и зима сменяются сами, по месяцам", "Spring, summer, autumn and winter follow the months", 320, 3, "valley_spring"),
    )
    val paths = listOf(
        StyleItem("path:sand", "sand", "Песок", "Sand", "Утоптанная тропинка", "A trodden track", 0, 0),
        StyleItem("path:stone", "stone", "Булыжник", "Cobbles", "Мощёная камнем дорога", "A cobbled road", 90, 1, "path_stone"),
        StyleItem("path:brick", "brick", "Кирпич", "Brick", "Красная кирпичная дорожка", "A red brick road", 110, 1, "path_brick"),
    )
    /** [art] here is the symbol shown in the shop. */
    val weather = listOf(
        StyleItem("fx:", "", "Ясно", "Clear", "Ничего не падает с неба", "Nothing falls from the sky", 0, 0, "☀"),
        StyleItem("fx:snow", "snow", "Снегопад", "Snowfall", "Тихо падают снежинки", "Snowflakes drift down", 70, 1, "❄"),
        StyleItem("fx:leaves", "leaves", "Листопад", "Falling leaves", "Ветер несёт листья", "The wind carries leaves", 70, 1, "🍂"),
        StyleItem("fx:petals", "petals", "Лепестки", "Petals", "Летят лепестки вишни", "Cherry petals in the air", 70, 1, "🌸"),
        StyleItem("fx:butterflies", "butterflies", "Бабочки", "Butterflies", "Днём над лугом порхают бабочки", "Butterflies over the meadow by day", 90, 2, "🦋"),
    )

    private val skinNames = mapOf(
        Look.TEA to listOf("Оранжерея" to "Greenhouse", "Горная хижина" to "Mountain hut"),
        Look.GYM to listOf("Додзё" to "Dojo", "Лодочная станция" to "Boathouse"),
        Look.BURROW to listOf("Грибной домик" to "Mushroom house", "Каменный коттедж" to "Stone cottage"),
        Look.TOWER to listOf("Обсерватория" to "Observatory", "Хрустальный шпиль" to "Crystal spire"),
        Look.TAVERN to listOf("Бревенчатый трактир" to "Log inn", "Таверна-замок" to "Keep tavern"),
        Look.STUDIO to listOf("Музыкальный автомат" to "Jukebox", "Эстрада" to "Bandstand"),
        Look.DESK to listOf("Маяк" to "Lighthouse", "Печатная машинка" to "Typewriter"),
        Look.READING to listOf("Дом на дереве" to "Treehouse", "Круглая библиотека" to "Round library"),
        Look.CINEMA to listOf("Летний кинотеатр" to "Open-air cinema", "Кинодворец" to "Movie palace"),
        Look.LODGE to listOf("Избушка на сваях" to "Stilt cabin", "Наблюдательная вышка" to "Watchtower"),
        Look.GATE to listOf("Привал" to "Rest stop", "Шатёр картографа" to "Cartographer's tent"),
        Look.WORKSHOP to listOf("Гончарная хижина" to "Potter's hut", "Фургон-мастерская" to "Workshop wagon"),
        Look.OFFICE to listOf("Почтовая станция" to "Post station", "Ратуша с часами" to "Clock-tower town hall"),
    )

    /** The designs of house [id] with look [look]: the original (0) and the two from the shop. */
    fun skins(id: String, look: Look): List<StyleItem> = listOf(
        StyleItem("skin:$id:0", "0", "Как в приложении", "As in the app", "Первый облик дома", "The house as it was built", 0, 0, houseArt(look, 0, 0)),
    ) + skinNames[look].orEmpty().mapIndexed { i, (ru, en) ->
        StyleItem("skin:$id:${i + 1}", "${i + 1}", ru, en, tr("Новый облик дома", "A new look for the house"), "", 160 + i * 60, 1 + i, houseArt(look, i + 1, 0))
    }

    /** The season the calendar is in. */
    fun seasonOf(d: LocalDate = LocalDate.now()) = when (d.monthValue) { 3, 4, 5 -> "spring"; 6, 7, 8 -> "summer"; 9, 10, 11 -> "autumn"; else -> "winter" }

    /** The valley picture for the chosen season. */
    fun valley(bg: String): String {
        val id = if (bg == "calendar") seasonOf() else bg
        return seasons.firstOrNull { it.id == id }?.art ?: "valley_bg"
    }
}

/** The picture of a house of look [look] in design [skin] (0: the original), by day (0) or night (1). */
fun houseArt(look: Look, skin: Int, n: Int) = "house_${look.name.lowercase()}_${if (skin > 0) "s${skin}_" else ""}$n"
