package com.streak.app.ui.lock

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.streak.app.R
import com.streak.app.ui.theme.StreakTheme

data class PinPadColors(val ink: Color, val key: Color, val dotEmpty: Color)

@Composable
fun pinPadColors(onBrand: Boolean): PinPadColors = if (onBrand) {
    PinPadColors(ink = Color.White, key = Color.White.copy(alpha = .14f), dotEmpty = Color.White.copy(alpha = .35f))
} else {
    val p = StreakTheme.palette
    PinPadColors(ink = p.ink, key = p.surfaceSoft, dotEmpty = p.border)
}

// Vnos kode: pike in stevilcnica (brez sistemske tipkovnice, ki bi si lahko zapomnila vnos).
// errorKey: ob spremembi se pike stresejo (napacna koda).
@Composable
fun PinPad(
    length: Int,
    value: String,
    onValueChange: (String) -> Unit,
    onComplete: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: PinPadColors = pinPadColors(onBrand = false),
    errorKey: Int = 0,
    onBiometric: (() -> Unit)? = null
) {
    val haptics = LocalHapticFeedback.current
    val shake = remember { Animatable(0f) }
    LaunchedEffect(errorKey) {
        if (errorKey > 0) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            shake.animateTo(0f, keyframes {
                durationMillis = 360
                -18f at 50; 16f at 110; -12f at 170; 8f at 230; -4f at 290; 0f at 360
            })
        }
    }
    fun press(digit: Char) {
        if (!enabled || value.length >= length) return
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        val next = value + digit
        onValueChange(next)
        if (next.length == length) onComplete(next)
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        val entered = pluralStringResource(R.plurals.lock_digits_entered, value.length, value.length, length)
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .offset { IntOffset(shake.value.dp.roundToPx(), 0) }
                .padding(vertical = 18.dp)
                .semantics { contentDescription = entered }
        ) {
            repeat(length) { i ->
                val filled = i < value.length
                Box(
                    Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(if (filled) colors.ink else Color.Transparent)
                        .border(2.dp, if (filled) colors.ink else colors.dotEmpty, CircleShape)
                )
            }
        }
        val rows = listOf("123", "456", "789")
        Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(top = 10.dp)) {
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                    row.forEach { d -> DigitKey(d, colors, enabled) { press(d) } }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                if (onBiometric != null) {
                    IconKey(Icons.Rounded.Fingerprint, stringResource(R.string.lock_use_biometric), colors, enabled, onBiometric)
                } else {
                    Box(Modifier.size(KEY))
                }
                DigitKey('0', colors, enabled) { press('0') }
                IconKey(Icons.AutoMirrored.Rounded.Backspace, stringResource(R.string.lock_delete_digit), colors, enabled && value.isNotEmpty()) {
                    onValueChange(value.dropLast(1))
                }
            }
        }
    }
}

private val KEY = 72.dp

@Composable
private fun DigitKey(digit: Char, colors: PinPadColors, enabled: Boolean, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(KEY)
            .clip(CircleShape)
            .background(colors.key)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
    ) {
        Text(digit.toString(), style = MaterialTheme.typography.headlineMedium, color = colors.ink)
    }
}

@Composable
private fun IconKey(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, colors: PinPadColors, enabled: Boolean, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(KEY)
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description }
    ) {
        Icon(icon, contentDescription = null, tint = if (enabled) colors.ink else colors.dotEmpty, modifier = Modifier.size(28.dp))
    }
}
