package com.streak.app.data.remote

import com.streak.app.domain.model.Habit
import com.streak.app.domain.model.Task
import javax.inject.Inject

// Prazna implementacija. Za priklop Supabase dodaj odvisnost supabase-kt
// in implementiraj klice na Postgres/REST. Repository ze klice ta vmesnik,
// zato UI sloja ne bo treba spreminjati.
class SupabaseRemoteDataSource @Inject constructor() : RemoteDataSource {

    // TODO(supabase): inicializiraj SupabaseClient (URL + anon key)

    override suspend fun pushHabits(habits: List<Habit>) {
        // TODO(supabase): upsert v tabelo "habits"
    }

    override suspend fun pushTasks(tasks: List<Task>) {
        // TODO(supabase): upsert v tabelo "tasks"
    }

    override suspend fun pullHabits(): List<Habit> {
        // TODO(supabase): select iz tabele "habits"
        return emptyList()
    }

    override suspend fun pullTasks(): List<Task> {
        // TODO(supabase): select iz tabele "tasks"
        return emptyList()
    }

    override suspend fun isAvailable(): Boolean = false
}
