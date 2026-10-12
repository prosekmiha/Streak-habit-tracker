package com.streak.app.domain.usecase

import com.streak.app.domain.repository.SettingsRepository
import com.streak.app.domain.time.ClockSanity
import com.streak.app.domain.time.DayClock
import javax.inject.Inject

// Vzdrzuje "zadnji znani dan" za zaznavo skoka ure. Klice se ob naravnih menjavah dneva
// (tudi v ozadju), da dnevi brez odpiranja aplikacije ne sprozijo laznega opozorila.
class TrackKnownDateUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val dayClock: DayClock
) {
    suspend operator fun invoke(afterReboot: Boolean = false) {
        val today = dayClock.today()
        settingsRepository.update { s ->
            ClockSanity.advance(s.lastKnownDate, today, afterReboot)?.let { s.copy(lastKnownDate = it) } ?: s
        }
    }
}
