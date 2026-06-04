
package com.nexora.player

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexora.player.data.model.AppDestination
import com.nexora.player.data.model.AppThemeMode
import com.nexora.player.data.model.MediaKind
import com.nexora.player.ui.components.BottomPlayerBar
import com.nexora.player.ui.components.GreetingBanner
import com.nexora.player.ui.components.SearchField
import com.nexora.player.ui.navigation.*
import com.nexora.player.ui.screens.*
import com.nexora.player.ui.theme.NexoraTheme
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

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
            val greeting = rememberGreeting()

            LaunchedEffect(state.currentItem?.id, state.currentItem?.kind) {
                if (state.currentItem?.kind == MediaKind.VIDEO) {
                    showNowPlaying = true
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
                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            GreetingBanner(greeting = greeting)
                            SearchField(query = state.search, onQueryChange = viewModel::setSearch)
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
                                        onClick = { viewModel.setDestination(destination) },
                                        icon = { Icon(iconFor(destination), contentDescription = destination.label) },
                                        label = { Text(destination.label) }
                                    )
                                }
                            }
                        }
                    }
                ) { padding ->
                    AppContent(
                        modifier = Modifier.padding(padding),
                        viewModel = viewModel,
                        state = state
                    )
                }

                if (showNowPlaying) {
                    ModalBottomSheet(onDismissRequest = { showNowPlaying = false }) {
                        Box(modifier = Modifier.fillMaxWidth().heightIn(min = 560.dp)) {
                            NowPlayingScreen()
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

@Composable
private fun AppContent(
    modifier: Modifier = Modifier,
    viewModel: AppViewModel,
    state: AppUiState
) {
    if (state.search.isNotBlank()) {
        SearchResultsScreen(
            modifier = modifier,
            query = state.search,
            audio = viewModel.filteredAudio(),
            videos = viewModel.filteredVideos(),
            onPlayAudio = viewModel::playFromLibrary,
            onPlayVideo = viewModel::playFromLibrary,
            onToggleFavorite = viewModel::toggleFavorite,
            favoriteIds = viewModel.favoriteIds()
        )
        return
    }

    when (state.selectedDestination) {
        AppDestination.MUSIC -> MusicScreen(
            modifier = modifier,
            items = viewModel.filteredAudio(),
            favorites = viewModel.favoriteIds(),
            sortMode = state.audioSort,
            onPlay = viewModel::playFromLibrary,
            onToggleFavorite = viewModel::toggleFavorite,
            onRefresh = viewModel::refreshLibrary,
            onSortSelected = viewModel::setAudioSort
        )

        AppDestination.VIDEOS -> VideoScreen(
            modifier = modifier,
            items = viewModel.filteredVideos(),
            sortMode = state.videoSort,
            onPlay = viewModel::playFromLibrary,
            onRefresh = viewModel::refreshLibrary,
            onSortSelected = viewModel::setVideoSort
        )

        AppDestination.QUEUE -> QueueScreen(
            modifier = modifier,
            queue = state.queue,
            currentIndex = state.queueIndex,
            onPlayItem = viewModel::jumpToQueueIndex,
            onRemoveItem = viewModel::removeQueueIndex,
            onClearQueue = viewModel::clearQueue
        )

        AppDestination.PLAYLISTS -> PlaylistsScreen(
            modifier = modifier,
            playlists = state.playlists,
            onCreatePlaylist = viewModel::createPlaylist,
            onDeletePlaylist = viewModel::deletePlaylist
        )

        AppDestination.FAVORITES -> FavoritesScreen(
            modifier = modifier,
            favorites = state.favorites
        )

        AppDestination.HISTORY -> HistoryScreen(
            modifier = modifier,
            history = state.history
        )

        AppDestination.SETTINGS -> SettingsScreen(
            modifier = modifier,
            themeMode = state.preferences.themeMode,
            dynamicColor = state.preferences.dynamicColor,
            onThemeChange = viewModel::setThemeMode,
            onDynamicColorChange = viewModel::setDynamicColor
        )
    }
}

@Composable
private fun rememberGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Buenos días"
        in 12..18 -> "Buenas tardes"
        else -> "Buenas noches"
    }
}

private fun iconFor(destination: AppDestination) = when (destination) {
    AppDestination.MUSIC -> Icons.Filled.LibraryMusic
    AppDestination.VIDEOS -> Icons.Filled.Movie
    AppDestination.QUEUE -> Icons.Filled.QueueMusic
    AppDestination.PLAYLISTS -> Icons.Filled.PlaylistPlay
    AppDestination.FAVORITES -> Icons.Filled.Favorite
    AppDestination.HISTORY -> Icons.Filled.History
    AppDestination.SETTINGS -> Icons.Filled.Settings
}
