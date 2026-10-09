package com.streak.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.streak.app.domain.model.AnimationLevel

private val LightColors = lightColorScheme(
    primary = Blurple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE4E5FB),
    onPrimaryContainer = Color(0xFF1B1F7A),
    secondary = Lime,
    onSecondary = InkOnPastel,
    tertiary = Bubblegum,
    onTertiary = InkOnPastel,
    background = Color.White,
    onBackground = LightPalette.ink,
    surface = Color.White,
    onSurface = LightPalette.ink,
    surfaceVariant = LightPalette.surfaceSoft,
    onSurfaceVariant = LightPalette.inkSoft,
    surfaceContainer = LightPalette.surfaceSoft,
    surfaceContainerHigh = Color.White,
    surfaceContainerLow = Color.White,
    outline = LightPalette.border,
    outlineVariant = LightPalette.border,
    error = Danger,
    onError = Color.White
)

private val DarkColors = darkColorScheme(
    primary = BlurpleLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF2B2F6E),
    onPrimaryContainer = Color(0xFFDADCFF),
    secondary = Lime,
    onSecondary = InkOnPastel,
    tertiary = Bubblegum,
    onTertiary = InkOnPastel,
    background = Color(0xFF121318),
    onBackground = DarkPalette.ink,
    surface = Color(0xFF1C1D24),
    onSurface = DarkPalette.ink,
    surfaceVariant = DarkPalette.surfaceSoft,
    onSurfaceVariant = DarkPalette.inkSoft,
    surfaceContainer = DarkPalette.surfaceSoft,
    surfaceContainerHigh = Color(0xFF1C1D24),
    surfaceContainerLow = Color(0xFF16171D),
    outline = DarkPalette.border,
    outlineVariant = DarkPalette.border,
    error = Color(0xFFFF6B6F),
    onError = Color.White
)

val LocalStreakPalette = staticCompositionLocalOf { LightPalette }

// Ucinkovita raven animacij (nastavitev aplikacije + sistemsko "Odstrani animacije")
val LocalAnimationLevel = staticCompositionLocalOf { AnimationLevel.FULL }

object StreakTheme {
    val palette: StreakPalette
        @Composable get() = LocalStreakPalette.current
}

@Composable
fun StreakTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    animationLevel: AnimationLevel = AnimationLevel.FULL,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val palette = if (darkTheme) DarkPalette else LightPalette

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalStreakPalette provides palette,
        LocalAnimationLevel provides animationLevel
    ) {
        MaterialTheme(colorScheme = colors, typography = StreakTypography, content = content)
    }
}
