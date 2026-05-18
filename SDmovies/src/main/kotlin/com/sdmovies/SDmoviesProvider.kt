package com.sdmovies

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.newMovieSearchResponse
import com.lagradost.cloudstream3.newMovieLoadResponse
import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.HomePageList
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.newExtractorLink
import org.jsoup.Jsoup

@CloudstreamPlugin
class SDmoviesProvider : MainAPI() {
    override val mainUrl = "https://sdmoviespoint.cyou"
    override val name = "SDmovies"
    override val hasMainPage = true

    override val mainPageUrlList = listOf(
        Pair("Latest", "$mainUrl/"),
        Pair("Movies", "$mainUrl/latest-movies"),
        Pair("Web Series", "$mainUrl/web-series")
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val list = mutableListOf<HomePageList>()

        mainPageUrlList.forEach { (name, url) ->
            try {
                val doc = Jsoup.connect(url).get()
                val items = doc.select("div.post-box").mapNotNull { element ->
                    val title = element.select("h2.post-title").text()
                    val link = element.select("a").attr("href")
                    val poster = element.select("img").attr("src")

                    if (title.isNotEmpty() && link.isNotEmpty()) {
                        newMovieSearchResponse(
                            name = title,
                            url = link,
                            apiName = this.name,
                            type = TvType.Movie,
                            posterUrl = poster
                        )
                    } else null
                }

                if (items.isNotEmpty()) {
                    list.add(HomePageList(name, items))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return HomePageResponse(list)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        return try {
            val doc = Jsoup.connect("$mainUrl/?s=$query").get()

            doc.select("div.post-box").mapNotNull { element ->
                val title = element.select("h2.post-title").text()
                val link = element.select("a").attr("href")
                val poster = element.select("img").attr("src")

                if (title.isNotEmpty() && link.isNotEmpty()) {
                    newMovieSearchResponse(
                        name = title,
                        url = link,
                        apiName = this.name,
                        type = TvType.Movie,
                        posterUrl = poster
                    )
                } else null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        return try {
            val doc = Jsoup.connect(url).get()

            val title = doc.select("h1.post-title").text()
            val poster = doc.select("img.post-image").attr("src")
            val description = doc.select("div.post-content p").text()

            newMovieLoadResponse(
                name = title,
                url = url,
                type = TvType.Movie,
                dataUrl = url
            ) {
                this.posterUrl = poster
                this.plot = description
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        return try {
            val doc = Jsoup.connect(data).get()

            doc.select("a.download-link").forEach { element ->
                val linkUrl = element.attr("href")
                val quality = element.text()

                if (linkUrl.isNotEmpty()) {
                    callback(
                        newExtractorLink(
                            source = this.name,
                            name = quality,
                            url = linkUrl,
                            referer = mainUrl,
                            quality = Qualities.Unknown.value,
                            isM3u8 = false
                        )
                    )
                }
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}