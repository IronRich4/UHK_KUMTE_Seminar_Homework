package com.example.kumte_simplyweather.model

import android.util.Log
import com.example.weatherapp.model.Weather
import com.example.weatherapp.model.WeatherLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

class OpenWeatherLoader : WeatherLoader {
    // API klíč pro OpenWeatherMap - používáme prozatím test klíč pro ladění
    private val API_KEY = "0cbdaf5e8430497d15e4a5784cc9c1f8" // testovací klíč pro ladění
    private val WEATHER_URL = "https://api.openweathermap.org/data/2.5/weather"

    override suspend fun loadWeather(city: String): Weather = withContext(Dispatchers.IO) {
        try {
            val encodedCity = java.net.URLEncoder.encode(city, "UTF-8")
            val url = URL("$WEATHER_URL?q=$encodedCity&units=metric&appid=$API_KEY&lang=cz")

            Log.d("WeatherLoader", "Fetching weather from: $url")

            val response = url.readText()
            Log.d("WeatherLoader", "Received response: $response")

            val jsonObj = JSONObject(response)
            val main = jsonObj.getJSONObject("main")
            val wind = jsonObj.getJSONObject("wind")
            val weather = jsonObj.getJSONArray("weather").getJSONObject(0)

            Weather(
                city = jsonObj.getString("name"), // Používáme název města z odpovědi
                temperature = main.getDouble("temp"),
                description = weather.getString("description"),
                humidity = main.getInt("humidity"),
                windSpeed = wind.getDouble("speed"),
                iconCode = weather.getString("icon")
            )
        } catch (e: Exception) {
            Log.e("WeatherLoader", "Error loading weather", e)
            throw e
        }
    }
}