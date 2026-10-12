package com.streak.app.domain.backup

import com.streak.app.domain.model.AppSettings
import com.streak.app.domain.model.CompletionStatus
import com.streak.app.domain.model.GoalType
import com.streak.app.domain.model.HabitSession
import com.streak.app.domain.model.Priority
import com.streak.app.domain.model.Reminder
import com.streak.app.domain.model.Task
import com.streak.app.domain.model.Vacation
import com.streak.app.testing.completion
import com.streak.app.testing.testHabit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

class BackupCodecTest {

    // Malo iteracij, da so testi hitri (format je enak)
    private val codec = BackupCodec(BackupCrypto(iterations = 1_000))
    private val now = Instant.parse("2026-10-11T08:00:00Z")
    private val d1 = LocalDate.of(2026, 10, 1)
    private val d2 = LocalDate.of(2026, 10, 2)

    private val sample = BackupContents(
        habits = listOf(
            testHabit("a").copy(title = "Branje", reminders = listOf(Reminder(time = LocalTime.of(20, 0)))),
            testHabit("b", goalType = GoalType.QUANTITY, target = 8.0).copy(title = "Voda", unit = "kozarcev")
        ),
        completions = listOf(
            completion(d1, habitId = "a"),
            completion(d2, CompletionStatus.PARTIAL, habitId = "b", value = 3.0).copy(note = "jutro, \"hitro\"")
        ),
        sessions = listOf(HabitSession("s1", "a", d1, now, now.plusSeconds(600), 600)),
        vacations = listOf(Vacation(startDate = d1, endDate = d2)),
        tasks = listOf(Task("t1", "Nakup", d2, 600, Priority.URGENT, false, false, null, 1L)),
        settings = BackupCodec.settingsOf(AppSettings(userName = "Miha"))
    )

    @Test
    fun `plain round trip keeps all data`() {
        val text = codec.encode(sample, now, "1.0")
        assertFalse(codec.isEncrypted(text))
        val result = codec.parse(text) as ParseResult.Ok
        assertEquals(sample.habits, result.contents.habits.map { it.copy(reminders = it.reminders.map { r -> r.copy(id = 0) }) })
        assertEquals(sample.completions, result.contents.completions)
        assertEquals(sample.sessions, result.contents.sessions)
        assertEquals(sample.vacations, result.contents.vacations)
        assertEquals(sample.tasks, result.contents.tasks)
        assertEquals("Miha", result.contents.settings?.userName)
        assertEquals(2, result.summary.habits)
        assertEquals(d1, result.summary.firstDate)
        assertEquals(d2, result.summary.lastDate)
        assertEquals(now, result.summary.createdAt)
    }

    @Test
    fun `encrypted round trip needs the right password`() {
        val text = codec.encode(sample, now, "1.0", "skrivnost".toCharArray())
        assertTrue(codec.isEncrypted(text))
        // Vsebina ni berljiva brez gesla
        assertFalse(text.contains("Branje"))
        assertEquals(ParseResult.NeedsPassword, codec.parse(text))
        assertEquals(ParseResult.WrongPassword, codec.parse(text, "napacno".toCharArray()))
        val ok = codec.parse(text, "skrivnost".toCharArray()) as ParseResult.Ok
        assertEquals(sample.completions, ok.contents.completions)
    }

    @Test
    fun `tampered ciphertext is rejected`() {
        val text = codec.encode(sample, now, "1.0", "geslo".toCharArray())
        val tampered = text.replace("\"data\":\"A", "\"data\":\"B").let { if (it == text) text.replace("\"data\":\"", "\"data\":\"AAAA") else it }
        val result = codec.parse(tampered, "geslo".toCharArray())
        assertTrue(result == ParseResult.WrongPassword || result == ParseResult.Corrupt)
    }

    @Test
    fun `unknown files are recognised`() {
        assertEquals(ParseResult.NotABackup, codec.parse("hello"))
        assertEquals(ParseResult.NotABackup, codec.parse("{\"foo\": 1}"))
        assertEquals(ParseResult.NewerVersion, codec.parse("{\"format\":\"streak-backup\",\"schemaVersion\":99,\"createdAt\":\"x\"}"))
        val badEnum = codec.encode(sample, now, "1.0").replace("\"PARTIAL\"", "\"MAYBE\"")
        assertEquals(ParseResult.Corrupt, codec.parse(badEnum))
    }

    @Test
    fun `unknown fields from newer apps are ignored`() {
        val text = codec.encode(sample, now, "1.0").replaceFirst("{", "{\"futureField\":[1,2,3],")
        assertTrue(codec.parse(text) is ParseResult.Ok)
    }

    @Test
    fun `records of missing habits are skipped and counted`() {
        val orphan = sample.copy(completions = sample.completions + completion(d1, habitId = "ghost"))
        val ok = codec.parse(codec.encode(orphan, now, "1.0")) as ParseResult.Ok
        assertEquals(2, ok.contents.completions.size)
        assertEquals(1, ok.summary.skipped)
    }

    @Test
    fun `merge keeps local data and adds missing, newer log wins`() {
        val local = BackupContents(
            habits = listOf(testHabit("a").copy(title = "Lokalno")),
            completions = listOf(completion(d1, habitId = "a").copy(loggedAt = now))
        )
        val incoming = sample.copy(
            completions = listOf(
                completion(d1, CompletionStatus.SKIPPED, habitId = "a").copy(loggedAt = now.minusSeconds(60)),
                completion(d2, habitId = "a").copy(loggedAt = now)
            )
        )
        val merged = BackupMerge.merge(local, incoming)
        assertEquals("Lokalno", merged.habits.first { it.id == "a" }.title)
        assertEquals(setOf("a", "b"), merged.habits.map { it.id }.toSet())
        assertEquals(CompletionStatus.DONE, merged.completions.first { it.date == d1 }.status)
        assertEquals(2, merged.completions.size)
        assertEquals(1, merged.tasks.size)
    }

    @Test
    fun `settings round trip keeps notification preferences`() {
        val s = AppSettings(userName = "Ana", quietHoursStart = LocalTime.of(22, 0), quietHoursEnd = LocalTime.of(7, 0), nudgeTime = LocalTime.of(21, 30))
        val restored = BackupCodec.applySettings(BackupCodec.settingsOf(s), AppSettings())
        assertEquals("Ana", restored.userName)
        assertEquals(LocalTime.of(22, 0), restored.quietHoursStart)
        assertEquals(LocalTime.of(21, 30), restored.nudgeTime)
        assertTrue(restored.onboardingDone)
    }

    @Test
    fun `csv escapes separators, quotes and formulas`() {
        val habits = listOf(testHabit("a").copy(title = "=SUM(A1)"), testHabit("b").copy(title = "Voda, čaj", unit = "kozarcev"))
        val csv = CompletionsCsv.build(
            habits,
            listOf(completion(d2, habitId = "a").copy(note = "rekel je \"da\""), completion(d1, habitId = "b", value = 2.5))
        )
        val lines = csv.trimEnd().split("\r\n")
        assertEquals("date,habit_id,habit,status,value,unit,note", lines[0])
        assertEquals("2026-10-01,b,\"Voda, čaj\",DONE,2.5,kozarcev,", lines[1])
        assertEquals("2026-10-02,a,'=SUM(A1),DONE,1,,\"rekel je \"\"da\"\"\"", lines[2])
    }
}
