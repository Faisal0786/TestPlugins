package com.lagradost

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class StreamIMDB : MainAPI() { 
    override var mainUrl = "https://streamimdb.ru"
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
        return listOf() // Abhi ke liye khali, pehle build test karte hain
    }
}
