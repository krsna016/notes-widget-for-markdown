package com.marfolog.noteswidgetformarkdown.util

import android.app.Activity
import android.content.Context
import androidx.annotation.StringRes
import com.marfolog.noteswidgetformarkdown.R

/**
 * FOSS flavour: never asks for a store rating — there is no store and no Play library here.
 *
 * Same API as the Play twin. The rate chip points at the GitHub repository instead, where a star
 * does for this build what a rating does on Google Play.
 */
object RatingPrompt {

    /** No store page to link to, so settings show no rating link. */
    const val HAS_STORE_PAGE = false

    @StringRes
    val RATE_BODY_RES = R.string.rate_dialog_body_foss

    @StringRes
    val RATE_BUTTON_RES = R.string.rate_dialog_rate_foss

    suspend fun maybeAsk(activity: Activity) = Unit

    fun openRatePage(context: Context) = ProjectLinks.open(context, ProjectLinks.REPO_URL)

    fun openStorePage(context: Context) = Unit
}
