package com.islamic.calendar.domain

import com.islamic.calendar.R
import java.time.LocalDate
import java.time.ZoneId
import java.time.chrono.HijrahDate
import java.time.LocalTime
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit

data class HijriDisplay(
    val dayOfMonth: Int,
    val monthValue: Int,
    val year: Int,
    val gregorian: LocalDate,
)

fun hijriMonthStringRes(monthValue: Int): Int {
    require(monthValue in 1..12) { "monthValue must be 1..12" }
    return when (monthValue) {
        1 -> R.string.hijri_month_1
        2 -> R.string.hijri_month_2
        3 -> R.string.hijri_month_3
        4 -> R.string.hijri_month_4
        5 -> R.string.hijri_month_5
        6 -> R.string.hijri_month_6
        7 -> R.string.hijri_month_7
        8 -> R.string.hijri_month_8
        9 -> R.string.hijri_month_9
        10 -> R.string.hijri_month_10
        11 -> R.string.hijri_month_11
        else -> R.string.hijri_month_12
    }
}

object HijriCalendar {

    fun todayDisplay(zoneId: ZoneId, offsetDays: Int): HijriDisplay {
        val gregorian = LocalDate.now(zoneId)
        val base = HijrahDate.from(gregorian)
        val adjusted = applyOffset(base, offsetDays)
        return HijriDisplay(
            dayOfMonth = adjusted.get(ChronoField.DAY_OF_MONTH),
            monthValue = adjusted.get(ChronoField.MONTH_OF_YEAR),
            year = adjusted.get(ChronoField.YEAR),
            gregorian = gregorian,
        )
    }

    /** Astronomical moon phase for the civil “today” in [zoneId] (unaffected by Hijri offset). */
    fun moonInstantAtLocalNoon(zoneId: ZoneId): java.time.Instant =
        LocalDate.now(zoneId).atTime(LocalTime.NOON).atZone(zoneId).toInstant()

    private fun applyOffset(base: HijrahDate, offsetDays: Int): HijrahDate {
        if (offsetDays == 0) return base
        return try {
            base.plus(offsetDays.toLong(), ChronoUnit.DAYS)
        } catch (_: Exception) {
            base
        }
    }
}
