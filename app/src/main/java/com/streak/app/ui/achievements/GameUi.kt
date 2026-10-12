package com.streak.app.ui.achievements

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalResources
import com.streak.app.R
import com.streak.app.domain.gamification.Badge
import com.streak.app.domain.gamification.BadgeKind
import com.streak.app.domain.gamification.Challenge
import com.streak.app.ui.theme.Bubblegum
import com.streak.app.ui.theme.Lavender
import com.streak.app.ui.theme.Lime
import com.streak.app.ui.theme.Mint
import com.streak.app.ui.theme.Peach
import com.streak.app.ui.theme.Sky
import com.streak.app.ui.theme.Sun
import java.time.LocalDate

// Prikaz znack, izzivov in misli dneva (besedila so v resursih zaradi prevodov)

@StringRes
fun Badge.titleRes(): Int = when (this) {
    Badge.FIRST_STEP -> R.string.badge_first_step
    Badge.STREAK_7 -> R.string.badge_streak_7
    Badge.STREAK_30 -> R.string.badge_streak_30
    Badge.STREAK_100 -> R.string.badge_streak_100
    Badge.STREAK_365 -> R.string.badge_streak_365
    Badge.PERFECT_DAY -> R.string.badge_perfect_day
    Badge.PERFECT_WEEK -> R.string.badge_perfect_week
    Badge.DONE_100 -> R.string.badge_done_100
    Badge.DONE_1000 -> R.string.badge_done_1000
    Badge.FIRST_SESSION -> R.string.badge_first_session
    Badge.FOCUS_10H -> R.string.badge_focus_10h
    Badge.EARLY_BIRD -> R.string.badge_early_bird
    Badge.REFLECTIVE -> R.string.badge_reflective
    Badge.CHALLENGER -> R.string.badge_challenger
    Badge.LEVEL_5 -> R.string.badge_level_5
    Badge.LEVEL_10 -> R.string.badge_level_10
}

// Opis znacke s pragom (mnozina po pragu, npr. "Veriga 30 dni")
fun Badge.describe(res: Resources): String = when (this) {
    Badge.FIRST_STEP -> res.getString(R.string.badge_first_step_desc)
    Badge.STREAK_7, Badge.STREAK_30, Badge.STREAK_100, Badge.STREAK_365 -> res.getQuantityString(R.plurals.badge_streak_desc, target, target)
    Badge.PERFECT_DAY -> res.getString(R.string.badge_perfect_day_desc)
    Badge.PERFECT_WEEK -> res.getString(R.string.badge_perfect_week_desc)
    Badge.DONE_100, Badge.DONE_1000 -> res.getQuantityString(R.plurals.badge_done_desc, target, target)
    Badge.FIRST_SESSION -> res.getString(R.string.badge_first_session_desc)
    Badge.FOCUS_10H -> res.getQuantityString(R.plurals.badge_focus_desc, target / 60, target / 60)
    Badge.EARLY_BIRD -> res.getQuantityString(R.plurals.badge_early_bird_desc, target, target)
    Badge.REFLECTIVE -> res.getQuantityString(R.plurals.badge_reflective_desc, target, target)
    Badge.CHALLENGER -> res.getQuantityString(R.plurals.badge_challenger_desc, target, target)
    Badge.LEVEL_5, Badge.LEVEL_10 -> res.getString(R.string.badge_level_desc, target)
}

@Composable
fun Badge.description(): String = describe(LocalResources.current)

val Badge.icon: ImageVector
    get() = when (this) {
        Badge.FIRST_STEP -> Icons.AutoMirrored.Filled.DirectionsWalk
        Badge.STREAK_7, Badge.STREAK_30, Badge.STREAK_100, Badge.STREAK_365 -> Icons.Filled.LocalFireDepartment
        Badge.PERFECT_DAY -> Icons.Filled.WbSunny
        Badge.PERFECT_WEEK -> Icons.Filled.CalendarMonth
        Badge.DONE_100 -> Icons.Filled.TaskAlt
        Badge.DONE_1000 -> Icons.Filled.Verified
        Badge.FIRST_SESSION -> Icons.Filled.SelfImprovement
        Badge.FOCUS_10H -> Icons.Filled.Timer
        Badge.EARLY_BIRD -> Icons.Filled.WbTwilight
        Badge.REFLECTIVE -> Icons.Filled.EditNote
        Badge.CHALLENGER -> Icons.Filled.EmojiEvents
        Badge.LEVEL_5 -> Icons.Filled.Star
        Badge.LEVEL_10 -> Icons.Filled.MilitaryTech
    }

val Badge.color: Color
    get() = when (kind) {
        BadgeKind.STREAK -> Peach
        BadgeKind.PERFECT -> Lime
        BadgeKind.VOLUME -> Sun
        BadgeKind.FOCUS -> Mint
        BadgeKind.HABIT -> if (this == Badge.CHALLENGER) Sky else Lavender
        BadgeKind.LEVEL -> Bubblegum
    }

@StringRes
fun Challenge.titleRes(): Int = when (this) {
    Challenge.PERFECT_DAY -> R.string.challenge_perfect_day
    Challenge.EARLY_START -> R.string.challenge_early_start
    Challenge.REFLECT -> R.string.challenge_reflect
    Challenge.DONE_BY_EVENING -> R.string.challenge_done_by_evening
}

@StringRes
fun Challenge.descriptionRes(): Int = when (this) {
    Challenge.PERFECT_DAY -> R.string.challenge_perfect_day_desc
    Challenge.EARLY_START -> R.string.challenge_early_start_desc
    Challenge.REFLECT -> R.string.challenge_reflect_desc
    Challenge.DONE_BY_EVENING -> R.string.challenge_done_by_evening_desc
}

// Misel dneva: vsak dan druga, enaka za ves dan
fun thoughtIndex(date: LocalDate, count: Int): Int = Math.floorMod(date.toEpochDay(), count.toLong()).toInt()
