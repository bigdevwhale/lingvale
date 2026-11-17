package app.lingvale

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.lingvale.data.Family
import app.lingvale.data.House
import app.lingvale.data.Snap
import app.lingvale.data.Status
import app.lingvale.domain.Game
import app.lingvale.domain.Lang
import app.lingvale.ui.theme.ValeTheme
import app.lingvale.ui.village.Quests
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Tapping a weekly task opens its hint, tapping again closes it. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class QuestHintTest {
    @get:Rule val rule = createComposeRule()

    @Test fun tapShowsAndHidesTheHint() {
        Lang.ru = true
        val live = Family.all.take(3).map { House(it, Status.LIVE, "app.${it.id}", Snap(it.id, "1.0", true, "room_bg", emptyList(), emptyList(), emptyList(), emptyList())) }
        val qs = Game.quests(live)
        assertTrue(qs.all { it.hint.isNotBlank() })
        rule.setContent { ValeTheme(false) { Quests(live) } }
        val q = qs.first()
        rule.onNodeWithText(q.hint).assertDoesNotExist()
        rule.onNodeWithText(q.title).performClick()
        rule.onNodeWithText(q.hint).assertIsDisplayed()
        rule.onNodeWithText(q.title).performClick()
        rule.onNodeWithText(q.hint).assertDoesNotExist()
    }
}
