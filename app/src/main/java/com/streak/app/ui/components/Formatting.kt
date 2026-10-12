package com.streak.app.ui.components

// 8.0 -> "8", 2.5 -> "2,5" (po jeziku naprave)
fun formatAmount(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else "%.1f".format(value)

// Stoparica seje: 05:07 ali 1:02:03
fun formatClock(seconds: Long): String =
    if (seconds >= 3600) "%d:%02d:%02d".format(seconds / 3600, seconds / 60 % 60, seconds % 60)
    else "%02d:%02d".format(seconds / 60, seconds % 60)
