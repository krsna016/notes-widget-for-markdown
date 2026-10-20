package com.marfolog.noteswidgetformarkdown.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.StringRes
import com.marfolog.noteswidgetformarkdown.R

/**
 * FOSS flavour: sends nothing. The answers become a pre-filled GitHub discussion, opened in the
 * browser, where the user can read it and decide whether to post it. Same API as the Play twin.
 */
object FeedbackSender {

    @StringRes
    val FOOTER_RES = R.string.feedback_footer_foss

    @StringRes
    val FAILED_RES = R.string.about_no_browser

    suspend fun send(context: Context, form: FeedbackForm): FeedbackSendResult {
        val url = FeedbackDiscussion.url(
            thumbs = form.thumbs,
            ideaLabels = form.orderedIdeas.mapNotNull { id -> FeedbackIdeas.labelRes(id)?.let(context::getString) },
            text = form.trimmedText
        )
        val opened = runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.isSuccess
        if (!opened) return FeedbackSendResult.Failed
        FeedbackReminder.markSent(context, System.currentTimeMillis())
        return FeedbackSendResult.Sent
    }
}
