package com.streak.app.session

import com.streak.app.domain.session.AmbientSound
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt

// Generatorji ambientnih zvokov: smiselna glasnost, brez NaN, brez rezanja in pricakovan znacaj
class AmbientVoiceTest {

    private val rate = 44_100

    private fun render(sound: AmbientSound, seconds: Int): FloatArray {
        val voice = checkNotNull(ambientVoice(sound))
        return FloatArray(rate * seconds) { softClip(voice.next() * 0.55f) }
    }

    private fun rms(x: FloatArray, from: Int = 0, to: Int = x.size): Double {
        var sum = 0.0
        for (i in from until to) sum += x[i] * x[i]
        return sqrt(sum / (to - from))
    }

    private fun zeroCrossingRate(x: FloatArray): Double =
        (1 until x.size).count { (x[it - 1] < 0f) != (x[it] < 0f) }.toDouble() / x.size

    // Razmerje med najglasnejso in najtisjo sekundo (val ima izrazit utrip)
    private fun loudnessSpread(x: FloatArray): Double {
        val perSecond = (0 until x.size / rate).map { rms(x, it * rate, (it + 1) * rate) }
        return perSecond.max() / perSecond.min()
    }

    @Test
    fun `none is silence`() = assertNull(ambientVoice(AmbientSound.NONE))

    @Test
    fun `all sounds are finite, audible and not clipping`() {
        AmbientSound.entries.filter { it != AmbientSound.NONE }.forEach { sound ->
            val x = render(sound, 20)
            assertTrue("$sound ima NaN", x.all { it.isFinite() })
            val level = rms(x)
            assertTrue("$sound prevec tih: $level", level > 0.03)
            assertTrue("$sound preglasen: $level", level < 0.5)
            val hot = x.count { abs(it) > 0.95f }.toDouble() / x.size
            assertTrue("$sound se reze: $hot", hot < 0.001)
        }
    }

    @Test
    fun `sounds have their own character`() {
        val brown = render(AmbientSound.BROWN_NOISE, 20)
        val rain = render(AmbientSound.RAIN, 20)
        val ocean = render(AmbientSound.OCEAN, 40)
        // Dez je svetel (visoke frekvence), rjavi sum temen
        assertTrue(zeroCrossingRate(rain) > 4 * zeroCrossingRate(brown))
        // Valovi prihajajo in odhajajo, rjavi sum je enakomeren
        assertTrue(loudnessSpread(ocean) > 2.0)
        assertTrue(loudnessSpread(brown) < 1.6)
    }
}
