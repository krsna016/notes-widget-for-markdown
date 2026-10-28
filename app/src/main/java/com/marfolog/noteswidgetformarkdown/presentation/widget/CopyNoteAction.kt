package com.marfolog.noteswidgetformarkdown.presentation.widget

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.marfolog.noteswidgetformarkdown.data.preferences.ExtraNotesStore
import com.marfolog.noteswidgetformarkdown.domain.usecase.GetNotesUseCase
import com.marfolog.noteswidgetformarkdown.presentation.setup.SetupActivity
import com.marfolog.noteswidgetformarkdown.util.AppLog
import kotlinx.coroutines.flow.firstOrNull
import org.koin.java.KoinJavaComponent.get

class CopyNoteAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val cached = WidgetDataCache.last?.state
        val primaryNote = if (cached is WidgetState.Success) {
            cached.notes.firstOrNull()
        } else {
            val prefs = context.getSharedPreferences(SetupActivity.PREFS_NAME, Context.MODE_PRIVATE)
            val folderUri = prefs.getString(SetupActivity.KEY_NOTES_URI, null)
            if (!folderUri.isNullOrEmpty()) {
                runCatching {
                    val getNotesUseCase: GetNotesUseCase = get(GetNotesUseCase::class.java)
                    getNotesUseCase(folderUri, ExtraNotesStore(context).getAll()).firstOrNull()?.firstOrNull()
                }.getOrNull()
            } else null
        }

        if (primaryNote == null) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "No note to copy", Toast.LENGTH_SHORT).show()
            }
            return
        }

        val textToCopy = "${primaryNote.title}\n\n${primaryNote.preview}"
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(primaryNote.title, textToCopy)
        clipboard.setPrimaryClip(clip)

        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, "Copied \"${primaryNote.title}\" to clipboard", Toast.LENGTH_SHORT).show()
        }
        AppLog.d("Action", "Copied ${primaryNote.title} to clipboard")
    }
}
