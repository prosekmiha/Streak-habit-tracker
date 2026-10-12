package com.streak.app.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.streak.app.R
import com.streak.app.domain.gamification.Badge
import com.streak.app.domain.gamification.Challenge
import com.streak.app.domain.gamification.GameMemory
import com.streak.app.domain.gamification.InboxEvent
import com.streak.app.domain.gamification.InboxType
import com.streak.app.ui.achievements.color
import com.streak.app.ui.achievements.icon
import com.streak.app.ui.achievements.titleRes
import com.streak.app.ui.components.StreakButton
import com.streak.app.ui.components.StreakSheet
import com.streak.app.ui.mascot.MascotMood
import com.streak.app.ui.mascot.poseDrawable
import com.streak.app.ui.theme.Bubblegum
import com.streak.app.ui.theme.InkOnPastel
import com.streak.app.ui.theme.StreakTheme
import com.streak.app.ui.theme.Sun
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

// Nabiralnik: nove znacke, nivoji in opravljeni izzivi (najnovejsi zgoraj)
@Composable
fun InboxSheet(memory: GameMemory?, onOpenAchievements: () -> Unit, onDismiss: () -> Unit) {
    // Kaj je bilo neprebrano ob odprtju (oznaka ostane, dokler je list odprt)
    val readUntil = remember { memory?.readUntil ?: 0L }
    val events = memory?.events.orEmpty().sortedByDescending { it.atMillis }
    StreakSheet(onDismiss = onDismiss, title = stringResource(R.string.inbox_title)) {
        if (events.isEmpty()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Image(painterResource(poseDrawable(MascotMood.SLEEPING)), contentDescription = null, modifier = Modifier.size(120.dp))
                Text(
                    stringResource(R.string.inbox_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = StreakTheme.palette.inkSoft,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        } else {
            events.take(MAX_SHOWN).forEachIndexed { i, event ->
                InboxRow(event, unread = event.atMillis > readUntil)
                if (i < minOf(events.size, MAX_SHOWN) - 1) HorizontalDivider(color = StreakTheme.palette.border)
            }
        }
        StreakButton(stringResource(R.string.inbox_open_achievements), onOpenAchievements, modifier = Modifier.padding(top = 18.dp))
    }
}

@Composable
private fun InboxRow(event: InboxEvent, unread: Boolean) {
    val (icon, color, text) = when (event.type) {
        InboxType.BADGE -> {
            val badge = runCatching { Badge.valueOf(event.key) }.getOrNull()
            Triple(badge?.icon ?: Icons.Filled.Star, badge?.color ?: Sun, stringResource(R.string.inbox_badge, badge?.let { stringResource(it.titleRes()) }.orEmpty()))
        }
        InboxType.LEVEL -> Triple(Icons.Filled.Star, Bubblegum, stringResource(R.string.inbox_level, event.key.toIntOrNull() ?: 0))
        InboxType.CHALLENGE -> {
            val challenge = runCatching { Challenge.valueOf(event.key) }.getOrNull()
            Triple(Icons.Filled.EmojiEvents, Sun, stringResource(R.string.inbox_challenge, challenge?.let { stringResource(it.titleRes()) }.orEmpty()))
        }
    }
    val locale = LocalConfiguration.current.locales[0]
    val time = remember(event.atMillis, locale) {
        Instant.ofEpochMilli(event.atMillis).atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(locale))
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp).semantics(mergeDescendants = true) {}
    ) {
        InboxIcon(icon, color)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(text, style = MaterialTheme.typography.titleSmall, color = StreakTheme.palette.ink)
            Text(time, style = MaterialTheme.typography.labelMedium, color = StreakTheme.palette.inkSoft)
        }
        if (unread) Box(Modifier.size(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
    }
}

@Composable
private fun InboxIcon(icon: ImageVector, color: Color) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(40.dp).clip(CircleShape).background(color)) {
        Icon(icon, contentDescription = null, tint = InkOnPastel, modifier = Modifier.size(22.dp))
    }
}

private const val MAX_SHOWN = 20
