package com.streak.app.data.repository

import com.streak.app.data.local.dao.TaskDao
import com.streak.app.data.mapper.toDomain
import com.streak.app.data.mapper.toEntity
import com.streak.app.domain.model.Task
import com.streak.app.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao
) : TaskRepository {

    override fun observeTasks(): Flow<List<Task>> =
        taskDao.observeAll().map { list -> list.map { it.toDomain() } }

    // Tudi ponavljajoce naloge, ki imajo na ta dan termin (ne le tiste z rokom na ta dan)
    override fun observeTasksForDate(date: LocalDate): Flow<List<Task>> =
        observeTasks().map { list ->
            list.filter { it.occursOn(date) }
                .sortedWith(compareBy<Task, Int?>(nullsFirst()) { it.dueMinuteOfDay }.thenByDescending { it.priority })
        }

    override suspend fun getTask(id: String): Task? = taskDao.get(id)?.toDomain()

    override suspend fun getTasks(): List<Task> = taskDao.getAll().map { it.toDomain() }

    override suspend fun upsertTask(task: Task) = taskDao.upsert(task.toEntity())

    override suspend fun deleteTask(id: String) = taskDao.delete(id)

    override suspend fun setDone(id: String, done: Boolean) = taskDao.setDone(id, done)
}
