package com.nirvana.innertube.pages

import com.nirvana.innertube.models.YTItem

data class LibraryContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)
