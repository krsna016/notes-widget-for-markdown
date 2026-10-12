package com.marfolog.noteswidgetformarkdown.util

import java.util.concurrent.TimeUnit

/**
 * When the Play build may ask Google Play for its rating dialog. Lives in the shared source set
 * so it is unit tested in both flavours; only the Play [RatingPrompt] ever calls it.
 *
 * Someone who has had the app for a few days has an opinion worth asking for; someone on day one
 * does not. Opens are not counted: most people only ever touch the widget. Google throttles the
 * dialog on its own as well, so this is only the floor, not a guarantee anything shows.
 */
internal object RatingEligibility {

    val MIN_AGE_MILLIS = TimeUnit.DAYS.toMillis(5)
    val COOLDOWN_MILLIS = TimeUnit.DAYS.toMillis(90)

    /**
     * [firstSeenAtMillis] is null until the first start has been recorded — never assumed, so an
     * existing user who just updated starts the five days from now rather than from install.
     * A clock that went backwards makes both intervals negative, which reads as "not yet".
     */
    fun isEligible(
        nowMillis: Long,
        firstSeenAtMillis: Long?,
        lastRequestAtMillis: Long?
    ): Boolean {
        if (firstSeenAtMillis == null) return false
        if (nowMillis - firstSeenAtMillis < MIN_AGE_MILLIS) return false
        return lastRequestAtMillis == null || nowMillis - lastRequestAtMillis >= COOLDOWN_MILLIS
    }
}
