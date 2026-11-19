package com.unpad.padukampus.ui.home

/** Deskripsi singkat kode cuaca WMO yang dipakai Open-Meteo (field weather_code). */
object WeatherCodes {
    fun describe(code: Int): String =
        when (code) {
            0 -> "cerah"
            1, 2 -> "cerah berawan"
            3 -> "berawan"
            45, 48 -> "berkabut"
            51, 53, 55, 56, 57 -> "gerimis"
            61, 63, 66 -> "hujan"
            65, 67 -> "hujan lebat"
            80, 81 -> "hujan lokal"
            82 -> "hujan lokal lebat"
            95 -> "badai petir"
            96, 99 -> "badai petir disertai es"
            71, 73, 75, 77, 85, 86 -> "salju"
            else -> "kode cuaca $code"
        }
}
