package com.marfolog.noteswidgetformarkdown.presentation.widget

import com.marfolog.noteswidgetformarkdown.domain.model.NoteSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NoteOpenRequestTest {

    private val groceries = NoteSummary(
        id = "content://provider/tree/primary%3AMyNotes/document/primary%3AMyNotes%2FGroceries.md",
        title = "Groceries",
        preview = "- milk",
        lastModified = 1_760_000_000_000L,
        fileUri = "content://provider/tree/primary%3AMyNotes/document/primary%3AMyNotes%2FGroceries.md",
        fileName = "Groceries.md",
        isExtra = true
    )

    private fun roundTrip(request: NoteOpenRequest): NoteOpenRequest? {
        val extras = request.toExtras().toMap()
        return NoteOpenRequest.fromExtras(
            string = { extras[it] as String? },
            boolean = { extras[it] as Boolean? ?: false }
        )
    }

    @Test
    fun `every field a tap needs survives the trip through the extras`() {
        val restored = roundTrip(NoteOpenRequest(groceries, vaultName = "MyNotes", noteFolderPath = "Lists"))!!

        assertEquals("MyNotes", restored.vaultName)
        assertEquals("Lists", restored.noteFolderPath)
        assertEquals(groceries.fileUri, restored.note.fileUri)
        assertEquals(groceries.fileUri, restored.note.id)
        assertEquals("Groceries", restored.note.title)
        assertEquals("Groceries.md", restored.note.fileName)
        assertEquals(true, restored.note.isExtra)
    }

    @Test
    fun `an extra note outside the vault keeps its missing folder`() {
        val restored = roundTrip(NoteOpenRequest(groceries, vaultName = null, noteFolderPath = null))!!

        assertNull(restored.vaultName)
        assertNull(restored.noteFolderPath)
    }

    @Test
    fun `without a file uri nothing is opened`() {
        assertNull(NoteOpenRequest.fromExtras(string = { null }, boolean = { false }))
        assertNull(NoteOpenRequest.fromExtras(string = { if (it == "title") "Groceries" else null }, boolean = { true }))
    }

    @Test
    fun `note opened reports the open-with setting under fixed names`() {
        assertEquals("target" to "obsidian", noteOpenedEvent(OpenWith.Obsidian))
        assertEquals("target" to "default", noteOpenedEvent(OpenWith.DefaultApp))
        assertEquals("target" to "ask", noteOpenedEvent(OpenWith.AskEveryTime))
    }
}
