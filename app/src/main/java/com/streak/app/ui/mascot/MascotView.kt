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
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.streak.app.domain.model.AnimationLevel
import com.streak.app.ui.theme.LocalAnimationLevel
import com.streak.app.ui.theme.MascotOrange
import kotlin.math.PI
import kotlin.math.sin

// Prikaz maskote. Do 3D rendererja (M7) jo rise kot ploskovno 2D ilustracijo;
// klicna mesta ostanejo enaka, ko se zamenja renderer.
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
            animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
            label = "phase"
        )
        p
    } else 0f
    val label = description ?: stringResource(mood.description)
    Canvas(
        modifier = modifier
            .aspectRatio(1f)
            .semantics { contentDescription = label }
    ) {
        drawMascot(mood, phase)
    }
}

private val Outline = Color(0xFF111114)
private val Glove = Color.White
private val Shoe = Color.White

private fun DrawScope.drawMascot(mood: MascotMood, phase: Float) {
    val s = size.minDimension
    val wave = sin(phase * 2 * PI).toFloat()
    val floating = mood == MascotMood.MEDITATING
    val bob = s * (if (floating) 0.025f else 0.012f) * wave
    val stroke = s * 0.022f

    // Senca na tleh
    val shadowW = s * (if (floating) 0.26f - 0.03f * wave else 0.36f)
    drawOval(
        Outline.copy(alpha = if (floating) 0.9f else 0.12f),
        topLeft = Offset(s * 0.5f - shadowW / 2, s * 0.9f),
        size = Size(shadowW, s * 0.045f)
    )

    if (mood == MascotMood.FREEZE) {
        drawRoundRect(
            Color(0xFFBFE3FF).copy(alpha = 0.7f),
            topLeft = Offset(s * 0.16f, s * 0.12f),
            size = Size(s * 0.68f, s * 0.78f),
            cornerRadius = CornerRadius(s * 0.08f)
        )
    }

    translate(top = -bob - if (floating) s * 0.05f else 0f) {
        val body = Offset(s * 0.5f, s * 0.44f)
        val r = s * 0.25f

        drawLegs(mood, s, stroke)
        drawArms(mood, s, stroke, wave, body, r)
        drawCircle(MascotOrange, r, body)
        drawCircle(Outline, r, body, style = Stroke(stroke))
        // Odsev
        drawArc(
            Color.White.copy(alpha = 0.35f), 200f, 50f, false,
            topLeft = Offset(body.x - r * 0.75f, body.y - r * 0.75f), size = Size(r * 1.5f, r * 1.5f),
            style = Stroke(stroke * 0.9f, cap = StrokeCap.Round)
        )
        drawFace(mood, s, stroke, body)
        drawGloves(mood, s, stroke, wave, body, r)
        drawExtras(mood, s, stroke, phase)
    }
}

private fun DrawScope.drawLegs(mood: MascotMood, s: Float, stroke: Float) {
    if (mood == MascotMood.MEDITATING) {
        // Prekrizane noge
        drawLine(Outline, Offset(s * 0.36f, s * 0.7f), Offset(s * 0.64f, s * 0.7f), stroke * 1.4f, StrokeCap.Round)
        shoe(Offset(s * 0.3f, s * 0.7f), s, stroke)
        shoe(Offset(s * 0.7f, s * 0.7f), s, stroke)
        return
    }
    val walk = mood == MascotMood.WALK
    val left = Offset(s * (if (walk) 0.38f else 0.43f), s * 0.86f)
    val right = Offset(s * (if (walk) 0.62f else 0.57f), s * 0.86f)
    drawLine(Outline, Offset(s * 0.45f, s * 0.67f), left, stroke * 1.3f, StrokeCap.Round)
    drawLine(Outline, Offset(s * 0.55f, s * 0.67f), right, stroke * 1.3f, StrokeCap.Round)
    shoe(left, s, stroke)
    shoe(right, s, stroke)
}

private fun DrawScope.shoe(center: Offset, s: Float, stroke: Float) {
    val w = s * 0.12f
    val h = s * 0.06f
    val tl = Offset(center.x - w / 2, center.y - h / 3)
    drawRoundRect(Shoe, tl, Size(w, h), CornerRadius(h / 2))
    drawRoundRect(Outline, tl, Size(w, h), CornerRadius(h / 2), style = Stroke(stroke * 0.8f))
}

// Polozaj rokavic glede na razpolozenje
private fun glovePositions(mood: MascotMood, s: Float, wave: Float): Pair<Offset, Offset> = when (mood) {
    MascotMood.WAVE -> Offset(s * 0.2f, s * 0.6f) to Offset(s * (0.84f + 0.03f * wave), s * 0.18f)
    MascotMood.HAPPY, MascotMood.CHEER, MascotMood.STRETCHING ->
        Offset(s * 0.16f, s * (0.18f + 0.02f * wave)) to Offset(s * 0.84f, s * (0.18f - 0.02f * wave))
    MascotMood.SAD -> Offset(s * 0.22f, s * 0.7f) to Offset(s * 0.78f, s * 0.7f)
    MascotMood.THINKING, MascotMood.WORKING -> Offset(s * 0.2f, s * 0.6f) to Offset(s * 0.6f, s * 0.6f)
    MascotMood.MEDITATING -> Offset(s * 0.2f, s * 0.6f) to Offset(s * 0.8f, s * 0.6f)
    MascotMood.THUMBS_UP -> Offset(s * 0.2f, s * 0.6f) to Offset(s * 0.82f, s * 0.36f)
    MascotMood.SLEEPING -> Offset(s * 0.3f, s * 0.66f) to Offset(s * 0.7f, s * 0.66f)
    else -> Offset(s * 0.2f, s * 0.6f) to Offset(s * 0.8f, s * 0.6f)
}

private fun DrawScope.drawArms(mood: MascotMood, s: Float, stroke: Float, wave: Float, body: Offset, r: Float) {
    val (left, right) = glovePositions(mood, s, wave)
    arm(Offset(body.x - r * 0.92f, body.y + r * 0.2f), left, s, stroke)
    arm(Offset(body.x + r * 0.92f, body.y + r * 0.2f), right, s, stroke)
}

// "Rubber-hose" roka kot kvadratna krivulja
private fun DrawScope.arm(from: Offset, to: Offset, s: Float, stroke: Float) {
    val mid = Offset((from.x + to.x) / 2 + (if (to.x < from.x) -s * 0.04f else s * 0.04f), (from.y + to.y) / 2 + s * 0.03f)
    val path = Path().apply {
        moveTo(from.x, from.y)
        quadraticTo(mid.x, mid.y, to.x, to.y)
    }
    drawPath(path, Outline, style = Stroke(stroke * 1.3f, cap = StrokeCap.Round))
}

private fun DrawScope.drawGloves(mood: MascotMood, s: Float, stroke: Float, wave: Float, body: Offset, r: Float) {
    val (left, right) = glovePositions(mood, s, wave)
    listOf(left, right).forEach {
        drawCircle(Glove, s * 0.055f, it)
        drawCircle(Outline, s * 0.055f, it, style = Stroke(stroke * 0.8f))
    }
    if (mood == MascotMood.THUMBS_UP) {
        drawLine(Outline, right, Offset(right.x, right.y - s * 0.08f), stroke, StrokeCap.Round)
    }
}

private fun DrawScope.drawFace(mood: MascotMood, s: Float, stroke: Float, body: Offset) {
    val eyeY = body.y - s * 0.04f
    val leftEye = Offset(body.x - s * 0.075f, eyeY)
    val rightEye = Offset(body.x + s * 0.075f, eyeY)
    val closed = mood == MascotMood.SLEEPING || mood == MascotMood.MEDITATING
    val eyeW = s * 0.085f
    val eyeH = s * 0.11f

    when {
        mood == MascotMood.VACATION -> {
            // Soncna ocala
            listOf(leftEye, rightEye).forEach {
                drawRoundRect(Outline, Offset(it.x - eyeW * 0.65f, it.y - eyeH * 0.35f), Size(eyeW * 1.3f, eyeH * 0.7f), CornerRadius(s * 0.02f))
            }
            drawLine(Outline, Offset(leftEye.x + eyeW * 0.6f, eyeY), Offset(rightEye.x - eyeW * 0.6f, eyeY), stroke)
        }
        closed -> listOf(leftEye, rightEye).forEach {
            drawArc(Outline, 20f, 140f, false, Offset(it.x - eyeW / 2, it.y - eyeH / 3), Size(eyeW, eyeH / 2), style = Stroke(stroke, cap = StrokeCap.Round))
        }
        else -> {
            val look = when (mood) {
                MascotMood.THINKING, MascotMood.WORKING -> Offset(s * 0.012f, -s * 0.02f)
                MascotMood.SAD -> Offset(0f, s * 0.018f)
                else -> Offset(0f, s * 0.006f)
            }
            listOf(leftEye, rightEye).forEachIndexed { i, it ->
                val wink = mood == MascotMood.THUMBS_UP && i == 1
                if (wink) {
                    drawArc(Outline, 200f, 140f, false, Offset(it.x - eyeW / 2, it.y), Size(eyeW, eyeH / 2), style = Stroke(stroke, cap = StrokeCap.Round))
                } else {
                    drawOval(Color.White, Offset(it.x - eyeW / 2, it.y - eyeH / 2), Size(eyeW, eyeH))
                    drawOval(Outline, Offset(it.x - eyeW / 2, it.y - eyeH / 2), Size(eyeW, eyeH), style = Stroke(stroke * 0.8f))
                    drawCircle(Outline, s * 0.022f, it + look)
                }
            }
        }
    }

    // Obrvi
    val browY = eyeY - eyeH * 0.75f
    val tilt = if (mood == MascotMood.SAD) s * 0.02f else 0f
    // Pri razmisljanju je ena obrv dvignjena (radovednost, ne jeza)
    val raise = if (mood == MascotMood.THINKING || mood == MascotMood.WORKING) s * 0.025f else 0f
    drawLine(Outline, Offset(leftEye.x - eyeW / 2, browY + tilt), Offset(leftEye.x + eyeW / 2, browY - tilt), stroke, StrokeCap.Round)
    drawLine(Outline, Offset(rightEye.x - eyeW / 2, browY - tilt - raise), Offset(rightEye.x + eyeW / 2, browY + tilt - raise * 1.3f), stroke, StrokeCap.Round)

    // Usta
    val mouth = Offset(body.x, body.y + s * 0.1f)
    val mw = s * 0.12f
    when (mood) {
        MascotMood.HAPPY, MascotMood.CHEER, MascotMood.WAVE -> {
            val path = Path().apply {
                moveTo(mouth.x - mw / 2, mouth.y - s * 0.015f)
                quadraticTo(mouth.x, mouth.y + s * 0.09f, mouth.x + mw / 2, mouth.y - s * 0.015f)
                close()
            }
            drawPath(path, Outline)
        }
        MascotMood.SAD -> drawArc(Outline, 200f, 140f, false, Offset(mouth.x - mw / 2, mouth.y), Size(mw, s * 0.06f), style = Stroke(stroke, cap = StrokeCap.Round))
        MascotMood.THINKING, MascotMood.WORKING, MascotMood.FREEZE ->
            drawLine(Outline, Offset(mouth.x - mw / 3, mouth.y + s * 0.01f), Offset(mouth.x + mw / 3, mouth.y), stroke, StrokeCap.Round)
        MascotMood.SLEEPING -> drawCircle(Outline, s * 0.018f, mouth, style = Stroke(stroke * 0.8f))
        else -> drawArc(Outline, 20f, 140f, false, Offset(mouth.x - mw / 2, mouth.y - s * 0.04f), Size(mw, s * 0.06f), style = Stroke(stroke, cap = StrokeCap.Round))
    }
}

private fun DrawScope.drawExtras(mood: MascotMood, s: Float, stroke: Float, phase: Float) {
    when (mood) {
        MascotMood.SLEEPING -> {
            val y = s * (0.18f - 0.04f * phase)
            zz(Offset(s * 0.72f, y), s * 0.05f, stroke)
            zz(Offset(s * 0.82f, y - s * 0.08f), s * 0.035f, stroke)
        }
        MascotMood.CHEER -> {
            val colors = listOf(Color(0xFFF7CD63), Color(0xFFFC8FC6), Color(0xFFA9C8F5), Color(0xFFB8EB6C))
            for (i in 0 until 12) {
                val x = s * (0.08f + 0.075f * i)
                val y = s * ((0.05f + 0.13f * ((i * 37) % 5) / 5f + phase * 0.3f) % 0.5f)
                drawRect(colors[i % colors.size], Offset(x, y), Size(s * 0.03f, s * 0.018f))
            }
        }
        MascotMood.WORKING -> drawOval(Color(0xFFA9C8F5), Offset(s * 0.7f, s * (0.22f + 0.03f * phase)), Size(s * 0.035f, s * 0.05f))
        else -> Unit
    }
}

private fun DrawScope.zz(origin: Offset, w: Float, stroke: Float) {
    val path = Path().apply {
        moveTo(origin.x, origin.y)
        lineTo(origin.x + w, origin.y)
        lineTo(origin.x, origin.y + w)
        lineTo(origin.x + w, origin.y + w)
    }
    drawPath(path, Outline, style = Stroke(stroke * 0.8f, cap = StrokeCap.Round))
}
