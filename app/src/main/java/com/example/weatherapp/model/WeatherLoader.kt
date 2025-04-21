package com.example.weatherapp.model

interface WeatherLoader {
    suspend fun loadWeather(city: String): Weather
}