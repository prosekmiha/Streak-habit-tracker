package com.streak.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// ------- Paleta iz dizajna -------
val Blurple = Color(0xFF4E55E0)     // primarna (gumbi, onboarding ozadje)
val BlurpleLight = Color(0xFF7C82F5) // primarna v temni temi
val Lime = Color(0xFFB8EB6C)        // opravljeni dnevi, seja
val Sun = Color(0xFFF7CD63)
val Bubblegum = Color(0xFFFC8FC6)
val Sky = Color(0xFFA9C8F5)
val Mint = Color(0xFFA6E3D8)
val Peach = Color(0xFFFFB38A)
val Lavender = Color(0xFFC8BDF7)
val MascotOrange = Color(0xFFF4874B)
val Danger = Color(0xFFE5484D)

// Crnilo za besedilo na pastelnih karticah (v obeh temah - kontrast WCAG AA)
val InkOnPastel = Color(0xFF111114)

// Pastelne barve kartic navad (shranjene kot hex v bazi)
val HabitColors = listOf(
    "#F7CD63", // sonce
    "#B8EB6C", // limeta
    "#FC8FC6", // roza
    "#A9C8F5", // nebo
    "#FFB38A", // breskev
    "#A6E3D8", // meta
    "#C8BDF7", // sivka
    "#F4874B"  // oranzna
)

// Dodatni barvni tokeni, ki jih Material ColorScheme nima
@Immutable
data class StreakPalette(
    val ink: Color,
    val inkSoft: Color,
    val surfaceSoft: Color,   // vnosna polja, neizbrani dnevi
    val cardDone: Color,      // opravljena kartica (zbledela)
    val cardDoneInk: Color,
    val daySelected: Color,   // izbran dan v tedenskem traku
    val daySelectedInk: Color,
    val dayDone: Color,
    val border: Color
)

val LightPalette = StreakPalette(
    ink = Color(0xFF111114),
    inkSoft = Color(0xFF6B6E7B),
    surfaceSoft = Color(0xFFF3F4F7),
    cardDone = Color(0xFFF3F4F7),
    cardDoneInk = Color(0xFF8A8D99),
    daySelected = Color(0xFF111114),
    daySelectedInk = Color.White,
    dayDone = Lime,
    border = Color(0xFFE6E7EC)
)

val DarkPalette = StreakPalette(
    ink = Color(0xFFF4F4F8),
    inkSoft = Color(0xFFA2A5B3),
    surfaceSoft = Color(0xFF262833),
    cardDone = Color(0xFF23252E),
    cardDoneInk = Color(0xFF8D90A0),
    daySelected = Color(0xFFF4F4F8),
    daySelectedInk = Color(0xFF111114),
    dayDone = Lime,
    border = Color(0xFF2E3040)
)
