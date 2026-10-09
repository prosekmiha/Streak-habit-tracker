package com.streak.app

import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.streak.app.domain.model.AnimationLevel
import com.streak.app.domain.model.AppSettings
import com.streak.app.domain.model.ThemeMode
import com.streak.app.domain.repository.SettingsRepository
import com.streak.app.ui.navigation.StreakApp
import com.streak.app.ui.onboarding.OnboardingScreen
import com.streak.app.ui.theme.StreakTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settingsRepository: SettingsRepository

    // null, dokler se nastavitve ne nalozijo (splash ostane, da ne utripne onboarding)
    private val settings = MutableStateFlow<AppSettings?>(null)
    private val loadedSettings: StateFlow<AppSettings?> = settings.asStateFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        splash.setKeepOnScreenCondition { settings.value == null }
        lifecycleScope.launch { settingsRepository.settings.collect { settings.value = it } }

        setContent {
            val current by loadedSettings.collectAsStateWithLifecycle()
            val s = current ?: return@setContent
            val dark = when (s.themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            StreakTheme(darkTheme = dark, animationLevel = effectiveAnimationLevel(s.animationLevel)) {
                if (s.onboardingDone) StreakApp(userName = s.userName) else OnboardingScreen()
            }
        }
    }

    // Sistemska nastavitev "Odstrani animacije" prevlada nad nastavitvijo aplikacije
    private fun effectiveAnimationLevel(level: AnimationLevel): AnimationLevel {
        val scale = Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        return if (scale == 0f) AnimationLevel.OFF else level
    }
}
