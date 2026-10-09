package com.streak.app.domain.usecase

import com.streak.app.domain.repository.HabitRepository
import com.streak.app.domain.repository.TaskRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

// Izvoz vseh navad in nalog v JSON niz
class ExportDataUseCase @Inject constructor(
    private val habitRepository: HabitRepository,
    private val taskRepository: TaskRepository
) {
    private val json = Json { prettyPrint = true }

    suspend operator fun invoke(): String {
        val habits = habitRepository.getHabitsOnce().map {
            ExportHabit(it.id, it.title, it.icon, it.colorHex, it.frequency.name, it.targetPerWeek)
        }
        val tasks = taskRepository.observeTasks().first().map {
            ExportTask(it.id, it.title, it.priority.name, it.isDone)
        }
        return json.encodeToString(ExportBundle(habits, tasks))
    }
}

@kotlinx.serialization.Serializable
data class ExportBundle(val habits: List<ExportHabit>, val tasks: List<ExportTask>)

@kotlinx.serialization.Serializable
data class ExportHabit(
    val id: String, val title: String, val icon: String,
    val colorHex: String, val frequency: String, val targetPerWeek: Int
)

@kotlinx.serialization.Serializable
data class ExportTask(val id: String, val title: String, val priority: String, val isDone: Boolean)
