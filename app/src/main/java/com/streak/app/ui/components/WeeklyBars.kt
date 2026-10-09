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
import com.streak.app.ui.theme.Lime

// Stolpcni graf zadnjih tednov (vrednosti 0f..1f). Tekoci teden je poudarjen.
@Composable
fun WeeklyBars(
    values: List<Float>,
    modifier: Modifier = Modifier,
    current: Color = MaterialTheme.colorScheme.primary,
    past: Color = Lime
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
            drawRoundRect(
                color = if (isLast) current else past,
                topLeft = Offset(x, y),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(6f, 6f)
            )
        }
    }
}
