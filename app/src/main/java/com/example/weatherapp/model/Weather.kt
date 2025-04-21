package com.example.weatherapp.model

data class Weather(
    val city: String,
    val temperature: Double,
    val description: String,
    val humidity: Int,
    val windSpeed: Double,
    val iconCode: String
)