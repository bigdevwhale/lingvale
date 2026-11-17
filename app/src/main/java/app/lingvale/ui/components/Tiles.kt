package app.lingvale.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.lingvale.ui.theme.Ink
import app.lingvale.ui.village.ArtIcon

/** Tabs drawn as little pictures with a word under them; the chosen one sits up and gets a red underline. */
@Composable
fun IconTabs(tabs: List<Pair<String, String>>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        tabs.forEachIndexed { i, (icon, label) ->
            val on = i == selected
            val lift by animateFloatAsState(if (on) 1f else 0f, label = "tab")
            Column(
                Modifier.clip(RoundedCornerShape(12.dp)).background(if (on) Color(0x1A6B4423) else Color.Transparent)
                    .clickable { onSelect(i) }.padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ArtIcon(icon, 30.dp, Modifier.graphicsLayer { translationY = -4f * lift; scaleX = 1f + .12f * lift; scaleY = 1f + .12f * lift }.alpha(if (on) 1f else .55f))
                Text(label, style = if (on) Ink.h3.copy(fontSize = Ink.small.fontSize) else Ink.small, maxLines = 1)
                Box(Modifier.padding(top = 2.dp).height(2.dp).width(if (on) 24.dp else 0.dp).background(Ink.red))
            }
        }
    }
}

/** A figure with its picture: the number big, what it is under it, and how it moved since before. */
@Composable
fun RowScope.Tile(icon: String, value: String, caption: String, trend: String? = null, up: Boolean? = null) {
    Column(
        Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(Ink.wash(0x14))
            .border(1.dp, Ink.wash(0x22), RoundedCornerShape(14.dp)).padding(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ArtIcon(icon, 30.dp)
            Spacer(Modifier.width(6.dp))
            Text(value, style = Ink.head.copy(fontSize = Ink.h3.fontSize * 1.25f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(caption, style = Ink.small, maxLines = 2)
        if (trend != null) Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
            if (up != null) ArtIcon(if (up) "g_up" else "g_down", 16.dp)
            Spacer(Modifier.width(3.dp))
            Text(trend, style = Ink.small.copy(color = when (up) { true -> Ink.green; false -> Ink.red; null -> Ink.soft }), maxLines = 1)
        }
    }
}

/** A line with a picture in front: for facts and patterns. */
@Composable
fun IconLine(icon: String, text: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        ArtIcon(icon, 28.dp)
        Spacer(Modifier.width(10.dp))
        Text(text, style = Ink.body, modifier = Modifier.weight(1f))
    }
}

/** Two tiles side by side. */
@Composable
fun TileRow(content: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), content = content)
}
