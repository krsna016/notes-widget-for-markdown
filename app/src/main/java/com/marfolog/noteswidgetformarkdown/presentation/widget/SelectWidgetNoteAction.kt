package com.marfolog.noteswidgetformarkdown.presentation.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.marfolog.noteswidgetformarkdown.presentation.setup.SetupActivity
import com.marfolog.noteswidgetformarkdown.util.AppLog

class SelectWidgetNoteAction : ActionCallback {

    companion object {
        val PARAM_NOTE_URI = ActionParameters.Key<String>("select_note_uri")
        val PARAM_SLIDER_OFFSET = ActionParameters.Key<Int>("slider_offset")
        const val KEY_SLIDER_OFFSET = "widget_slider_offset"
    }

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val prefs = context.getSharedPreferences(SetupActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()

        parameters[PARAM_NOTE_URI]?.let { uri ->
            AppLog.d("SelectNoteAction", "Selecting note: $uri")
            editor.putString(SetupActivity.KEY_SELECTED_NOTE_URI, uri)
        }

        parameters[PARAM_SLIDER_OFFSET]?.let { offset ->
            AppLog.d("SelectNoteAction", "Updating slider offset: $offset")
            editor.putInt(KEY_SLIDER_OFFSET, offset)
        }

        editor.apply()
        NotesWidget.updateAll(context)
    }
}
