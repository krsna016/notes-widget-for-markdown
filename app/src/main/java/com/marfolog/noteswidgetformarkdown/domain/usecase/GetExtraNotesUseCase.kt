package com.marfolog.noteswidgetformarkdown.domain.usecase

import com.marfolog.noteswidgetformarkdown.domain.model.ExtraNote
import com.marfolog.noteswidgetformarkdown.domain.repository.FileRepository

/** Every extra note in the order it was added, including the ones that can no longer be read. */
class GetExtraNotesUseCase(private val fileRepository: FileRepository) {

    suspend operator fun invoke(fileUris: List<String>): List<ExtraNote> =
        fileUris.map { fileUri ->
            ExtraNote(fileUri = fileUri, note = fileRepository.getExtraNote(fileUri).getOrNull())
        }
}
