package com.nexora.player.data.model

import android.net.Uri
import androidx.annotation.StringRes
import com.nexora.player.R

enum class MediaKind {
    AUDIO,
    VIDEO
}

enum class SortMode(@StringRes val labelRes: Int) {
    DATE_ADDED_DESC(R.string.sort_date_added_desc),
    DATE_ADDED_ASC(R.string.sort_date_added_asc),
    TITLE_ASC(R.string.sort_title_asc),
    TITLE_DESC(R.string.sort_title_desc),
    DURATION_ASC(R.string.sort_duration_asc),
    DURATION_DESC(R.string.sort_duration_desc),
    ARTIST_ASC(R.string.sort_artist_asc),
    ALBUM_ASC(R.string.sort_album_asc),
    SIZE_ASC(R.string.sort_size_asc),
    SIZE_DESC(R.string.sort_size_desc),
    RESOLUTION_ASC(R.string.sort_resolution_asc),
    RESOLUTION_DESC(R.string.sort_resolution_desc)
}

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class AppLanguage(@StringRes val labelRes: Int, val tag: String?) {
    SYSTEM(R.string.language_system, null),
    SPANISH(R.string.language_spanish, "es"),
    ENGLISH(R.string.language_english, "en");

    companion object {
        fun fromTag(tag: String?): AppLanguage = when (tag?.lowercase()) {
            null, "" -> SYSTEM
            "es" -> SPANISH
            "en" -> ENGLISH
            else -> SYSTEM
        }
    }
}

enum class AppDestination(@StringRes val labelRes: Int) {
    MUSIC(R.string.nav_music),
    VIDEOS(R.string.nav_videos),
    QUEUE(R.string.nav_queue),
    PLAYLISTS(R.string.nav_playlists),
    FAVORITES(R.string.nav_favorites),
    HISTORY(R.string.nav_history),
    SETTINGS(R.string.nav_settings)
}

data class MediaEntry(
    val id: Long,
    val kind: MediaKind,
    val uri: Uri,
    val title: String,
    val subtitle: String = "",
    val album: String = "",
    val artist: String = "",
    val durationMs: Long = 0L,
    val dateAdded: Long = 0L,
    val sizeBytes: Long = 0L,
    val width: Int? = null,
    val height: Int? = null,
    val mimeType: String? = null,
    val folder: String? = null,
    val albumId: Long? = null
) {
    val resolutionLabel: String
        get() = if (width != null && height != null) "${width}x$height" else "—"
}

data class PlaybackSnapshot(
    val queue: List<MediaEntry> = emptyList(),
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false
) {
    val currentItem: MediaEntry?
        get() = queue.getOrNull(currentIndex)
}
