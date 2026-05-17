package com.example

import android.util.Log
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import org.json.JSONObject

object StreamImdbExtractor {

    data class LoadLinkData(
        val imdbId: String? = null,
        val tmdbId: String? = null,
        val type: String,
        val season: Int? = null,
        val episode: Int? = null,
        val url: String? = null
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

            var imdbId =
                parsed.imdbId

            var tmdbId =
                parsed.tmdbId

            val type =
                parsed.type

            val pageUrl =
                parsed.url

            if (
                type == "tv" &&
                tmdbId.isNullOrBlank() &&
                !pageUrl.isNullOrBlank()
            ) {

                try {

                    val html =
                        app.get(pageUrl).text

                    val regex =
                        Regex(
                            """window\.__cbTvMeta\s*=\s*(\{.*?\});""",
                            RegexOption.DOT_MATCHES_ALL
                        )

                    val match =
                        regex.find(html)

                    if (match != null) {

                        val json =
                            JSONObject(match.groupValues[1])

                        tmdbId =
                            json.optString("id")

                        Log.d(
                            "StreamIMDB",
                            "EXTRACTED TMDB = $tmdbId"
                        )
                    }

                } catch (e: Exception) {

                    Log.e(
                        "StreamIMDB",
                        "TMDB EXTRACT ERROR = ${e.message}"
                    )
                }
            }

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
                JSONObject(response)

            if (!json.has("data")) {

                Log.e(
                    "StreamIMDB",
                    "NO DATA FOUND"
                )

                return false
            }

            val dataObject =
                json.getJSONObject("data")

            val streamUrls =
                dataObject.optJSONArray("stream_urls")

            if (streamUrls != null) {

                for (i in 0 until streamUrls.length()) {

                    val streamUrl =
                        streamUrls.optString(i)

                    if (streamUrl.isNotBlank()) {

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
                }
            }

            val subtitles =
                json.optJSONArray("default_subs")
                    ?: dataObject.optJSONArray("default_subs")

            if (subtitles != null) {

                for (i in 0 until subtitles.length()) {

                    val sub =
                        subtitles.optJSONObject(i)
                            ?: continue

                    val lang =
                        sub.optString("lang")
                            .ifBlank {
                                sub.optString("code")
                            }

                    val url =
                        sub.optString("url")

                    if (url.isNotBlank()) {

                        Log.d(
                            "StreamIMDB",
                            "SUBTITLE = $lang -> $url"
                        )

                        subtitleCallback.invoke(
                            SubtitleFile(
                                lang.ifBlank {
                                    "Unknown"
                                },
                                url
                            )
                        )
                    }
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