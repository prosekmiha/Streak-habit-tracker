package com.streak.app.ui.achievements

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.streak.app.R
import com.streak.app.game.GameCelebration
import com.streak.app.ui.home.CelebrationDialog
import com.streak.app.ui.theme.Bubblegum
import com.streak.app.ui.theme.InkOnPastel

// Praznovanje nove znacke ali novega nivoja (ista postavitev kot mejnik verige)
@Composable
fun GameCelebrationOverlay(celebration: GameCelebration, onDismiss: () -> Unit) {
    val context = LocalContext.current
    when (celebration) {
        is GameCelebration.NewBadge -> {
            val badge = celebration.badge
            val label = stringResource(R.string.new_badge)
            val title = stringResource(badge.titleRes())
            val description = badge.description()
            val shareText = stringResource(R.string.share_badge, title)
            CelebrationDialog(
                label = label,
                title = title,
                text = description,
                badge = { Icon(badge.icon, contentDescription = null, tint = InkOnPastel, modifier = Modifier.size(52.dp)) },
                badgeColor = badge.color,
                onShare = { ShareCard.share(context, badgeShareSpec(context, badge, label, description, shareText)) },
                onDismiss = onDismiss
            )
        }
        is GameCelebration.LevelUp -> {
            val level = celebration.level
            val label = stringResource(R.string.new_level)
            val title = stringResource(R.string.level_up_title, level)
            val text = stringResource(R.string.level_up_text)
            val shareText = stringResource(R.string.share_level, level)
            CelebrationDialog(
                label = label,
                title = title,
                text = text,
                badge = { Text(level.toString(), style = MaterialTheme.typography.displayMedium, color = InkOnPastel) },
                badgeColor = Bubblegum,
                onShare = {
                    ShareCard.share(context, ShareCardSpec(label.uppercase(), title, text, Bubblegum, badgeText = level.toString(), shareText = shareText))
                },
                onDismiss = onDismiss
            )
        }
    }
}
