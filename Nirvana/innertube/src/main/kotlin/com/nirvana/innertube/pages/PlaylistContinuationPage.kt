package com.nirvana.innertube.pages

import com.nirvana.innertube.models.SongItem

data class PlaylistContinuationPage(
    val songs: List<SongItem>,
    val continuation: String?,
)
