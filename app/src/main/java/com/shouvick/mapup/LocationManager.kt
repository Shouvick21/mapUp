package com.shouvick.mapup

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

// Ensure this matches your data class definition
data class Location(
    val lat: String,
    val long: String
)

class LocationManager(
    private val context: Context
) {
    private val fusedLocation = LocationServices.getFusedLocationProviderClient(context)

    // Using callbackFlow to convert the Google Callback API into a Coroutine Flow
    fun getLocationFlow(): Flow<Location> = callbackFlow {

        // 1. Check Permissions
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            close(Exception("Missing Location Permissions"))
            return@callbackFlow
        }


        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            2000L
        ).apply {
            setMinUpdateIntervalMillis(1000L)
            setWaitForAccurateLocation(false)
        }.build()


        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val androidLocation = result.lastLocation ?: return

                val myLocation = Location(
                    lat = androidLocation.latitude.toString(),
                    long = androidLocation.longitude.toString()
                )

                trySend(myLocation)
            }
        }

        try {
            fusedLocation.requestLocationUpdates(
                locationRequest,
                callback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            close(e) // Should not happen due to check above, but good practice
        }


        awaitClose {
            Log.d("LocationManager", "Stopping location updates")
            fusedLocation.removeLocationUpdates(callback)
        }
    }
    // Add this inside your LocationManager class
    fun checkLocationSettings(
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            2000L
        ).build()

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .setAlwaysShow(true)

        val settingsClient = LocationServices.getSettingsClient(context)
        val task = settingsClient.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            // GPS is already ON and High Accuracy
            onSuccess()
        }

        task.addOnFailureListener { exception ->
            // GPS is OFF or Low Accuracy
            onFailure(exception)
        }
    }
}

