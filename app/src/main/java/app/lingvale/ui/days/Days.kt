package app.lingvale.ui.days

import app.lingvale.ui.art.art

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.lingvale.container
import app.lingvale.data.House
import app.lingvale.domain.DAYS
import app.lingvale.domain.Game
import app.lingvale.domain.Gazette
import app.lingvale.domain.Issue
import app.lingvale.domain.clock
import app.lingvale.domain.count
import app.lingvale.domain.dayName
import app.lingvale.domain.mondayOf
import app.lingvale.domain.today
import app.lingvale.domain.tr
import app.lingvale.domain.weekdayShort
import app.lingvale.ui.art.Sprite
import app.lingvale.ui.components.Gap
import app.lingvale.ui.components.dotted
import app.lingvale.ui.components.paper
import app.lingvale.ui.components.Rule
import app.lingvale.ui.theme.Ink
import app.lingvale.ui.village.ArtIcon

@Composable
fun DaysScreen(houses: List<House>) {
    val live = houses.filter { it.live }
    var tab by remember { mutableIntStateOf(app.lingvale.ui.Start.daysTab) }
    var day by remember { mutableLongStateOf(today()) }
    Column(Modifier.fillMaxSize().paper().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 18.dp, vertical = 10.dp)) {
        Text(tr("Дни", "Days"), style = Ink.head)
        Gap(6.dp)
        app.lingvale.ui.components.IconTabs(listOf("g_sun" to tr("День", "Day"), "g_checklist" to tr("Неделя", "Week"), "g_link" to tr("Связи", "Links"),
            "nav_days" to tr("Вестник", "Gazette"), "g_calendar_small" to tr("Одеяло", "Quilt")), tab, { tab = it })
        Rule(color = Ink.rule, modifier = Modifier.padding(top = 2.dp))
        Gap(8.dp)
        if (live.isEmpty()) {
            Text(tr("Газета выйдет, как только в деревню переедет первый дом.", "The gazette comes out as soon as the first house moves in."), style = Ink.lead)
            return@Column
        }
        when (tab) {
            0 -> DayTab(live, day) { day = it }
            1 -> WeekTab(live)
            2 -> LinksTab(live)
            3 -> Paper(live, day) { day = it }
            else -> Quilt(live) { day = it; tab = 0 }
        }
    }
}

@Composable
private fun Tab(text: String, on: Boolean, onClick: () -> Unit) {
    Column(Modifier.clickable(onClick = onClick).padding(vertical = 6.dp)) {
        Text(text, style = if (on) Ink.h3 else Ink.h3.copy(color = Ink.soft, fontWeight = androidx.compose.ui.text.font.FontWeight.Normal))
        Box(Modifier.padding(top = 3.dp).height(2.dp).width(if (on) 40.dp else 0.dp).background(Ink.red))
    }
}

/** One day's issue of the gazette. */
@Composable
private fun Paper(live: List<House>, day: Long, onDay: (Long) -> Unit) {
    val c = LocalContext.current.container
    val pr by c.prefs.progress.collectAsState()
    val total = Game.gathered(live, pr.start) + pr.bonus
    val balance = total - pr.spent
    val hint = Game.decor.firstOrNull { it.id !in pr.owned && it.cost > balance && it.level <= Game.level(total).n }?.let {
        tr("Лавка сообщает: «${it.name}» ждёт покупателя – ${it.cost} светлячков.", "The shop says: the ${it.name.lowercase()} awaits a buyer – ${it.cost} fireflies.")
    }
    val issue: Issue = remember(live, day, pr) { Gazette.issue(live, day, pr.start, Game.level(total).name, hint) }

    Skyline(live, pr.owned)
    Text(tr("ВЕСТНИК ЛИНГВЕЙЛА", "THE LINGVALE GAZETTE"), style = Ink.mast, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    Rule(double = true, modifier = Modifier.padding(top = 4.dp))
    Text(issue.dateLine, style = Ink.small, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
    Rule()
    Gap(12.dp)
    Text(issue.headline, style = Ink.head)
    Gap(6.dp)
    Text(issue.lead, style = Ink.lead)
    if (issue.pieces.isNotEmpty()) {
        Gap(14.dp)
        // Two columns, like a real paper.
        val cols = if (issue.pieces.size == 1) listOf(issue.pieces) else listOf(issue.pieces.filterIndexed { i, _ -> i % 2 == 0 }, issue.pieces.filterIndexed { i, _ -> i % 2 == 1 })
        Row(Modifier.height(IntrinsicSize.Min)) {
            cols.forEachIndexed { ci, col ->
                if (ci == 1) Box(Modifier.padding(horizontal = 10.dp).width(1.dp).fillMaxHeight().background(Ink.rule))
                Column(Modifier.weight(1f)) {
                    col.forEach { p ->
                        val lead = p.house.snap!!.team.firstOrNull()
                        Row(verticalAlignment = Alignment.Bottom) {
                            ArtIcon(p.house.art(0), 54.dp)
                            if (lead != null && p.house.pkg != null) Sprite(p.house.pkg, lead.species, lead.stage, 40.dp, shiny = lead.shiny, frame = lead.frame)
                        }
                        Text(p.title, style = Ink.h3)
                        Text(p.text, style = Ink.body)
                        Gap(4.dp)
                        Text(p.quote, style = Ink.body.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, color = Ink.soft))
                        Gap(14.dp)
                    }
                }
            }
        }
    }
    if (issue.chronicle.isNotEmpty()) {
        Text(tr("ХРОНИКА ДНЯ", "THE DAY'S CHRONICLE"), style = Ink.caps, modifier = Modifier.padding(top = 6.dp))
        Rule(color = Ink.rule, modifier = Modifier.padding(vertical = 4.dp))
        issue.chronicle.forEach { (e, h) ->
            Row(Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.Bottom) {
                Text(clock(e.at), style = Ink.body.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), modifier = Modifier.width(46.dp))
                Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(h.home.color)))
                Spacer(Modifier.width(6.dp))
                Text(e.title.ifBlank { h.home.untitled }, style = Ink.body)
                Spacer(Modifier.weight(1f).height(16.dp).dotted())
                if (e.min > 0) Text(tr("${e.min} мин", "${e.min} min"), style = Ink.small)
            }
        }
    }
    if (issue.notices.isNotEmpty()) {
        Gap(16.dp)
        Column(Modifier.fillMaxWidth().background(Ink.wash(0x14)).padding(12.dp)) {
            Text(tr("ОБЪЯВЛЕНИЯ", "NOTICES"), style = Ink.caps)
            Gap(4.dp)
            issue.notices.forEach { Text("· $it", style = Ink.body, modifier = Modifier.padding(vertical = 2.dp)) }
        }
    }
    Gap(18.dp)
    Rule(double = true)
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        if (day > today() - 6) Text(tr("← вчерашний выпуск", "← the day before"), style = Ink.h3.copy(color = Ink.red), modifier = Modifier.clickable { onDay(day - 1) })
        Spacer(Modifier.weight(1f))
        if (day < today()) Text(tr("следующий →", "next →"), style = Ink.h3.copy(color = Ink.red), modifier = Modifier.clickable { onDay(day + 1) })
    }
}

/** A fabric for each house: its colour and a pattern of its own. */
private fun DrawScope.fabric(color: Color, kind: Int, o: Offset, s: Float, clip: Path? = null) {
    val draw: DrawScope.() -> Unit = {
        drawRect(color, o, Size(s, s))
        val d = color.copy(alpha = 1f).let { Color(it.red * .78f, it.green * .78f, it.blue * .78f) }
        val l = Color(1f, 1f, 1f, .35f)
        when (kind % 4) {
            0 -> { var y = o.y + s * .15f; while (y < o.y + s) { var x = o.x + s * .15f; while (x < o.x + s) { drawCircle(l, s * .05f, Offset(x, y)); x += s * .25f }; y += s * .25f } }
            1 -> { var k = -s; while (k < s) { drawLine(d, Offset(o.x + k, o.y), Offset(o.x + k + s, o.y + s), s * .06f); k += s * .2f } }
            2 -> { var k = 0f; while (k < s) { drawLine(d, Offset(o.x + k, o.y), Offset(o.x + k, o.y + s), s * .03f); drawLine(d, Offset(o.x, o.y + k), Offset(o.x + s, o.y + k), s * .03f); k += s * .2f } }
            else -> { var y = o.y + s * .2f; while (y < o.y + s) { drawLine(l, Offset(o.x, y), Offset(o.x + s, y), s * .04f); y += s * .2f } }
        }
    }
    if (clip != null) clipPath(clip) { draw() } else clipRect(o.x, o.y, o.x + s, o.y + s) { draw() }
}

/** The year as a patchwork quilt: a patch a day, sewn from the fabrics of the houses busy that day. */
@Composable
private fun Quilt(live: List<House>, onIssue: (Long) -> Unit) {
    val weeks = 18
    val end = today()
    val start = mondayOf(end) - (weeks - 1) * 7L
    val busy = remember(live) { (start..end).associateWith { d -> Game.busy(live, d) } }
    val kinds = live.withIndex().associate { (i, h) -> h.home.id to i }
    var picked by remember { mutableStateOf<Long?>(null) }
    val patches = busy.count { it.value.isNotEmpty() }
    val streak = run { var best = 0; var cur = 0; for (d in start..end) { if (busy[d].isNullOrEmpty()) cur = 0 else { cur++; best = maxOf(best, cur) } }; best }
    Text(tr("Каждый лоскут – один день. Цвет даёт дом, где в тот день что-то происходило; если домов несколько, лоскут сшит из нескольких тканей.",
        "Each patch is one day. Its colour comes from the house that was busy; when several were, the patch is sewn from several fabrics."), style = Ink.lead)
    Gap(10.dp)
    Row { (0..6).forEach { k -> Text(weekdayShort(start + k), style = Ink.caps, textAlign = TextAlign.Center, modifier = Modifier.weight(1f)) } }
    Canvas(Modifier.fillMaxWidth().aspectRatio(7f / weeks).pointerInput(busy) {
        detectTapGestures { o ->
            val cell = size.width / 7f
            val d = start + (o.y / cell).toInt() * 7 + (o.x / cell).toInt()
            if (d <= end) picked = d
        }
    }) {
        val cell = size.width / 7f; val gap = cell * .06f; val s = cell - gap
        val linen = Color(0xFFE6D9BD)
        for (w in 0 until weeks) for (k in 0 until 7) {
            val d = start + w * 7 + k
            if (d > end) continue
            val o = Offset(k * cell + gap / 2, w * cell + gap / 2)
            val hs = busy[d].orEmpty()
            when (hs.size) {
                0 -> { drawRect(linen, o, Size(s, s)); var y = o.y + s * .1f; while (y < o.y + s) { drawLine(Ink.wash(0x22), Offset(o.x, y), Offset(o.x + s, y), 1f); y += s * .1f } }
                1 -> fabric(Color(hs[0].home.color), kinds[hs[0].home.id] ?: 0, o, s)
                2 -> {
                    fabric(Color(hs[0].home.color), kinds[hs[0].home.id] ?: 0, o, s, Path().apply { moveTo(o.x, o.y); lineTo(o.x + s, o.y); lineTo(o.x, o.y + s); close() })
                    fabric(Color(hs[1].home.color), kinds[hs[1].home.id] ?: 1, o, s, Path().apply { moveTo(o.x + s, o.y); lineTo(o.x + s, o.y + s); lineTo(o.x, o.y + s); close() })
                }
                else -> hs.take(4).forEachIndexed { i, h ->
                    val q = Offset(o.x + (i % 2) * s / 2, o.y + (i / 2) * s / 2)
                    fabric(Color(h.home.color), kinds[h.home.id] ?: i, o, s, Path().apply { addRect(androidx.compose.ui.geometry.Rect(q, Size(s / 2, s / 2))) })
                    if (hs.size == 3 && i == 2) fabric(Color(hs[0].home.color), kinds[hs[0].home.id] ?: 0, o, s, Path().apply { addRect(androidx.compose.ui.geometry.Rect(Offset(o.x + s / 2, o.y + s / 2), Size(s / 2, s / 2))) })
                }
            }
            // Stitches round every patch.
            drawRect(Color(0xCCFFF8E8), Offset(o.x + s * .08f, o.y + s * .08f), Size(s * .84f, s * .84f), style = Stroke(1.6f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 4f))))
            if (d == picked) drawRect(Ink.red, o, Size(s, s), style = Stroke(4f))
            if (d == end) drawCircle(Ink.red, s * .08f, Offset(o.x + s * .82f, o.y + s * .18f))
        }
    }
    Gap(10.dp)
    picked?.let { d ->
        val hs = busy[d].orEmpty()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(dayName(d) + ": " + (if (hs.isEmpty()) tr("отдых", "a day of rest") else hs.joinToString(", ") { it.home.house }), style = Ink.body, modifier = Modifier.weight(1f))
            if (d > today() - 7) Text(tr("выпуск →", "issue →"), style = Ink.h3.copy(color = Ink.red), modifier = Modifier.clickable { onIssue(d) })
        }
        Gap(8.dp)
    }
    Text(tr("Лоскутов сшито: $patches из ${end - start + 1}. Самая длинная полоса – ${count(streak, DAYS)}.", "Patches sewn: $patches of ${end - start + 1}. The longest run: ${count(streak, DAYS)}."), style = Ink.body)
    Gap(10.dp)
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        live.forEach { h ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Canvas(Modifier.size(18.dp)) { fabric(Color(h.home.color), kinds[h.home.id] ?: 0, Offset.Zero, size.width) }
                Spacer(Modifier.width(4.dp)); Text(h.home.house, style = Ink.small)
            }
        }
    }
}


/** The gazette's masthead: the reader's own village – the houses that live in it, on one printed line, with what was bought for it. */
@Composable
private fun Skyline(live: List<House>, owned: Set<String>) {
    val extras = listOf("windmill", "lamp", "well", "appletree").filter { it in owned }.take(2)
    Box(Modifier.fillMaxWidth().height(78.dp), contentAlignment = Alignment.BottomCenter) {
        Row(Modifier.fillMaxWidth().padding(bottom = 3.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
            extras.getOrNull(0)?.let { ArtIcon("decor_$it", 40.dp) }
            live.forEach { h -> ArtIcon(h.art(0), if (h.home.look == app.lingvale.data.Look.TOWER) 70.dp else 60.dp) }
            extras.getOrNull(1)?.let { ArtIcon("decor_$it", 40.dp) }
        }
        Rule(modifier = Modifier.align(Alignment.BottomCenter))
    }
}
