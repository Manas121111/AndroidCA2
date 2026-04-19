package com.smarttour360.app.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.Locale

data class UserLocation(
    val latitude: Double,
    val longitude: Double,
    val cityName: String
)

object LocationHelper {

    @SuppressLint("MissingPermission")
    fun getCurrentLocation(context: Context): Flow<UserLocation?> = callbackFlow {
        val hasFinePermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarsePermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFinePermission && !hasCoarsePermission) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        var completed = false

        fun emitAndClose(latitude: Double, longitude: Double) {
            if (completed) return
            completed = true
            val city = reverseGeocode(context, latitude, longitude)
            trySend(UserLocation(latitude, longitude, city))
            close()
        }

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                fusedClient.removeLocationUpdates(this)
                emitAndClose(location.latitude, location.longitude)
            }
        }

        fusedClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    emitAndClose(location.latitude, location.longitude)
                } else if (!completed) {
                    val request = LocationRequest.Builder(
                        Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                        10_000L
                    )
                        .setMaxUpdates(1)
                        .setWaitForAccurateLocation(false)
                        .build()
                    fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
                }
            }
            .addOnFailureListener {
                if (!completed) {
                    trySend(null)
                    close()
                }
            }

        awaitClose {
            fusedClient.removeLocationUpdates(callback)
        }
    }

    fun reverseGeocode(context: Context, lat: Double, lon: Double): String {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            addresses?.firstOrNull()?.let { address ->
                address.locality
                    ?: address.subAdminArea
                    ?: address.adminArea
                    ?: "Your Location"
            } ?: "Your Location"
        } catch (_: Exception) {
            "Your Location"
        }
    }
}
