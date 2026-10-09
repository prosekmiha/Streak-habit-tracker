package com.streak.app.ui.components

import androidx.compose.ui.graphics.Color

// Pretvori hex niz (#rrggbb) v Compose Color; ob napaki vrne akcentno barvo
fun parseHexColor(hex: String): Color = try {
    val clean = hex.removePrefix("#")
    val r = clean.substring(0, 2).toInt(16)
    val g = clean.substring(2, 4).toInt(16)
    val b = clean.substring(4, 6).toInt(16)
    Color(r, g, b)
} catch (e: Exception) {
    Color(0xFF8EA2FF)
}
