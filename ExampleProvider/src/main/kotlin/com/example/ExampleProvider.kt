package com.example

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class ExampleProvider : MainAPI() {

    override var mainUrl = "https://example.com"
    override var name = "Example"
    override val hasMainPage = false
    override var lang = "en"

    override val supportedTypes = setOf(
        TvType.Movie
    )

    override suspend fun search(query: String): List<SearchResponse> {
        return emptyList()
    }

    override suspend fun load(url: String): LoadResponse? {
        return null
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        return false
    }
}
