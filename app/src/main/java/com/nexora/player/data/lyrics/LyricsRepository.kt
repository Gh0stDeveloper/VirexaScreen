package com.nexora.player.data.lyrics

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.nexora.player.data.local.LyricsEntity
import com.nexora.player.data.local.NexoraDatabase
import com.nexora.player.data.model.MediaEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class LyricsRepository(
    private val context: Context,
    private val database: NexoraDatabase
) {
    private val dao = database.lyricsDao()

    fun observeLyrics(mediaId: Long): Flow<Lyrics?> {
        return dao.observeByMediaId(mediaId).map { entity ->
            entity?.let { rehydrate(it) }
        }
    }

    suspend fun loadLyrics(
        media: MediaEntry,
        allowOnlineSearch: Boolean = false
    ): Lyrics? = withContext(Dispatchers.IO) {
        dao.getByMediaId(media.id)?.let { return@withContext rehydrate(it) }

        loadFromSidecar(media)?.let { lyrics ->
            saveLyrics(media, lyrics, exportToSidecarFile = false)
            return@withContext lyrics
        }

        loadEmbeddedLyrics(media)?.let { lyrics ->
            saveLyrics(media, lyrics, exportToSidecarFile = false)
            return@withContext lyrics
        }

        if (allowOnlineSearch) {
            fetchFromLrclib(media)?.let { lyrics ->
                saveLyrics(media, lyrics, exportToSidecarFile = false)
                return@withContext lyrics
            }
        }

        null
    }

    suspend fun saveLyrics(
        media: MediaEntry,
        lyrics: Lyrics,
        exportToSidecarFile: Boolean = false
    ) = withContext(Dispatchers.IO) {
        dao.upsert(
            LyricsEntity(
                mediaId = media.id,
                mediaUriString = media.uri.toString(),
                title = lyrics.title,
                artist = lyrics.artist,
                album = lyrics.album,
                source = lyrics.source.name,
                isSynced = lyrics.isSynced,
                offsetMs = lyrics.offsetMs,
                rawText = lyrics.rawText,
                updatedAt = System.currentTimeMillis()
            )
        )

        if (exportToSidecarFile) {
            writeSidecarLrc(media, LrcParser.toLrc(lyrics))
        }
    }

    suspend fun deleteLyrics(mediaId: Long) {
        withContext(Dispatchers.IO) { dao.deleteByMediaId(mediaId) }
    }

    private fun rehydrate(entity: LyricsEntity): Lyrics {
        return LrcParser.parse(
            rawText = entity.rawText,
            mediaId = entity.mediaId,
            title = entity.title,
            artist = entity.artist,
            album = entity.album,
            source = runCatching { LyricsSource.valueOf(entity.source) }.getOrElse { LyricsSource.DATABASE }
        )
    }

    private fun loadFromSidecar(media: MediaEntry): Lyrics? {
        return try {
            val sidecarUri = findSidecarLrcUri(media) ?: return null
            context.contentResolver.openInputStream(sidecarUri)?.use { input ->
                val raw = input.readBytes().toString(Charsets.UTF_8)
                LrcParser.parse(
                    rawText = raw,
                    mediaId = media.id,
                    title = media.title,
                    artist = media.artist,
                    album = media.album,
                    source = LyricsSource.LOCAL_LRC
                )
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun writeSidecarLrc(media: MediaEntry, rawLrc: String) {
        try {
            val uri = findWritableSidecar(media) ?: return
            context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
                output.write(rawLrc.toByteArray(Charsets.UTF_8))
            }
        } catch (_: Throwable) {
            // Room already has the backup.
        }
    }

    private fun findWritableSidecar(media: MediaEntry): Uri? {
        if (media.uri.scheme != "file") return null
        val audioFile = File(media.uri.path ?: return null)
        val parent = audioFile.parentFile ?: return null
        val sidecar = File(parent, "${audioFile.nameWithoutExtension}.lrc")
        return Uri.fromFile(sidecar)
    }

    private fun findSidecarLrcUri(media: MediaEntry): Uri? {
        if (media.uri.scheme == "file") {
            val audioFile = File(media.uri.path ?: return null)
            val sidecar = File(audioFile.parentFile ?: return null, "${audioFile.nameWithoutExtension}.lrc")
            if (sidecar.exists()) return Uri.fromFile(sidecar)
        }

        val displayName = queryDisplayName(media.uri) ?: return null
        val baseName = displayName.substringBeforeLast('.', displayName)
        return queryMediaStoreLrc(baseName, media.folder)
    }

    private fun queryDisplayName(uri: Uri): String? {
        return try {
            context.contentResolver.query(
                uri,
                arrayOf(MediaStore.MediaColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun queryMediaStoreLrc(baseName: String, folder: String?): Uri? {
        return try {
            val filesUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Files.getContentUri("external")
            }

            val projection = arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.RELATIVE_PATH
            )

            val selection: String
            val selectionArgs: Array<String>

            if (!folder.isNullOrBlank() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                selection = "${MediaStore.Files.FileColumns.RELATIVE_PATH} = ? AND ${MediaStore.Files.FileColumns.DISPLAY_NAME} = ?"
                selectionArgs = arrayOf(folder, "$baseName.lrc")
            } else {
                selection = "${MediaStore.Files.FileColumns.DISPLAY_NAME} = ?"
                selectionArgs = arrayOf("$baseName.lrc")
            }

            context.contentResolver.query(filesUri, projection, selection, selectionArgs, null)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                if (cursor.moveToFirst()) {
                    val id = cursor.getLong(idCol)
                    ContentUris.withAppendedId(filesUri, id)
                } else null
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun loadEmbeddedLyrics(media: MediaEntry): Lyrics? {
        return try {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, media.uri)
                val keyField = runCatching {
                    MediaMetadataRetriever::class.java.getField("METADATA_KEY_LYRIC").getInt(null)
                }.getOrNull() ?: return null

                val embedded = retriever.extractMetadata(keyField)?.trim()?.takeIf { it.isNotBlank() } ?: return null
                LrcParser.parse(
                    rawText = embedded,
                    mediaId = media.id,
                    title = media.title,
                    artist = media.artist,
                    album = media.album,
                    source = LyricsSource.EMBEDDED
                )
            } finally {
                runCatching { retriever.release() }
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun fetchFromLrclib(media: MediaEntry): Lyrics? {
        return try {
            val url = buildString {
                append("https://lrclib.net/api/search?")
                append("track_name=").append(URLEncoder.encode(media.title, "UTF-8"))
                if (media.artist.isNotBlank()) {
                    append("&artist_name=").append(URLEncoder.encode(media.artist, "UTF-8"))
                }
                if (media.album.isNotBlank()) {
                    append("&album_name=").append(URLEncoder.encode(media.album, "UTF-8"))
                }
                if (media.durationMs > 0L) {
                    append("&duration=").append(media.durationMs / 1000L)
                }
            }

            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("User-Agent", "NexoraPlayer/1.0 (Android)")
            }

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val array = JSONArray(body)
            var fallbackPlain: Lyrics? = null

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val synced = obj.optString("syncedLyrics").trim()
                val plain = obj.optString("plainLyrics").trim()

                val candidate = when {
                    synced.isNotBlank() -> synced
                    plain.isNotBlank() -> plain
                    else -> continue
                }

                val parsed = LrcParser.parse(
                    rawText = candidate,
                    mediaId = media.id,
                    title = obj.optString("trackName", media.title),
                    artist = obj.optString("artistName", media.artist),
                    album = obj.optString("albumName", media.album),
                    source = LyricsSource.LRCLIB
                )

                if (synced.isNotBlank()) return parsed
                if (fallbackPlain == null) fallbackPlain = parsed
            }

            fallbackPlain
        } catch (_: Throwable) {
            null
        }
    }
}
