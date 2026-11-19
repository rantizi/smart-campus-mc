package com.unpad.padukampus.data.remote.dto

import com.google.gson.annotations.SerializedName

// Nullable wire fields are validated by the repository; Gson does not enforce Kotlin nullability.
data class WeatherDto(val current: CurrentWeatherDto?, val hourly: HourlyWeatherDto?)

data class CurrentWeatherDto(
    @SerializedName("temperature_2m") val temperature: Double?,
    @SerializedName("weather_code") val weatherCode: Int?,
)

data class HourlyWeatherDto(
    val time: List<String>?,
    @SerializedName("precipitation_probability") val probability: List<Int?>?,
)
