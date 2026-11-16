package app.lingvale

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import app.lingvale.data.DayInfo
import app.lingvale.data.EventInfo
import app.lingvale.data.Family
import app.lingvale.data.House
import app.lingvale.data.PetInfo
import app.lingvale.data.Snap
import app.lingvale.data.SpeciesInfo
import app.lingvale.data.Status
import app.lingvale.domain.Lang
import app.lingvale.domain.today
import app.lingvale.ui.Root
import app.lingvale.ui.Start
import app.lingvale.ui.theme.ValeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

/**
 * The screens drawn to PNGs in app/build/art/screens (run with `-Prender`), from made-up snapshots and the
 * real pet pictures of the sibling repos (../puffling, ../pumpling, ../pennyling).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "ru-w393dp-h1700dp-xxhdpi")
class RenderScreens {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private val out = File(System.getProperty("render.out") ?: "build/art", "screens").apply { mkdirs() }
    private val repos = File("../..").canonicalFile

    private fun settle() = repeat(8) { rule.mainClock.advanceTimeBy(400); Thread.sleep(200); org.robolectric.shadows.ShadowLooper.idleMainLooper() }
    private fun shot(name: String) {
        settle()
        val v = rule.activity.window.decorView
        val b = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
        rule.runOnUiThread { v.draw(android.graphics.Canvas(b)) }
        File(out, "$name.png").outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun res(app: String) = File(repos, "$app/app/src/main/res/drawable-nodpi")

    private fun snap(app: String, r: Random): Snap {
        val kinds = res(app).list().orEmpty().filter { it.startsWith("pet_") && it.endsWith("_0.webp") }.map { it.removePrefix("pet_").removeSuffix("_0.webp") }.sorted()
        val species = kinds.map { SpeciesInfo(it, it.replaceFirstChar { c -> c.uppercase() }, it, 0xFF000000 or r.nextLong(0x404040, 0xC0C0C0), "×1,5 за что-нибудь хорошее", "×1.5 for something good", false) }
        val owned = kinds.shuffled(r).take(kinds.size / 2)
        val pets = owned.mapIndexed { i, k -> PetInfo(i + 1L, k, k.take(4).replaceFirstChar { it.uppercase() }, if (i < 3) 3 - (i % 2) else 1 + i % 4, r.nextDouble(), if (i < 3) i else -1, i == 5, today() - 90, today() - 80) } +
            PetInfo(99, kinds.first(), "", 0, .4, -1, false, today() - 2, 0)
        val rr = Random(11)
        val pump = (0 until 180).filter { rr.nextFloat() < .5f }.toSet()
        val dayKs = (0 until 180).filter { k -> when (app) { "pumpling" -> k in pump; "puffling" -> if (k in pump) r.nextFloat() < .8f else r.nextFloat() < .25f; else -> r.nextFloat() < .55f } }
        val titles = mapOf("puffling" to listOf("Квадратное дыхание", "Вим Хоф", "Тихое сидение"), "pumpling" to listOf("Ноги", "Верх тела", "Бег 5 км"),
            "pennyling" to listOf("Продукты · 1 240 ₽", "Кафе · 340 ₽", "Транспорт · 62 ₽"))[app]!!
        val zone = ZoneId.systemDefault()
        val events = dayKs.filter { it < 8 }.flatMap { k ->
            val d = LocalDate.now().minusDays(k.toLong())
            (0..r.nextInt(2)).map { j -> EventInfo(d.atTime(7 + r.nextInt(14), r.nextInt(60)).atZone(zone).toInstant().toEpochMilli(), if (app == "pennyling") 0 else 10 + r.nextInt(40), titles[j % 3], titles[j % 3]) }
        }
        val days = dayKs.map { k ->
            val ev = events.filter { java.time.Instant.ofEpochMilli(it.at).atZone(zone).toLocalDate().toEpochDay() == today() - k }
            if (k < 8) DayInfo(today() - k, ev.size, ev.sumOf { it.min }) else DayInfo(today() - k, 1 + r.nextInt(3), if (app == "pennyling") 0 else 10 + r.nextInt(50))
        }
        val scene = mapOf("puffling" to "room_bg", "pumpling" to "gym_bg", "pennyling" to "burrow_bg")[app]!!
        return Snap(app, "1.0", true, scene, pets, species, days, events)
    }

    @Test fun render() {
        Lang.ru = true
        val c = ApplicationProvider.getApplicationContext<LingvaleApp>().container
        c.prefs.welcomed = true
        c.prefs.askedToBuild = true
        listOf("puffling", "pumpling", "pennyling").forEach { c.prefs.build(it) }
        c.prefs.update { it.copy(owned = app.lingvale.domain.Game.decor.map { d -> d.id }.toSet()) }
        c.art.source = { pkg, name -> File(res(pkg.removePrefix("app.").substringBefore('.')), "$name.webp").takeIf { it.exists() }?.readBytes() }
        val r = Random(7)
        c.village.set(Family.all.map { h ->
            when (h.id) {
                "puffling", "pumpling", "pennyling" -> House(h, Status.LIVE, "app.${h.id}", snap(h.id, r))
                "inkling" -> House(h, Status.OLD, "app.inkling", null)
                else -> House(h, Status.MISSING, null, null)
            }
        })
        val pumpling = c.village.houses.value.first { it.home.id == "pumpling" }
        val shots: List<Pair<String, @androidx.compose.runtime.Composable () -> Unit>> = listOf(
            "01-village" to { Start.tab = 0; Root() },
            "02-house-overview" to { app.lingvale.ui.village.HouseBody(pumpling, startTab = 0) },
            "03-house-residents" to { app.lingvale.ui.village.HouseBody(pumpling, startTab = 1) },
            "04-house-history" to { app.lingvale.ui.village.HouseBody(pumpling, startTab = 2) },
            "05-shop" to { app.lingvale.ui.village.ShopBody(c.village.houses.value) },
            "06-album" to { Start.tab = 1; Root() },
            "07-day" to { Start.tab = 2; Start.daysTab = 0; Root() },
            "08-week" to { Start.tab = 2; Start.daysTab = 1; Root() },
            "09-links" to { Start.tab = 2; Start.daysTab = 2; Root() },
            "10-gazette" to { Start.tab = 2; Start.daysTab = 3; Root() },
            "11-quilt" to { Start.tab = 2; Start.daysTab = 4; Root() },
            "13-shop-valley" to { app.lingvale.ui.village.ShopBody(c.village.houses.value.filter { it.live }, startTab = 1) },
            "14-shop-houses" to { app.lingvale.ui.village.ShopBody(c.village.houses.value.filter { it.live }, startTab = 2) },
            "15-village-winter" to {
                c.prefs.setStyle { it.copy(bg = "winter", path = "stone", weather = "snow", skins = mapOf("puffling" to 1, "pumpling" to 2, "pennyling" to 1)) }
                Start.tab = 0; Root()
            },
            "16-village-autumn" to {
                c.prefs.setStyle { it.copy(bg = "autumn", path = "brick", weather = "leaves", skins = mapOf("puffling" to 2, "pumpling" to 1, "pennyling" to 2)) }
                Start.tab = 0; Root()
            },
            "12-achievements" to { androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.padding(horizontal = 18.dp).verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                val live = c.village.houses.value.filter { it.live && it.home.id in c.prefs.built.value }
                app.lingvale.ui.village.AchievementList(app.lingvale.domain.Achievements.all(live, 900, c.prefs.progress.value.owned.size, 4))
            } },
        )
        var idx by mutableIntStateOf(0)
        rule.mainClock.autoAdvance = false
        rule.setContent { key(idx) { ValeTheme(false) { androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.background(app.lingvale.ui.theme.Ink.paper)) { shots[idx].second() } } } }
        shots.forEachIndexed { i, (name, _) ->
            if (i > 0) rule.runOnUiThread { idx = i }
            shot(name)
        }
    }
}
