package com.marfolog.noteswidgetformarkdown.util

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.StringRes
import com.marfolog.noteswidgetformarkdown.R
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Play flavour: asks Google Play to show its in-app rating dialog, now and then.
 *
 * [maybeAsk] only tells Play that this would be a fair moment, and Play decides whether the dialog
 * actually appears. The app never learns whether it did, or what was rated. The app's own rate
 * chip and settings link only ever open the store page — Google does not allow the in-app review
 * to be started from a button. The foss flavour ships a twin of this file that points at GitHub.
 */
object RatingPrompt {

    @StringRes
    val RATE_BODY_RES = R.string.rate_dialog_body_play

    @StringRes
    val RATE_BUTTON_RES = R.string.rate_dialog_rate_play

    /** The row in the settings feedback section, where an icon already stands in for the star. */
    @StringRes
    val ABOUT_RATE_RES = R.string.about_rate

    private const val AREA = "Rating"
    private const val PREFS = "app_prefs"
    private const val KEY_LAST_REQUEST_AT = "rating_last_request_at"

    /** Play normally answers in well under a second; a save must not hang on a slow one. */
    private const val REQUEST_TIMEOUT_MILLIS = 3_000L

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
            firstSeenAtMillis = RateReminder.firstSeenAt(activity),
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

    fun openRatePage(context: Context) = openStorePage(context)

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
