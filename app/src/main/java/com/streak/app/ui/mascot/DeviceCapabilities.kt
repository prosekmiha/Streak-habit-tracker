package com.streak.app.ui.mascot

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.PowerManager

// Ali naprava zmore 3D maskoto. Ob nizki zmogljivosti, varcevanju z baterijo ali
// pregrevanju se uporabijo vnaprej zrenderirane 2D poze istega modela.
object DeviceCapabilities {

    fun supports3d(context: Context): Boolean {
        val am = context.getSystemService(ActivityManager::class.java)
        val glEs3 = am.deviceConfigurationInfo.reqGlEsVersion >= 0x30000
        if (!glEs3 || am.isLowRamDevice) return false
        val pm = context.getSystemService(PowerManager::class.java)
        if (pm.isPowerSaveMode) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && pm.currentThermalStatus >= PowerManager.THERMAL_STATUS_SEVERE) return false
        return true
    }

    // Pri zmernem pregrevanju omeji na 30 fps
    fun reducedFrameRate(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        val pm = context.getSystemService(PowerManager::class.java)
        return pm.currentThermalStatus >= PowerManager.THERMAL_STATUS_MODERATE
    }
}
