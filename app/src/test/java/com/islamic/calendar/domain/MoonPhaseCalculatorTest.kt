package com.islamic.calendar.domain

import java.time.Instant
import org.junit.Assert.assertTrue
import org.junit.Test

class MoonPhaseCalculatorTest {

    @Test
    fun synodicPhaseInRange() {
        val instants = listOf(
            Instant.parse("2020-06-21T12:00:00Z"),
            Instant.parse("2024-01-11T12:00:00Z"),
            Instant.parse("2026-05-10T12:00:00Z"),
        )
        instants.forEach { instant ->
            val info = MoonPhaseCalculator.forInstant(instant)
            assertTrue(info.phase >= 0.0 && info.phase < 1.0)
            assertTrue(info.illuminatedFraction in 0.0..1.0)
        }
    }
}
