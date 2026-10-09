package com.streak.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.streak.app.domain.model.LogStatus
import java.time.LocalDate

// GitHub-style heatmap zadnjega leta. Vsak stolpec je teden (7 dni navzdol).
@Composable
fun Heatmap(
    statusByDate: Map<LocalDate, LogStatus>,
    baseColor: Color,
    modifier: Modifier = Modifier,
    weeks: Int = 26
) {
    val empty = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val today = LocalDate.now()
    // Zacni na zacetku tedna pred N tedni
    val start = today.minusWeeks((weeks - 1).toLong()).with(java.time.DayOfWeek.MONDAY)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
    ) {
        val cols = weeks
        val rows = 7
        val gap = 4f
        val cell = ((size.width - gap * (cols - 1)) / cols).coerceAtMost((size.height - gap * (rows - 1)) / rows)
        for (c in 0 until cols) {
            for (r in 0 until rows) {
                val date = start.plusWeeks(c.toLong()).plusDays(r.toLong())
                if (date.isAfter(today)) continue
                val status = statusByDate[date]
                val color = when (status) {
                    LogStatus.DONE -> baseColor
                    LogStatus.SKIPPED -> baseColor.copy(alpha = 0.45f)
                    else -> empty
                }
                val x = c * (cell + gap)
                val y = r * (cell + gap)
                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, y),
                    size = Size(cell, cell),
                    cornerRadius = CornerRadius(3f, 3f)
                )
            }
        }
    }
}
