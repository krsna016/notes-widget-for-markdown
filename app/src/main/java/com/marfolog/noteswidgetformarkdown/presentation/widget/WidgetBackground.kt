package com.marfolog.noteswidgetformarkdown.presentation.widget

import android.content.Context
import com.marfolog.noteswidgetformarkdown.domain.model.WidgetColor
import com.marfolog.noteswidgetformarkdown.domain.model.WidgetColorChoice
import com.marfolog.noteswidgetformarkdown.presentation.setup.SetupActivity
import com.marfolog.noteswidgetformarkdown.util.KeyValueStore
import com.marfolog.noteswidgetformarkdown.util.SharedPreferencesStore
import kotlin.math.roundToInt

/**
 * The widget's own background: its colour, and how opaque it is in percent. Only the root behind
 * the notes fades; note cards, buttons and the bottom bar keep their colours.
 */
internal object WidgetBackground {

    /** For everyone who never saved a value, new installs and updated users alike. */
    const val DEFAULT_PERCENT = 85
    const val STEP_PERCENT = 5

    /** Stops between the two ends of a 0–100 slider, as Material's Slider counts them. */
    const val SLIDER_STEPS = 100 / STEP_PERCENT - 1

    fun percent(context: Context): Int = percent(store(context))

    /** A stored value outside 0–100 was never picked on the slider, so it reads as the default. */
    internal fun percent(store: KeyValueStore): Int =
        store.getInt(SetupActivity.KEY_WIDGET_BACKGROUND)?.takeIf { it in 0..100 }?.let(::snap) ?: DEFAULT_PERCENT

    fun save(context: Context, percent: Int) = save(store(context), percent)

    internal fun save(store: KeyValueStore, percent: Int) =
        store.putInt(SetupActivity.KEY_WIDGET_BACKGROUND, snap(percent))

    fun snap(percent: Int): Int =
        (percent.coerceIn(0, 100) + STEP_PERCENT / 2) / STEP_PERCENT * STEP_PERCENT

    fun fromSlider(position: Float): Int =
        if (position.isNaN()) DEFAULT_PERCENT else snap(position.roundToInt())

    fun alpha(percent: Int): Float = snap(percent) / 100f

    fun isOpaque(percent: Int): Boolean = snap(percent) == 100

    /** Text sitting on the root needs its own backdrop only once the wallpaper shows through. */
    fun needsBacking(percent: Int): Boolean = !isOpaque(percent)

    fun color(context: Context): WidgetColorChoice = color(store(context))

    internal fun color(store: KeyValueStore): WidgetColorChoice = WidgetColorChoice(
        color = WidgetColor.fromStorage(store.getString(SetupActivity.KEY_WIDGET_COLOR)),
        customHex = store.getString(SetupActivity.KEY_WIDGET_COLOR_HEX)?.takeIf { it.isNotBlank() }
    )

    fun saveColor(context: Context, choice: WidgetColorChoice) = saveColor(store(context), choice)

    internal fun saveColor(store: KeyValueStore, choice: WidgetColorChoice) {
        store.putString(SetupActivity.KEY_WIDGET_COLOR, choice.color.storageValue)
        store.putString(SetupActivity.KEY_WIDGET_COLOR_HEX, choice.customHex.orEmpty())
    }

    private fun store(context: Context): KeyValueStore = SharedPreferencesStore(
        context.getSharedPreferences(SetupActivity.PREFS_NAME, Context.MODE_PRIVATE)
    )
}
