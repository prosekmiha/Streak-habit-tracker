package com.streak.app.domain.session

// Trajno shranjena tekoca seja (prezivi smrt procesa in ponovni zagon naprave)
interface ActiveSessionStore {
    suspend fun load(): ActiveSession?
    suspend fun lastAmbient(): AmbientSound
    suspend fun save(session: ActiveSession?)
}
