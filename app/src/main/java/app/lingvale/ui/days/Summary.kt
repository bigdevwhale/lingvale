package app.lingvale.ui.days

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.lingvale.data.House
import app.lingvale.domain.FIREFLIES
import app.lingvale.domain.Game
import app.lingvale.domain.HouseFacts
import app.lingvale.domain.Insights
import app.lingvale.domain.MINUTES
import app.lingvale.domain.change
import app.lingvale.domain.vs
import app.lingvale.domain.clock
import app.lingvale.domain.count
import app.lingvale.domain.dayName
import app.lingvale.domain.dayOf
import app.lingvale.domain.dayTitle
import app.lingvale.domain.mondayOf
import app.lingvale.domain.today
import app.lingvale.domain.tr
import app.lingvale.domain.weekdayShort
import app.lingvale.ui.components.Gap
import app.lingvale.ui.components.IconLine
import app.lingvale.ui.components.PaperSection
import app.lingvale.ui.components.Tile
import app.lingvale.ui.components.TileRow
import app.lingvale.ui.theme.Ink
import app.lingvale.ui.village.ArtIcon
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private fun House.icon() = "house_${home.look.name.lowercase()}_0"

/** The «Day» tab: a strip of two weeks to pick from, then the chosen day in figures, hour by hour and thing by thing. */
@Composable
fun DayTab(live: List<House>, day: Long, onDay: (Long) -> Unit) {
    Strip(live, day, onDay)
    Gap(10.dp)
    val busy = Game.busy(live, day)
    val n = live.sumOf { h -> h.snap!!.days.firstOrNull { it.d == day }?.n ?: 0 }
    val min = live.sumOf { h -> h.snap!!.days.firstOrNull { it.d == day }?.min ?: 0 }
    Row(verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f)) {
            Text(dayName(day), style = Ink.head)
            if (day >= today() - 1) Text(dayTitle(day), style = Ink.small)
        }
        ArtIcon("icon_firefly", 30.dp); Text("+${Game.dayGain(live, day)}", style = Ink.h3)
    }
    Gap(6.dp)
    TileRow {
        Tile("g_checklist", "$n", tr("дел за день", "things done"))
        Tile("g_hourglass", "$min", tr("минут за день", "minutes"))
        Tile("g_link", "${busy.size}/${live.size}", tr("домов в деле", "houses busy"))
    }
    if (day >= today() - 7) PaperSection(tr("По часам · нажмите на дом", "Hour by hour · tap a house")) { Timeline(live, day) }
    else Text(tr("Подробности по часам хранятся неделю.", "Hour by hour is kept for a week."), style = Ink.small, modifier = Modifier.padding(top = 10.dp))
}

/** Fourteen days: each a little column of the houses busy that day, tall by how much was done. */
@Composable
private fun Strip(live: List<House>, day: Long, onDay: (Long) -> Unit) {
    val days = (13 downTo 0).map { today() - it }
    val totals = days.associateWith { d -> live.map { h -> h to (h.snap!!.days.firstOrNull { it.d == d }?.n ?: 0) } }
    val top = totals.values.maxOf { l -> l.sumOf { it.second } }.coerceAtLeast(1)
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState(Int.MAX_VALUE)), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        days.forEach { d ->
            Column(Modifier.width(40.dp).clip(RoundedCornerShape(10.dp)).background(if (d == day) Color(0x1F6B4423) else Color.Transparent).clickable { onDay(d) }.padding(vertical = 5.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Canvas(Modifier.width(18.dp).height(54.dp)) {
                    var y = size.height
                    totals[d]!!.forEach { (h, n) ->
                        if (n == 0) return@forEach
                        val hh = size.height * n / top
                        drawRoundRect(Color(h.home.color), Offset(0f, y - hh), Size(size.width, hh), CornerRadius(4f)); y -= hh + 2f
                    }
                    if (y == size.height) drawCircle(Ink.rule, 3f, Offset(size.width / 2, size.height - 4f))
                }
                Text(weekdayShort(d), style = Ink.caps.copy(color = if (d == day) Ink.red else Ink.soft))
                Text("${LocalDate.ofEpochDay(d).dayOfMonth}", style = Ink.h3.copy(color = if (d == day) Ink.red else Ink.ink))
            }
        }
    }
}

/** A lane per busy house from 6 to 24 o'clock, a block per thing done; tap a lane to see its things. */
@Composable
private fun Timeline(live: List<House>, day: Long) {
    val zone = ZoneId.systemDefault()
    val busy = live.map { h -> h to h.snap!!.events.filter { dayOf(it.at) == day } }.filter { it.second.isNotEmpty() }
    var open by remember(day) { androidx.compose.runtime.mutableStateOf<String?>(null) }
    Column {
        if (busy.isEmpty()) Text(tr("В этот день было тихо.", "A quiet day."), style = Ink.body)
        busy.forEach { (h, events) ->
            val on = open == h.home.id
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(if (on) Color(0x146B4423) else Color.Transparent)
                .clickable { open = if (on) null else h.home.id }.padding(vertical = 3.dp)) {
                ArtIcon(h.icon(), 30.dp)
                Spacer(Modifier.width(6.dp))
                Canvas(Modifier.weight(1f).height(24.dp)) {
                    val w = size.width
                    drawRoundRect(Color(0x10000000), Offset(0f, 4f), Size(w, size.height - 8f), CornerRadius(8f))
                    for (hr in 9..21 step 3) { val x = w * (hr - 6) / 18f; drawLine(Color(0x22000000), Offset(x, 4f), Offset(x, size.height - 4f), 1f) }
                    events.forEach { e ->
                        val t = Instant.ofEpochMilli(e.at).atZone(zone)
                        val hour = (t.hour + t.minute / 60f).coerceIn(6f, 24f)
                        val x = w * (hour - 6) / 18f
                        val bw = (w * maxOf(e.min, 10) / (18f * 60)).coerceAtLeast(7f)
                        drawRoundRect(Color(h.home.color), Offset(x, 4f), Size(bw, size.height - 8f), CornerRadius(6f))
                    }
                }
                Text("${events.size}", style = Ink.h3, modifier = Modifier.width(26.dp), textAlign = TextAlign.End)
            }
            if (on) Column(Modifier.padding(start = 36.dp, bottom = 6.dp)) {
                events.sortedBy { it.at }.forEach { e ->
                    Row(Modifier.padding(vertical = 2.dp)) {
                        Text(clock(e.at), style = Ink.small.copy(color = Ink.ink), modifier = Modifier.width(44.dp))
                        Text(e.title.ifBlank { h.home.untitled }, style = Ink.body, modifier = Modifier.weight(1f), maxLines = 1)
                        if (e.min > 0) Text(tr("${e.min} мин", "${e.min} min"), style = Ink.small)
                    }
                }
            }
        }
        if (busy.isNotEmpty()) Row(Modifier.padding(start = 36.dp, end = 26.dp, top = 2.dp)) {
            listOf("6", "12", "18", "24").forEachIndexed { i, s -> Text(s, style = Ink.small, modifier = Modifier.weight(1f), textAlign = if (i == 3) TextAlign.End else TextAlign.Start) }
        }
    }
}

/** The «Week» tab: the week in three figures, a table of the houses by day, and the best day. */
@Composable
fun WeekTab(live: List<House>) {
    val mon = mondayOf(today())
    val t = today()
    val busyDays = (mon..t).count { Game.busy(live, it).isNotEmpty() }
    val gain = (mon..t).sumOf { Game.dayGain(live, it) }
    val facts = live.map { HouseFacts(it) }
    val n = facts.sumOf { it.week.n }; val before = facts.sumOf { it.lastWeekSoFar.n }
    Gap(4.dp)
    TileRow {
        Tile("g_calendar_small", "$busyDays/${t - mon + 1}", tr("дней с делами", "busy days"))
        Tile("g_checklist", "$n", tr("дел за неделю", "things this week"), vs(n, before, tr(" к прошлой", " vs last")), if (n == before) null else n > before)
    }
    TileRow {
        Tile("icon_firefly", "$gain", tr("светлячков за неделю", "fireflies this week"))
        Tile("g_hourglass", "${facts.sumOf { it.week.min }}", tr("минут за неделю", "minutes this week"))
    }
    PaperSection(tr("Дома по дням", "Houses by day")) {
        Column {
            Row(Modifier.padding(start = 40.dp)) {
                (0..6).forEach { Text(weekdayShort(mon + it), style = Ink.caps.copy(color = if (mon + it == t) Ink.red else Ink.soft), textAlign = TextAlign.Center, modifier = Modifier.weight(1f)) }
                Spacer(Modifier.width(44.dp))
            }
            live.forEachIndexed { i, h ->
                val by = h.snap!!.days.associateBy { it.d }
                Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    ArtIcon(h.icon(), 34.dp)
                    Spacer(Modifier.width(6.dp))
                    (0..6).forEach { k ->
                        val d = mon + k
                        val v = by[d]?.n ?: 0
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            when {
                                v > 0 -> Box(Modifier.size(26.dp).clip(CircleShape).background(Color(h.home.color)), contentAlignment = Alignment.Center) { Text("$v", style = Ink.small.copy(color = Color.White)) }
                                d <= t -> Box(Modifier.size(6.dp).clip(CircleShape).background(Ink.rule))
                            }
                        }
                    }
                    Column(Modifier.width(44.dp), horizontalAlignment = Alignment.End) {
                        Text("${facts[i].week.n}", style = Ink.h3)
                        val up = facts[i].week.n - facts[i].lastWeekSoFar.n
                        if (up != 0) ArtIcon(if (up > 0) "g_up" else "g_down", 14.dp)
                    }
                }
            }
        }
    }
    val best = (mon..t).maxByOrNull { Game.dayGain(live, it) }
    if (best != null && Game.dayGain(live, best) > 0) IconLine("g_trophy", tr("Лучший день недели – ${dayName(best).lowercase()}: ${count(Game.busy(live, best).size, app.lingvale.domain.HOUSES)} в деле, +${count(Game.dayGain(live, best), FIREFLIES)}.",
        "Best day this week: ${dayName(best)}, ${Game.busy(live, best).size} houses busy, +${Game.dayGain(live, best)} fireflies."))
}

/** The «Links» tab: how the village lives as a whole, and which houses go together. */
@Composable
fun LinksTab(live: List<House>) {
    val links = remember(live) { Insights.links(live) }
    val t = today()
    val days = (t - 29..t).map { Game.busy(live, it).size }
    val prev = (t - 59..t - 30).map { Game.busy(live, it).size }
    Gap(4.dp)
    TileRow {
        Tile("g_calendar_small", "${days.count { it > 0 }}/30", tr("дней с делами за месяц", "busy days this month"), tr("было ${prev.count { it > 0 }}", "was ${prev.count { it > 0 }}"),
            if (days.count { it > 0 } == prev.count { it > 0 }) null else days.count { it > 0 } > prev.count { it > 0 })
        Tile("g_link", "${days.count { it >= 2 }}", tr("дней, когда заняты 2+ дома", "days with 2+ houses busy"))
    }
    PaperSection(tr("Как живёт деревня", "How the village lives")) {
        Column {
            val wd = (t - 181..t).filter { Game.busy(live, it).isNotEmpty() }.groupingBy { LocalDate.ofEpochDay(it).dayOfWeek }.eachCount()
            wd.maxByOrNull { it.value }?.let { IconLine("g_trophy", tr("Самый деятельный день – ${app.lingvale.domain.dayName(it.key)}.", "The busiest weekday: ${app.lingvale.domain.dayName(it.key)}.")) }
            wd.minByOrNull { it.value }?.let { IconLine("g_moon", tr("Самый тихий – ${app.lingvale.domain.dayName(it.key)}.", "The quietest: ${app.lingvale.domain.dayName(it.key)}.")) }
            val zone = ZoneId.systemDefault()
            val hours = live.flatMap { h -> h.snap!!.events.map { Instant.ofEpochMilli(it.at).atZone(zone).hour } }
            if (hours.size >= 5) {
                IconLine("g_sunrise", tr("Утром, до полудня, – ${hours.count { it < 12 } * 100 / hours.size}% дел за неделю.", "Mornings: ${hours.count { it < 12 } * 100 / hours.size}% of the week's things."))
                IconLine("g_moon", tr("Вечером, после шести, – ${hours.count { it >= 18 } * 100 / hours.size}%.", "Evenings after six: ${hours.count { it >= 18 } * 100 / hours.size}%."))
            }
        }
    }
    PaperSection(tr("Какие дома ходят вместе", "Which houses go together")) {
        Column {
            if (links.isEmpty()) IconLine("g_magnifier", if (live.size < 2) tr("Связи появятся, когда в деревне будет хотя бы два дома.", "Links appear once there are two houses.")
                else tr("Пока мало данных: нужно хотя бы по неделе дел в двух домах за последние три месяца.", "Not enough yet: two houses need a week or so of things done in the last three months."))
            links.forEach { l ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    ArtIcon(l.a.icon(), 34.dp); ArtIcon("g_link", 22.dp); ArtIcon(l.b.icon(), 34.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(l.text, style = Ink.body, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
