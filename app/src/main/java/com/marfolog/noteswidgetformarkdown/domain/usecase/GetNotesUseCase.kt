package com.marfolog.noteswidgetformarkdown.domain.usecase

import com.marfolog.noteswidgetformarkdown.domain.model.NoteSummary
import com.marfolog.noteswidgetformarkdown.domain.repository.FileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetNotesUseCase(private val fileRepository: FileRepository) {

    operator fun invoke(folderUri: String, extraFileUris: List<String> = emptyList()): Flow<List<NoteSummary>> {
        return fileRepository.getNotes(folderUri, extraFileUris).map { notes ->
            notes.sortedByDescending { it.lastModified }
        }
    }
}
