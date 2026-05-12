package com.example

import com.lagradost.cloudstream3.*

class ExampleProvider : MainAPI() { 
    override var mainUrl = "https://streamimdb.ru"
    override var name = "StreamIMDB" 
    override val hasMainPage = true
    override var lang = "hi"
    override val hasQuickSearch = true

    // 🚀 1. CATEGORIES (Ye CloudStream mein upar Tabs ban kar dikhengi)
    override val mainPage = mainPageOf(
        "$mainUrl/home" to "Home",
        "$mainUrl/movies" to "Movies",
        "$mainUrl/tv-shows" to "TV Series",
        "$mainUrl/category/action" to "Action",
        "$mainUrl/category/comedy" to "Comedy",
        "$mainUrl/category/horror" to "Horror",
        "$mainUrl/category/sci-fi-&-fantasy" to "Sci-Fi"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = request.data // Ye upar wali categories ka exact URL uthayega

        // 🕵️‍♂️ 2. STEALTH HEADERS (Website ko lagega hum asli Chrome browser hain)
        val stealthHeaders = mapOf(
            "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
            "Referer" to mainUrl,
            "Accept-Language" to "en-US,en;q=0.5"
        )

        // app.get ke andar humne 'stealthHeaders' pass kar diye
        val document = app.get(url, headers = stealthHeaders).document
        val homeList = ArrayList<HomePageList>()

        // 3. PARSING LOGIC (Home Page aur Category Page dono ke liye)
        val sections = document.select("section.cb-section")

        if (sections.isNotEmpty()) {
            // Agar Home page hai (Trending, Popular jahan alag-alag sections hote hain)
            sections.forEach { section ->
                // Section ka naam nikalo, aur extra "See all" text hata do
                val sectionName = section.selectFirst(".cb-section-title")?.text()?.replace("See all", "")?.trim() ?: request.name
                
                val movies = section.select("div.cb-card").mapNotNull { card ->
                    val title = card.selectFirst(".cb-card-title")?.text() ?: return@mapNotNull null
                    val link = card.selectFirst("a")?.attr("href") ?: return@mapNotNull null
                    val fullUrl = if (link.startsWith("/")) "$mainUrl$link" else link
                    val poster = card.selectFirst("img")?.attr("src")

                    newMovieSearchResponse(title, fullUrl, TvType.Movie) {
                        this.posterUrl = poster
                    }
                }

                if (movies.isNotEmpty()) {
                    homeList.add(HomePageList(sectionName, movies))
                }
            }
        } else {
            // Agar kisi Category par click kiya hai (jahan sirf grid hota hai)
            val movies = document.select("div.cb-card").mapNotNull { card ->
                val title = card.selectFirst(".cb-card-title")?.text() ?: return@mapNotNull null
                val link = card.selectFirst("a")?.attr("href") ?: return@mapNotNull null
                val fullUrl = if (link.startsWith("/")) "$mainUrl$link" else link
                val poster = card.selectFirst("img")?.attr("src")

                newMovieSearchResponse(title, fullUrl, TvType.Movie) {
                    this.posterUrl = poster
                }
            }

            if (movies.isNotEmpty()) {
                homeList.add(HomePageList(request.name, movies))
            }
        }

        return newHomePageResponse(homeList)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        return listOf()
    }

    override suspend fun load(url: String): LoadResponse? {
        return null
    }
}
