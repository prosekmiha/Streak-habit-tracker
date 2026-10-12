package com.streak.app.domain.model

import java.time.LocalTime

// Opomnik naloge; velja za vsak termin naloge (enkratna: rok, ponavljajoca: vsak dan po pravilu)
sealed interface TaskReminder {
    // Minute pred rokom (0 = ob roku); zahteva uro roka
    data class BeforeDue(val minutes: Int) : TaskReminder

    // Ob uri na dan roka ali n dni prej (0 = isti dan)
    data class DayAt(val daysBefore: Int, val time: LocalTime) : TaskReminder

    // Na dan roka vsakih n minut med "from" in "until", dokler naloga ni opravljena
    data class Repeat(val everyMinutes: Int, val from: LocalTime, val until: LocalTime) : TaskReminder

    companion object {
        const val MAX = 5
        const val MIN_INTERVAL = 5

        // Zapis v bazi: "B60", "D1@1200", "R60@540-1260", loceni s ";"
        fun encode(reminders: List<TaskReminder>): String = reminders.joinToString(";") {
            when (it) {
                is BeforeDue -> "B${it.minutes}"
                is DayAt -> "D${it.daysBefore}@${it.time.toMinute()}"
                is Repeat -> "R${it.everyMinutes}@${it.from.toMinute()}-${it.until.toMinute()}"
            }
        }

        // Neveljavni deli se preskocijo
        fun decode(text: String?): List<TaskReminder> =
            text.orEmpty().split(";").filter { it.isNotBlank() }.mapNotNull { runCatching { decodeOne(it) }.getOrNull() }

        private fun decodeOne(s: String): TaskReminder? {
            val body = s.substring(1)
            return when (s[0]) {
                'B' -> BeforeDue(body.toInt().coerceAtLeast(0))
                'D' -> body.split("@").let { (d, m) -> DayAt(d.toInt().coerceAtLeast(0), m.toInt().toTime()) }
                'R' -> body.split("@").let { (every, window) ->
                    val (from, until) = window.split("-")
                    Repeat(every.toInt().coerceAtLeast(MIN_INTERVAL), from.toInt().toTime(), until.toInt().toTime())
                }
                else -> null
            }
        }

        private fun LocalTime.toMinute() = hour * 60 + minute
        private fun Int.toTime(): LocalTime = LocalTime.of(this / 60 % 24, this % 60)
    }
}
