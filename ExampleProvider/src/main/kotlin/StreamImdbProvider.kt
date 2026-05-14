package com.example

import android.util.Log
import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addImdbId
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.USER_AGENT
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import org.json.JSONObject

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

    private val apiUrl = "https://api.themoviedb.org/3"

    companion object {
        private const val apiKey = "fceea78d0d9713c879f0cfeb0dbfb40b"
    }

    override val mainPage = mainPageOf(

        "trending/all/day?api_key=$apiKey" to "Trending",

        "movie/popular?api_key=$apiKey" to "Popular Movies",

        "tv/popular?api_key=$apiKey" to "Popular TV",

        "movie/top_rated?api_key=$apiKey" to "Top Rated Movies",

        "tv/top_rated?api_key=$apiKey" to "Top Rated TV",

        "movie/now_playing?api_key=$apiKey" to "Now Playing",

        "movie/upcoming?api_key=$apiKey" to "Upcoming Movies",

        "discover/movie?api_key=$apiKey&with_genres=28" to "Action Movies",

        "discover/movie?api_key=$apiKey&with_genres=35" to "Comedy Movies",

        "discover/movie?api_key=$apiKey&with_genres=27" to "Horror Movies",

        "discover/movie?api_key=$apiKey&with_genres=878" to "Sci-Fi Movies",

        "discover/tv?api_key=$apiKey&with_genres=16&with_origin_country=JP" to "Anime",

        "discover/movie?api_key=$apiKey&with_watch_providers=8&watch_region=US" to "Netflix",

        "discover/movie?api_key=$apiKey&with_watch_providers=9&watch_region=US" to "Amazon Prime"
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {

        val home = app.get(
            "$apiUrl/${request.data}&page=$page"
        ).parsedSafe<Results>()?.results?.mapNotNull {

            it.toSearchResponse()

        } ?: emptyList()

        return newHomePageResponse(
            request.name,
            home
        )
    }

    private fun Media.toSearchResponse(): SearchResponse? {

        val mediaType =
            mediaType ?: if (title != null) "movie" else "tv"

        val tvType =
            if (mediaType == "tv") TvType.TvSeries
            else TvType.Movie

        return if (tvType == TvType.TvSeries) {

            newTvSeriesSearchResponse(
                name ?: title ?: return null,
                LinkData(
                    id,
                    mediaType
                ).toJson(),
                tvType
            ) {

                this.posterUrl =
                    getImageUrl(posterPath)

                this.score =
                    Score.from10(voteAverage)
            }

        } else {

            newMovieSearchResponse(
                title ?: name ?: return null,
                LinkData(
                    id,
                    mediaType
                ).toJson(),
                tvType
            ) {

                this.posterUrl =
                    getImageUrl(posterPath)

                this.score =
                    Score.from10(voteAverage)
            }
        }
    }

    override suspend fun quickSearch(
        query: String
    ): List<SearchResponse>? {

        return search(query)
    }

    override suspend fun search(
        query: String
    ): List<SearchResponse> {

        return app.get(
            "$apiUrl/search/multi?api_key=$apiKey&query=$query"
        ).parsedSafe<Results>()?.results?.mapNotNull {

            if (it.mediaType == "person")
                return@mapNotNull null

            it.toSearchResponse()

        } ?: emptyList()
    }

    override suspend fun load(
        url: String
    ): LoadResponse? {

        val data =
            parseJson<LinkData>(url)

        val type =
            if (data.type == "tv")
                TvType.TvSeries
            else
                TvType.Movie

        val append =
            "credits,videos,external_ids,recommendations"

        val loadUrl =
            if (type == TvType.Movie) {

                "$apiUrl/movie/${data.id}?api_key=$apiKey&append_to_response=$append"

            } else {

                "$apiUrl/tv/${data.id}?api_key=$apiKey&append_to_response=$append"
            }

        val res = app.get(loadUrl)
            .parsedSafe<MediaDetail>()
            ?: return null

        val title =
            res.title ?: res.name ?: return null

        val poster =
            getImageUrl(res.posterPath)

        val backdrop =
            getImageUrl(res.backdropPath)

        val year =
            (res.releaseDate ?: res.firstAirDate)
                ?.substringBefore("-")
                ?.toIntOrNull()

        val genres =
            res.genres?.mapNotNull {
                it.name
            }

        val actors =
            res.credits?.cast?.mapNotNull {

                ActorData(
                    Actor(
                        it.name ?: return@mapNotNull null,
                        getImageUrl(it.profilePath)
                    ),
                    roleString = it.character
                )

            } ?: emptyList()

        val recommendations =
            res.recommendations?.results?.mapNotNull {
                it.toSearchResponse()
            }

        val trailers =
            res.videos?.results?.mapNotNull {

                it.key?.let { key ->
                    "https://www.youtube.com/watch?v=$key"
                }

            } ?: emptyList()

        if (type == TvType.TvSeries) {

            val episodes =
                mutableListOf<Episode>()

            res.seasons?.forEach { season ->

                val seasonNumber =
                    season.seasonNumber ?: return@forEach

                if (seasonNumber == 0)
                    return@forEach

                val seasonData = app.get(
                    "$apiUrl/tv/${data.id}/season/$seasonNumber?api_key=$apiKey"
                ).parsedSafe<SeasonDetail>()
                    ?: return@forEach

                seasonData.episodes?.forEach { ep ->

                    val epNum = ep.episodeNumber ?: return@forEach

                    episodes.add(
                        newEpisode(
                            "${res.externalIds?.imdbId ?: return@forEach}|tv|$seasonNumber|$epNum"
                        ) {

                            this.name = ep.name

                            this.season =
                                seasonNumber

                            this.episode =
                                epNum

                            this.posterUrl =
                                getImageUrl(ep.stillPath)

                            this.description =
                                ep.overview

                            this.score =
                                Score.from10(ep.voteAverage)
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

                this.posterUrl = poster

                this.backgroundPosterUrl =
                    backdrop

                this.year = year

                this.plot =
                    res.overview

                this.tags =
                    genres

                this.actors =
                    actors

                this.recommendations =
                    recommendations

                this.score =
                    Score.from10(res.voteAverage)

                addTrailer(trailers)

                addImdbId(
                    res.externalIds?.imdbId
                )
            }

        } else {

            return newMovieLoadResponse(
                title,
                url,
                TvType.Movie,
                "${res.externalIds?.imdbId ?: return null}|movie"
            ) {

                this.posterUrl = poster

                this.backgroundPosterUrl =
                    backdrop

                this.year = year

                this.plot =
                    res.overview

                this.tags =
                    genres

                this.actors =
                    actors

                this.recommendations =
                    recommendations

                this.score =
                    Score.from10(res.voteAverage)

                addTrailer(trailers)

                addImdbId(
                    res.externalIds?.imdbId
                )
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

    private fun getImageUrl(
        path: String?
    ): String? {

        if (path.isNullOrBlank())
            return null

        return "https://image.tmdb.org/t/p/w500$path"
    }

    data class LinkData(
        val id: Int? = null,
        val type: String? = null
    )

    data class EpisodeData(
        val id: Int? = null,
        val season: Int? = null,
        val episode: Int? = null
    )

    data class Results(

        @JsonProperty("results")
        val results: List<Media>? = arrayListOf()
    )

    data class Media(

        @JsonProperty("id")
        val id: Int? = null,

        @JsonProperty("title")
        val title: String? = null,

        @JsonProperty("name")
        val name: String? = null,

        @JsonProperty("media_type")
        val mediaType: String? = null,

        @JsonProperty("poster_path")
        val posterPath: String? = null,

        @JsonProperty("vote_average")
        val voteAverage: Double? = null
    )

    data class Genres(

        @JsonProperty("name")
        val name: String? = null
    )

    data class Cast(

        @JsonProperty("name")
        val name: String? = null,

        @JsonProperty("character")
        val character: String? = null,

        @JsonProperty("profile_path")
        val profilePath: String? = null
    )

    data class Credits(

        @JsonProperty("cast")
        val cast: List<Cast>? = arrayListOf()
    )

    data class Trailer(

        @JsonProperty("key")
        val key: String? = null
    )

    data class TrailerResults(

        @JsonProperty("results")
        val results: List<Trailer>? = arrayListOf()
    )

    data class ExternalIds(

        @JsonProperty("imdb_id")
        val imdbId: String? = null
    )

    data class Seasons(

        @JsonProperty("season_number")
        val seasonNumber: Int? = null
    )

    data class EpisodeItem(

        @JsonProperty("name")
        val name: String? = null,

        @JsonProperty("overview")
        val overview: String? = null,

        @JsonProperty("episode_number")
        val episodeNumber: Int? = null,

        @JsonProperty("still_path")
        val stillPath: String? = null,

        @JsonProperty("vote_average")
        val voteAverage: Double? = null
    )

    data class SeasonDetail(

        @JsonProperty("episodes")
        val episodes: List<EpisodeItem>? = arrayListOf()
    )

    data class RecommendationResults(

        @JsonProperty("results")
        val results: List<Media>? = arrayListOf()
    )

    data class MediaDetail(

        @JsonProperty("title")
        val title: String? = null,

        @JsonProperty("name")
        val name: String? = null,

        @JsonProperty("overview")
        val overview: String? = null,

        @JsonProperty("poster_path")
        val posterPath: String? = null,

        @JsonProperty("backdrop_path")
        val backdropPath: String? = null,

        @JsonProperty("release_date")
        val releaseDate: String? = null,

        @JsonProperty("first_air_date")
        val firstAirDate: String? = null,

        @JsonProperty("vote_average")
        val voteAverage: Double? = null,

        @JsonProperty("genres")
        val genres: List<Genres>? = arrayListOf(),

        @JsonProperty("credits")
        val credits: Credits? = null,

        @JsonProperty("videos")
        val videos: TrailerResults? = null,

        @JsonProperty("external_ids")
        val externalIds: ExternalIds? = null,

        @JsonProperty("recommendations")
        val recommendations: RecommendationResults? = null,

        @JsonProperty("seasons")
        val seasons: List<Seasons>? = arrayListOf()
    )
}
