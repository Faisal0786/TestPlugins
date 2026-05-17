package com.example

import android.util.Log
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.AppUtils.parseJson

object StreamImdbExtractor {

    data class LoadLinkData(
        val imdbId: String? = null,
        val tmdbId: String? = null,
        val type: String,
        val season: Int? = null,
        val episode: Int? = null
    )

    suspend fun loadLinks(
        name: String,
        data: String,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {

        try {

            Log.d("StreamIMDB", "RAW DATA = $data")

            val parsed =
                parseJson<LoadLinkData>(data)

            val imdbId =
                parsed.imdbId

            val tmdbId =
                parsed.tmdbId

            val type =
                parsed.type

            val apiUrl =
                if (type == "tv") {

                    val season =
                        parsed.season ?: 1

                    val episode =
                        parsed.episode ?: 1

                    if (!imdbId.isNullOrBlank()) {

                        "https://streamdata.vaplayer.ru/api.php?imdb=$imdbId&type=tv&season=$season&episode=$episode"

                    } else {

                        "https://streamdata.vaplayer.ru/api.php?tmdb=$tmdbId&type=tv&season=$season&episode=$episode"
                    }

                } else {

                    if (!imdbId.isNullOrBlank()) {

                        "https://streamdata.vaplayer.ru/api.php?imdb=$imdbId&type=movie"

                    } else {

                        "https://streamdata.vaplayer.ru/api.php?tmdb=$tmdbId&type=movie"
                    }
                }

            Log.d("StreamIMDB", "API URL = $apiUrl")

            val response =
                app.get(
                    apiUrl,
                    headers = mapOf(
                        "Referer" to "https://brightpathsignals.com/",
                        "Origin" to "https://brightpathsignals.com/"
                    )
                ).text

            Log.d("StreamIMDB", "API RESPONSE = $response")

            val json =
                org.json.JSONObject(response)

            val dataObject =
                json.getJSONObject("data")

            val streamUrls =
                dataObject.getJSONArray("stream_urls")

            for (i in 0 until streamUrls.length()) {

                val streamUrl =
                    streamUrls.getString(i)

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

                        quality =
                            Qualities.Unknown.value
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