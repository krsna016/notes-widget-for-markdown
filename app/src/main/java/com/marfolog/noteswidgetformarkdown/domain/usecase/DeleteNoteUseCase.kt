package com.marfolog.noteswidgetformarkdown.domain.usecase

import com.marfolog.noteswidgetformarkdown.domain.model.NoteSummary
import com.marfolog.noteswidgetformarkdown.domain.repository.FileRepository

class DeleteNoteUseCase(private val fileRepository: FileRepository) {

    /**
     * Only folder notes are ever deleted. An extra note is a file the user pointed the widget at
     * from somewhere else, and taking it off the widget must leave the file alone — so it is
     * refused here instead of trusting every caller to branch the right way.
     */
    suspend operator fun invoke(note: NoteSummary): Result<Boolean> {
        if (note.isExtra) {
            return Result.failure(
                IllegalArgumentException("Extra notes are removed from the widget, never deleted")
            )
        }
        return fileRepository.deleteNote(note.fileUri)
    }
}
