package com.streak.app.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.streak.app.R
import com.streak.app.domain.model.AnimationLevel
import com.streak.app.domain.model.StreakUnit
import com.streak.app.ui.components.ButtonVariant
import com.streak.app.ui.components.StreakButton
import com.streak.app.ui.components.TextAction
import com.streak.app.ui.achievements.ShareCard
import com.streak.app.ui.achievements.ShareCardSpec
import com.streak.app.ui.mascot.MascotMood
import com.streak.app.ui.mascot.MascotView
import com.streak.app.ui.theme.Blurple
import com.streak.app.ui.theme.InkOnPastel
import com.streak.app.ui.theme.LocalAnimationLevel
import com.streak.app.ui.theme.Sun
import kotlinx.coroutines.launch
import kotlin.math.sin

// Celozaslonsko praznovanje mejnika verige
@Composable
fun CelebrationOverlay(celebration: Celebration, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val unitLabel = stringResource(
        when (celebration.unit) {
            StreakUnit.DAY -> R.string.badge_days
            StreakUnit.WEEK -> R.string.badge_weeks
            StreakUnit.MONTH -> R.string.badge_months
        }
    )
    val streak = streakText(celebration.count, celebration.unit)
    val label = stringResource(R.string.new_milestone)
    val text = stringResource(R.string.milestone_text, celebration.habitTitle)
    val shareText = stringResource(R.string.share_milestone, celebration.habitTitle, streak)
    CelebrationDialog(
        label = label,
        title = "$streak!",
        text = text,
        badge = {
            Text(celebration.count.toString(), style = MaterialTheme.typography.displaySmall, color = InkOnPastel)
            Text(unitLabel, style = MaterialTheme.typography.labelMedium, color = InkOnPastel)
        },
        badgeColor = Sun,
        onShare = {
            ShareCard.share(context, ShareCardSpec(label.uppercase(), "$streak!", celebration.habitTitle, Sun, badgeText = celebration.count.toString(), shareText = shareText))
        },
        onDismiss = onDismiss
    )
}

// Skupna postavitev praznovanja: konfeti, maskota, okrogla znacka, naslov, deljenje (slika)
@Composable
fun CelebrationDialog(
    label: String,
    title: String,
    text: String,
    badge: @Composable ColumnScope.() -> Unit,
    badgeColor: Color,
    onShare: suspend () -> Unit,
    onDismiss: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(title) { haptics.performHapticFeedback(HapticFeedbackType.LongPress) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(Modifier.fillMaxSize().background(Blurple)) {
            Confetti(Modifier.fillMaxSize())
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 28.dp, vertical = 24.dp)
            ) {
                Text(
                    label.uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    modifier = Modifier
                        .padding(top = 24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = .16f))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
                Box(Modifier.padding(top = 16.dp).size(width = 300.dp, height = 320.dp)) {
                    MascotView(MascotMood.CHEER, Modifier.align(Alignment.BottomStart).size(width = 280.dp, height = 303.dp), hero = true)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-6).dp, y = 4.dp)
                            .rotate(10f)
                            .size(104.dp)
                            .clip(CircleShape)
                            .background(badgeColor)
                            .border(5.dp, InkOnPastel, CircleShape),
                        content = badge
                    )
                }
                Text(
                    title,
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 16.dp).semantics { heading() }
                )
                Text(
                    text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFE4E5FB),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 10.dp)
                )
                Spacer(Modifier.weight(1f))
                StreakButton(
                    stringResource(R.string.share_achievement),
                    { scope.launch { onShare() } },
                    variant = ButtonVariant.White,
                    icon = Icons.Rounded.IosShare,
                    height = 58.dp
                )
                TextAction(stringResource(R.string.continue_action), onDismiss, color = Color.White, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}

// Padajoci konfeti (staticni, ce so animacije izklopljene)
@Composable
private fun Confetti(modifier: Modifier) {
    val animated = LocalAnimationLevel.current == AnimationLevel.FULL
    val t = if (animated) {
        val transition = rememberInfiniteTransition(label = "confetti")
        val v by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(5000, easing = LinearEasing)), label = "t")
        v
    } else 0.3f
    val colors = listOf(Color(0xFFF7CD63), Color(0xFFFC8FC6), Color(0xFFB8EB6C), Color(0xFFA9C8F5), Color(0xFFFFB38A))
    Canvas(modifier) {
        for (i in 0 until 28) {
            val seed = i * 37 % 100 / 100f
            val x = size.width * ((i * 0.137f + seed * 0.3f) % 1f)
            val y = size.height * ((seed + t * (0.6f + seed * 0.4f) + i * 0.05f) % 1f)
            val angle = (t * 360f * (if (i % 2 == 0) 1 else -1)) + i * 23f
            rotate(angle, Offset(x, y)) {
                if (i % 3 == 0) drawCircle(colors[i % colors.size], 10f, Offset(x, y))
                else drawRoundRect(colors[i % colors.size], Offset(x - 16f, y - 9f), Size(32f, 18f), CornerRadius(6f))
            }
        }
        // Nezno utripanje sredine
        drawCircle(Color.White.copy(alpha = 0.04f + 0.02f * sin(t * 6.28f)), size.minDimension * 0.45f, Offset(size.width / 2, size.height * 0.38f))
    }
}
