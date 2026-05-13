package com.example
//under development 
import android.util.Log
import com.lagradost.cloudstream3.*
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

        "$mainUrl/movies" to "Latest Movies",
        "$mainUrl/most-viewed" to "Trending",
        "$mainUrl/most-viewed-tv" to "Top TV",

        "$mainUrl/category/action" to "Action",
        "$mainUrl/category/action-&-adventure" to "Action & Adventure",
        "$mainUrl/category/adventure" to "Adventure",
        "$mainUrl/category/animation" to "Animation",
        "$mainUrl/category/comedy" to "Comedy",
        "$mainUrl/category/crime" to "Crime",
        "$mainUrl/category/documentary" to "Documentary",
        "$mainUrl/category/drama" to "Drama",
        "$mainUrl/category/family" to "Family",
        "$mainUrl/category/fantasy" to "Fantasy",
        "$mainUrl/category/history" to "History",
        "$mainUrl/category/horror" to "Horror",
        "$mainUrl/category/kids" to "Kids",
        "$mainUrl/category/music" to "Music",
        "$mainUrl/category/mystery" to "Mystery",
        "$mainUrl/category/news" to "News",
        "$mainUrl/category/reality" to "Reality",
        "$mainUrl/category/romance" to "Romance",
        "$mainUrl/category/sci-fi-&-fantasy" to "Sci-Fi & Fantasy",
        "$mainUrl/category/science-fiction" to "Science Fiction",
        "$mainUrl/category/soap" to "Soap",
        "$mainUrl/category/talk" to "Talk",
        "$mainUrl/category/thriller" to "Thriller",
        "$mainUrl/category/tv-movie" to "TV Movie",
        "$mainUrl/category/war" to "War",
        "$mainUrl/category/war-&-politics" to "War & Politics",
        "$mainUrl/category/western" to "Western"
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

        val home = document
            .select(".cb-card")
            .mapNotNull {
                it.toSearchResult()
            }

        Log.d(
            "StreamIMDB",
            "HOME ITEMS = ${home.size}"
        )

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

        return newMovieSearchResponse(
            title,
            href,
            TvType.Movie
        ) {
            this.posterUrl = poster
        }
    }

    override suspend fun search(
        query: String
    ): List<SearchResponse> {

        val document = app.get(
            "$mainUrl/search?q=$query",
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
        var tmdbScore: Int? = null
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

                tmdbScore =
                    (movie.getDouble("vote_average") * 10)
                        .toInt()

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

        return newMovieLoadResponse(
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

            if (tmdbScore != null) {

                this.rating = tmdbScore
            }

            if (!tmdbActors.isNullOrEmpty()) {

                this.actors =
                    tmdbActors
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
