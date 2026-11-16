package app.lingvale.ui.village

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.lingvale.container
import app.lingvale.data.House
import app.lingvale.domain.Game
import app.lingvale.domain.fmt
import app.lingvale.domain.tr
import app.lingvale.ui.components.Gap
import app.lingvale.ui.theme.Ink
import app.lingvale.ui.components.paper
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

/** The village shop: fireflies buy things that stand in the valley for good. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopSheet(houses: List<House>, onClose: () -> Unit) {
    val c = LocalContext.current.container
    val pr by c.prefs.progress.collectAsState()
    val live = houses.filter { it.live }
    val total = Game.gathered(live, pr.start) + pr.bonus
    val balance = total - pr.spent
    val level = Game.level(total)
    ModalBottomSheet(onDismissRequest = onClose, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = Ink.paper) {
        ShopBody(houses)
    }
}

/** The shop's shelves. */
@Composable
fun ShopBody(houses: List<House>) {
    val c = LocalContext.current.container
    val pr by c.prefs.progress.collectAsState()
    val live = houses.filter { it.live }
    val total = Game.gathered(live, pr.start) + pr.bonus
    val balance = total - pr.spent
    val level = Game.level(total)
    run {
        Column(Modifier.fillMaxWidth().paper().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding().padding(bottom = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tr("Лавка", "The shop"), style = Ink.head.copy(fontSize = 28.sp), modifier = Modifier.weight(1f))
                ArtIcon("icon_firefly", 30.dp); Text(fmt(balance), style = Ink.head.copy(color = Color(0xFFB07A10)))
            }
            Text(tr("Светлячков приносят дела в приложениях: по два за каждое и бонус, когда в один день заняты несколько домов. Купленное остаётся в деревне насовсем.",
                "Fireflies come from what you do in the apps: two for each thing, and a bonus when several houses are busy on the same day. What you buy stays in the village for good."), style = Ink.lead)
            Gap(12.dp)
            Game.decor.chunked(3).forEach { row ->
                Row(Modifier.padding(bottom = 8.dp).height(androidx.compose.foundation.layout.IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { d ->
                        val owned = d.id in pr.owned
                        val locked = d.level > level.n
                        val can = !owned && !locked && d.cost <= balance
                        Column(
                            Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(16.dp)).background(if (owned) Color(0x1F5E8A4E) else Color(0x10000000))
                                .clickable(enabled = can) { c.prefs.update { it.copy(spent = it.spent + d.cost, owned = it.owned + d.id) } }
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            ArtIcon("decor_${d.id}", 72.dp, Modifier.alpha(if (locked) .35f else 1f))
                            Text(d.name, style = Ink.h3.copy(fontSize = 14.sp), textAlign = TextAlign.Center, maxLines = 2, minLines = 2)
                            Text(d.about, style = Ink.small, textAlign = TextAlign.Center, maxLines = 2, minLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            Spacer(Modifier.weight(1f).heightIn(min = 6.dp))
                            when {
                                owned -> Text(tr("✓ в деревне", "✓ in the village"), style = Ink.small.copy(color = Color(0xFF4F7A3A)), maxLines = 1, modifier = Modifier.padding(vertical = 4.dp))
                                locked -> Text(tr("🔒 ${Game.levels[d.level].name}", "🔒 ${Game.levels[d.level].name}"), style = Ink.small, maxLines = 1, modifier = Modifier.padding(vertical = 4.dp))
                                else -> Row(Modifier.clip(RoundedCornerShape(10.dp)).background(if (can) Color(0xFF6B4423) else Color(0x22000000)).padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    ArtIcon("icon_firefly", 16.dp); Spacer(Modifier.width(2.dp))
                                    Text("${d.cost}", style = Ink.h3.copy(color = if (can) Color(0xFFFFF1D6) else Ink.soft))
                                }
                            }
                        }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}
