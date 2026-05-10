package com.islamic.calendar.domain

import com.islamic.calendar.R
import java.time.Instant
import java.time.ZoneOffset
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.PI

enum class MoonPhaseLabel {
    NewMoon,
    WaxingCrescent,
    FirstQuarter,
    WaxingGibbous,
    FullMoon,
    WaningGibbous,
    LastQuarter,
    WaningCrescent,
}

fun MoonPhaseLabel.stringRes(): Int = when (this) {
    MoonPhaseLabel.NewMoon -> R.string.moon_new
    MoonPhaseLabel.WaxingCrescent -> R.string.moon_waxing_crescent
    MoonPhaseLabel.FirstQuarter -> R.string.moon_first_quarter
    MoonPhaseLabel.WaxingGibbous -> R.string.moon_waxing_gibbous
    MoonPhaseLabel.FullMoon -> R.string.moon_full
    MoonPhaseLabel.WaningGibbous -> R.string.moon_waning_gibbous
    MoonPhaseLabel.LastQuarter -> R.string.moon_last_quarter
    MoonPhaseLabel.WaningCrescent -> R.string.moon_waning_crescent
}

data class MoonPhaseInfo(
    /** Synodic phase in [0, 1): 0 new, 0.5 full */
    val phase: Double,
    val illuminatedFraction: Double,
    val label: MoonPhaseLabel,
    val waxing: Boolean,
)

object MoonPhaseCalculator {

    fun forInstant(instant: Instant): MoonPhaseInfo {
        val jd = julianDayUtc(instant)
        val phase = synodicPhase(jd)
        val illuminated = ((1 - cos(2 * PI * phase)) / 2).coerceIn(0.0, 1.0)
        val waxing = phase < 0.5
        return MoonPhaseInfo(
            phase = phase,
            illuminatedFraction = illuminated,
            label = labelForPhase(phase),
            waxing = waxing,
        )
    }

    private fun synodicPhase(julianDay: Double): Double {
        val knownNewMoon = 2451549.09766
        val synodicMonth = 29.530588853
        var age = (julianDay - knownNewMoon) % synodicMonth
        if (age < 0) age += synodicMonth
        return age / synodicMonth
    }

    private fun julianDayUtc(instant: Instant): Double {
        val zdt = instant.atZone(ZoneOffset.UTC)
        var y = zdt.year
        val m = zdt.monthValue
        val dayFraction =
            (zdt.hour + zdt.minute / 60.0 + zdt.second / 3600.0 + zdt.nano / 3_600_000_000_000.0) / 24.0
        val d = zdt.dayOfMonth + dayFraction

        var yy = y
        var mm = m.toDouble()
        if (m <= 2) {
            yy -= 1
            mm += 12
        }
        val a = yy / 100
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (yy + 4716)) + floor(30.6001 * (mm + 1)) + d + b - 1524.5
    }

    private fun labelForPhase(phase: Double): MoonPhaseLabel {
        val p = ((phase % 1.0) + 1.0) % 1.0
        return when {
            p < 0.03 || p >= 0.97 -> MoonPhaseLabel.NewMoon
            p < 0.22 -> MoonPhaseLabel.WaxingCrescent
            p < 0.28 -> MoonPhaseLabel.FirstQuarter
            p < 0.47 -> MoonPhaseLabel.WaxingGibbous
            p < 0.53 -> MoonPhaseLabel.FullMoon
            p < 0.72 -> MoonPhaseLabel.WaningGibbous
            p < 0.78 -> MoonPhaseLabel.LastQuarter
            else -> MoonPhaseLabel.WaningCrescent
        }
    }
}
