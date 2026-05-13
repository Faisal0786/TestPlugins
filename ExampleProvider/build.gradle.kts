dependencies {
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
}

// Use an integer for version numbers
version = 200

cloudstream {
    name = "ExampleProvider"
    description = "Faisal's Premium Extension"

    authors = listOf("Faisal0786")

    status = 1

    language = "en"

    tvTypes = listOf(
        "Movie",
        "TvSeries"
    )

    iconUrl = "https://streamimdb.ru/favicon.ico"
}

android {
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
}
