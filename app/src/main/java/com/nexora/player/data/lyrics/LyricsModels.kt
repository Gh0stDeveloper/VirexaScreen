package com.nexora.player.data.lyrics

data class LyricWord(
    val startMs: Long,
    val endMs: Long? = null,
    val text: String
)

data class LyricLine(
    val startMs: Long,
    val endMs: Long? = null,
    val text: String,
    val words: List<LyricWord> = emptyList()
)

enum class LyricsSource {
    LOCAL_LRC,
    EMBEDDED,
    LRCLIB,
    MANUAL,
    DATABASE
}

data class Lyrics(
    val mediaId: Long,
    val title: String,
    val artist: String,
    val album: String = "",
    val source: LyricsSource,
    val isSynced: Boolean = false,
    val offsetMs: Long = 0L,
    val rawText: String,
    val lines: List<LyricLine>,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun currentLineIndex(positionMs: Long): Int {
        if (lines.isEmpty()) return -1
        if (!isSynced) return 0

        val adjusted = (positionMs - offsetMs).coerceAtLeast(0L)

        var low = 0
        var high = lines.lastIndex

        while (low <= high) {
            val mid = (low + high) ushr 1
            val start = lines[mid].startMs
            if (start <= adjusted) {
                low = mid + 1
            } else {
                high = mid - 1
            }
        }

        return high.coerceIn(0, lines.lastIndex)
    }
}
