package com.streak.app.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.streak.app.R
import com.streak.app.data.repository.StreakRecalculator
import com.streak.app.domain.model.StreakUnit
import com.streak.app.domain.repository.HabitRepository
import com.streak.app.domain.streak.DayEvaluator
import com.streak.app.domain.time.DayClock
import com.streak.app.notifications.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

// Job ob menjavi logicnega dne: posodobi streake in povzetke (trenutni streak je
// odvisen od "danes") in pripravi jutranji briefing. Zgresenih dni NE zapisuje -
// izpeljejo se iz urnika.
@HiltWorker
class DailyResetWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val habitRepository: HabitRepository,
    private val recalculator: StreakRecalculator,
    private val dayClock: DayClock,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        recalculator.onDayChanged()

        val today = dayClock.today()
        val dueToday = habitRepository.getHabits().count { habit ->
            habit.isActiveOn(today) && (habit.schedule.streakUnit != StreakUnit.DAY || DayEvaluator.isScheduled(habit, today))
        }
        if (dueToday > 0) {
            val text = applicationContext.resources.getQuantityString(R.plurals.notif_briefing_habits, dueToday, dueToday)
            notificationHelper.showBriefing(text)
        }
        return Result.success()
    }

    companion object { const val WORK_NAME = "daily_reset" }
}
