package com.virexa.screen.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.os.Environment
import java.io.File

class RecordingRepository(private val context: Context) {

    private fun recordingsDir(folderName: String = "VirexaScreen"): File {
        val base = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir
        return File(base, folderName.ifBlank { "VirexaScreen" }).apply { mkdirs() }
    }

    fun outputFileForNewRecording(folderName: String = "VirexaScreen"): File {
        val timestamp = System.currentTimeMillis()
        return File(recordingsDir(folderName), "Virexa_$timestamp.mp4")
    }

    fun listRecordings(folderName: String = "VirexaScreen"): List<RecordingFile> {
        return recordingsDir(folderName)
            .listFiles { file -> file.isFile && file.extension.lowercase() == "mp4" }
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

    fun deleteRecording(file: RecordingFile): Boolean = File(file.filePath).delete()

    fun renameRecording(file: RecordingFile, newBaseName: String): RecordingFile? {
        val source = File(file.filePath)
        if (!source.exists()) return null
        val safeName = newBaseName.trim().ifBlank { file.displayName }
            .replace(Regex("[^a-zA-Z0-9_\\- ]"), "_")
        val target = File(source.parentFile, "$safeName.mp4")
        return if (source.renameTo(target)) {
            file.copy(fileName = target.name, filePath = target.absolutePath)
        } else null
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
