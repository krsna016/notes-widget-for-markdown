package com.marfolog.noteswidgetformarkdown.data.preferences

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtraNotesStoreTest {

    private val gifts = "content://com.android.externalstorage.documents/document/primary%3ADownload%2FGifts.md"
    private val books = "content://com.android.externalstorage.documents/document/primary%3ADocuments%2FBooks.md"
    private val notesTree = "content://com.android.externalstorage.documents/tree/primary%3ADocuments%2FMyNotes"

    @Test
    fun `a list survives a round trip in the order it was added`() {
        val list = listOf(gifts, books)
        assertEquals(list, parseUriList(serializeUriList(list)))
    }

    @Test
    fun `nothing stored yet is an empty list`() {
        assertEquals(emptyList<String>(), parseUriList(null))
        assertEquals(emptyList<String>(), parseUriList(""))
    }

    @Test
    fun `unreadable json is an empty list, not a crash`() {
        assertEquals(emptyList<String>(), parseUriList("{not json"))
        assertEquals(emptyList<String>(), parseUriList("{\"a\":1}"))
    }

    @Test
    fun `blanks and repeats are dropped`() {
        assertEquals(listOf(gifts), parseUriList("[\"$gifts\", \"\", \"$gifts\"]"))
        assertEquals(listOf(gifts), parseUriList(serializeUriList(listOf(gifts, gifts))))
    }

    @Test
    fun `a picked file's own grant can be released`() {
        assertTrue(canReleaseGrant(gifts, setOf(notesTree)))
    }

    /** The git reader and vault locator scan every folder grant; losing one breaks them. */
    @Test
    fun `a folder grant is never released, even if it ended up in the list`() {
        assertFalse(canReleaseGrant(notesTree, emptySet()))
        assertFalse(canReleaseGrant("$notesTree/document/primary%3ADocuments%2FMyNotes%2FGifts.md", emptySet()))
    }

    @Test
    fun `a uri the rest of the app still relies on is never released`() {
        assertFalse(canReleaseGrant(gifts, setOf(gifts)))
    }
}
