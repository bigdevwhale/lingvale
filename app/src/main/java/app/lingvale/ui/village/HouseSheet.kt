package app.lingvale.ui.village

import app.lingvale.ui.art.art

import app.lingvale.ui.components.sheetMax
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import app.lingvale.container
import app.lingvale.data.House
import app.lingvale.data.PetInfo
import app.lingvale.data.Status
import app.lingvale.domain.MINUTES
import app.lingvale.domain.DAYS
import app.lingvale.domain.FIREFLIES
import app.lingvale.domain.HouseFacts
import app.lingvale.domain.Lang
import app.lingvale.domain.Tally
import app.lingvale.domain.change
import app.lingvale.domain.vs
import app.lingvale.domain.dayName
import app.lingvale.domain.dayTitle
import app.lingvale.ui.components.dotted
import app.lingvale.ui.components.IconTabs
import app.lingvale.ui.components.IconLine
import app.lingvale.ui.components.Rule
import app.lingvale.ui.components.Tile
import app.lingvale.ui.components.TileRow
import androidx.compose.runtime.mutableIntStateOf
import app.lingvale.domain.clock
import app.lingvale.domain.count
import app.lingvale.domain.dayOf
import app.lingvale.domain.mondayOf
import app.lingvale.domain.today
import app.lingvale.domain.tr
import app.lingvale.domain.weekdayShort
import app.lingvale.ui.art.Art
import app.lingvale.ui.art.Sprite
import app.lingvale.ui.components.Gap
import app.lingvale.ui.components.PaperSection
import app.lingvale.ui.components.paper
import app.lingvale.ui.components.ruled
import app.lingvale.ui.theme.Ink
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.roundToInt

fun stageName(s: Int) = when (s) {
    0 -> tr("Яйцо", "Egg"); 1 -> tr("Малыш", "Baby"); 2 -> tr("Подросток", "Youngster"); 3 -> tr("Взрослый", "Grown"); else -> tr("Мастер", "Master")
}

private fun petName(h: House, p: PetInfo) = p.name.ifBlank { h.snap!!.species(p.species)?.name.orEmpty() }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HouseSheet(h: House, onClose: () -> Unit, onDemolish: (() -> Unit)? = null) {
    ModalBottomSheet(onDismissRequest = onClose, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = Ink.paper, dragHandle = null,
        contentWindowInsets = { androidx.compose.foundation.layout.WindowInsets(0) }) {
        androidx.compose.foundation.layout.Box(Modifier.sheetMax(handle = 8.dp)) {
        HouseBody(h, onDemolish = onDemolish)
        }
    }
}

/** What's inside a house: its room, calendar, board of honour and book. */
@Composable
fun HouseBody(h: House, modifier: Modifier = Modifier, startTab: Int = 0, onDemolish: (() -> Unit)? = null) {
    val ctx = LocalContext.current
    run {
        Column(modifier.fillMaxWidth().paper().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 18.dp)) {
            val snap = h.snap
            if (h.live && snap != null && h.pkg != null) {
                Room(h)
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        ArtIcon(h.art(0), 52.dp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(h.home.house, style = Ink.head)
                            Text("${h.home.app} · ${h.home.about.lowercase()}", style = Ink.small)
                        }
                    }
                    Gap(10.dp)
                    var tab by remember { mutableIntStateOf(startTab) }
                    IconTabs(listOf("g_checklist" to tr("Обзор", "Overview"), "g_crown" to tr("Жильцы", "Residents"), "g_book" to tr("История", "History")), tab, { tab = it })
                    Rule(color = Ink.rule, modifier = Modifier.padding(top = 2.dp, bottom = 6.dp))
                    when (tab) {
                        0 -> Overview(h)
                        1 -> Residents(h)
                        else -> History(h)
                    }
                    Gap(16.dp)
                    Door(tr("Войти в ${h.home.app}", "Step into ${h.home.app}")) { ctx.container.bridge.launch(h.pkg)?.let { ctx.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
                    if (onDemolish != null) {
                        var ask by remember { mutableStateOf(false) }
                        Text(if (ask) tr("Точно снести? Нажмите ещё раз. Данные в ${h.home.app} не пострадают, дом можно построить снова.", "Sure? Tap again. ${h.home.app} keeps its data; you can build the house again.")
                            else tr("Снести дом", "Take the house down"), style = Ink.small.copy(color = Ink.red), textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().clickable { if (ask) onDemolish() else ask = true }.padding(vertical = 12.dp))
                    }
                }
            } else Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ArtIcon(if (h.status == Status.MISSING) "plot_0" else "plot_1", 96.dp)
                    Spacer(Modifier.width(10.dp))
                    Column { Text(h.home.house, style = Ink.head); Text(h.home.app + " · " + h.home.about.lowercase(), style = Ink.small) }
                }
                Gap(12.dp)
                Text(
                    when (h.status) {
                        Status.OLD -> tr("Вещи уже в ящиках: ${h.home.app} стоит на телефоне, но в этой версии ещё нет дороги в деревню. Обновите приложение – и дом встанет здесь вместе с жильцами.",
                            "The things are already in crates: ${h.home.app} is on the phone, but this version has no road to the village yet. Update it and the house moves in with its residents.")
                        Status.LOCKED -> tr("${h.home.app} и деревня собраны разными ключами, поэтому жильцы не могут переехать. Поставьте обе программы из одного места.",
                            "${h.home.app} and the village are signed with different keys, so the residents can't move in. Install both from the same place.")
                        else -> tr("Участок ждёт хозяев. Здесь встанет ${h.home.house.lowercase()}, когда на телефоне появится ${h.home.app}.",
                            "The plot is waiting. The ${h.home.house.lowercase()} will stand here once ${h.home.app} is on the phone.")
                    }, style = Ink.hand)
                Gap(16.dp)
                if (h.pkg != null) Door(tr("Открыть ${h.home.app}", "Open ${h.home.app}")) { ctx.container.bridge.launch(h.pkg)?.let { ctx.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
                else Door(tr("Узнать про ${h.home.app}", "About ${h.home.app}")) { runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(h.home.page)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
            }
        }
    }
}

/** A door-coloured button with a serif label. */
@Composable
private fun Door(text: String, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF6B4423)).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(text, style = Ink.h3.copy(color = Color(0xFFFFF1D6)))
    }
}

/** The app's own room with its team; a tapped resident hops and says something. */
@Composable
private fun Room(h: House) {
    val pkg = h.pkg!!; val snap = h.snap!!
    val art = LocalContext.current.container.art
    val bg = if (snap.scene.isNotEmpty()) art.get(pkg, snap.scene) else null
    var said by remember { mutableStateOf<Pair<Int, String>?>(null) }
    LaunchedEffect(said) { if (said != null) { delay(3200); said = null } }
    // A tall room (Keyling's desk) is shown no taller than 4:3 and cropped to its lower part, where the residents stand.
    val ratio = if (bg != null) bg.width.toFloat() / bg.height else 1.5f
    BoxWithConstraints(Modifier.fillMaxWidth().aspectRatio(maxOf(ratio, 1.7f)).background(Ink.paper2)) {
        if (bg != null) Canvas(Modifier.fillMaxSize()) {
            val drawnH = size.width / ratio
            val srcH = (bg.height * (size.height / drawnH)).roundToInt().coerceAtMost(bg.height)
            drawImage(bg, IntOffset(0, bg.height - srcH), IntSize(bg.width, srcH), IntOffset.Zero, IntSize(size.width.roundToInt(), size.height.roundToInt()), filterQuality = FilterQuality.Low)
        }
        val w = maxWidth; val hgt = maxHeight
        val spots = listOf(.5f to .46f, .2f to .32f, .8f to .32f)
        snap.walkers.take(3).forEachIndexed { i, p ->
            val (cx, sz) = spots[i]
            val s = minOf(hgt * sz * 1.15f, w * sz * .6f)
            val hop = remember { Animatable(0f) }
            val scope = rememberCoroutineScope()
            Box(Modifier.offset(x = w * cx - s / 2, y = hgt * .95f - s).graphicsLayer { translationY = -hop.value }
                .clickable(remember { MutableInteractionSource() }, null) {
                    scope.launch { hop.snapTo(0f); hop.animateTo(40f, spring(stiffness = Spring.StiffnessMedium)); hop.animateTo(0f, spring(dampingRatio = .4f)) }
                    val sp = snap.species(p.species)
                    val lines = listOf(
                        tr("${petName(h, p)} машет вам", "${petName(h, p)} waves at you"),
                        tr("${petName(h, p)}: ${stageName(p.stage).lowercase()}, ${sp?.name?.lowercase().orEmpty()}", "${petName(h, p)}: ${stageName(p.stage).lowercase()} ${sp?.name?.lowercase().orEmpty()}"),
                        tr("${petName(h, p)} зевает и потягивается", "${petName(h, p)} yawns and stretches"),
                        sp?.about?.let { "${petName(h, p)}: $it" } ?: tr("${petName(h, p)} улыбается", "${petName(h, p)} smiles"),
                    )
                    said = i to lines[(System.currentTimeMillis() / 1000 % lines.size).toInt()]
                }) {
                Sprite(pkg, p.species, p.stage, s, shiny = p.shiny)
            }
        }
        said?.let { (i, text) ->
            val (cx, sz) = spots[i]
            Text(text, style = Ink.body.copy(color = Ink.ink), textAlign = TextAlign.Center,
                modifier = Modifier.offset(x = (w * cx - w * .28f).coerceAtLeast(6.dp), y = hgt * (.95f - sz) - 34.dp).widthIn(max = w * .56f)
                    .clip(RoundedCornerShape(12.dp)).background(Color(0xF2FFFBEF)).padding(horizontal = 10.dp, vertical = 6.dp))
        }
    }
}

/** This week on a paper calendar hanging on a nail: a wax seal on every day something was done. */
@Composable
private fun Calendar(h: House) {
    val mon = mondayOf(today())
    val by = h.snap!!.days.associateBy { it.d }
    Column(Modifier.fillMaxWidth().padding(horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(12.dp).clip(CircleShape).background(Color(0xFF8C8C94)))
        Canvas(Modifier.width(120.dp).height(14.dp)) {
            drawLine(Color(0xFF6B4A30), androidx.compose.ui.geometry.Offset(size.width / 2, 0f), androidx.compose.ui.geometry.Offset(10f, size.height), 3f)
            drawLine(Color(0xFF6B4A30), androidx.compose.ui.geometry.Offset(size.width / 2, 0f), androidx.compose.ui.geometry.Offset(size.width - 10f, size.height), 3f)
        }
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(Color(0xFFFFFBF0))) {
            Box(Modifier.fillMaxWidth().height(30.dp).background(Color(0xFFB8322A)), contentAlignment = Alignment.Center) {
                Text(LocalDate.ofEpochDay(mon).let { tr("неделя с ${it.dayOfMonth}-го", "the week of the ${it.dayOfMonth}th") }.uppercase(), style = Ink.caps.copy(color = Color(0xFFFFE9D6)))
            }
            Box(Modifier.fillMaxWidth().height(3.dp).background(Color(0xFF7E1E18)))
            Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 10.dp)) {
                (0..6).forEach { k ->
                    val d = mon + k
                    val di = by[d]
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(weekdayShort(d), style = Ink.caps.copy(color = if (k >= 5) Ink.red else Ink.soft))
                        Text("${LocalDate.ofEpochDay(d).dayOfMonth}", style = Ink.head.copy(color = if (d == today()) Ink.red else Ink.ink))
                        Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                            if (di != null) ArtIcon("seal", 32.dp) else if (d < today()) Text("–", style = Ink.h3.copy(color = Ink.rule))
                        }
                        Text(if (di != null && h.home.minutes && di.min > 0) "${di.min}′" else " ", style = Ink.hand.copy(fontSize = androidx.compose.ui.unit.TextUnit(17f, androidx.compose.ui.unit.TextUnitType.Sp)))
                    }
                }
            }
            val days = (0..6).count { by[mon + it] != null }
            val mins = (0..6).sumOf { by[mon + it]?.min ?: 0 }
            Text(tr("$days из 7 дней с печатью", "$days of 7 days sealed") + if (h.home.minutes && mins > 0) " · " + count(mins, MINUTES) else "",
                style = Ink.hand, modifier = Modifier.padding(start = 14.dp, bottom = 10.dp))
        }
    }
}

/** The first tab: this week at a glance, the calendar, and when things usually happen. */
@Composable
private fun Overview(h: House) {
    val f = remember(h) { HouseFacts(h) }
    val w = h.home.what
    val diff = f.week.n - f.lastWeekSoFar.n
    Column {
        TileRow {
            Tile("g_checklist", "${f.week.n}", tr("${plural(f.week.n, w)} на этой неделе", "${plural(f.week.n, w)} this week"),
                vs(f.week.n, f.lastWeekSoFar.n, tr(" к прошлой", " vs last")), if (diff == 0) null else diff > 0)
            if (h.home.minutes) Tile("g_hourglass", "${f.week.min}", tr("минут на этой неделе", "minutes this week"),
                vs(f.week.min, f.lastWeekSoFar.min, tr(" к прошлой", " vs last")), if (f.week.min == f.lastWeekSoFar.min) null else f.week.min > f.lastWeekSoFar.min)
            else Tile("icon_firefly", "${f.fireflies}", tr("светлячков принёс деревне", "fireflies for the village"))
        }
        TileRow {
            Tile("g_flame", if (f.streak > 0) "${f.streak}" else "0", tr("${plural(f.streak, DAYS)} подряд", "${plural(f.streak, DAYS)} in a row"), tr("рекорд – ${f.bestStreak}", "best: ${f.bestStreak}"))
            Tile("g_calendar_small", "${f.month.days}/30", tr("дней с делами за месяц", "busy days this month"),
                vs(f.month.days, f.prevMonth.days, tr(" к прошлому", " vs before")), if (f.month.days == f.prevMonth.days) null else f.month.days > f.prevMonth.days)
        }
        PaperSection(tr("Эта неделя", "This week")) { WeekSeals(h) }
    }
}

/** The third tab: the weeks gone by, the months compared, everything since the start, and the book. */
@Composable
private fun History(h: House) {
    val f = remember(h) { HouseFacts(h) }
    Column {
        PaperSection(tr("Ритм дома", "The house's rhythm")) {
            Column {
                f.partOfDay?.let { (m, a, e) ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        listOf(Triple("g_sunrise", tr("утро", "morning"), m), Triple("g_sun", tr("день", "afternoon"), a), Triple("g_moon", tr("вечер", "evening"), e)).forEach { (ic, label, pct) ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                ArtIcon(ic, 40.dp, Modifier.graphicsLayer { alpha = .35f + .65f * pct / 100f })
                                Text("$pct%", style = Ink.h3)
                                Text(label, style = Ink.small)
                            }
                        }
                    }
                    Gap(8.dp)
                }
                f.favDay?.let { IconLine("g_calendar_small", tr("Чаще всего дела бывают в ${dayName(it, accusative = true)}.", "Things happen most often on ${dayName(it)}.")) }
                if (f.perDay > 0) IconLine("g_checklist", tr("В обычный день с делами – ${fmt1(f.perDay)}", "A usual busy day: ${fmt1(f.perDay)}") + " " + plural(2, h.home.what) +
                    if (h.home.minutes && f.minPerDay > 0) tr(", около ${count(f.minPerDay, MINUTES)}.", ", about ${count(f.minPerDay, MINUTES)}.") else ".")
                if (h.home.minutes) IconLine("icon_firefly", tr("Дом принёс деревне ${count(f.fireflies, FIREFLIES)}.", "The house has brought the village ${count(f.fireflies, FIREFLIES)}."))
            }
        }

        PaperSection(tr("Двенадцать недель", "Twelve weeks")) { Weeks(h) }
        PaperSection(tr("Месяц к месяцу", "Month on month")) {
            Column {
                TileRow {
                    Tile("g_checklist", "${f.month.n}", tr("за 30 дней", "in 30 days"), tr("было ${f.prevMonth.n}", "was ${f.prevMonth.n}"), if (f.month.n == f.prevMonth.n) null else f.month.n > f.prevMonth.n)
                    if (h.home.minutes) Tile("g_hourglass", "${f.month.min}", tr("минут за 30 дней", "minutes in 30 days"), tr("было ${f.prevMonth.min}", "was ${f.prevMonth.min}"), if (f.month.min == f.prevMonth.min) null else f.month.min > f.prevMonth.min)
                }
            }
        }
        f.first?.let { first ->
            PaperSection(tr("С самого начала", "Since the start")) {
                Column {
                    IconLine("g_calendar_small", tr("В деле с ${dayTitle(first)} – дней с делами: ${f.all.days}.", "At it since ${dayTitle(first)}: ${f.all.days} busy days."))
                    IconLine("g_trophy", count(f.all.n, h.home.what) + if (h.home.minutes && f.all.min > 0) tr(" и ${count(f.all.min, MINUTES)}", " and ${count(f.all.min, MINUTES)}") else "")
                    IconLine("g_flame", tr("Самая длинная серия – ${count(f.bestStreak, DAYS)}.", "The longest streak: ${count(f.bestStreak, DAYS)}."))
                }
            }
        }
        PaperSection(tr("Книга жильцов", "The residents' book")) { Book(h) }
    }
}

/** A countable word without its number: «тренировок», «тренировки». */
private fun plural(n: Int, w: app.lingvale.domain.Words) = count(n, w).substringAfter(' ')


private fun fmt1(v: Double) = if (Lang.ru) "%.1f".format(java.util.Locale.US, v).replace('.', ',') else "%.1f".format(java.util.Locale.US, v)

/** Twelve weeks in ink: a column a week, this week in red. */
@Composable
private fun Weeks(h: House) {
    val f = remember(h) { HouseFacts(h) }
    val vals = f.weeks.map { if (h.home.minutes) it.min else it.n }
    val top = vals.maxOrNull()?.coerceAtLeast(1) ?: 1
    val avg = vals.dropLast(1).filter { it > 0 }.let { if (it.isEmpty()) 0 else it.sum() / it.size }
    Column {
        Text((if (h.home.minutes) tr("Минут в неделю", "Minutes a week") else tr("Дел в неделю", "Things a week")) + tr(" · обычно $avg, сейчас ${vals.last()}", " · usually $avg, now ${vals.last()}"), style = Ink.small)
        Gap(6.dp)
        Canvas(Modifier.fillMaxWidth().height(110.dp)) {
            val n = vals.size; val cw = size.width / n; val base = size.height - 2f
            drawLine(Ink.ink, androidx.compose.ui.geometry.Offset(0f, base), androidx.compose.ui.geometry.Offset(size.width, base), 2f)
            if (avg > 0) {
                val y = base - (size.height - 18f) * avg / top
                drawLine(Ink.rule, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(size.width, y), 2f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
            }
            vals.forEachIndexed { i, v ->
                val hh = (size.height - 18f) * v / top
                val x = i * cw + cw * .2f
                val c = if (i == n - 1) Ink.red else Color(h.home.color)
                drawRect(c, androidx.compose.ui.geometry.Offset(x, base - hh), androidx.compose.ui.geometry.Size(cw * .6f, hh))
                // hatching, like a pen drawing
                var k = 0f
                while (k < hh) { drawLine(Color(0x33000000), androidx.compose.ui.geometry.Offset(x, base - k), androidx.compose.ui.geometry.Offset(x + cw * .6f, base - k - cw * .3f), 1.2f); k += 6f }
            }
        }
        Row {
            Text(tr("12 недель назад", "12 weeks ago"), style = Ink.small, modifier = Modifier.weight(1f))
            Text(tr("эта неделя", "this week"), style = Ink.small.copy(color = Ink.red))
        }
    }
}

/** Everyone who lives in the house: the team first, then the rest, then the eggs. */
@Composable
private fun Residents(h: House) {
    val snap = h.snap!!
    val team = snap.team.take(3)
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
            team.forEach { p ->
                Column(Modifier.width(108.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Sprite(h.pkg!!, p.species, p.stage, if (p.slot == 0) 96.dp else 80.dp, shiny = p.shiny)
                    Text(petName(h, p), style = Ink.h3, maxLines = 1)
                    Text((if (p.slot == 0) tr("вожак · ", "leader · ") else "") + stageName(p.stage).lowercase(), style = Ink.small)
                    if (p.stage < 4) Text(tr("до роста ${(p.progress * 100).toInt()}%", "${(p.progress * 100).toInt()}% to grow"), style = Ink.hand.copy(fontSize = androidx.compose.ui.unit.TextUnit(18f, androidx.compose.ui.unit.TextUnitType.Sp)))
                }
            }
        }
        val rest = snap.pets.filter { p -> team.none { it.id == p.id } }.sortedWith(compareBy<PetInfo> { it.egg }.thenByDescending { it.stage })
        if (rest.isNotEmpty()) {
            Gap(12.dp)
            Text(tr("Остальные жильцы", "The other residents"), style = Ink.caps)
            Gap(6.dp)
            rest.chunked(4).forEach { row ->
                Row(Modifier.fillMaxWidth()) {
                    row.forEach { p ->
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Sprite(h.pkg!!, p.species, p.stage, 54.dp, shiny = p.shiny)
                            Text(if (p.egg) tr("яйцо", "egg") else petName(h, p), style = Ink.small.copy(color = Ink.ink), maxLines = 1)
                            Text(if (p.egg) "${(p.progress * 100).toInt()}%" else stageName(p.stage).lowercase(), style = Ink.small, maxLines = 1)
                        }
                    }
                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                }
                Gap(6.dp)
            }
        }
        val f = remember(h) { HouseFacts(h) }
        f.nextToGrow?.let { p ->
            Gap(4.dp)
            Text(tr("Ближе всех к росту – ${petName(h, p)}: ${(p.progress * 100).toInt()}% до стадии «${stageName(p.stage + 1).lowercase()}».",
                "Closest to growing: ${petName(h, p)}, ${(p.progress * 100).toInt()}% of the way to ${stageName(p.stage + 1).lowercase()}."), style = Ink.hand)
        }
    }
}

/** The last week of the house written into its book by hand. */
@Composable
private fun Book(h: House) {
    val events = h.snap!!.events.sortedByDescending { it.at }.take(9)
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(Color(0xFFFBF6EA)).ruled(28.dp).padding(start = 36.dp, end = 10.dp, top = 4.dp, bottom = 8.dp)) {
        if (events.isEmpty()) Text(tr("Страница пока чистая.", "The page is still blank."), style = Ink.hand, modifier = Modifier.height(28.dp))
        events.forEach { e ->
            val title = e.title.ifBlank { h.home.untitled }
            Text("${weekdayShort(dayOf(e.at))}, ${clock(e.at)} – $title" + if (e.min > 0) tr(", ${e.min} мин", ", ${e.min} min") else "",
                style = Ink.hand, maxLines = 1, modifier = Modifier.height(28.dp))
        }
    }
}


/** The week in one row: a wax seal on each day something was done, the minutes under it. */
@Composable
private fun WeekSeals(h: House) {
    val mon = mondayOf(today())
    val by = h.snap!!.days.associateBy { it.d }
    Row(Modifier.fillMaxWidth()) {
        (0..6).forEach { k ->
            val d = mon + k
            val di = by[d]
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(weekdayShort(d), style = Ink.caps.copy(color = if (d == today()) Ink.red else Ink.soft))
                Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                    if (di != null) ArtIcon("seal", 32.dp) else Box(Modifier.size(8.dp).clip(CircleShape).background(if (d <= today()) Ink.rule else Color(0x14000000)))
                }
                Text(if (di != null) (if (h.home.minutes && di.min > 0) "${di.min}′" else "${di.n}") else " ", style = Ink.small)
            }
        }
    }
}
