package com.marfolog.noteswidgetformarkdown.util

import java.util.concurrent.TimeUnit

/**
 * When settings offer the feedback dialog on their own. The "Send feedback" row opens it any time;
 * this only decides about the unasked-for appearance.
 *
 * A week after the first start, once setup is finished, never right after a failed fast note sent
 * the user here to fix something, and never again once answered or declined for good. "Not now"
 * holds it back for a day. A clock earlier than a stored time reads as "not yet". Whether it may
 * appear in this visit at all, next to other dialogs, is up to [PromptBudget].
 */
internal object FeedbackSchedule {

    val MIN_AGE_MILLIS = TimeUnit.DAYS.toMillis(7)
    val SNOOZE_MILLIS = TimeUnit.HOURS.toMillis(24)
    val SEND_INTERVAL_MILLIS = TimeUnit.HOURS.toMillis(24)
    val PENDING_MILLIS = TimeUnit.SECONDS.toMillis(60)

    fun shouldOffer(
        nowMillis: Long,
        setupComplete: Boolean,
        firstSeenAtMillis: Long?,
        openedFrom: String?,
        answered: Boolean,
        neverAgain: Boolean,
        snoozedAtMillis: Long?
    ): Boolean {
        if (!setupComplete || answered || neverAgain) return false
        if (openedFrom == UsageEvent.FROM_FAST_NOTE_ERROR) return false
        if (firstSeenAtMillis == null || nowMillis - firstSeenAtMillis < MIN_AGE_MILLIS) return false
        return snoozedAtMillis == null || nowMillis - snoozedAtMillis >= SNOOZE_MILLIS
    }

    /**
     * At most one submission a day. A clock earlier than the last one does not block: the stored
     * time is then the one that cannot be trusted, and a wrong date must not lock feedback out.
     */
    fun canSend(nowMillis: Long, lastSentAtMillis: Long?): Boolean {
        if (lastSentAtMillis == null) return true
        val elapsed = nowMillis - lastSentAtMillis
        return elapsed < 0 || elapsed >= SEND_INTERVAL_MILLIS
    }

    /**
     * A send that timed out may still land a moment later, so for a minute another press of Send
     * would risk a second copy. A clock earlier than the start does not block, as in [canSend].
     */
    fun isPending(nowMillis: Long, pendingSinceMillis: Long?): Boolean {
        if (pendingSinceMillis == null) return false
        return nowMillis - pendingSinceMillis in 0 until PENDING_MILLIS
    }
}
