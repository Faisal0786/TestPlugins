override val mainPage = mainPageOf(
    "$mainUrl/movies" to "Movies"
)

override suspend fun getMainPage(
    page: Int,
    request: MainPageRequest
): HomePageResponse {

    val document = app.get(
        request.data,
        headers = mapOf(
            "User-Agent" to USER_AGENT
        )
    ).document

    val home = document.select(".cb-card").mapNotNull { element ->

        val aTag = element.selectFirst("a")
        val img = element.selectFirst("img")

        val title = img?.attr("alt")?.trim() ?: return@mapNotNull null

        val href = fixUrlNull(
            aTag?.attr("href")
        ) ?: return@mapNotNull null

        val poster = fixUrlNull(
            img.attr("src")
        )

        newMovieSearchResponse(
            title,
            href,
            TvType.Movie
        ) {
            this.posterUrl = poster
        }
    }

    Log.d("StreamIMDB", "HOME ITEMS = ${home.size}")

    return newHomePageResponse(
        request.name,
        home
    )
}
