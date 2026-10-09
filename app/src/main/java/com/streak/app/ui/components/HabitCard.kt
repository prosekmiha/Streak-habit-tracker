package com.streak.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.streak.app.domain.model.HabitWithStreak
import com.streak.app.ui.theme.softTint

// Polna barvna kartica navade – pastelni odtenek barve, ikona v cipu, pike zadnjih dni in krogec za odkljukanje.
@Composable
fun HabitCard(
    item: HabitWithStreak,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = parseHexColor(item.habit.colorHex)
    val done = item.isDoneToday
    val statusMap = item.logs.associate { it.date to it.status }

    val tinted = softTint(color)
    val bg by animateColorAsState(
        if (done) color else tinted,
        label = "bg"
    )
    val onCard = if (done) Color.White else MaterialTheme.colorScheme.onSurface
    val checkScale by animateFloatAsState(
        targetValue = if (done) 1f else 0.85f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "check"
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .clickable(
                interactionSource = MutableInteractionSource(),
                indication = null
            ) { onClick() }
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        // Ikonski cip
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    if (done) Color.White.copy(alpha = 0.22f) else color.copy(alpha = 0.18f),
                    RoundedCornerShape(14.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = IconMap.resolve(item.habit.icon),
                contentDescription = null,
                tint = if (done) Color.White else color,
                modifier = Modifier.size(22.dp)
            )
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = item.habit.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = onCard
            )
            StreakDots(
                statusByDate = statusMap,
                color = if (done) Color.White else color,
                trackColor = if (done) Color.White.copy(alpha = 0.25f) else color.copy(alpha = 0.18f)
            )
        }

        // Krogec za odkljukanje
        Box(
            modifier = Modifier
                .size(32.dp)
                .scale(checkScale)
                .then(
                    if (done) Modifier.background(Color.White, CircleShape)
                    else Modifier.border(2.dp, color.copy(alpha = 0.5f), CircleShape)
                )
                .clickable(
                    interactionSource = MutableInteractionSource(),
                    indication = null
                ) { onToggle() },
            contentAlignment = Alignment.Center
        ) {
            if (done) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
