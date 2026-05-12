package com.example

import com.lagradost.cloudstream3.*

class ExampleProvider : MainAPI() { 
    override var mainUrl = "https://streamimdb.ru"
    // App ke andar naam 'StreamIMDB' hi dikhega, chahe class ka naam ExampleProvider ho
    override var name = "StreamIMDB" 
    override val hasMainPage = true
    override var lang = "hi"
    override val hasQuickSearch = true

        override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        // 1. Website ka HTML download kar rahe hain
        val document = app.get(mainUrl).document
        val homeList = ArrayList<HomePageList>()

        // 2. Har ek "section" dhoondh rahe hain jisme movies hain
        document.select("section.cb-row-section").forEach { section ->
            
            // Section ka naam nikalna (jaise "Trending Today", "Popular Now")
            val sectionName = section.selectFirst("h2.cb-section-title")?.text() ?: "More Movies"

            // 3. Us section ke andar saare "cb-card" (Movies) nikalna
            val movies = section.select("div.cb-card").mapNotNull { card ->
                
                // Title, Link, aur Image nikalna
                val title = card.selectFirst("h3.cb-card-title")?.text() ?: return@mapNotNull null
                
                // Link relative aata hai ("/movie/abc"), isliye uske aage mainUrl lagana zaroori hai
                val shortUrl = card.selectFirst("a")?.attr("href") ?: return@mapNotNull null
                val fullUrl = if (shortUrl.startsWith("/")) "$mainUrl$shortUrl" else shortUrl
                
                val poster = card.selectFirst("img")?.attr("src")

                // CloudStream ko movie ka data dena
                newMovieSearchResponse(title, fullUrl, TvType.Movie) {
                    this.posterUrl = poster
                }
            }

            // Agar section mein movies hain, toh usko list mein add kar do
            if (movies.isNotEmpty()) {
                homeList.add(HomePageList(sectionName, movies))
            }
        }

        return newHomePageResponse(homeList)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        return listOf() // Abhi blank, bas build theek karne ke liye
    }

    override suspend fun load(url: String): LoadResponse? {
        return null
    }
}
