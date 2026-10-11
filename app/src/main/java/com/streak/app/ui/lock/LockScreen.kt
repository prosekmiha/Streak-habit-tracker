package com.streak.app.ui.lock

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.streak.app.R
import com.streak.app.lock.BiometricAuth
import com.streak.app.ui.components.StreakButton
import com.streak.app.ui.components.StreakSheet
import com.streak.app.ui.components.StreakTextField
import com.streak.app.ui.components.TextAction
import com.streak.app.ui.mascot.MascotMood
import com.streak.app.ui.mascot.MascotView
import com.streak.app.ui.theme.Blurple
import com.streak.app.ui.theme.StreakTheme

// Zaklenjen zaslon: prekrije aplikacijo, dokler uporabnik ne vpise kode ali se ne
// potrdi z biometrijo. Vsebina pod njim je skrita tudi bralnikom zaslona.
@Composable
fun LockScreen(onExitApp: () -> Unit, viewModel: LockViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val config by viewModel.config.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    var recoveryOpen by remember { mutableStateOf(false) }
    var autoPrompted by rememberSaveable { mutableStateOf(false) }
    val biometricTitle = stringResource(R.string.lock_biometric_title)
    val usePin = stringResource(R.string.lock_use_pin)
    val canBiometric = config.biometric && activity != null && BiometricAuth.available(context)

    fun promptBiometric() {
        if (activity != null) BiometricAuth.prompt(activity, biometricTitle, usePin, onSuccess = viewModel::biometricSucceeded)
    }
    // Ob prikazu takoj ponudi biometrijo (enkrat na zaklep)
    LaunchedEffect(canBiometric) {
        if (canBiometric && !autoPrompted && state.lockoutSeconds == 0L) {
            autoPrompted = true
            promptBiometric()
        }
    }
    BackHandler(onBack = onExitApp)

    val locked = state.lockoutSeconds > 0
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .background(Blurple)
            // Dotiki ne pridejo do vsebine pod zaklepom
            .clickable(interactionSource = null, indication = null, onClick = {})
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Spacer(Modifier.weight(0.6f))
        MascotView(MascotMood.SLEEPING, Modifier.size(width = 120.dp, height = 130.dp))
        Text(
            stringResource(R.string.lock_title),
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp).semantics { heading() }
        )
        val message = when {
            locked -> stringResource(R.string.lock_wait, state.lockoutSeconds / 60, state.lockoutSeconds % 60)
            state.freeAttemptsLeft != null && state.freeAttemptsLeft!! > 0 ->
                pluralStringResource(R.plurals.lock_wrong, state.freeAttemptsLeft!!, state.freeAttemptsLeft!!)
            state.freeAttemptsLeft != null -> stringResource(R.string.lock_wrong_last)
            else -> stringResource(R.string.lock_subtitle)
        }
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = .85f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp).semantics { liveRegion = LiveRegionMode.Polite }
        )
        PinPad(
            length = config.pinLength,
            value = state.pin,
            onValueChange = viewModel::setPin,
            onComplete = viewModel::submit,
            enabled = !locked && !state.busy,
            colors = pinPadColors(onBrand = true),
            errorKey = state.errorKey,
            onBiometric = if (canBiometric) ({ promptBiometric() }) else null,
            modifier = Modifier.padding(top = 8.dp)
        )
        Spacer(Modifier.weight(1f))
        TextAction(stringResource(R.string.lock_forgot), { recoveryOpen = true }, color = Color.White)
        Spacer(Modifier.height(8.dp))
    }

    if (recoveryOpen) {
        RecoverySheet(
            error = state.recoveryError,
            locked = locked,
            onSubmit = { code -> viewModel.useRecovery(code) { recoveryOpen = false } },
            onDismiss = { recoveryOpen = false }
        )
    }
}

@Composable
private fun RecoverySheet(error: Boolean, locked: Boolean, onSubmit: (String) -> Unit, onDismiss: () -> Unit) {
    var code by remember { mutableStateOf("") }
    StreakSheet(onDismiss = onDismiss, title = stringResource(R.string.lock_recovery_title)) {
        Text(stringResource(R.string.lock_recovery_text), style = MaterialTheme.typography.bodyMedium, color = StreakTheme.palette.inkSoft)
        StreakTextField(
            value = code,
            onValueChange = { code = it.take(20) },
            label = stringResource(R.string.lock_recovery_label),
            placeholder = "XXXX-XXXX-XXXX",
            isError = error,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, autoCorrectEnabled = false)
        )
        if (error) {
            Text(stringResource(R.string.lock_recovery_wrong), style = MaterialTheme.typography.bodySmall, color = StreakTheme.palette.danger, modifier = Modifier.padding(top = 8.dp))
        }
        Text(stringResource(R.string.lock_recovery_none), style = MaterialTheme.typography.bodySmall, color = StreakTheme.palette.inkSoft, modifier = Modifier.padding(top = 12.dp))
        StreakButton(
            stringResource(R.string.lock_recovery_submit),
            { onSubmit(code) },
            Modifier.padding(top = 18.dp).fillMaxWidth(),
            enabled = code.isNotBlank() && !locked
        )
    }
}

// Aplikacija pod zaklepom ostane sestavljena (navigacija se ohrani), a je skrita
// bralnikom zaslona in nedosegljiva za dotike
@Composable
fun LockOverlayHost(locked: Boolean, onExitApp: () -> Unit, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().then(if (locked) Modifier.clearAndSetSemantics { } else Modifier)) { content() }
        if (locked) LockScreen(onExitApp)
    }
}
