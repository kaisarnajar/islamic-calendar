package com.islamic.calendar.data

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.time.ZoneId
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class ZoneResolution(
    val zoneId: ZoneId,
    /** True when [zoneId] came from [android.location.Address.getTimezone] (API 34+). */
    val usedGeocoderTimezone: Boolean,
)

class LocationRepository(private val context: Context) {

    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)

    /**
     * Uses approximate location and reverse geocoding when possible (API 34+ [android.location.Address.getTimezone]),
     * otherwise [ZoneId.systemDefault] with [ZoneResolution.usedGeocoderTimezone] false.
     */
    suspend fun resolveZoneFromLastKnownLocation(): ZoneResolution = withContext(Dispatchers.IO) {
        val location = fetchBestLocation()
            ?: return@withContext ZoneResolution(ZoneId.systemDefault(), false)
        val zone = zoneIdFromLocation(location)
        if (zone != null) {
            ZoneResolution(zone, true)
        } else {
            ZoneResolution(ZoneId.systemDefault(), false)
        }
    }

    private suspend fun fetchBestLocation(): Location? {
        return try {
            fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await()
                ?: fusedClient.lastLocation.await()
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun zoneIdFromLocation(location: Location): ZoneId? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(context, Locale.getDefault())
        val address = geocoder.getFromLocationCompat(location.latitude, location.longitude)
            ?: return null
        return timezoneZoneId(address)
    }

    /** [Address.getTimezone] is API 34+; reflect so older compile SDK stubs still build. */
    private fun timezoneZoneId(address: Address): ZoneId? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return null
        val id = try {
            Address::class.java.getMethod("getTimezone").invoke(address) as? String
        } catch (_: Exception) {
            null
        } ?: return null
        return id.takeIf { it.isNotBlank() }?.let { ZoneId.of(it) }
    }

    private suspend fun Geocoder.getFromLocationCompat(
        latitude: Double,
        longitude: Double,
    ) = suspendCoroutine { cont ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getFromLocation(latitude, longitude, 1) { addresses ->
                cont.resume(addresses.firstOrNull())
            }
        } else {
            @Suppress("DEPRECATION")
            cont.resume(getFromLocation(latitude, longitude, 1)?.firstOrNull())
        }
    }
}
