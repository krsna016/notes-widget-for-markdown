package com.marfolog.noteswidgetformarkdown.util

import com.marfolog.noteswidgetformarkdown.presentation.widget.WidgetBackground

/**
 * Every name and value [Telemetry] may send. Values come only from the fixed sets below or from
 * enums the app defines itself — never from a note, a file name or a folder.
 *
 * Firebase caps event and parameter names at 40 characters, user property names at 24 and their
 * values at 36.
 */
object UsageEvent {

    const val SETUP_STEP_DONE = "setup_step_done"
    const val SETUP_SKIPPED = "setup_skipped"

    const val WIDGET_ADDED = "widget_added"
    const val NOTE_OPENED = "note_opened"
    const val NOTE_CREATED = "note_created"
    const val FAST_NOTE_SAVED = "fast_note_saved"
    const val REFRESH_TAPPED = "refresh_tapped"
    const val SETTINGS_OPENED = "settings_opened"

    const val FOLDER_SELECTED = "folder_selected"
    const val EXTRA_FILE_ADDED = "extra_file_added"
    const val EXTRA_FILE_REMOVED = "extra_file_removed"
    const val FAST_TARGET_SET = "fast_target_set"
    const val VAULT_ROOT_SET = "vault_root_set"
    const val GIT_ROOT_SET = "git_root_set"
    const val CARD_COLOR_CHANGED = "card_color_changed"
    const val CARD_REORDERED = "card_reordered"
    const val CARD_HEIGHT_CHANGED = "card_height_changed"
    const val SETTING_CHANGED = "setting_changed"
    const val ABOUT_CLICKED = "about_clicked"

    const val RATE_CHIP_SHOWN = "rate_chip_shown"
    const val RATE_CHIP_TAPPED = "rate_chip_tapped"
    const val RATE_DIALOG_CHOICE = "rate_dialog_choice"

    const val FEEDBACK_DIALOG_SHOWN = "feedback_dialog_shown"
    const val FEEDBACK_DIALOG_CHOICE = "feedback_dialog_choice"
    const val FEEDBACK_THUMBS = "feedback_thumbs"

    const val PARAM_STEP = "step"
    const val PARAM_TYPE = "type"
    const val PARAM_TARGET = "target"
    const val PARAM_STYLE = "style"
    const val PARAM_FROM = "from"
    const val PARAM_SETTING = "setting"
    const val PARAM_VALUE = "value"
    const val PARAM_BUTTON = "button"
    const val PARAM_WINDOW = "window"
    const val PARAM_CHOICE = "choice"

    const val STEP_FOLDER = "folder"
    const val STEP_WIDGET = "widget"

    const val WIDGET_CARDS = "cards"
    const val WIDGET_LIST = "list"
    const val WIDGET_BOTH = "both"
    const val WIDGET_NONE = "none"

    const val FROM_WIDGET = "widget"
    const val FROM_LAUNCHER = "launcher"
    const val FROM_SYNC_CHIP = "sync_chip"
    const val FROM_FAST_NOTE_ERROR = "fast_note_error"

    const val SETTING_OPEN_WITH = "open_with"
    const val SETTING_SYNC_MODE = "sync_mode"
    const val SETTING_STALE_HOURS = "stale_hours"
    const val SETTING_THEME = "theme"
    const val SETTING_TELEMETRY = "telemetry"
    const val SETTING_RATE_CHIP = "rate_chip"
    const val SETTING_FAST_NOTE_STYLE = "fast_note_style"
    const val SETTING_WIDGET_BACKGROUND = "widget_background"

    const val BUTTON_REPORT_BUG = "report_bug"
    const val BUTTON_RATE = "rate"
    const val BUTTON_SAY_HI = "say_hi"
    const val BUTTON_SOURCE = "source"
    const val BUTTON_FEEDBACK = "feedback"
    const val BUTTON_PRIVACY = "privacy"

    const val CHOICE_RATE = "rate"
    const val CHOICE_IDEA = "idea"
    const val CHOICE_LATER = "later"
    const val CHOICE_NEVER = "never"
    const val CHOICE_SENT = "sent"

    const val PROPERTY_WIDGET_TYPE = "widget_type"
    const val PROPERTY_SYNC_MODE = "sync_mode"
    const val PROPERTY_OPEN_WITH = "open_with"
    const val PROPERTY_HAS_EXTRA_NOTES = "has_extra_notes"
    const val PROPERTY_FAST_NOTE_STYLE = "fast_note_style"
    const val PROPERTY_WIDGET_ALPHA = "widget_alpha"

    private val KNOWN_OPENED_FROM = setOf(FROM_WIDGET, FROM_SYNC_CHIP, FROM_FAST_NOTE_ERROR)

    fun onOff(enabled: Boolean): String = if (enabled) "on" else "off"

    fun yesNo(value: Boolean): String = if (value) "yes" else "no"

    fun widgetType(cardWidgets: Int, listWidgets: Int): String = when {
        cardWidgets > 0 && listWidgets > 0 -> WIDGET_BOTH
        cardWidgets > 0 -> WIDGET_CARDS
        listWidgets > 0 -> WIDGET_LIST
        else -> WIDGET_NONE
    }

    /** Widget background opacity in four buckets, so the property says how, not exactly how much. */
    fun widgetAlpha(percent: Int): String = when (WidgetBackground.snap(percent)) {
        100 -> "100"
        in 75..95 -> "75-95"
        in 30..70 -> "30-70"
        else -> "0-25"
    }

    /**
     * What a submitted feedback form is counted as: the choice and the
     * thumb. The text never leaves through here.
     */
    fun feedbackSent(form: FeedbackForm): List<Pair<String, Pair<String, String>>> =
        listOf(FEEDBACK_DIALOG_CHOICE to (PARAM_CHOICE to CHOICE_SENT)) +
            listOfNotNull(form.thumbs?.let { FEEDBACK_THUMBS to (PARAM_VALUE to it.value) })

    /** No extra means the launcher; anything unknown is folded into it rather than sent as is. */
    fun openedFrom(extra: String?): String = extra?.takeIf { it in KNOWN_OPENED_FROM } ?: FROM_LAUNCHER
}
