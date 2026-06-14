package com.nexora.player.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nexora.player.data.lyrics.LyricLine
import com.nexora.player.data.lyrics.Lyrics
import com.nexora.player.data.lyrics.LyricsSource

@Entity(tableName = "lyrics")
data class LyricsEntity(
    @PrimaryKey val mediaId: Long,
    val mediaUriString: String,
    val title: String,
    val artist: String,
    val album: String,
    val source: String,
    val isSynced: Boolean,
    val offsetMs: Long,
    val rawText: String,
    val updatedAt: Long = System.currentTimeMillis()
)

fun LyricsEntity.toDomain(parsedLines: List<LyricLine>): Lyrics {
    return Lyrics(
        mediaId = mediaId,
        title = title,
        artist = artist,
        album = album,
        source = runCatching { LyricsSource.valueOf(source) }.getOrElse { LyricsSource.MANUAL },
        isSynced = isSynced,
        offsetMs = offsetMs,
        rawText = rawText,
        lines = parsedLines,
        updatedAt = updatedAt
    )
}
