package com.streak.app.domain.time

import java.time.LocalDate
import java.time.temporal.ChronoUnit

// Zaznava velikih skokov datuma naprave (rocna sprememba ure, napacna baterija ...).
// Nicesar ne prepreci in ne brise: le opozori pred vpisom.
sealed interface ClockStatus {
    data object Ok : ClockStatus
    data class JumpedForward(val days: Long) : ClockStatus
    data class JumpedBackward(val days: Long) : ClockStatus
}

object ClockSanity {
    const val FORWARD_TOLERANCE_DAYS = 3L

    fun check(lastKnown: LocalDate?, today: LocalDate): ClockStatus {
        if (lastKnown == null) return ClockStatus.Ok
        val diff = ChronoUnit.DAYS.between(lastKnown, today)
        return when {
            diff < 0 -> ClockStatus.JumpedBackward(-diff)
            diff > FORWARD_TOLERANCE_DAYS -> ClockStatus.JumpedForward(diff)
            else -> ClockStatus.Ok
        }
    }
}
