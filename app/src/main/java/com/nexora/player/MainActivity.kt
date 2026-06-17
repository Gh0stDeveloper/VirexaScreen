package com.nexora.player

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexora.player.data.local.FavoriteMediaEntity
import com.nexora.player.data.local.PlaybackHistoryEntity
import com.nexora.player.data.local.PlaylistEntity
import com.nexora.player.data.model.MediaEntry
import com.nexora.player.data.model.AppDestination
import com.nexora.player.data.model.AppLanguage
import com.nexora.player.data.model.AppThemeMode
import com.nexora.player.data.model.MediaKind
import com.nexora.player.ui.components.BottomPlayerBar
import com.nexora.player.ui.components.GreetingBanner
import com.nexora.player.ui.screens.FavoritesScreen
import com.nexora.player.ui.screens.HistoryScreen
import com.nexora.player.ui.screens.MusicScreen
import com.nexora.player.ui.screens.NowPlayingScreen
import com.nexora.player.ui.screens.PlaylistDetailScreen
import com.nexora.player.ui.screens.PlaylistsScreen
import com.nexora.player.ui.screens.QueueScreen
import com.nexora.player.ui.screens.SearchResultsScreen
import com.nexora.player.ui.screens.SettingsScreen
import com.nexora.player.ui.screens.VideoScreen
import com.nexora.player.ui.theme.NexoraTheme
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : AppCompatActivity() {

    private val viewModel: AppViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { viewModel.refreshLibrary() }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        requestMediaPermissions()

        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            var showNowPlaying by rememberSaveable { mutableStateOf(false) }
            var searchExpanded by rememberSaveable { mutableStateOf(false) }
            var lastAutoOpenedItemId by rememberSaveable { mutableStateOf<Long?>(null) }
            var selectedPlaylistId by rememberSaveable { mutableStateOf<Long?>(null) }

            val greeting = rememberGreeting()

            LaunchedEffect(state.currentItem?.id, state.isPlaying) {
                val current = state.currentItem
                if (state.isPlaying && current != null && lastAutoOpenedItemId != current.id) {
                    showNowPlaying = true
                    lastAutoOpenedItemId = current.id
                }
            }

            NexoraTheme(
                darkTheme = when (state.preferences.themeMode) {
                    AppThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
                    AppThemeMode.LIGHT -> false
                    AppThemeMode.DARK -> true
                },
                dynamicColor = state.preferences.dynamicColor
            ) {
                val destinations = listOf(
                    AppDestination.MUSIC,
                    AppDestination.VIDEOS,
                    AppDestination.QUEUE,
                    AppDestination.PLAYLISTS,
                    AppDestination.FAVORITES,
                    AppDestination.HISTORY,
                    AppDestination.SETTINGS
                )

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        Column(
                            modifier = Modifier
                                .statusBarsPadding()
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            GreetingBanner(
                                greeting = greeting,
                                query = state.search,
                                expanded = searchExpanded,
                                onExpandedChange = { searchExpanded = it },
                                onQueryChange = { viewModel.setSearch(it) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    bottomBar = {
                        Column {
                            BottomPlayerBar(
                                current = state.currentItem,
                                isPlaying = state.isPlaying,
                                onClick = { showNowPlaying = true },
                                onPrevious = viewModel::playPrevious,
                                onTogglePlay = viewModel::togglePlayPause,
                                onNext = viewModel::playNext
                            )
                            NavigationBar {
                                destinations.forEach { destination ->
                                    val selected = state.selectedDestination == destination
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = {
                                            searchExpanded = false
                                            selectedPlaylistId = null
                                            viewModel.setDestination(destination)
                                        },
                                        icon = {
                                            Icon(
                                                iconFor(destination),
                                                contentDescription = stringResource(destination.labelRes)
                                            )
                                        },
                                        label = { Text(stringResource(destination.labelRes)) }
                                    )
                                }
                            }
                        }
                    }
                ) { padding ->
                    AppContent(
                        modifier = Modifier.padding(padding),
                        viewModel = viewModel,
                        state = state,
                        selectedPlaylistId = selectedPlaylistId,
                        onOpenPlaylist = { selectedPlaylistId = it.id },
                        onClosePlaylist = { selectedPlaylistId = null }
                    )
                }

                if (showNowPlaying) {
                    androidx.compose.ui.window.Dialog(
                        onDismissRequest = { showNowPlaying = false },
                        properties = androidx.compose.ui.window.DialogProperties(
                            usePlatformDefaultWidth = false
                        )
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            NowPlayingScreen(
                                modifier = Modifier.fillMaxSize(),
                                onClose = { showNowPlaying = false }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun requestMediaPermissions() {
        val permissions = buildList {
            if (Build.VERSION.SDK_INT >= 33) {
                add(Manifest.permission.READ_MEDIA_AUDIO)
                add(Manifest.permission.READ_MEDIA_VIDEO)
                add(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppContent(
    modifier: Modifier = Modifier,
    viewModel: AppViewModel,
    state: AppUiState,
    selectedPlaylistId: Long?,
    onOpenPlaylist: (PlaylistEntity) -> Unit,
    onClosePlaylist: () -> Unit
) {
    if (state.search.isNotBlank()) {
        SearchResultsScreen(
            modifier = modifier,
            query = state.search,
            audio = viewModel.filteredAudio(),
            videos = viewModel.filteredVideos(),
            onlineEnabled = state.preferences.onlineMusicSearchEnabled,
            onlineLoading = state.onlineLoading,
            onlineError = state.onlineError,
            onlineTracks = state.onlineTracks,
            savedOnlineKeys = state.onlineSavedTracks.map { "${it.providerId}:${it.sourceId}" }.toSet(),
            onPlayAudio = viewModel::playFromLibrary,
            onPlayVideo = viewModel::playFromLibrary,
            onPlayOnline = viewModel::playOnlineQueue,
            onToggleFavorite = viewModel::toggleFavorite,
            onToggleSaveOnline = viewModel::toggleSavedOnlineTrack,
            favoriteIds = viewModel.favoriteIds()
        )
        return
    }

    selectedPlaylistId?.let { playlistId ->
        val playlist = state.playlists.firstOrNull { it.id == playlistId }
        if (playlist != null) {
            val playlistItems by viewModel.playlistItems(playlist.id)
                .collectAsStateWithLifecycle(initialValue = emptyList())

            PlaylistDetailScreen(
                modifier = modifier,
                playlist = playlist,
                playlistItems = playlistItems,
                availableSongs = viewModel.filteredAudio(),
                onBack = onClosePlaylist,
                onPlayItem = viewModel::playPlaylistQueue,
                onRemoveItem = { viewModel.removeFromPlaylist(it.id) },
                onAddSong = { song ->
                    viewModel.addToPlaylist(playlist, song)
                }
            )
            return
        } else {
            onClosePlaylist()
        }
    }

    DestinationPagerContent(
        modifier = modifier,
        state = state,
        viewModel = viewModel,
        onOpenPlaylist = onOpenPlaylist
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DestinationPagerContent(
    modifier: Modifier,
    state: AppUiState,
    viewModel: AppViewModel,
    onOpenPlaylist: (PlaylistEntity) -> Unit
) {
    val destinations = listOf(
        AppDestination.MUSIC,
        AppDestination.VIDEOS,
        AppDestination.QUEUE,
        AppDestination.PLAYLISTS,
        AppDestination.FAVORITES,
        AppDestination.HISTORY,
        AppDestination.SETTINGS
    )

    val pagerState = rememberPagerState(
        initialPage = destinations.indexOf(state.selectedDestination).coerceAtLeast(0),
        pageCount = { destinations.size }
    )

    LaunchedEffect(state.selectedDestination) {
        val target = destinations.indexOf(state.selectedDestination).coerceAtLeast(0)
        if (pagerState.currentPage != target) {
            pagerState.animateScrollToPage(target)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        val destination = destinations[pagerState.currentPage]
        if (state.selectedDestination != destination) {
            viewModel.setDestination(destination)
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxSize()
    ) { page ->
        when (destinations[page]) {
            AppDestination.MUSIC -> MusicScreen(
                modifier = Modifier.fillMaxSize(),
                items = viewModel.filteredAudio(),
                favorites = viewModel.favoriteIds(),
                playlists = state.playlists,
                sortMode = state.audioSort,
                onPlay = viewModel::playFromLibrary,
                onToggleFavorite = viewModel::toggleFavorite,
                onAddToPlaylist = viewModel::addToPlaylist,
                onHideFromLibrary = viewModel::hideFromLibrary,
                onDeleteFromLibrary = viewModel::deleteFromLibrary,
                onRefresh = viewModel::refreshLibrary,
                onSortSelected = viewModel::setAudioSort
            )

            AppDestination.VIDEOS -> VideoScreen(
                modifier = Modifier.fillMaxSize(),
                items = viewModel.filteredVideos(),
                sortMode = state.videoSort,
                onPlay = viewModel::playFromLibrary,
                onRefresh = viewModel::refreshLibrary,
                onSortSelected = viewModel::setVideoSort
            )

            AppDestination.QUEUE -> QueueScreen(
                modifier = Modifier.fillMaxSize(),
                queue = state.queue,
                currentIndex = state.queueIndex,
                onPlayItem = viewModel::jumpToQueueIndex,
                onRemoveItem = viewModel::removeQueueIndex,
                onClearQueue = viewModel::clearQueue
            )

            AppDestination.PLAYLISTS -> PlaylistsScreen(
                modifier = Modifier.fillMaxSize(),
                playlists = state.playlists,
                onCreatePlaylist = viewModel::createPlaylist,
                onDeletePlaylist = viewModel::deletePlaylist,
                onOpenPlaylist = onOpenPlaylist,
                playlistPreviewItems = { playlistId -> viewModel.playlistPreviewItems(playlistId) }
            )

            AppDestination.FAVORITES -> FavoritesScreen(
                modifier = Modifier.fillMaxSize(),
                favorites = state.favorites.filter { it.mediaKind == com.nexora.player.data.model.MediaKind.AUDIO.name },
                onPlayFavoriteQueue = viewModel::playFavoriteQueue,
                onToggleFavorite = { favorite -> viewModel.toggleFavorite(favorite.toMediaEntry()) }
            )

            AppDestination.HISTORY -> HistoryScreen(
                modifier = Modifier.fillMaxSize(),
                history = state.history,
                onPlayItem = { historyItem -> viewModel.play(historyItem.toMediaEntry()) }
            )

            AppDestination.SETTINGS -> SettingsScreen(
                modifier = Modifier.fillMaxSize(),
                themeMode = state.preferences.themeMode,
                dynamicColor = state.preferences.dynamicColor,
                hiddenAudioCount = state.preferences.hiddenAudioIds.size,
                onlineMusicSearchEnabled = state.preferences.onlineMusicSearchEnabled,
                currentLanguage = rememberAppLanguage(),
                onThemeChange = viewModel::setThemeMode,
                onDynamicColorChange = viewModel::setDynamicColor,
                onOnlineMusicSearchChange = viewModel::setOnlineMusicSearchEnabled,
                onLanguageChange = ::applyLanguage,
                onRestoreHiddenAudio = viewModel::restoreHiddenAudio
            )
        }
    }
}
private fun FavoriteMediaEntity.toMediaEntry(): MediaEntry = MediaEntry(
    id = mediaId,
    kind = if (mediaKind == MediaKind.VIDEO.name) MediaKind.VIDEO else MediaKind.AUDIO,
    uri = android.net.Uri.parse(uriString),
    title = title,
    album = album,
    artist = artist,
    durationMs = durationMs
)

private fun PlaybackHistoryEntity.toMediaEntry(): MediaEntry = MediaEntry(
    id = mediaId,
    kind = if (mediaKind == MediaKind.VIDEO.name) MediaKind.VIDEO else MediaKind.AUDIO,
    uri = android.net.Uri.parse(uriString),
    title = title,
    album = album,
    artist = artist,
    durationMs = durationMs
)

@Composable
private fun rememberGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> stringResource(com.nexora.player.R.string.greeting_morning)
        in 12..18 -> stringResource(com.nexora.player.R.string.greeting_afternoon)
        else -> stringResource(com.nexora.player.R.string.greeting_evening)
    }
}

@Composable
private fun rememberAppLanguage(): AppLanguage {
    val tags = AppCompatDelegate.getApplicationLocales().toLanguageTags()
    return AppLanguage.fromTag(tags)
}

private fun applyLanguage(language: AppLanguage) {
    val locales = when (val tag = language.tag) {
        null -> LocaleListCompat.getEmptyLocaleList()
        else -> LocaleListCompat.forLanguageTags(tag)
    }
    AppCompatDelegate.setApplicationLocales(locales)
}

private fun iconFor(destination: AppDestination) = when (destination) {
    AppDestination.MUSIC -> Icons.Filled.LibraryMusic
    AppDestination.VIDEOS -> Icons.Filled.Movie
    AppDestination.QUEUE -> Icons.AutoMirrored.Filled.QueueMusic
    AppDestination.PLAYLISTS -> Icons.AutoMirrored.Filled.PlaylistPlay
    AppDestination.FAVORITES -> Icons.Filled.Favorite
    AppDestination.HISTORY -> Icons.Filled.History
    AppDestination.SETTINGS -> Icons.Filled.Settings
}
