package app.lingvale.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.lingvale.R

/** Colour tokens: cream paper, meadow green for action, warm lamp gold. */
class Pal(
    val dark: Boolean, val bg: Color, val card: Color, val card2: Color, val ink: Color, val mute: Color, val line: Color, val track: Color,
    val acc: Color, val accInk: Color, val accSoft: Color, val gold: Color, val goldSoft: Color, val nav: Color, val shadow: Color,
)

private fun c(hex: Long) = Color(0xFF000000 or hex)

val Light = Pal(false, c(0xF4F1E6), c(0xFFFFFF), c(0xEBE6D7), c(0x26302A), c(0x7C857C), c(0xE2DDCD), c(0xE7E2D3),
    c(0x4F8A55), c(0xFFFFFF), c(0xDDEBDA), c(0xD99A2B), c(0xFAEED6), c(0xFBFAF4), Color(0x22303A2A))

val Dark = Pal(true, c(0x121712), c(0x1B221C), c(0x232C24), c(0xE8EEE6), c(0x8F9A8E), c(0x2B342B), c(0x283128),
    c(0x86C08A), c(0x0F1A10), c(0x1E3220), c(0xF0B455), c(0x3A2E1A), c(0x161C16), Color(0x55000000))

/** The palette in use; switched by the theme setting before the tree is drawn. */
object C {
    @Volatile var p: Pal = Light
    val dark get() = p.dark
    val bg get() = p.bg; val card get() = p.card; val card2 get() = p.card2; val ink get() = p.ink; val mute get() = p.mute
    val line get() = p.line; val track get() = p.track; val acc get() = p.acc; val accInk get() = p.accInk; val accSoft get() = p.accSoft
    val gold get() = p.gold; val goldSoft get() = p.goldSoft; val nav get() = p.nav; val shadow get() = p.shadow
}

val Nunito = FontFamily(Font(R.font.nunito_regular, FontWeight.Normal), Font(R.font.nunito_semibold, FontWeight.SemiBold), Font(R.font.nunito_bold, FontWeight.Bold), Font(R.font.nunito_bold, FontWeight.ExtraBold))
val Inter = FontFamily(Font(R.font.inter_regular, FontWeight.Normal), Font(R.font.inter_medium, FontWeight.Medium), Font(R.font.inter_semibold, FontWeight.SemiBold), Font(R.font.inter_semibold, FontWeight.Bold))

val Serif = FontFamily(Font(R.font.pt_serif_regular, FontWeight.Normal), Font(R.font.pt_serif_bold, FontWeight.Bold), Font(R.font.pt_serif_italic, FontWeight.Normal, androidx.compose.ui.text.font.FontStyle.Italic))
val Hand = FontFamily(Font(R.font.caveat, FontWeight.Normal))

/** Paper things in the village – the gazette, the album, the house book – keep their own ink whatever the theme. */
object Ink {
    val paper = Color(0xFFF2E8D2); val paper2 = Color(0xFFE9DCC0); val ink = Color(0xFF2E2418); val soft = Color(0xFF6E5C45)
    val rule = Color(0xFFB8A27C); val red = Color(0xFFA8322A); val blue = Color(0xFF2D4A7A)
    val mast get() = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 27.sp, letterSpacing = 1.5.sp, color = ink)
    val head get() = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 23.sp, lineHeight = 27.sp, color = ink)
    val h3 get() = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 20.sp, color = ink)
    val body get() = TextStyle(fontFamily = Serif, fontSize = 14.sp, lineHeight = 19.sp, color = ink)
    val lead get() = TextStyle(fontFamily = Serif, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, fontSize = 15.sp, lineHeight = 21.sp, color = ink)
    val small get() = TextStyle(fontFamily = Serif, fontSize = 12.sp, lineHeight = 16.sp, color = soft)
    val caps get() = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.4.sp, color = soft)
    val hand get() = TextStyle(fontFamily = Hand, fontSize = 21.sp, lineHeight = 24.sp, color = blue)
    val handBig get() = TextStyle(fontFamily = Hand, fontSize = 26.sp, lineHeight = 28.sp, color = blue)
}

object T {
    val title get() = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, lineHeight = 30.sp, color = C.ink)
    val h2 get() = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 19.sp, lineHeight = 24.sp, color = C.ink)
    val h3 get() = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 21.sp, color = C.ink)
    val body get() = TextStyle(fontFamily = Nunito, fontSize = 15.sp, lineHeight = 21.sp, color = C.ink)
    val bodyB get() = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 21.sp, color = C.ink)
    val small get() = TextStyle(fontFamily = Nunito, fontSize = 13.sp, lineHeight = 17.sp, color = C.mute)
    val smallB get() = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 17.sp, color = C.ink)
    val tiny get() = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 14.sp, color = C.mute)
    val sec get() = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, letterSpacing = 1.sp, color = C.mute)
    val num get() = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = C.ink)
    val numS get() = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = C.ink)
    val btn get() = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
}

@Composable
fun ValeTheme(dark: Boolean, content: @Composable () -> Unit) {
    C.p = if (dark) Dark else Light
    val p = C.p
    val scheme = if (dark) darkColorScheme(primary = p.acc, background = p.bg, surface = p.card, onSurface = p.ink, onBackground = p.ink)
    else lightColorScheme(primary = p.acc, background = p.bg, surface = p.card, onSurface = p.ink, onBackground = p.ink)
    MaterialTheme(colorScheme = scheme, content = content)
}
