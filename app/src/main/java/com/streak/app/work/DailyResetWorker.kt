package com.streak.app.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.streak.app.data.repository.StreakRecalculator
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

// Job ob menjavi logicnega dne: posodobi streake in povzetke (trenutni streak je
// odvisen od "danes"). Zgresenih dni NE zapisuje - izpeljejo se iz urnika. Jutranji
// povzetek je locen alarm ob nastavljeni uri (ne ob polnoci).
@HiltWorker
class DailyResetWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val recalculator: StreakRecalculator
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        recalculator.onDayChanged()
        return Result.success()
    }

    companion object { const val WORK_NAME = "daily_reset" }
}
