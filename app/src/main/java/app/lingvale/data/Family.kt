package app.lingvale.data

import app.lingvale.domain.Words
import app.lingvale.domain.tr

/** How a house looks in the village. */
enum class Look { TEA, GYM, BURROW, TOWER, TAVERN, STUDIO, DESK, READING, CINEMA }

/**
 * One app of the -ling family: its house in the village. [bridged] apps already speak to Lingvale;
 * the others get an empty plot until their next version. [what] counts what a day's activity is made of.
 */
data class Home(
    val id: String, val app: String, val houseRu: String, val houseEn: String, val look: Look,
    val color: Long, val what: Words, val minutes: Boolean,
    val aboutRu: String, val aboutEn: String, val untitledRu: String, val untitledEn: String,
    val bridged: Boolean,
) {
    val house get() = tr(houseRu, houseEn)
    val about get() = tr(aboutRu, aboutEn)
    val untitled get() = tr(untitledRu, untitledEn)
    val page get() = "https://bigdevwhale.github.io/stillcraft/apps/$id/"
}

object Family {
    val all = listOf(
        Home("puffling", "Puffling", "Чайный домик", "Tea house", Look.TEA, 0xFF3E8FB0,
            Words("практика", "практики", "практик", "practice", "practices"), true,
            "Дыхание, тишина и дневник чувств", "Breathing, stillness and a diary of feelings", "Практика", "Practice", bridged = true),
        Home("pumpling", "Pumpling", "Подвальный зал", "Basement gym", Look.GYM, 0xFFC9783A,
            Words("тренировка", "тренировки", "тренировок", "workout", "workouts"), true,
            "Тренировки и кардио", "Workouts and cardio", "Тренировка", "Workout", bridged = true),
        Home("pennyling", "Pennyling", "Норка", "Burrow", Look.BURROW, 0xFF5E8A4E,
            Words("запись", "записи", "записей", "record", "records"), false,
            "Деньги от зарплаты до зарплаты", "Money from payday to payday", "Запись", "Record", bridged = true),
        Home("reelkin", "Reelkin", "Кинотеатр", "Cinema", Look.CINEMA, 0xFF9E3B4A,
            Words("сеанс", "сеанса", "сеансов", "screening", "screenings"), true,
            "Фильмы и сериалы с питомцем в будке киномеханика", "Films and series with a pet in the projection booth", "Сеанс", "Screening", bridged = true),
        Home("inkling", "Inkling", "Башня академии", "Academy tower", Look.TOWER, 0xFF7A5BB5,
            Words("занятие", "занятия", "занятий", "lesson", "lessons"), true,
            "Учёба в академии магии", "Study in an academy of magic", "Занятие", "Lesson", bridged = true),
        Home("questling", "Questling", "Таверна", "Tavern", Look.TAVERN, 0xFFB5523E,
            Words("партия", "партии", "партий", "session", "sessions"), true,
            "Дневник игр", "A diary of games", "Партия", "Session", bridged = true),
        Home("songling", "Songling", "Студия", "Studio", Look.STUDIO, 0xFFC8553D,
            Words("песня", "песни", "песен", "song", "songs"), true,
            "Питомец, который слушает музыку", "A pet that listens to music", "Музыка", "Music", bridged = true),
        Home("keyling", "Keyling", "Письменный стол", "Writing desk", Look.DESK, 0xFF3557C9,
            Words("запись", "записи", "записей", "entry", "entries"), false,
            "Тексты, романы, диссертации", "Novels, stories, theses", "Текст", "Writing", bridged = true),
        Home("familiar", "Familiar", "Читальня", "Reading room", Look.READING, 0xFFAE93FF,
            Words("чтение", "чтения", "чтений", "reading", "readings"), true,
            "Книги и фамильяр", "Books and a familiar", "Чтение", "Reading", bridged = true),
    )
    val byId = all.associateBy { it.id }
}
