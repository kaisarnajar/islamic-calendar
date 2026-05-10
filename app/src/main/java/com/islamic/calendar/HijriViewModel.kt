package com.islamic.calendar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.islamic.calendar.domain.HijriCalendar
import com.islamic.calendar.domain.HijriDisplay
import com.islamic.calendar.domain.MoonPhaseCalculator
import com.islamic.calendar.domain.MoonPhaseInfo
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HijriUiState(
    val zoneId: ZoneId,
    val usedGeocoderTimezone: Boolean,
    val hijri: HijriDisplay,
    val moon: MoonPhaseInfo,
    val offsetDays: Int,
    val isRefreshingLocation: Boolean,
)

class HijriViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as IslamicCalendarApp
    private val userOffsetRepository = app.userOffsetRepository
    private val locationRepository = app.locationRepository

    private val _uiState =
        MutableStateFlow(buildState(zoneId = ZoneId.systemDefault(), usedGeocoderTimezone = false, offset = 0))
    val uiState: StateFlow<HijriUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userOffsetRepository.offsetDays.collectLatest { offset ->
                _uiState.update { current ->
                    buildState(current.zoneId, current.usedGeocoderTimezone, offset)
                }
            }
        }
    }

    fun refreshZoneFromLocation() {
        viewModelScope.launch {
            val offset = _uiState.value.offsetDays
            _uiState.update { it.copy(isRefreshingLocation = true) }
            val resolution = locationRepository.resolveZoneFromLastKnownLocation()
            _uiState.value = buildState(
                resolution.zoneId,
                resolution.usedGeocoderTimezone,
                offset,
            )
            _uiState.update { it.copy(isRefreshingLocation = false) }
        }
    }

    /** Call when coarse location permission is revoked so the calendar falls back to device time zone. */
    fun clearLocationZone() {
        val offset = _uiState.value.offsetDays
        _uiState.value = buildState(ZoneId.systemDefault(), usedGeocoderTimezone = false, offset = offset)
    }

    fun incrementOffset() {
        persistOffset(_uiState.value.offsetDays + 1)
    }

    fun decrementOffset() {
        persistOffset(_uiState.value.offsetDays - 1)
    }

    private fun persistOffset(value: Int) {
        viewModelScope.launch {
            userOffsetRepository.setOffsetDays(value)
        }
    }

    private fun buildState(zoneId: ZoneId, usedGeocoderTimezone: Boolean, offset: Int): HijriUiState {
        val hijri = HijriCalendar.todayDisplay(zoneId, offset)
        val moonInstant = HijriCalendar.moonInstantAtLocalNoon(zoneId)
        val moon = MoonPhaseCalculator.forInstant(moonInstant)
        return HijriUiState(
            zoneId = zoneId,
            usedGeocoderTimezone = usedGeocoderTimezone,
            hijri = hijri,
            moon = moon,
            offsetDays = offset,
            isRefreshingLocation = false,
        )
    }
}
