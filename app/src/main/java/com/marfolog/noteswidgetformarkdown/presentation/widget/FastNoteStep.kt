package com.marfolog.noteswidgetformarkdown.presentation.widget

import com.marfolog.noteswidgetformarkdown.domain.model.NoteSummary

/** What the fast note dialog shows. */
internal sealed interface FastNoteStep {
    data object Write : FastNoteStep
    data object Loading : FastNoteStep
    data class PickTarget(val notes: List<NoteSummary>) : FastNoteStep
    data object NoNotesFolder : FastNoteStep
    data object NoNotesInFolder : FastNoteStep
    data object NotesUnreadable : FastNoteStep
}

/**
 * Without a usable target the dialog asks for one right there, from the same notes the settings
 * offer, instead of sending the user off to settings first. A saved target that no longer exists
 * or cannot be written counts as no target. [targetExists] and [folderNotes] are null while still
 * loading.
 */
internal fun fastNoteStep(
    targetUri: String?,
    targetExists: Boolean?,
    notesFolderUri: String?,
    folderNotes: Result<List<NoteSummary>>?
): FastNoteStep {
    if (!targetUri.isNullOrBlank()) {
        if (targetExists == null) return FastNoteStep.Loading
        if (targetExists) return FastNoteStep.Write
    }
    if (notesFolderUri.isNullOrBlank()) return FastNoteStep.NoNotesFolder
    if (folderNotes == null) return FastNoteStep.Loading
    val notes = folderNotes.getOrNull() ?: return FastNoteStep.NotesUnreadable
    val targets = notes.filterNot { it.isExtra }
    return if (targets.isEmpty()) FastNoteStep.NoNotesInFolder else FastNoteStep.PickTarget(targets)
}
