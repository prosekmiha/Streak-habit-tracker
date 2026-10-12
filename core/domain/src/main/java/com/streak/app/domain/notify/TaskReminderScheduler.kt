package com.streak.app.domain.notify

import com.streak.app.domain.model.Task

// Razporeja opomnike nalog: en alarm na nalogo, ob sprozitvi se razporedi naslednji
interface TaskReminderScheduler {
    // Preklice obstojeci alarm in nastavi naslednjega (opravljena naloga: pospravi tudi obvestilo)
    fun schedule(task: Task)

    // Alarm in prikazano obvestilo
    fun cancel(taskId: String)

    // Po zagonu, spremembi ure ali uvozu
    suspend fun rescheduleAll()
}
