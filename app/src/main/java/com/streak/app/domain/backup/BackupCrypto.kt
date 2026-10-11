package com.streak.app.domain.backup

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

// Sifriranje varnostne kopije z geslom: PBKDF2-HMAC-SHA256 (nakljucna sol) -> AES-256-GCM.
// GCM hkrati preveri celovitost, zato napacno geslo ali poskodovana datoteka ne
// moreta dati napacnih podatkov.
class BackupCrypto(private val iterations: Int = DEFAULT_ITERATIONS) {

    private val random = SecureRandom()
    private val b64 = Base64.getEncoder()
    private val b64d = Base64.getDecoder()

    fun encrypt(plain: ByteArray, password: CharArray): EncryptedBackup {
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val iv = ByteArray(IV_BYTES).also(random::nextBytes)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(password, salt, iterations), GCMParameterSpec(TAG_BITS, iv))
        cipher.updateAAD(AAD)
        return EncryptedBackup(
            iterations = iterations,
            salt = b64.encodeToString(salt),
            iv = b64.encodeToString(iv),
            data = b64.encodeToString(cipher.doFinal(plain))
        )
    }

    // null = napacno geslo ali poskodovani podatki
    fun decrypt(envelope: EncryptedBackup, password: CharArray): ByteArray? {
        require(envelope.kdf == "PBKDF2WithHmacSHA256") { "kdf" }
        // Omejitev preprecuje, da bi tuja datoteka napravo "zamrznila" z ogromnim stevilom iteracij
        require(envelope.iterations in MIN_ITERATIONS..MAX_ITERATIONS) { "iterations" }
        val salt = b64d.decode(envelope.salt)
        val iv = b64d.decode(envelope.iv)
        val data = b64d.decode(envelope.data)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, deriveKey(password, salt, envelope.iterations), GCMParameterSpec(TAG_BITS, iv))
        cipher.updateAAD(AAD)
        return try {
            cipher.doFinal(data)
        } catch (_: AEADBadTagException) {
            null
        }
    }

    private fun deriveKey(password: CharArray, salt: ByteArray, iterations: Int): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, iterations, KEY_BITS)
        try {
            val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            return SecretKeySpec(bytes, "AES")
        } finally {
            spec.clearPassword()
        }
    }

    companion object {
        // OWASP priporoca 600k za PBKDF2-SHA256; na telefonu je to nekaj sekund ob izvozu/uvozu
        const val DEFAULT_ITERATIONS = 600_000
        private const val MIN_ITERATIONS = 1_000
        private const val MAX_ITERATIONS = 5_000_000
        private const val SALT_BYTES = 16
        private const val IV_BYTES = 12
        private const val KEY_BITS = 256
        private const val TAG_BITS = 128
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private val AAD = BackupFile.FORMAT_ENCRYPTED.toByteArray()
    }
}
