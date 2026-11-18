package app.lingvale

import app.lingvale.data.Manifest
import app.lingvale.data.Moving
import app.lingvale.data.NotMoving
import app.lingvale.data.Part
import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

/** A moving file comes back as it was packed, and the village's own things survive the trip. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MovingTest {
    private val dir = File(System.getProperty("java.io.tmpdir"), "moving-test").apply { deleteRecursively(); mkdirs() }

    @Test fun packsAndOpens() {
        val a = File(dir, "a").apply { writeBytes(ByteArray(70_000) { (it % 251).toByte() }) }
        val b = File(dir, "b").apply { writeText("""{"app":"roamling"}""") }
        val m = Manifest(1234L, JSONObject().put("start", 5), listOf(Part("pumpling", "apps/pumpling.zip", "1.2"), Part("roamling", "apps/roamling.json", "1.0")))
        val zip = File(dir, "m.zip")
        zip.outputStream().use { Moving.pack(it, m, "9.9", mapOf("pumpling" to a, "roamling" to b)) }
        val back = Moving.open(zip)
        assertEquals(m.parts, back.parts); assertEquals(1234L, back.at); assertEquals(5, back.village!!.getInt("start"))
        val out = File(dir, "out")
        assertTrue(Moving.extract(zip, "apps/pumpling.zip", out)); assertArrayEquals(a.readBytes(), out.readBytes())
        assertFalse(Moving.extract(zip, "apps/nothing.json", out))
    }

    @Test fun refusesOtherFiles() {
        val notZip = File(dir, "x").apply { writeText("hello") }
        assertFalse(runCatching { Moving.open(notZip) }.exceptionOrNull().let { it as NotMoving }.newer)
        assertFalse((runCatching { Moving.decode("""{"app":"pumpling","format":1}""") }.exceptionOrNull() as NotMoving).newer)
        assertTrue((runCatching { Moving.decode("""{"app":"lingvale","format":${Moving.FORMAT + 1}}""") }.exceptionOrNull() as NotMoving).newer)
    }

    @Test fun villageSurvives() {
        val p = Prefs(RuntimeEnvironment.getApplication())
        p.build("puffling"); p.build("roamling"); p.setTheme("dark")
        p.setStyle { it.copy(bg = "winter", weather = "snow", skins = mapOf("roamling" to 2)) }
        p.update { it.copy(start = 20000, spent = 40, bonus = 7, owned = setOf("well"), claimed = setOf("ach:x"), letter = 3, got = setOf("x")) }
        val file = JSONObject(p.export().toString())
        p.demolish("puffling"); p.setTheme(""); p.setStyle { app.lingvale.Styling() }; p.update { it.copy(start = 1, spent = 0, owned = emptySet(), got = emptySet()) }
        p.restore(file)
        assertEquals(listOf("puffling", "roamling"), p.built.value); assertEquals("dark", p.theme.value)
        assertEquals(app.lingvale.Styling("winter", "sand", "snow", mapOf("roamling" to 2)), p.style.value)
        assertEquals(Progress(20000, 40, 7, setOf("well"), setOf("ach:x"), 3, setOf("x")), p.progress.value)
        assertEquals(p.progress.value, Prefs(RuntimeEnvironment.getApplication()).progress.value)   // and it is on disk
    }
}
