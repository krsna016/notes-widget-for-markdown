package com.marfolog.noteswidgetformarkdown.domain.model

data class NoteSummary(
    val id: String,
    val title: String,
    val preview: String,
    val lastModified: Long,
    val fileUri: String,
    val fileName: String,
    /**
     * Added on its own from outside the notes folder. Removing such a note only takes it off the
     * widget; the file itself is never deleted.
     */
    val isExtra: Boolean = false
)
