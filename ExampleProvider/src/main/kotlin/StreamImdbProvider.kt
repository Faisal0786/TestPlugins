package com.example

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.*

class StreamImdbProvider : MainAPI() {

    override var name = "StreamIMDB"
    override var mainUrl = "https://streamimdb.ru"

    override var lang = "en"

    override val hasMainPage = true
    override val hasQuickSearch = true

    override var supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries,
        TvType.Anime
    )

    override val mainPage = mainPageOf(
        "$mainUrl/" to "Home",
        "$mainUrl/movies" to "Movies",
        "$mainUrl/tv-shows" to "TV Shows",
        "$mainUrl/most-viewed" to "Most Viewed",
        "$mainUrl/most-viewed-tv" to "Top TV"
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {

        val url =
            if (page == 1)
                request.data
            else
                "${request.data}?page=$page"

        val document =
            app.get(url).document

        val home = document.select("div.cb-card")
            .mapNotNull { card ->

                val href = fixUrlNull(
                    card.selectFirst("a")
                        ?.attr("href")
                ) ?: return@mapNotNull null

                val title =
                    card.selectFirst(".cb-card-title")
                        ?.text()
                        ?.trim()
                        ?: return@mapNotNull null

                val poster =
                    fixUrlNull(
                        card.selectFirst("img")
                            ?.attr("src")
                    )

                val meta =
                    card.selectFirst(".cb-card-meta")
                        ?.text()
                        ?.lowercase()

                val isTv =
                    meta?.contains("tv") == true

                if (isTv) {

                    newTvSeriesSearchResponse(
                        title,
                        href,
                        TvType.TvSeries
                    ) {
                        this.posterUrl = poster
                    }

                } else {

                    newMovieSearchResponse(
                        title,
                        href,
                        TvType.Movie
                    ) {
                        this.posterUrl = poster
                    }
                }
            }

        return newHomePageResponse(
            request.name,
            home
        )
    }

    override suspend fun quickSearch(
        query: String
    ): List<SearchResponse>? {

        return search(query)
    }

    override suspend fun search(
        query: String
    ): List<SearchResponse> {

        val document = app.get(
            "$mainUrl/search?q=$query"
        ).document

        return document.select("div.cb-card")
            .mapNotNull { card ->

                val href = fixUrlNull(
                    card.selectFirst("a")
                        ?.attr("href")
                ) ?: return@mapNotNull null

                val title =
                    card.selectFirst(".cb-card-title")
                        ?.text()
                        ?.trim()
                        ?: return@mapNotNull null

                val poster =
                    fixUrlNull(
                        card.selectFirst("img")
                            ?.attr("src")
                    )

                val meta =
                    card.selectFirst(".cb-card-meta")
                        ?.text()
                        ?.lowercase()

                val isTv =
                    meta?.contains("tv") == true

                if (isTv) {

                    newTvSeriesSearchResponse(
                        title,
                        href,
                        TvType.TvSeries
                    ) {
                        this.posterUrl = poster
                    }

                } else {

                    newMovieSearchResponse(
                        title,
                        href,
                        TvType.Movie
                    ) {
                        this.posterUrl = poster
                    }
                }
            }
    }

    override suspend fun load(
        url: String
    ): LoadResponse? {

        val document =
            app.get(url).document

        val title =
            document.selectFirst(".cb-detail-title")
                ?.text()
                ?.trim()
                ?: return null

        val poster =
            fixUrlNull(
                document.selectFirst(".cb-detail-poster img")
                    ?.attr("src")
            )

        val backdrop =
            document.selectFirst(".cb-detail-backdrop")
                ?.attr("style")
                ?.substringAfter("url('")
                ?.substringBefore("')")

        val plot =
            document.selectFirst(".cb-detail-overview")
                ?.text()
                ?.trim()

        val year =
            Regex("""\b(19|20)\d{2}\b""")
                .find(
                    document.selectFirst(".cb-detail-meta")
                        ?.text()
                        ?: ""
                )
                ?.value
                ?.toIntOrNull()

        val tags =
            document.select("a[href*='/category/']")
                .map {
                    it.text().trim()
                }

        val isTv =
            url.contains("/tv/")

        if (isTv) {

            val episodes =
                document.select(".cb-episode-item")
                    .mapNotNull { ep ->

                        val epHref = fixUrlNull(
                            ep.attr("href")
                        ) ?: return@mapNotNull null

                        val epTitle =
                            ep.selectFirst(".cb-episode-title")
                                ?.text()
                                ?.trim()

                        val epNum =
                            ep.selectFirst(".cb-episode-num")
                                ?.text()
                                ?.toIntOrNull()

                        val season =
                            Regex("/season/(\\d+)/")
                                .find(epHref)
                                ?.groupValues
                                ?.getOrNull(1)
                                ?.toIntOrNull()

                        newEpisode(epHref) {

                            this.name = epTitle

                            this.season = season

                            this.episode = epNum
                        }
                    }

            return newTvSeriesLoadResponse(
                title,
                url,
                TvType.TvSeries,
                episodes
            ) {

                this.posterUrl = poster

                this.backgroundPosterUrl =
                    backdrop

                this.plot = plot

                this.year = year

                this.tags = tags
            }

        } else {

            return newMovieLoadResponse(
                title,
                url,
                TvType.Movie,
                url
            ) {

                this.posterUrl = poster

                this.backgroundPosterUrl =
                    backdrop

                this.plot = plot

                this.year = year

                this.tags = tags
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {

        return StreamImdbExtractor.loadLinks(
            name,
            data,
            subtitleCallback,
            callback
        )
    }
}