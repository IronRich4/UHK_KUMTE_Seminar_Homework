package com.example.kumte_simplyweather

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import com.google.android.gms.location.LocationServices
import java.util.*
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resumeWithException

suspend fun <T> Task<T>.await(): T {
    return suspendCancellableCoroutine { cont ->
        addOnCompleteListener {
            if (it.exception != null) {
                cont.resumeWithException(it.exception!!)
            } else {
                cont.resume(it.result, null)
            }
        }
    }
}

@SuppressLint("MissingPermission")
suspend fun getCurrentLocation(context: Context): Location? {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    return fusedLocationClient.lastLocation.await()
}

fun getCityName(context: Context, location: Location): String {
    val geocoder = Geocoder(context, Locale.getDefault())
    try {
        Log.d("LocationUtils", "getCityName: Latitude = ${location.latitude}, Longitude = ${location.longitude}")
        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
        if (addresses != null && addresses.isNotEmpty()) {
            val cityName = addresses[0]?.locality ?: addresses[0]?.subAdminArea ?: "Neznámé město"
            Log.d("LocationUtils", "getCityName: CityName = $cityName")
            return cityName
        } else {
            Log.w("LocationUtils", "getCityName: No addresses found for location")
            return "Neznámé město"
        }
    } catch (e: Exception) {
        Log.e("LocationUtils", "getCityName: Error = ${e.message}")
        return "Neznámé město"
    }
}
