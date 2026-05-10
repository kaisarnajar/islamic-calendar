package com.islamic.calendar.domain

import java.time.LocalDate
import java.time.ZoneOffset
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class HijriCalendarTest {

    @Test
    fun userOffsetMatchesHijrahPlusDays() {
        val zone = ZoneOffset.UTC
        val today = LocalDate.now(zone)
        val offset = 2
        val display = HijriCalendar.todayDisplay(zone, offset)
        val expected = HijrahDate.from(today).plus(offset.toLong(), ChronoUnit.DAYS)
        assertEquals(expected.get(ChronoField.YEAR), display.year)
        assertEquals(expected.get(ChronoField.MONTH_OF_YEAR), display.monthValue)
        assertEquals(expected.get(ChronoField.DAY_OF_MONTH), display.dayOfMonth)
    }
}
