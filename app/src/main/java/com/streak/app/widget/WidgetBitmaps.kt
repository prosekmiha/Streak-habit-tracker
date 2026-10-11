package com.streak.app.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.LruCache
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.unit.Dp
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withSave
import androidx.core.graphics.withTranslation
import com.streak.app.ui.components.IconMap
import com.streak.app.ui.components.parseHexColor
import com.streak.app.ui.theme.InkOnPastel
import com.streak.app.ui.theme.Lime
import kotlin.math.roundToInt

// Bitne slike za widgete. RemoteViews ne zna izrisati Compose vektorjev, zato ikone
// navad (iste kot v aplikaciji) narisemo v bitno sliko; majhne so in se predpomnijo.
object WidgetBitmaps {
    private val cache = object : LruCache<String, Bitmap>(CACHE_BYTES) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    // Ikona navade na pastelni zaobljeni ploscici
    fun habitTile(context: Context, icon: String, colorHex: String, size: Dp): Bitmap {
        val px = size.toPx(context)
        return cached("tile:$icon:$colorHex:$px") {
            createBitmap(px, px).also { bmp ->
                val canvas = Canvas(bmp)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = parseHexColor(colorHex).toArgb() }
                val r = px * 0.3f
                canvas.drawRoundRect(RectF(0f, 0f, px.toFloat(), px.toFloat()), r, r, paint)
                drawVector(canvas, IconMap.resolve(icon), px * 0.2f, px * 0.6f, InkOnPastel.toArgb())
            }
        }
    }

    // Ikona navade brez ozadja (za widgete v barvi navade)
    fun habitIcon(context: Context, icon: String, size: Dp): Bitmap {
        val px = size.toPx(context)
        return cached("icon:$icon:$px") {
            createBitmap(px, px).also { drawVector(Canvas(it), IconMap.resolve(icon), 0f, px.toFloat(), InkOnPastel.toArgb()) }
        }
    }

    // Obroc napredka: prosojna sled (deluje na svetlem in temnem ozadju) in zelen lok
    fun ring(context: Context, progress: Float, size: Dp): Bitmap {
        val px = size.toPx(context)
        val step = (progress.coerceIn(0f, 1f) * 100).roundToInt()
        return cached("ring:$step:$px") {
            createBitmap(px, px).also { bmp ->
                val canvas = Canvas(bmp)
                val stroke = px * 0.12f
                val rect = RectF(stroke / 2, stroke / 2, px - stroke / 2, px - stroke / 2)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = stroke
                    strokeCap = Paint.Cap.ROUND
                }
                paint.color = TRACK
                canvas.drawArc(rect, 0f, 360f, false, paint)
                if (step > 0) {
                    paint.color = Lime.toArgb()
                    canvas.drawArc(rect, -90f, 360f * step / 100f, false, paint)
                }
            }
        }
    }

    private fun drawVector(canvas: Canvas, vector: ImageVector, offset: Float, size: Float, color: Int) {
        canvas.withTranslation(offset, offset) {
            scale(size / vector.viewportWidth, size / vector.viewportHeight)
            drawGroup(this, vector.root, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color })
        }
    }

    private fun drawGroup(canvas: Canvas, group: VectorGroup, paint: Paint): Unit = canvas.withSave {
        translate(group.translationX + group.pivotX, group.translationY + group.pivotY)
        rotate(group.rotation)
        scale(group.scaleX, group.scaleY)
        translate(-group.pivotX, -group.pivotY)
        for (node in group) {
            when (node) {
                is VectorGroup -> drawGroup(this, node, paint)
                is VectorPath -> {
                    val path = PathParser().addPathNodes(node.pathData).toPath().asAndroidPath()
                    path.fillType = if (node.pathFillType == PathFillType.EvenOdd) android.graphics.Path.FillType.EVEN_ODD else android.graphics.Path.FillType.WINDING
                    paint.alpha = (node.fillAlpha * 255).roundToInt()
                    drawPath(path, paint)
                }
            }
        }
    }

    private fun cached(key: String, create: () -> Bitmap): Bitmap =
        cache.get(key) ?: create().also { cache.put(key, it) }

    private fun Dp.toPx(context: Context): Int = (value * context.resources.displayMetrics.density).roundToInt().coerceAtLeast(1)

    private const val TRACK = 0x33888888
    private const val CACHE_BYTES = 2 * 1024 * 1024
}
