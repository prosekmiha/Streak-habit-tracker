package com.streak.app.ui.achievements

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withTranslation
import com.streak.app.R
import com.streak.app.ui.components.drawImageVector
import com.streak.app.ui.theme.Blurple
import com.streak.app.ui.theme.InkOnPastel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

// Vsebina kartice za deljenje: znacka, nov nivo ali mejnik verige
data class ShareCardSpec(
    val label: String,     // npr. "NOVA ZNACKA"
    val title: String,     // npr. ime znacke
    val subtitle: String,
    val badgeColor: Color,
    val icon: ImageVector? = null,
    val badgeText: String? = null, // stevilka namesto ikone (nivo, dnevi)
    val shareText: String
)

// Dosezek kot slika (1080 x 1350) za deljenje; brez osebnih podatkov razen imena navade,
// ce ga uporabnik deli sam
object ShareCard {
    private const val W = 1080
    private const val H = 1350
    private const val TEXT_TOP = 820f

    suspend fun share(context: Context, spec: ShareCardSpec) {
        val uri = withContext(Dispatchers.IO) {
            val bitmap = render(context, spec)
            val dir = File(context.cacheDir, "exports").apply { mkdirs() }
            dir.listFiles { f -> f.name.startsWith("achievement-") }?.forEach { it.delete() }
            val file = File(dir, "achievement-${System.currentTimeMillis()}.png")
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
            FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        }
        val send = Intent(Intent.ACTION_SEND)
            .setType("image/png")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_TEXT, spec.shareText)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        send.clipData = ClipData.newRawUri(null, uri)
        context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun render(context: Context, spec: ShareCardSpec): Bitmap {
        val bmp = createBitmap(W, H)
        val c = Canvas(bmp)
        c.drawColor(Blurple.toArgb())
        drawConfetti(c)

        // Maskota
        ContextCompat.getDrawable(context, R.drawable.mascot_cheer)?.let { d ->
            val mw = 560
            val mh = mw * d.intrinsicHeight / d.intrinsicWidth.coerceAtLeast(1)
            d.setBounds((W - mw) / 2 - 90, 210, (W - mw) / 2 - 90 + mw, 210 + mh)
            d.draw(c)
        }

        // Okrogla znacka desno zgoraj (kot pri praznovanju v aplikaciji)
        val cx = 760f
        val cy = 330f
        val r = 150f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        c.withTranslation(cx, cy) {
            rotate(10f)
            paint.color = spec.badgeColor.toArgb()
            drawCircle(0f, 0f, r, paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 14f
            paint.color = InkOnPastel.toArgb()
            drawCircle(0f, 0f, r, paint)
            paint.style = Paint.Style.FILL
            when {
                spec.badgeText != null -> {
                    val text = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = InkOnPastel.toArgb()
                        textSize = if (spec.badgeText.length > 3) 92f else 120f
                        useDisplayFont(context)
                        textAlign = Paint.Align.CENTER
                    }
                    drawText(spec.badgeText, 0f, -(text.ascent() + text.descent()) / 2, text)
                }
                spec.icon != null -> drawImageVector(spec.icon, -r * 0.55f, -r * 0.55f, r * 1.1f, InkOnPastel.toArgb())
            }
        }

        // Oznaka, naslov, podnaslov
        val label = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.White.toArgb()
            textSize = 38f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.08f
        }
        val labelWidth = label.measureText(spec.label)
        paint.color = Color.White.copy(alpha = 0.16f).toArgb()
        c.drawRoundRect(RectF((W - labelWidth) / 2 - 30, 92f, (W + labelWidth) / 2 + 30, 160f), 24f, 24f, paint)
        label.textAlign = Paint.Align.CENTER
        c.drawText(spec.label, W / 2f, 140f, label)

        var y = 860f
        y += drawCentered(c, spec.title, TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.White.toArgb()
            textSize = 84f
            useDisplayFont(context)
        }, y)
        y += 24f
        drawCentered(c, spec.subtitle, TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color(0xFFE4E5FB).toArgb()
            textSize = 42f
        }, y)

        val footer = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.White.copy(alpha = 0.75f).toArgb()
            textSize = 34f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        c.drawText(context.getString(R.string.share_card_footer), W / 2f, H - 70f, footer)
        return bmp
    }

    private fun drawCentered(c: Canvas, text: String, paint: TextPaint, top: Float): Float {
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, W - 160)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setMaxLines(3)
            .build()
        c.withTranslation(80f, top) { layout.draw(this) }
        return layout.height.toFloat()
    }

    private fun drawConfetti(c: Canvas) {
        val colors = listOf(0xFFF7CD63, 0xFFFC8FC6, 0xFFB8EB6C, 0xFFA9C8F5, 0xFFFFB38A).map { it.toInt() }
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        for (i in 0 until 26) {
            val seed = i * 37 % 100 / 100f
            val x = W * ((i * 0.137f + seed * 0.3f) % 1f)
            val y = H * ((seed + i * 0.071f) % 1f)
            // Pod maskoto je besedilo: tam konfeti le ob robovih
            if (y > TEXT_TOP && x > 60f && x < W - 60f) continue
            p.color = colors[i % colors.size]
            c.withTranslation(x, y) {
                rotate(i * 23f)
                if (i % 3 == 0) drawCircle(0f, 0f, 12f, p) else drawRoundRect(RectF(-20f, -11f, 20f, 11f), 7f, 7f, p)
            }
        }
    }

    // Naslovni font aplikacije (Roboto Flex, sirok in krepak); ob napaki sistemski krepki
    private fun TextPaint.useDisplayFont(context: Context) {
        val font = runCatching { ResourcesCompat.getFont(context, R.font.streak_display) }.getOrNull()
        if (font == null) {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        } else {
            typeface = font
            fontVariationSettings = "'wght' 850, 'wdth' 145"
        }
    }
}
