package com.unpad.padukampus.data.remote.api

import com.unpad.padukampus.data.remote.dto.WeatherDto
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenMeteoApi {
    @GET("v1/forecast")
    suspend fun forecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "temperature_2m,weather_code",
        @Query("hourly") hourly: String = "precipitation_probability",
        @Query("timezone") timezone: String = "Asia/Jakarta",
        @Query("forecast_days") days: Int = 2,
    ): WeatherDto
}
