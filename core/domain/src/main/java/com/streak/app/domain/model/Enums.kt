package com.streak.app.domain.model

// Kaksen je cilj navade
enum class GoalType {
    YES_NO,   // opravljeno / ni opravljeno
    QUANTITY, // kolicina z enoto (npr. 8 kozarcev, 20 strani)
    DURATION  // trajanje v sekundah (casovna seja)
}

// Kako pogosto se navada izvaja
enum class ScheduleType {
    DAILY,           // vsak dan
    WEEKDAYS,        // izbrani dnevi v tednu
    TIMES_PER_WEEK,  // X-krat na teden
    TIMES_PER_MONTH  // X-krat na mesec
}

// V katerem delu dneva se navada obicajno izvaja (za grupiranje)
enum class TimeOfDay { ANYTIME, MORNING, AFTERNOON, EVENING }

enum class HabitCategory { NONE, HEALTH, FINANCE, LEARNING, HOME, MINDFULNESS, WORK }

// Shranjeno stanje dneva navade. "Zgreseno" se ne shranjuje, ampak izpelje iz urnika.
enum class CompletionStatus {
    DONE,    // cilj dosezen
    PARTIAL, // delni napredek (kolicina/trajanje pod ciljem)
    SKIPPED, // namerno preskoceno - ne prekine verige
    FROZEN   // porabljena zamrznitev - ne prekine verige
}

// Izpeljano stanje navade za dolocen dan (za UI in izracun streaka)
enum class DayState {
    DONE,
    PARTIAL,       // napredek pod ciljem; ce dan mine, steje kot zgresen
    SKIPPED,
    FROZEN,
    VACATION,
    MISSED,        // nacrtovan pretekli dan brez opravljenega cilja
    PENDING,       // nacrtovan dan, ki se se ni koncal
    NOT_SCHEDULED, // na ta dan navada ni predvidena
    INACTIVE,      // pred zacetkom ali po koncu navade
    FUTURE
}

// Enota, v kateri se steje streak
enum class StreakUnit { DAY, WEEK, MONTH }

// Prioriteta naloge
enum class Priority { LOW, NORMAL, URGENT }

// Izbira teme aplikacije
enum class ThemeMode { SYSTEM, LIGHT, DARK }

// Raven animacij (maskota, praznovanja)
enum class AnimationLevel { FULL, REDUCED, OFF }
