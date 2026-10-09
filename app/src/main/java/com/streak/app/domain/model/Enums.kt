package com.streak.app.domain.model

// Kako pogosto se navada izvaja
enum class Frequency { DAILY, WEEKLY, CUSTOM }

// Stanje dnevnega zapisa navade
enum class LogStatus { DONE, SKIPPED, MISSED }

// Prioriteta naloge
enum class Priority { LOW, NORMAL, URGENT }

// Izbira teme aplikacije
enum class ThemeMode { SYSTEM, LIGHT, DARK }
