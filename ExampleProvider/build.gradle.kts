import org.jetbrains.kotlin.konan.properties.Properties

version = 1

android {

    defaultConfig {

        val properties = Properties()

        properties.load(
            project.rootProject
                .file("local.properties")
                .inputStream()
        )

        android.buildFeatures.buildConfig = true

        buildConfigField(
            "String",
            "TMDB_KEY",
            "\"${properties.getProperty("TMDB_KEY")}\""
        )
    }
}

cloudstream {

    description = "StreamIMDB Premium Provider"

    authors = listOf(
        "Faisal0786"
    )

    status = 1

    tvTypes = listOf(
        "TvSeries",
        "Movie",
        "AsianDrama",
        "Anime",
        "Torrent"
    )

    iconUrl = "https://streamimdb.ru/favicon.ico"
}