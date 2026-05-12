package com.example

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class ExampleProvider : MainAPI() {

    override var mainUrl = "https://streamimdb.ru"
    override var name = "StreamIMDB"
    override val hasMainPage = true
    override var lang = "en"
    override val hasQuickSearch = true

    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries
    )

    private val stealthHeaders = mapOf(
        "User-Agent" to "Mozilla/5.0",
        "Referer" to "$mainUrl/"
    )

    override val mainPage = mainPageOf(
        "$mainUrl/" to "Home"
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {

        val items = listOf(
            newMovieSearchResponse(
                "TEST MOVIE",
                "$mainUrl/test",
                TvType.Movie
            ) {
                posterUrl =
                    "https://via.placeholder.com/300x450.png"
            }
        )

        return newHomePageResponse(
            HomePageList(
                "TEST",
                items
            )
        )
    }

    override suspend fun search(
        query: String
    ): List<SearchResponse> {

        return listOf(
            newMovieSearchResponse(
                "TEST SEARCH",
                "$mainUrl/test",
                TvType.Movie
            ) {
                posterUrl =
                    "https://via.placeholder.com/300x450.png"
            }
        )
    }

    override suspend fun load(
        url: String
    ): LoadResponse {

        return newMovieLoadResponse(
            "TEST MOVIE",
            url,
            TvType.Movie,
            url
        ) {
            posterUrl =
                "https://via.placeholder.com/300x450.png"

            plot = "Test description"
            year = 2025
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {

        callback.invoke(
            newExtractorLink(
                source = name,
                name = name,
                url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
            ) {
                quality = Qualities.P1080.value
            }
        )

        return true
    }
}
