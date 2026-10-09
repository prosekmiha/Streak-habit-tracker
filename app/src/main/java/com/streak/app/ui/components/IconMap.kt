package com.streak.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Iron
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PedalBike
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SmokeFree
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

// Ikona navade: ime (shranjeno v bazi), vektor in privzeta pastelna barva ploscice
data class HabitIcon(val name: String, val vector: ImageVector, val tileHex: String)

object IconMap {
    val all: List<HabitIcon> = listOf(
        HabitIcon("Work", Icons.Filled.Work, "#F7CD63"),
        HabitIcon("Bolt", Icons.Filled.Bolt, "#FC8FC6"),
        HabitIcon("Person", Icons.Filled.Person, "#A9C8F5"),
        HabitIcon("Wallet", Icons.Filled.AccountBalanceWallet, "#B8EB6C"),
        HabitIcon("Restaurant", Icons.Filled.RestaurantMenu, "#FFB38A"),
        HabitIcon("Headphones", Icons.Filled.Headphones, "#A6E3D8"),
        HabitIcon("Run", Icons.Filled.DirectionsRun, "#F7CD63"),
        HabitIcon("Book", Icons.AutoMirrored.Filled.MenuBook, "#FC8FC6"),
        HabitIcon("Water", Icons.Filled.WaterDrop, "#A9C8F5"),
        HabitIcon("Iron", Icons.Filled.Iron, "#B8EB6C"),
        HabitIcon("Flame", Icons.Filled.LocalFireDepartment, "#FFB38A"),
        HabitIcon("Leaf", Icons.Filled.Eco, "#A6E3D8"),
        HabitIcon("Keyboard", Icons.Filled.Keyboard, "#A6E3D8"),
        HabitIcon("Image", Icons.Filled.Image, "#F7CD63"),
        HabitIcon("Music", Icons.Filled.MusicNote, "#FC8FC6"),
        HabitIcon("Celebration", Icons.Filled.Celebration, "#A9C8F5"),
        HabitIcon("Meditation", Icons.Filled.SelfImprovement, "#B8EB6C"),
        HabitIcon("Sleep", Icons.Filled.Bed, "#C8BDF7"),
        HabitIcon("Alarm", Icons.Filled.Alarm, "#FFB38A"),
        HabitIcon("Fitness", Icons.Filled.FitnessCenter, "#F7CD63"),
        HabitIcon("Code", Icons.Filled.Code, "#A9C8F5"),
        HabitIcon("Savings", Icons.Filled.Savings, "#B8EB6C"),
        HabitIcon("Cleaning", Icons.Filled.CleaningServices, "#A6E3D8"),
        HabitIcon("Coffee", Icons.Filled.LocalCafe, "#FFB38A"),
        HabitIcon("Bike", Icons.Filled.PedalBike, "#C8BDF7"),
        HabitIcon("Heart", Icons.Filled.Favorite, "#FC8FC6"),
        HabitIcon("Mind", Icons.Filled.Psychology, "#C8BDF7"),
        HabitIcon("School", Icons.Filled.School, "#F7CD63"),
        HabitIcon("Brush", Icons.Filled.Brush, "#FC8FC6"),
        HabitIcon("Language", Icons.Filled.Translate, "#A9C8F5"),
        HabitIcon("Sun", Icons.Filled.WbSunny, "#F7CD63"),
        HabitIcon("Pets", Icons.Filled.Pets, "#FFB38A"),
        HabitIcon("SmokeFree", Icons.Filled.SmokeFree, "#A6E3D8"),
        HabitIcon("Star", Icons.Filled.Star, "#C8BDF7")
    )

    private val byName = all.associateBy { it.name }

    fun find(name: String): HabitIcon = byName[name] ?: byName.getValue("Star")

    fun resolve(name: String): ImageVector = find(name).vector
}
