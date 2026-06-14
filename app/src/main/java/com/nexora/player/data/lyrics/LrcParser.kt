package com.nexora.player.data.lyrics

private val metadataRegex = Regex("""^\[(\w{2,20}):(.+)]$""")
private val timestampRegex = Regex("""\[(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?]""")
private val enhancedWordRegex = Regex("""<(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?>""")

object LrcParser {

    fun parse(
        rawText: String,
        mediaId: Long,
        title: String,
        artist: String,
        album: String = "",
        source: LyricsSource
    ): Lyrics {
        val cleaned = rawText.replace("\uFEFF", "").trim()
        val parsedLines = mutableListOf<LyricLine>()

        var offsetMs = 0L
        var parsedTitle = title
        var parsedArtist = artist
        var parsedAlbum = album
        var isSynced = false

        cleaned.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isBlank()) return@forEach

            val metaMatch = metadataRegex.matchEntire(line)
            if (metaMatch != null) {
                val key = metaMatch.groupValues[1].lowercase()
                val value = metaMatch.groupValues[2].trim()
                when (key) {
                    "ti" -> parsedTitle = value
                    "ar" -> parsedArtist = value
                    "al" -> parsedAlbum = value
                    "offset" -> offsetMs = value.toLongOrNull() ?: 0L
                }
                return@forEach
            }

            val enhanced = parseEnhancedLine(line)
            if (enhanced != null) {
                isSynced = true
                parsedLines += enhanced.map { it.copy(startMs = (it.startMs + offsetMs).coerceAtLeast(0L)) }
                return@forEach
            }

            val timestamps = timestampRegex.findAll(line)
                .mapNotNull { match ->
                    val minute = match.groupValues[1].toLongOrNull() ?: return@mapNotNull null
                    val second = match.groupValues[2].toLongOrNull() ?: return@mapNotNull null
                    val fraction = match.groupValues[3]
                    val millis = when (fraction.length) {
                        1 -> fraction.toLongOrNull()?.times(100)
                        2 -> fraction.toLongOrNull()?.times(10)
                        3 -> fraction.toLongOrNull()
                        else -> 0L
                    } ?: 0L
                    (minute * 60_000L) + (second * 1000L) + millis
                }
                .toList()

            val text = timestampRegex.replace(line, "").trim()

            if (timestamps.isNotEmpty()) {
                isSynced = true
                timestamps.forEach { ts ->
                    parsedLines += LyricLine(
                        startMs = (ts + offsetMs).coerceAtLeast(0L),
                        text = text
                    )
                }
            } else if (text.isNotBlank()) {
                parsedLines += LyricLine(
                    startMs = 0L,
                    text = text
                )
            }
        }

        val sorted = parsedLines.sortedBy { it.startMs }
        val withEnd = sorted.mapIndexed { index, line ->
            val next = sorted.getOrNull(index + 1)
            line.copy(endMs = next?.startMs)
        }

        return Lyrics(
            mediaId = mediaId,
            title = parsedTitle,
            artist = parsedArtist,
            album = parsedAlbum,
            source = source,
            isSynced = isSynced,
            offsetMs = offsetMs,
            rawText = cleaned,
            lines = withEnd
        )
    }

    fun toLrc(lyrics: Lyrics): String {
        return buildString {
            appendLine("[ti:${lyrics.title}]")
            appendLine("[ar:${lyrics.artist}]")
            if (lyrics.album.isNotBlank()) appendLine("[al:${lyrics.album}]")
            if (lyrics.offsetMs != 0L) appendLine("[offset:${lyrics.offsetMs}]")
            lyrics.lines.forEach { line ->
                append('[')
                append(line.startMs.toLrcTimestamp())
                append(']')
                appendLine(line.text)
            }
        }.trimEnd()
    }

    private fun parseEnhancedLine(line: String): List<LyricLine>? {
        val matches = enhancedWordRegex.findAll(line).toList()
        if (matches.isEmpty()) return null

        val words = mutableListOf<LyricWord>()
        val textBuilder = StringBuilder()

        for ((index, match) in matches.withIndex()) {
            val startMs = match.toTimestampMs()
            val nextStart = matches.getOrNull(index + 1)?.toTimestampMs()
            val startIndex = match.range.last + 1
            val endIndex = matches.getOrNull(index + 1)?.range?.first ?: line.length
            val wordText = line.substring(startIndex, endIndex).trim()
            if (wordText.isBlank()) continue

            if (textBuilder.isNotEmpty()) textBuilder.append(' ')
            textBuilder.append(wordText)

            words += LyricWord(
                startMs = startMs,
                endMs = nextStart,
                text = wordText
            )
        }

        if (words.isEmpty()) return null

        return listOf(
            LyricLine(
                startMs = words.first().startMs,
                endMs = words.last().endMs,
                text = textBuilder.toString(),
                words = words
            )
        )
    }

    private fun MatchResult.toTimestampMs(): Long {
        val minute = groupValues[1].toLongOrNull() ?: 0L
        val second = groupValues[2].toLongOrNull() ?: 0L
        val fraction = groupValues[3]
        val millis = when (fraction.length) {
            1 -> fraction.toLongOrNull()?.times(100)
            2 -> fraction.toLongOrNull()?.times(10)
            3 -> fraction.toLongOrNull()
            else -> 0L
        } ?: 0L
        return (minute * 60_000L) + (second * 1000L) + millis
    }

    private fun Long.toLrcTimestamp(): String {
        val totalSeconds = this / 1000L
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        val centiseconds = (this % 1000L) / 10L
        return "%02d:%02d.%02d".format(minutes, seconds, centiseconds)
    }
}
