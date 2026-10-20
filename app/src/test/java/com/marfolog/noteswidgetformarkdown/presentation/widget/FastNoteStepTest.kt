package com.marfolog.noteswidgetformarkdown.presentation.widget

import com.marfolog.noteswidgetformarkdown.domain.model.NoteSummary
import java.io.FileNotFoundException
import org.junit.Assert.assertEquals
import org.junit.Test

class FastNoteStepTest {

    private val folder = "content://com.android.externalstorage.documents/tree/primary%3ADocuments%2FMyNotes"
    private val groceries = note("Groceries")
    private val projects = note("Projects")

    private fun note(title: String, isExtra: Boolean = false) = NoteSummary(
        id = "$folder/document/$title",
        title = title,
        preview = "",
        lastModified = 1_760_000_000_000L,
        fileUri = "$folder/document/$title.md",
        fileName = "$title.md",
        isExtra = isExtra
    )

    @Test
    fun `with a target set the dialog goes straight to writing`() {
        assertEquals(FastNoteStep.Write, fastNoteStep(groceries.fileUri, true, folder, null))
        assertEquals(FastNoteStep.Write, fastNoteStep(groceries.fileUri, true, null, null))
    }

    @Test
    fun `without a target the folder notes are offered to pick from`() {
        assertEquals(
            FastNoteStep.PickTarget(listOf(groceries, projects)),
            fastNoteStep(null, null, folder, Result.success(listOf(groceries, projects)))
        )
    }

    @Test
    fun `a blank stored target counts as no target`() {
        assertEquals(
            FastNoteStep.PickTarget(listOf(groceries)),
            fastNoteStep("  ", null, folder, Result.success(listOf(groceries)))
        )
    }

    @Test
    fun `extra notes are not offered as a target, same as in settings`() {
        val gifts = note("Gifts", isExtra = true)
        assertEquals(
            FastNoteStep.PickTarget(listOf(projects)),
            fastNoteStep(null, null, folder, Result.success(listOf(gifts, projects)))
        )
    }

    @Test
    fun `while the folder is still being read the dialog waits`() {
        assertEquals(FastNoteStep.Loading, fastNoteStep(null, null, folder, null))
    }

    @Test
    fun `no notes folder yet sends the user to settings`() {
        assertEquals(FastNoteStep.NoNotesFolder, fastNoteStep(null, null, null, null))
        assertEquals(FastNoteStep.NoNotesFolder, fastNoteStep(null, null, "", null))
    }

    @Test
    fun `a folder without notes says so`() {
        assertEquals(FastNoteStep.NoNotesInFolder, fastNoteStep(null, null, folder, Result.success(emptyList())))
    }

    @Test
    fun `a folder holding only extra notes has nothing to pick`() {
        assertEquals(
            FastNoteStep.NoNotesInFolder,
            fastNoteStep(null, null, folder, Result.success(listOf(note("Gifts", isExtra = true))))
        )
    }

    @Test
    fun `a folder that cannot be read says so`() {
        assertEquals(
            FastNoteStep.NotesUnreadable,
            fastNoteStep(null, null, folder, Result.failure(FileNotFoundException("gone")))
        )
    }

    @Test
    fun `a saved target that was deleted or renamed offers the folder notes`() {
        assertEquals(
            FastNoteStep.PickTarget(listOf(groceries, projects)),
            fastNoteStep("$folder/document/Old.md", false, folder, Result.success(listOf(groceries, projects)))
        )
    }

    @Test
    fun `a missing target while the folder is still being read waits`() {
        assertEquals(FastNoteStep.Loading, fastNoteStep("$folder/document/Old.md", false, folder, null))
    }

    @Test
    fun `a missing target without a notes folder sends the user to settings`() {
        assertEquals(FastNoteStep.NoNotesFolder, fastNoteStep("$folder/document/Old.md", false, null, null))
    }

    @Test
    fun `while the saved target is being checked the dialog waits`() {
        assertEquals(FastNoteStep.Loading, fastNoteStep(groceries.fileUri, null, folder, null))
        assertEquals(FastNoteStep.Loading, fastNoteStep(groceries.fileUri, null, null, null))
    }
}
