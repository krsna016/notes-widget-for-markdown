package com.marfolog.noteswidgetformarkdown.util

import android.content.Context
import com.marfolog.noteswidgetformarkdown.data.preferences.ExtraNotesStore
import com.marfolog.noteswidgetformarkdown.domain.model.FastNoteStyle
import com.marfolog.noteswidgetformarkdown.domain.model.WidgetColor
import com.marfolog.noteswidgetformarkdown.presentation.setup.SetupActivity
import com.marfolog.noteswidgetformarkdown.presentation.widget.OpenWith
import com.marfolog.noteswidgetformarkdown.presentation.widget.WidgetBackground
import com.marfolog.noteswidgetformarkdown.presentation.widget.WidgetInventory
import com.marfolog.noteswidgetformarkdown.ui.theme.WidgetPalette

/** How the app is set up, as user properties — so events can be split by setup, not by person. */
object UsageProperties {

    private val SYNC_MODES = setOf("git", "other", "none")

    fun publish(context: Context) {
        val prefs = context.getSharedPreferences(SetupActivity.PREFS_NAME, Context.MODE_PRIVATE)
        of(
            cardWidgets = WidgetInventory.count(context, WidgetInventory.Kind.Cards),
            listWidgets = WidgetInventory.count(context, WidgetInventory.Kind.List),
            syncMode = prefs.getString(SetupActivity.KEY_SYNC_MODE, null)
                ?: if (prefs.getBoolean(SetupActivity.KEY_SHOW_GIT_STATUS, true)) "git" else "none",
            openWith = OpenWith.current(context),
            hasExtraNotes = ExtraNotesStore(context).getAll().isNotEmpty(),
            fastNoteStyle = FastNoteStyle.from(prefs.getString(SetupActivity.KEY_FAST_NOTE_STYLE, null)),
            widgetBackgroundPercent = WidgetBackground.percent(context),
            widgetColor = WidgetPalette.shown(WidgetBackground.color(context))
        ).forEach { (name, value) -> Telemetry.setUserProperty(name, value) }
    }

    internal fun of(
        cardWidgets: Int,
        listWidgets: Int,
        syncMode: String,
        openWith: OpenWith,
        hasExtraNotes: Boolean,
        fastNoteStyle: FastNoteStyle,
        widgetBackgroundPercent: Int,
        widgetColor: WidgetColor
    ): List<Pair<String, String>> = listOf(
        UsageEvent.PROPERTY_WIDGET_TYPE to UsageEvent.widgetType(cardWidgets, listWidgets),
        UsageEvent.PROPERTY_SYNC_MODE to (syncMode.takeIf { it in SYNC_MODES } ?: "none"),
        UsageEvent.PROPERTY_OPEN_WITH to openWith.storageValue,
        UsageEvent.PROPERTY_HAS_EXTRA_NOTES to UsageEvent.yesNo(hasExtraNotes),
        UsageEvent.PROPERTY_FAST_NOTE_STYLE to fastNoteStyle.storageValue,
        UsageEvent.PROPERTY_WIDGET_ALPHA to UsageEvent.widgetAlpha(widgetBackgroundPercent),
        UsageEvent.PROPERTY_WIDGET_COLOR to widgetColor.storageValue
    )
}
