package com.streak.app.domain.gamification

import com.streak.app.domain.model.DailyAggregate
import java.time.LocalDate

// Igrifikacija je v celoti izpeljana iz zapisov: XP, nivo in znacke se vedno izracunajo
// iz opravljenih navad, sej in dnevnih povzetkov. Odznacitev XP odvzame (ni "kmetovanja"),
// po obnovi varnostne kopije pa je stanje enako kot prej.

// Dnevni povzetek opravljenih navad (iz baze, ze zdruzen po dnevu)
data class DayActivity(
    val date: LocalDate,
    val done: Int,
    // Opravljeno isti koledarski dan pred 9:00 oz. pred 18:00 (brez naknadnih vpisov)
    val doneBefore9: Int,
    val doneBefore18: Int,
    val notes: Int
)

data class DaySessions(
    val date: LocalDate,
    val count: Int,
    val seconds: Long,
    // Vsota zaokrozenih 5-minutnih enot po seji (najvec MAX_SESSION_UNITS na sejo)
    val units: Int
)

data class GameInput(
    val activity: List<DayActivity>,
    val sessions: List<DaySessions>,
    val aggregates: List<DailyAggregate>,
    // Najdaljsa veriga katerekoli navade, preracunana v dneve (teden = 7, mesec = 30)
    val bestStreakDays: Int,
    val today: LocalDate,
    // Cisti dnevi navad za opuscanje (nimajo zapisov, zato niso v activity)
    val cleanDays: Int = 0
)

object XpRules {
    const val DONE = 10
    const val PERFECT_DAY = 20
    const val CHALLENGE = 30
    // 2 XP za vsakih 5 minut seje, najvec 1 ura na sejo
    const val PER_SESSION_UNIT = 2
    const val SESSION_UNIT_SECONDS = 300
    const val MAX_SESSION_UNITS = 12
}

data class LevelInfo(val level: Int, val xp: Int, val levelStartXp: Int, val nextLevelXp: Int) {
    val xpIntoLevel: Int get() = xp - levelStartXp
    val xpForLevel: Int get() = nextLevelXp - levelStartXp
    val progress: Float get() = if (xpForLevel <= 0) 0f else (xpIntoLevel.toFloat() / xpForLevel).coerceIn(0f, 1f)
}

object Levels {
    // Prag nivoja n: 50 * n * (n - 1) -> 1: 0, 2: 100, 3: 300, 4: 600, 5: 1000, 10: 4500
    fun threshold(level: Int): Int = 50 * level * (level - 1)

    fun of(xp: Int): LevelInfo {
        var level = 1
        while (threshold(level + 1) <= xp) level++
        return LevelInfo(level, xp, threshold(level), threshold(level + 1))
    }
}

// Dnevni izziv: izbran po datumu, izpolnljiv za vsakogar z vsaj eno navado
enum class Challenge {
    PERFECT_DAY,   // opravi vse danasnje navade
    EARLY_START,   // opravi navado pred 9:00
    REFLECT,       // dodaj opombo k opravljeni navadi
    DONE_BY_EVENING; // vse opravljeno do 18:00

    companion object {
        fun forDate(date: LocalDate): Challenge {
            val raw = raw(date.toEpochDay())
            // Isti izziv dva dni zapored bi bil dolgocasen
            return entries[if (raw == raw(date.toEpochDay() - 1)) (raw + 1) % entries.size else raw]
        }

        private fun raw(day: Long): Int {
            var x = day * -0x61c8864680b583ebL
            x = x xor (x ushr 29)
            return Math.floorMod(x, entries.size.toLong()).toInt()
        }
    }
}

data class ChallengeStatus(val challenge: Challenge, val done: Boolean)

enum class BadgeKind { STREAK, PERFECT, VOLUME, FOCUS, HABIT, LEVEL }

// Znacke s pragom; napredek se izracuna iz GameStats
enum class Badge(val kind: BadgeKind, val target: Int) {
    FIRST_STEP(BadgeKind.VOLUME, 1),
    STREAK_7(BadgeKind.STREAK, 7),
    STREAK_30(BadgeKind.STREAK, 30),
    STREAK_100(BadgeKind.STREAK, 100),
    STREAK_365(BadgeKind.STREAK, 365),
    PERFECT_DAY(BadgeKind.PERFECT, 1),
    PERFECT_WEEK(BadgeKind.PERFECT, 7),
    DONE_100(BadgeKind.VOLUME, 100),
    DONE_1000(BadgeKind.VOLUME, 1000),
    FIRST_SESSION(BadgeKind.FOCUS, 1),
    FOCUS_10H(BadgeKind.FOCUS, 600), // minute
    EARLY_BIRD(BadgeKind.HABIT, 10),
    REFLECTIVE(BadgeKind.HABIT, 10),
    CHALLENGER(BadgeKind.HABIT, 10),
    LEVEL_5(BadgeKind.LEVEL, 5),
    LEVEL_10(BadgeKind.LEVEL, 10);

    fun progressIn(stats: GameStats, level: Int): Int = when (this) {
        FIRST_STEP, DONE_100, DONE_1000 -> stats.totalDone
        STREAK_7, STREAK_30, STREAK_100, STREAK_365 -> stats.bestStreakDays
        PERFECT_DAY -> stats.perfectDays
        PERFECT_WEEK -> stats.longestPerfectRun
        FIRST_SESSION -> stats.sessions
        FOCUS_10H -> (stats.sessionSeconds / 60).toInt()
        EARLY_BIRD -> stats.earlyCompletions
        REFLECTIVE -> stats.notes
        CHALLENGER -> stats.challengesDone
        LEVEL_5, LEVEL_10 -> level
    }
}

data class BadgeProgress(val badge: Badge, val current: Int) {
    val earned: Boolean get() = current >= badge.target
    val progress: Float get() = (current.toFloat() / badge.target).coerceIn(0f, 1f)
}

data class GameStats(
    val totalDone: Int = 0,
    val perfectDays: Int = 0,
    val longestPerfectRun: Int = 0,
    val sessions: Int = 0,
    val sessionSeconds: Long = 0,
    val earlyCompletions: Int = 0,
    val notes: Int = 0,
    val challengesDone: Int = 0,
    val bestStreakDays: Int = 0
)

data class XpBreakdown(val habits: Int = 0, val perfectDays: Int = 0, val challenges: Int = 0, val sessions: Int = 0) {
    val total: Int get() = habits + perfectDays + challenges + sessions
}

data class GameState(
    val xp: XpBreakdown,
    val level: LevelInfo,
    val stats: GameStats,
    val badges: List<BadgeProgress>,
    val today: ChallengeStatus
) {
    val earned: Set<Badge> get() = badges.filter { it.earned }.map { it.badge }.toSet()

    companion object {
        fun empty(today: LocalDate) = Gamification.compute(GameInput(emptyList(), emptyList(), emptyList(), 0, today))
    }
}

object Gamification {

    fun compute(input: GameInput): GameState {
        val activity = input.activity.filter { !it.date.isAfter(input.today) }.associateBy { it.date }
        val aggregates = input.aggregates.filter { !it.date.isAfter(input.today) }.associateBy { it.date }
        val sessions = input.sessions.filter { !it.date.isAfter(input.today) }

        val perfectDates = aggregates.values.filter { isPerfect(it) }.map { it.date }.toSet()
        val challengeDays = (activity.keys + aggregates.keys).count { challengeDone(Challenge.forDate(it), activity[it], aggregates[it]) }

        val stats = GameStats(
            totalDone = activity.values.sumOf { it.done } + input.cleanDays,
            perfectDays = perfectDates.size,
            longestPerfectRun = longestPerfectRun(aggregates.values.sortedBy { it.date }),
            sessions = sessions.sumOf { it.count },
            sessionSeconds = sessions.sumOf { it.seconds },
            earlyCompletions = activity.values.sumOf { it.doneBefore9 },
            notes = activity.values.sumOf { it.notes },
            challengesDone = challengeDays,
            bestStreakDays = input.bestStreakDays
        )
        val xp = XpBreakdown(
            habits = stats.totalDone * XpRules.DONE,
            perfectDays = stats.perfectDays * XpRules.PERFECT_DAY,
            challenges = stats.challengesDone * XpRules.CHALLENGE,
            sessions = sessions.sumOf { it.units } * XpRules.PER_SESSION_UNIT
        )
        val level = Levels.of(xp.total)
        val todayChallenge = Challenge.forDate(input.today)
        return GameState(
            xp = xp,
            level = level,
            stats = stats,
            badges = Badge.entries.map { BadgeProgress(it, it.progressIn(stats, level.level)) },
            today = ChallengeStatus(todayChallenge, challengeDone(todayChallenge, activity[input.today], aggregates[input.today]))
        )
    }

    fun challengeDone(challenge: Challenge, activity: DayActivity?, aggregate: DailyAggregate?): Boolean = when (challenge) {
        Challenge.PERFECT_DAY -> aggregate != null && isPerfect(aggregate)
        Challenge.EARLY_START -> (activity?.doneBefore9 ?: 0) > 0
        Challenge.REFLECT -> (activity?.notes ?: 0) > 0
        // Vsi vpisi dneva pred 18:00 (cisti dnevi opuscanja nimajo vpisa in ne stejejo)
        Challenge.DONE_BY_EVENING -> aggregate != null && isPerfect(aggregate) && (activity?.doneBefore18 ?: 0) >= (activity?.done ?: 0)
    }

    private fun isPerfect(a: DailyAggregate) = a.scheduled > 0 && a.done >= a.scheduled

    // Zaporedni popolni dnevi; dnevi brez nacrtovanih navad (tudi manjkajoci povzetki,
    // ki se ne shranjujejo) verige ne prekinejo
    private fun longestPerfectRun(days: List<DailyAggregate>): Int {
        var best = 0
        var run = 0
        for (d in days) {
            when {
                d.scheduled == 0 -> Unit
                isPerfect(d) -> { run++; best = maxOf(best, run) }
                else -> run = 0
            }
        }
        return best
    }
}
