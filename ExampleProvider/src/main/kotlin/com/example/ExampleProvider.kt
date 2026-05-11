package com.example

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.APIHolder.capitalize

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
        val url = "$mainUrl/api.php?search=$query"
        val response = app.get(url).text
        // Abhi ke liye empty list bhej rahe hain taaki build pass ho jaye
        return listOf<SearchResponse>()
    }

    override suspend fun load(url: String): LoadResponse? {
        return null
    }
}
