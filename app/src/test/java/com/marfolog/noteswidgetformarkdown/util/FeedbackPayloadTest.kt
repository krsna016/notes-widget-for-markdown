package com.marfolog.noteswidgetformarkdown.util

import org.junit.Assert.assertEquals
import org.junit.Test

class FeedbackPayloadTest {

    @Test
    fun `a full answer becomes exactly the fields the rules allow, minus the server time`() {
        val form = FeedbackForm(
            thumbs = FeedbackThumbs.Up,
            ideas = setOf("monospace", "pinned_note"),
            text = "  Would love a pinned Groceries note.  "
        )

        assertEquals(
            mapOf(
                "appVersion" to "1.3.0",
                "versionCode" to 6,
                "thumbs" to "up",
                "ideas" to listOf("pinned_note", "monospace"),
                "text" to "Would love a pinned Groceries note."
            ),
            FeedbackPayload.of(form, versionName = "1.3.0", versionCode = 6)
        )
    }

    @Test
    fun `no thumb is sent as null and no ideas as an empty list`() {
        val payload = FeedbackPayload.of(FeedbackForm(text = "Works"), "1.3.0", 6)

        assertEquals(null, payload["thumbs"])
        assertEquals(emptyList<String>(), payload["ideas"])
        assertEquals(setOf("appVersion", "versionCode", "thumbs", "ideas", "text"), payload.keys)
    }

    @Test
    fun `text longer than the limit is cut to a thousand characters after trimming`() {
        val payload = FeedbackPayload.of(FeedbackForm(text = "   " + "x".repeat(1_500)), "1.3.0", 6)

        assertEquals("x".repeat(1_000), payload["text"])
    }

    @Test
    fun `only spaces become an empty text`() {
        val payload = FeedbackPayload.of(FeedbackForm(thumbs = FeedbackThumbs.Down, text = "  "), "1.3.0", 6)

        assertEquals("", payload["text"])
        assertEquals("down", payload["thumbs"])
    }
}
