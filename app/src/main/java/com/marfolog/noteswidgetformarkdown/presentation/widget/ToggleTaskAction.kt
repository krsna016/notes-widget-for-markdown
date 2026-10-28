package com.marfolog.noteswidgetformarkdown.presentation.widget

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.marfolog.noteswidgetformarkdown.presentation.setup.SetupActivity
import com.marfolog.noteswidgetformarkdown.util.AppLog

class ToggleTaskAction : ActionCallback {

    companion object {
        val PARAM_NOTE_URI = ActionParameters.Key<String>("note_uri")
        val PARAM_TASK_TEXT = ActionParameters.Key<String>("task_text")
        val PARAM_IS_CHECKED = ActionParameters.Key<Boolean>("is_checked")
    }

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val prefs = context.getSharedPreferences(SetupActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val uriStr = parameters[PARAM_NOTE_URI] ?: prefs.getString(SetupActivity.KEY_SELECTED_NOTE_URI, null) ?: return
        val taskText = parameters[PARAM_TASK_TEXT] ?: return
        val currentlyChecked = parameters[PARAM_IS_CHECKED] ?: false

        AppLog.d("TaskAction", "Toggling task '$taskText' (currentlyChecked=$currentlyChecked) in $uriStr")

        val uri = Uri.parse(uriStr)
        runCatching {
            val contentResolver = context.contentResolver
            val originalText = contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader(Charsets.UTF_8).readText()
            } ?: return@runCatching

            if (originalText.isBlank()) {
                AppLog.w("TaskAction", "Note content is blank; aborting toggle")
                return@runCatching
            }

            val lines = originalText.lines().toMutableList()
            var modified = false
            val cleanTarget = taskText.trim().lowercase().replace("**", "").replace("⏳", "").trim()
            val targetSubject = cleanTarget.substringBefore("(").substringBefore("—").trim()

            for (i in lines.indices) {
                val line = lines[i]
                val match = Regex("""^(\s*[-*+]\s+\[)([ xX])(\]\s+)(.*)$""").find(line)
                if (match != null) {
                    val (prefix, mark, bracketSpace, content) = match.destructured
                    val isLineChecked = mark.equals("x", ignoreCase = true)
                    val cleanContent = content.trim().lowercase()

                    if (isLineChecked == currentlyChecked &&
                        (cleanContent == cleanTarget || cleanContent.contains(cleanTarget) || cleanTarget.contains(cleanContent) || (targetSubject.length >= 3 && cleanContent.contains(targetSubject)))) {
                        val newMark = if (currentlyChecked) " " else "x"
                        lines[i] = "$prefix$newMark$bracketSpace$content"
                        modified = true
                        break
                    }
                } else if (line.contains("|")) {
                    val tableMatch = Regex("""^(.*\[)([ xX⏳])(\].*)$""").find(line)
                    if (tableMatch != null) {
                        val (before, mark, after) = tableMatch.destructured
                        val isLineChecked = mark.equals("x", ignoreCase = true)
                        val lineLower = line.lowercase()
                        if (isLineChecked == currentlyChecked && (targetSubject.length >= 3 && lineLower.contains(targetSubject))) {
                            val newMark = if (currentlyChecked) " " else "x"
                            lines[i] = "$before$newMark$after"
                            modified = true
                            break
                        }
                    }
                }
            }

            if (modified) {
                val oldBytes = originalText.toByteArray(Charsets.UTF_8)
                val newBytes = lines.joinToString("\n").toByteArray(Charsets.UTF_8)
                val writeSuccess = runCatching {
                    contentResolver.openOutputStream(uri, "wt")?.use { stream ->
                        stream.write(newBytes)
                        stream.flush()
                        true
                    } ?: false
                }.getOrDefault(false)

                if (!writeSuccess) {
                    runCatching {
                        contentResolver.openOutputStream(uri, "wt")?.use { stream ->
                            stream.write(oldBytes)
                            stream.flush()
                        }
                    }
                } else {
                    Handler(Looper.getMainLooper()).post {
                        val status = if (currentlyChecked) "Unchecked" else "Completed"
                        Toast.makeText(context, "$status: $taskText", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }.onFailure { e ->
            AppLog.e("TaskAction", "Failed to toggle task", e)
        }

        AutoFlipScheduler.scheduleAutoFlip(context)
        NotesWidget.updateAll(context)
    }
}
