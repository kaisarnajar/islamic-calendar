package com.islamic.calendar

import android.app.Application
import com.islamic.calendar.data.LocationRepository
import com.islamic.calendar.data.UserOffsetRepository

class IslamicCalendarApp : Application() {

    val userOffsetRepository by lazy { UserOffsetRepository(this) }
    val locationRepository by lazy { LocationRepository(this) }
}
