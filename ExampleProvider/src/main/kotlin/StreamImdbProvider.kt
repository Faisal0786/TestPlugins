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

    private val stealthHeaders = mapOf(
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
        "Referer" to "$mainUrl/",
        "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8"
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
        val url = if (page == 1) request.data else "${request.data}?page=$page"
        val document = app.get(url, headers = stealthHeaders).document

        val home = document.select("div.cb-card").mapNotNull { card ->
            val href = fixUrlNull(card.selectFirst("a")?.attr("href")) ?: return@mapNotNull null
            val title = card.selectFirst(".cb-card-title")?.text()?.trim() ?: return@mapNotNull null
            
            val imgElement = card.selectFirst("img")
            val poster = fixUrlNull(
                imgElement?.attr("data-src")?.takeIf { it.isNotEmpty() } 
                ?: imgElement?.attr("src")
            )

            val meta = card.selectFirst(".cb-card-meta")?.text()?.lowercase()
            val isTv = meta?.contains("tv") == true || href.contains("/tv/")

            if (isTv) {
                newTvSeriesSearchResponse(title, href, TvType.TvSeries) { this.posterUrl = poster }
            } else {
                newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = poster }
            }
        }
        return newHomePageResponse(request.name, home)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("$mainUrl/search?q=${query.replace(" ", "+")}", headers = stealthHeaders).document
        return document.select("div.cb-card").mapNotNull { card ->
            val href = fixUrlNull(card.selectFirst("a")?.attr("href")) ?: return@mapNotNull null
            val title = card.selectFirst(".cb-card-title")?.text()?.trim() ?: return@mapNotNull null
            
            val imgElement = card.selectFirst("img")
            val poster = fixUrlNull(
                imgElement?.attr("data-src")?.takeIf { it.isNotEmpty() } 
                ?: imgElement?.attr("src")
            )
            
            val isTv = href.contains("/tv/")

            if (isTv) {
                newTvSeriesSearchResponse(title, href, TvType.TvSeries) { this.posterUrl = poster }
            } else {
                newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = poster }
            }
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val res = app.get(url, headers = stealthHeaders)
        val document = res.document

        val title = document.selectFirst(".cb-detail-title-logo")?.attr("alt")
                    ?: document.selectFirst(".cb-detail-title")?.text()
                    ?: document.selectFirst("h1")?.text()
                    ?: "Unknown"

        val poster = document.selectFirst("meta[property=og:image]")?.attr("content")
        
        // 1. Backdrop Fix: Background image handle karne ke liye
        val backdrop = document.selectFirst(".cb-detail-banner-bg")?.attr("style")
            ?.substringAfter("background-image:url('")?.substringBefore("')")

        val plot = document.selectFirst("#cbPlot")?.text()?.trim()
        val year = document.select(".cb-meta-plain").firstOrNull()?.text()?.toIntOrNull()
        val tags = document.select(".cb-meta-plain").map { it.text().trim() }

        // 2. Trailer Fix: YouTube iframe se link nikalna
        val trailer = document.selectFirst("#cbBgTrailer")?.attr("src")
            ?.let { "https://www.youtube.com/watch?v=" + it.substringAfter("/embed/").substringBefore("?") }

        // 3. Cast Fix: Actors ka naam aur image
        val actors = document.select(".cb-cast-item").mapNotNull { 
            val name = it.selectFirst(".cb-cast-name")?.text() ?: return@mapNotNull null
            val role = it.selectFirst(".cb-cast-role")?.text()
            val image = it.selectFirst("img")?.attr("data-src") ?: it.selectFirst("img")?.attr("src")
            ActorData(Actor(name, image), role)
        }

        val isTv = url.contains("/tv/") || document.select(".cb-season").isNotEmpty()

        if (isTv) {
            val episodes = ArrayList<Episode>()
            document.select(".cb-season").forEach { seasonWrap ->
                val seasonNum = seasonWrap.selectFirst(".cb-season-number")?.text()
                    ?.replace("Season", "", ignoreCase = true)?.trim()?.toIntOrNull() ?: 1
                    
                seasonWrap.select(".cb-episode-item").forEach { ep ->
                    val epHref = fixUrlNull(ep.attr("href")) ?: return@forEach
                    val epTitle = ep.selectFirst(".cb-episode-title")?.text()?.trim()
                    val epNum = ep.selectFirst(".cb-episode-num")?.text()?.toIntOrNull()
                    
                    // 4. Episode Thumbnail Fix
                    val epThumb = ep.selectFirst("img")?.attr("data-src") ?: ep.selectFirst("img")?.attr("src")

                    episodes.add(newEpisode(epHref) {
                        this.name = epTitle
                        this.season = seasonNum
                        this.episode = epNum
                        this.posterUrl = epThumb
                    })
                }
            }

            return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = poster
                this.backgroundPosterUrl = backdrop
                this.plot = plot
                this.year = year
                this.tags = tags
                this.actors = actors
                this.trailerUrl = trailer
            }
        } else {
            return newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl = poster
                this.backgroundPosterUrl = backdrop
                this.plot = plot
                this.year = year
                this.tags = tags
                this.actors = actors
                this.trailerUrl = trailer
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        return false
    }
}
