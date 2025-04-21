package com.example.kumte_simplyweather

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun LocationScreen(onCityDetected: (String) -> Unit) {
    val context = LocalContext.current
    var city by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true &&
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
                Log.d("LocationScreen", "Permissions granted")
                getLocation(context, scope, onCityDetected) { newCity, newLoading, newError ->
                    city = newCity
                    isLoading = newLoading
                    error = newError
                }
            } else {
                Log.d("LocationScreen", "Permissions denied")
                error = "Přístup k poloze byl odepřen"
            }
        }
    )

    LaunchedEffect(Unit) {
        if (hasLocationPermission(context)) {
            Log.d("LocationScreen", "Permissions already granted")
            getLocation(context, scope, onCityDetected) { newCity, newLoading, newError ->
                city = newCity
                isLoading = newLoading
                error = newError
            }
        } else {
            Log.d("LocationScreen", "Requesting permissions")
            permissionLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Zjišťuji vaši polohu...", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        when {
            isLoading -> CircularProgressIndicator()
            error != null -> Text(error!!, color = MaterialTheme.colorScheme.error)
            city != null -> {
                Text("Vaše město: $city", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(16.dp))
                Button(onClick = { onCityDetected(city!!) }) {
                    Text("Pokračovat")
                }
            }
        }
    }
}

fun getLocation(
    context: Context,
    scope: CoroutineScope,
    onCityDetected: (String) -> Unit,
    updateState: (String?, Boolean, String?) -> Unit
) {
    updateState(null, true, null)
    scope.launch {
        try {
            val location = getCurrentLocation(context)
            if (location != null) {
                val cityName = getCityName(context, location)
                updateState(cityName, false, null)
                onCityDetected(cityName)
            } else {
                updateState(null, false, "Nelze získat polohu")
            }
        } catch (e: Exception) {
            updateState(null, false, "Chyba: ${e.message}")
        }
    }
}

