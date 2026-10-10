package com.streak.app.domain.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ActiveSessionTest {

    private val t0 = Instant.parse("2026-03-29T00:30:00Z")
    private fun at(elapsed: Long, wallOffset: Long = elapsed, boot: Int = 1) =
        TimePoint(elapsed, t0.plusMillis(wallOffset), boot)

    @Test
    fun `elapsed follows monotonic clock`() {
        val s = ActiveSession.start("h", "Meditacija", 600, at(1_000))
        assertEquals(60_000, s.elapsedMs(at(61_000)))
    }

    @Test
    fun `wall clock change does not affect elapsed`() {
        val s = ActiveSession.start("h", "Meditacija", 600, at(1_000))
        // Uporabnik premakne uro za uro nazaj (ali DST): monotona ura steje naprej
        assertEquals(30_000, s.elapsedMs(at(31_000, wallOffset = -3_600_000)))
    }

    @Test
    fun `pause freezes time and resume continues`() {
        var s = ActiveSession.start("h", "Meditacija", 600, at(0))
        s = s.pause(at(10_000))
        assertFalse(s.running)
        assertEquals(10_000, s.elapsedMs(at(500_000)))
        s = s.resume(at(500_000))
        assertTrue(s.running)
        assertEquals(15_000, s.elapsedMs(at(505_000)))
    }

    @Test
    fun `pause and resume are idempotent`() {
        val s = ActiveSession.start("h", "Meditacija", 600, at(0)).pause(at(5_000))
        assertEquals(s, s.pause(at(9_000)))
        val r = s.resume(at(9_000))
        assertEquals(r, r.resume(at(12_000)))
    }

    @Test
    fun `remaining to goal counts down and stops at zero`() {
        val s = ActiveSession.start("h", "Meditacija", 600, at(0))
        assertEquals(540_000L, s.remainingToGoalMs(at(60_000)))
        assertEquals(null, s.remainingToGoalMs(at(600_000)))
        assertEquals(null, s.copy(targetSeconds = 0).remainingToGoalMs(at(1_000)))
    }

    @Test
    fun `after reboot wall clock is used`() {
        val s = ActiveSession.start("h", "Meditacija", 600, at(50_000_000, wallOffset = 0, boot = 1))
        // Po ponovnem zagonu je elapsedRealtime majhen, boot count drugacen
        val afterReboot = TimePoint(20_000, t0.plusMillis(120_000), 2)
        assertEquals(120_000, s.elapsedMs(afterReboot))
    }

    @Test
    fun `wall clock going backwards after reboot never yields negative time`() {
        val s = ActiveSession.start("h", "Meditacija", 600, at(1_000, wallOffset = 0, boot = 1)).pause(at(31_000, wallOffset = 30_000))
            .resume(TimePoint(40_000, t0.plusMillis(40_000), 1))
        val afterReboot = TimePoint(5_000, t0.minusMillis(60_000), 2)
        assertEquals(30_000, s.elapsedMs(afterReboot))
    }
}
