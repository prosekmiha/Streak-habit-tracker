package com.streak.app.domain.notify

import com.streak.app.domain.model.AppSettings
import com.streak.app.domain.model.Habit

// Razporeja alarme: opomnike navad, odlog, jutranji povzetek, vecerni opomnik in menjavo dne
interface ReminderScheduler {
    fun canScheduleExact(): Boolean

    // Po zagonu naprave, posodobitvi, spremembi ure/pasu ali dovoljenja
    suspend fun rescheduleAll()

    // Naslednja pojavitev vsakega vklopljenega opomnika navade
    fun schedule(habit: Habit)

    fun cancel(habitId: String, includeSnooze: Boolean = true)

    fun cancel(habit: Habit) = cancel(habit.id)

    // "Cez 15 min" iz obvestila
    fun snooze(habitId: String, minutes: Long = SNOOZE_MINUTES)

    // Alarm ob meji logicnega dne ("dan se konca ob")
    suspend fun scheduleDailyReset(settings: AppSettings? = null)

    // Jutranji povzetek in vecerni opomnik ob nastavljenih urah
    suspend fun scheduleDaily(settings: AppSettings? = null)

    companion object {
        const val MAX_REMINDERS = 8
        const val SNOOZE_MINUTES = 15L
    }
}
