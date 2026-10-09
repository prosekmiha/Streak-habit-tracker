package com.streak.app.data.remote

import com.streak.app.domain.model.Habit
import com.streak.app.domain.model.Task

// Vmesnik za oblacno sinhronizacijo. Lokalna baza je vedno vir resnice;
// ta sloj le potiska/vlece spremembe v oblak, ce je sync vklopljen.
interface RemoteDataSource {
    suspend fun pushHabits(habits: List<Habit>)
    suspend fun pushTasks(tasks: List<Task>)
    suspend fun pullHabits(): List<Habit>
    suspend fun pullTasks(): List<Task>
    suspend fun isAvailable(): Boolean
}
