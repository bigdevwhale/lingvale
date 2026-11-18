package app.lingvale.ui

import app.lingvale.ui.components.sheetMax
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Cottage
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import app.lingvale.ui.components.paper
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.lingvale.container
import app.lingvale.currentLanguage
import app.lingvale.data.House
import app.lingvale.domain.tr
import app.lingvale.setLanguage
import app.lingvale.ui.bestiary.BestiaryScreen
import app.lingvale.ui.components.Btn
import app.lingvale.ui.components.Gap
import app.lingvale.ui.components.Pill
import app.lingvale.ui.components.Sec
import app.lingvale.ui.days.DaysScreen
import app.lingvale.ui.theme.C
import app.lingvale.ui.theme.T
import app.lingvale.ui.village.HouseSheet
import app.lingvale.ui.village.VillageScreen

/** Where a demo or screenshot run starts. */
object Start { @Volatile var tab = 0; @Volatile var open: String? = null; @Volatile var daysTab = 0 }

@Composable
fun Root() {
    val c = LocalContext.current.container
    val all by c.village.houses.collectAsState()
    val built by c.prefs.built.collectAsState()
    // The village is what the player built, in building order; a house whose app is gone stays out until it's back.
    val houses = remember(all, built) { built.mapNotNull { id -> all.firstOrNull { it.home.id == id && it.live } } }
    val canBuild = all.any { it.live && it.home.id !in built }
    var building by remember { mutableStateOf(false) }
    var tab by rememberSaveable { mutableIntStateOf(Start.tab) }
    var house by remember { mutableStateOf<House?>(Start.open?.let { id -> houses.firstOrNull { it.home.id == id } }) }
    var settings by remember { mutableStateOf(false) }
    var shop by remember { mutableStateOf(Start.open == "shop") }
    var welcome by remember { mutableStateOf(!c.prefs.welcomed) }
    Column(Modifier.fillMaxSize().background(C.bg)) {
        Box(Modifier.weight(1f)) {
            when (tab) {
                0 -> VillageScreen(houses, { house = it }, { settings = true }, { shop = true }, onBuild = if (canBuild) ({ building = true }) else null)
                1 -> BestiaryScreen(houses)
                else -> DaysScreen(houses)
            }
        }
        Column(Modifier.fillMaxWidth().paper()) {
            Box(Modifier.fillMaxWidth().height(3.dp).background(androidx.compose.ui.graphics.Color(0xFF8A6440)))
            Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(top = 4.dp, bottom = 2.dp)) {
                Tab("nav_village", tr("Деревня", "Village"), tab == 0) { tab = 0 }
                Tab("nav_album", tr("Альбом", "Album"), tab == 1) { tab = 1 }
                Tab("nav_days", tr("Дни", "Days"), tab == 2) { tab = 2 }
            }
        }
    }
    house?.let { h -> HouseSheet(houses.firstOrNull { it.home.id == h.home.id } ?: h, { house = null }, onDemolish = { c.prefs.demolish(h.home.id); house = null }) }
    if (building) app.lingvale.ui.village.BuildSheet(all, built, { h -> c.prefs.build(h.home.id); building = false }) { building = false }
    // The first time, with nothing built yet, the building sheet opens by itself after the welcome.
    LaunchedEffect(welcome, canBuild) { if (!welcome && built.isEmpty() && canBuild && !c.prefs.askedToBuild) { c.prefs.askedToBuild = true; building = true } }
    if (settings) SettingsSheet { settings = false }
    if (shop) app.lingvale.ui.village.ShopSheet(houses) { shop = false }
    if (welcome) Welcome { c.prefs.welcomed = true; welcome = false }
}

/** A tab of the bottom bar: a little picture that sits up when chosen; the others wait in grey. */
@Composable
private fun androidx.compose.foundation.layout.RowScope.Tab(icon: String, label: String, on: Boolean, onClick: () -> Unit) {
    val ctx = LocalContext.current
    val lift by androidx.compose.animation.core.animateFloatAsState(if (on) 1f else 0f, androidx.compose.animation.core.spring(dampingRatio = .45f), label = "nav")
    val grey = remember { androidx.compose.ui.graphics.ColorFilter.colorMatrix(androidx.compose.ui.graphics.ColorMatrix().apply { setToSaturation(.15f) }) }
    Column(Modifier.weight(1f).clickable(remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, null, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally) {
        app.lingvale.ui.art.Art.get(ctx, icon)?.let { b ->
            androidx.compose.foundation.Image(remember(b) { androidx.compose.ui.graphics.painter.BitmapPainter(b, filterQuality = androidx.compose.ui.graphics.FilterQuality.Low) }, label,
                Modifier.size(44.dp).graphicsLayer { translationY = -6f * lift; scaleX = 1f + .15f * lift; scaleY = 1f + .15f * lift; alpha = .6f + .4f * lift },
                colorFilter = if (on) null else grey)
        }
        Text(label, style = if (on) app.lingvale.ui.theme.Ink.h3.copy(fontSize = app.lingvale.ui.theme.Ink.small.fontSize, color = app.lingvale.ui.theme.Ink.red) else app.lingvale.ui.theme.Ink.small)
        Box(Modifier.padding(top = 1.dp).height(2.dp).width(20.dp * lift).background(app.lingvale.ui.theme.Ink.red))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSheet(onClose: () -> Unit) {
    val c = LocalContext.current.container
    val theme by c.prefs.theme.collectAsState()
    ModalBottomSheet(onDismissRequest = onClose, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = app.lingvale.ui.theme.Ink.paper, contentWindowInsets = { androidx.compose.foundation.layout.WindowInsets(0) }) {
        androidx.compose.foundation.layout.Box(Modifier.sheetMax()) {
        Column(Modifier.fillMaxWidth().paper().verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding().padding(bottom = 20.dp)) {
            Text(tr("Настройки", "Settings"), style = app.lingvale.ui.theme.Ink.head)
            app.lingvale.ui.components.PaperSection(tr("Тема", "Theme")) {}
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill(tr("Как в системе", "System"), theme == "", { c.prefs.setTheme("") })
                Pill(tr("Светлая", "Light"), theme == "light", { c.prefs.setTheme("light") })
                Pill(tr("Тёмная", "Dark"), theme == "dark", { c.prefs.setTheme("dark") })
            }
            app.lingvale.ui.components.PaperSection(tr("Язык", "Language")) {}
            val lang = currentLanguage()
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill(tr("Как в системе", "System"), lang == "", { setLanguage("") })
                Pill("Русский", lang == "ru", { setLanguage("ru") })
                Pill("English", lang == "en", { setLanguage("en") })
            }
            MovingSection()
            app.lingvale.ui.components.PaperSection(tr("О деревне", "About")) {}
            Text(tr("Lingvale берёт питомцев и дни у самих приложений и никуда их не отправляет: всё остаётся на телефоне. В приложениях она ничего не меняет, кроме одного случая: когда вы сами загружаете файл переезда.",
                "Lingvale takes the pets and days from the apps themselves and sends nothing anywhere: everything stays on the phone. It changes nothing in the apps, with one exception: when you load a moving file yourself."), style = app.lingvale.ui.theme.Ink.body)
        }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Welcome(onDone: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDone, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = app.lingvale.ui.theme.Ink.paper, contentWindowInsets = { androidx.compose.foundation.layout.WindowInsets(0) }) {
        androidx.compose.foundation.layout.Box(Modifier.sheetMax()) {
        Column(Modifier.fillMaxWidth().paper().padding(horizontal = 20.dp).navigationBarsPadding().padding(bottom = 20.dp)) {
            Text(tr("Добро пожаловать в Lingvale", "Welcome to Lingvale"), style = app.lingvale.ui.theme.Ink.head)
            Gap(10.dp)
            listOf(
                tr("Это деревня, где вместе живут питомцы из Puffling, Pumpling, Pennyling и других приложений семейства.",
                    "A village where the pets of Puffling, Pumpling, Pennyling and the rest of the family live together."),
                tr("Дома вы строите сами: выберите, какие приложения поселить в долине. Жители и их дела переедут вместе с домом, вводить ничего не нужно.",
                    "You build the houses yourself: pick which apps settle in the valley. Their residents and days move in with the house; nothing to type in."),
                tr("Каждое дело приносит деревне светлячков, а когда в один день заняты несколько домов – ещё больше. На светлячков в лавке покупают колодцы, фонари и мельницу.",
                    "Everything you do brings the village fireflies, and more when several houses are busy on the same day. The shop sells wells, lamps and a windmill for them."),
                tr("Ночью в окнах тех домов, где на неделе что-то происходило, горит свет. Пустых окон никто не ругает.",
                    "At night the houses that were busy this week have lit windows. Nobody minds dark ones."),
            ).forEachIndexed { i, s ->
                Row(Modifier.padding(vertical = 6.dp)) {
                    Text("${i + 1}", style = app.lingvale.ui.theme.Ink.head.copy(color = app.lingvale.ui.theme.Ink.red), modifier = Modifier.padding(end = 10.dp))
                    Text(s, style = app.lingvale.ui.theme.Ink.body)
                }
            }
            Gap(16.dp)
            Btn(tr("Понятно", "Got it"), onDone)
        }
        }
    }
}
