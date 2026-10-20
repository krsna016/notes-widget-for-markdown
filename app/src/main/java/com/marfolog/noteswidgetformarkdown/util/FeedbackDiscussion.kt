package com.marfolog.noteswidgetformarkdown.util

import java.net.URLEncoder

/**
 * The foss build sends nothing itself: feedback becomes a pre-filled GitHub discussion that the
 * user reads, and posts or not, in their own browser.
 */
internal object FeedbackDiscussion {

    const val TITLE = "Feedback from the app"

    fun url(thumbs: FeedbackThumbs?, text: String): String {
        val body = body(thumbs, text)
        return buildString {
            append(ProjectLinks.NEW_IDEA_URL)
            append("&title=").append(encode(TITLE))
            if (body.isNotEmpty()) append("&body=").append(encode(body))
        }
    }

    fun body(thumbs: FeedbackThumbs?, text: String): String = listOfNotNull(
        thumbs?.let { "How is it working: ${if (it == FeedbackThumbs.Up) "👍" else "👎"}" },
        text.trim().takeIf { it.isNotEmpty() }
    ).joinToString("\n\n")

    /** Spaces as %20: a "+" is read literally by some parsers of the query string. */
    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8").replace("+", "%20")
}
