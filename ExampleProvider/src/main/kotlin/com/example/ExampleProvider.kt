package com.example

import android.util.Log
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

    override val mainPage = mainPageOf(
        "$mainUrl/movies" to "Movies"
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {

        val document = app.get(
            request.data,
            headers = mapOf(
                "User-Agent" to USER_AGENT
            )
        ).document

        val home = document.select(".cb-card").mapNotNull { element ->
            element.toSearchResult()
        }

        Log.d("StreamIMDB", "HOME ITEMS = ${home.size}")

        return newHomePageResponse(
            request.name,
            home
        )
    }

    private fun Element.toSearchResult(): SearchResponse? {

        val aTag = this.selectFirst("a")
        val img = this.selectFirst("img")

        val title = img?.attr("alt")?.trim()
            ?: return null

        val href = fixUrlNull(
            aTag?.attr("href")
        ) ?: return null

        val poster = fixUrlNull(
            img.attr("src")
        )

        return newMovieSearchResponse(
            title,
            href,
            TvType.Movie
        ) {
            this.posterUrl = poster
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {

        val document = app.get(
            "$mainUrl/search/$query",
            headers = mapOf(
                "User-Agent" to USER_AGENT
            )
        ).document

        return document.select(".cb-card").mapNotNull {
            it.toSearchResult()
        }
    }

    override suspend fun load(url: String): LoadResponse {

        val document = app.get(
            fixUrl(url),
            headers = mapOf(
                "User-Agent" to USER_AGENT
            )
        ).document

        val title =
            document.selectFirst("meta[property=og:title]")
                ?.attr("content")
                ?.substringBefore(" -")
                ?.trim()

                ?: document.selectFirst("title")
                    ?.text()
                    ?.substringBefore(" -")
                    ?.trim()

                ?: "No Title"

        val poster = fixUrlNull(
            document.selectFirst("meta[property=og:image]")
                ?.attr("content")
        )

        val description =
            document.selectFirst("meta[property=og:description]")
                ?.attr("content")

        val year = Regex("""(19|20)\d{2}""")
            .find(document.text())
            ?.value
            ?.toIntOrNull()

        val ratingText =
            document.selectFirst("[class*=rating]")
                ?.text()

        val rating = ratingText
            ?.filter {
                it.isDigit() || it == '.'
            }
            ?.toRatingInt()

        val actors = document
            .select("a[href*=actor], a[href*=cast]")
            .map {
                Actor(it.text())
            }

        return newMovieLoadResponse(
            title,
            url,
            TvType.Movie,
            url
        ) {

            this.posterUrl = poster
            this.plot = description
            this.year = year
            this.rating = rating

            if (actors.isNotEmpty()) {
                this.actors = actors
            }
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
                source = this.name,
                name = this.name,
                url = data
            ) {
                referer = ""
                quality = Qualities.Unknown.value
            }
        )

        return true
    }
}
