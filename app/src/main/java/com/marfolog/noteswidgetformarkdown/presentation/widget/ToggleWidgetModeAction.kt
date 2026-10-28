package com.marfolog.noteswidgetformarkdown.presentation.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.marfolog.noteswidgetformarkdown.presentation.setup.SetupActivity
import com.marfolog.noteswidgetformarkdown.util.AppLog

class ToggleWidgetModeAction : ActionCallback {

    companion object {
        val PARAM_TARGET_MODE = ActionParameters.Key<String>("target_mode")
        const val MODE_NOTES = "notes"
        const val MODE_CLOCK = "clock"
        const val KEY_WIDGET_MODE = "widget_display_mode"
        const val KEY_LAST_TOUCH_TIME = "widget_last_touch_time"
    }

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val prefs = context.getSharedPreferences(SetupActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val currentMode = prefs.getString(KEY_WIDGET_MODE, MODE_NOTES) ?: MODE_NOTES
        val requestedTarget = parameters[PARAM_TARGET_MODE]
        val targetMode = requestedTarget ?: if (currentMode == MODE_NOTES) MODE_CLOCK else MODE_NOTES

        AppLog.d("WidgetMode", "Toggling widget mode from $currentMode to $targetMode")

        prefs.edit()
            .putString(KEY_WIDGET_MODE, targetMode)
            .putLong(KEY_LAST_TOUCH_TIME, System.currentTimeMillis())
            .apply()

        if (targetMode == MODE_CLOCK) {
            AutoFlipScheduler.cancelAutoFlip(context)
        } else if (targetMode == MODE_NOTES) {
            val autoFlipEnabled = prefs.getBoolean("widget_clock_autoflip", false)
            if (autoFlipEnabled) {
                AutoFlipScheduler.scheduleAutoFlip(context)
            } else {
                AutoFlipScheduler.cancelAutoFlip(context)
            }
        }

        NotesWidget.updateAll(context)
    }
}
