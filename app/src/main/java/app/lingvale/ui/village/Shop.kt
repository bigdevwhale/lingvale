package app.lingvale.ui.village

import app.lingvale.ui.components.sheetMax
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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.setValue

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
    ModalBottomSheet(onDismissRequest = onClose, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = Ink.paper, contentWindowInsets = { androidx.compose.foundation.layout.WindowInsets(0) }) {
        androidx.compose.foundation.layout.Box(Modifier.sheetMax()) {
        ShopBody(houses)
        }
    }
}

/** The shop's shelves. */
@Composable
fun ShopBody(houses: List<House>, startTab: Int = 0) {
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
            var tab by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableIntStateOf(startTab) }
            app.lingvale.ui.components.IconTabs(listOf("decor_well" to tr("Украшения", "Decor"), "nav_village" to tr("Долина", "Valley"), "g_crown" to tr("Дома", "Houses")), tab, { tab = it })
            Gap(10.dp)
            when (tab) {
                1 -> ValleyShelves(level.n, balance)
                2 -> HouseShelves(live, level.n, balance)
                else -> DecorShelves(level.n, balance)
            }
        }
    }
}

/** The decorations that stand in the valley. */
@Composable
private fun DecorShelves(levelN: Int, balance: Int) {
    val c = LocalContext.current.container
    val pr by c.prefs.progress.collectAsState()
    run {
        run {
            Game.decor.chunked(3).forEach { row ->
                Row(Modifier.padding(bottom = 8.dp).height(androidx.compose.foundation.layout.IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { d ->
                        val owned = d.id in pr.owned
                        val locked = d.level > levelN
                        val can = !owned && !locked && d.cost <= balance
                        Column(
                            Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(16.dp)).background(if (owned) Color(0x1F5E8A4E) else Ink.wash(0x10))
                                .clickable(enabled = can) { c.prefs.update { it.copy(spent = it.spent + d.cost, owned = it.owned + d.id) } }
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            ArtIcon("decor_${d.id}", 72.dp, Modifier.alpha(if (locked) .35f else 1f))
                            Text(d.name, style = Ink.h3.copy(fontSize = 14.sp), textAlign = TextAlign.Center, maxLines = 2, minLines = 2)
                            Text(d.about, style = Ink.small, textAlign = TextAlign.Center, maxLines = 2, minLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            Spacer(Modifier.weight(1f).heightIn(min = 6.dp))
                            when {
                                owned -> Text(tr("✓ в деревне", "✓ in the village"), style = Ink.small.copy(color = Ink.green), maxLines = 1, modifier = Modifier.padding(vertical = 4.dp))
                                locked -> Text(tr("🔒 ${Game.levels[d.level].name}", "🔒 ${Game.levels[d.level].name}"), style = Ink.small, maxLines = 1, modifier = Modifier.padding(vertical = 4.dp))
                                else -> Row(Modifier.clip(RoundedCornerShape(10.dp)).background(if (can) Color(0xFF6B4423) else Ink.wash(0x22)).padding(horizontal = 10.dp, vertical = 4.dp),
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

/** Seasons, paths and weather: each bought once, then picked freely. */
@Composable
private fun ValleyShelves(level: Int, balance: Int) {
    val c = LocalContext.current.container
    val st by c.prefs.style.collectAsState()
    Shelf(tr("Время года", "Season"), app.lingvale.domain.Style.seasons, { st.bg == it.id }, level, balance, { c.prefs.setStyle { s -> s.copy(bg = it.id) } }) { item, dim ->
        Thumb(item.art, null, dim)
    }
    Shelf(tr("Дорожка", "Path"), app.lingvale.domain.Style.paths, { st.path == it.id }, level, balance, { c.prefs.setStyle { s -> s.copy(path = it.id) } }) { item, dim ->
        Thumb("valley_bg", item.art.ifEmpty { null }, dim, crop = true)
    }
    Shelf(tr("Погода", "Weather"), app.lingvale.domain.Style.weather, { st.weather == it.id }, level, balance, { c.prefs.setStyle { s -> s.copy(weather = it.id) } }) { item, dim ->
        androidx.compose.foundation.layout.Box(Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF9CC9E0)).alpha(if (dim) .4f else 1f), contentAlignment = Alignment.Center) {
            Text(item.art, fontSize = 30.sp)
        }
    }
}

/** A design for every house in the village. */
@Composable
private fun HouseShelves(live: List<House>, level: Int, balance: Int) {
    val c = LocalContext.current.container
    val st by c.prefs.style.collectAsState()
    if (live.isEmpty()) Text(tr("Сначала постройте дом.", "Build a house first."), style = Ink.body)
    live.forEach { h ->
        Shelf(h.home.house, app.lingvale.domain.Style.skins(h.home.id, h.home.look), { (st.skins[h.home.id] ?: 0).toString() == it.id }, level, balance,
            { c.prefs.setStyle { s -> s.copy(skins = s.skins + (h.home.id to it.id.toInt())) } }) { item, dim ->
            ArtIcon(item.art, 72.dp, Modifier.alpha(if (dim) .4f else 1f))
        }
    }
}

/**
 * A row of things to choose from: a free or owned one is picked with a tap; another one is bought with the tap
 * (when the fireflies are there and the level is reached) and picked at once.
 */
@Composable
private fun Shelf(title: String, items: List<app.lingvale.domain.StyleItem>, chosen: (app.lingvale.domain.StyleItem) -> Boolean, level: Int, balance: Int,
                  pick: (app.lingvale.domain.StyleItem) -> Unit, picture: @Composable (app.lingvale.domain.StyleItem, Boolean) -> Unit) {
    val c = LocalContext.current.container
    val pr by c.prefs.progress.collectAsState()
    app.lingvale.ui.components.PaperSection(title) {}
    items.chunked(3).forEach { row ->
        Row(Modifier.padding(bottom = 8.dp).height(androidx.compose.foundation.layout.IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { item ->
                val owned = item.free || item.key in pr.owned
                val locked = !owned && item.level > level
                val can = !owned && !locked && item.cost <= balance
                val on = chosen(item)
                Column(
                    Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(16.dp))
                        .background(if (on) Color(0x335E8A4E) else Ink.wash(0x10))
                        .then(if (on) Modifier.border(2.dp, Color(0xFF5E8A4E), RoundedCornerShape(16.dp)) else Modifier)
                        .clickable(enabled = owned || can) {
                            if (!owned) c.prefs.update { if (item.key in it.owned) it else it.copy(spent = it.spent + item.cost, owned = it.owned + item.key) }
                            pick(item)
                        }
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    picture(item, locked)
                    Text(item.name, style = Ink.h3.copy(fontSize = 14.sp), textAlign = TextAlign.Center, maxLines = 2, minLines = 2)
                    Text(item.about, style = Ink.small, textAlign = TextAlign.Center, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    Spacer(Modifier.weight(1f).heightIn(min = 4.dp))
                    when {
                        on -> Text(tr("✓ выбрано", "✓ chosen"), style = Ink.small.copy(color = Ink.green), maxLines = 1, modifier = Modifier.padding(vertical = 4.dp))
                        owned -> Text(tr("выбрать", "pick"), style = Ink.small.copy(color = Ink.blue), maxLines = 1, modifier = Modifier.padding(vertical = 4.dp))
                        locked -> Text("🔒 ${Game.levels[item.level].name}", style = Ink.small, maxLines = 1, modifier = Modifier.padding(vertical = 4.dp))
                        else -> Row(Modifier.clip(RoundedCornerShape(10.dp)).background(if (can) Color(0xFF6B4423) else Ink.wash(0x22)).padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            ArtIcon("icon_firefly", 16.dp); Spacer(Modifier.width(2.dp))
                            Text("${item.cost}", style = Ink.h3.copy(color = if (can) Color(0xFFFFF1D6) else Ink.soft))
                        }
                    }
                }
            }
            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

/** A small picture of the valley [bg] with [overlay] on top; [crop] shows only the middle, around the bridge. */
@Composable
private fun Thumb(bg: String, overlay: String?, dim: Boolean, crop: Boolean = false) {
    val ctx = LocalContext.current
    androidx.compose.foundation.layout.Box(Modifier.size(width = 64.dp, height = if (crop) 64.dp else 96.dp).clip(RoundedCornerShape(10.dp)).alpha(if (dim) .4f else 1f)) {
        listOfNotNull(bg, overlay).forEach { name ->
            app.lingvale.ui.art.Art.get(ctx, name)?.let { b ->
                val painter = androidx.compose.runtime.remember(b, crop) {
                    if (crop) androidx.compose.ui.graphics.painter.BitmapPainter(b, androidx.compose.ui.unit.IntOffset(b.width * 3 / 10, b.height * 4 / 10), androidx.compose.ui.unit.IntSize(b.width * 2 / 5, b.width * 2 / 5), androidx.compose.ui.graphics.FilterQuality.Low)
                    else androidx.compose.ui.graphics.painter.BitmapPainter(b, filterQuality = androidx.compose.ui.graphics.FilterQuality.Low)
                }
                androidx.compose.foundation.Image(painter, null, Modifier.matchParentSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
            }
        }
    }
}
