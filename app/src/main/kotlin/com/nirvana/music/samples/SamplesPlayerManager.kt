/**
 * Nirvana Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Ported from Innertube's SamplesPlayerManager.
 */

package com.nirvana.music.samples

import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tracks play/pause + buffering state for whichever ExoPlayer is currently
 * the "active" page in the Samples (Shorts-style) feed.
 *
 * Each page in SamplesScreen owns its own short-lived ExoPlayer instance -
 * this class doesn't create or hold a player of its own, it just exposes
 * whichever one is currently active as StateFlows so SamplesScreen /
 * SamplesViewModel can drive UI (play/pause icon, buffering spinner, the
 * "pause when leaving the tab" behavior) without reaching into Compose-owned
 * player instances directly.
 */
class SamplesPlayerManager {
    /** The ExoPlayer for whichever feed page is currently on-screen, if any. */
    var activePlayer: ExoPlayer? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    /** Called by the active page's Player.Listener/position-poll to publish its latest state. */
    fun reportActiveState(
        isPlaying: Boolean,
        isBuffering: Boolean,
    ) {
        _isPlaying.value = isPlaying
        _isBuffering.value = isBuffering
    }

    /** Tap-to-toggle on the active page. */
    fun togglePlayPause() {
        activePlayer?.let { it.playWhenReady = !it.playWhenReady }
    }

    /** Pauses the active player - called when navigating away from the Samples tab entirely. */
    fun pause() {
        activePlayer?.playWhenReady = false
        _isPlaying.value = false
    }
}
