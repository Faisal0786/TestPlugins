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

    var providerVersion = 242

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
                    ?.takeIf { it.isNotBlank() }
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

    override suspend fun search(
        query: String
    ): List<SearchResponse> {

        val document =
            app.get(
                "$mainUrl/search?q=${query.replace(" ", "+")}",
                headers = stealthHeaders
            ).document

        return document.select("div.cb-card")
            .mapNotNull {
                toSearchResult(it)
            }
    }

    override suspend fun load(
        url: String
    ): LoadResponse? {

        val document =
            app.get(
                url,
                headers = stealthHeaders
            ).document

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
            document.selectFirst("#cbBannerBg")
                ?.attr("style")
                ?.substringAfter("url('")
                ?.substringBefore("')")

        val plot =
            document.selectFirst("#cbPlot")
                ?.text()
                ?.trim()

        // IMPORTANT FOR EXTRACTOR
        val imdbId =
            Regex("""tt\d+""")
                .find(document.html())
                ?.value
                ?: "null"

        val tmdbId =
            document.html()
                .substringAfter("tmdb:")
                .substringBefore(",")
                .filter { it.isDigit() }
                .ifBlank { "null" }

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

            document.select(".cb-episode-item")
                .forEach { ep ->

                    val epHref =
                        fixUrlNull(
                            ep.attr("href")
                        ) ?: return@forEach

                    val epTitle =
                        ep.selectFirst(".cb-episode-title")
                            ?.text()
                            ?.trim()

                    val epNum =
                        ep.selectFirst(".cb-episode-num")
                            ?.text()
                            ?.toIntOrNull()

                    val seasonNum =
                        Regex("""/season/(\d+)""")
                            .find(epHref)
                            ?.groupValues
                            ?.getOrNull(1)
                            ?.toIntOrNull()
                            ?: 1

                    val epThumb =
                        fixUrlNull(
                            ep.selectFirst(".cb-episode-thumb img")
                                ?.attr("data-src")
                                ?: ep.selectFirst(".cb-episode-thumb img")
                                    ?.attr("src")
                        )

                    episodes.add(
                        newEpisode(
                            "$imdbId|$tmdbId|tv|$seasonNum|$epNum"
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
            }

        } else {

            return newMovieLoadResponse(
                title,
                url,
                TvType.Movie,
                "$imdbId|$tmdbId|movie"
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