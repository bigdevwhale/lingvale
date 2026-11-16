package app.lingvale.ui.art

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.platform.LocalContext
import app.lingvale.container
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Hue turned for the rare pets, as in the apps. */
fun shinyFilter(): ColorFilter {
    val a = PI.toFloat() * .55f; val c = cos(a); val s = sin(a)
    val m = floatArrayOf(
        .213f + c * .787f - s * .213f, .715f - c * .715f - s * .715f, .072f - c * .072f + s * .928f, 0f, 0f,
        .213f - c * .213f + s * .143f, .715f + c * .285f + s * .140f, .072f - c * .072f - s * .283f, 0f, 0f,
        .213f - c * .213f - s * .787f, .715f - c * .715f + s * .715f, .072f + c * .928f + s * .072f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f)
    return ColorFilter.colorMatrix(ColorMatrix(m))
}

/** The frame of a pet sheet for a stage: 0 baby, 1 youngster, 2 grown (and master). */
fun frameFor(stage: Int) = when { stage <= 1 -> 0; stage == 2 -> 1; else -> 2 }

/** A creature borrowed from its app: an egg for stage 0, a dark silhouette when [shadow] (not met yet). */
@Composable
fun Sprite(pkg: String, species: String, stage: Int, size: Dp, modifier: Modifier = Modifier, shiny: Boolean = false, shadow: Boolean = false, tint: Color = Color(0x33000000)) {
    val art = LocalContext.current.container.art
    val name = if (stage == 0) "egg_$species" else "pet_${species}_${frameFor(stage)}"
    Canvas(modifier.size(size)) {
        val img = art.get(pkg, name) ?: return@Canvas
        val w = this.size.width; val h = this.size.height
        val scale = if (shadow) .92f else when (stage) { 0 -> .62f; 1 -> .66f; 2 -> .82f; else -> .96f }
        val box = minOf(w / img.width, h * .9f / img.height) * scale
        val bw = img.width * box; val bh = img.height * box
        val footY = h * .94f
        if (!shadow) drawOval(Color(0x22000000), Offset(w * .26f, footY - h * .035f), Size(w * .48f, h * .07f))
        drawImage(img, IntOffset.Zero, IntSize(img.width, img.height), IntOffset(((w - bw) / 2).roundToInt(), (footY - bh).roundToInt()),
            IntSize(bw.roundToInt(), bh.roundToInt()), filterQuality = FilterQuality.Medium,
            colorFilter = when { shadow -> ColorFilter.tint(tint); shiny -> shinyFilter(); else -> null })
    }
}
