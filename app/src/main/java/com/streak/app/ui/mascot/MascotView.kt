package com.streak.app.ui.mascot

import android.view.TextureView
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.streak.app.R
import com.streak.app.domain.model.AnimationLevel
import com.streak.app.ui.theme.LocalAnimationLevel

private const val ASPECT = 240f / 260f

// Maskota. hero = true: animiran 3D model (Filament) v kljucnih trenutkih (dobrodoslica,
// praznovanje, seja). Sicer - in na sibkih napravah, z zmanjsanimi ali izklopljenimi
// animacijami - staticna poza, zrenderirana iz istega 3D modela.
@Composable
fun MascotView(
    mood: MascotMood,
    modifier: Modifier = Modifier,
    description: String? = null,
    hero: Boolean = false
) {
    val context = LocalContext.current
    val label = description ?: stringResource(mood.description)
    val animated = LocalAnimationLevel.current == AnimationLevel.FULL
    val use3d = hero && animated && remember { DeviceCapabilities.supports3d(context) }
    Box(modifier.aspectRatio(ASPECT).semantics { contentDescription = label }) {
        if (use3d) {
            var ready by remember { mutableStateOf(false) }
            Mascot3D(mood, Modifier.fillMaxSize(), onReady = { ready = true })
            // Med nalaganjem modela (hladen zagon) staticna poza istega lika
            if (!ready) MascotPose(mood)
        } else {
            MascotPose(mood)
        }
    }
}

@Composable
private fun MascotPose(mood: MascotMood) {
    Image(
        painter = painterResource(poseDrawable(mood)),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun Mascot3D(mood: MascotMood, modifier: Modifier, onReady: () -> Unit) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    // Izris le, ko je zaslon viden (lifecycle RESUMED); ob izhodu sprosti GPU vire
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextureView(ctx).also { tv ->
                val scene = MascotScene(ctx, tv).apply {
                    if (DeviceCapabilities.reducedFrameRate(ctx)) frameIntervalNanos = 33_000_000L
                    onFirstFrame = onReady
                }
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_RESUME -> scene.resume()
                        Lifecycle.Event.ON_PAUSE -> scene.pause()
                        else -> Unit
                    }
                }
                lifecycle.addObserver(observer)
                tv.tag = SceneHandle(scene) { lifecycle.removeObserver(observer) }
            }
        },
        update = { tv -> (tv.tag as SceneHandle).scene.setAnimation(animationFor(mood)) },
        onRelease = { tv -> (tv.tag as SceneHandle).release() }
    )
}

private class SceneHandle(val scene: MascotScene, private val unregister: () -> Unit) {
    fun release() {
        unregister()
        scene.destroy()
    }
}

// Ime animacije v GLB za vsako stanje
internal fun animationFor(mood: MascotMood): String = when (mood) {
    MascotMood.IDLE -> "idle"
    MascotMood.WAVE -> "wave"
    MascotMood.WALK -> "walk"
    MascotMood.HAPPY, MascotMood.CHEER, MascotMood.STRETCHING -> "cheer"
    MascotMood.SAD -> "sad"
    MascotMood.THINKING, MascotMood.WORKING -> "think"
    MascotMood.SLEEPING, MascotMood.FREEZE -> "sleep"
    MascotMood.MEDITATING -> "meditate"
    MascotMood.THUMBS_UP, MascotMood.VACATION -> "thumbs"
}

// Vnaprej zrenderirana poza istega modela (tools/mascot/build_mascot.py)
@DrawableRes
fun poseDrawable(mood: MascotMood): Int = when (animationFor(mood)) {
    "wave" -> R.drawable.mascot_wave
    "walk" -> R.drawable.mascot_walk
    "cheer" -> R.drawable.mascot_cheer
    "sad" -> R.drawable.mascot_sad
    "think" -> R.drawable.mascot_think
    "sleep" -> R.drawable.mascot_sleep
    "meditate" -> R.drawable.mascot_meditate
    "thumbs" -> R.drawable.mascot_thumbs
    else -> R.drawable.mascot_idle
}
