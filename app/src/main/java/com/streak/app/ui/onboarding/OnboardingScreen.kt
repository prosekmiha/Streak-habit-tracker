package com.streak.app.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.streak.app.ui.data.BusyDialog
import com.streak.app.ui.data.DataViewModel
import com.streak.app.ui.data.ImportFlow
import com.streak.app.ui.data.ImportStep

@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel = hiltViewModel(), dataViewModel: DataViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val data by dataViewModel.state.collectAsStateWithLifecycle()
    val openBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { dataViewModel.readImport(it) }
    BackHandler(enabled = state.step != OnboardingStep.WELCOME) { viewModel.back() }

    AnimatedContent(targetState = state.step, label = "onboarding") { step ->
        when (step) {
            OnboardingStep.WELCOME -> WelcomeStep(
                onStart = { viewModel.goTo(OnboardingStep.NAME) },
                onRestore = { openBackup.launch(arrayOf("*/*")) }
            )
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

    // Obnova iz datoteke (preskoci onboarding)
    ImportFlow(
        step = data.import,
        onPassword = dataViewModel::submitPassword,
        onConfirm = dataViewModel::confirmImport,
        onClose = { if (data.import is ImportStep.Done) dataViewModel.finishOnboarding() else dataViewModel.closeImport() }
    )
    if (data.import is ImportStep.Working) BusyDialog()
}
