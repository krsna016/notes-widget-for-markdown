package com.marfolog.noteswidgetformarkdown.data.repository

import com.marfolog.noteswidgetformarkdown.domain.model.NoteSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The widget shows the folder's notes and the extra notes as one list. The same file reached
 * both ways must show up once, and it must be the folder copy — that one carries the delete
 * behaviour and the appearance the user set up for it.
 */
class ExtraNotesTest {

    private val provider = "content://com.android.externalstorage.documents"
    private val folderTree = "$provider/tree/primary%3ADocuments%2FMyNotes"

    private fun folderNoteUri(name: String) =
        "$folderTree/document/primary%3ADocuments%2FMyNotes%2F$name"

    private fun pickedUri(path: String) = "$provider/document/primary%3A${path.replace("/", "%2F")}"

    @Test
    fun `a tree uri and a picked uri for the same file are the same document`() {
        assertEquals(
            documentIdentity(folderNoteUri("Gifts.md")),
            documentIdentity(pickedUri("Documents/MyNotes/Gifts.md"))
        )
    }

    @Test
    fun `different files are different documents`() {
        assertFalse(
            documentIdentity(folderNoteUri("Gifts.md")) ==
                documentIdentity(pickedUri("Documents/Projects/Gifts.md"))
        )
    }

    @Test
    fun `the same document id at another provider is another document`() {
        val other = "content://some.other.provider/document/primary%3ADocuments%2FMyNotes%2FGifts.md"
        assertFalse(documentIdentity(folderNoteUri("Gifts.md")) == documentIdentity(other))
    }

    @Test
    fun `a plus in the document id is kept as a plus`() {
        assertEquals(
            "com.android.externalstorage.documents/primary:A+B.md",
            documentIdentity("$provider/document/primary%3AA+B.md")
        )
    }

    @Test
    fun `something that is not a document uri has no identity`() {
        assertNull(documentIdentity("not a uri"))
        assertNull(documentIdentity(folderTree))
    }

    @Test
    fun `an extra note already in the folder is shown once, as the folder note`() {
        val folderNote = note(folderNoteUri("Gifts.md"))
        val sameFileAsExtra = note(pickedUri("Documents/MyNotes/Gifts.md"), isExtra = true)
        val merged = mergeFolderAndExtraNotes(listOf(folderNote), listOf(sameFileAsExtra))
        assertEquals(listOf(folderNote), merged)
    }

    @Test
    fun `extra notes outside the folder are added after the folder notes, in their own order`() {
        val folderNote = note(folderNoteUri("Projects.md"))
        val first = note(pickedUri("Download/Gifts.md"), isExtra = true)
        val second = note(pickedUri("Documents/Other/Books.md"), isExtra = true)
        assertEquals(
            listOf(folderNote, first, second),
            mergeFolderAndExtraNotes(listOf(folderNote), listOf(first, second))
        )
    }

    @Test
    fun `the same extra note twice is shown once`() {
        val extra = note(pickedUri("Download/Gifts.md"), isExtra = true)
        assertEquals(listOf(extra), mergeFolderAndExtraNotes(emptyList(), listOf(extra, extra)))
    }

    @Test
    fun `a file in a subfolder of the notes folder is not a duplicate`() {
        // The folder listing is not recursive, so this file is not on the widget yet.
        val folderNote = note(folderNoteUri("Gifts.md"))
        val nested = note(pickedUri("Documents/MyNotes/Archive/Gifts.md"), isExtra = true)
        assertEquals(
            listOf(folderNote, nested),
            mergeFolderAndExtraNotes(listOf(folderNote), listOf(nested))
        )
    }

    @Test
    fun `picking a file the folder already shows is recognised`() {
        val folderNotes = listOf(folderNoteUri("Projects.md"), folderNoteUri("Gifts.md"))
        assertTrue(isAlreadyAFolderNote(pickedUri("Documents/MyNotes/Gifts.md"), folderNotes))
    }

    @Test
    fun `picking a file from elsewhere or from a subfolder is allowed`() {
        val folderNotes = listOf(folderNoteUri("Gifts.md"))
        assertFalse(isAlreadyAFolderNote(pickedUri("Download/Gifts.md"), folderNotes))
        assertFalse(isAlreadyAFolderNote(pickedUri("Documents/MyNotes/Archive/Gifts.md"), folderNotes))
        assertFalse(isAlreadyAFolderNote(pickedUri("Documents/MyNotes/Gifts.md"), emptyList()))
    }

    @Test
    fun `a missing file still gets a name from a path-like document id`() {
        assertEquals("Gifts.md", fileNameFromDocumentUri(pickedUri("Documents/Gifts.md")))
        assertEquals("Gifts.md", fileNameFromDocumentUri(pickedUri("Gifts.md")))
    }

    @Test
    fun `an opaque document id gives no name rather than a number`() {
        assertNull(fileNameFromDocumentUri("content://com.android.providers.downloads.documents/document/msf%3A31"))
    }

    @Test
    fun `only md files count as markdown`() {
        assertTrue(isMarkdownFileName("Gifts.md"))
        assertTrue(isMarkdownFileName("Gifts.MD"))
        assertFalse(isMarkdownFileName("Gifts.txt"))
        assertFalse(isMarkdownFileName("md"))
        assertFalse(isMarkdownFileName(null))
    }

    private fun note(uri: String, isExtra: Boolean = false) = NoteSummary(
        id = uri,
        title = "",
        preview = "",
        lastModified = 0L,
        fileUri = uri,
        fileName = "",
        isExtra = isExtra
    )
}
