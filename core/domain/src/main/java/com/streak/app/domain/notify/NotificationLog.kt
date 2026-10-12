package com.streak.app.domain.notify

import java.time.LocalDate

// Stevec poslanih obvestil za logicni dan (dnevna omejitev)
interface NotificationLog {
    // Vrne true in poveca stevec, ce omejitev se ni dosezena (cap 0 = brez omejitve)
    suspend fun tryConsume(today: LocalDate, cap: Int): Boolean
}
