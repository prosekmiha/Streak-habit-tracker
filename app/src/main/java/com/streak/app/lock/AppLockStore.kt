package com.streak.app.lock

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.streak.app.domain.session.TimePoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.KeyStore
import java.time.Instant
import java.util.Base64
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.inject.Inject
import javax.inject.Singleton

// Ni v varnostnih kopijah (pravila vkljucujejo le posnetek in nastavitve)
private val Context.lockStore: DataStore<Preferences> by preferencesDataStore(name = "app_lock")

data class LockConfig(
    val enabled: Boolean = false,
    val pinLength: Int = 4,
    val biometric: Boolean = false,
    // 0 = takoj ob odhodu iz aplikacije
    val timeoutMs: Long = 0,
    // FLAG_SECURE: brez posnetkov zaslona, skrita vsebina v nedavnih aplikacijah
    val privacyScreen: Boolean = false,
    val failedAttempts: Int = 0,
    val lockoutSince: TimePoint? = null
)

// Zgoscene skrivnosti (koda in obnovitvena koda) s soljo
data class LockSecrets(val pinHash: ByteArray, val pinSalt: ByteArray, val recoveryHash: ByteArray?, val recoverySalt: ByteArray?)

@Singleton
class AppLockStore @Inject constructor(@ApplicationContext private val context: Context) {

    private object Keys {
        val ENABLED = booleanPreferencesKey("enabled")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val PIN_SALT = stringPreferencesKey("pin_salt")
        val PIN_LENGTH = intPreferencesKey("pin_length")
        val RECOVERY_HASH = stringPreferencesKey("recovery_hash")
        val RECOVERY_SALT = stringPreferencesKey("recovery_salt")
        val BIOMETRIC = booleanPreferencesKey("biometric")
        val TIMEOUT = longPreferencesKey("timeout_ms")
        val PRIVACY = booleanPreferencesKey("privacy_screen")
        val FAILED = intPreferencesKey("failed_attempts")
        val LOCK_ELAPSED = longPreferencesKey("lockout_elapsed")
        val LOCK_WALL = longPreferencesKey("lockout_wall")
        val LOCK_BOOT = intPreferencesKey("lockout_boot")
    }

    private val b64 = Base64.getEncoder()
    private val b64d = Base64.getDecoder()

    val config: Flow<LockConfig> = context.lockStore.data.map { it.toConfig() }

    suspend fun current(): LockConfig = config.first()

    suspend fun secrets(): LockSecrets? {
        val p = context.lockStore.data.first()
        val hash = p[Keys.PIN_HASH] ?: return null
        val salt = p[Keys.PIN_SALT] ?: return null
        return LockSecrets(b64d.decode(hash), b64d.decode(salt), p[Keys.RECOVERY_HASH]?.let(b64d::decode), p[Keys.RECOVERY_SALT]?.let(b64d::decode))
    }

    suspend fun savePin(hash: ByteArray, salt: ByteArray, length: Int) = context.lockStore.edit {
        it[Keys.PIN_HASH] = b64.encodeToString(hash)
        it[Keys.PIN_SALT] = b64.encodeToString(salt)
        it[Keys.PIN_LENGTH] = length
    }

    suspend fun saveRecovery(hash: ByteArray, salt: ByteArray) = context.lockStore.edit {
        it[Keys.RECOVERY_HASH] = b64.encodeToString(hash)
        it[Keys.RECOVERY_SALT] = b64.encodeToString(salt)
    }

    suspend fun update(transform: (LockConfig) -> LockConfig) = context.lockStore.edit { p ->
        val c = transform(p.toConfig())
        p[Keys.ENABLED] = c.enabled
        p[Keys.BIOMETRIC] = c.biometric
        p[Keys.TIMEOUT] = c.timeoutMs
        p[Keys.PRIVACY] = c.privacyScreen
        p[Keys.FAILED] = c.failedAttempts
        p.writeLockout(c.lockoutSince)
    }

    suspend fun clear() = context.lockStore.edit { it.clear() }

    private fun MutablePreferences.writeLockout(t: TimePoint?) {
        if (t == null) {
            remove(Keys.LOCK_ELAPSED); remove(Keys.LOCK_WALL); remove(Keys.LOCK_BOOT)
        } else {
            this[Keys.LOCK_ELAPSED] = t.elapsedMs
            this[Keys.LOCK_WALL] = t.wall.toEpochMilli()
            this[Keys.LOCK_BOOT] = t.bootCount
        }
    }

    private fun Preferences.toConfig(): LockConfig {
        val elapsed = this[Keys.LOCK_ELAPSED]
        val wall = this[Keys.LOCK_WALL]
        val boot = this[Keys.LOCK_BOOT]
        return LockConfig(
            enabled = (this[Keys.ENABLED] ?: false) && this[Keys.PIN_HASH] != null,
            pinLength = this[Keys.PIN_LENGTH] ?: 4,
            biometric = this[Keys.BIOMETRIC] ?: false,
            timeoutMs = this[Keys.TIMEOUT] ?: 0,
            privacyScreen = this[Keys.PRIVACY] ?: false,
            failedAttempts = this[Keys.FAILED] ?: 0,
            lockoutSince = if (elapsed != null && wall != null && boot != null) TimePoint(elapsed, Instant.ofEpochMilli(wall), boot) else null
        )
    }

    // "Poper": HMAC s kljucem v Keystoru, ki ne zapusti naprave
    fun pepper(data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(pepperKey())
        return mac.doFinal(data)
    }

    private fun pepperKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(PEPPER_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, "AndroidKeyStore")
        generator.init(KeyGenParameterSpec.Builder(PEPPER_ALIAS, KeyProperties.PURPOSE_SIGN).build())
        return generator.generateKey()
    }

    private companion object {
        const val PEPPER_ALIAS = "streak_pin_pepper"
    }
}
