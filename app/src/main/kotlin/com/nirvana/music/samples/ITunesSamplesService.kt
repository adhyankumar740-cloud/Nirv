/**
 * Nirvana Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Ported from Innertube's Samples feed. Uses Apple's public iTunes Search
 * API only for its `musicVideo` entity, which (unlike the plain "song"
 * entity) reliably returns a real ~30s *video* preview URL instead of just
 * an audio-only snippet. No API key required.
 */

package com.nirvana.music.samples

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object ITunesSamplesService {
    private val client =
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

    private const val BASE_URL = "https://itunes.apple.com/search"

    /** Varied terms mixed together so the feed has more than one page to swipe through. */
    val defaultTerms =
        listOf(
            "top hits", "pop hits", "hip hop", "dance music",
            "rock hits", "trending music", "viral songs", "rnb hits",
        )

    private suspend fun search(
        term: String,
        limit: Int = 25,
    ): List<SampleTrack> =
        withContext(Dispatchers.IO) {
            try {
                val encoded = URLEncoder.encode(term, "UTF-8")
                val url = "$BASE_URL?term=$encoded&media=musicVideo&entity=musicVideo&limit=$limit"
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use emptyList()
                    val body = response.body?.string() ?: return@use emptyList()
                    val json = JSONObject(body)
                    val results = json.optJSONArray("results") ?: return@use emptyList()
                    (0 until results.length()).mapNotNull { i ->
                        val item = results.getJSONObject(i)
                        val previewUrl =
                            item.optString("previewUrl").takeIf { it.isNotBlank() }
                                ?: return@mapNotNull null
                        SampleTrack(
                            id = item.optLong("trackId"),
                            title = item.optString("trackName", "Unknown"),
                            artist = item.optString("artistName", "Unknown Artist"),
                            album = item.optString("collectionName", ""),
                            genre = item.optString("primaryGenreName", "Music"),
                            previewUrl = previewUrl,
                            thumbnailUrl =
                                item.optString("artworkUrl100").takeIf { it.isNotBlank() }
                                    ?.replace("100x100", "600x600"),
                        )
                    }
                }
            } catch (e: Exception) {
                emptyList()
            }
        }

    /** Fetches several terms in parallel and merges/dedupes into one swipeable feed. */
    suspend fun getSamplesFeed(terms: List<String> = defaultTerms): List<SampleTrack> =
        coroutineScope {
            terms
                .map { term -> async { search(term) } }
                .awaitAll()
                .flatten()
                .distinctBy { it.id }
                .shuffled()
        }
}
