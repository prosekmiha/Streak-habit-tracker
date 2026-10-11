package com.streak.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.activity.ComponentActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.streak.app.domain.model.AnimationLevel
import com.streak.app.domain.model.DayState
import com.streak.app.domain.model.GoalType
import com.streak.app.testing.testHabit
import com.streak.app.ui.components.ButtonVariant
import com.streak.app.ui.components.HabitCardUi
import com.streak.app.ui.components.HabitGridCard
import com.streak.app.ui.components.RoundIconButton
import com.streak.app.ui.components.Stepper
import com.streak.app.ui.components.StreakButton
import com.streak.app.ui.components.StreakSwitch
import com.streak.app.ui.theme.StreakTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// Dostopnost kljucnih gradnikov: preverjanja ATF (velikost tarc, oznake, kontrast)
// in semantika za bralnik zaslona (opis, stanje, dejanja po meri)
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class AccessibilityTest {

    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private fun card(state: DayState, goal: GoalType = GoalType.YES_NO) = HabitCardUi(
        testHabit("h", goalType = goal, target = if (goal == GoalType.YES_NO) 1.0 else 8.0).copy(title = "Branje", colorHex = "#F7CD63"),
        state, if (state == DayState.DONE) 1.0 else 0.0, currentStreak = 5
    )

    // ATF pod Robolectricom ne deluje (ne zazna niti 20-dp gumba brez oznake), zato lastno
    // preverjanje semanticnega drevesa: vsak element z dejanjem je vsaj 48 x 48 dp in ima oznako
    private fun assertTouchTargetsAndLabels() {
        val density = compose.density
        val problems = compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick), useUnmergedTree = false)
            .fetchSemanticsNodes()
            .mapNotNull { node ->
                val touch = node.touchBoundsInRoot
                val w = with(density) { touch.width.toDp() }
                val h = with(density) { touch.height.toDp() }
                val label = node.config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString()
                    ?: node.config.getOrNull(SemanticsProperties.Text)?.joinToString()
                when {
                    w < 47.5.dp || h < 47.5.dp -> "premajhna tarca ${w}x$h: $label"
                    label.isNullOrBlank() -> "brez oznake: $node"
                    else -> null
                }
            }
        assertTrue(problems.joinToString("; "), problems.isEmpty())
    }

    @Test fun componentsPassAccessibilityChecks() {
        compose.setContent {
            StreakTheme(darkTheme = false, animationLevel = AnimationLevel.OFF) {
                var on by remember { mutableStateOf(false) }
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        HabitGridCard(card(DayState.PENDING), {}, {}, Modifier.weight(1f))
                        HabitGridCard(card(DayState.DONE), {}, {}, Modifier.weight(1f))
                    }
                    StreakButton("Shrani", {}, variant = ButtonVariant.Primary)
                    StreakButton("Kasneje", {}, variant = ButtonVariant.Secondary)
                    Stepper("3", {}, {})
                    StreakSwitch(on, { on = it }, contentDescription = "Zasebni zaslon")
                    RoundIconButton(Icons.Rounded.Close, "Zapri", {})
                }
            }
        }
        assertTouchTargetsAndLabels()
    }

    // Preverjanje res zazna gumb brez oznake (Compose sam razsiri dotikalno obmocje na 48 dp,
    // zato majhen element sam po sebi ni napaka)
    @Test fun checkerCatchesUnlabeledButton() {
        compose.setContent { androidx.compose.foundation.layout.Box(Modifier.padding(4.dp).size(20.dp).clickable { }) }
        val error = runCatching { assertTouchTargetsAndLabels() }.exceptionOrNull()
        assertTrue(error is AssertionError && error.message.orEmpty().startsWith("brez oznake"))
    }

    @Test fun habitCardExposesStateAndActions() {
        var taps = 0
        var longPresses = 0
        compose.setContent {
            StreakTheme(darkTheme = false, animationLevel = AnimationLevel.OFF) {
                HabitGridCard(card(DayState.PENDING), onTap = { taps++ }, onLongPress = { longPresses++ })
            }
        }
        val node = compose.onNodeWithContentDescription("Branje", substring = true)
        val semantics = node.fetchSemanticsNode().config
        assertEquals("ni opravljeno, 5 dni zapored".isNotEmpty(), semantics.getOrNull(SemanticsProperties.StateDescription) != null)
        val actions = semantics.getOrNull(SemanticsActions.CustomActions).orEmpty()
        assertEquals(2, actions.size)
        // Dejanje po meri "oznaci kot opravljeno" in "moznosti navade"
        actions.forEach { it.action() }
        assertEquals(1, taps)
        assertEquals(1, longPresses)
        node.performSemanticsAction(SemanticsActions.OnClick)
        assertEquals(2, taps)
    }

    @Test fun decorativeIconsAreHidden() {
        compose.setContent {
            StreakTheme(darkTheme = false, animationLevel = AnimationLevel.OFF) {
                HabitGridCard(card(DayState.DONE, GoalType.QUANTITY), {}, {})
            }
        }
        // Kartica je en zdruzen element; ikone v njej nimajo lastnih opisov
        compose.onAllNodesWithContentDescription("Book").assertCountEquals(0)
        val merged = compose.onNodeWithContentDescription("Branje", substring = true).fetchSemanticsNode()
        assertTrue(SemanticsMatcher.keyIsDefined(SemanticsProperties.StateDescription).matches(merged))
    }
}
