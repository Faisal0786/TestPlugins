package com.example

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import org.jsoup.nodes.Element

class ExampleProvider : MainAPI() {
    override var mainUrl = "https://streamimdb.ru"
    override var name = "StreamIMDB"
    override val hasMainPage = true
    override var lang = "hi"
    override val hasQuickSearch = true
    
    // Search aur Filter ke liye types batana zaroori hai
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)

    // 1. STEALTH HEADERS - Website ko dhoka dene ke liye
    private val stealthHeaders = mapOf(
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
        "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8",
        "Referer" to "$mainUrl/",
        "Accept-Language" to "en-US,en;q=0.9"
    )

    // 2. CATEGORIES - App ke top par buttons ban jayenge
    override val mainPage = mainPageOf(
        "$mainUrl/home" to "Home",
        "$mainUrl/movies" to "Latest Movies",
        "$mainUrl/tv-shows" to "Latest TV Shows",
        "$mainUrl/category/action" to "Action",
        "$mainUrl/category/comedy" to "Comedy",
        "$mainUrl/category/horror" to "Horror",
        "$mainUrl/category/thriller" to "Thriller"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        // Request bhej rahe hain stealth headers ke saath
        val document = app.get(request.data, headers = stealthHeaders).document
        val homeList = ArrayList<HomePageList>()

        // Agar Home Page hai toh sections dhoondho (Trending, Popular etc.)
        val sections = document.select("section.cb-section")
        
        if (sections.isNotEmpty()) {
            sections.forEach { section ->
                val title = section.selectFirst(".cb-section-title")?.text()?.replace("See all", "")?.trim() ?: "Featured"
                val movies = section.select(".cb-card").mapNotNull { it.toSearchResult() }
                if (movies.isNotEmpty()) homeList.add(HomePageList(title, movies))
            }
        } else {
            // Agar Category page hai (Action/Comedy) toh seedha grid uthao
            val movies = document.select(".cb-card").mapNotNull { it.toSearchResult() }
            if (movies.isNotEmpty()) homeList.add(HomePageList(request.name, movies))
        }

        return newHomePageResponse(homeList)
    }

    // 3. SEARCH FUNCTION - Search bar ke liye
    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/search?q=${query.replace(" ", "+")}"
        val document = app.get(url, headers = stealthHeaders).document
        return document.select(".cb-card").mapNotNull { it.toSearchResult() }
    }

    // Ek common function card se data nikalne ke liye
    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst(".cb-card-title")?.text() ?: return null
        val href = this.selectFirst("a")?.attr("href") ?: return null
        // Lazy loading handle karne ke liye data-src bhi check karte hain
        val posterUrl = this.selectFirst("img")?.attr("data-src") 
            ?: this.selectFirst("img")?.attr("src")

        return newMovieSearchResponse(title, fixUrl(href), TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        // Filhal sirf home page aur search test karna hai
        return null
    }
}
