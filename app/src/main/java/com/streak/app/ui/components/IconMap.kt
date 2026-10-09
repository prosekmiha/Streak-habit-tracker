package com.streak.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PedalBike
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SmokeFree
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.ui.graphics.vector.ImageVector

// Preslikava imena ikone (shranjeno v bazi) v Material ImageVector
object IconMap {
    val all: List<Pair<String, ImageVector>> = listOf(
        "DirectionsRun" to Icons.Filled.DirectionsRun,
        "FitnessCenter" to Icons.Filled.FitnessCenter,
        "MenuBook" to Icons.Filled.MenuBook,
        "WaterDrop" to Icons.Filled.WaterDrop,
        "SelfImprovement" to Icons.Filled.SelfImprovement,
        "Spa" to Icons.Filled.Spa,
        "Bedtime" to Icons.Filled.Bedtime,
        "BrightnessHigh" to Icons.Filled.BrightnessHigh,
        "LocalCafe" to Icons.Filled.LocalCafe,
        "RestaurantMenu" to Icons.Filled.RestaurantMenu,
        "MusicNote" to Icons.Filled.MusicNote,
        "PedalBike" to Icons.Filled.PedalBike,
        "FavoriteBorder" to Icons.Filled.FavoriteBorder,
        "SmokeFree" to Icons.Filled.SmokeFree,
        "Bolt" to Icons.Filled.Bolt,
        "AccessTime" to Icons.Filled.AccessTime,
        "Star" to Icons.Filled.Star
    )

    private val map = all.toMap()

    fun resolve(name: String): ImageVector = map[name] ?: Icons.Filled.Star
}
