package app.lingvale.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.lingvale.container
import app.lingvale.data.Family
import app.lingvale.data.Line
import app.lingvale.data.Mark
import app.lingvale.data.Moving
import app.lingvale.data.Stage
import app.lingvale.domain.Lang
import app.lingvale.domain.tr
import app.lingvale.ui.components.Btn
import app.lingvale.ui.components.Gap
import app.lingvale.ui.components.PaperSection
import app.lingvale.ui.theme.Ink
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** The settings' «Moving house»: every app's backup and the village into one file, and back on another phone. */
@Composable
fun MovingSection() {
    val ctx = LocalContext.current
    val c = ctx.container
    val move by c.mover.state.collectAsState()
    val say = { s: String -> android.widget.Toast.makeText(ctx, s, android.widget.Toast.LENGTH_LONG).show() }
    val saveFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri -> if (uri != null) c.mover.save(uri) }
    val openFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) c.mover.pick(uri) }
    val noFiles = tr("На телефоне нет приложения для выбора файлов", "This phone has no app to pick files with")
    PaperSection(tr("Переезд", "Moving house")) {}
    when (move.stage) {
        Stage.IDLE, Stage.ERROR -> {
            if (move.stage == Stage.ERROR) Text(when (move.error) {
                "newer" -> tr("Этот файл сохранён более новой Lingvale. Обновите её и попробуйте снова.", "This file was saved by a newer Lingvale. Update it and try again.")
                "write" -> tr("Не удалось сохранить файл.", "Couldn't save the file.")
                else -> tr("Это не файл переезда Lingvale.", "That isn't a Lingvale moving file.")
            }, style = Ink.body.copy(color = Ink.red), modifier = Modifier.padding(bottom = 8.dp))
            Text(tr("Lingvale соберёт копии всех установленных приложений семейства и саму деревню в один файл. На новом телефоне установите приложения, откройте здесь этот файл – и каждое получит своё обратно.",
                "Lingvale gathers the backups of every installed app of the family, and the village itself, into one file. On the new phone install the apps, open the file here, and each one gets its own back."), style = Ink.body)
            Gap(12.dp)
            Btn(tr("Сохранить всё в файл", "Save everything to a file"), { runCatching { saveFile.launch(Moving.name()) }.onFailure { say(noFiles) } })
            Gap(8.dp)
            Btn(tr("Загрузить из файла", "Load from a file"), { runCatching { openFile.launch(arrayOf("application/zip", "application/octet-stream", "application/x-zip-compressed")) }.onFailure { say(noFiles) } }, soft = true)
        }
        Stage.SAVING, Stage.SAVED -> {
            val saved = move.stage == Stage.SAVED
            Text(if (saved) tr("Файл сохранён. Перенесите его на новый телефон.", "The file is saved. Take it to the new phone.") else tr("Собираю копии…", "Gathering the backups…"), style = Ink.body)
            Gap(6.dp)
            move.lines.forEach { Entry(it, saving = true) }
            if (move.lines.any { it.mark == Mark.OLD }) Text(tr("Старые версии приложений не умеют отдавать копию. Обновите их и сохраните файл заново.",
                "Old versions of the apps can't hand over a backup. Update them and save the file again."), style = Ink.small, modifier = Modifier.padding(top = 6.dp))
            if (saved) { Gap(12.dp); Btn(tr("Готово", "Done"), { c.mover.reset() }, soft = true) }
        }
        Stage.PICKED -> {
            Text(tr("Файл от ${date(move.at)}. Отметьте, что загрузить.", "A file of ${date(move.at)}. Tick what to load."), style = Ink.body)
            Gap(6.dp)
            move.lines.forEach { l -> Entry(l, saving = false, onClick = if (l.mark == Mark.READY) ({ c.mover.toggle(l.id) }) else null) }
            if (move.lines.any { it.mark == Mark.MISSING || it.mark == Mark.OLD }) Text(tr("Приложения, которых здесь нет или которые устарели, можно загрузить позже из этого же файла.",
                "Apps that aren't here yet, or are too old, can be loaded later from this same file."), style = Ink.small, modifier = Modifier.padding(top = 6.dp))
            val any = move.lines.any { it.on && it.mark == Mark.READY }
            if (any) Text(tr("Всё, что сейчас есть в отмеченных приложениях на этом телефоне, заменится данными из файла.",
                "Everything the ticked apps hold on this phone now will be replaced by the file's."), style = Ink.body.copy(color = Ink.red), modifier = Modifier.padding(top = 8.dp))
            Gap(12.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Btn(tr("Отмена", "Cancel"), { c.mover.reset() }, Modifier.weight(1f), soft = true)
                if (any) Btn(tr("Загрузить", "Load"), { c.mover.load() }, Modifier.weight(1f))
            }
        }
        Stage.LOADING, Stage.LOADED -> {
            val done = move.stage == Stage.LOADED
            val bad = move.lines.any { it.mark == Mark.FAILED || it.mark == Mark.NEWER || it.mark == Mark.BUSY }
            Text(if (!done) tr("Раздаю копии по приложениям…", "Handing the backups to the apps…")
                else if (bad) tr("Загружено не всё – посмотрите, что не получилось.", "Not everything loaded: see what didn't work.")
                else tr("Готово: приложения получили своё обратно.", "Done: the apps have their own back."), style = Ink.body)
            Gap(6.dp)
            move.lines.forEach { Entry(it, saving = false) }
            if (done) { Gap(12.dp); Btn(tr("Готово", "Done"), { c.mover.reset() }, soft = true) }
        }
    }
}

/** One line of a move: a tick, the app and its house, and how it stands. */
@Composable
private fun Entry(l: Line, saving: Boolean, onClick: (() -> Unit)? = null) {
    val home = Family.byId[l.id]
    val title = if (l.id == Moving.VILLAGE) tr("Деревня", "The village") else home?.app ?: l.id
    val sub = if (l.id == Moving.VILLAGE) tr("дома, лавка, светлячки", "houses, the shop, fireflies") else home?.house.orEmpty()
    val (text, color) = when (l.mark) {
        Mark.READY -> (if (l.on) tr("загрузить", "load") else tr("пропустить", "skip")) to Ink.soft
        Mark.WORKING -> "…" to Ink.soft
        Mark.DONE -> (if (saving) tr("в файле", "in the file") else tr("загружено", "loaded")) to Ink.green
        Mark.FAILED -> tr("не получилось", "didn't work") to Ink.red
        Mark.OLD -> tr("обновите приложение", "update the app") to Ink.red
        Mark.MISSING -> tr("не установлено", "not installed") to Ink.soft
        Mark.NEWER -> tr("копия новее приложения – обновите его", "the backup is newer than the app: update it") to Ink.red
        Mark.BUSY -> tr("занято – закончите начатое в приложении", "busy: finish what's going on in the app") to Ink.red
    }
    val ticked = when (l.mark) { Mark.READY, Mark.WORKING -> l.on; Mark.DONE -> true; else -> false }
    Row(Modifier.fillMaxWidth().let { if (onClick != null) it.clickable(onClick = onClick) else it }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(20.dp).border(1.5.dp, Ink.rule, RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
            if (ticked) Text("✓", style = Ink.h3.copy(color = Ink.green))
        }
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Text(title, style = Ink.h3)
            if (sub.isNotEmpty()) Text(sub, style = Ink.small)
        }
        Text(text, style = Ink.small.copy(color = color), modifier = Modifier.padding(start = 8.dp).weight(.8f, fill = false))
    }
}

private fun date(ms: Long): String =
    DateTimeFormatter.ofPattern("d MMMM yyyy", if (Lang.ru) Locale.forLanguageTag("ru") else Locale.ENGLISH).format(Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()))
