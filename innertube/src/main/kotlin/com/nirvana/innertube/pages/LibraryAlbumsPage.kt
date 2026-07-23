package com.nirvana.innertube.pages

import com.nirvana.innertube.models.Album
import com.nirvana.innertube.models.AlbumItem
import com.nirvana.innertube.models.Artist
import com.nirvana.innertube.models.ArtistItem
import com.nirvana.innertube.models.MusicResponsiveListItemRenderer
import com.nirvana.innertube.models.MusicTwoRowItemRenderer
import com.nirvana.innertube.models.PlaylistItem
import com.nirvana.innertube.models.SongItem
import com.nirvana.innertube.models.YTItem
import com.nirvana.innertube.models.oddElements
import com.nirvana.innertube.utils.parseTime

data class LibraryAlbumsPage(
    val albums: List<AlbumItem>,
    val continuation: String?,
) {
    companion object {
        fun fromMusicTwoRowItemRenderer(renderer: MusicTwoRowItemRenderer): AlbumItem? {
            return AlbumItem(
                        browseId = renderer.navigationEndpoint.browseEndpoint?.browseId ?: return null,
                        playlistId = renderer.thumbnailOverlay?.musicItemThumbnailOverlayRenderer?.content
                            ?.musicPlayButtonRenderer?.playNavigationEndpoint
                            ?.watchPlaylistEndpoint?.playlistId ?: return null,
                        title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                        artists = null,
                        year = renderer.subtitle?.runs?.lastOrNull()?.text?.toIntOrNull(),
                        thumbnail = renderer.thumbnailRenderer.getThumbnailUrl() ?: return null,
                        explicit = renderer.subtitleBadges?.find {
                            it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                        } != null
                    )
        }
    }
}
