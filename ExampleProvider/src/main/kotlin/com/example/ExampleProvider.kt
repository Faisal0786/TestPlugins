package com.example

import com.lagradost.cloudstream3.*

class ExampleProvider : MainAPI() { 
    override var mainUrl = "https://streamimdb.ru"
    // App ke andar naam 'StreamIMDB' hi dikhega, chahe class ka naam ExampleProvider ho
    override var name = "StreamIMDB" 
    override val hasMainPage = true
    override var lang = "hi"
    override val hasQuickSearch = true

    override suspend fun getMainPage(page: Int, request: HomePageRequest): HomePageResponse {
        val list = listOf(
            HomePageList("Latest Movies", "$mainUrl/api.php?type=movies"),
            HomePageList("Latest Series", "$mainUrl/api.php?type=series")
        )
        return HomePageResponse(list)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        return listOf() // Abhi blank, bas build theek karne ke liye
    }

    override suspend fun load(url: String): LoadResponse? {
        return null
    }
}
