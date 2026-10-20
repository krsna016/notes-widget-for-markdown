package com.marfolog.noteswidgetformarkdown.presentation.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import com.marfolog.noteswidgetformarkdown.presentation.setup.SetupActivity
import com.marfolog.noteswidgetformarkdown.util.Telemetry
import com.marfolog.noteswidgetformarkdown.util.UsageEvent
import com.marfolog.noteswidgetformarkdown.util.UsageProperties

/**
 * Which widget instances the app has already seen, so `widget_added` is reported once per widget
 * placed on a home screen. onEnabled only fires for the first instance of each kind, and onUpdate
 * fires for every instance on every system update — comparing ids is what tells a new one apart.
 */
internal object WidgetInventory {

    enum class Kind(val eventValue: String, val prefsKey: String, val receiver: Class<*>) {
        Cards(UsageEvent.WIDGET_CARDS, "known_widget_ids_cards", NotesWidgetReceiver::class.java),
        List(UsageEvent.WIDGET_LIST, "known_widget_ids_list", NotesListWidgetReceiver::class.java)
    }

    fun count(context: Context, kind: Kind): Int = currentIds(context, kind).size

    /** First start after an update: widgets already on the home screen were not just added. */
    fun adoptExisting(context: Context) {
        Kind.entries.forEach { kind -> save(context, kind, currentIds(context, kind)) }
    }

    @Synchronized
    fun onUpdate(context: Context, kind: Kind, appWidgetIds: IntArray) {
        val known = known(context, kind)
        val added = newIds(appWidgetIds.toSet(), known)
        if (added.isEmpty()) return
        save(context, kind, known + added)
        repeat(added.size) {
            Telemetry.event(UsageEvent.WIDGET_ADDED, UsageEvent.PARAM_TYPE to kind.eventValue)
        }
        UsageProperties.publish(context)
    }

    @Synchronized
    fun onDeleted(context: Context, kind: Kind, appWidgetIds: IntArray) {
        save(context, kind, known(context, kind) - appWidgetIds.toSet())
        UsageProperties.publish(context)
    }

    fun newIds(current: Set<Int>, known: Set<Int>): Set<Int> = current - known

    private fun currentIds(context: Context, kind: Kind): Set<Int> = runCatching {
        AppWidgetManager.getInstance(context)
            .getAppWidgetIds(ComponentName(context, kind.receiver))
            .toSet()
    }.getOrDefault(emptySet())

    private fun known(context: Context, kind: Kind): Set<Int> =
        context.getSharedPreferences(SetupActivity.PREFS_NAME, Context.MODE_PRIVATE)
            .getStringSet(kind.prefsKey, null)
            .orEmpty()
            .mapNotNull { it.toIntOrNull() }
            .toSet()

    private fun save(context: Context, kind: Kind, ids: Set<Int>) {
        context.getSharedPreferences(SetupActivity.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(kind.prefsKey, ids.map { it.toString() }.toSet())
            .apply()
    }
}
