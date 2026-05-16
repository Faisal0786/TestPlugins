package com.example

import android.util.Log
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import org.json.JSONObject

object StreamImdbExtractor {

    suspend fun loadLinks(
        name: String,
        data: String,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {

        try {
            val parsed = parseJson<StreamImdbProvider.LoadLinkData>(data)

            val imdbId = parsed.imdbId
            val tmdbId = parsed.tmdbId
            val type = parsed.type

            // Strict Clean Variable Construction
            val hasValidImdb = !imdbId.isNullOrBlank() && imdbId != "null"
            val hasValidTmdb = !tmdbId.isNullOrBlank() && tmdbId != "null"

            // Agar dono me se koi ek bhi ID nahi milti to fetch attempt stop karo
            if (!hasValidImdb && !hasValidTmdb) {
                Log.e("StreamIMDB", "Extraction Aborted: Missing valid IMDB and TMDB IDs")
                return false
            }

            val apiUrl = if (type == "tv") {
                val season = parsed.season
                val episode = parsed.episode
                if (hasValidImdb) {
                    "https://streamdata.vaplayer.ru/api.php?imdb=$imdbId&type=tv&season=$season&episode=$episode"
                } else {
                    "https://streamdata.vaplayer.ru/api.php?tmdb=$tmdbId&type=tv&season=$season&episode=$episode"
                }
            } else {
                if (hasValidImdb) {
                    "https://streamdata.vaplayer.ru/api.php?imdb=$imdbId&type=movie"
                } else {
                    "https://streamdata.vaplayer.ru/api.php?tmdb=$tmdbId&type=movie"
                }
            }

            Log.d("StreamIMDB", "Target API URL Generated = $apiUrl")

            val responseObj = app.get(
                apiUrl,
                headers = mapOf(
                    "Referer" to "https://brightpathsignals.com/",
                    "Origin" to "https://brightpathsignals.com/"
                )
            )

            if (!responseObj.isSuccessful) return false
            val response = responseObj.text

            Log.d("StreamIMDB", "API RESPONSE = $response")

            val json = JSONObject(response)
            val dataObject = json.optJSONObject("data") ?: return false

            // Safe Array Check for Stream Links
            val streamUrls = dataObject.optJSONArray("stream_urls")
            if (streamUrls != null) {
                for (i in 0 until streamUrls.length()) {
                    val streamUrl = streamUrls.optString(i)
                    if (streamUrl.isNullOrBlank()) continue

                    // CineStream dynamic payload structure insertion pattern copy
                    callback.invoke(
                        newExtractorLink(
                            source = name,
                            name = "$name Server ${i + 1}",
                            url = streamUrl,
                            type = if (streamUrl.contains(".m3u8") || streamUrl.contains("hls")) ExtractorLinkType.M3U8 else INFER_TYPE
                        ) {
                            headers = mapOf(
                                "Referer" to "https://brightpathsignals.com/"
                            )
                            quality = Qualities.Unknown.value
                        }
                    )
                }
            }

            // Universal Node Subtitle Parsing Array Strategy (Both Layer Mappings)
            val subtitles = json.optJSONArray("default_subs") 
                ?: dataObject.optJSONArray("default_subs")
                ?: json.optJSONArray("subtitles")
                ?: dataObject.optJSONArray("subtitles")

            if (subtitles != null) {
                for (i in 0 until subtitles.length()) {
                    val sub = subtitles.optJSONObject(i) ?: continue
                    val subUrl = sub.optString("url")
                    val subLang = sub.optString("lang").ifBlank { sub.optString("label", "Unknown") }

                    if (!subUrl.isNullOrBlank()) {
                        // Safe protocol handling integration fallback
                        val cleanSubUrl = if (subUrl.startsWith("//")) "https:$subUrl" else subUrl
                        subtitleCallback.invoke(
                            SubtitleFile(
                                subLang,
                                cleanSubUrl
                            )
                        )
                    }
                }
            }

            return true

        } catch (e: Exception) {
            Log.e("StreamIMDB", "EXTRACTOR ERROR = ${e.message}")
            e.printStackTrace()
        }

        return false
    }
}
