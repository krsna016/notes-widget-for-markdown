package com.marfolog.noteswidgetformarkdown.util

import android.content.Context
import com.marfolog.noteswidgetformarkdown.presentation.setup.SetupActivity
import java.util.concurrent.TimeUnit

/** Dialogs settings open on their own, highest priority first. */
enum class AutoPrompt { Consent, WhatsNew, Feedback }

/**
 * How often settings may interrupt with a dialog nobody asked for: one per visit at most, and
 * three days of quiet between any two. When several are due the first in [AutoPrompt] wins and
 * the others wait. Dialogs the user opens themselves are not counted.
 *
 * A clock earlier than the last automatic dialog reads as "not yet".
 */
object PromptBudget {

    val MIN_GAP_MILLIS = TimeUnit.DAYS.toMillis(3)

    private const val KEY_LAST_AUTO_PROMPT_AT = "last_auto_prompt_at"

    internal fun pick(
        due: Set<AutoPrompt>,
        nowMillis: Long,
        lastAutoPromptAtMillis: Long?,
        promptedThisVisit: Boolean
    ): AutoPrompt? {
        if (promptedThisVisit) return null
        if (lastAutoPromptAtMillis != null && nowMillis - lastAutoPromptAtMillis < MIN_GAP_MILLIS) return null
        return AutoPrompt.entries.firstOrNull { it in due }
    }

    fun pick(context: Context, due: Set<AutoPrompt>, nowMillis: Long, promptedThisVisit: Boolean): AutoPrompt? =
        pick(store(context), due, nowMillis, promptedThisVisit)

    internal fun pick(store: KeyValueStore, due: Set<AutoPrompt>, nowMillis: Long, promptedThisVisit: Boolean) =
        pick(due, nowMillis, store.getLong(KEY_LAST_AUTO_PROMPT_AT), promptedThisVisit)

    fun markShown(context: Context, nowMillis: Long) = markShown(store(context), nowMillis)

    internal fun markShown(store: KeyValueStore, nowMillis: Long) = store.putLong(KEY_LAST_AUTO_PROMPT_AT, nowMillis)

    private fun store(context: Context): KeyValueStore = SharedPreferencesStore(
        context.getSharedPreferences(SetupActivity.PREFS_NAME, Context.MODE_PRIVATE)
    )
}
