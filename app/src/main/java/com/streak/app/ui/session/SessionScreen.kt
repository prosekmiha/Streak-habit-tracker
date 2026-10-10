package com.streak.app.ui.session

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.AlarmOn
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.streak.app.R
import com.streak.app.domain.session.AmbientSound
import com.streak.app.ui.components.ButtonVariant
import com.streak.app.ui.components.DialogAction
import com.streak.app.ui.components.OptionSheet
import com.streak.app.ui.components.RoundIconButton
import com.streak.app.ui.components.StreakButton
import com.streak.app.ui.components.StreakDialog
import com.streak.app.ui.mascot.MascotMood
import com.streak.app.ui.mascot.MascotView
import com.streak.app.ui.theme.InkOnPastel
import com.streak.app.ui.theme.Lime

// Zaslon seje po dizajnu: zeleno ozadje, meditirajoca maskota, stevec, dihanje in nasveti
@Composable
fun SessionScreen(onClose: () -> Unit, onOpenSession: (String) -> Unit, viewModel: SessionViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmExit by remember { mutableStateOf(false) }
    var pickAmbient by remember { mutableStateOf(false) }
    KeepScreenOn()
    BackHandler { if (state.elapsedSeconds > 0) confirmExit = true else onClose() }
    LaunchedEffect(state.ended) { if (state.ended) onClose() }
    LifecycleResumeEffect(Unit) {
        viewModel.refreshAlarmPermission()
        onPauseOrDispose { }
    }
    val context = LocalContext.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .background(Lime)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton(Icons.Rounded.Close, stringResource(R.string.close), { if (state.elapsedSeconds > 0) confirmExit = true else onClose() }, container = Color.White.copy(alpha = .5f), tint = InkOnPastel)
            Text(
                state.habit?.title.orEmpty(),
                style = MaterialTheme.typography.titleMedium,
                color = InkOnPastel,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f).semantics { heading() }
            )
            Spacer(Modifier.size(44.dp))
        }

        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            MascotView(MascotMood.MEDITATING, Modifier.size(width = 250.dp, height = 271.dp), hero = true)
        }

        Text(
            formatElapsed(state.elapsedSeconds),
            style = MaterialTheme.typography.displayMedium,
            color = InkOnPastel,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
        )
        if (state.targetSeconds > 0) {
            Text(
                if (state.goalReached) stringResource(R.string.session_goal_reached) else stringResource(R.string.session_goal, state.targetSeconds / 60),
                style = MaterialTheme.typography.labelLarge,
                color = InkOnPastel.copy(alpha = .75f)
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(top = 10.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(InkOnPastel)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(Lime))
            Text(
                stringResource(if (state.breathingIn) R.string.breathe_in else R.string.breathe_out, state.breathingCount),
                style = MaterialTheme.typography.labelLarge,
                color = Lime,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 18.dp)) {
            AmbientRow(state.ambient) { pickAmbient = true }
            Tip(Icons.Rounded.Air, stringResource(R.string.tip_breathing))
            if (state.exactAlarmMissing && !state.goalReached && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Brez dovoljenja lahko opozorilo ob cilju (pri zaklenjenem zaslonu) zamudi
                ActionRow(Icons.Rounded.AlarmOn, stringResource(R.string.session_exact_alarm)) {
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, "package:${context.packageName}".toUri())
                        )
                    }
                }
            } else {
                Tip(Icons.Rounded.WaterDrop, stringResource(R.string.tip_water))
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 18.dp)) {
            val pauseLabel = stringResource(if (state.running) R.string.pause else R.string.resume)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(InkOnPastel)
                    .clickable(role = Role.Button, onClick = viewModel::togglePause)
                    .semantics { contentDescription = pauseLabel }
            ) { Icon(if (state.running) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = Lime, modifier = Modifier.size(26.dp)) }
            StreakButton(stringResource(R.string.finish), { viewModel.finish(save = true) }, Modifier.weight(1f), ButtonVariant.White, height = 58.dp)
        }
    }

    // Izhod z zaslona: seja lahko tece naprej (obvestilo s stoparico)
    if (confirmExit) {
        StreakDialog(
            onDismiss = { confirmExit = false },
            title = stringResource(R.string.session_exit_title),
            text = stringResource(R.string.session_exit_text, formatElapsed(state.elapsedSeconds)),
            mascot = MascotMood.THINKING
        ) {
            DialogAction(stringResource(R.string.session_keep_running), ButtonVariant.Dark) { confirmExit = false; onClose() }
            DialogAction(stringResource(R.string.session_save_exit), ButtonVariant.Secondary) { confirmExit = false; viewModel.finish(save = true) }
            DialogAction(stringResource(R.string.session_discard), ButtonVariant.Danger) { confirmExit = false; viewModel.finish(save = false) }
        }
    }

    state.conflict?.let { other ->
        StreakDialog(
            onDismiss = onClose,
            title = stringResource(R.string.session_conflict_title),
            text = stringResource(R.string.session_conflict_text, other.title),
            mascot = MascotMood.THINKING
        ) {
            DialogAction(stringResource(R.string.session_conflict_open), ButtonVariant.Dark) { onOpenSession(other.habitId) }
            DialogAction(stringResource(R.string.session_conflict_replace), ButtonVariant.Secondary) { viewModel.replaceConflict() }
            DialogAction(stringResource(R.string.cancel), ButtonVariant.Secondary) { onClose() }
        }
    }

    if (pickAmbient) {
        OptionSheet(
            title = stringResource(R.string.ambient_title),
            options = AmbientSound.entries,
            selected = state.ambient,
            label = { ambientLabel(it) },
            onSelect = viewModel::setAmbient,
            onDismiss = { pickAmbient = false },
            description = { if (it == AmbientSound.NONE) null else stringResource(ambientDescription(it)) }
        )
    }
}

@Composable
private fun ActionRow(icon: ImageVector, text: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = .42f))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Icon(icon, null, tint = InkOnPastel, modifier = Modifier.size(20.dp))
        Text(text, style = MaterialTheme.typography.titleSmall, color = InkOnPastel, modifier = Modifier.padding(start = 12.dp).weight(1f))
        Icon(Icons.Rounded.ChevronRight, null, tint = InkOnPastel.copy(alpha = .75f), modifier = Modifier.size(20.dp))
    }
}

// Izbira zvoka ozadja (namesto nasveta o glasbi)
@Composable
private fun AmbientRow(sound: AmbientSound, onClick: () -> Unit) {
    val label = ambientLabel(sound)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = .42f))
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.ambient_title), onClick = onClick)
            .padding(horizontal = 16.dp)
    ) {
        Icon(Icons.Rounded.MusicNote, null, tint = InkOnPastel, modifier = Modifier.size(20.dp))
        Text(
            stringResource(R.string.ambient_title),
            style = MaterialTheme.typography.titleSmall,
            color = InkOnPastel,
            modifier = Modifier.padding(start = 12.dp).weight(1f)
        )
        Text(label, style = MaterialTheme.typography.labelLarge, color = InkOnPastel.copy(alpha = .75f))
        Icon(Icons.Rounded.ChevronRight, null, tint = InkOnPastel.copy(alpha = .75f), modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun ambientLabel(sound: AmbientSound): String = stringResource(
    when (sound) {
        AmbientSound.NONE -> R.string.ambient_none
        AmbientSound.BROWN_NOISE -> R.string.ambient_brown
        AmbientSound.RAIN -> R.string.ambient_rain
        AmbientSound.OCEAN -> R.string.ambient_ocean
        AmbientSound.FIRE -> R.string.ambient_fire
    }
)

private fun ambientDescription(sound: AmbientSound): Int = when (sound) {
    AmbientSound.BROWN_NOISE -> R.string.ambient_brown_desc
    AmbientSound.RAIN -> R.string.ambient_rain_desc
    AmbientSound.OCEAN -> R.string.ambient_ocean_desc
    else -> R.string.ambient_fire_desc
}

@Composable
private fun Tip(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = .42f))
            .padding(horizontal = 16.dp)
    ) {
        Icon(icon, null, tint = InkOnPastel, modifier = Modifier.size(20.dp))
        Text(text, style = MaterialTheme.typography.titleSmall, color = InkOnPastel, modifier = Modifier.padding(start = 12.dp))
    }
}

@Composable
private fun formatElapsed(seconds: Long): String =
    stringResource(R.string.session_elapsed, seconds / 60, seconds % 60)

// Med sejo naj zaslon ostane prizgan
@Composable
private fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
}
