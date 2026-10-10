package com.streak.app.session

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.streak.app.R
import com.streak.app.domain.session.ActiveSession
import com.streak.app.domain.session.AmbientSound
import com.streak.app.notifications.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

// Foreground storitev (mediaPlayback) le, medtem ko igra ambientni zvok. Uporablja isto
// obvestilo kot seja; ob koncu zvoka obvestilo ostane, storitev pa se ustavi.
@AndroidEntryPoint
class AmbientService : Service() {

    @Inject lateinit var controller: SessionController
    @Inject lateinit var notifier: SessionNotifier

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var synth = AmbientSynth()
    private var observer: Job? = null
    private var goalTimer: Job? = null
    private var focusRequest: AudioFocusRequest? = null
    private var stopped = false
    private var lastStartId = 0

    private val audioManager get() = getSystemService(AudioManager::class.java)

    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            // Druga aplikacija trajno predvaja zvok: utisaj ambient
            AudioManager.AUDIOFOCUS_LOSS -> scope.launch { controller.setAmbient(AmbientSound.NONE) }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> synth.setMuted(true)
            AudioManager.AUDIOFOCUS_GAIN -> synth.setMuted(false)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lastStartId = startId
        stopped = false
        val session = controller.active.value
        // startForeground mora biti poklican vedno, tudi ce se takoj ustavimo
        ServiceCompat.startForeground(
            this,
            SessionNotifier.NOTIFICATION_ID,
            session?.let { notifier.build(it, controller.now()) } ?: placeholder(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0
        )
        onState(session)
        if (observer == null) {
            observer = scope.launch { controller.active.collect { onState(it) } }
        }
        return START_NOT_STICKY
    }

    private fun onState(session: ActiveSession?) {
        if (stopped) return
        if (session == null || !session.running || session.ambient == AmbientSound.NONE) {
            stopPlayback()
        } else {
            requestFocus()
            synth.play(session.ambient)
            scheduleGoal(session)
        }
    }

    // Proces med predvajanjem zivi, zato cilj sprozimo tocno (alarm je lahko netocen)
    private fun scheduleGoal(session: ActiveSession) {
        goalTimer?.cancel()
        val remaining = session.remainingToGoalMs(controller.now())
        if (session.goalNotified || remaining == null) return
        goalTimer = scope.launch {
            delay(remaining)
            controller.onGoalReached()
        }
    }

    private fun stopPlayback() {
        if (stopped) return
        stopped = true
        goalTimer?.cancel()
        synth.release()
        synth = AmbientSynth()
        abandonFocus()
        val session = controller.active.value
        ServiceCompat.stopForeground(
            this,
            if (session == null) ServiceCompat.STOP_FOREGROUND_REMOVE else ServiceCompat.STOP_FOREGROUND_DETACH
        )
        // Obvestilo seje ostane kot navadno (ne vec vezano na storitev)
        session?.let { notifier.show(it, controller.now()) }
        // Ne ustavi, ce je medtem prisel nov zagon (npr. hitro Premor -> Nadaljuj)
        stopSelf(lastStartId)
    }

    private fun requestFocus() {
        if (focusRequest != null) return
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setOnAudioFocusChangeListener(focusListener)
            .build()
        audioManager.requestAudioFocus(request)
        focusRequest = request
    }

    private fun abandonFocus() {
        focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        focusRequest = null
    }

    private fun placeholder() = NotificationCompat.Builder(this, NotificationHelper.CHANNEL_SESSION)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(getString(R.string.notif_session_generic))
        .setSilent(true)
        .build()

    override fun onDestroy() {
        synth.release()
        abandonFocus()
        scope.cancel()
        super.onDestroy()
    }
}
