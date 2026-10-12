package com.streak.app.domain.backup

import com.streak.app.domain.model.Priority
import com.streak.app.domain.model.Task
import com.streak.app.domain.model.TaskReminder
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

class BackupTaskRemindersTest {

    private val codec = BackupCodec(BackupCrypto(iterations = 1_000))
    private val now = Instant.parse("2026-10-11T08:00:00Z")

    @Test
    fun `task reminders survive a round trip`() {
        val task = Task(
            "t1", "Nakup", LocalDate.of(2026, 10, 2), 600, Priority.URGENT, false, true, "FREQ=DAILY", 1L,
            reminders = listOf(
                TaskReminder.BeforeDue(60),
                TaskReminder.DayAt(1, LocalTime.of(20, 0)),
                TaskReminder.Repeat(30, LocalTime.of(9, 0), LocalTime.of(18, 0))
            )
        )
        val text = codec.encode(BackupContents(tasks = listOf(task)), now, "1.0")
        val result = codec.parse(text) as ParseResult.Ok
        assertEquals(listOf(task), result.contents.tasks)
    }

    @Test
    fun `older backups and unknown reminder kinds are tolerated`() {
        val text = """
            {"format":"streak-backup","schemaVersion":2,"createdAt":"2026-10-11T08:00:00Z",
             "tasks":[{"id":"a","title":"Stara","createdAt":1},
                      {"id":"b","title":"Nova","createdAt":2,"reminders":[{"kind":"FUTURE"},{"kind":"BEFORE","minutes":15}]}]}
        """.trimIndent()
        val tasks = (codec.parse(text) as ParseResult.Ok).contents.tasks
        assertEquals(emptyList<TaskReminder>(), tasks.first { it.id == "a" }.reminders)
        assertEquals(listOf(TaskReminder.BeforeDue(15)), tasks.first { it.id == "b" }.reminders)
    }
}
