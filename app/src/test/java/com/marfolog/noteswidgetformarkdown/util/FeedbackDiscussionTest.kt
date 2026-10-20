package com.marfolog.noteswidgetformarkdown.util

import java.net.URLDecoder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedbackDiscussionTest {

    private val base = "https://github.com/marfolog/notes-widget-for-markdown/discussions/new?category=ideas"

    private fun query(url: String): Map<String, String> = url.substringAfter('?').split('&').associate {
        val (key, value) = it.split('=', limit = 2)
        key to URLDecoder.decode(value, "UTF-8")
    }

    @Test
    fun `a full answer opens a new idea with title and body`() {
        val url = FeedbackDiscussion.url(
            thumbs = FeedbackThumbs.Up,
            ideaLabels = listOf("Pinned note on top", "Search"),
            text = "Search across MyNotes & Projects?"
        )

        assertTrue(url.startsWith("$base&title="))
        assertEquals(
            mapOf(
                "category" to "ideas",
                "title" to "Feedback from the app",
                "body" to "How is it working: 👍\n\n" +
                    "What would make it better:\n- Pinned note on top\n- Search\n\n" +
                    "Search across MyNotes & Projects?"
            ),
            query(url)
        )
    }

    @Test
    fun `spaces are encoded as percent twenty, never as plus`() {
        val url = FeedbackDiscussion.url(thumbs = null, ideaLabels = emptyList(), text = "1+1 is two")

        assertFalse(url.contains('+'))
        assertTrue(url.contains("1%2B1%20is%20two"))
    }

    @Test
    fun `ampersands, hashes and diacritics in the text cannot break the query`() {
        val text = "Čeština & emoji 🎉 #1 ?x=y"
        val url = FeedbackDiscussion.url(thumbs = null, ideaLabels = emptyList(), text = text)

        assertEquals(text, query(url)["body"])
    }

    @Test
    fun `missing parts leave no empty headings`() {
        assertEquals("How is it working: 👎", FeedbackDiscussion.body(FeedbackThumbs.Down, emptyList(), "  "))
        assertEquals("What would make it better:\n- Images in notes", FeedbackDiscussion.body(null, listOf("Images in notes"), ""))
        assertEquals("Just text", FeedbackDiscussion.body(null, emptyList(), "  Just text \n"))
    }

    @Test
    fun `an empty answer still opens the form, only without a body`() {
        assertEquals("$base&title=Feedback%20from%20the%20app", FeedbackDiscussion.url(null, emptyList(), ""))
    }
}
