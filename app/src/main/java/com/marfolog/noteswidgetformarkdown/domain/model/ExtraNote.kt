package com.marfolog.noteswidgetformarkdown.domain.model

/** One entry of the extra notes list. [note] is null when the file is gone or access was lost. */
data class ExtraNote(
    val fileUri: String,
    val note: NoteSummary?
) {
    val isAvailable: Boolean get() = note != null
}
