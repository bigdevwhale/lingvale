package app.lingvale.ui.components

import app.lingvale.ui.theme.Ink
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.lingvale.ui.theme.C
import app.lingvale.ui.theme.T

@Composable
fun Card(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, pad: Dp = 16.dp, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(C.card)
            .border(1.dp, C.line, RoundedCornerShape(18.dp))
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }.padding(pad),
        content = content,
    )
}

@Composable
fun Sec(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = T.sec, modifier = modifier.padding(start = 4.dp, top = 18.dp, bottom = 8.dp))
}

@Composable
fun Dot(color: Color, size: Dp = 10.dp) = Box(Modifier.size(size).clip(CircleShape).background(color))

@Composable
fun Pill(text: String, on: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.clip(RoundedCornerShape(50)).background(if (on) Color(0xFF6B4423) else Ink.wash(0x14)).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = app.lingvale.ui.theme.Ink.body.copy(color = if (on) Color(0xFFFFF1D6) else app.lingvale.ui.theme.Ink.ink)) }
}

@Composable
fun Btn(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, soft: Boolean = false) {
    Box(
        modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(10.dp)).background(if (soft) Ink.wash(0x22) else Color(0xFF6B4423)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = app.lingvale.ui.theme.Ink.h3.copy(color = if (soft) app.lingvale.ui.theme.Ink.ink else Color(0xFFFFF1D6))) }
}

/** A number with a caption under it. */
@Composable
fun RowScope.Stat(value: String, label: String) {
    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = T.num)
        Text(label, style = T.tiny, textAlign = TextAlign.Center)
    }
}

/** A thin bar 0..1. */
@Composable
fun Meter(v: Float, color: Color, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(C.track)) {
        Box(Modifier.fillMaxWidth(v.coerceIn(0f, 1f)).height(6.dp).clip(RoundedCornerShape(3.dp)).background(color))
    }
}

@Composable
fun Gap(h: Dp) = Box(Modifier.height(h))

@Composable
fun Line(vararg parts: @Composable RowScope.() -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { parts.forEach { it() } }
}
