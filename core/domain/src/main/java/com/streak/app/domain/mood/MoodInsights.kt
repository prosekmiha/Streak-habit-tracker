package com.streak.app.domain.mood

import com.streak.app.domain.model.Completion
import com.streak.app.domain.model.CompletionStatus
import com.streak.app.domain.model.DailyAggregate
import com.streak.app.domain.model.MoodEntry

// Povzetek razpolozenja za obdobje in povezava z navadami.
// Primerjave se pokazejo le, ce je na obeh straneh dovolj dni (MIN_DAYS), sicer bi bile nakljucne.
data class MoodStats(
    val entries: Int,
    val average: Double?,
    // Povprecje ob dneh z vsaj GOOD_DAY_RATIO opravljenih navad in ob ostalih dneh z navadami
    val goodDaysAverage: Double?,
    val otherDaysAverage: Double?,
    // Navada, ob katere opravljenih dneh je razpolozenje najvisje (in visje od povprecja)
    val topHabitId: String?,
    val topHabitAverage: Double?
) {
    val hasCorrelation: Boolean get() = goodDaysAverage != null && otherDaysAverage != null

    companion object {
        val Empty = MoodStats(0, null, null, null, null, null)
    }
}

object MoodInsights {
    const val MIN_DAYS = 3
    const val GOOD_DAY_RATIO = 0.8

    fun compute(moods: List<MoodEntry>, aggregates: List<DailyAggregate>, completions: List<Completion>): MoodStats {
        if (moods.isEmpty()) return MoodStats.Empty
        val byDate = moods.associate { it.date to it.mood }
        val average = byDate.values.average()

        val good = ArrayList<Int>()
        val other = ArrayList<Int>()
        aggregates.filter { it.scheduled > 0 }.forEach { a ->
            val mood = byDate[a.date] ?: return@forEach
            if (a.done.toDouble() / a.scheduled >= GOOD_DAY_RATIO) good += mood else other += mood
        }
        val enough = good.size >= MIN_DAYS && other.size >= MIN_DAYS

        val top = completions
            .filter { it.status == CompletionStatus.DONE }
            .groupBy { it.habitId }
            .mapNotNull { (habitId, list) ->
                val values = list.mapNotNull { byDate[it.date] }
                if (values.size >= MIN_DAYS) habitId to values.average() else null
            }
            .filter { it.second > average }
            .maxWithOrNull(compareBy<Pair<String, Double>> { it.second }.thenBy { it.first })

        return MoodStats(
            entries = byDate.size,
            average = average,
            goodDaysAverage = if (enough) good.average() else null,
            otherDaysAverage = if (enough) other.average() else null,
            topHabitId = top?.first,
            topHabitAverage = top?.second
        )
    }
}
