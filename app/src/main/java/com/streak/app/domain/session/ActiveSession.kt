package com.streak.app.domain.session

import java.time.Instant

// Ambientni zvok med sejo (generiran na napravi, brez datotek)
enum class AmbientSound { NONE, BROWN_NOISE, RAIN, OCEAN, FIRE }

// Trenutek, izmerjen z dvema urama: monotono (elapsedRealtime, neodvisno od spremembe ure)
// in stensko (za primer ponovnega zagona naprave, ko se elapsedRealtime ponastavi)
data class TimePoint(val elapsedMs: Long, val wall: Instant, val bootCount: Int)

// Tekoca seja. Shrani se ob vsaki spremembi, zato prezivi smrt procesa, ugasnjen zaslon
// in ponovni zagon naprave.
data class ActiveSession(
    val habitId: String,
    val title: String,
    // Cilj v sekundah (0 = brez cilja)
    val targetSeconds: Long,
    val startedAt: Instant,
    // Cas, nabran pred zadnjim nadaljevanjem
    val accumulatedMs: Long = 0,
    // Zacetek trenutnega teka; null = pavza
    val runningSince: TimePoint? = null,
    val ambient: AmbientSound = AmbientSound.NONE,
    // Ali je bilo obvestilo "cilj dosezen" ze prikazano
    val goalNotified: Boolean = false
) {
    val running: Boolean get() = runningSince != null

    // Koliko casa se manjka do cilja (null, ce cilja ni ali je ze dosezen)
    fun remainingToGoalMs(now: TimePoint): Long? =
        if (targetSeconds <= 0) null else (targetSeconds * 1000 - elapsedMs(now)).takeIf { it > 0 }

    fun elapsedMs(now: TimePoint): Long {
        val since = runningSince ?: return accumulatedMs
        val run = if (since.bootCount == now.bootCount && now.elapsedMs >= since.elapsedMs) {
            now.elapsedMs - since.elapsedMs
        } else {
            // Naprava se je vmes ponovno zagnala: zanesemo se na stensko uro (nikoli negativno)
            (now.wall.toEpochMilli() - since.wall.toEpochMilli()).coerceAtLeast(0)
        }
        return accumulatedMs + run
    }

    fun pause(now: TimePoint): ActiveSession =
        if (!running) this else copy(accumulatedMs = elapsedMs(now), runningSince = null)

    fun resume(now: TimePoint): ActiveSession =
        if (running) this else copy(runningSince = now)

    companion object {
        fun start(habitId: String, title: String, targetSeconds: Long, now: TimePoint, ambient: AmbientSound = AmbientSound.NONE) =
            ActiveSession(habitId, title, targetSeconds, startedAt = now.wall, runningSince = now, ambient = ambient)
    }
}
