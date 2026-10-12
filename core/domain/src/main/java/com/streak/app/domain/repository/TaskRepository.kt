package com.streak.app.domain.repository

import com.streak.app.domain.model.Task
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface TaskRepository {
    fun observeTasks(): Flow<List<Task>>
    fun observeTasksForDate(date: LocalDate): Flow<List<Task>>
    suspend fun getTask(id: String): Task?
    suspend fun getTasks(): List<Task>
    suspend fun upsertTask(task: Task)
    suspend fun deleteTask(id: String)
    suspend fun setDone(id: String, done: Boolean)
}
