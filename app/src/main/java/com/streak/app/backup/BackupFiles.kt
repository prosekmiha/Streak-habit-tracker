package com.streak.app.backup

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import javax.inject.Inject

// Branje in pisanje datotek varnostne kopije (SAF in deljenje)
interface BackupFiles {
    // Zacasna datoteka za deljenje (prejsnje se pobrisejo)
    suspend fun writeShareFile(fileName: String, text: String): Uri

    // Pisanje na mesto, ki ga je uporabnik izbral v SAF
    suspend fun write(uri: Uri, text: String): Boolean

    // null, ce branje ne uspe ali je datoteka vecja od maxBytes
    suspend fun read(uri: Uri, maxBytes: Int): String?
}

class AndroidBackupFiles @Inject constructor(
    @ApplicationContext private val context: Context
) : BackupFiles {

    override suspend fun writeShareFile(fileName: String, text: String): Uri = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "exports").apply { deleteRecursively(); mkdirs() }
        val file = File(dir, fileName).apply { writeText(text) }
        FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    }

    override suspend fun write(uri: Uri, text: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(text.toByteArray()) }
        }.isSuccess
    }

    override suspend fun read(uri: Uri, maxBytes: Int): String? = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openInputStream(uri)!!.use { input ->
                val bytes = input.readNBytesCompat(maxBytes + 1)
                if (bytes.size > maxBytes) null else bytes.decodeToString()
            }
        }.getOrNull()
    }
}

// InputStream.readNBytes je sele od API 33
private fun InputStream.readNBytesCompat(max: Int): ByteArray {
    val out = ByteArrayOutputStream()
    val buf = ByteArray(64 * 1024)
    while (out.size() < max) {
        val n = read(buf, 0, minOf(buf.size, max - out.size()))
        if (n < 0) break
        out.write(buf, 0, n)
    }
    return out.toByteArray()
}
