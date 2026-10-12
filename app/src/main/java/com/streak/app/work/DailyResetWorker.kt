package com.streak.app.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.streak.app.data.repository.StreakRecalculator
import com.streak.app.domain.usecase.TrackKnownDateUseCase
import com.streak.app.widget.WidgetUpdater
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

// Job ob menjavi logicnega dne: posodobi streake in povzetke (trenutni streak je
// odvisen od "danes"). Zgresenih dni NE zapisuje - izpeljejo se iz urnika. Jutranji
// povzetek je locen alarm ob nastavljeni uri (ne ob polnoci).
@HiltWorker
class DailyResetWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val recalculator: StreakRecalculator,
    private val widgetUpdater: WidgetUpdater,
    private val trackKnownDate: TrackKnownDateUseCase
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        recalculator.onDayChanged()
        // Naravna menjava dneva: tudi brez odpiranja aplikacije ni lazne zaznave skoka ure
        trackKnownDate()
        // Widgeti morajo pokazati nov dan, tudi ce proces takoj nato konca
        widgetUpdater.updateAll()
        return Result.success()
    }

    companion object { const val WORK_NAME = "daily_reset" }
}
