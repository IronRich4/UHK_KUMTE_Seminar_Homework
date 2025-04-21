package com.example.kumte_simplyweather

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WeatherScreen()
                }
            }
        }
    }
}

data class Weather(
    val city: String,
    val temperature: Double,
    val description: String,
    val humidity: Int,
    val windSpeed: Double,
    val iconCode: String
)

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
                    val apiKey = "0cbdaf5e8430497d15e4a5784cc9c1f8" // NAHRAĎ SVÝM API KLÍČEM!!
                    val url = URL("https://api.openweathermap.org/data/2.5/weather?q=$cityName&units=metric&appid=$apiKey&lang=cz")

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
                }

                // Aktualizace stavové proměnné weather, aby se UI překreslilo
                weather = fetchedWeather
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

            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(16.dp)
                    )
                }
                errorMessage != null -> {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                weather != null -> {
                    WeatherCard(weather!!)
                }
            }
        }
    }
}

@Composable
fun WeatherCard(weather: Weather) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = weather.city,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Emoji podle počasí
            val weatherEmoji = when {
                weather.iconCode.contains("01") -> "☀️" // čisté nebe
                weather.iconCode.contains("02") -> "⛅" // polojasno
                weather.iconCode.contains("03") || weather.iconCode.contains("04") -> "☁️" // oblačno
                weather.iconCode.contains("09") || weather.iconCode.contains("10") -> "🌧️" // déšť
                weather.iconCode.contains("11") -> "⛈️" // bouřka
                weather.iconCode.contains("13") -> "❄️" // sníh
                weather.iconCode.contains("50") -> "🌫️" // mlha
                else -> "🌡️"
            }

            Text(
                text = weatherEmoji,
                fontSize = 64.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Text(
                text = "${weather.temperature.toInt()}°C",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Text(
                text = weather.description.capitalize(),
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Vlhkost")
                    Text(
                        text = "${weather.humidity}%",
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Vítr")
                    Text(
                        text = "${weather.windSpeed} m/s",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun String.capitalize(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}
