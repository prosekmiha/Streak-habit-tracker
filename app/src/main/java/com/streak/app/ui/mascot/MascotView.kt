package com.streak.app.ui.mascot

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.streak.app.domain.model.AnimationLevel
import com.streak.app.ui.theme.LocalAnimationLevel
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

// Prikaz maskote kot ploskovne ilustracije (ista risba kot mockupi). 3D renderer (M7)
// bo zamenjal le notranjost te komponente; klicna mesta ostanejo enaka.
@Composable
fun MascotView(
    mood: MascotMood,
    modifier: Modifier = Modifier,
    description: String? = null
) {
    val animated = LocalAnimationLevel.current == AnimationLevel.FULL
    val phase = if (animated) {
        val transition = rememberInfiniteTransition(label = "mascot")
        val p by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart),
            label = "phase"
        )
        p
    } else 0f
    val label = description ?: stringResource(mood.description)
    val elements = remember(mood) { MascotArt.elements(mood).map { it.prepared() } }

    Canvas(modifier = modifier.aspectRatio(MascotArt.VIEW_W / MascotArt.VIEW_H).semantics { contentDescription = label }) {
        val s = min(size.width / MascotArt.VIEW_W, size.height / MascotArt.VIEW_H)
        val dx = (size.width - MascotArt.VIEW_W * s) / 2f
        val dy = (size.height - MascotArt.VIEW_H * s) / 2f
        val wave = sin(phase * 2 * PI).toFloat()
        val floating = mood == MascotMood.MEDITATING
        val bob = (if (floating) 5f else 2.5f) * wave

        translate(dx, dy) {
            scale(s, pivot = Offset.Zero) {
                if (mood == MascotMood.FREEZE) {
                    drawRoundRect(Color(0xFFBFE3FF), Offset(30f, 30f), Size(180f, 214f), CornerRadius(26f), alpha = .8f)
                }
                translate(top = -bob) {
                    elements.forEach { drawEl(it) }
                    if (mood == MascotMood.VACATION) drawSunglasses()
                }
            }
        }
    }
}

// Poti razcleni le enkrat na pozo
private sealed interface Prepared {
    val el: MascotEl
}
private data class PreparedPath(override val el: MascotEl.P, val path: Path) : Prepared
private data class PreparedOther(override val el: MascotEl) : Prepared

private fun MascotEl.prepared(): Prepared = when (this) {
    is MascotEl.P -> PreparedPath(this, PathParser().parsePathString(d).toPath())
    else -> PreparedOther(this)
}

private fun DrawScope.drawEl(p: Prepared) {
    when (val el = p.el) {
        is MascotEl.P -> {
            val path = (p as PreparedPath).path
            el.fill?.let { drawPath(path, it, alpha = el.alpha) }
            el.stroke?.let { drawPath(path, it, alpha = el.alpha, style = Stroke(el.width, cap = StrokeCap.Round, join = StrokeJoin.Round)) }
        }
        is MascotEl.C -> {
            el.fill?.let { drawCircle(it, el.r, Offset(el.cx, el.cy)) }
            el.stroke?.let { drawCircle(it, el.r, Offset(el.cx, el.cy), style = Stroke(el.width)) }
        }
        is MascotEl.E -> {
            val tl = Offset(el.cx - el.rx, el.cy - el.ry)
            val sz = Size(el.rx * 2, el.ry * 2)
            el.fill?.let { drawOval(it, tl, sz, alpha = el.alpha) }
            el.stroke?.let { drawOval(it, tl, sz, alpha = el.alpha, style = Stroke(el.width)) }
        }
        is MascotEl.R -> rotate(el.rotate, pivot = Offset(el.x + el.w / 2, el.y + el.h / 2)) {
            drawRoundRect(el.fill, Offset(el.x, el.y), Size(el.w, el.h), CornerRadius(el.rx))
        }
    }
}

private fun DrawScope.drawSunglasses() {
    val ink = MascotArt.Ink
    drawRoundRect(ink, Offset(84f, 100f), Size(32f, 22f), CornerRadius(8f))
    drawRoundRect(ink, Offset(124f, 100f), Size(32f, 22f), CornerRadius(8f))
    drawLine(ink, Offset(116f, 108f), Offset(124f, 108f), 5f)
}
