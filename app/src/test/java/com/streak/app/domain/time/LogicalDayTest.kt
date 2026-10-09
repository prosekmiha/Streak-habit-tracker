package com.streak.app.domain.time

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class LogicalDayTest {

    private val ljubljana = ZoneId.of("Europe/Ljubljana")
    private val newYork = ZoneId.of("America/New_York")
    private fun at(dateTime: String, zone: ZoneId = ljubljana): Instant =
        LocalDateTime.parse(dateTime).atZone(zone).toInstant()

    @Test fun `midnight setting is the calendar day`() {
        assertEquals(LocalDate.of(2026, 10, 7), LogicalDay.of(at("2026-10-07T00:00"), ljubljana, LocalTime.MIDNIGHT))
        assertEquals(LocalDate.of(2026, 10, 6), LogicalDay.of(at("2026-10-06T23:59:59"), ljubljana, LocalTime.MIDNIGHT))
    }

    @Test fun `late night belongs to previous day when day ends at 4am`() {
        val endsAt = LocalTime.of(4, 0)
        assertEquals(LocalDate.of(2026, 10, 6), LogicalDay.of(at("2026-10-07T01:30"), ljubljana, endsAt))
        assertEquals(LocalDate.of(2026, 10, 6), LogicalDay.of(at("2026-10-07T03:59:59"), ljubljana, endsAt))
        assertEquals(LocalDate.of(2026, 10, 7), LogicalDay.of(at("2026-10-07T04:00"), ljubljana, endsAt))
    }

    @Test fun `next boundary respects day ends at`() {
        val endsAt = LocalTime.of(4, 0)
        assertEquals(at("2026-10-08T04:00"), LogicalDay.nextBoundary(at("2026-10-07T10:00"), ljubljana, endsAt))
        assertEquals(at("2026-10-07T04:00"), LogicalDay.nextBoundary(at("2026-10-07T02:00"), ljubljana, endsAt))
    }

    @Test fun `spring forward day is 23 hours long`() {
        // 29.3.2026: ura skoci iz 02:00 na 03:00
        val start = LogicalDay.startOf(LocalDate.of(2026, 3, 29), ljubljana, LocalTime.MIDNIGHT)
        val next = LogicalDay.startOf(LocalDate.of(2026, 3, 30), ljubljana, LocalTime.MIDNIGHT)
        assertEquals(Duration.ofHours(23), Duration.between(start, next))
    }

    @Test fun `fall back day is 25 hours long and the repeated hour stays on the same day`() {
        // 25.10.2026: ura se iz 03:00 vrne na 02:00
        val start = LogicalDay.startOf(LocalDate.of(2026, 10, 25), ljubljana, LocalTime.MIDNIGHT)
        val next = LogicalDay.startOf(LocalDate.of(2026, 10, 26), ljubljana, LocalTime.MIDNIGHT)
        assertEquals(Duration.ofHours(25), Duration.between(start, next))
        val firstTwoThirty = Instant.parse("2026-10-25T00:30:00Z") // 02:30 CEST
        val secondTwoThirty = Instant.parse("2026-10-25T01:30:00Z") // 02:30 CET
        assertEquals(LocalDate.of(2026, 10, 25), LogicalDay.of(firstTwoThirty, ljubljana, LocalTime.MIDNIGHT))
        assertEquals(LocalDate.of(2026, 10, 25), LogicalDay.of(secondTwoThirty, ljubljana, LocalTime.MIDNIGHT))
    }

    @Test fun `boundary inside the DST gap moves forward`() {
        // "dan se konca ob" 02:30 ne obstaja 29.3.2026 -> meja je 03:30 CEST
        val endsAt = LocalTime.of(2, 30)
        val boundary = LogicalDay.startOf(LocalDate.of(2026, 3, 29), ljubljana, endsAt)
        assertEquals(at("2026-03-29T03:30"), boundary)
        assertEquals(LocalDate.of(2026, 3, 28), LogicalDay.of(at("2026-03-29T03:00"), ljubljana, endsAt))
        assertEquals(LocalDate.of(2026, 3, 29), LogicalDay.of(at("2026-03-29T03:30"), ljubljana, endsAt))
    }

    @Test fun `same instant maps to the local day of the current zone when traveling`() {
        val instant = Instant.parse("2026-10-07T03:00:00Z") // 05:00 v Ljubljani, 23:00 prejsnji dan v New Yorku
        assertEquals(LocalDate.of(2026, 10, 7), LogicalDay.of(instant, ljubljana, LocalTime.MIDNIGHT))
        assertEquals(LocalDate.of(2026, 10, 6), LogicalDay.of(instant, newYork, LocalTime.MIDNIGHT))
    }
}
