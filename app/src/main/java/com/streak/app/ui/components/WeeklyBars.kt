package com.streak.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.streak.app.ui.theme.Accent
import com.streak.app.ui.theme.Success

// Stolpcni graf zadnjih tednov (vrednosti 0f..1f). Zadnji stolpec poudarjen z akcentom.
@Composable
fun WeeklyBars(
    values: List<Float>,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
    ) {
        if (values.isEmpty()) return@Canvas
        val gap = 10f
        val barWidth = (size.width - gap * (values.size - 1)) / values.size
        val maxH = size.height
        values.forEachIndexed { index, v ->
            val h = (maxH * v.coerceIn(0.05f, 1f))
            val x = index * (barWidth + gap)
            val y = maxH - h
            val isLast = index == values.lastIndex
            val brush = if (isLast)
                Brush.verticalGradient(listOf(Accent, Accent.copy(alpha = 0.7f)))
            else
                Brush.verticalGradient(listOf(Success, Success.copy(red = 0.24f, green = 0.66f, blue = 0.4f)))
            drawRoundRect(
                brush = brush,
                topLeft = Offset(x, y),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(6f, 6f)
            )
        }
    }
}
