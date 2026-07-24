/**
 * Nirvana Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Ported from Innertube's Samples (Reels/Shorts-style) feed.
 */

package com.nirvana.music.samples

/** A single card in the vertical Samples swipe feed. */
data class SampleTrack(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val genre: String,
    val previewUrl: String,
    val thumbnailUrl: String?,
    val isFavorite: Boolean = false,
)
