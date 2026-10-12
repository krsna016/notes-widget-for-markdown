package com.marfolog.noteswidgetformarkdown.util

import android.app.Activity
import android.content.Context

/**
 * FOSS flavour: never asks for a rating and links to no store.
 *
 * Same API as the Play twin, but there is no store to rate in and no Play library in this build,
 * so every call is a no-op — nothing is recorded either.
 */
object RatingPrompt {

    /** No store page to link to, so settings show no rating link. */
    const val HAS_STORE_PAGE = false

    fun recordFirstSeen(context: Context) = Unit

    suspend fun maybeAsk(activity: Activity) = Unit

    fun openStorePage(context: Context) = Unit
}
