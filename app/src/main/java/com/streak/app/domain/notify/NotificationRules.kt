package com.streak.app.domain.notify

import com.streak.app.domain.model.DayState
import com.streak.app.domain.model.Habit
import com.streak.app.domain.model.HabitWithStreak
import java.time.LocalTime
import java.time.ZonedDateTime

// Cista pravila za obvestila (brez Androida, pokrita z unit testi)
object NotificationRules {

    // Naslednja pojavitev ure po trenutku "now" v njegovem casovnem pasu. V vrzeli DST
    // (ura ne obstaja) se premakne naprej, ob prekrivanju se uporabi prva pojavitev.
    fun nextOccurrence(now: ZonedDateTime, time: LocalTime): ZonedDateTime {
        val zone = now.zone
        val today = now.toLocalDate().atTime(time).atZone(zone)
        return if (today.isAfter(now)) today else now.toLocalDate().plusDays(1).atTime(time).atZone(zone)
    }

    // Ali je ura v tihih urah; obdobje lahko prečka polnoč (npr. 22:00-07:00)
    fun inQuietHours(start: LocalTime?, end: LocalTime?, time: LocalTime): Boolean {
        if (start == null || end == null || start == end) return false
        return if (start < end) time >= start && time < end else time >= start || time < end
    }

    // Opomnik ima smisel le za navado, ki je danes na sporedu in se ni opravljena
    // (preskok, zamrznitev in dopust ne potrebujejo opomnika)
    fun needsReminder(state: DayState?): Boolean = state == DayState.PENDING || state == DayState.PARTIAL

    fun pendingToday(habits: List<HabitWithStreak>): List<Habit> =
        habits.filter { !it.habit.isArchived && needsReminder(it.todayState) }.map { it.habit }

    // Besedilo vecernega opomnika: najvec tri imena, ostalo kot stevilo
    fun nudgeNames(pending: List<Habit>, max: Int = 3): Pair<List<String>, Int> =
        pending.take(max).map { it.title } to (pending.size - max).coerceAtLeast(0)
}
