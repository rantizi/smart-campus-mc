# Aturan R8 build release PaduKampus (optimasi dan pengaburan kode, Laporan 3).

# Gson membaca DTO Open-Meteo lewat refleksi; beberapa field (current, hourly, time) tidak
# memakai @SerializedName sehingga namanya tidak boleh diubah R8. Cache cuaca di Room juga
# menyimpan HourlyWeatherDto sebagai JSON.
-keep class com.unpad.padukampus.data.remote.dto.** { *; }
-keepattributes Signature, *Annotation*

# Retrofit, OkHttp, Room, WorkManager, Navigation, osmdroid dan Firebase membawa consumer rules
# sendiri. Tanda tangan generik interface Retrofit dipertahankan untuk suspend function.
-keep,allowobfuscation,allowshrinking interface com.unpad.padukampus.data.remote.api.OpenMeteoApi

# Nomor baris tetap terbaca di laporan crash tanpa membuka nama kelas asli.
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile
