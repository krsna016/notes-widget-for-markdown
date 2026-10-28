package com.marfolog.noteswidgetformarkdown.presentation.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.marfolog.noteswidgetformarkdown.presentation.setup.SetupActivity
import com.marfolog.noteswidgetformarkdown.util.AppLog

class ToggleClockStyleAction : ActionCallback {

    companion object {
        const val KEY_CLOCK_STYLE = "widget_clock_style"
        const val STYLE_DIGITAL = "digital"
        const val STYLE_ANALOG = "analog"
    }

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val prefs = context.getSharedPreferences(SetupActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val currentStyle = prefs.getString(KEY_CLOCK_STYLE, STYLE_DIGITAL) ?: STYLE_DIGITAL
        val newStyle = if (currentStyle == STYLE_DIGITAL) STYLE_ANALOG else STYLE_DIGITAL

        AppLog.d("ClockStyle", "Toggling clock style from $currentStyle to $newStyle")

        prefs.edit()
            .putString(KEY_CLOCK_STYLE, newStyle)
            .apply()

        NotesWidget.updateAll(context)
    }
}
