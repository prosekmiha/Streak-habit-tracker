package com.streak.app.domain.backup

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class BackupReminderTest {
    private val now = Instant.parse("2026-10-11T10:00:00Z")
    private fun daysAgo(d: Long) = now.minus(Duration.ofDays(d))

    @Test
    fun `not due without habits or when disabled`() {
        assertFalse(BackupReminder.isDue(now, true, null, null, null))
        assertFalse(BackupReminder.isDue(now, false, null, null, daysAgo(100)))
    }

    @Test
    fun `first reminder after two weeks of use`() {
        assertFalse(BackupReminder.isDue(now, true, null, null, daysAgo(13)))
        assertTrue(BackupReminder.isDue(now, true, null, null, daysAgo(14)))
    }

    @Test
    fun `then every 30 days after the last backup`() {
        assertFalse(BackupReminder.isDue(now, true, daysAgo(29), null, daysAgo(200)))
        assertTrue(BackupReminder.isDue(now, true, daysAgo(30), null, daysAgo(200)))
    }

    @Test
    fun `snooze hides the reminder until it expires`() {
        assertFalse(BackupReminder.isDue(now, true, daysAgo(60), now.plusSeconds(60), daysAgo(200)))
        assertTrue(BackupReminder.isDue(now, true, daysAgo(60), now.minusSeconds(60), daysAgo(200)))
    }
}
