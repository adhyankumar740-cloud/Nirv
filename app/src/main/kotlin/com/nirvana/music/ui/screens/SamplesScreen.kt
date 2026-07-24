/**
 * Nirvana Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Ported from Innertube's Samples screen: a vertical, Shorts/Reels-style
 * swipe feed of short music-video previews (iTunes `musicVideo` entity),
 * with a "Play Full Song" action that hands off to Nirvana's own
 * YouTube Music player.
 */

package com.nirvana.music.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.nirvana.music.LocalNavController
import com.nirvana.music.LocalPlayerConnection
import com.nirvana.music.R
import com.nirvana.music.models.toMediaMetadata
import com.nirvana.music.playback.queues.YouTubeQueue
import com.nirvana.music.samples.SampleTrack
import com.nirvana.music.samples.SamplesPlayerManager
import com.nirvana.music.viewmodels.SamplesViewModel
import kotlinx.coroutines.delay

@Composable
fun SamplesScreen(
    viewModel: SamplesViewModel = hiltViewModel(),
) {
    val navController = LocalNavController.current
    val playerConnection = LocalPlayerConnection.current

    val samples by viewModel.samples.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentIndex by viewModel.currentIndex.collectAsState()
    val isPlaying by viewModel.playerManager.isPlaying.collectAsState()
    val isBuffering by viewModel.playerManager.isBuffering.collectAsState()
    val isResolvingFullSong by viewModel.isResolvingFullSong.collectAsState()
    val songToPlay by viewModel.songToPlay.collectAsState()

    BackHandler { navController.popBackStack() }

    // Once a "Play Full Song" match is resolved, hand it off to Nirvana's own player.
    LaunchedEffect(songToPlay) {
        songToPlay?.let { song ->
            playerConnection?.playQueue(YouTubeQueue.radio(song.toMediaMetadata()))
            viewModel.consumeSongToPlay()
        }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.playerManager.pause() }
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black),
    ) {
        if (isLoading && samples.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (samples.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Couldn't load samples right now. Pull down to try again later.", color = Color.Gray)
            }
        } else {
            val pagerState = rememberPagerState(initialPage = currentIndex, pageCount = { samples.size })

            LaunchedEffect(pagerState.currentPage) {
                viewModel.onSwipe(pagerState.currentPage)
            }

            // beyondViewportPageCount = 1 keeps the next page composed (and its ExoPlayer
            // prepared/buffered ahead of time) so swiping to it is instant.
            VerticalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                val track = samples[page]
                val isActivePage = page == pagerState.currentPage

                SampleFeedCard(
                    track = track,
                    isActivePage = isActivePage,
                    isPlaying = isPlaying && isActivePage,
                    isBuffering = isBuffering && isActivePage,
                    isResolvingFullSong = isResolvingFullSong,
                    playerManager = viewModel.playerManager,
                    onFavoriteClick = { viewModel.toggleFavorite(track) },
                    onPlayFullSongClick = { viewModel.playFullSong(track) },
                )
            }
        }

        // Back button, top-left, over the feed.
        IconButton(
            onClick = { navController.popBackStack() },
            modifier =
                Modifier
                    .padding(top = 24.dp, start = 8.dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f)),
        ) {
            Icon(
                painter = painterResource(R.drawable.arrow_back),
                contentDescription = "Back",
                tint = Color.White,
            )
        }
    }
}

/**
 * Full-screen 9:16 video surface (Reels/Shorts style) for one feed page.
 * Each page owns its own ExoPlayer instance - the active page plays; kept-alive
 * neighbor pages stay paused but fully prepared/buffered, so swiping to them is
 * instant. PlayerView with useController=false + pointerInteropFilter{false} is
 * what lets the outer VerticalPager receive the swipe gesture instead of the
 * embedded native view swallowing it.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun SampleVideoPage(
    track: SampleTrack,
    isActivePage: Boolean,
    playerManager: SamplesPlayerManager,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val exoPlayer =
        remember(track.id) {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(track.previewUrl))
                repeatMode = Player.REPEAT_MODE_ONE
                volume = 1f
                prepare()
            }
        }

    DisposableEffect(track.id) {
        onDispose { exoPlayer.release() }
    }

    LaunchedEffect(isActivePage, exoPlayer) {
        if (isActivePage) {
            playerManager.activePlayer = exoPlayer
            exoPlayer.playWhenReady = true
        } else {
            exoPlayer.playWhenReady = false
        }
    }

    DisposableEffect(exoPlayer) {
        val listener =
            object : Player.Listener {
                override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                    if (playerManager.activePlayer === exoPlayer) {
                        playerManager.reportActiveState(
                            isPlaying = isPlayingNow,
                            isBuffering = exoPlayer.playbackState == Player.STATE_BUFFERING,
                        )
                    }
                }

                override fun onPlaybackStateChanged(state: Int) {
                    if (playerManager.activePlayer === exoPlayer) {
                        playerManager.reportActiveState(
                            isPlaying = exoPlayer.isPlaying,
                            isBuffering = state == Player.STATE_BUFFERING,
                        )
                    }
                }
            }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    LaunchedEffect(isActivePage, exoPlayer) {
        while (isActivePage) {
            playerManager.reportActiveState(
                isPlaying = exoPlayer.isPlaying,
                isBuffering = exoPlayer.playbackState == Player.STATE_BUFFERING,
            )
            delay(500)
        }
    }

    AndroidView(
        modifier = modifier.pointerInteropFilter { false },
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                setOnTouchListener { _, _ -> false }
            }
        },
    )
}

@Composable
fun SampleFeedCard(
    track: SampleTrack,
    isActivePage: Boolean,
    isPlaying: Boolean,
    isBuffering: Boolean,
    isResolvingFullSong: Boolean,
    playerManager: SamplesPlayerManager,
    onFavoriteClick: () -> Unit,
    onPlayFullSongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(Color.Black),
    ) {
        SampleVideoPage(
            track = track,
            isActivePage = isActivePage,
            playerManager = playerManager,
            modifier = Modifier.fillMaxSize(),
        )

        // Dark gradient at the bottom for text legibility over the video.
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.45f),
                                    Color.Black.copy(alpha = 0.92f),
                                ),
                            startY = 400f,
                        ),
                    ),
        )

        // Tap-to-play/pause overlay (only meaningful on the active page).
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .clickable(enabled = isActivePage) { playerManager.togglePlayPause() },
            contentAlignment = Alignment.Center,
        ) {
            if (isActivePage) {
                if (isBuffering) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                } else if (!isPlaying) {
                    Box(
                        modifier =
                            Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.65f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.play),
                            contentDescription = "Play video preview",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp),
                        )
                    }
                }
            }
        }

        // Header badge.
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, start = 76.dp, end = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.music_note),
                    contentDescription = "Music sign",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "SAMPLES",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                )
            }

            Box(
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    text = track.genre,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        // Bottom track details & actions.
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = track.artist,
                    color = Color.LightGray,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (track.album.isNotBlank()) {
                    Text(
                        text = track.album,
                        color = Color.Gray,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onPlayFullSongClick,
                    enabled = !isResolvingFullSong,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.Black,
                        ),
                    shape = RoundedCornerShape(12.dp),
                    modifier =
                        Modifier
                            .height(44.dp)
                            .testTag("play_full_song_button"),
                ) {
                    if (isResolvingFullSong) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.play),
                            contentDescription = "Play full song",
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Play Full Song", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            Column(
                modifier = Modifier.fillMaxHeight(0.3f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                IconButton(
                    onClick = onFavoriteClick,
                    modifier =
                        Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f)),
                ) {
                    Icon(
                        painter = painterResource(if (track.isFavorite) R.drawable.favorite else R.drawable.favorite_border),
                        contentDescription = "Sample Favorite",
                        tint = if (track.isFavorite) MaterialTheme.colorScheme.tertiary else Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }
}
