package com.marfolog.noteswidgetformarkdown.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedbackFormTest {

    @Test
    fun `an empty form cannot be sent`() {
        assertFalse(FeedbackForm().canSend)
    }

    @Test
    fun `only spaces in the text box do not count as an answer`() {
        assertFalse(FeedbackForm(text = "   \n ").canSend)
    }

    @Test
    fun `any one answer is enough to send`() {
        assertTrue(FeedbackForm(thumbs = FeedbackThumbs.Down).canSend)
        assertTrue(FeedbackForm(text = "Love it").canSend)
    }

    @Test
    fun `thumbs are a single choice and tapping the chosen one again clears it`() {
        val up = FeedbackForm().withThumbs(FeedbackThumbs.Up)
        assertEquals(FeedbackThumbs.Up, up.thumbs)

        val switched = up.withThumbs(FeedbackThumbs.Down)
        assertEquals(FeedbackThumbs.Down, switched.thumbs)

        assertNull(switched.withThumbs(FeedbackThumbs.Down).thumbs)
    }

    @Test
    fun `typing stops at a thousand characters`() {
        val form = FeedbackForm().withText("a".repeat(1_200))

        assertEquals(1_000, form.text.length)
    }

    @Test
    fun `exactly a thousand characters are kept whole`() {
        val text = "b".repeat(1_000)

        assertEquals(text, FeedbackForm().withText(text).text)
    }

    @Test
    fun `the cut never leaves half an emoji behind`() {
        val text = "a".repeat(999) + "👍" + "tail"

        val capped = FeedbackForm.capText(text)

        assertEquals("a".repeat(999), capped)
    }

    @Test
    fun `the sent text is trimmed`() {
        assertEquals("More colours please", FeedbackForm(text = "  More colours please \n").trimmedText)
    }

    @Test
    fun `thumbs read back from their stored value`() {
        assertEquals(FeedbackThumbs.Up, FeedbackThumbs.from("up"))
        assertEquals(FeedbackThumbs.Down, FeedbackThumbs.from("down"))
        assertNull(FeedbackThumbs.from(null))
        assertNull(FeedbackThumbs.from("sideways"))
    }
}
