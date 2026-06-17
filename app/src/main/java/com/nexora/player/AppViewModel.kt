package com.nexora.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nexora.player.data.local.*
import com.nexora.player.data.model.*
import com.nexora.player.data.preferences.AppPreferences
import com.nexora.player.data.preferences.PreferencesRepository
import com.nexora.player.data.repository.MediaStoreRepository
import com.nexora.player.playback.PlayerEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class AppUiState(
    val audio: List<MediaEntry> = emptyList(),
    val videos: List<MediaEntry> = emptyList(),
    val queue: List<MediaEntry> = emptyList(),
    val queueIndex: Int = -1,
    val selectedDestination: AppDestination = AppDestination.MUSIC,
    val audioSort: SortMode = SortMode.DATE_ADDED_DESC,
    val videoSort: SortMode = SortMode.DATE_ADDED_DESC,
    val search: String = "",
    val currentItem: MediaEntry? = null,
    val isPlaying: Boolean = false,
    val favorites: List<FavoriteMediaEntity> = emptyList(),
    val playlists: List<PlaylistEntity> = emptyList(),
    val history: List<PlaybackHistoryEntity> = emptyList(),
    val preferences: AppPreferences = AppPreferences()
)

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val mediaRepository = MediaStoreRepository(context)
    private val preferencesRepository = PreferencesRepository(context)
    private val database = NexoraDatabase.get(context)

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
        observePlayback()
        observeDatabase()
        observePreferences()
        refreshLibrary()
    }

    private fun observePlayback() {
        viewModelScope.launch {
            PlayerEngine.snapshot.collectLatest { snapshot ->
                _uiState.value = _uiState.value.copy(
                    queue = snapshot.queue,
                    queueIndex = snapshot.currentIndex,
                    currentItem = snapshot.currentItem,
                    isPlaying = snapshot.isPlaying
                )
            }
        }
    }

    private fun observePreferences() {
        viewModelScope.launch {
            preferencesRepository.preferences.collect { prefs ->
                _uiState.value = _uiState.value.copy(
                    audioSort = prefs.audioSort,
                    videoSort = prefs.videoSort,
                    selectedDestination = prefs.lastDestination,
                    preferences = prefs
                )
                refreshLibrary()
            }
        }
    }

    private fun observeDatabase() {
        viewModelScope.launch {
            combine(
                database.favoritesDao().observeAll(),
                database.playlistsDao().observePlaylists(),
                database.historyDao().observeRecent()
            ) { favorites, playlists, history ->
                Triple(favorites, playlists, history)
            }.collect { (favorites, playlists, history) ->
                _uiState.value = _uiState.value.copy(
                    favorites = favorites,
                    playlists = playlists,
                    history = history
                )
            }
        }
    }

    fun refreshLibrary() {
        viewModelScope.launch {
            val hiddenIds = _uiState.value.preferences.hiddenAudioIds
            val audio = mediaRepository.loadAudio(_uiState.value.audioSort)
                .filterNot { it.id in hiddenIds }
            val videos = mediaRepository.loadVideos(_uiState.value.videoSort)
            _uiState.value = _uiState.value.copy(audio = audio, videos = videos)
        }
    }

    fun setDestination(destination: AppDestination) {
        _uiState.value = _uiState.value.copy(selectedDestination = destination)
        viewModelScope.launch { preferencesRepository.setLastDestination(destination) }
    }

    fun setSearch(query: String) {
        _uiState.value = _uiState.value.copy(search = query)
    }

    fun setAudioSort(sortMode: SortMode) {
        _uiState.value = _uiState.value.copy(audioSort = sortMode)
        viewModelScope.launch { preferencesRepository.setAudioSort(sortMode) }
        refreshLibrary()
    }

    fun setVideoSort(sortMode: SortMode) {
        _uiState.value = _uiState.value.copy(videoSort = sortMode)
        viewModelScope.launch { preferencesRepository.setVideoSort(sortMode) }
        refreshLibrary()
    }

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch { preferencesRepository.setThemeMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setDynamicColor(enabled) }
    }

    fun playQueue(items: List<MediaEntry>, startIndex: Int = 0) {
        PlayerEngine.playQueue(context, items, startIndex)
    }

    fun play(item: MediaEntry) {
        PlayerEngine.play(context, item)
        viewModelScope.launch {
            database.historyDao().insert(
                PlaybackHistoryEntity(
                    mediaId = item.id,
                    mediaKind = item.kind.name,
                    title = item.title,
                    artist = item.artist,
                    album = item.album,
                    durationMs = item.durationMs,
                    uriString = item.uri.toString()
                )
            )
        }
    }

    fun playFromLibrary(library: List<MediaEntry>, item: MediaEntry) {
        val startIndex = library.indexOfFirst { it.id == item.id }
        if (startIndex >= 0) playQueue(library, startIndex) else play(item)
    }

    fun playPlaylistQueue(items: List<PlaylistItemEntity>, item: PlaylistItemEntity) {
        val queue = items.map { it.toMediaEntry() }
        val startIndex = queue.indexOfFirst { it.id == item.mediaId && it.kind.name == item.mediaKind }
        if (startIndex >= 0) playQueue(queue, startIndex) else play(item.toMediaEntry())
    }

    fun togglePlayPause() {
        PlayerEngine.togglePlayPause(context)
    }

    fun playNext() {
        PlayerEngine.skipNext(context)
    }

    fun playPrevious() {
        PlayerEngine.skipPrevious(context)
    }

    fun jumpToQueueIndex(index: Int) {
        PlayerEngine.jumpTo(context, index)
    }

    fun removeQueueIndex(index: Int) {
        PlayerEngine.removeAt(context, index)
    }

    fun clearQueue() {
        PlayerEngine.clear(context)
    }

    fun hideFromLibrary(entry: MediaEntry) {
        viewModelScope.launch {
            preferencesRepository.addHiddenAudioId(entry.id)
            refreshLibrary()
        }
    }

    fun restoreHiddenAudio() {
        viewModelScope.launch {
            preferencesRepository.clearHiddenAudioIds()
            refreshLibrary()
        }
    }

    fun deleteFromLibrary(entry: MediaEntry) {
        viewModelScope.launch {
            try {
                context.contentResolver.delete(entry.uri, null, null)
            } finally {
                cleanupAfterMediaChange(entry)
                preferencesRepository.removeHiddenAudioId(entry.id)
                refreshLibrary()
            }
        }
    }

    fun toggleFavorite(entry: MediaEntry) {
        viewModelScope.launch {
            val mediaKind = entry.kind.name
            val exists = database.favoritesDao().isFavorite(entry.id, mediaKind)
            if (exists) {
                database.favoritesDao().delete(entry.id, mediaKind)
            } else {
                database.favoritesDao().upsert(
                    FavoriteMediaEntity(
                        mediaId = entry.id,
                        mediaKind = mediaKind,
                        title = entry.title,
                        artist = entry.artist,
                        album = entry.album,
                        durationMs = entry.durationMs,
                        uriString = entry.uri.toString()
                    )
                )
            }
        }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            database.playlistsDao().insertPlaylist(PlaylistEntity(name = name.trim()))
        }
    }

    fun deletePlaylist(playlist: PlaylistEntity) {
        viewModelScope.launch {
            database.playlistsDao().deleteItemsForPlaylist(playlist.id)
            database.playlistsDao().deletePlaylist(playlist.id)
        }
    }

    fun addToPlaylist(playlist: PlaylistEntity, entry: MediaEntry) {
        viewModelScope.launch {
            val next = database.playlistsDao().nextOrderIndex(playlist.id) + 1
            database.playlistsDao().insertPlaylistItem(
                PlaylistItemEntity(
                    playlistId = playlist.id,
                    mediaId = entry.id,
                    mediaKind = entry.kind.name,
                    title = entry.title,
                    artist = entry.artist,
                    album = entry.album,
                    durationMs = entry.durationMs,
                    uriString = entry.uri.toString(),
                    orderIndex = next
                )
            )
        }
    }


    fun playlistItems(playlistId: Long): Flow<List<PlaylistItemEntity>> {
        return database.playlistsDao().observeItems(playlistId)
    }

    fun playlistPreviewItems(playlistId: Long): Flow<List<PlaylistItemEntity>> {
        return database.playlistsDao().observeItems(playlistId).map { it.take(4) }
    }

    fun removeFromPlaylist(itemId: Long) {
        viewModelScope.launch {
            database.playlistsDao().deletePlaylistItem(itemId)
        }
    }

    fun playFavoriteQueue(favorites: List<FavoriteMediaEntity>, favorite: FavoriteMediaEntity) {
        val audioFavorites = favorites.map { it.toMediaEntry() }
        val selected = favorite.toMediaEntry()
        val startIndex = audioFavorites.indexOfFirst { it.id == selected.id && it.kind == selected.kind }
        if (startIndex >= 0) {
            playQueue(audioFavorites, startIndex)
        } else {
            play(selected)
        }
    }

    @Deprecated("Use playFavoriteQueue for favorites section playback")
    fun playFavorite(favorite: FavoriteMediaEntity) {
        play(favorite.toMediaEntry())
    }

    private suspend fun cleanupAfterMediaChange(entry: MediaEntry) {
        val snapshot = PlayerEngine.snapshot.value
        val queueIndex = snapshot.queue.indexOfFirst { it.id == entry.id && it.kind == entry.kind }
        if (queueIndex >= 0) {
            PlayerEngine.removeAt(context, queueIndex)
        }
        if (snapshot.currentItem?.id == entry.id && snapshot.currentItem?.kind == entry.kind) {
            when {
                snapshot.queue.size > 1 -> PlayerEngine.skipNext(context)
                else -> PlayerEngine.clear(context)
            }
        }
        database.favoritesDao().delete(entry.id, entry.kind.name)
        database.playlistsDao().deleteItemsByMediaId(entry.id, entry.kind.name)
        database.lyricsDao().deleteByMediaId(entry.id)
        database.historyDao().deleteByMediaId(entry.id)
    }

    fun playPlaylistItem(item: PlaylistItemEntity) {
        play(item.toMediaEntry())
    }

    private fun FavoriteMediaEntity.toMediaEntry(): MediaEntry {
        return MediaEntry(
            id = mediaId,
            kind = if (mediaKind == MediaKind.VIDEO.name) MediaKind.VIDEO else MediaKind.AUDIO,
            uri = android.net.Uri.parse(uriString),
            title = title,
            artist = artist,
            album = album,
            durationMs = durationMs
        )
    }

    private fun PlaylistItemEntity.toMediaEntry(): MediaEntry {
        return MediaEntry(
            id = mediaId,
            kind = if (mediaKind == MediaKind.VIDEO.name) MediaKind.VIDEO else MediaKind.AUDIO,
            uri = android.net.Uri.parse(uriString),
            title = title,
            artist = artist,
            album = album,
            durationMs = durationMs
        )
    }

    fun filteredAudio(): List<MediaEntry> {
        val q = _uiState.value.search.trim().lowercase()
        return _uiState.value.audio.filter {
            q.isBlank() || it.title.lowercase().contains(q) || it.artist.lowercase().contains(q) || it.album.lowercase().contains(q)
        }
    }

    fun filteredVideos(): List<MediaEntry> {
        val q = _uiState.value.search.trim().lowercase()
        return _uiState.value.videos.filter {
            q.isBlank() || it.title.lowercase().contains(q) || it.folder.orEmpty().lowercase().contains(q)
        }
    }

    fun favoriteIds(): Set<Long> = _uiState.value.favorites.map { it.mediaId }.toSet()
}
