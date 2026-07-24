/**
 * Nirvana Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Ported from Innertube's SamplesViewModel, adapted to search/play full
 * songs through Nirvana's own YouTube Music (innertube) backend instead of
 * a separate music player stack.
 */

package com.nirvana.music.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nirvana.innertube.YouTube
import com.nirvana.innertube.models.SongItem
import com.nirvana.music.samples.ITunesSamplesService
import com.nirvana.music.samples.SampleTrack
import com.nirvana.music.samples.SamplesPlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SamplesViewModel
    @Inject
    constructor() : ViewModel() {
        val playerManager = SamplesPlayerManager()

        private val _samples = MutableStateFlow<List<SampleTrack>>(emptyList())
        val samples = _samples.asStateFlow()

        private val _isLoading = MutableStateFlow(false)
        val isLoading = _isLoading.asStateFlow()

        private val _currentIndex = MutableStateFlow(0)
        val currentIndex = _currentIndex.asStateFlow()

        // True while resolving the matching YouTube Music song for "Play Full Song".
        private val _isResolvingFullSong = MutableStateFlow(false)
        val isResolvingFullSong = _isResolvingFullSong.asStateFlow()

        // Emitted once a full-song match is found; the screen consumes this to hand it
        // off to Nirvana's real player (LocalPlayerConnection.playQueue).
        private val _songToPlay = MutableStateFlow<SongItem?>(null)
        val songToPlay = _songToPlay.asStateFlow()

        // Session-only favorites (not persisted to Nirvana's main library database).
        private val favoriteIds = mutableSetOf<Long>()

        init {
            loadSamples()
        }

        fun loadSamples() {
            viewModelScope.launch {
                _isLoading.value = true
                val tracks = ITunesSamplesService.getSamplesFeed()
                _samples.value = tracks.map { it.copy(isFavorite = it.id in favoriteIds) }
                _isLoading.value = false
            }
        }

        fun onSwipe(newIndex: Int) {
            if (newIndex in _samples.value.indices) {
                _currentIndex.value = newIndex
            }
        }

        fun toggleFavorite(track: SampleTrack) {
            if (track.id in favoriteIds) favoriteIds.remove(track.id) else favoriteIds.add(track.id)
            _samples.value =
                _samples.value.map {
                    if (it.id == track.id) it.copy(isFavorite = !it.isFavorite) else it
                }
        }

        /**
         * "Play Full Song" - looks up the best-matching YouTube Music song for this
         * sample's title/artist. The match is published via [songToPlay]; the screen
         * observes it and hands it off to the app's real player.
         */
        fun playFullSong(track: SampleTrack) {
            viewModelScope.launch {
                _isResolvingFullSong.value = true
                playerManager.pause()
                val match =
                    runCatching {
                        YouTube
                            .search("${track.title} ${track.artist}", YouTube.SearchFilter.FILTER_SONG)
                            .getOrNull()
                            ?.items
                            ?.filterIsInstance<SongItem>()
                            ?.firstOrNull()
                    }.getOrNull()
                _isResolvingFullSong.value = false
                _songToPlay.value = match
            }
        }

        fun consumeSongToPlay() {
            _songToPlay.value = null
        }

        override fun onCleared() {
            super.onCleared()
            playerManager.pause()
        }
    }
