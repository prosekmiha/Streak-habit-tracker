package com.streak.app.ui.mood

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SentimentDissatisfied
import androidx.compose.material.icons.rounded.SentimentNeutral
import androidx.compose.material.icons.rounded.SentimentSatisfied
import androidx.compose.material.icons.rounded.SentimentVeryDissatisfied
import androidx.compose.material.icons.rounded.SentimentVerySatisfied
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.streak.app.R
import com.streak.app.ui.theme.Bubblegum
import com.streak.app.ui.theme.Lime
import com.streak.app.ui.theme.Mint
import com.streak.app.ui.theme.Peach
import com.streak.app.ui.theme.Sun

// Razpolozenje 1..5: barva, ikona in ime (enako povsod: list, kartica, statistika)
object MoodUi {
    val levels = 1..5

    fun color(level: Int): Color = when (level) {
        1 -> Bubblegum
        2 -> Peach
        3 -> Sun
        4 -> Lime
        else -> Mint
    }

    fun icon(level: Int): ImageVector = when (level) {
        1 -> Icons.Rounded.SentimentVeryDissatisfied
        2 -> Icons.Rounded.SentimentDissatisfied
        3 -> Icons.Rounded.SentimentNeutral
        4 -> Icons.Rounded.SentimentSatisfied
        else -> Icons.Rounded.SentimentVerySatisfied
    }

    @StringRes
    fun label(level: Int): Int = when (level) {
        1 -> R.string.mood_level_1
        2 -> R.string.mood_level_2
        3 -> R.string.mood_level_3
        4 -> R.string.mood_level_4
        else -> R.string.mood_level_5
    }
}
