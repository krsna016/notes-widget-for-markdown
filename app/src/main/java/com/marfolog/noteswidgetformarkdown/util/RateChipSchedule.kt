package com.marfolog.noteswidgetformarkdown.util

import java.util.concurrent.TimeUnit

/**
 * When the "★ Rate" chip sits on the widget. Both flavours show it; only where it leads differs.
 *
 * A week after the first start, or two days after updating to a build that has the chip,
 * whichever is later — so someone who has used the app for a while sees it soon after the update,
 * and a new install not before it has earned an opinion. Then up to three short windows, each two
 * days long and sixteen days apart. Nothing in between: a chip that never goes away is noise.
 */
internal object RateChipSchedule {

    val FIRST_SEEN_DELAY_MILLIS = TimeUnit.DAYS.toMillis(7)
    val FEATURE_SEEN_DELAY_MILLIS = TimeUnit.DAYS.toMillis(2)
    val WINDOW_LENGTH_MILLIS = TimeUnit.DAYS.toMillis(2)
    val WINDOW_PERIOD_MILLIS = TimeUnit.DAYS.toMillis(16)
    const val WINDOW_COUNT = 3

    /**
     * Index of the window open at [nowMillis] (0, 1 or 2), or null when the chip stays hidden.
     * Missing timestamps read as "not now". So does a clock earlier than either timestamp: the
     * first show is always later than both.
     */
    fun openWindow(
        nowMillis: Long,
        firstSeenAtMillis: Long?,
        featureSeenAtMillis: Long?,
        rateTapped: Boolean,
        neverAgain: Boolean,
        disabledInSettings: Boolean
    ): Int? {
        if (rateTapped || neverAgain || disabledInSettings) return null
        if (firstSeenAtMillis == null || featureSeenAtMillis == null) return null
        val sinceFirstShow = nowMillis - firstShowAt(firstSeenAtMillis, featureSeenAtMillis)
        if (sinceFirstShow < 0) return null
        val window = sinceFirstShow / WINDOW_PERIOD_MILLIS
        val insideWindow = sinceFirstShow % WINDOW_PERIOD_MILLIS < WINDOW_LENGTH_MILLIS
        return window.toInt().takeIf { window < WINDOW_COUNT && insideWindow }
    }

    fun firstShowAt(firstSeenAtMillis: Long, featureSeenAtMillis: Long): Long =
        maxOf(
            firstSeenAtMillis + FIRST_SEEN_DELAY_MILLIS,
            featureSeenAtMillis + FEATURE_SEEN_DELAY_MILLIS
        )

    /** "Shown" is counted once per window, not once per widget redraw. */
    fun isFirstShowOfWindow(window: Int?, lastReportedWindow: Int?): Boolean =
        window != null && (lastReportedWindow == null || window > lastReportedWindow)
}
