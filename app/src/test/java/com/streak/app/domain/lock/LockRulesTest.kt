package com.streak.app.domain.lock

import com.streak.app.domain.session.TimePoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.SecureRandom
import java.time.Instant
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class LockRulesTest {

    private val t0 = Instant.parse("2026-10-11T10:00:00Z")
    private fun at(elapsed: Long, wall: Long = elapsed, boot: Int = 1) = TimePoint(elapsed, t0.plusMillis(wall), boot)

    @Test
    fun `first five attempts are free, then delay doubles up to an hour`() {
        assertEquals(0, LockoutPolicy.delayMs(4))
        assertEquals(30_000, LockoutPolicy.delayMs(5))
        assertEquals(60_000, LockoutPolicy.delayMs(6))
        assertEquals(120_000, LockoutPolicy.delayMs(7))
        assertEquals(3_600_000, LockoutPolicy.delayMs(30))
        assertEquals(3_600_000, LockoutPolicy.delayMs(1_000))
    }

    @Test
    fun `lockout ignores wall clock changes and survives reboot`() {
        val since = at(1_000)
        assertEquals(20_000, LockoutPolicy.remainingMs(since, 30_000, at(11_000, wall = -999_999)))
        // Po ponovnem zagonu se uporabi stenska ura
        assertEquals(10_000, LockoutPolicy.remainingMs(since, 30_000, TimePoint(500, t0.plusMillis(21_000), 2)))
        // Ura premaknjena nazaj po ponovnem zagonu: zamik ostane v celoti
        assertEquals(30_000, LockoutPolicy.remainingMs(since, 30_000, TimePoint(500, t0.minusMillis(60_000), 2)))
        assertEquals(0, LockoutPolicy.remainingMs(since, 30_000, at(40_000)))
    }

    @Test
    fun `auto lock after timeout in background`() {
        assertFalse(AutoLock.shouldLock(null, 0, at(0)))
        assertTrue(AutoLock.shouldLock(at(0), 0, at(1)))
        assertFalse(AutoLock.shouldLock(at(0), 60_000, at(59_000)))
        assertTrue(AutoLock.shouldLock(at(0), 60_000, at(60_000)))
    }

    @Test
    fun `pin hash verifies only the right pin and depends on salt and pepper`() {
        val pepperKey = ByteArray(32) { it.toByte() }
        fun pepper(key: ByteArray) = { data: ByteArray -> Mac.getInstance("HmacSHA256").apply { init(SecretKeySpec(key, "HmacSHA256")) }.doFinal(data) }
        val hasher = PinHasher(pepper(pepperKey), iterations = 1_000)
        val salt = hasher.newSalt()
        val hash = hasher.hash("2580", salt)
        assertTrue(hasher.verify("2580", salt, hash))
        assertFalse(hasher.verify("2581", salt, hash))
        assertNotEquals(hash.toList(), hasher.hash("2580", hasher.newSalt()).toList())
        val otherDevice = PinHasher(pepper(ByteArray(32) { 7 }), iterations = 1_000)
        assertFalse(otherDevice.verify("2580", salt, hash))
    }

    @Test
    fun `recovery code is readable and tolerant to typing`() {
        val code = RecoveryCode.generate(SecureRandom())
        assertEquals(12, code.length)
        assertTrue(code.none { it in "ILOU" })
        assertEquals(code, RecoveryCode.normalize(RecoveryCode.format(code).lowercase()))
        assertEquals("0111", RecoveryCode.normalize("o-i l1"))
    }

    @Test
    fun `obvious pins are rejected`() {
        listOf("1111", "1234", "4321", "123456", "000000").forEach { assertTrue(it, PinRules.isWeak(it)) }
        listOf("2580", "1357", "915273").forEach { assertFalse(it, PinRules.isWeak(it)) }
    }
}
