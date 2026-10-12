package com.marfolog.noteswidgetformarkdown.util

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Play flavour: asks Google Play to show its in-app rating dialog, now and then.
 *
 * The app adds no prompt of its own — it only tells Play that this would be a fair moment, and
 * Play decides whether the dialog actually appears. The app never learns whether it did, or what
 * was rated. The only own UI is a plain link to the store page in settings. The foss flavour
 * ships a no-op twin of this file.
 */
object RatingPrompt {

    const val HAS_STORE_PAGE = true

    private const val AREA = "Rating"
    private const val PREFS = "app_prefs"
    private const val KEY_FIRST_SEEN_AT = "rating_first_seen_at"
    private const val KEY_LAST_REQUEST_AT = "rating_last_request_at"

    /** Play normally answers in well under a second; a save must not hang on a slow one. */
    private const val REQUEST_TIMEOUT_MILLIS = 3_000L

    /**
     * Called on every process start, which widget updates cause too — so it also counts people
     * who never open the app itself. Only the first call writes: for someone updating from a
     * version without this, the five days start now, not at their install date.
     */
    fun recordFirstSeen(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.contains(KEY_FIRST_SEEN_AT)) {
            prefs.edit().putLong(KEY_FIRST_SEEN_AT, System.currentTimeMillis()).apply()
        }
    }

    /**
     * Returns once Play is done with its flow, so a caller that finishes right after does not cut
     * the dialog off. Returns straight away when it is not time yet — a plain prefs read — and on
     * every failure: no Play Store, an old one, no network, none of it is the user's problem.
     */
    suspend fun maybeAsk(activity: Activity) {
        val prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val eligible = RatingEligibility.isEligible(
            nowMillis = now,
            firstSeenAtMillis = prefs.getLong(KEY_FIRST_SEEN_AT, -1L).takeIf { it >= 0 },
            lastRequestAtMillis = prefs.getLong(KEY_LAST_REQUEST_AT, -1L).takeIf { it >= 0 }
        )
        if (!eligible) return

        runCatching {
            val manager = ReviewManagerFactory.create(activity)
            val reviewInfo = withTimeoutOrNull(REQUEST_TIMEOUT_MILLIS) { manager.requestReview() }
                ?: return
            if (activity.isFinishing || activity.isDestroyed) return
            // Stored before launching, so the cooldown holds even if Play shows nothing — there
            // is no way to tell, and asking again tomorrow would only feel like nagging.
            prefs.edit().putLong(KEY_LAST_REQUEST_AT, now).apply()
            AppLog.d(AREA, "Handing the rating prompt to Play")
            manager.launchReview(activity, reviewInfo)
        }.onFailure { AppLog.d(AREA, "Rating prompt not available: ${it.javaClass.simpleName}") }
    }

    /** The Play Store app if there is one, the web page otherwise. */
    fun openStorePage(context: Context) {
        val packageName = context.packageName
        val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
        val web = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
        )
        try {
            context.startActivity(market)
        } catch (e: ActivityNotFoundException) {
            runCatching { context.startActivity(web) }
                .onFailure { AppLog.d(AREA, "Nothing can open the store page") }
        }
    }
}
