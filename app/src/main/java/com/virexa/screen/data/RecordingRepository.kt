
package com.virexa.screen.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import java.io.File

class RecordingRepository(private val context: Context) {

    data class RecordingDestination(
        val file: File? = null,
        val uri: Uri? = null,
        val parcelFileDescriptor: ParcelFileDescriptor? = null,
        val displayPath: String,
    )

    private fun normalizedFolder(folderName: String = "VixeraScreen"): String {
        return folderName.trim().ifBlank { "VixeraScreen" }
    }

    fun createRecordingDestination(folderName: String = "VixeraScreen"): RecordingDestination {
        val folder = normalizedFolder(folderName)
        val timestamp = System.currentTimeMillis()
        val displayName = "Virexa_$timestamp.mp4"

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/$folder/")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            val uri = context.contentResolver.insert(collection, values)
                ?: throw IllegalStateException("No se pudo crear el archivo de grabación")
            val pfd = context.contentResolver.openFileDescriptor(uri, "w")
                ?: throw IllegalStateException("No se pudo abrir el archivo de grabación")
            RecordingDestination(
                file = null,
                uri = uri,
                parcelFileDescriptor = pfd,
                displayPath = "${Environment.DIRECTORY_MOVIES}/$folder/$displayName",
            )
        } else {
            @Suppress("DEPRECATION")
            val base = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
            val dir = File(base, folder).apply { mkdirs() }
            val file = File(dir, displayName)
            RecordingDestination(
                file = file,
                uri = null,
                parcelFileDescriptor = null,
                displayPath = file.absolutePath,
            )
        }
    }

    fun listRecordings(folderName: String = "VixeraScreen"): List<RecordingFile> {
        val folder = normalizedFolder(folderName)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            listFromMediaStore(folder)
        } else {
            listFromFileSystem(folder)
        }
    }

    fun deleteRecording(file: RecordingFile): Boolean {
        return if (file.isContentUri) {
            runCatching { context.contentResolver.delete(Uri.parse(file.filePath), null, null) > 0 }.getOrDefault(false)
        } else {
            File(file.filePath).delete()
        }
    }

    fun renameRecording(file: RecordingFile, newBaseName: String): RecordingFile? {
        val safeName = newBaseName.trim().ifBlank { file.displayName }
            .replace(Regex("[^a-zA-Z0-9_\\- ]"), "_")
        return if (file.isContentUri) {
            val uri = Uri.parse(file.filePath)
            val values = ContentValues().apply { put(MediaStore.MediaColumns.DISPLAY_NAME, "$safeName.mp4") }
            if (context.contentResolver.update(uri, values, null, null) > 0) {
                file.copy(fileName = "$safeName.mp4", filePath = uri.toString())
            } else null
        } else {
            val source = File(file.filePath)
            if (!source.exists()) return null
            val target = File(source.parentFile, "$safeName.mp4")
            if (source.renameTo(target)) {
                file.copy(fileName = target.name, filePath = target.absolutePath)
            } else null
        }
    }

    private fun listFromMediaStore(folder: String): List<RecordingFile> {
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.Video.VideoColumns.DURATION,
            MediaStore.MediaColumns.SIZE,
            MediaStore.Video.VideoColumns.WIDTH,
            MediaStore.Video.VideoColumns.HEIGHT,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.RELATIVE_PATH,
        )
        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} = ?"
        val selectionArgs = arrayOf("${Environment.DIRECTORY_MOVIES}/$folder/")
        val sortOrder = "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"

        val result = mutableListOf<RecordingFile>()
        context.contentResolver.query(collection, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
            val idIdx = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameIdx = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val durationIdx = cursor.getColumnIndexOrThrow(MediaStore.Video.VideoColumns.DURATION)
            val sizeIdx = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val widthIdx = cursor.getColumnIndexOrThrow(MediaStore.Video.VideoColumns.WIDTH)
            val heightIdx = cursor.getColumnIndexOrThrow(MediaStore.Video.VideoColumns.HEIGHT)
            val modifiedIdx = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIdx)
                val uri = Uri.withAppendedPath(collection, id.toString())
                val width = cursor.getInt(widthIdx)
                val height = cursor.getInt(heightIdx)
                result += RecordingFile(
                    fileName = cursor.getString(nameIdx) ?: "Virexa_$id.mp4",
                    filePath = uri.toString(),
                    durationMs = cursor.getLong(durationIdx),
                    sizeBytes = cursor.getLong(sizeIdx),
                    resolution = if (width > 0 && height > 0) "$width x $height" else "Desconocida",
                    createdAtMillis = cursor.getLong(modifiedIdx) * 1000L,
                    audioMode = AudioMode.NONE,
                )
            }
        }
        return result
    }

    private fun listFromFileSystem(folder: String): List<RecordingFile> {
        @Suppress("DEPRECATION")
        val base = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        val dir = File(base, folder).apply { mkdirs() }
        return dir.listFiles { file -> file.isFile && file.extension.lowercase() == "mp4" }
            ?.sortedByDescending { it.lastModified() }
            ?.mapNotNull { file ->
                val meta = runCatching { readMetadata(file) }.getOrNull()
                RecordingFile(
                    fileName = file.name,
                    filePath = file.absolutePath,
                    durationMs = meta?.durationMs ?: 0L,
                    sizeBytes = file.length(),
                    resolution = meta?.resolution ?: "Desconocida",
                    createdAtMillis = file.lastModified(),
                    audioMode = AudioMode.NONE,
                )
            } ?: emptyList()
    }

    private data class Metadata(val durationMs: Long, val resolution: String)

    private fun readMetadata(file: File): Metadata {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            Metadata(duration, if (width > 0 && height > 0) "$width x $height" else "Desconocida")
        } finally {
            retriever.release()
        }
    }
}
