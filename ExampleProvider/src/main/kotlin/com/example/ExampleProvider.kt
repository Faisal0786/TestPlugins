package com.example

import android.util.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element
import org.json.JSONObject
import java.net.URLEncoder

class ExampleProvider : MainAPI() {

    override var mainUrl = "https://streamimdb.ru"
    override var name = "StreamIMDB"
    override val hasMainPage = true
    override var lang = "en"
    override val hasQuickSearch = false

    private val tmdbApi = "fceea78d0d9713c879f0cfeb0dbfb40b"

    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries
    )

    override val mainPage = mainPageOf(

        "trending" to "Trending",

        "popular_movies" to "Popular Movies",
        "top_movies" to "Top Rated Movies",
        "now_playing" to "Now Playing",
        "upcoming" to "Upcoming Movies",

        "popular_tv" to "Popular TV",
        "top_tv" to "Top Rated TV",
        "airing_today" to "Airing Today",
        "on_the_air" to "On The Air",

        "action" to "Action Movies",
        "comedy" to "Comedy Movies",
        "horror" to "Horror Movies",
        "scifi" to "Sci-Fi Movies",
        "thriller" to "Thriller Movies",
        "crime" to "Crime Movies",
        "romance" to "Romance Movies",
        "adventure" to "Adventure Movies",
        "animation" to "Animation Movies",

        "anime" to "Anime",

        "netflix" to "Netflix",
        "prime" to "Amazon Prime",
        "disney" to "Disney+",
        "appletv" to "Apple TV+",
        "hbo" to "HBO Max",
        "hulu" to "Hulu",
        "paramount" to "Paramount+",
        "hotstar" to "Hotstar",
        "sonyliv" to "SonyLIV",
        "zee5" to "ZEE5",
        "crunchyroll" to "Crunchyroll",
        "pixar" to "Pixar"
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {

        val endpoint = when (request.data) {

            "trending" ->
                "trending/all/day"

            "popular_movies" ->
                "movie/popular"

            "top_movies" ->
                "movie/top_rated"

            "now_playing" ->
                "movie/now_playing"

            "upcoming" ->
                "movie/upcoming"

            "popular_tv" ->
                "tv/popular"

            "top_tv" ->
                "tv/top_rated"

            "airing_today" ->
                "tv/airing_today"

            "on_the_air" ->
                "tv/on_the_air"

            "action" ->
                "discover/movie?with_genres=28"

            "comedy" ->
                "discover/movie?with_genres=35"

            "horror" ->
                "discover/movie?with_genres=27"

            "scifi" ->
                "discover/movie?with_genres=878"

            "thriller" ->
                "discover/movie?with_genres=53"

            "crime" ->
                "discover/movie?with_genres=80"

            "romance" ->
                "discover/movie?with_genres=10749"

            "adventure" ->
                "discover/movie?with_genres=12"

            "animation" ->
                "discover/movie?with_genres=16"

            "anime" ->
                "discover/tv?with_genres=16&with_origin_country=JP"

            "netflix" ->
                "discover/movie?with_watch_providers=8&watch_region=US"

            "prime" ->
                "discover/movie?with_watch_providers=9&watch_region=US"

            "disney" ->
                "discover/movie?with_watch_providers=337&watch_region=US"

            "appletv" ->
                "discover/movie?with_watch_providers=350&watch_region=US"

            "hbo" ->
                "discover/movie?with_watch_providers=384&watch_region=US"

            "hulu" ->
                "discover/movie?with_watch_providers=15&watch_region=US"

            "paramount" ->
                "discover/movie?with_watch_providers=531&watch_region=US"

            "hotstar" ->
                "discover/movie?with_watch_providers=39&watch_region=IN"

            "sonyliv" ->
                "discover/movie?with_watch_providers=237&watch_region=IN"

            "zee5" ->
                "discover/movie?with_watch_providers=2326&watch_region=IN"

            "crunchyroll" ->
                "discover/tv?with_watch_providers=283&watch_region=US"

            "pixar" ->
                "discover/movie?with_companies=3"

            else ->
                "trending/all/day"
        }

        val separator =
            if (endpoint.contains("?")) "&"
            else "?"

        val json = app.get(
            "https://api.themoviedb.org/3/$endpoint${separator}api_key=$tmdbApi"
        ).text

        val results = JSONObject(json)
            .getJSONArray("results")

        val home = mutableListOf<SearchResponse>()

        for (i in 0 until results.length()) {

            val item = results.getJSONObject(i)

            val mediaType =
                if (
                    item.optString("media_type") == "tv" ||
                    item.has("name")
                ) {
                    "tv"
                } else {
                    "movie"
                }

            val title =
                if (mediaType == "tv") {
                    item.optString("name")
                } else {
                    item.optString("title")
                }

            val posterPath =
                item.optString("poster_path")

            if (posterPath.isNullOrBlank())
                continue

            val poster =
                "https://image.tmdb.org/t/p/w500$posterPath"

            val rating =
                item.optDouble("vote_average")
                    .times(10)
                    .toInt()

            val year =
                if (mediaType == "tv") {
                    item.optString("first_air_date")
                } else {
                    item.optString("release_date")
                }

            val parsedYear =
                year.substringBefore("-")
                    .toIntOrNull()

            val searchQuery = URLEncoder.encode(
                title,
                "UTF-8"
            )

            val searchDocument = app.get(
                "$mainUrl/search?q=$searchQuery",
                headers = mapOf(
                    "User-Agent" to USER_AGENT
                )
            ).document

            val matchedCard = searchDocument
                .select(".cb-card")
                .firstOrNull()

            val realUrl = matchedCard
                ?.selectFirst("a")
                ?.attr("href")
                ?.let { fixUrlNull(it) }
                ?: continue

            val response =
                if (mediaType == "tv") {

                    newTvSeriesSearchResponse(
                        title,
                        realUrl,
                        TvType.TvSeries
                    ) {
                        this.posterUrl = poster
                        this.rating = rating
                        this.year = parsedYear
                    }

                } else {

                    newMovieSearchResponse(
                        title,
                        realUrl,
                        TvType.Movie
                    ) {
                        this.posterUrl = poster
                        this.rating = rating
                        this.year = parsedYear
                    }
                }

            home.add(response)
        }

        return newHomePageResponse(
            request.name,
            home
        )
    }

    private fun Element.toSearchResult(): SearchResponse? {

        val anchor = selectFirst("a")
        val image = selectFirst("img")

        val title = image
            ?.attr("alt")
            ?.trim()
            ?: return null

        val href = fixUrlNull(
            anchor?.attr("href")
        ) ?: return null

        val poster = fixUrlNull(
            image.attr("src")
        )

        return if (href.contains("/tv/")) {

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

    override suspend fun search(
        query: String
    ): List<SearchResponse> {

        val document = app.get(
            "$mainUrl/search?q=${URLEncoder.encode(query, "UTF-8")}",
            headers = mapOf(
                "User-Agent" to USER_AGENT
            )
        ).document

        val results = document
            .select(".cb-card")
            .mapNotNull {
                it.toSearchResult()
            }

        Log.d(
            "StreamIMDB",
            "SEARCH RESULTS = ${results.size}"
        )

        return results
    }

    override suspend fun load(
        url: String
    ): LoadResponse {

        val document = app.get(
            fixUrl(url),
            headers = mapOf(
                "User-Agent" to USER_AGENT
            )
        ).document

        val rawTitle = document
            .selectFirst("meta[property=og:title]")
            ?.attr("content")
            ?: "No Title"

        val title = rawTitle
            .replace("Watch ", "")
            .substringBefore(" Online")
            .trim()

        val description = document
            .selectFirst("meta[property=og:description]")
            ?.attr("content")

        val sitePoster = fixUrlNull(
            document.selectFirst(
                "meta[property=og:image]"
            )?.attr("content")
        )

        val year = Regex("""\((\d{4})\)""")
            .find(title)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()

        var tmdbPoster: String? = sitePoster
        var tmdbBackdrop: String? = null
        var tmdbYear: Int? = year
        var tmdbActors: List<ActorData>? = null

        try {

            val cleanTitle = title
                .substringBefore("(")
                .trim()

            val encodedTitle = URLEncoder.encode(
                cleanTitle,
                "UTF-8"
            )

            val tmdbSearch = app.get(
                "https://api.themoviedb.org/3/search/movie?api_key=$tmdbApi&query=$encodedTitle"
            ).text

            val tmdbJson = JSONObject(
                tmdbSearch
            )

            val results = tmdbJson
                .getJSONArray("results")

            if (results.length() > 0) {

                val movie = results
                    .getJSONObject(0)

                val movieId = movie
                    .getInt("id")

                if (!movie.isNull("poster_path")) {

                    tmdbPoster =
                        "https://image.tmdb.org/t/p/w500" +
                            movie.getString("poster_path")
                }

                if (!movie.isNull("backdrop_path")) {

                    tmdbBackdrop =
                        "https://image.tmdb.org/t/p/original" +
                            movie.getString("backdrop_path")
                }

                tmdbYear =
                    movie.getString("release_date")
                        .substringBefore("-")
                        .toIntOrNull()

                val detailsResponse = app.get(
                    "https://api.themoviedb.org/3/movie/$movieId?api_key=$tmdbApi&append_to_response=credits"
                ).text

                val detailsJson = JSONObject(
                    detailsResponse
                )

                val castArray = detailsJson
                    .getJSONObject("credits")
                    .getJSONArray("cast")

                val actorList =
                    mutableListOf<ActorData>()

                for (
                    i in 0 until minOf(
                        10,
                        castArray.length()
                    )
                ) {

                    val actor = castArray
                        .getJSONObject(i)

                    actorList.add(
                        ActorData(
                            Actor(
                                actor.getString("name")
                            )
                        )
                    )
                }

                tmdbActors = actorList
            }

        } catch (e: Exception) {

            Log.e(
                "StreamIMDB",
                "TMDB ERROR: ${e.message}"
            )
        }

        return if (url.contains("/tv/")) {

            val seasons = mutableListOf<SeasonData>()

            try {

                val cleanTitle = title
                    .substringBefore("(")
                    .trim()

                val encodedTitle = URLEncoder.encode(
                    cleanTitle,
                    "UTF-8"
                )

                val tvSearch = app.get(
                    "https://api.themoviedb.org/3/search/tv?api_key=$tmdbApi&query=$encodedTitle"
                ).text

                val tvJson = JSONObject(tvSearch)

                val tvResults = tvJson
                    .getJSONArray("results")

                if (tvResults.length() > 0) {

                    val tv = tvResults
                        .getJSONObject(0)

                    val tvId = tv
                        .getInt("id")

                    val tvDetails = JSONObject(
                        app.get(
                            "https://api.themoviedb.org/3/tv/$tvId?api_key=$tmdbApi&append_to_response=credits"
                        ).text
                    )

                    val seasonCount =
                        tvDetails.optInt("number_of_seasons")

                    for (seasonNumber in 1..seasonCount) {

                        try {

                            val seasonJson = JSONObject(
                                app.get(
                                    "https://api.themoviedb.org/3/tv/$tvId/season/$seasonNumber?api_key=$tmdbApi"
                                ).text
                            )

                            val episodesArray =
                                seasonJson.getJSONArray("episodes")

                            val episodeList =
                                mutableListOf<Episode>()

                            for (j in 0 until episodesArray.length()) {

                                val ep =
                                    episodesArray.getJSONObject(j)

                                val epName =
                                    ep.optString("name")

                                val epNumber =
                                    ep.optInt("episode_number")

                                val epPoster =
                                    ep.optString("still_path")

                                val posterUrl =
                                    if (epPoster.isNotBlank()) {
                                        "https://image.tmdb.org/t/p/w500$epPoster"
                                    } else {
                                        null
                                    }

                                val episodeData =
                                    "$url|$seasonNumber|$epNumber"

                                episodeList.add(
                                    Episode(
                                        episodeData,
                                        epName
                                    ) {
                                        this.season = seasonNumber
                                        this.episode = epNumber
                                        this.posterUrl = posterUrl
                                    }
                                )
                            }

                            seasons.add(
                                SeasonData(
                                    seasonNumber,
                                    episodeList
                                )
                            )

                        } catch (_: Exception) {
                        }
                    }
                }

            } catch (e: Exception) {

                Log.e(
                    "StreamIMDB",
                    "TV ERROR: ${e.message}"
                )
            }

            newTvSeriesLoadResponse(
                title,
                url,
                TvType.TvSeries,
                seasons
            ) {

                this.posterUrl =
                    tmdbPoster ?: sitePoster

                this.backgroundPosterUrl =
                    tmdbBackdrop

                this.plot =
                    description

                this.year =
                    tmdbYear ?: year

                if (!tmdbActors.isNullOrEmpty()) {
                    this.actors = tmdbActors
                }
            }

        } else {

            newMovieLoadResponse(
                title,
                url,
                TvType.Movie,
                url
            ) {

                this.posterUrl =
                    tmdbPoster ?: sitePoster

                this.backgroundPosterUrl =
                    tmdbBackdrop

                this.plot =
                    description

                this.year =
                    tmdbYear ?: year

                if (!tmdbActors.isNullOrEmpty()) {
                    this.actors = tmdbActors
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

        return true
    }
}