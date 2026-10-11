package com.streak.app.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

// Kljuc baze: nakljucnih 256 bitov, shranjenih ovitih (AES-GCM) s kljucem v Android
// Keystoru, ki ne zapusti naprave. Ovita datoteka je v no_backup, zato je ni v nobeni
// varnostni kopiji; sifrirane baze brez te naprave ni mogoce odpreti.
@Singleton
class DatabaseKeyStore @Inject constructor(@ApplicationContext private val context: Context) {

    private val wrappedFile get() = File(context.noBackupFilesDir, WRAPPED_KEY_FILE)

    // SQLCipher "raw key" oblika (brez dodatnega PBKDF2 ob vsakem odpiranju)
    @Synchronized
    fun rawKey(): String {
        val key = if (wrappedFile.exists()) unwrap(wrappedFile.readBytes()) else create()
        return "x'" + key.joinToString("") { "%02x".format(it) } + "'"
    }

    // Kljuca ni mogoce vec odviti (npr. ponastavljen Keystore): zacni z novim
    @Synchronized
    fun reset() {
        wrappedFile.delete()
        runCatching { keyStore().deleteEntry(ALIAS) }
    }

    private fun create(): ByteArray {
        val key = ByteArray(32).also(SecureRandom()::nextBytes)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, wrappingKey())
        val out = cipher.iv + cipher.doFinal(key)
        val tmp = File(wrappedFile.path + ".tmp")
        tmp.writeBytes(out)
        if (!tmp.renameTo(wrappedFile)) error("Kljuca baze ni mogoce shraniti")
        return key
    }

    private fun unwrap(data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, wrappingKey(), GCMParameterSpec(128, data, 0, IV_BYTES))
        return cipher.doFinal(data, IV_BYTES, data.size - IV_BYTES)
    }

    private fun keyStore() = KeyStore.getInstance(KEYSTORE).apply { load(null) }

    private fun wrappingKey(): SecretKey {
        (keyStore().getKey(ALIAS, null) as? SecretKey)?.let { return it }
        Log.i(TAG, "Ustvarjam kljuc za ovijanje kljuca baze")
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    private companion object {
        const val TAG = "DatabaseKeyStore"
        const val KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "streak_db_wrap"
        const val WRAPPED_KEY_FILE = "db.key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
    }
}
