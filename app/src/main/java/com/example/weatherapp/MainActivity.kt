package com.example.kumte_simplyweather

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import androidx.compose.ui.platform.LocalFocusManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import org.json.JSONObject
import java.net.URL

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val navController = rememberNavController()
                NavHost(navController, startDestination = "location") {
                    composable("location") {
                        LocationScreen { city ->
                            navController.navigate("weather/$city")
                        }
                    }
                    composable(
                        route = "weather/{city}",
                        arguments = listOf(navArgument("city") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val city = backStackEntry.arguments?.getString("city") ?: "Praha"
                        WeatherScreen(city)
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen() {
    var city by remember { mutableStateOf("Praha") }
    var weather by remember { mutableStateOf<Weather?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val TAG = "WeatherScreen"

    // Funkce pro bezpečné načtení počasí
    fun loadWeather(cityName: String) {
        if (cityName.isBlank()) {
            errorMessage = "Zadejte název města"
            return
        }

        isLoading = true
        errorMessage = null

        scope.launch {
            try {
                Log.d(TAG, "Načítání počasí pro město: $cityName")

                val fetchedWeather = withContext(Dispatchers.IO) {
                    val apiKey = "0cbdaf5e8430497d15e4a5784cc9c1f8\n"
                    val url = URL("https://api.openweathermap.org/data/2.5/weather?q=$cityName&units=metric&appid=$apiKey&lang=cz")

                    try {
                        val response = url.readText()
                        Log.d(TAG, "API odpověď: $response")

                        val jsonObj = JSONObject(response)
                        val main = jsonObj.getJSONObject("main")
                        val wind = jsonObj.getJSONObject("wind")
                        val weatherArray = jsonObj.getJSONArray("weather")
                        val weatherObj = weatherArray.getJSONObject(0)

                        Weather(
                            city = jsonObj.getString("name"),
                            temperature = main.getDouble("temp"),
                            description = weatherObj.getString("description"),
                            humidity = main.getInt("humidity"),
                            windSpeed = wind.getDouble("speed"),
                            iconCode = weatherObj.getString("icon")
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Chyba při čtení odpovědi: ${e.message}", e)
                        throw e
                    }
                }

                // Update UI
                weather = fetchedWeather // Use the correct state variable
                Log.d(TAG, "Počasí úspěšně načteno: ${weather?.city}, ${weather?.temperature}°C")
            } catch (e: Exception) {
                Log.e(TAG, "Chyba: ${e.message}", e)
                errorMessage = "Nepodařilo se načíst počasí: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    // Načíst počasí při prvním zobrazení
    LaunchedEffect(Unit) {
        loadWeather(city)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                title = {
                    Text(
                        text = "Počasí",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TextField(
                value = city,
                onValueChange = { city = it },
                label = { Text("Město") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    focusManager.clearFocus()
                    loadWeather(city)
                }),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            Button(
                onClick = { loadWeather(city) },
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Text("Zobrazit počasí")
            }

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(16.dp)
                )
            } else if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            } else if (weather != null) {
                WeatherCard(weather!!)
            }
        }
    }
}

private fun String.capitalize(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}
