dependencies {
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
}

version = 200

cloudstream {
    description = "Faisal's Premium Extension"
    authors = listOf("Faisal0786")
    status = 1
    tvTypes = listOf(
        "Movie",
        "TvSeries"
    )
    hasMainPage = true
    language = "en"
    iconUrl = "https://streamimdb.ru/favicon.ico"
}

android {
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
}