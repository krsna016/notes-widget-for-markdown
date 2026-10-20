package com.marfolog.noteswidgetformarkdown.util

import android.content.Context
import com.marfolog.noteswidgetformarkdown.presentation.setup.SetupActivity

/** What the feedback dialog remembers between visits. The rules are in [FeedbackSchedule]. */
object FeedbackReminder {

    private const val KEY_ANSWERED = "feedback_answered"
    private const val KEY_NEVER_AGAIN = "feedback_never_again"
    private const val KEY_SNOOZED_AT = "feedback_snoozed_at"
    private const val KEY_LAST_SENT_AT = "feedback_last_sent_at"
    private const val KEY_PENDING_SINCE = "feedback_pending_since"

    fun shouldOffer(
        context: Context,
        nowMillis: Long,
        setupComplete: Boolean,
        openedFrom: String?
    ): Boolean = shouldOffer(store(context), nowMillis, setupComplete, openedFrom)

    internal fun shouldOffer(
        store: KeyValueStore,
        nowMillis: Long,
        setupComplete: Boolean,
        openedFrom: String?
    ): Boolean = FeedbackSchedule.shouldOffer(
        nowMillis = nowMillis,
        setupComplete = setupComplete,
        firstSeenAtMillis = RateReminder.firstSeenAt(store),
        openedFrom = openedFrom,
        answered = store.getBoolean(KEY_ANSWERED, false),
        neverAgain = store.getBoolean(KEY_NEVER_AGAIN, false),
        snoozedAtMillis = store.getLong(KEY_SNOOZED_AT)
    )

    fun canSend(context: Context, nowMillis: Long): Boolean = canSend(store(context), nowMillis)

    internal fun canSend(store: KeyValueStore, nowMillis: Long): Boolean =
        FeedbackSchedule.canSend(nowMillis, store.getLong(KEY_LAST_SENT_AT))

    fun markSent(context: Context, nowMillis: Long) = markSent(store(context), nowMillis)

    internal fun markSent(store: KeyValueStore, nowMillis: Long) {
        store.putBoolean(KEY_ANSWERED, true)
        store.putLong(KEY_LAST_SENT_AT, nowMillis)
        store.remove(KEY_PENDING_SINCE)
    }

    fun isPending(context: Context, nowMillis: Long): Boolean = isPending(store(context), nowMillis)

    internal fun isPending(store: KeyValueStore, nowMillis: Long): Boolean =
        FeedbackSchedule.isPending(nowMillis, store.getLong(KEY_PENDING_SINCE))

    fun markPending(context: Context, nowMillis: Long) = markPending(store(context), nowMillis)

    internal fun markPending(store: KeyValueStore, nowMillis: Long) = store.putLong(KEY_PENDING_SINCE, nowMillis)

    fun clearPending(context: Context) = clearPending(store(context))

    internal fun clearPending(store: KeyValueStore) = store.remove(KEY_PENDING_SINCE)

    fun markSnoozed(context: Context, nowMillis: Long) = markSnoozed(store(context), nowMillis)

    internal fun markSnoozed(store: KeyValueStore, nowMillis: Long) = store.putLong(KEY_SNOOZED_AT, nowMillis)

    fun markNeverAgain(context: Context) = markNeverAgain(store(context))

    internal fun markNeverAgain(store: KeyValueStore) = store.putBoolean(KEY_NEVER_AGAIN, true)

    private fun store(context: Context): KeyValueStore = SharedPreferencesStore(
        context.getSharedPreferences(SetupActivity.PREFS_NAME, Context.MODE_PRIVATE)
    )
}
