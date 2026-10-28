package com.marfolog.noteswidgetformarkdown.presentation.widget

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.marfolog.noteswidgetformarkdown.data.preferences.ExtraNotesStore
import com.marfolog.noteswidgetformarkdown.data.preferences.NoteCardSettingsStore
import com.marfolog.noteswidgetformarkdown.domain.model.NoteCardTextSize
import com.marfolog.noteswidgetformarkdown.domain.usecase.GetNotesUseCase
import com.marfolog.noteswidgetformarkdown.presentation.setup.SetupActivity
import com.marfolog.noteswidgetformarkdown.util.AppLog
import kotlinx.coroutines.flow.firstOrNull
import org.koin.java.KoinJavaComponent.get

class CycleTextSizeAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val store = NoteCardSettingsStore(context)
        val allSettings = store.getAll()
        
        // Determine current text size from existing settings or default
        val currentSize = allSettings.values.firstOrNull()?.textSize ?: NoteCardTextSize.Default
        val entries = NoteCardTextSize.entries
        val currentIndex = entries.indexOf(currentSize)
        val nextSize = entries[(currentIndex + 1) % entries.size]

        // Update all configured cards or existing notes
        val prefs = context.getSharedPreferences(SetupActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val folderUri = prefs.getString(SetupActivity.KEY_NOTES_URI, null)
        val currentNotes = if (!folderUri.isNullOrEmpty()) {
            runCatching {
                val getNotesUseCase: GetNotesUseCase = get(GetNotesUseCase::class.java)
                getNotesUseCase(folderUri, ExtraNotesStore(context).getAll()).firstOrNull() ?: emptyList()
            }.getOrDefault(emptyList())
        } else emptyList()

        if (allSettings.isEmpty() && currentNotes.isNotEmpty()) {
            currentNotes.forEach { note ->
                store.save(note.fileUri, store.get(note.fileUri).copy(textSize = nextSize))
            }
        } else {
            allSettings.forEach { (noteId, appearance) ->
                store.save(noteId, appearance.copy(textSize = nextSize))
            }
            currentNotes.forEach { note ->
                if (!allSettings.containsKey(note.fileUri)) {
                    store.save(note.fileUri, store.get(note.fileUri).copy(textSize = nextSize))
                }
            }
        }

        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, "Font Size: ${nextSize.label}", Toast.LENGTH_SHORT).show()
        }

        NotesWidget.updateAll(context)
        AppLog.d("Action", "Cycled text size to ${nextSize.label}")
    }
}
