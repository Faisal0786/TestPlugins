package com.sdmovies

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.api.SubtitleFile
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.api.MainAPI
import com.lagradost.cloudstream3.api.SearchResponse
import com.lagradost.cloudstream3.api.LoadResponse
import com.lagradost.cloudstream3.api.MovieSearchResponse
import com.lagradost.cloudstream3.api.MovieLoadResponse
import com.lagradost.cloudstream3.api.HomePageResponse
import com.lagradost.cloudstream3.api.HomePageList
import com.lagradost.cloudstream3.api.MainPageRequest
import com.lagradost.cloudstream3.api.TvType
import com.lagradost.cloudstream3.utils.Qualities
import org.jsoup.Jsoup

@CloudstreamPlugin
class SDmoviesProvider: MainAPI() {
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
                        MovieSearchResponse(
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
                    MovieSearchResponse(
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
            
            MovieLoadResponse(
                name = title,
                url = url,
                apiName = this.name,
                type = TvType.Movie,
                posterUrl = poster,
                plot = description,
                tags = emptyList()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    override suspend fun getVideoLinks(url: String): List<ExtractorLink> {
        return try {
            val doc = Jsoup.connect(url).get()
            val links = mutableListOf<ExtractorLink>()
            
            doc.select("a.download-link").forEach { element ->
                val linkUrl = element.attr("href")
                val quality = element.text()
                
                if (linkUrl.isNotEmpty()) {
                    links.add(
                        ExtractorLink(
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
            
            links
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
