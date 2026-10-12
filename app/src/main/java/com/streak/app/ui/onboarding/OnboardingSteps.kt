package com.streak.app.ui.onboarding

import android.Manifest
import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.streak.app.R
import com.streak.app.domain.model.GoalType
import com.streak.app.domain.model.ScheduleType
import com.streak.app.domain.model.TimeOfDay
import com.streak.app.ui.components.ButtonVariant
import com.streak.app.ui.components.IconMap
import com.streak.app.ui.components.RoundIconButton
import com.streak.app.ui.components.StepIndicator
import com.streak.app.ui.components.StreakButton
import com.streak.app.ui.components.StreakTextField
import com.streak.app.ui.components.TextAction
import com.streak.app.ui.components.parseHexColor
import com.streak.app.ui.mascot.MascotMood
import com.streak.app.ui.mascot.MascotView
import com.streak.app.ui.theme.Blurple
import com.streak.app.ui.theme.InkOnPastel
import com.streak.app.ui.theme.Lavender
import com.streak.app.ui.theme.Lime
import com.streak.app.ui.theme.Sky
import com.streak.app.ui.theme.StreakTheme
import com.streak.app.ui.theme.Sun

// Koraki uvajanja (dobrodoslica, ime, obvestila, predloge)

@Composable
internal fun WelcomeStep(onStart: () -> Unit, onRestore: () -> Unit) {
    LightStatusBarIcons()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .background(Blurple)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp, vertical = 24.dp)
    ) {
        Spacer(Modifier.height(36.dp))
        Text(
            stringResource(R.string.onboarding_title),
            style = MaterialTheme.typography.displayLarge,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() }
        )
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            // Zelena "trata" pod maskoto kot v dizajnu
            Box(
                Modifier
                    .offset(y = 92.dp)
                    .size(width = 250.dp, height = 104.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Lime)
                    .border(5.dp, InkOnPastel, RoundedCornerShape(50))
            )
            MascotView(MascotMood.WAVE, Modifier.size(width = 270.dp, height = 292.dp), hero = true)
        }
        StreakButton(stringResource(R.string.onboarding_start), onStart, variant = ButtonVariant.White, height = 58.dp)
        TextAction(stringResource(R.string.onboarding_restore), onRestore, color = Color.White, modifier = Modifier.padding(top = 6.dp))
        Text(
            stringResource(R.string.onboarding_privacy),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 18.dp)
        )
    }
}

// Na modrem ozadju svetle ikone statusne vrstice (tema jih sicer prepise ob rekompoziciji)
@Composable
internal fun LightStatusBarIcons() {
    val view = LocalView.current
    val controller = remember(view) { WindowCompat.getInsetsController((view.context as Activity).window, view) }
    SideEffect { controller.isAppearanceLightStatusBars = false }
    val themeLightBars = MaterialTheme.colorScheme.background.luminance() > 0.5f
    DisposableEffect(controller) {
        onDispose { controller.isAppearanceLightStatusBars = themeLightBars }
    }
}

@Composable
internal fun StepScaffold(
    step: Int,
    onBack: () -> Unit,
    onSkip: (() -> Unit)?,
    bottom: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 10.dp)) {
            RoundIconButton(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), onBack)
            StepIndicator(
                step = step,
                total = 3,
                description = stringResource(R.string.onboarding_step, step, 3),
                modifier = Modifier.weight(1f).padding(horizontal = 14.dp)
            )
            if (onSkip != null) TextAction(stringResource(R.string.skip), onSkip) else Spacer(Modifier.width(60.dp))
        }
        Box(Modifier.weight(1f).fillMaxWidth()) { content() }
        Column(Modifier.padding(vertical = 16.dp)) { bottom() }
    }
}

@Composable
internal fun StepTitle(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(title, style = MaterialTheme.typography.displaySmall, color = StreakTheme.palette.ink, modifier = Modifier.semantics { heading() })
        Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = StreakTheme.palette.inkSoft, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
internal fun NameStep(name: String, onNameChange: (String) -> Unit, onBack: () -> Unit, onNext: () -> Unit) {
    StepScaffold(
        step = 1,
        onBack = onBack,
        onSkip = onNext,
        bottom = { StreakButton(stringResource(R.string.next), onNext, height = 58.dp) }
    ) {
        Column(Modifier.padding(top = 32.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                StepTitle(stringResource(R.string.onboarding_name_title), stringResource(R.string.onboarding_name_subtitle), Modifier.weight(1f))
                MascotView(MascotMood.WAVE, Modifier.size(width = 92.dp, height = 100.dp))
            }
            StreakTextField(
                value = name,
                onValueChange = onNameChange,
                label = stringResource(R.string.onboarding_name_label),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { onNext() }),
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

@Composable
internal fun NotificationsStep(onBack: () -> Unit, onNext: () -> Unit) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { onNext() }
    StepScaffold(
        step = 2,
        onBack = onBack,
        onSkip = null,
        bottom = {
            StreakButton(stringResource(R.string.onboarding_notif_allow), height = 58.dp, onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else onNext()
            })
            TextAction(stringResource(R.string.not_now), onNext, Modifier.fillMaxWidth().padding(top = 6.dp))
        }
    ) {
        Column(Modifier.padding(top = 24.dp)) {
            NotificationPreview()
            Text(
                stringResource(R.string.onboarding_notif_title),
                style = MaterialTheme.typography.displaySmall,
                color = StreakTheme.palette.ink,
                modifier = Modifier.padding(top = 24.dp).semantics { heading() }
            )
            Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(top = 16.dp)) {
                Bullet(Icons.Rounded.Check, Lime, stringResource(R.string.onboarding_notif_point1))
                Bullet(Icons.Rounded.NightsStay, Lavender, stringResource(R.string.onboarding_notif_point2))
                Bullet(Icons.Rounded.Lock, Sky, stringResource(R.string.onboarding_notif_point3))
            }
        }
    }
}

// Rumena kartica s primerom obvestila in maskoto, ki razmislja
@Composable
internal fun NotificationPreview() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(230.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Sun)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(18.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White)
                .border(2.dp, InkOnPastel, RoundedCornerShape(18.dp))
                .padding(12.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Blurple)
            ) { Icon(Icons.Rounded.LocalFireDepartment, null, tint = Sun, modifier = Modifier.size(20.dp)) }
            Column(Modifier.padding(start = 12.dp)) {
                Text(stringResource(R.string.onboarding_notif_sample_title), style = MaterialTheme.typography.titleSmall, color = InkOnPastel)
                Text(stringResource(R.string.onboarding_notif_sample_text), style = MaterialTheme.typography.bodySmall, color = Color(0xFF6B6E7B))
            }
        }
        MascotView(
            MascotMood.THINKING,
            Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 14.dp)
                .size(width = 150.dp, height = 162.dp)
        )
    }
}

@Composable
internal fun Bullet(icon: ImageVector, color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(color)
        ) { Icon(icon, null, tint = InkOnPastel, modifier = Modifier.size(18.dp)) }
        Text(text, style = MaterialTheme.typography.titleSmall, color = StreakTheme.palette.ink, modifier = Modifier.padding(start = 12.dp))
    }
}

@Composable
internal fun TemplatesStep(
    selected: Set<String>,
    saving: Boolean,
    onToggle: (String) -> Unit,
    onBack: () -> Unit,
    onFinish: () -> Unit,
    onSkip: () -> Unit
) {
    StepScaffold(
        step = 3,
        onBack = onBack,
        onSkip = null,
        bottom = {
            if (selected.isEmpty()) {
                StreakButton(stringResource(R.string.onboarding_templates_skip), onSkip, enabled = !saving, height = 58.dp)
            } else {
                StreakButton(
                    pluralStringResource(R.plurals.onboarding_templates_start, selected.size, selected.size),
                    onFinish,
                    enabled = !saving,
                    height = 58.dp
                )
            }
        }
    ) {
        Column(Modifier.padding(top = 24.dp)) {
            StepTitle(stringResource(R.string.onboarding_templates_title), stringResource(R.string.onboarding_templates_subtitle))
            LazyVerticalGrid(
                columns = GridCells.Adaptive(150.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(top = 18.dp)
            ) {
                items(HabitTemplates.all, key = { it.id }) { t ->
                    TemplateCard(t, t.id in selected) { onToggle(t.id) }
                }
            }
        }
    }
}

@Composable
internal fun TemplateCard(template: HabitTemplate, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 116.dp)
            .clip(shape)
            .background(parseHexColor(template.colorHex))
            .border(3.dp, if (selected) InkOnPastel else Color.Transparent, shape)
            .toggleable(value = selected, role = Role.Checkbox, onValueChange = { onClick() })
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(IconMap.resolve(template.icon), null, tint = InkOnPastel, modifier = Modifier.size(22.dp))
            Text(
                templateTag(template),
                style = MaterialTheme.typography.labelSmall,
                color = InkOnPastel,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = .55f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )
            Spacer(Modifier.weight(1f))
            if (selected) {
                Box(Modifier.size(24.dp).clip(CircleShape).background(InkOnPastel), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            } else {
                Box(Modifier.size(24.dp).border(2.5.dp, InkOnPastel, CircleShape))
            }
        }
        Text(stringResource(template.title), style = MaterialTheme.typography.titleMedium, color = InkOnPastel, modifier = Modifier.padding(top = 14.dp))
        Text(stringResource(template.description), style = MaterialTheme.typography.bodySmall, color = InkOnPastel.copy(alpha = .8f))
    }
}

@Composable
internal fun templateTag(t: HabitTemplate): String = when {
    t.goalType == GoalType.QUANTITY -> stringResource(R.string.goal_quantity)
    t.goalType == GoalType.QUIT -> stringResource(R.string.goal_quit)
    t.goalType == GoalType.DURATION -> stringResource(R.string.tag_session)
    t.schedule.type == ScheduleType.TIMES_PER_WEEK -> stringResource(R.string.times_per_week_value, t.schedule.timesPerPeriod)
    t.timeOfDay == TimeOfDay.MORNING -> stringResource(R.string.filter_morning)
    t.timeOfDay == TimeOfDay.EVENING -> stringResource(R.string.filter_evening)
    else -> stringResource(R.string.interval_daily)
}
