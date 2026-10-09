package com.streak.app.ui.onboarding

import android.Manifest
import android.app.Activity
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.streak.app.R
import com.streak.app.ui.components.IconMap
import com.streak.app.ui.components.PillButton
import com.streak.app.ui.components.StreakTextField
import com.streak.app.ui.components.WhitePillButton
import com.streak.app.ui.components.parseHexColor
import com.streak.app.ui.mascot.MascotMood
import com.streak.app.ui.mascot.MascotView
import com.streak.app.ui.theme.Blurple
import com.streak.app.ui.theme.InkOnPastel
import com.streak.app.ui.theme.StreakTheme

@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BackHandler(enabled = state.step != OnboardingStep.WELCOME) { viewModel.back() }

    AnimatedContent(targetState = state.step, label = "onboarding") { step ->
        when (step) {
            OnboardingStep.WELCOME -> WelcomeStep(onStart = { viewModel.goTo(OnboardingStep.NAME) })
            OnboardingStep.NAME -> NameStep(
                name = state.name,
                onNameChange = viewModel::setName,
                onBack = { viewModel.back() },
                onNext = { viewModel.goTo(OnboardingStep.NOTIFICATIONS) }
            )
            OnboardingStep.NOTIFICATIONS -> NotificationsStep(
                onBack = { viewModel.back() },
                onNext = { viewModel.goTo(OnboardingStep.TEMPLATES) }
            )
            OnboardingStep.TEMPLATES -> TemplatesStep(
                selected = state.selectedTemplates,
                saving = state.saving,
                onToggle = viewModel::toggleTemplate,
                onBack = { viewModel.back() },
                onFinish = { viewModel.finish(withTemplates = true) },
                onSkip = { viewModel.finish(withTemplates = false) }
            )
        }
    }
}

@Composable
private fun WelcomeStep(onStart: () -> Unit) {
    // Na modrem ozadju svetle ikone statusne vrstice
    // (SideEffect, ker tema ob vsaki rekompoziciji nastavi svojo vrednost)
    val view = LocalView.current
    val controller = remember(view) { WindowCompat.getInsetsController((view.context as Activity).window, view) }
    SideEffect { controller.isAppearanceLightStatusBars = false }
    val themeLightBars = MaterialTheme.colorScheme.background.luminance() > 0.5f
    DisposableEffect(controller) {
        onDispose { controller.isAppearanceLightStatusBars = themeLightBars }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Blurple)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))
        Text(
            stringResource(R.string.onboarding_title),
            style = MaterialTheme.typography.displayLarge,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() }
        )
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            MascotView(MascotMood.WAVE, Modifier.fillMaxWidth(0.8f).heightIn(max = 320.dp))
        }
        WhitePillButton(stringResource(R.string.onboarding_start), onStart)
        Text(
            stringResource(R.string.onboarding_privacy),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 20.dp)
        )
    }
}

@Composable
private fun StepScaffold(
    onBack: () -> Unit,
    title: String,
    subtitle: String,
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
        IconButton(onClick = onBack, modifier = Modifier.padding(top = 8.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
        }
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 8.dp).semantics { heading() }
        )
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = StreakTheme.palette.inkSoft,
            modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
        )
        Box(Modifier.weight(1f).fillMaxWidth()) { content() }
        Column(Modifier.padding(vertical = 16.dp)) { bottom() }
    }
}

@Composable
private fun NameStep(name: String, onNameChange: (String) -> Unit, onBack: () -> Unit, onNext: () -> Unit) {
    StepScaffold(
        onBack = onBack,
        title = stringResource(R.string.onboarding_name_title),
        subtitle = stringResource(R.string.onboarding_name_subtitle),
        bottom = { PillButton(stringResource(R.string.next), onNext) }
    ) {
        StreakTextField(
            value = name,
            onValueChange = onNameChange,
            label = stringResource(R.string.onboarding_name_label),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { onNext() })
        )
    }
}

@Composable
private fun NotificationsStep(onBack: () -> Unit, onNext: () -> Unit) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { onNext() }
    StepScaffold(
        onBack = onBack,
        title = stringResource(R.string.onboarding_notif_title),
        subtitle = stringResource(R.string.onboarding_notif_subtitle),
        bottom = {
            PillButton(stringResource(R.string.onboarding_notif_allow), onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else onNext()
            })
            androidx.compose.material3.TextButton(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            ) { Text(stringResource(R.string.not_now), style = MaterialTheme.typography.labelLarge) }
        }
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            MascotView(MascotMood.THINKING, Modifier.fillMaxWidth(0.7f))
        }
    }
}

@Composable
private fun TemplatesStep(
    selected: Set<String>,
    saving: Boolean,
    onToggle: (String) -> Unit,
    onBack: () -> Unit,
    onFinish: () -> Unit,
    onSkip: () -> Unit
) {
    StepScaffold(
        onBack = onBack,
        title = stringResource(R.string.onboarding_templates_title),
        subtitle = stringResource(R.string.onboarding_templates_subtitle),
        bottom = {
            if (selected.isEmpty()) {
                PillButton(stringResource(R.string.onboarding_templates_skip), onSkip, enabled = !saving)
            } else {
                PillButton(
                    pluralStringResource(R.plurals.onboarding_templates_start, selected.size, selected.size),
                    onFinish,
                    enabled = !saving
                )
            }
        }
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(150.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(HabitTemplates.all, key = { it.id }) { t ->
                TemplateCard(t, t.id in selected) { onToggle(t.id) }
            }
            item(span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.height(4.dp)) }
        }
    }
}

@Composable
private fun TemplateCard(template: HabitTemplate, selected: Boolean, onClick: () -> Unit) {
    val ink = InkOnPastel
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp)
            .background(parseHexColor(template.colorHex), shape)
            .then(if (selected) Modifier.border(3.dp, ink, shape) else Modifier)
            .clickable(role = Role.Checkbox, onClick = onClick)
            .semantics(mergeDescendants = true) { this.selected = selected }
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(IconMap.resolve(template.icon), contentDescription = null, tint = ink, modifier = Modifier.size(24.dp))
            Spacer(Modifier.weight(1f))
            if (selected) {
                Box(Modifier.size(24.dp).background(ink, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            } else {
                Box(Modifier.size(24.dp).border(2.dp, ink, CircleShape))
            }
        }
        Text(stringResource(template.title), style = MaterialTheme.typography.titleMedium, color = ink, modifier = Modifier.padding(top = 16.dp))
        Text(stringResource(template.description), style = MaterialTheme.typography.bodySmall, color = ink.copy(alpha = 0.8f))
    }
}
