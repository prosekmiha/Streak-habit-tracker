package com.streak.app.domain.time

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

// Pretvorba trenutka v "logicni" dan glede na nastavitev "dan se konca ob".
// Primer: dayEndsAt = 04:00 -> vpis ob 01:30 v sredo pripada torku.
// dayEndsAt = 00:00 pomeni obicajen koledarski dan.
object LogicalDay {

    // Definirano prek startOf, da se meja ujema tudi, ko dayEndsAt pade v DST "luknjo"
    fun of(instant: Instant, zone: ZoneId, dayEndsAt: LocalTime): LocalDate {
        val calendarDate = instant.atZone(zone).toLocalDate()
        return if (instant.isBefore(startOf(calendarDate, zone, dayEndsAt))) calendarDate.minusDays(1)
        else calendarDate
    }

    // Zacetek logicnega dne kot trenutek. ZonedDateTime.of pri DST "luknji"
    // (ura ne obstaja) premakne cas naprej, pri prekrivanju vzame prvo pojavitev.
    fun startOf(date: LocalDate, zone: ZoneId, dayEndsAt: LocalTime): Instant =
        ZonedDateTime.of(date, dayEndsAt, zone).toInstant()

    // Naslednja meja dneva strogo za podanim trenutkom
    fun nextBoundary(instant: Instant, zone: ZoneId, dayEndsAt: LocalTime): Instant =
        startOf(of(instant, zone, dayEndsAt).plusDays(1), zone, dayEndsAt)
}
