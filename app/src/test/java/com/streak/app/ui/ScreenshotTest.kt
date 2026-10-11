package com.streak.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.streak.app.R
import com.streak.app.domain.gamification.Badge
import com.streak.app.domain.model.AnimationLevel
import com.streak.app.domain.model.DayState
import com.streak.app.domain.model.GoalType
import com.streak.app.testing.testHabit
import com.streak.app.ui.achievements.BadgeCircle
import com.streak.app.ui.components.ButtonVariant
import com.streak.app.ui.components.HabitCardUi
import com.streak.app.ui.components.HabitGridCard
import com.streak.app.ui.components.StreakButton
import com.streak.app.ui.components.WeekDayStatus
import com.streak.app.ui.components.WeekDayUi
import com.streak.app.ui.components.WeekStrip
import com.streak.app.ui.mascot.MascotMood
import com.streak.app.ui.mascot.poseDrawable
import com.streak.app.ui.theme.StreakTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

// Posnetki kljucnih gradnikov (Roborazzi): svetla/temna tema, 200 % pisava, jeziki
// z dolgimi besedami ali drugo pisavo, poze maskote.
// Posnetki: ./gradlew recordRoborazziDebug, primerjava: ./gradlew verifyRoborazziDebug
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class ScreenshotTest {

    @get:Rule val compose = createComposeRule()

    private val today = LocalDate.of(2026, 10, 11)

    private fun shoot(name: String, dark: Boolean = false, fontScale: Float = 1f, content: @Composable () -> Unit) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                StreakTheme(darkTheme = dark, animationLevel = AnimationLevel.OFF) {
                    Column(Modifier.background(MaterialTheme.colorScheme.background)) { content() }
                }
            }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }

    @Test fun homeLight() = shoot("home_light") { Home() }
    @Test fun homeDark() = shoot("home_dark", dark = true) { Home() }
    @Test fun homeLargeFont() = shoot("home_font200", fontScale = 2f) { Home() }

    @Test fun homeGerman() {
        RuntimeEnvironment.setQualifiers("+de")
        shoot("home_de") { Home() }
    }

    @Test fun homeGreek() {
        RuntimeEnvironment.setQualifiers("+el")
        shoot("home_el") { Home() }
    }

    @Test fun homeBulgarian() {
        RuntimeEnvironment.setQualifiers("+bg")
        shoot("home_bg", dark = true) { Home() }
    }

    @Test fun buttonsAndBadges() = shoot("components") {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ButtonVariant.entries.forEach { StreakButton(it.name, {}, variant = it) }
            StreakButton(stringResource(R.string.save), {}, enabled = false)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(Badge.FIRST_STEP, Badge.STREAK_7, Badge.PERFECT_DAY, Badge.FOCUS_10H, Badge.LEVEL_5).forEachIndexed { i, b ->
                    BadgeCircle(b, earned = i % 2 == 0, size = 56.dp)
                }
            }
        }
    }

    @Test fun mascotPoses() = shoot("mascot_poses") {
        val moods = MascotMood.entries
        moods.chunked(4).forEach { row ->
            Row {
                row.forEach { mood ->
                    Column(Modifier.width(100.dp).padding(4.dp)) {
                        Image(painterResource(poseDrawable(mood)), contentDescription = null, modifier = Modifier.size(92.dp))
                        Text(mood.name, style = MaterialTheme.typography.labelSmall, color = StreakTheme.palette.ink)
                    }
                }
            }
        }
    }

    // Domaci zaslon iz pravih gradnikov (tedenski trak in mreza kartic v vseh stanjih)
    @Composable
    private fun Home() {
        val days = (-3L..3L).map { offset ->
            val d = today.plusDays(offset)
            val status = when {
                offset > 0 -> WeekDayStatus.FUTURE
                offset == -1L -> WeekDayStatus.SOME_DONE
                offset < 0 -> WeekDayStatus.ALL_DONE
                else -> WeekDayStatus.NONE
            }
            WeekDayUi(d, status, isToday = offset == 0L)
        }
        WeekStrip(days, selected = today, onSelect = {}, modifier = Modifier.padding(vertical = 12.dp))
        val cards = listOf(
            HabitCardUi(testHabit("a").copy(title = stringResource(R.string.tpl_reading), icon = "Book", colorHex = "#F7CD63", goalType = GoalType.QUANTITY, targetValue = 20.0, unit = stringResource(R.string.unit_pages)), DayState.PARTIAL, 12.0, 4),
            HabitCardUi(testHabit("b").copy(title = stringResource(R.string.tpl_household), icon = "Iron", colorHex = "#B8EB6C"), DayState.DONE, 1.0, 12),
            HabitCardUi(testHabit("c").copy(title = stringResource(R.string.tpl_meditation), icon = "Meditation", colorHex = "#A6E3D8", goalType = GoalType.DURATION, targetValue = 600.0), DayState.PENDING, 0.0, 0),
            HabitCardUi(testHabit("d").copy(title = stringResource(R.string.tpl_water), icon = "Water", colorHex = "#A9C8F5"), DayState.SKIPPED, 0.0, 7)
        )
        cards.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { HabitGridCard(it, onTap = {}, onLongPress = {}, modifier = Modifier.weight(1f)) }
            }
        }
    }
}
