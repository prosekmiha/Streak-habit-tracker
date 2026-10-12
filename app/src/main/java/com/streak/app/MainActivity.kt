package com.streak.app

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.streak.app.dev.DevSeeder
import com.streak.app.domain.model.AnimationLevel
import com.streak.app.domain.model.AppSettings
import com.streak.app.domain.model.ThemeMode
import com.streak.app.domain.repository.SettingsRepository
import com.streak.app.locale.AppLocale
import com.streak.app.ui.navigation.Routes
import com.streak.app.ui.navigation.StreakApp
import com.streak.app.ui.onboarding.OnboardingScreen
import com.streak.app.ui.theme.StreakTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var devSeeder: dagger.Lazy<DevSeeder>

    // null, dokler se nastavitve ne nalozijo (splash ostane, da ne utripne onboarding)
    private val settings = MutableStateFlow<AppSettings?>(null)
    private val loadedSettings: StateFlow<AppSettings?> = settings.asStateFlow()

    // Cilj iz obvestila ali bliznjice, ki ga navigacija se ni obdelala
    private val pendingRoute = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Meritve hitrosti: napolni bazo s testnimi podatki (le debug/benchmark, docs/quality.md)
        if (BuildConfig.SEED_ENABLED && savedInstanceState == null && intent?.getBooleanExtra(EXTRA_SEED, false) == true) {
            seedAndShowResult()
            return
        }
        splash.setKeepOnScreenCondition { settings.value == null }
        lifecycleScope.launch { settingsRepository.settings.collect { settings.value = it } }
        if (savedInstanceState == null) handleIntent(intent)
        // Zasebni zaslon: brez posnetkov zaslona in skrita vsebina v nedavnih aplikacijah
        lifecycleScope.launch {
            settingsRepository.settings.map { it.privacyScreen }.distinctUntilChanged().collect { secure ->
                if (secure) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE) else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) setRecentsScreenshotEnabled(!secure)
            }
        }

        setContent {
            val current by loadedSettings.collectAsStateWithLifecycle()
            val s = current ?: return@setContent
            val route by pendingRoute.collectAsStateWithLifecycle()
            val dark = when (s.themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            StreakTheme(darkTheme = dark, animationLevel = effectiveAnimationLevel(s.animationLevel)) {
                if (s.onboardingDone) {
                    StreakApp(userName = s.userName, pendingRoute = route, onRouteHandled = { pendingRoute.value = null })
                } else {
                    OnboardingScreen()
                }
            }
        }
    }

    private fun seedAndShowResult() {
        val status = MutableStateFlow("SEEDING")
        lifecycleScope.launch {
            val habits = intent.getIntExtra(EXTRA_SEED_HABITS, 200)
            val years = intent.getIntExtra(EXTRA_SEED_YEARS, 5)
            // Datoteka v zunanji mapi aplikacije: meritve (adb shell) jo preverjajo za konec polnjenja
            val marker = java.io.File(getExternalFilesDir(null), "seed_status")
            marker.delete()
            status.value = runCatching { devSeeder.get().seed(habits, years) }.fold({ "SEED_DONE" }, { "SEED_FAILED: ${it.message}" })
            runCatching { marker.writeText(status.value) }
        }
        setContent {
            val text by status.collectAsStateWithLifecycle()
            androidx.compose.material3.Text(text, modifier = androidx.compose.ui.Modifier.statusBarsPadding())
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocale.wrap(newBase))
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val habitId = intent?.getStringExtra(EXTRA_HABIT_ID)
        pendingRoute.value = when (intent?.action) {
            ACTION_OPEN_SESSION, ACTION_START_SESSION -> habitId?.let(Routes::session)
            ACTION_NEW_HABIT -> Routes.addHabit()
            ACTION_OPEN_HABIT -> habitId?.let(Routes::habitDetail)
            ACTION_LOG_MOOD -> Routes.MOOD
            else -> null
        } ?: pendingRoute.value
    }

    // Sistemska nastavitev "Odstrani animacije" prevlada nad nastavitvijo aplikacije
    private fun effectiveAnimationLevel(level: AnimationLevel): AnimationLevel {
        val scale = Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        return if (scale == 0f) AnimationLevel.OFF else level
    }

    companion object {
        const val ACTION_OPEN_SESSION = "com.streak.app.action.OPEN_SESSION"
        const val ACTION_LOG_MOOD = "com.streak.app.action.LOG_MOOD"
        const val ACTION_START_SESSION = "com.streak.app.action.START_SESSION"
        const val ACTION_NEW_HABIT = "com.streak.app.action.NEW_HABIT"
        const val ACTION_OPEN_HABIT = "com.streak.app.action.OPEN_HABIT"
        const val EXTRA_HABIT_ID = "habit_id"
        const val EXTRA_SEED = "dev_seed"
        const val EXTRA_SEED_HABITS = "dev_seed_habits"
        const val EXTRA_SEED_YEARS = "dev_seed_years"
    }
}
