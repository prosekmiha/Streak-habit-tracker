package com.streak.app.domain.gamification

enum class InboxType { BADGE, LEVEL, CHALLENGE }

// Dogodek v nabiralniku (zvonec na domacem zaslonu)
data class InboxEvent(val type: InboxType, val key: String, val atMillis: Long)

data class GameMemory(
    // Prvi izracun po namestitvi/obnovi le zapomni stanje, brez praznovanj
    val initialized: Boolean,
    val seenBadges: Set<String>,
    val lastLevel: Int,
    val lastChallengeDay: Long?,
    val events: List<InboxEvent>,
    val readUntil: Long
) {
    val unread: Int get() = events.count { it.atMillis > readUntil }
}
