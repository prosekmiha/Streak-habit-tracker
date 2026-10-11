package com.streak.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.sp
import com.streak.app.R

// Naslovni font: Roboto Flex (OFL) v siroki, krepki razlicici - blizu sloga dizajna.
// Besedilo: Manrope (OFL). Oba pokrivata latinico, cirilico in grscino.
@OptIn(ExperimentalTextApi::class)
private fun display(weight: Int, width: Float, fontWeight: FontWeight) = Font(
    R.font.streak_display,
    fontWeight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight), FontVariation.width(width))
)

@OptIn(ExperimentalTextApi::class)
private fun text(weight: Int, fontWeight: FontWeight) = Font(
    R.font.streak_text,
    fontWeight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight))
)

val DisplayFamily = FontFamily(
    display(750, 135f, FontWeight.Bold),
    display(850, 145f, FontWeight.ExtraBold),
    display(950, 151f, FontWeight.Black)
)

val TextFamily = FontFamily(
    text(400, FontWeight.Normal),
    text(500, FontWeight.Medium),
    text(600, FontWeight.SemiBold),
    text(700, FontWeight.Bold),
    text(800, FontWeight.ExtraBold)
)

private val BaseTypography = Typography(
    // Onboarding naslov
    displayLarge = TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight.Black, fontSize = 34.sp, lineHeight = 38.sp),
    // Ime v pozdravu, stevec seje
    displayMedium = TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight.Black, fontSize = 36.sp, lineHeight = 40.sp),
    displaySmall = TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight.Black, fontSize = 30.sp, lineHeight = 34.sp),
    headlineMedium = TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, lineHeight = 28.sp),
    headlineSmall = TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, lineHeight = 24.sp),
    titleLarge = TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 22.sp),
    titleMedium = TextStyle(fontFamily = TextFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 21.sp),
    titleSmall = TextStyle(fontFamily = TextFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 18.sp),
    bodyLarge = TextStyle(fontFamily = TextFamily, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = TextFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 19.sp),
    bodySmall = TextStyle(fontFamily = TextFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = TextFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = TextFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = TextFamily, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 14.sp)
)

// Deljenje besed povsod: pri veliki pisavi (do 200 %) in v jezikih z dolgimi besedami
// se besede ne lomijo sredi zloga
private fun TextStyle.hyphenated() = copy(hyphens = Hyphens.Auto, lineBreak = LineBreak.Paragraph)

val StreakTypography = with(BaseTypography) {
    Typography(
        displayLarge = displayLarge.hyphenated(), displayMedium = displayMedium.hyphenated(), displaySmall = displaySmall.hyphenated(),
        headlineLarge = headlineLarge.hyphenated(), headlineMedium = headlineMedium.hyphenated(), headlineSmall = headlineSmall.hyphenated(),
        titleLarge = titleLarge.hyphenated(), titleMedium = titleMedium.hyphenated(), titleSmall = titleSmall.hyphenated(),
        bodyLarge = bodyLarge.hyphenated(), bodyMedium = bodyMedium.hyphenated(), bodySmall = bodySmall.hyphenated(),
        labelLarge = labelLarge.hyphenated(), labelMedium = labelMedium.hyphenated(), labelSmall = labelSmall.hyphenated()
    )
}
