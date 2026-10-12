package com.streak.app.domain.time

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ClockSanityTest {
    private val day = LocalDate.of(2026, 10, 7)

    @Test fun `no last known date is ok`() = assertEquals(ClockStatus.Ok, ClockSanity.check(null, day))

    @Test fun `normal progression is ok`() {
        assertEquals(ClockStatus.Ok, ClockSanity.check(day, day))
        assertEquals(ClockStatus.Ok, ClockSanity.check(day.minusDays(3), day))
    }

    @Test fun `large forward jump is reported`() =
        assertEquals(ClockStatus.JumpedForward(30), ClockSanity.check(day.minusDays(30), day))

    @Test fun `any backward jump is reported`() =
        assertEquals(ClockStatus.JumpedBackward(1), ClockSanity.check(day.plusDays(1), day))

    @Test fun `known date advances only forward and without a suspicious jump`() {
        assertEquals(day, ClockSanity.advance(null, day))
        assertEquals(day, ClockSanity.advance(day.minusDays(1), day))
        assertEquals(null, ClockSanity.advance(day, day))
        assertEquals(null, ClockSanity.advance(day.plusDays(1), day))
        assertEquals(null, ClockSanity.advance(day.minusDays(30), day))
    }

    @Test fun `after a reboot any forward jump is accepted`() {
        assertEquals(day, ClockSanity.advance(day.minusDays(30), day, afterReboot = true))
        assertEquals(null, ClockSanity.advance(day.plusDays(1), day, afterReboot = true))
    }
}
