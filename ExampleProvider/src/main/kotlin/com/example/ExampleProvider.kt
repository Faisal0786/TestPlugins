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

    // =========================
    // HEADERS
    // =========================
    private val stealthHeaders = mapOf(
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
        "Referer" to "$mainUrl/",
        "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
        "Accept-Language" to "en-US,en;q=0.9"
    )

    // =========================
    // MAIN PAGE
    // =========================
    override val mainPage = mainPageOf(
        "$mainUrl/" to "Home",
        "$mainUrl/movies" to "Movies",
        "$mainUrl/tv-shows" to "TV Shows",
        "$mainUrl/category/action" to "Action",
        "$mainUrl/category/comedy" to "Comedy",
        "$mainUrl/category/horror" to "Horror"
    )

    // =========================
    // MAIN PAGE LOAD
    // =========================
    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {

        val url =
            if (page == 1) {
                request.data
            } else {
                "${request.data}?page=$page"
            }

        val document = app.get(
            url,
            headers = stealthHeaders
        ).document

        val home = document.select(
            ".cb-card, .movie-card, .item, .poster"
        ).mapNotNull { it.toSearchResult() }

        return newHomePageResponse(
            request.name,
            home
        )
    }

    // =========================
    // SEARCH
    // =========================
    override suspend fun search(query: String): List<SearchResponse> {

        val searchUrl =
            "$mainUrl/search?q=${query.replace(" ", "+")}"

        val document = app.get(
            searchUrl,
            headers = stealthHeaders
        ).document

        return document.select(
            ".cb-card, .movie-card, .item, .poster"
        ).mapNotNull { it.toSearchResult() }
    }

    // =========================
    // LOAD DETAILS
    // =========================
    override suspend fun load(url: String): LoadResponse {

        val document = app.get(
            url,
            headers = stealthHeaders
        ).document

        val title =
            document.selectFirst(
                "h1, .cb-details-title, .entry-title"
            )?.text()?.trim()
                ?: "Unknown"

        val poster =
            document.selectFirst(
                "meta[property=og:image]"
            )?.attr("content")

        val descriptionText =
            document.selectFirst(
                ".description, .content, .entry-content, .overview"
            )?.text()?.trim()

        val year =
            document.selectFirst(
                ".year, .date, .release"
            )?.text()
                ?.filter { it.isDigit() }
                ?.toIntOrNull()

        val isTv =
            url.contains("/tv") ||
            url.contains("series") ||
            url.contains("tv-shows")

        return if (isTv) {

            newTvSeriesLoadResponse(
                title,
                url,
                TvType.TvSeries,
                emptyList()
            ) {
                posterUrl = poster
                descriptionText?.let {
                    plot = it
                }
                year?.let {
                    this.year = it
                }
            }

        } else {

            newMovieLoadResponse(
                title,
                url,
                TvType.Movie,
                url
            ) {
                posterUrl = poster
                descriptionText?.let {
                    plot = it
                }
                year?.let {
                    this.year = it
                }
            }
        }
    }

    // =========================
    // LOAD LINKS
    // =========================
    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {

        val document = app.get(
            data,
            headers = stealthHeaders
        ).document

        val iframe =
            document.selectFirst("iframe")
                ?.attr("src")
                ?: return false

        callback.invoke(
            newExtractorLink(
                source = name,
                name = "Stream Player",
                url = fixUrl(iframe)
            ) {
                referer = "$mainUrl/"
                quality = Qualities.Unknown.value
                isM3u8 = iframe.contains(".m3u8")
            }
        )

        return true
    }

    // =========================
    // PARSER
    // =========================
    private fun Element.toSearchResult(): SearchResponse? {

        val title =
            selectFirst(
                ".cb-card-title, .movie-title, h2, h3, .title"
            )?.text()?.trim()
                ?: return null

        val href =
            selectFirst("a")
                ?.attr("href")
                ?: return null

        val poster =
            selectFirst("img")
                ?.attr("data-src")
                ?.takeIf { it.isNotBlank() }
                ?: selectFirst("img")
                    ?.attr("src")

        val isTv =
            href.contains("/tv") ||
            href.contains("series") ||
            href.contains("tv-shows")

        return if (isTv) {

            newTvSeriesSearchResponse(
                title,
                fixUrl(href),
                TvType.TvSeries
            ) {
                posterUrl = poster
            }

        } else {

            newMovieSearchResponse(
                title,
                fixUrl(href),
                TvType.Movie
            ) {
                posterUrl = poster
            }
        }
    }
}
