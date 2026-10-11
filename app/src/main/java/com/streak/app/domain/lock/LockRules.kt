package com.streak.app.domain.lock

import com.streak.app.domain.session.TimePoint
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

// Zamik po napacnih poskusih: prvih 5 je brez zamika, nato 30 s in vsakic dvakrat
// dlje, najvec 1 ura. Zamik se meri kot sejni cas (monotona ura, po ponovnem
// zagonu stenska), zato ga ne obide ne premik ure ne ponovni zagon aplikacije.
object LockoutPolicy {
    const val FREE_ATTEMPTS = 5
    private const val BASE_MS = 30_000L
    private const val MAX_MS = 60 * 60_000L

    fun delayMs(failedAttempts: Int): Long {
        if (failedAttempts < FREE_ATTEMPTS) return 0
        val exponent = (failedAttempts - FREE_ATTEMPTS).coerceAtMost(20)
        return (BASE_MS shl exponent).coerceAtMost(MAX_MS)
    }

    fun remainingMs(since: TimePoint?, delayMs: Long, now: TimePoint): Long {
        if (since == null || delayMs <= 0) return 0
        val elapsed = if (since.bootCount == now.bootCount && now.elapsedMs >= since.elapsedMs) {
            now.elapsedMs - since.elapsedMs
        } else {
            (now.wall.toEpochMilli() - since.wall.toEpochMilli()).coerceAtLeast(0)
        }
        return (delayMs - elapsed).coerceAtLeast(0)
    }
}

// Samodejni zaklep po casu v ozadju (timeout 0 = takoj)
object AutoLock {
    fun shouldLock(backgroundSince: TimePoint?, timeoutMs: Long, now: TimePoint): Boolean {
        if (backgroundSince == null) return false
        if (timeoutMs <= 0) return true
        return LockoutPolicy.remainingMs(backgroundSince, timeoutMs, now) == 0L
    }
}

// Zgoscevanje kode: PBKDF2-HMAC-SHA256 z naklucno soljo, nato "poper" (HMAC s kljucem v
// Keystoru), da kratke kode ni mogoce preizkusiti izven naprave.
class PinHasher(
    private val pepper: (ByteArray) -> ByteArray,
    private val iterations: Int = DEFAULT_ITERATIONS
) {
    fun newSalt(): ByteArray = ByteArray(16).also(SecureRandom()::nextBytes)

    fun hash(secret: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(secret.toCharArray(), salt, iterations, 256)
        try {
            val derived = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            return pepper(derived)
        } finally {
            spec.clearPassword()
        }
    }

    // Primerjava v konstantnem casu
    fun verify(secret: String, salt: ByteArray, expected: ByteArray): Boolean =
        MessageDigest.isEqual(hash(secret, salt), expected)

    companion object {
        const val DEFAULT_ITERATIONS = 120_000
    }
}

// Obnovitvena koda za pozabljeno kodo: 12 znakov Crockford base32 (brez I, L, O, U)
object RecoveryCode {
    private const val ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"
    const val LENGTH = 12

    fun generate(random: SecureRandom = SecureRandom()): String =
        (1..LENGTH).map { ALPHABET[random.nextInt(ALPHABET.length)] }.joinToString("")

    // Za prikaz: XXXX-XXXX-XXXX
    fun format(code: String): String = code.chunked(4).joinToString("-")

    // Tolerantno do presledkov, pomisljajev, malih crk in zamenljivih znakov
    fun normalize(input: String): String = input.uppercase()
        .filter { it.isLetterOrDigit() }
        .map {
            when (it) {
                'O' -> '0'
                'I', 'L' -> '1'
                else -> it
            }
        }
        .joinToString("")
}

object PinRules {
    val LENGTHS = listOf(4, 6)

    // Prepreci ocitne kode (1111, 1234, 4321 ...)
    fun isWeak(pin: String): Boolean {
        if (pin.toSet().size == 1) return true
        val digits = pin.map { it - '0' }
        val steps = digits.zipWithNext { a, b -> b - a }.toSet()
        return steps == setOf(1) || steps == setOf(-1)
    }
}
