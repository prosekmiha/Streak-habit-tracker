package com.streak.app.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.streak.app.domain.repository.HabitRepository
import com.streak.app.domain.repository.TaskRepository
import com.streak.app.notifications.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate

// Dnevni job ob polnoci: oznaci vceraj neopravljene navade kot MISSED in
// pripravi jutranji "briefing".
@HiltWorker
class DailyResetWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val habitRepository: HabitRepository,
    private val taskRepository: TaskRepository,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        // Oznaci vcerajsnje neopravljene navade kot MISSED
        habitRepository.markMissedFor(LocalDate.now().minusDays(1))

        // Pripravi dnevni briefing
        val habitCount = habitRepository.getHabitsOnce().size
        val tasksToday = taskRepository.observeTasksForDate(LocalDate.now()).first().count { !it.isDone }
        notificationHelper.showBriefing("Danes imaš $habitCount navad in $tasksToday nalog")

        return Result.success()
    }

    companion object { const val WORK_NAME = "daily_reset" }
}
