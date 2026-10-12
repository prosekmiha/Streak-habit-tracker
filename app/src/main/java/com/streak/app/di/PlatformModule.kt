package com.streak.app.di

import com.streak.app.backup.AndroidBackupFiles
import com.streak.app.backup.BackupFiles
import com.streak.app.backup.BackupManager
import com.streak.app.data.local.DatabaseGuard
import com.streak.app.domain.backup.BackupService
import com.streak.app.domain.health.DatabaseHealthMonitor
import com.streak.app.domain.notify.PermissionStatus
import com.streak.app.domain.notify.ReminderScheduler
import com.streak.app.domain.notify.TaskReminderScheduler
import com.streak.app.domain.session.SessionController
import com.streak.app.notifications.AlarmScheduler
import com.streak.app.notifications.AndroidPermissionStatus
import com.streak.app.notifications.TaskAlarmScheduler
import com.streak.app.session.AndroidSessionController
import com.streak.app.ui.onboarding.HabitTemplateCatalog
import com.streak.app.ui.onboarding.ResourceHabitTemplateCatalog
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Androidove storitve (alarmi, seje, datoteke, dovoljenja) za domenskimi vmesniki
@Module
@InstallIn(SingletonComponent::class)
abstract class PlatformModule {

    @Binds @Singleton
    abstract fun bindReminderScheduler(impl: AlarmScheduler): ReminderScheduler

    @Binds @Singleton
    abstract fun bindTaskReminderScheduler(impl: TaskAlarmScheduler): TaskReminderScheduler

    @Binds @Singleton
    abstract fun bindSessionController(impl: AndroidSessionController): SessionController

    @Binds @Singleton
    abstract fun bindBackupService(impl: BackupManager): BackupService

    @Binds @Singleton
    abstract fun bindDatabaseHealthMonitor(impl: DatabaseGuard): DatabaseHealthMonitor

    @Binds
    abstract fun bindBackupFiles(impl: AndroidBackupFiles): BackupFiles

    @Binds
    abstract fun bindPermissionStatus(impl: AndroidPermissionStatus): PermissionStatus

    @Binds
    abstract fun bindHabitTemplateCatalog(impl: ResourceHabitTemplateCatalog): HabitTemplateCatalog
}
