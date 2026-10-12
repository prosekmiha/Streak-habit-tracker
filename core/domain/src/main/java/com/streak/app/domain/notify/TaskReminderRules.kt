package com.streak.app.domain.notify

import com.streak.app.domain.model.Task
import com.streak.app.domain.model.TaskReminder
import java.time.LocalDate
import java.time.ZonedDateTime

// Naslednji sprozilec opomnika naloge in termin, na katerega se nanasa
data class TaskTrigger(val at: ZonedDateTime, val occurrence: LocalDate)

// Cista pravila za opomnike nalog (brez Androida, pokrita z unit testi)
object TaskReminderRules {
    // Kako dalec naprej se isce termin ponavljajoce naloge
    private const val HORIZON_DAYS = 400L

    // Najzgodnejsi sprozilec strogo po "now" cez vse opomnike in termine; null, ce ga ni
    // (opravljena, brez roka ali brez opomnikov). Ure v vrzeli DST se premaknejo naprej.
    fun nextTrigger(task: Task, now: ZonedDateTime): TaskTrigger? {
        if (task.isDone || task.reminders.isEmpty()) return null
        val due = task.dueDate ?: return null
        // Opomnik lahko pride najvec "lead" dni pred terminom
        val lead = task.reminders.maxOf { leadDays(it, task) }
        val today = now.toLocalDate()
        var best: TaskTrigger? = null
        var date = maxOf(due, today)
        val last = today.plusDays(HORIZON_DAYS)
        while (!date.isAfter(last)) {
            // Kasnejsi termini ne morejo dati zgodnejsega sprozilca
            val found = best
            if (found != null && date.minusDays(lead).isAfter(found.at.toLocalDate())) break
            if (task.occursOn(date)) {
                for (r in task.reminders) {
                    val at = triggerFor(r, task, date, now) ?: continue
                    val current = best
                    if (current == null || at.isBefore(current.at)) best = TaskTrigger(at, date)
                }
            }
            if (!task.isRecurring) break
            date = date.plusDays(1)
        }
        return best
    }

    // Prvi sprozilec opomnika za termin "date", ki je po "now"
    private fun triggerFor(r: TaskReminder, task: Task, date: LocalDate, now: ZonedDateTime): ZonedDateTime? {
        val zone = now.zone
        return when (r) {
            is TaskReminder.BeforeDue -> task.dueTime?.let { date.atTime(it).atZone(zone).minusMinutes(r.minutes.toLong()) }
            is TaskReminder.DayAt -> date.minusDays(r.daysBefore.toLong()).atTime(r.time).atZone(zone)
            is TaskReminder.Repeat -> {
                val step = r.everyMinutes.coerceAtLeast(TaskReminder.MIN_INTERVAL).toLong()
                // Okno "from" za "until" je prazno (sicer bi se sprozil "from" izven okna)
                generateSequence(r.from.takeIf { it <= r.until }) { t -> t.plusMinutes(step).takeIf { it > t && it <= r.until } }
                    .map { date.atTime(it).atZone(zone) }
                    .firstOrNull { it.isAfter(now) }
            }
        }?.takeIf { it.isAfter(now) }
    }

    private fun leadDays(r: TaskReminder, task: Task): Long = when (r) {
        is TaskReminder.BeforeDue -> if (task.dueTime == null) 0 else (r.minutes / (24 * 60) + 1).toLong()
        is TaskReminder.DayAt -> r.daysBefore.toLong()
        is TaskReminder.Repeat -> 0
    }
}
