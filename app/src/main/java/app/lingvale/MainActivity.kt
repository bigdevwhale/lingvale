package app.lingvale

import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.lifecycleScope
import app.lingvale.domain.Lang
import app.lingvale.ui.Root
import app.lingvale.ui.Start
import app.lingvale.ui.theme.ValeTheme
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.DEMO && BuildConfig.DEMO_LANG.isNotEmpty() && AppCompatDelegate.getApplicationLocales().toLanguageTags() != BuildConfig.DEMO_LANG) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(BuildConfig.DEMO_LANG)); return }
        Lang.ru = resources.configuration.locales[0].language == "ru"
        if (BuildConfig.DEMO) {
            Start.tab = intent.getIntExtra("tab", 0); Start.open = intent.getStringExtra("open")
            if (BuildConfig.SHOTS) container.prefs.welcomed = true
        }
        setContent {
            val theme by container.prefs.theme.collectAsState()
            val sys = isSystemInDarkTheme()
            val dark = when (theme) { "dark" -> true; "light" -> false; else -> sys }
            LaunchedEffect(dark) {
                val t = android.graphics.Color.TRANSPARENT
                enableEdgeToEdge(statusBarStyle = if (dark) SystemBarStyle.dark(t) else SystemBarStyle.light(t, t), navigationBarStyle = if (dark) SystemBarStyle.dark(t) else SystemBarStyle.light(t, t))
            }
            ValeTheme(dark) { Root() }
        }
    }

    /** The apps may have changed while we were away: ask them again every time the village comes to the front. */
    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            container.village.refresh()
            // The demo (for screenshots) puts up every house it can, in the family's order, so the valley isn't empty.
            if (BuildConfig.DEMO && container.prefs.built.value.isEmpty()) {
                container.prefs.askedToBuild = true
                container.village.houses.value.filter { it.live }.forEach { container.prefs.build(it.home.id) }
            }
        }
    }
}

/** Switches the app language: "" follows the phone, "ru" or "en" pins it. The activity restarts in the new language. */
fun setLanguage(tag: String) = AppCompatDelegate.setApplicationLocales(if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag))
fun currentLanguage(): String = AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore('-')
