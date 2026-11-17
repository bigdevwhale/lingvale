package app.lingvale.ui.village

import app.lingvale.ui.art.art

import app.lingvale.ui.components.sheetMax
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.lingvale.ui.art.MAP_RATIO
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import app.lingvale.ui.components.PaperSection
import app.lingvale.ui.components.paper
import app.lingvale.ui.components.ruled
import app.lingvale.ui.theme.Ink
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.lingvale.container
import app.lingvale.data.House
import app.lingvale.data.Status
import app.lingvale.domain.Game
import app.lingvale.domain.MINUTES
import app.lingvale.domain.count
import app.lingvale.domain.fmt
import app.lingvale.domain.mondayOf
import app.lingvale.domain.today
import app.lingvale.domain.tr
import app.lingvale.ui.art.Art
import app.lingvale.ui.art.Sprite
import app.lingvale.ui.art.VillageMap
import app.lingvale.ui.components.Card
import app.lingvale.ui.components.Dot
import app.lingvale.ui.components.Gap
import app.lingvale.ui.components.Meter
import app.lingvale.ui.components.Sec
import app.lingvale.ui.theme.C
import app.lingvale.ui.theme.T

/** What a house did from [from] on: things done and minutes. */
fun House.since(from: Long): Pair<Int, Int> {
    val d = snap?.days?.filter { it.d >= from } ?: return 0 to 0
    return d.sumOf { it.n } to d.sumOf { it.min }
}

/** A picture from the village's own art, as an icon. */
@Composable
fun ArtIcon(name: String, size: Dp, modifier: Modifier = Modifier) {
    val b = Art.get(LocalContext.current, name) ?: return
    if (name == "g_moon") Box(modifier.size(size).clip(CircleShape).background(Color(0xFF3B4A78)), contentAlignment = Alignment.Center) {
        Image(remember(b) { BitmapPainter(b, filterQuality = FilterQuality.Low) }, null, Modifier.size(size * .78f))
    } else Image(remember(b) { BitmapPainter(b, filterQuality = FilterQuality.Low) }, null, modifier.size(size))
}

@Composable
fun VillageScreen(houses: List<House>, onHouse: (House) -> Unit, onSettings: () -> Unit, onShop: () -> Unit, onBuild: (() -> Unit)? = null) {
    val c = LocalContext.current.container
    val pr by c.prefs.progress.collectAsState()
    val live = houses.filter { it.live }
    val total = Game.gathered(live, pr.start) + pr.bonus
    val balance = total - pr.spent
    val level = Game.level(total); val next = Game.next(total)
    val chats = remember(houses) { Game.chat(live) }
    val fair = Game.fair(live)
    var sheet by remember { mutableStateOf<String?>(null) }
    val style by c.prefs.style.collectAsState()
    val achs = remember(houses, total, pr.owned, pr.claimed.size) { app.lingvale.domain.Achievements.all(live, total, Game.decor.count { it.id in pr.owned }, pr.claimed.count { !it.startsWith("ach:") }) }
    // Reached once, kept for good: some achievements stand on last week's events only.
    val reached = achs.filter { it.done && it.id !in pr.got }.map { it.id }
    androidx.compose.runtime.LaunchedEffect(reached) { if (reached.isNotEmpty()) c.prefs.update { it.copy(got = it.got + reached) } }
    val toClaim = achs.count { (it.done || it.id in pr.got) && "ach:${it.id}" !in pr.claimed }
    // One screen: the map takes what's left between the header and the three buttons; details open in sheets.
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF6FA8C8), Color(0xFF8FC0D8), Color(0xFF8DBE6C))))) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(tr("${level.name} Lingvale", "Lingvale ${level.name.lowercase()}"), style = T.h2.copy(color = Color.White, shadow = androidx.compose.ui.graphics.Shadow(Color(0x66000000), androidx.compose.ui.geometry.Offset(0f, 2f), 4f)))
                if (next != null) {
                    Box(Modifier.padding(top = 3.dp, bottom = 2.dp).width(150.dp).height(5.dp).clip(RoundedCornerShape(3.dp)).background(Color(0x55FFFFFF))) {
                        Box(Modifier.fillMaxWidth(((total - level.from).toFloat() / (next.from - level.from)).coerceIn(0f, 1f)).height(5.dp).background(Color(0xFFFFE08A)))
                    }
                    Text(tr("до уровня «${next.name}» – ${next.from - total}", "${next.from - total} to ${next.name.lowercase()}"), style = T.tiny.copy(color = Color(0xDDFFFFFF)))
                }
            }
            Row(Modifier.clip(RoundedCornerShape(50)).background(Color(0xCC1E2418)).clickable(onClick = onShop).padding(start = 6.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically) {
                ArtIcon("icon_firefly", 26.dp)
                Text(fmt(balance), style = T.numS.copy(color = Color(0xFFFFE08A)))
            }
            Spacer(Modifier.width(6.dp))
            Box(Modifier.size(36.dp)) {
                Box(Modifier.size(36.dp).clip(CircleShape).background(Color(0xCC1E2418)).clickable { sheet = "achievements" }, contentAlignment = Alignment.Center) { ArtIcon("g_trophy", 24.dp) }
                if (toClaim > 0) Text("$toClaim", style = T.tiny.copy(color = Color.White), modifier = Modifier.align(Alignment.TopEnd).clip(CircleShape).background(Color(0xFFC0392B)).padding(horizontal = 5.dp))
            }
            Spacer(Modifier.width(6.dp))
            Box(Modifier.size(36.dp).clip(CircleShape).background(Color(0xCC1E2418)).clickable(onClick = onSettings), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Settings, tr("Настройки", "Settings"), tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            val w = minOf(maxWidth, maxHeight * MAP_RATIO)
            Box(Modifier.width(w)) { VillageMap(houses, c.art, pr.owned, fair, chats, onHouse, onBuild = onBuild, style = style) }
        }
        if (live.isEmpty()) {
            Text(if (onBuild != null) tr("Долина пока пустая. Нажмите на участок «+ Построить» и выберите первый дом.", "The valley is empty. Tap the «+ Build» plot and pick the first house.")
                else tr("Долина пока пустая: дома можно построить, когда на телефоне появятся приложения семейства новой версии.", "The valley is empty: houses can be built once the family's apps are on the phone."),
                style = Ink.lead, modifier = Modifier.fillMaxWidth().paper().padding(16.dp))
        } else {
            val gain = Game.dayGain(live, today())
            val qs = remember(live) { Game.quests(live) }
            val week = mondayOf(today())
            val ready = qs.count { it.done && "$week:${it.id}" !in pr.claimed }
            Row(Modifier.fillMaxWidth().paper().padding(horizontal = 10.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Action("icon_jar", if (gain > 0) "+$gain" else "0", tr("сегодня", "today")) { sheet = "today" }
                Action("icon_scroll", "${qs.count { "$week:${it.id}" in pr.claimed }}/${qs.size}", if (ready > 0) tr("забрать!", "claim!") else tr("задания", "tasks"), alert = ready > 0) { sheet = "quests" }
                Action("decor_well", fmt(balance), tr("лавка", "shop")) { onShop() }
            }
        }
    }
    when (sheet) {
        "today" -> PaperSheet({ sheet = null }) {
            Today(live, fair)
            if ("postbox" in pr.owned) Letter(live)
            if ("board" in pr.owned) Board(live)
            if ("postbox" !in pr.owned || "board" !in pr.owned) Text(tr("В лавке есть доска объявлений и почтовый ящик: с ними здесь появятся новости деревни и письмо о прошлой неделе.",
                "The shop has a notice board and a post box: with them, village news and a letter about last week appear here."), style = Ink.small, modifier = Modifier.padding(top = 12.dp))
        }
        "quests" -> PaperSheet({ sheet = null }) { Quests(live) }
        "achievements" -> PaperSheet({ sheet = null }) { AchievementList(achs) }
    }
}

/** One of the three buttons under the map: a picture, a figure, a word. */
@Composable
private fun RowScope.Action(icon: String, value: String, label: String, alert: Boolean = false, onClick: () -> Unit) {
    Row(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(if (alert) Color(0x33A8322A) else Color(0x14000000)).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically) {
        ArtIcon(icon, 34.dp)
        Spacer(Modifier.width(6.dp))
        Column {
            Text(value, style = Ink.h3, maxLines = 1)
            Text(label, style = Ink.small.copy(color = if (alert) Ink.red else Ink.soft), maxLines = 1)
        }
    }
}

/** A paper sheet from the bottom. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PaperSheet(onClose: () -> Unit, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    androidx.compose.material3.ModalBottomSheet(onDismissRequest = onClose, sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = Ink.paper, contentWindowInsets = { androidx.compose.foundation.layout.WindowInsets(0) }) {
        androidx.compose.foundation.layout.Box(Modifier.sheetMax()) {
        Column(Modifier.fillMaxWidth().paper().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).navigationBarsPadding().padding(bottom = 18.dp), content = content)
        }
    }
}

@Composable
private fun Today(live: List<House>, fair: Boolean) {
    val gain = Game.dayGain(live, today())
    val busy = Game.busy(live, today())
    PaperSection(tr("Сегодня в долине", "Today in the valley")) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ArtIcon("icon_jar", 64.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (gain > 0) "+" + count(gain, app.lingvale.domain.FIREFLIES) else tr("Банка пока пуста", "The jar is empty so far"), style = Ink.head)
                    Text(
                        when {
                            fair -> tr("Ярмарка! Заняты ${busy.size} дома – светлячки летят тучей.", "A fair! ${busy.size} houses busy – the fireflies pour in.")
                            busy.size == 1 -> tr("Светится ${busy[0].home.house.lowercase()}. Займётся второй дом – будет ещё +6.", "The ${busy[0].home.house.lowercase()} is lit. A second busy house brings +6 more.")
                            busy.isEmpty() -> tr("Светлячки прилетают за любым делом в приложениях семейства.", "Fireflies come for anything done in the family's apps.")
                            else -> tr("Заняты ${busy.size} дома. Третий – и начнётся ярмарка.", "${busy.size} houses busy. A third one starts a fair.")
                        }, style = Ink.hand)
                }
            }
            Gap(8.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                live.forEach { h -> ArtIcon(h.art(if (h in busy) 1 else 0), 44.dp, Modifier.alpha(if (h in busy) 1f else .45f)) }
            }
        }
    }
}

/** This week's tasks; tap one for how to do it ([openAll] shows every hint, for the screen renders). */
@Composable
internal fun Quests(live: List<House>, openAll: Boolean = false) {
    val c = LocalContext.current.container
    val pr by c.prefs.progress.collectAsState()
    val week = mondayOf(today())
    val qs = remember(live) { Game.quests(live) }
    var open by remember { mutableStateOf<String?>(null) }
    PaperSection(tr("Задания недели", "This week's tasks")) {
        Column {
            Text(tr("Считаются дела в приложениях, чьи дома стоят в деревне, с понедельника по воскресенье. Нажмите на задание – подскажу, как его выполнить.",
                "What counts is what you do in the apps whose houses stand in the village, Monday to Sunday. Tap a task to see how to do it."), style = Ink.small)
            qs.forEach { q ->
                val key = "$week:${q.id}"
                val claimed = key in pr.claimed
                Row(Modifier.fillMaxWidth().clickable { open = if (open == q.id) null else q.id }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    ArtIcon("icon_scroll", 40.dp, Modifier.alpha(if (claimed) .5f else 1f))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(q.title, style = Ink.h3.copy(color = if (claimed) Ink.soft else Ink.ink))
                        Text(tr("${minOf(q.have, q.goal)} из ${q.goal} · награда ${q.reward}", "${minOf(q.have, q.goal)} of ${q.goal} · reward ${q.reward}"), style = Ink.hand.copy(color = if (claimed) Ink.rule else Ink.blue))
                        if (openAll || open == q.id) Text(q.hint, style = Ink.small, modifier = Modifier.padding(top = 4.dp))
                    }
                    when {
                        claimed -> ArtIcon("postmark", 46.dp, Modifier.alpha(.7f))
                        q.done -> Column(Modifier.clickable { c.prefs.update { it.copy(bonus = it.bonus + q.reward, claimed = it.claimed + key) } }, horizontalAlignment = Alignment.CenterHorizontally) {
                            ArtIcon("seal", 40.dp)
                            Text(tr("забрать", "claim"), style = Ink.hand.copy(color = Ink.red))
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}

/** Every achievement by section: reached ones in colour with their reward to claim, the rest greyed with how far along they are. */
@Composable
internal fun AchievementList(achs: List<app.lingvale.domain.Ach>) {
    val c = LocalContext.current.container
    val pr by c.prefs.progress.collectAsState()
    fun got(a: app.lingvale.domain.Ach) = a.done || a.id in pr.got
    Text(tr("Достижения", "Achievements"), style = Ink.head)
    Text(tr("Получено ${achs.count(::got)} из ${achs.size}. За каждое – светлячки в банку.", "${achs.count(::got)} of ${achs.size} reached. Each one brings fireflies."), style = Ink.small)
    app.lingvale.domain.Achievements.groups.forEachIndexed { g, title ->
        val list = achs.filter { it.group == g }.sortedBy { if (got(it)) 0 else 1 }
        if (list.isEmpty()) return@forEachIndexed
        PaperSection("$title · ${list.count(::got)}/${list.size}") {
            Column {
                list.forEach { a ->
                    val key = "ach:${a.id}"
                    val claimed = key in pr.claimed
                    val ok = got(a)
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        ArtIcon(a.icon, 38.dp, Modifier.alpha(if (ok) 1f else .3f))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(a.title, style = Ink.h3.copy(color = if (ok) Ink.ink else Ink.soft))
                            Text(a.about, style = Ink.small)
                            if (!ok && a.goal > 1) {
                                Meter(a.have.toFloat() / a.goal, Ink.blue, Modifier.padding(top = 3.dp).fillMaxWidth(.8f))
                                Text("${fmt(minOf(a.have, a.goal))} / ${fmt(a.goal)}", style = Ink.small.copy(color = Ink.soft))
                            }
                        }
                        when {
                            claimed -> ArtIcon("postmark", 40.dp, Modifier.alpha(.7f))
                            ok -> Column(Modifier.clickable { c.prefs.update { if (key in it.claimed) it else it.copy(bonus = it.bonus + a.reward, claimed = it.claimed + key) } },
                                horizontalAlignment = Alignment.CenterHorizontally) {
                                ArtIcon("seal", 36.dp)
                                Text("+${a.reward}", style = Ink.hand.copy(color = Ink.red))
                            }
                            else -> Row(verticalAlignment = Alignment.CenterVertically) {
                                ArtIcon("icon_firefly", 20.dp, Modifier.alpha(.5f))
                                Text("${a.reward}", style = Ink.small.copy(color = Ink.soft))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Board(live: List<House>) {
    PaperSection(tr("Доска объявлений", "Notice board")) {
        Column { Game.news(live).forEach { line -> Text("· $line", style = Ink.body, modifier = Modifier.padding(vertical = 2.dp)) } }
    }
}

@Composable
private fun Letter(live: List<House>) {
    PaperSection(tr("Письмо о прошлой неделе", "A letter about last week")) {
        Text(Game.letter(live), style = Ink.hand, modifier = Modifier.fillMaxWidth().background(Color(0xFFFBF6EA)).ruled(24.dp).padding(start = 36.dp, end = 10.dp, top = 2.dp, bottom = 6.dp))
    }
}


/** The building sheet: the family's houses that can go up now, and the ones waiting for their app. */
@Composable
fun BuildSheet(all: List<House>, built: List<String>, onBuild: (House) -> Unit, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val can = all.filter { it.live && it.home.id !in built }
    val later = all.filter { !it.live && it.home.id !in built }
    PaperSheet(onClose) {
        Text(tr("Что построить?", "What to build?"), style = Ink.head)
        Text(tr("Дом встаёт на свободную поляну, и в него сразу переезжают питомцы из приложения.", "The house goes up on a free clearing and the app's pets move straight in."), style = Ink.small)
        Gap(10.dp)
        if (can.isEmpty()) Text(tr("Все доступные дома уже стоят в деревне.", "Every house you can build is already up."), style = Ink.body)
        can.forEach { h ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                ArtIcon(h.art(0), 64.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(h.home.house, style = Ink.h3)
                    Text("${h.home.app} · ${count(h.snap!!.hatched.size, app.lingvale.domain.PETS)}", style = Ink.small)
                }
                Text(tr("Построить", "Build"), style = Ink.h3.copy(color = Color(0xFFFFF1D6)),
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Color(0xFF6B4423)).clickable { onBuild(h) }.padding(horizontal = 12.dp, vertical = 8.dp))
            }
        }
        if (later.isNotEmpty()) {
            PaperSection(tr("Нужно приложение", "Needs the app")) {
                Column {
                    later.forEach { h ->
                        Row(Modifier.fillMaxWidth().clickable {
                            runCatching { ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(h.home.page)).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
                        }.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            ArtIcon(h.art(0), 40.dp, Modifier.alpha(.45f))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(h.home.house, style = Ink.body.copy(color = Ink.soft))
                                Text(when (h.status) {
                                    Status.OLD -> tr("обновите ${h.home.app}", "update ${h.home.app}")
                                    Status.LOCKED -> tr("${h.home.app} собран другим ключом", "${h.home.app} is signed differently")
                                    else -> tr("поставьте ${h.home.app} ›", "install ${h.home.app} ›")
                                }, style = Ink.small)
                            }
                        }
                    }
                }
            }
        }
    }
}
