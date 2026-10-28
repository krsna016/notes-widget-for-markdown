package com.marfolog.noteswidgetformarkdown.presentation.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.marfolog.noteswidgetformarkdown.presentation.setup.SetupActivity
import com.marfolog.noteswidgetformarkdown.util.AppLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AutoFlipReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences(SetupActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val autoFlipEnabled = prefs.getBoolean("widget_clock_autoflip", true)
        if (!autoFlipEnabled) return

        val lastTouch = prefs.getLong(ToggleWidgetModeAction.KEY_LAST_TOUCH_TIME, 0L)
        val elapsed = System.currentTimeMillis() - lastTouch
        if (elapsed < 9_000L) {
            AppLog.d("AutoFlip", "Widget was touched ${elapsed}ms ago; skipping auto-flip")
            return
        }

        val currentMode = prefs.getString(ToggleWidgetModeAction.KEY_WIDGET_MODE, ToggleWidgetModeAction.MODE_NOTES)
        if (currentMode == ToggleWidgetModeAction.MODE_NOTES) {
            AppLog.d("AutoFlip", "10 seconds idle elapsed. Flipping widget to clock camouflage mode.")
            prefs.edit()
                .putString(ToggleWidgetModeAction.KEY_WIDGET_MODE, ToggleWidgetModeAction.MODE_CLOCK)
                .apply()

            val pending = goAsync()
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    NotesWidget.updateAll(context.applicationContext)
                } catch (t: Throwable) {
                    AppLog.w("AutoFlip", "Failed to update widget on auto-flip", t)
                } finally {
                    pending.finish()
                }
            }
        }
    }
}

object AutoFlipScheduler {

    const val AUTO_FLIP_DELAY_MILLIS = 10_000L

    fun scheduleAutoFlip(context: Context, delayMs: Long = AUTO_FLIP_DELAY_MILLIS) {
        val prefs = context.getSharedPreferences(SetupActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val autoFlipEnabled = prefs.getBoolean("widget_clock_autoflip", true)
        if (!autoFlipEnabled) return

        prefs.edit()
            .putLong(ToggleWidgetModeAction.KEY_LAST_TOUCH_TIME, System.currentTimeMillis())
            .apply()

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AutoFlipReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context,
            9922,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAtMillis = System.currentTimeMillis() + delayMs
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
            }
        } catch (e: Throwable) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
        }
    }
}
