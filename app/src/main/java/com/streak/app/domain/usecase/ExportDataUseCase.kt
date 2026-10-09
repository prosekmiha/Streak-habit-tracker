package com.streak.app.domain.usecase

import com.streak.app.domain.repository.HabitRepository
import com.streak.app.domain.repository.TaskRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate
import javax.inject.Inject

// Izvoz navad, zapisov in nalog v JSON niz.
// Zacasna oblika; polni izvoz/uvoz s shemo in preverjanjem pride v M5.
class ExportDataUseCase @Inject constructor(
    private val habitRepository: HabitRepository,
    private val taskRepository: TaskRepository
) {
    private val json = Json { prettyPrint = true }

    suspend operator fun invoke(): String {
        val habits = habitRepository.getHabits(includeArchived = true).map {
            ExportHabit(it.id, it.title, it.icon, it.colorHex, it.schedule.type.name, it.schedule.timesPerPeriod)
        }
        val completions = habitRepository.getCompletionsBetween(LocalDate.MIN, LocalDate.MAX).map {
            ExportCompletion(it.habitId, it.date.toString(), it.status.name, it.value)
        }
        val tasks = taskRepository.observeTasks().first().map {
            ExportTask(it.id, it.title, it.priority.name, it.isDone)
        }
        return json.encodeToString(ExportBundle(SCHEMA_VERSION, habits, completions, tasks))
    }

    private companion object {
        const val SCHEMA_VERSION = 1
    }
}

@Serializable
data class ExportBundle(
    val schemaVersion: Int,
    val habits: List<ExportHabit>,
    val completions: List<ExportCompletion>,
    val tasks: List<ExportTask>
)

@Serializable
data class ExportHabit(
    val id: String, val title: String, val icon: String,
    val colorHex: String, val schedule: String, val timesPerPeriod: Int
)

@Serializable
data class ExportCompletion(val habitId: String, val date: String, val status: String, val value: Double)

@Serializable
data class ExportTask(val id: String, val title: String, val priority: String, val isDone: Boolean)
