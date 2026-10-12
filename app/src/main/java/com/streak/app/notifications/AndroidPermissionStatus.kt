package com.streak.app.notifications

import android.content.Context
import android.os.PowerManager
import com.streak.app.domain.notify.PermissionStatus
import com.streak.app.domain.notify.ReminderScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AndroidPermissionStatus @Inject constructor(
    @ApplicationContext private val context: Context,
    private val helper: NotificationHelper,
    private val scheduler: ReminderScheduler
) : PermissionStatus {
    override fun notificationsEnabled(): Boolean = helper.enabled()

    override fun exactAlarmsAllowed(): Boolean = scheduler.canScheduleExact()

    override fun batteryUnrestricted(): Boolean =
        context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)
}
