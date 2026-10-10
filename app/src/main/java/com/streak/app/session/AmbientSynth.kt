package com.streak.app.session

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.streak.app.domain.session.AmbientSound
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.max

// Ambientni zvoki, generirani v realnem casu (brez zvocnih datotek, brez omrezja).
// Neskoncni, brez ponavljanja zanke; mehak vklop, izklop in prehod med zvoki.
internal class AmbientSynth {
    @Volatile private var requested = AmbientSound.NONE
    @Volatile private var muted = false
    @Volatile private var stopRequested = false
    private var thread: Thread? = null

    fun play(sound: AmbientSound) {
        requested = sound
        if (thread == null) {
            thread = Thread(::loop, "ambient-synth").apply {
                priority = Thread.MAX_PRIORITY
                start()
            }
        }
    }

    fun setMuted(value: Boolean) {
        muted = value
    }

    // Postopno utisa in sprosti AudioTrack (nit se konca sama)
    fun release() {
        stopRequested = true
    }

    private fun loop() {
        val minBuffer = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_FLOAT)
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(max(minBuffer, SAMPLE_RATE / 4 * 4))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        track.play()

        val buffer = FloatArray(CHUNK)
        var currentSound = requested
        var current: AmbientVoice? = ambientVoice(currentSound)
        var previous: AmbientVoice? = null
        var crossfade = 1f
        var gain = 0f
        val fadeIn = 1f / (SAMPLE_RATE * 3f)
        val fadeOut = 1f / (SAMPLE_RATE * 1.2f)
        val crossStep = 1f / (SAMPLE_RATE * 1.5f)

        try {
            while (true) {
                val want = requested
                if (want != currentSound) {
                    previous = current
                    current = ambientVoice(want)
                    currentSound = want
                    crossfade = 0f
                }
                val target = if (stopRequested || muted) 0f else 1f
                for (i in 0 until CHUNK) {
                    gain = if (gain < target) minOf(target, gain + fadeIn) else maxOf(target, gain - fadeOut)
                    if (crossfade < 1f) crossfade = minOf(1f, crossfade + crossStep)
                    val a = current?.next() ?: 0f
                    val b = if (crossfade < 1f) previous?.next() ?: 0f else 0f
                    buffer[i] = softClip((a * crossfade + b * (1f - crossfade)) * gain * VOLUME)
                }
                if (crossfade >= 1f) previous = null
                track.write(buffer, 0, CHUNK, AudioTrack.WRITE_BLOCKING)
                if (stopRequested && gain <= 0f) break
            }
        } finally {
            runCatching { track.stop() }
            track.release()
        }
    }

    private companion object {
        const val SAMPLE_RATE = 44_100
        const val CHUNK = 1024
        const val VOLUME = 0.55f

    }
}

// Mehka omejitev (aproksimacija tanh), da glasni vrhovi ne pokajo
internal fun softClip(x: Float): Float {
    val c = x.coerceIn(-3f, 3f)
    return c * (27f + c * c) / (27f + 9f * c * c)
}

// Generator vzorcev za izbran zvok (44,1 kHz, mono); null = tisina
internal fun ambientVoice(sound: AmbientSound): AmbientVoice? = when (sound) {
    AmbientSound.NONE -> null
    AmbientSound.BROWN_NOISE -> BrownNoiseVoice()
    AmbientSound.RAIN -> RainVoice()
    AmbientSound.OCEAN -> OceanVoice()
    AmbientSound.FIRE -> FireVoice()
}

// --- Gradniki -------------------------------------------------------------------------

internal abstract class AmbientVoice {
    private var seed = (System.nanoTime().toInt() or 1)

    // Beli sum v [-1, 1) (xorshift32)
    protected fun white(): Float {
        var x = seed
        x = x xor (x shl 13)
        x = x xor (x ushr 17)
        x = x xor (x shl 5)
        seed = x
        return x / 2_147_483_648f
    }

    protected fun random01(): Float = white() * 0.5f + 0.5f

    abstract fun next(): Float
}

private const val RATE = 44_100f

// Enopolni nizkoprepustni filter
private class LowPass(cutoffHz: Float) {
    private val a = 1f - exp(-2.0 * PI * cutoffHz / RATE).toFloat()
    private var y = 0f
    fun process(x: Float): Float {
        y += a * (x - y)
        return y
    }
}

private class HighPass(cutoffHz: Float) {
    private val lp = LowPass(cutoffHz)
    fun process(x: Float) = x - lp.process(x)
}

// Roza sum (filter Paula Kelleta)
private class Pink {
    private var b0 = 0f; private var b1 = 0f; private var b2 = 0f
    private var b3 = 0f; private var b4 = 0f; private var b5 = 0f; private var b6 = 0f
    fun process(w: Float): Float {
        b0 = 0.99886f * b0 + w * 0.0555179f
        b1 = 0.99332f * b1 + w * 0.0750759f
        b2 = 0.96900f * b2 + w * 0.1538520f
        b3 = 0.86650f * b3 + w * 0.3104856f
        b4 = 0.55000f * b4 + w * 0.5329522f
        b5 = -0.7616f * b5 - w * 0.0168980f
        val out = b0 + b1 + b2 + b3 + b4 + b5 + b6 + w * 0.5362f
        b6 = w * 0.115926f
        return out * 0.11f
    }
}

// Rjavi sum (integriran beli sum z uhajanjem)
private class Brown {
    private var last = 0f
    fun process(w: Float): Float {
        last = (last + 0.02f * w) / 1.02f
        return last * 3.5f
    }
}

// Pocasno nakljucno nihanje v [lo, hi] (za naravno spreminjanje jakosti)
private class Drift(private val lo: Float, private val hi: Float, seconds: Float) {
    private var value = (lo + hi) / 2
    private var target = value
    private val step = 1f / (RATE * seconds)
    fun next(random01: Float, retarget: Boolean): Float {
        if (retarget) target = lo + (hi - lo) * random01
        value += (target - value) * step * 4f
        return value
    }
}

private fun smoothstep(t: Float): Float {
    val x = t.coerceIn(0f, 1f)
    return x * x * (3f - 2f * x)
}

// --- Zvoki ----------------------------------------------------------------------------

private class BrownNoiseVoice : AmbientVoice() {
    private val brown = Brown()
    private val soften = LowPass(1800f)
    override fun next() = soften.process(brown.process(white())) * 0.9f
}

private class RainVoice : AmbientVoice() {
    private val pink = Pink()
    private val bed = HighPass(500f)
    private val bedSoft = LowPass(7000f)
    private val dropFilter = HighPass(1200f)
    private val intensity = Drift(0.7f, 1f, 4f)
    private var dropEnv = 0f
    private var dropDecay = 0f
    private var counter = 0

    override fun next(): Float {
        val level = intensity.next(random01(), counter++ % 22_050 == 0)
        val base = bedSoft.process(bed.process(pink.process(white()))) * 0.55f * level
        if (random01() < 70f / RATE) {
            val r = random01()
            dropEnv = 0.12f + 0.55f * r * r
            dropDecay = exp(-1.0 / (RATE * (0.0015 + 0.006 * random01()))).toFloat()
        }
        val drop = dropFilter.process(white() * dropEnv)
        dropEnv *= dropDecay
        return base + drop * 0.5f
    }
}

private class OceanVoice : AmbientVoice() {
    private val pink = Pink()
    private val brown = Brown()
    private var lp = 0f
    private var phase = 0f
    private var period = 9f
    private var peak = 0.8f

    override fun next(): Float {
        val noise = brown.process(white()) * 0.7f + pink.process(white()) * 0.5f
        phase += 1f / (period * RATE)
        if (phase >= 1f) {
            phase -= 1f
            period = 7f + 5f * random01()
            peak = 0.6f + 0.4f * random01()
        }
        // Val hitro naraste in pocasi odteka
        val env = peak * if (phase < 0.4f) smoothstep(phase / 0.4f) else 1f - smoothstep((phase - 0.4f) / 0.6f)
        val cutoff = 250f + 2200f * env
        lp += (2f * PI.toFloat() * cutoff / RATE) * (noise - lp)
        return lp * (0.25f + 0.9f * env) * 1.4f
    }
}

private class FireVoice : AmbientVoice() {
    private val brown = Brown()
    private val rumble = LowPass(180f)
    private val pink = Pink()
    private val hiss = HighPass(2000f)
    private val flicker = Drift(0.4f, 1f, 0.6f)
    private var env = 0f
    private var decay = 0f
    private var burstLeft = 0
    private var burstWait = 0
    private var counter = 0

    override fun next(): Float {
        val low = rumble.process(brown.process(white())) * 1.6f
        val fl = flicker.next(random01(), counter++ % 4_410 == 0)
        val high = hiss.process(pink.process(white())) * 0.05f * fl
        if (burstLeft > 0 && --burstWait <= 0) {
            burstLeft--
            crackle()
            burstWait = (RATE * (0.005f + 0.035f * random01())).toInt()
        } else if (random01() < 4f / RATE) {
            crackle()
            burstLeft = (random01() * 4).toInt()
            burstWait = (RATE * (0.005f + 0.035f * random01())).toInt()
        }
        val c = white() * env
        env *= decay
        return low + high + c * 0.7f
    }

    private fun crackle() {
        val r = random01()
        env = 0.3f + 0.7f * r * r
        decay = exp(-1.0 / (RATE * (0.0003 + 0.0012 * random01()))).toFloat()
    }
}
