package app.lingvale.ui.bestiary

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lingvale.data.House
import app.lingvale.data.PetInfo
import app.lingvale.data.SpeciesInfo
import app.lingvale.domain.tr
import app.lingvale.ui.art.Sprite
import app.lingvale.ui.components.Gap
import app.lingvale.ui.components.Rule
import app.lingvale.ui.components.IconTabs
import app.lingvale.ui.components.IconLine
import androidx.compose.foundation.layout.BoxWithConstraints
import app.lingvale.ui.components.paper
import app.lingvale.ui.theme.Ink
import app.lingvale.ui.village.ArtIcon
import app.lingvale.ui.village.stageName

private data class Pick(val house: House, val sp: SpeciesInfo, val no: Int)

/** A pale version of a colour, for the stamp's background. */
private fun pale(c: Long, k: Float = .72f): Color { val x = Color(c); return Color(x.red + (1 - x.red) * k, x.green + (1 - x.green) * k, x.blue + (1 - x.blue) * k) }

/** The stamp album: a page for every house, a stamp for every kind of creature; the ones met are franked. */
@Composable
fun BestiaryScreen(houses: List<House>) {
    val live = houses.filter { it.live && it.pkg != null }
    var page by remember { mutableIntStateOf(0) }
    var pick by remember { mutableStateOf<Pick?>(null) }
    val total = live.sumOf { it.snap!!.species.size }
    val met = live.sumOf { h -> h.snap!!.hatched.map { it.species }.distinct().size }
    Column(Modifier.fillMaxSize().paper().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 18.dp, vertical = 12.dp)) {
        Text(tr("Альбом марок", "The stamp album"), style = Ink.head.copy(fontSize = 28.sp))
        Text(if (live.isEmpty()) tr("Марки появятся, когда в деревню переедет первый дом", "Stamps come when the first house moves in") else tr("собрано $met из $total", "$met of $total collected"), style = Ink.handBig)
        if (live.isEmpty()) return@Column
        Box(Modifier.padding(top = 4.dp).fillMaxWidth().height(6.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(3.dp)).background(Color(0x1A000000))) {
            Box(Modifier.fillMaxWidth(if (total == 0) 0f else met.toFloat() / total).height(6.dp).background(Ink.red))
        }
        Gap(12.dp)
        IconTabs(live.map { "house_${it.home.look.name.lowercase()}_0" to it.home.house }, page, { page = it })
        val h = live[page.coerceIn(live.indices)]
        val snap = h.snap!!
        val mine = snap.hatched.groupBy { it.species }
        Gap(10.dp)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(h.home.house, style = Ink.head, modifier = Modifier.weight(1f))
            Text(tr("${mine.size} из ${snap.species.size}", "${mine.size} of ${snap.species.size}"), style = Ink.hand)
        }
        Rule(double = true, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
        snap.species.withIndex().chunked(4).forEach { row ->
            Row(Modifier.padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { (i, sp) ->
                    val best = mine[sp.id]?.maxByOrNull { it.stage }
                    Stamp(h, sp, i + 1, best, Modifier.weight(1f).clickable { pick = Pick(h, sp, i + 1) })
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        Gap(8.dp)
        Text(tr("Новые марки приходят из ${h.home.app}: там находят яйца.", "New stamps come from ${h.home.app}, where the eggs are found."), style = Ink.small)
    }
    pick?.let { PickSheet(it) { pick = null } }
}

/** One stamp: the creature on its own colour inside a perforated border, franked when met; an empty mount with a question when not. */
@Composable
private fun Stamp(h: House, sp: SpeciesInfo, no: Int, best: PetInfo?, modifier: Modifier = Modifier, big: Boolean = false) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        BoxWithConstraints(Modifier.fillMaxWidth().aspectRatio(.84f)) {
            val w = maxWidth
            Box(Modifier.fillMaxSize().padding(horizontal = w * .1f, vertical = w * .1f).background(if (best != null) pale(sp.color) else Color(0x0F000000)))
            if (best != null) {
                Sprite(h.pkg!!, sp.id, best.stage, w * .74f, Modifier.align(Alignment.Center).offset(y = w * .04f), shiny = best.shiny)
                ArtIcon("postmark", w * .42f, Modifier.align(Alignment.TopEnd).offset(x = w * .04f, y = -w * .02f).alpha(.45f).rotate(-12f))
                if (best.shiny) ArtIcon("star", w * .2f, Modifier.align(Alignment.BottomEnd).offset(x = -w * .12f, y = -w * .12f))
            } else {
                Text("?", style = Ink.head.copy(fontSize = (if (big) 64 else 34).sp, color = Ink.rule), modifier = Modifier.align(Alignment.Center))
            }
            ArtIcon("frame_stamp", w, Modifier.fillMaxSize().alpha(if (best != null) 1f else .55f))
            Text("$no", style = Ink.caps.copy(color = Ink.soft), modifier = Modifier.align(Alignment.TopStart).padding(start = w * .14f, top = w * .12f))
        }
        if (!big) Text(if (best != null) sp.name else tr("не встречен", "not met"), style = Ink.small.copy(color = if (best != null) Ink.ink else Ink.rule),
            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 1.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PickSheet(p: Pick, onClose: () -> Unit) {
    val snap = p.house.snap!!
    val mine = snap.pets.filter { it.species == p.sp.id && !it.egg }.sortedByDescending { it.stage }
    val best = mine.firstOrNull()
    ModalBottomSheet(onDismissRequest = onClose, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = Ink.paper) {
        Column(Modifier.fillMaxWidth().paper().padding(horizontal = 22.dp).navigationBarsPadding().padding(bottom = 22.dp)) {
            Stamp(p.house, p.sp, p.no, best, Modifier.width(200.dp).align(Alignment.CenterHorizontally), big = true)
            Gap(8.dp)
            Text(if (best != null) p.sp.name else tr("Марка № ${p.no} пока пустая", "Stamp No. ${p.no} is still empty"), style = Ink.head, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Text("${p.house.home.house} · ${p.house.home.app}", style = Ink.small, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Gap(10.dp)
            IconLine("g_magnifier", tr("Особая примета: ", "Distinctive trait: ") + p.sp.about.replaceFirstChar { it.lowercase() })
            if (mine.isEmpty()) IconLine("g_egg", tr("Яйцо этого вида находят в ${p.house.home.app}.", "Its egg is found in ${p.house.home.app}."))
            mine.forEach { pet ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Sprite(p.house.pkg!!, pet.species, pet.stage, 40.dp, shiny = pet.shiny)
                    Spacer(Modifier.width(10.dp))
                    Text(pet.name.ifBlank { p.sp.name }, style = Ink.h3, modifier = Modifier.weight(1f))
                    Text(stageName(pet.stage).lowercase() + if (pet.stage < 4) tr(" · ${(pet.progress * 100).toInt()}% до роста", " · ${(pet.progress * 100).toInt()}% to grow") else "", style = Ink.small)
                }
            }
        }
    }
}
