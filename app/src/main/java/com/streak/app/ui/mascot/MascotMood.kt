package com.streak.app.ui.mascot

import androidx.annotation.StringRes
import com.streak.app.R

// Stanja maskote. Ista imena uporabljajo animacije 3D modela (M7) in vnaprej
// zrenderirane poze za widgete in obvestila.
enum class MascotMood(@StringRes val description: Int) {
    IDLE(R.string.mascot_idle),
    WAVE(R.string.mascot_wave),
    WALK(R.string.mascot_walk),
    HAPPY(R.string.mascot_happy),
    CHEER(R.string.mascot_cheer),
    SAD(R.string.mascot_sad),
    THINKING(R.string.mascot_thinking),
    SLEEPING(R.string.mascot_sleeping),
    MEDITATING(R.string.mascot_meditating),
    STRETCHING(R.string.mascot_stretching),
    THUMBS_UP(R.string.mascot_thumbs_up),
    FREEZE(R.string.mascot_freeze),
    VACATION(R.string.mascot_vacation),
    WORKING(R.string.mascot_working)
}
