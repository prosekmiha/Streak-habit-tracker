package com.streak.app.ui.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Password
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.streak.app.R
import com.streak.app.domain.lock.PinRules
import com.streak.app.domain.lock.RecoveryCode
import com.streak.app.lock.AppLockManager
import com.streak.app.lock.BiometricAuth
import com.streak.app.ui.components.OptionSheet
import com.streak.app.ui.components.ScreenTopBar
import com.streak.app.ui.components.SegmentedControl
import com.streak.app.ui.components.SettingsGroup
import com.streak.app.ui.components.SettingsRow
import com.streak.app.ui.components.StreakButton
import com.streak.app.ui.components.StreakSheet
import com.streak.app.ui.components.StreakSwitch
import com.streak.app.ui.theme.Bubblegum
import com.streak.app.ui.theme.Lavender
import com.streak.app.ui.theme.Lime
import com.streak.app.ui.theme.Mint
import com.streak.app.ui.theme.Sky
import com.streak.app.ui.theme.StreakTheme
import com.streak.app.ui.theme.Sun

// Zaklep in zasebnost: koda, biometrija, samodejni zaklep, obnovitvena koda, zasebni zaslon
@Composable
fun SecurityScreen(onBack: () -> Unit, viewModel: SecurityViewModel = hiltViewModel()) {
    val config by viewModel.config.collectAsStateWithLifecycle()
    val flow by viewModel.flow.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val biometricAvailable = remember { BiometricAuth.available(context) }
    var pickTimeout by remember { mutableStateOf(false) }

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
        ScreenTopBar(stringResource(R.string.security_title), Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), onBack)

        SettingsGroup {
            ToggleRow(
                stringResource(R.string.security_lock), Icons.Rounded.Lock, Bubblegum, config.enabled,
                { on -> if (on) viewModel.startEnable() else viewModel.startVerify(VerifyPurpose.DISABLE) },
                showDivider = config.enabled
            )
            if (config.enabled) {
                SettingsRow(stringResource(R.string.security_change_pin), Icons.Rounded.Password, Sun, { viewModel.startVerify(VerifyPurpose.CHANGE_PIN) })
                if (biometricAvailable) {
                    ToggleRow(stringResource(R.string.security_biometric), Icons.Rounded.Fingerprint, Mint, config.biometric, { viewModel.setBiometric(it) })
                }
                SettingsRow(stringResource(R.string.security_timeout), Icons.Rounded.Timer, Sky, { pickTimeout = true }, timeoutLabel(config.timeoutMs))
                SettingsRow(stringResource(R.string.security_new_recovery), Icons.Rounded.Key, Lime, { viewModel.startVerify(VerifyPurpose.NEW_RECOVERY) }, showDivider = false)
            }
        }
        Hint(stringResource(if (config.enabled && !biometricAvailable) R.string.security_lock_hint_no_bio else R.string.security_lock_hint))

        SettingsGroup {
            ToggleRow(stringResource(R.string.security_privacy_screen), Icons.Rounded.VisibilityOff, Lavender, config.privacyScreen, { viewModel.setPrivacyScreen(it) }, showDivider = false)
        }
        Hint(stringResource(R.string.security_privacy_hint))
        Hint(stringResource(R.string.security_encryption_hint))
    }

    if (pickTimeout) {
        OptionSheet(
            title = stringResource(R.string.security_timeout),
            options = AppLockManager.TIMEOUTS,
            selected = config.timeoutMs,
            label = { timeoutLabel(it) },
            onSelect = viewModel::setTimeout,
            onDismiss = { pickTimeout = false }
        )
    }

    when (val step = flow) {
        is SecurityFlow.Verify -> StreakSheet(onDismiss = viewModel::close, title = stringResource(R.string.security_verify_title)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                SheetMessage(
                    when (step.error) {
                        PinError.WRONG -> stringResource(R.string.security_wrong_pin)
                        PinError.LOCKED_OUT -> stringResource(R.string.security_locked_out)
                        else -> stringResource(R.string.security_verify_text)
                    },
                    isError = step.error != null
                )
                PinPad(config.pinLength, step.pin, viewModel::setPin, viewModel::verify, errorKey = step.errorKey)
            }
        }
        is SecurityFlow.NewPin -> StreakSheet(onDismiss = viewModel::close, title = stringResource(if (step.first == null) R.string.security_new_pin_title else R.string.security_confirm_pin_title)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                if (step.first == null) {
                    SegmentedControl(
                        PinRules.LENGTHS, step.length,
                        { pluralStringResource(R.plurals.security_digits, it, it) },
                        viewModel::setLength,
                        height = 38.dp
                    )
                }
                SheetMessage(
                    when (step.error) {
                        PinError.WEAK -> stringResource(R.string.security_weak_pin)
                        PinError.MISMATCH -> stringResource(R.string.security_mismatch)
                        else -> stringResource(if (step.first == null) R.string.security_new_pin_text else R.string.security_confirm_pin_text)
                    },
                    isError = step.error != null
                )
                PinPad(step.length, step.pin, viewModel::setPin, viewModel::newPin, errorKey = step.errorKey)
            }
        }
        is SecurityFlow.ShowRecovery -> StreakSheet(onDismiss = {}, title = stringResource(R.string.security_recovery_title)) {
            Text(stringResource(R.string.security_recovery_text), style = MaterialTheme.typography.bodyMedium, color = StreakTheme.palette.inkSoft)
            Text(
                RecoveryCode.format(step.code),
                style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Monospace, letterSpacing = 2.sp),
                color = StreakTheme.palette.ink,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(StreakTheme.palette.surfaceSoft)
                    .padding(vertical = 18.dp)
            )
            StreakButton(stringResource(R.string.security_recovery_done), viewModel::close, Modifier.padding(top = 20.dp).fillMaxWidth())
        }
        SecurityFlow.None -> Unit
    }
}

@Composable
private fun SheetMessage(text: String, isError: Boolean) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = if (isError) StreakTheme.palette.danger else StreakTheme.palette.inkSoft,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 12.dp).semantics { liveRegion = LiveRegionMode.Polite }
    )
}

@Composable
private fun timeoutLabel(ms: Long): String = when (ms) {
    0L -> stringResource(R.string.security_timeout_now)
    else -> pluralStringResource(R.plurals.security_timeout_minutes, (ms / 60_000).toInt(), (ms / 60_000).toInt())
}

@Composable
private fun ToggleRow(title: String, icon: ImageVector, iconBg: Color, checked: Boolean, onChange: (Boolean) -> Unit, showDivider: Boolean = true) {
    SettingsRow(title, icon, iconBg, { onChange(!checked) }, showDivider = showDivider) {
        StreakSwitch(checked, onChange, contentDescription = title)
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = StreakTheme.palette.inkSoft, modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 8.dp))
}
