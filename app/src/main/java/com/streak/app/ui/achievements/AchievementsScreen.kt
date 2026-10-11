package com.streak.app.ui.achievements

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.streak.app.R
import com.streak.app.domain.gamification.Badge
import com.streak.app.domain.gamification.BadgeProgress
import com.streak.app.domain.gamification.GameState
import com.streak.app.ui.components.ButtonVariant
import com.streak.app.ui.components.KpiTile
import com.streak.app.ui.components.ProgressTrack
import com.streak.app.ui.components.ScreenTopBar
import com.streak.app.ui.components.StreakButton
import com.streak.app.ui.components.StreakSheet
import com.streak.app.ui.mascot.MascotMood
import com.streak.app.ui.mascot.poseDrawable
import com.streak.app.ui.theme.Blurple
import com.streak.app.ui.theme.InkOnPastel
import com.streak.app.ui.theme.Lavender
import com.streak.app.ui.theme.Lime
import com.streak.app.ui.theme.Mint
import com.streak.app.ui.theme.StreakTheme
import com.streak.app.ui.theme.Sun
import kotlinx.coroutines.launch

// Dosezki: nivo in XP, izziv dneva, misel dneva, znacke
@Composable
fun AchievementsScreen(onBack: () -> Unit, viewModel: AchievementsViewModel = hiltViewModel()) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf<Badge?>(null) }
    val thoughts = stringArrayResource(R.array.daily_thoughts)

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp)
    ) {
        ScreenTopBar(stringResource(R.string.achievements_title), Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), onBack)
        val state = ui ?: return@Column
        val game = state.game
        LevelCard(game)
        XpTiles(game)
        ChallengeCard(game)
        Column(
            Modifier
                .padding(top = 12.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(StreakTheme.palette.surfaceSoft)
                .padding(18.dp)
                .semantics(mergeDescendants = true) {}
        ) {
            Text(stringResource(R.string.thought_title), style = MaterialTheme.typography.labelLarge, color = StreakTheme.palette.inkSoft)
            Text(
                thoughts[thoughtIndex(state.today, thoughts.size)],
                style = MaterialTheme.typography.titleMedium,
                color = StreakTheme.palette.ink,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        BadgeGrid(game.badges, onSelect = { selected = it })
        XpHelp()
    }

    selected?.let { badge ->
        val progress = ui?.game?.badges?.firstOrNull { it.badge == badge } ?: return@let
        BadgeSheet(progress, onDismiss = { selected = null })
    }
}

@Composable
private fun LevelCard(game: GameState) {
    val level = game.level
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(top = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Blurple)
            .padding(start = 20.dp, top = 18.dp, bottom = 18.dp, end = 8.dp)
            .semantics(mergeDescendants = true) {}
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.level_label, level.level),
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                modifier = Modifier.semantics { heading() }
            )
            Text(stringResource(R.string.xp_total, game.xp.total), style = MaterialTheme.typography.titleMedium, color = Color(0xFFE4E5FB))
            ProgressTrack(
                progress = level.progress,
                modifier = Modifier.padding(top = 12.dp).fillMaxWidth(),
                track = Color.White.copy(alpha = 0.22f),
                color = Lime
            )
            Text(
                stringResource(R.string.level_progress, level.xpIntoLevel, level.xpForLevel, level.level + 1),
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFE4E5FB),
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        Image(
            painterResource(poseDrawable(if (game.today.done) MascotMood.CHEER else MascotMood.THUMBS_UP)),
            contentDescription = null,
            modifier = Modifier.size(width = 104.dp, height = 112.dp)
        )
    }
}

@Composable
private fun XpTiles(game: GameState) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 12.dp).fillMaxWidth()) {
        KpiTile(stringResource(R.string.xp_total, game.xp.habits), stringResource(R.string.xp_habits), Sun, Modifier.weight(1f))
        KpiTile(stringResource(R.string.xp_total, game.xp.perfectDays), stringResource(R.string.xp_perfect_days), Lime, Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 10.dp).fillMaxWidth()) {
        KpiTile(stringResource(R.string.xp_total, game.xp.challenges), stringResource(R.string.xp_challenges), Lavender, Modifier.weight(1f))
        KpiTile(stringResource(R.string.xp_total, game.xp.sessions), stringResource(R.string.xp_sessions), Mint, Modifier.weight(1f))
    }
}

@Composable
private fun ChallengeCard(game: GameState) {
    val challenge = game.today.challenge
    Column(
        Modifier
            .padding(top = 12.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Sun)
            .padding(18.dp)
            .semantics(mergeDescendants = true) {}
    ) {
        Text(stringResource(R.string.challenge_title), style = MaterialTheme.typography.labelLarge, color = InkOnPastel.copy(alpha = 0.7f))
        Text(stringResource(challenge.titleRes()), style = MaterialTheme.typography.headlineSmall, color = InkOnPastel, modifier = Modifier.padding(top = 4.dp))
        Text(stringResource(challenge.descriptionRes()), style = MaterialTheme.typography.bodyMedium, color = InkOnPastel, modifier = Modifier.padding(top = 2.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(top = 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (game.today.done) InkOnPastel else Color.White.copy(alpha = 0.6f))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            if (game.today.done) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Lime, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(
                stringResource(if (game.today.done) R.string.challenge_done else R.string.challenge_pending),
                style = MaterialTheme.typography.labelLarge,
                color = if (game.today.done) Color.White else InkOnPastel
            )
        }
    }
}

@Composable
private fun BadgeGrid(badges: List<BadgeProgress>, onSelect: (Badge) -> Unit) {
    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 24.dp, bottom = 10.dp)) {
        Text(
            stringResource(R.string.badges_title),
            style = MaterialTheme.typography.headlineSmall,
            color = StreakTheme.palette.ink,
            modifier = Modifier.weight(1f).semantics { heading() }
        )
        Text(
            stringResource(R.string.badges_count, badges.count { it.earned }, badges.size),
            style = MaterialTheme.typography.labelLarge,
            color = StreakTheme.palette.inkSoft
        )
    }
    // Zemljevid znack po tri v vrsto (zaslon se ze drsi, zato brez lenega seznama)
    badges.chunked(COLUMNS).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(bottom = 10.dp)) {
            row.forEach { BadgeTile(it, Modifier.weight(1f).fillMaxHeight()) { onSelect(it.badge) } }
            repeat(COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun BadgeTile(item: BadgeProgress, modifier: Modifier, onClick: () -> Unit) {
    val palette = StreakTheme.palette
    val title = stringResource(item.badge.titleRes())
    val status = if (item.earned) stringResource(R.string.badge_earned) else progressText(item)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(palette.surfaceSoft)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 6.dp)
            .semantics(mergeDescendants = true) { contentDescription = "$title, $status" }
    ) {
        BadgeCircle(item.badge, item.earned, 58.dp)
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = if (item.earned) palette.ink else palette.inkSoft,
            textAlign = TextAlign.Center,
            maxLines = 2,
            modifier = Modifier.padding(top = 8.dp)
        )
        if (!item.earned) {
            Text(status, style = MaterialTheme.typography.labelSmall, color = palette.inkSoft, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
fun BadgeCircle(badge: Badge, earned: Boolean, size: Dp) {
    val palette = StreakTheme.palette
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (earned) badge.color else palette.border)
    ) {
        Icon(badge.icon, contentDescription = null, tint = if (earned) InkOnPastel else palette.inkSoft, modifier = Modifier.size(size * 0.5f))
    }
}

@Composable
private fun BadgeSheet(item: BadgeProgress, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val badge = item.badge
    val title = stringResource(badge.titleRes())
    val description = badge.description()
    val label = stringResource(R.string.new_badge)
    val shareText = stringResource(R.string.share_badge, title)
    StreakSheet(onDismiss = onDismiss, title = title) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            BadgeCircle(badge, item.earned, 96.dp)
            Text(
                description,
                style = MaterialTheme.typography.bodyLarge,
                color = StreakTheme.palette.ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 14.dp)
            )
            if (item.earned) {
                Text(stringResource(R.string.badge_earned), style = MaterialTheme.typography.labelLarge, color = StreakTheme.palette.inkSoft, modifier = Modifier.padding(top = 6.dp))
                StreakButton(
                    stringResource(R.string.share_achievement),
                    { scope.launch { ShareCard.share(context, badgeShareSpec(context, badge, label, description, shareText)) } },
                    variant = ButtonVariant.Primary,
                    icon = Icons.Rounded.IosShare,
                    modifier = Modifier.padding(top = 20.dp)
                )
            } else {
                ProgressTrack(item.progress, Modifier.padding(top = 16.dp).fillMaxWidth(), track = StreakTheme.palette.border)
                Text(
                    progressText(item),
                    style = MaterialTheme.typography.labelLarge,
                    color = StreakTheme.palette.inkSoft,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

// Napredek do znacke; cas seje v urah
@Composable
private fun progressText(item: BadgeProgress): String {
    val current = item.current.coerceAtMost(item.badge.target)
    return if (item.badge == Badge.FOCUS_10H) stringResource(R.string.badge_progress_hours, current / 60, item.badge.target / 60)
    else stringResource(R.string.badge_progress, current, item.badge.target)
}

fun badgeShareSpec(context: Context, badge: Badge, label: String, description: String, shareText: String) = ShareCardSpec(
    label = label.uppercase(),
    title = context.getString(badge.titleRes()),
    subtitle = description,
    badgeColor = badge.color,
    icon = badge.icon,
    shareText = shareText
)

@Composable
private fun XpHelp() {
    val palette = StreakTheme.palette
    Column(
        Modifier
            .padding(top = 14.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(palette.surfaceSoft)
            .padding(18.dp)
    ) {
        Text(stringResource(R.string.xp_how_title), style = MaterialTheme.typography.titleMedium, color = palette.ink)
        listOf(R.string.xp_how_done, R.string.xp_how_perfect, R.string.xp_how_challenge, R.string.xp_how_session).forEach {
            Text(stringResource(it), style = MaterialTheme.typography.bodyMedium, color = palette.ink, modifier = Modifier.padding(top = 6.dp))
        }
        Text(stringResource(R.string.xp_how_note), style = MaterialTheme.typography.bodySmall, color = palette.inkSoft, modifier = Modifier.padding(top = 10.dp))
    }
}

private const val COLUMNS = 3
