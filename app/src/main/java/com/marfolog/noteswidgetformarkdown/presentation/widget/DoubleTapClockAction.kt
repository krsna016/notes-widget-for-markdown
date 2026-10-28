package com.marfolog.noteswidgetformarkdown.presentation.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.marfolog.noteswidgetformarkdown.presentation.setup.SetupActivity
import com.marfolog.noteswidgetformarkdown.util.AppLog

class DoubleTapClockAction : ActionCallback {

    companion object {
        @Volatile
        private var lastTapTime = 0L
        private const val DOUBLE_TAP_TIMEOUT_MS = 650L
        private const val KEY_CLOCK_LAST_TAP = "clock_last_tap_time"
    }

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val now = System.currentTimeMillis()
        val prefs = context.getSharedPreferences(SetupActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val prefLastTap = prefs.getLong(KEY_CLOCK_LAST_TAP, 0L)

        val effectiveLastTap = maxOf(lastTapTime, prefLastTap)
        val elapsed = now - effectiveLastTap

        if (elapsed in 40..DOUBLE_TAP_TIMEOUT_MS) {
            AppLog.d("DoubleTapClock", "Double tap detected ($elapsed ms). Flipping back to notes.")
            lastTapTime = 0L
            prefs.edit()
                .putLong(KEY_CLOCK_LAST_TAP, 0L)
                .putString(ToggleWidgetModeAction.KEY_WIDGET_MODE, ToggleWidgetModeAction.MODE_NOTES)
                .apply()
            NotesWidget.updateAll(context)
        } else {
            AppLog.d("DoubleTapClock", "Single tap on clock ($elapsed ms). Waiting for second tap.")
            lastTapTime = now
            prefs.edit().putLong(KEY_CLOCK_LAST_TAP, now).apply()
        }
    }
}
