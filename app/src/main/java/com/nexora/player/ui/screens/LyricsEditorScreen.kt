package com.nexora.player.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nexora.player.R
import com.nexora.player.data.lyrics.LrcParser
import com.nexora.player.data.lyrics.LyricsSource

@Composable
fun LyricsEditorDialog(
    currentPositionMs: Long,
    initialText: String,
    onSave: (rawText: String, exportToFile: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var rawText by remember(initialText) { mutableStateOf(initialText) }
    var selectedLineIndex by rememberSaveable { mutableIntStateOf(-1) }
    var exportToFile by rememberSaveable { mutableStateOf(true) }

    val parsed = remember(rawText) {
        LrcParser.parse(
            rawText = rawText,
            mediaId = 0L,
            title = "",
            artist = "",
            album = "",
            source = LyricsSource.MANUAL
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = androidx.compose.ui.res.stringResource(R.string.lyrics_editor_title))
                        Text(
                            text = androidx.compose.ui.res.stringResource(R.string.lyrics_editor_subtitle),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = null)
                    }
                }

                OutlinedTextField(
                    value = rawText,
                    onValueChange = { rawText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    label = { Text(androidx.compose.ui.res.stringResource(R.string.lyrics_editor_input_label)) }
                )

                Row {
                    Checkbox(
                        checked = exportToFile,
                        onCheckedChange = { exportToFile = it }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(androidx.compose.ui.res.stringResource(R.string.lyrics_editor_export_lrc))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            if (selectedLineIndex in parsed.lines.indices) {
                                rawText = stampLine(rawText, selectedLineIndex, currentPositionMs)
                            }
                        }
                    ) {
                        Icon(Icons.Filled.AccessTime, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(androidx.compose.ui.res.stringResource(R.string.lyrics_mark_time))
                    }

                    Button(onClick = { onSave(rawText, exportToFile) }) {
                        Icon(Icons.Filled.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(androidx.compose.ui.res.stringResource(R.string.save))
                    }
                }

                Divider()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    parsed.lines.forEachIndexed { index, line ->
                        ElevatedAssistChip(
                            onClick = { selectedLineIndex = index },
                            label = { Text(line.text.ifBlank { "…" }) }
                        )
                    }
                }
            }
        }
    }
}

private fun stampLine(raw: String, lineIndex: Int, positionMs: Long): String {
    val lines = raw.lines().toMutableList()
    if (lineIndex !in lines.indices) return raw

    val timestamp = positionMs.toLrcTimestamp()
    val original = lines[lineIndex].trimStart()
    val stripped = original.replace(Regex("""^\[(\d{1,2}):(\d{2})(?:[.:]\d{1,3})?]"""), "")
    lines[lineIndex] = "[$timestamp]$stripped"
    return lines.joinToString("\n")
}

private fun Long.toLrcTimestamp(): String {
    val totalSeconds = this / 1000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    val centiseconds = (this % 1000L) / 10L
    return "%02d:%02d.%02d".format(minutes, seconds, centiseconds)
}
