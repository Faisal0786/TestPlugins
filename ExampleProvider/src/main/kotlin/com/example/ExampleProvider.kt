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
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
        "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
        "Accept-Language" to "en-US,en;q=0.9",
        "Referer" to "$mainUrl/"
    )

    override val mainPage = mainPageOf(
        "$mainUrl/" to "Home",
        "$mainUrl/movies" to "Movies",
        "$mainUrl/tv-shows" to "TV Shows"
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {

        val response = app.get(
            request.data,
            headers = stealthHeaders
        )

        // =========================
        // DEBUG POPUP
        // =========================
        throw Error(
            """
URL = ${request.data}

STATUS = ${response.code}

HTML START =

${response.text.take(1000)}
            """.trimIndent()
        )

        val document = response.document

        val cards = document.select(
            """
            .cb-card,
            .movie-item,
            .poster,
            .item,
            .ml-item,
            article,
            .film
            """.trimIndent()
        )

        // SECOND DEBUG
        throw Error("TOTAL CARDS FOUND = ${cards.size}")

        val home = cards.mapNotNull {
            it.toSearchResult()
        }

        return newHomePageResponse(
            listOf(
                HomePageList(
                    request.name,
                    home
                )
            )
        )
    }

    override suspend fun search(
        query: String
    ): List<SearchResponse> {

        val url =
            "$mainUrl/search?q=${query.replace(" ", "+")}"

        val response = app.get(
            url,
            headers = stealthHeaders
        )

        throw Error(
            """
SEARCH STATUS = ${response.code}

SEARCH HTML =

${response.text.take(1000)}
            """.trimIndent()
        )

        val document = response.document

        val cards = document.select(
            """
            .cb-card,
            .movie-item,
            .poster,
            .item,
            .ml-item,
            article,
            .film
            """.trimIndent()
        )

        return cards.mapNotNull {
            it.toSearchResult()
        }
    }

    private fun Element.toSearchResult(): SearchResponse? {

        val title = this.selectFirst(
            """
            .cb-card-title,
            .title,
            h2,
            h3,
            .name,
            a[title]
            """.trimIndent()
        )?.text()?.trim()

        if (title.isNullOrEmpty()) {
            return null
        }

        val href = this.selectFirst("a")
            ?.attr("href")

        if (href.isNullOrEmpty()) {
            return null
        }

        val poster = this.selectFirst("img")
            ?.attr("data-src")
            ?.ifBlank { null }
            ?: this.selectFirst("img")
                ?.attr("src")
                ?.ifBlank { null }

        return newMovieSearchResponse(
            title,
            fixUrl(href),
            TvType.Movie
        ) {
            this.posterUrl = poster
        }
    }

    override suspend fun load(
        url: String
    ): LoadResponse? {

        val response = app.get(
            url,
            headers = stealthHeaders
        )

        throw Error(
            """
LOAD STATUS = ${response.code}

LOAD HTML =

${response.text.take(1000)}
            """.trimIndent()
        )

        val document = response.document

        val title = document.selectFirst("title")
            ?.text()
            ?: return null

        return newMovieLoadResponse(
            title,
            url,
            TvType.Movie,
            url
        ) {
            posterUrl = document.selectFirst("img")
                ?.attr("src")
        }
    }
}
