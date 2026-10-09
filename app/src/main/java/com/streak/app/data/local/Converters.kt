package com.streak.app.data.local

import androidx.room.TypeConverter
import com.streak.app.domain.model.Frequency
import com.streak.app.domain.model.LogStatus
import com.streak.app.domain.model.Priority

// Pretvorbe enumov v/iz String za Room
class Converters {
    @TypeConverter fun freqToString(v: Frequency): String = v.name
    @TypeConverter fun stringToFreq(v: String): Frequency = Frequency.valueOf(v)

    @TypeConverter fun statusToString(v: LogStatus): String = v.name
    @TypeConverter fun stringToStatus(v: String): LogStatus = LogStatus.valueOf(v)

    @TypeConverter fun priorityToString(v: Priority): String = v.name
    @TypeConverter fun stringToPriority(v: String): Priority = Priority.valueOf(v)
}
