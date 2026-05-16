package com.example

import android.util.Log
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.*
import org.json.JSONObject

object StreamImdbExtractor {

    suspend fun loadLinks(
        name: String,
        data: String,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {

        try {

            val parts = data.split('|')

            val imdbId = parts[0]
            val tmdbId = parts[1]
            val type = parts[2]

            val apiUrl =
                if (type == "tv") {

                    val season = parts[3]
                    val episode = parts[4]

                    if (imdbId != "null") {
                        "https://streamdata.vaplayer.ru/api.php?imdb=$imdbId&type=tv&season=$season&episode=$episode"
                    } else {
                        "https://streamdata.vaplayer.ru/api.php?tmdb=$tmdbId&type=tv&season=$season&episode=$episode"
                    }

                } else {

                    if (imdbId != "null") {
                        "https://streamdata.vaplayer.ru/api.php?imdb=$imdbId&type=movie"
                    } else {
                        "https://streamdata.vaplayer.ru/api.php?tmdb=$tmdbId&type=movie"
                    }
                }

            val response = app.get(
                apiUrl,
                headers = mapOf(
                    "Referer" to "https://brightpathsignals.com/",
                    "Origin" to "https://brightpathsignals.com/"
                )
            ).text

            Log.d("StreamIMDB", "API RESPONSE = $response")

            val json = JSONObject(response)

            val dataObject =
                json.getJSONObject("data")

            val streamUrls =
                dataObject.getJSONArray("stream_urls")

            for (i in 0 until streamUrls.length()) {

                val streamUrl =
                    streamUrls.getString(i)

                Log.d("StreamIMDB", "FOUND LINK = $streamUrl")

                callback.invoke(
                    newExtractorLink(
                        source = name,
                        name = "$name Server ${i + 1}",
                        url = streamUrl,
                        type = ExtractorLinkType.M3U8
                    ) {
                        headers = mapOf(
                            "Referer" to "https://brightpathsignals.com/"
                        )

                        quality = Qualities.Unknown.value
                    }
                )
            }

            val subtitles =
                json.optJSONArray("default_subs")
                    ?: dataObject.optJSONArray("default_subs")

            if (subtitles != null) {

                for (i in 0 until subtitles.length()) {

                    val sub =
                        subtitles.getJSONObject(i)

                    subtitleCallback.invoke(
                        SubtitleFile(
                            sub.optString("lang"),
                            sub.optString("url")
                        )
                    )
                }
            }

            return true

        } catch (e: Exception) {

            Log.e(
                "StreamIMDB",
                "EXTRACTOR ERROR = ${e.message}"
            )
        }

        return false
    }
}