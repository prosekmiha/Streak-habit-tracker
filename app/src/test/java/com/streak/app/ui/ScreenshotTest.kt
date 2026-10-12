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
import com.streak.app.domain.model.CompletionStatus
import com.streak.app.domain.model.DayState
import com.streak.app.domain.model.GoalType
import com.streak.app.domain.model.ScheduleType
import com.streak.app.domain.plan.WeekPlanner
import com.streak.app.testing.completion
import com.streak.app.testing.testHabit
import com.streak.app.ui.achievements.BadgeCircle
import com.streak.app.ui.components.ButtonVariant
import com.streak.app.ui.components.HabitCardUi
import com.streak.app.ui.components.HabitGridCard
import com.streak.app.ui.mood.MoodCard
import com.streak.app.ui.mood.MoodForm
import com.streak.app.ui.mood.MoodSheetContent
import com.streak.app.ui.review.ReviewContent
import com.streak.app.ui.review.ReviewUiState
import com.streak.app.ui.stats.MoodStatsContent
import com.streak.app.ui.stats.MoodStatsUiState
import com.streak.app.ui.components.StreakButton
import com.streak.app.ui.components.WeekDayStatus
import com.streak.app.ui.components.WeekDayUi
import com.streak.app.ui.components.WeekStrip
import com.streak.app.ui.mascot.MascotMood
import com.streak.app.ui.mascot.poseDrawable
import com.streak.app.ui.stats.WeekPlanContent
import com.streak.app.ui.stats.WeekPlanUiState
import com.streak.app.ui.theme.StreakTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.DayOfWeek
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

    @Test fun weekPlanLight() = shoot("week_plan_light") { WeekPlan() }
    @Test fun weekPlanDark() = shoot("week_plan_dark", dark = true) { WeekPlan() }
    @Test fun weekPlanLargeFont() = shoot("week_plan_font200", fontScale = 2f) { WeekPlan() }

    @Test fun weekPlanSlovenian() {
        RuntimeEnvironment.setQualifiers("+sl")
        shoot("week_plan_sl") { WeekPlan() }
    }

    @Test fun quitCardsLight() = shoot("quit_cards_light") { QuitCards() }
    @Test fun quitCardsDark() = shoot("quit_cards_dark", dark = true) { QuitCards() }
    @Test fun quitCardsLargeFont() = shoot("quit_cards_font200", fontScale = 2f) { QuitCards() }

    @Test fun quitCardsSlovenian() {
        RuntimeEnvironment.setQualifiers("+sl")
        shoot("quit_cards_sl") { QuitCards() }
    }

    @Test fun moodLight() = shoot("mood_light") { Mood() }
    @Test fun moodDark() = shoot("mood_dark", dark = true) { Mood() }
    @Test fun moodLargeFont() = shoot("mood_font200", fontScale = 2f) { Mood() }

    @Test fun moodSlovenian() {
        RuntimeEnvironment.setQualifiers("+sl")
        shoot("mood_sl") { Mood() }
    }

    @Test fun moodStatsLight() = shoot("mood_stats_light") { MoodStats() }
    @Test fun moodStatsDark() = shoot("mood_stats_dark", dark = true) { MoodStats() }

    @Test fun reviewLight() = shoot("review_light") { ReviewSample() }
    @Test fun reviewDark() = shoot("review_dark", dark = true) { ReviewSample() }
    @Test fun reviewLargeFont() = shoot("review_font200", fontScale = 2f) { ReviewSample() }

    @Test fun reviewSlovenian() {
        RuntimeEnvironment.setQualifiers("+sl")
        shoot("review_sl") { ReviewSample() }
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

    // Tedenski plan: vse skupine urnikov in vsa stanja dni (danes je sreda)
    @Composable
    private fun WeekPlan() {
        val monday = LocalDate.of(2026, 10, 5)
        val wednesday = monday.plusDays(2)
        val habits = listOf(
            testHabit("a").copy(title = stringResource(R.string.tpl_water), icon = "Water", colorHex = "#A9C8F5"),
            testHabit("b", type = ScheduleType.WEEKDAYS, weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))
                .copy(title = stringResource(R.string.tpl_reading), icon = "Book", colorHex = "#F7CD63"),
            testHabit("c", type = ScheduleType.TIMES_PER_WEEK, times = 3).copy(title = stringResource(R.string.tpl_meditation), icon = "Meditation", colorHex = "#A6E3D8"),
            testHabit("d", type = ScheduleType.TIMES_PER_MONTH, times = 4).copy(title = stringResource(R.string.tpl_household), icon = "Iron", colorHex = "#B8EB6C")
        )
        val logs = listOf(
            completion(monday, habitId = "a"),
            completion(monday.plusDays(1), CompletionStatus.SKIPPED, habitId = "a"),
            completion(monday, habitId = "c"),
            completion(monday.plusDays(2), habitId = "c"),
            completion(LocalDate.of(2026, 10, 2), habitId = "d")
        )
        val plan = WeekPlanner.build(habits, monday, logs, emptyList(), wednesday)
        WeekPlanContent(
            WeekPlanUiState(loaded = true, today = wednesday, weekStart = monday, isCurrentWeek = true, plan = plan),
            onPrevWeek = {}, onNextWeek = {}, onThisWeek = {}, onOpenHabit = {}
        )
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

    // Navade za opuscanje: cist dan in dan z zdrsom
    @Composable
    private fun QuitCards() {
        val smoking = testHabit("q").copy(title = stringResource(R.string.tpl_no_smoking), icon = "SmokeFree", colorHex = "#A6E3D8", goalType = GoalType.QUIT, targetValue = 0.0)
        val sugar = testHabit("s").copy(title = stringResource(R.string.tpl_no_sugar), icon = "Restaurant", colorHex = "#FFB38A", goalType = GoalType.QUIT, targetValue = 0.0)
        Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HabitGridCard(HabitCardUi(smoking, DayState.DONE, 0.0, 23, 23), onTap = {}, onLongPress = {}, modifier = Modifier.weight(1f))
            HabitGridCard(HabitCardUi(sugar, DayState.MISSED, 2.0, 0, 9), onTap = {}, onLongPress = {}, modifier = Modifier.weight(1f))
        }
    }

    // Kartica na domacem zaslonu in vsebina lista za vpis
    @Composable
    private fun Mood() {
        Column(Modifier.padding(20.dp)) {
            MoodCard(onPick = {}, onLater = {})
            Column(Modifier.padding(top = 20.dp)) {
                MoodSheetContent(
                    form = MoodForm(date = today, mood = 4, energy = 2, note = "", exists = true),
                    onMood = {}, onEnergy = {}, onNote = {}, onSave = {}, onDelete = {}
                )
            }
        }
    }

    @Composable
    private fun MoodStats() {
        val month = java.time.YearMonth.from(today)
        val moods = (1..today.dayOfMonth).filter { it % 4 != 0 }.associate { month.atDay(it) to (it % 5) + 1 }
        MoodStatsContent(
            MoodStatsUiState(
                month = month,
                today = today,
                moodByDate = moods,
                stats = com.streak.app.domain.mood.MoodStats(42, 3.6, 4.2, 2.9, "a", 4.4),
                topHabitTitle = stringResource(R.string.tpl_exercise),
                loaded = true
            ),
            onPrev = {}, onNext = {}, onSelectDate = {}
        )
    }

    @Composable
    private fun ReviewSample() {
        val range = com.streak.app.domain.review.ReviewRange.month(java.time.YearMonth.from(today))
        val habit = testHabit("r").copy(title = stringResource(R.string.tpl_reading), colorHex = "#F7CD63")
        val review = com.streak.app.domain.review.Review(
            range = range, doneCount = 128, cleanDays = 11, activeDays = 11, perfectDays = 7, longestPerfectRun = 4,
            topHabit = habit, topHabitDays = 10, bestWeekday = java.time.DayOfWeek.TUESDAY,
            chronotype = com.streak.app.domain.review.Chronotype.EARLY_BIRD,
            sessionCount = 9, sessionSeconds = 4 * 3600 + 25 * 60, moodAverage = 3.8, moodEntries = 9
        )
        ReviewContent(
            ReviewUiState(range = range, review = review, today = today),
            onBack = {}, onPeriod = {}, onPrev = {}, onNext = {}, onShare = {}
        )
    }
}
