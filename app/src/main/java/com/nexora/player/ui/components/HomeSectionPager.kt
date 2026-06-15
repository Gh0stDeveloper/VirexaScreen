package com.nexora.player.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.nexora.player.R
import com.nexora.player.data.local.PlaylistEntity
import com.nexora.player.data.local.PlaybackHistoryEntity
import com.nexora.player.data.model.AppDestination
import com.nexora.player.data.model.MediaEntry
import com.nexora.player.data.model.SortMode
import com.nexora.player.ui.screens.MusicScreen
import com.nexora.player.ui.screens.PlaylistsScreen
import com.nexora.player.ui.screens.VideoScreen

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeSectionPager(
    modifier: Modifier = Modifier,
    selectedDestination: AppDestination,
    audio: List<MediaEntry>,
    videos: List<MediaEntry>,
    favorites: Set<Long>,
    playlists: List<PlaylistEntity>,
    history: List<PlaybackHistoryEntity>,
    audioSort: SortMode,
    videoSort: SortMode,
    hiddenAudioCount: Int,
    onDestinationSelected: (AppDestination) -> Unit,
    onPlayAudio: (List<MediaEntry>, MediaEntry) -> Unit,
    onPlayVideo: (List<MediaEntry>, MediaEntry) -> Unit,
    onToggleFavorite: (MediaEntry) -> Unit,
    onAddToPlaylist: (PlaylistEntity, MediaEntry) -> Unit,
    onHideFromLibrary: (MediaEntry) -> Unit,
    onRefreshAudio: () -> Unit,
    onRefreshVideo: () -> Unit,
    onAudioSortSelected: (SortMode) -> Unit,
    onVideoSortSelected: (SortMode) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onDeletePlaylist: (PlaylistEntity) -> Unit,
    onOpenPlaylist: (PlaylistEntity) -> Unit
) {
    val pages = listOf(
        AppDestination.MUSIC,
        AppDestination.VIDEOS,
        AppDestination.PLAYLISTS
    )
    val pagerState = rememberPagerState(
        initialPage = pages.indexOf(selectedDestination).coerceAtLeast(0)
    ) {
        pages.size
    }
    var syncingSelection by remember { androidx.compose.runtime.mutableStateOf(false) }

    LaunchedEffect(selectedDestination) {
        val targetPage = pages.indexOf(selectedDestination).takeIf { it >= 0 } ?: 0
        if (pagerState.currentPage != targetPage) {
            syncingSelection = true
            try {
                pagerState.animateScrollToPage(targetPage)
            } finally {
                syncingSelection = false
            }
        }
    }

    LaunchedEffect(pagerState) {
        androidx.compose.runtime.snapshotFlow { pagerState.settledPage }
            .collect { page ->
                val destination = pages.getOrNull(page) ?: AppDestination.MUSIC
                if (!syncingSelection && destination != selectedDestination) {
                    onDestinationSelected(destination)
                }
            }
    }

    Column(modifier = modifier.fillMaxSize()) {
        PrimaryTabRow(
            selectedTabIndex = pagerState.currentPage,
            modifier = Modifier.fillMaxWidth()
        ) {
            pages.forEachIndexed { index, destination ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        onDestinationSelected(destination)
                    },
                    text = { Text(stringResource(destination.labelRes)) },
                    icon = {
                        Icon(
                            imageVector = when (destination) {
                                AppDestination.MUSIC -> Icons.Filled.LibraryMusic
                                AppDestination.VIDEOS -> Icons.Filled.Movie
                                AppDestination.PLAYLISTS -> Icons.AutoMirrored.Filled.PlaylistPlay
                            },
                            contentDescription = null
                        )
                    }
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (pages[page]) {
                AppDestination.MUSIC -> MusicScreen(
                    modifier = Modifier.fillMaxSize(),
                    items = audio,
                    favorites = favorites,
                    playlists = playlists,
                    history = history,
                    sortMode = audioSort,
                    hiddenAudioCount = hiddenAudioCount,
                    onPlay = onPlayAudio,
                    onToggleFavorite = onToggleFavorite,
                    onAddToPlaylist = onAddToPlaylist,
                    onHideFromLibrary = onHideFromLibrary,
                    onRefresh = onRefreshAudio,
                    onSortSelected = onAudioSortSelected
                )

                AppDestination.VIDEOS -> VideoScreen(
                    modifier = Modifier.fillMaxSize(),
                    items = videos,
                    sortMode = videoSort,
                    onPlay = onPlayVideo,
                    onRefresh = onRefreshVideo,
                    onSortSelected = onVideoSortSelected
                )
                AppDestination.PLAYLISTS -> PlaylistsScreen(
                    modifier = Modifier.fillMaxSize(),
                    playlists = playlists,
                    onCreatePlaylist = onCreatePlaylist,
                    onDeletePlaylist = onDeletePlaylist,
                    onOpenPlaylist = onOpenPlaylist
                )
            }
        }
    }
}
