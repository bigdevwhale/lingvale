package app.lingvale

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
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
import app.lingvale.ui.theme.Ink
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
 * A full village of eight houses drawn to app/build/art/village (run with `-Prender`): the layout where the reading
 * room stands under the cinema (#2), and the weekly tasks with every hint open. Apps whose pets are painted in code
 * borrow a sibling's pet pictures; only where the residents stand matters here.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "ru-w393dp-h1700dp-xxhdpi")
class RenderVillage {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private val out = File(System.getProperty("render.out") ?: "build/art", "village").apply { mkdirs() }
    private val repos = File("../..").canonicalFile
    private val order = listOf("reelkin", "songling", "familiar", "inkling", "questling", "puffling", "pumpling", "pennyling")
    private val artOf = mapOf("reelkin" to "questling", "songling" to "puffling", "familiar" to "inkling")

    private fun settle() = repeat(8) { rule.mainClock.advanceTimeBy(400); Thread.sleep(200); org.robolectric.shadows.ShadowLooper.idleMainLooper() }
    private fun shot(name: String) {
        settle()
        val v = rule.activity.window.decorView
        val b = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
        rule.runOnUiThread { v.draw(android.graphics.Canvas(b)) }
        File(out, "$name.png").outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun res(app: String) = File(repos, "${artOf[app] ?: app}/app/src/main/res/drawable-nodpi")

    private fun snap(app: String, r: Random): Snap {
        val kinds = res(app).list().orEmpty().filter { it.startsWith("pet_") && it.endsWith("_0.webp") }.map { it.removePrefix("pet_").removeSuffix("_0.webp") }.sorted()
        val species = kinds.map { SpeciesInfo(it, it, it, 0xFF808080, "", "", false) }
        val pets = kinds.shuffled(r).take(3).mapIndexed { i, k -> PetInfo(i + 1L, k, k, 3, .5, i, false, today() - 90, today() - 80) }
        val zone = ZoneId.systemDefault()
        val events = (0 until 4).map { k -> EventInfo(LocalDate.now().minusDays(k.toLong()).atTime(8 + k * 3, 10).atZone(zone).toInstant().toEpochMilli(), 30, "Дело", "Thing") }
        val days = (0 until 30).map { k -> DayInfo(today() - k, 1 + k % 2, 30) }
        return Snap(app, "1.0", true, "room_bg", pets, species, days, events)
    }

    @Test fun render() {
        Lang.ru = true
        val c = ApplicationProvider.getApplicationContext<LingvaleApp>().container
        c.prefs.welcomed = true
        c.prefs.askedToBuild = true
        order.forEach { c.prefs.build(it) }
        c.art.source = { pkg, name -> File(res(pkg.removePrefix("app.").substringBefore('.')), "$name.webp").takeIf { it.exists() }?.readBytes() }
        val r = Random(7)
        c.village.set(Family.all.map { h ->
            if (h.id in order) House(h, Status.LIVE, "app.${h.id}", snap(h.id, r)) else House(h, Status.MISSING, null, null)
        })
        val live = c.village.houses.value.filter { it.live }
        val shots: List<Pair<String, @androidx.compose.runtime.Composable () -> Unit>> = listOf(
            "village" to { Start.tab = 0; Root() },
            "quests" to { Column(Modifier.padding(18.dp)) { app.lingvale.ui.village.Quests(live, openAll = true) } },
        )
        var idx by mutableIntStateOf(0)
        rule.mainClock.autoAdvance = false
        rule.setContent { key(idx) { ValeTheme(false) { Box(Modifier.background(Ink.paper)) { shots[idx].second() } } } }
        shots.forEachIndexed { i, (name, _) ->
            if (i > 0) rule.runOnUiThread { idx = i }
            shot(name)
        }
    }
}
