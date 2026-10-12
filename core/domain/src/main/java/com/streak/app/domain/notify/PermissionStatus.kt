package com.streak.app.domain.notify

// Stanje sistemskih dovoljenj, od katerih so odvisni opomniki
interface PermissionStatus {
    fun notificationsEnabled(): Boolean
    fun exactAlarmsAllowed(): Boolean
    fun batteryUnrestricted(): Boolean
}
