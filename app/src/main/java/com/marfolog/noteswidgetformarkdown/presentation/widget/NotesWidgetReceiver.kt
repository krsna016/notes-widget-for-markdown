package com.marfolog.noteswidgetformarkdown.presentation.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.marfolog.noteswidgetformarkdown.worker.RefreshWorker
import java.util.concurrent.TimeUnit

import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.marfolog.noteswidgetformarkdown.util.AppLog

class NotesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NotesWidget()

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        if (action == "com.marfolog.noteswidgetformarkdown.REFRESH" || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            AppLog.d("WidgetReceiver", "Explicit refresh broadcast received: $action")
            val pending = goAsync()
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    NotesWidget.updateAll(context.applicationContext)
                } catch (t: Throwable) {
                    AppLog.w("WidgetReceiver", "Failed on explicit refresh broadcast", t)
                } finally {
                    pending.finish()
                }
            }
        }
    }

    // Only bookkeeping here: calling NotesWidget.updateAll() from onUpdate would loop.
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        WidgetInventory.onUpdate(context, WidgetInventory.Kind.Cards, appWidgetIds)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        WidgetInventory.onDeleted(context, WidgetInventory.Kind.Cards, appWidgetIds)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        val request = PeriodicWorkRequestBuilder<RefreshWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            RefreshWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WorkManager.getInstance(context).cancelUniqueWork(RefreshWorker.WORK_NAME)
    }
}
