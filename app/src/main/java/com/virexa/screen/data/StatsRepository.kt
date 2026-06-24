package com.virexa.screen.data

import java.util.Calendar

object StatsRepository {

    fun compute(recordings: List<RecordingFile>): RecordingStats {
        if (recordings.isEmpty()) return RecordingStats()

        val now = System.currentTimeMillis()
        val weekAgo = now - 7L * 24 * 60 * 60 * 1000
        val monthStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val totalDuration = recordings.sumOf { it.durationMs }
        val totalSize = recordings.sumOf { it.sizeBytes }
        val longest = recordings.maxOf { it.durationMs }
        val largest = recordings.maxOf { it.sizeBytes }
        val avgDuration = if (recordings.isNotEmpty()) totalDuration / recordings.size else 0L
        val thisWeek = recordings.count { it.createdAtMillis >= weekAgo }
        val thisMonth = recordings.count { it.createdAtMillis >= monthStart }

        return RecordingStats(
            totalRecordings = recordings.size,
            totalDurationMs = totalDuration,
            totalSizeBytes = totalSize,
            longestDurationMs = longest,
            largestSizeBytes = largest,
            averageDurationMs = avgDuration,
            thisWeekCount = thisWeek,
            thisMonthCount = thisMonth,
        )
    }
}
