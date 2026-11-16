package app.lingvale.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.lingvale.ui.art.Art
import app.lingvale.ui.theme.Ink

/** Old paper under the content: the parchment texture tiled, or plain paper colour until it's there. */
@Composable
fun Modifier.paper(): Modifier {
    val tex = Art.get(LocalContext.current, "paper_tex")
    val brush = remember(tex) { tex?.let { ShaderBrush(ImageShader(it, TileMode.Repeated, TileMode.Repeated)) } }
    return this.background(Ink.paper).let { m -> if (brush != null) m.background(brush, alpha = .9f) else m }
}

/** A printed rule: one line, or two close together for a [double] one. */
@Composable
fun Rule(double: Boolean = false, modifier: Modifier = Modifier, color: Color = Ink.ink) {
    Canvas(modifier.fillMaxWidth().height(if (double) 5.dp else 1.dp)) {
        drawLine(color, Offset(0f, 0f), Offset(size.width, 0f), 1.5f)
        if (double) drawLine(color, Offset(0f, size.height), Offset(size.width, size.height), 1f)
    }
}

/** A dotted line for leaders between a time and a title. */
fun Modifier.dotted(color: Color = Ink.rule): Modifier = drawBehind {
    drawLine(color, Offset(0f, size.height * .72f), Offset(size.width, size.height * .72f), 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 6f)))
}

/** Lines of a ledger page drawn under handwriting, [step] apart. */
fun Modifier.ruled(step: Dp = 28.dp, color: Color = Color(0x553A6EA5)): Modifier = drawBehind {
    val s = step.toPx()
    var y = s
    while (y < size.height) { drawLine(color, Offset(0f, y), Offset(size.width, y), 1.2f); y += s }
    drawLine(Color(0x66C0392B), Offset(28.dp.toPx(), 0f), Offset(28.dp.toPx(), size.height), 1.5f)
}

/** A section printed on paper, with a caps title and a rule under it. */
@Composable
fun PaperSection(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier.fillMaxWidth().padding(top = 18.dp)) {
        androidx.compose.material3.Text(title.uppercase(), style = Ink.caps)
        Rule(modifier = Modifier.padding(top = 4.dp, bottom = 10.dp), color = Ink.rule)
        Box { content() }
    }
}
