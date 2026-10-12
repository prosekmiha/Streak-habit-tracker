package com.streak.app.di

import android.util.Log
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

// Scope, ki zivi toliko casa kot proces (za ozadna opravila, ki ne smejo
// biti preklicana ob zaprtju zaslona)
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object CoroutinesModule {

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope {
        // Napaka v ozadnem opravilu (npr. zaprta baza po neuspeli migraciji) ne sme sesuti procesa
        val handler = CoroutineExceptionHandler { _, e -> Log.e("ApplicationScope", "Neulovljena napaka", e) }
        return CoroutineScope(SupervisorJob() + Dispatchers.Default + handler)
    }
}
