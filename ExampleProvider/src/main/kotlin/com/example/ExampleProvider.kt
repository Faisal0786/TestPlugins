package com.example

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

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

    // Version bump to clear cache
    override var version = 175

    // =========================
    // HEADERS (Anti-Bot)
    // =========================
    private val stealthHeaders = mapOf(
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
        "Referer" to "$mainUrl/",
        "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8",
        "Accept-Language" to "en-US,en;q=0.9",
        "Upgrade-Insecure-Requests" to "1"
    )

    // =========================
    // MAIN PAGE CATEGORIES
    // =========================
    override val mainPage = mainPageOf(
        "$mainUrl/home" to "Home",
        "$mainUrl/movies" to "Latest Movies",
        "$mainUrl/tv-shows" to "Latest Series",
        "$mainUrl/category/action" to "Action",
        "$mainUrl/category/comedy" to "Comedy",
        "$mainUrl/category/horror" to "Horror",
        "$mainUrl/category/drama" to "Drama Updated"
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val url = if (page == 1) {
            request.data
        } else {
            if (request.data.contains("?")) "${request.data}&page=$page" else "${request.data}?page=$page"
        }

        val document = app.get(url, headers = stealthHeaders).document
        val isTv = request.data.contains("tv-shows")

        val items = document.select(".cb-card").mapNotNull { card ->
            val title = card.selectFirst(".cb-card-title")?.text()?.trim() ?: return@mapNotNull null
            val href = card.selectFirst("a")?.attr("href") ?: return@mapNotNull null
            val poster = card.selectFirst("img")?.attr("data-src")?.takeIf { it.isNotBlank() }
                ?: card.selectFirst("img")?.attr("src")

            if (isTv) {
                newTvSeriesSearchResponse(title, fixUrl(href), TvType.TvSeries) {
                    this.posterUrl = poster
                }
            } else {
                newMovieSearchResponse(title, fixUrl(href), TvType.Movie) {
                    this.posterUrl = poster
                }
            }
        }
        return newHomePageResponse(request.name, items)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchUrl = "$mainUrl/search?q=${query.replace(" ", "+")}"
        val document = app.get(searchUrl, headers = stealthHeaders).document

        return document.select(".cb-card").mapNotNull { card ->
            val title = card.selectFirst(".cb-card-title")?.text()?.trim() ?: return@mapNotNull null
            val href = card.selectFirst("a")?.attr("href") ?: return@mapNotNull null
            val poster = card.selectFirst("img")?.attr("data-src")?.takeIf { it.isNotBlank() }
                ?: card.selectFirst("img")?.attr("src")

            newMovieSearchResponse(title, fixUrl(href), TvType.Movie) {
                this.posterUrl = poster
            }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url, headers = stealthHeaders).document
        val title = document.selectFirst(".cb-details-title")?.text() ?: "Unknown"
        val poster = document.selectFirst("meta[property=og:image]")?.attr("content")

        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl = poster
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data, headers = stealthHeaders).document
        val iframe = document.selectFirst("iframe[src*=/embed/]")?.attr("src") ?: return false

        callback.invoke(
            ExtractorLink(name, "Stream Player", fixUrl(iframe), "$mainUrl/", Qualities.Unknown.value, false)
        )
        return true
    }
}
