package com.example

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.AppUtils.toJson

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

    data class LoadLinkData(
        val imdbId: String,
        val tmdbId: String,
        val type: String,
        val season: Int? = null,
        val episode: Int? = null
    )

    private val stealthHeaders = mapOf(
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
        "Referer" to "$mainUrl/",
        "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8"
    )

    override val mainPage = mainPageOf(
        "top10" to "Top 10 Today",
        "trending" to "Trending Tv Shows",
        "popular" to "Popular Now",
        "episodes" to "Latest Episodes",
        "latesttv" to "Latest TV Shows",
        "toprated" to "Top Rated"
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {

        val document =
            app.get(
                mainUrl,
                headers = stealthHeaders
            ).document

        val items = when (request.data) {

            "top10" -> {

                document.select(".cb-top10-item")
                    .mapNotNull { card ->

                        val href =
                            fixUrlNull(card.attr("href"))
                                ?: return@mapNotNull null

                        val title =
                            card.attr("aria-label")
                                .substringAfter("-")
                                .trim()

                        val poster =
                            fixUrlNull(
                                card.selectFirst("img")
                                    ?.attr("src")
                            )

                        val isTv =
                            href.contains("/tv/")

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

            "trending" -> {

                document.select(
                    "#cb-trending-series .cb-card"
                ).mapNotNull {
                    toSearchResult(it)
                }
            }

            "popular" -> {

                document.select(".cb-row-section")
                    .eq(1)
                    .select(".cb-card")
                    .mapNotNull {
                        toSearchResult(it)
                    }
            }

            "episodes" -> {

                document.select(".cb-row-section")
                    .eq(2)
                    .select(".cb-card")
                    .mapNotNull {
                        toSearchResult(it)
                    }
            }

            "latesttv" -> {

                document.select(".cb-row-section")
                    .eq(3)
                    .select(".cb-card")
                    .mapNotNull {
                        toSearchResult(it)
                    }
            }

            "toprated" -> {

                document.select(".cb-row-section")
                    .eq(4)
                    .select(".cb-card")
                    .mapNotNull {
                        toSearchResult(it)
                    }
            }

            else -> emptyList()
        }

        return newHomePageResponse(
            request.name,
            items
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

        val document =
            app.get(
                "$mainUrl/search?q=${query.replace(" ", "+")}",
                headers = stealthHeaders
            ).document

        return document.select("div.cb-card")
            .mapNotNull { card ->

                val href =
                    fixUrlNull(
                        card.selectFirst("a")
                            ?.attr("href")
                    ) ?: return@mapNotNull null

                val title =
                    card.selectFirst(".cb-card-title")
                        ?.text()
                        ?.trim()
                        ?: return@mapNotNull null

                val imgElement =
                    card.selectFirst("img")

                val poster =
                    fixUrlNull(
                        imgElement?.attr("data-src")
                            ?.takeIf { it.isNotEmpty() }
                            ?: imgElement?.attr("src")
                    )

                val isTv =
                    href.contains("/tv/")

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

    private fun toSearchResult(
        card: org.jsoup.nodes.Element
    ): SearchResponse? {

        val href =
            fixUrlNull(
                card.selectFirst("a")
                    ?.attr("href")
            ) ?: return null

        val title =
            card.selectFirst(".cb-card-title")
                ?.text()
                ?.trim()
                ?: return null

        val poster =
            fixUrlNull(
                card.selectFirst("img")
                    ?.attr("data-src")
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?: card.selectFirst("img")
                        ?.attr("src")
            )

        val isTv =
            href.contains("/tv/")

        return if (isTv) {

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

    override suspend fun load(
        url: String
    ): LoadResponse? {

        val res =
            app.get(
                url,
                headers = stealthHeaders
            )

        val document =
            res.document

        val title =
            document.selectFirst(".cb-detail-title-logo")
                ?.attr("alt")
                ?: document.selectFirst(".cb-detail-title")
                    ?.text()
                ?: document.selectFirst("h1")
                    ?.text()
                ?: "Unknown"

        val poster =
            document.selectFirst("meta[property=og:image]")
                ?.attr("content")

        val backdrop =
            document.selectFirst(".cb-detail-banner-bg")
                ?.attr("style")
                ?.substringAfter("url('")
                ?.substringBefore("')")

        val plot =
            document.selectFirst("#cbPlot")
                ?.text()
                ?.trim()

        val tmdbId =
            Regex("""__cbCwMeta\s*=\s*\{.*?"id":"(\d+)"""")
                .find(document.html())
                ?.groupValues
                ?.getOrNull(1)
                ?: "null"

        val imdbId =
            if (tmdbId != "null") {

                try {

                    val mediaType =
                        if (
                            url.contains("/tv/")
                            || document.select(".cb-season").isNotEmpty()
                        ) {
                            "tv"
                        } else {
                            "movie"
                        }

                    val external =
                        app.get(
                            "https://api.themoviedb.org/3/$mediaType/$tmdbId/external_ids?api_key=fceea78d0d9713c879f0cfeb0dbfb40b"
                        ).text

                    org.json.JSONObject(external)
                        .optString("imdb_id", "null")

                } catch (e: Exception) {
                    "null"
                }

            } else {
                "null"
            }

        val year =
            Regex("""(19|20)\d{2}""")
                .find(document.text())
                ?.value
                ?.toIntOrNull()

        val tags =
            document.select(".cb-meta-plain")
                .map {
                    it.text().trim()
                }

        val trailer =
            document.selectFirst("#cbBgTrailer")
                ?.attr("src")
                ?.substringAfter("/embed/")
                ?.substringBefore("?")

        val actors =
            document.select(".cb-cast-item-card")
                .mapNotNull {

                    val actorName =
                        it.selectFirst(".cb-cast-item-name")
                            ?.text()
                            ?.trim()
                            ?: return@mapNotNull null

                    val actorRole =
                        it.selectFirst(".cb-cast-item-role")
                            ?.text()
                            ?.trim()

                    val actorImage =
                        fixUrlNull(
                            it.selectFirst("img")
                                ?.attr("data-src")
                                ?.takeIf { img -> img.isNotBlank() }
                                ?: it.selectFirst("img")
                                    ?.attr("src")
                        )

                    ActorData(
                        actor = Actor(
                            actorName,
                            actorImage
                        ),
                        roleString = actorRole
                    )
                }

        val isTv =
            url.contains("/tv/")
                    || document.select(".cb-season")
                .isNotEmpty()

        if (isTv) {

            val episodes =
                ArrayList<Episode>()

            document.select(".cb-season")
                .forEach { seasonWrap ->

                    val seasonNum =
                        seasonWrap.selectFirst(".cb-season-number")
                            ?.text()
                            ?.replace(
                                "Season",
                                "",
                                ignoreCase = true
                            )
                            ?.trim()
                            ?.toIntOrNull()
                            ?: 1

                    seasonWrap.select(".cb-episode-item")
                        .forEach { ep ->

                            val epTitle =
                                ep.selectFirst(".cb-episode-title")
                                    ?.text()
                                    ?.trim()

                            val epNum =
                                Regex("""\d+""")
                                    .find(
                                        ep.selectFirst(".cb-episode-num")
                                            ?.text()
                                            ?: ""
                                    )
                                    ?.value
                                    ?.toIntOrNull()
                                    ?: 1

                            val epThumb =
                                fixUrlNull(
                                    ep.selectFirst(".cb-episode-thumb img")
                                        ?.attr("data-src")
                                        ?.takeIf { img -> img.isNotBlank() }
                                        ?: ep.selectFirst(".cb-episode-thumb img")
                                            ?.attr("src")
                                )

                            episodes.add(
                                newEpisode(
                                    LoadLinkData(
                                        imdbId,
                                        tmdbId,
                                        "tv",
                                        seasonNum,
                                        epNum
                                    ).toJson()
                                ) {

                                    this.name =
                                        epTitle

                                    this.season =
                                        seasonNum

                                    this.episode =
                                        epNum

                                    this.posterUrl =
                                        epThumb
                                }
                            )
                        }
                }

            return newTvSeriesLoadResponse(
                title,
                url,
                TvType.TvSeries,
                episodes
            ) {

                this.posterUrl =
                    poster

                this.backgroundPosterUrl =
                    backdrop

                this.plot =
                    plot

                this.year =
                    year

                this.tags =
                    tags

                this.actors =
                    actors

                trailer?.let {
                    addTrailer(
                        "https://www.youtube.com/watch?v=$it"
                    )
                }
            }

        } else {

            return newMovieLoadResponse(
                title,
                url,
                TvType.Movie,
                LoadLinkData(
                    imdbId,
                    tmdbId,
                    "movie"
                ).toJson()
            ) {

                this.posterUrl =
                    poster

                this.backgroundPosterUrl =
                    backdrop

                this.plot =
                    plot

                this.year =
                    year

                this.tags =
                    tags

                this.actors =
                    actors

                trailer?.let {
                    addTrailer(
                        "https://www.youtube.com/watch?v=$it"
                    )
                }
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