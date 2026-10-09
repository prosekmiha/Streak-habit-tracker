package com.streak.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.streak.app.domain.model.DayState
import java.time.LocalDate

// Vrstica pik za zadnjih N dni (privzeto 7)
@Composable
fun StreakDots(
    statusByDate: Map<LocalDate, DayState>,
    today: LocalDate,
    color: Color,
    days: Int = 7,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (i in days - 1 downTo 0) {
            val date = today.minusDays(i.toLong())
            val status = statusByDate[date]
            val dotColor = when (status) {
                DayState.DONE -> color
                DayState.PARTIAL -> color.copy(alpha = 0.6f)
                DayState.SKIPPED, DayState.FROZEN, DayState.VACATION -> color.copy(alpha = 0.4f)
                else -> trackColor
            }
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .background(dotColor, RoundedCornerShape(3.dp))
            )
        }
    }
}
