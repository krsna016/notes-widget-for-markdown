package com.marfolog.noteswidgetformarkdown.presentation.widget

import com.marfolog.noteswidgetformarkdown.domain.model.NoteSummary
import com.marfolog.noteswidgetformarkdown.util.UsageEvent

/** What [TrampolineActivity] needs to open a note — only plain fields, never an intent. */
internal data class NoteOpenRequest(
    val note: NoteSummary,
    val vaultName: String?,
    val noteFolderPath: String?
) {
    fun toExtras(): List<Pair<String, Any?>> = listOf(
        EXTRA_FILE_URI to note.fileUri,
        EXTRA_FILE_NAME to note.fileName,
        EXTRA_TITLE to note.title,
        EXTRA_IS_EXTRA to note.isExtra,
        EXTRA_VAULT_NAME to vaultName,
        EXTRA_NOTE_FOLDER_PATH to noteFolderPath
    )

    companion object {
        private const val EXTRA_FILE_URI = "file_uri"
        private const val EXTRA_FILE_NAME = "file_name"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_IS_EXTRA = "is_extra"
        private const val EXTRA_VAULT_NAME = "vault_name"
        private const val EXTRA_NOTE_FOLDER_PATH = "note_folder_path"

        /** Null when there is no file to open. */
        fun fromExtras(string: (String) -> String?, boolean: (String) -> Boolean): NoteOpenRequest? {
            val fileUri = string(EXTRA_FILE_URI) ?: return null
            return NoteOpenRequest(
                note = NoteSummary(
                    id = fileUri,
                    title = string(EXTRA_TITLE).orEmpty(),
                    preview = "",
                    lastModified = 0L,
                    fileUri = fileUri,
                    fileName = string(EXTRA_FILE_NAME).orEmpty(),
                    isExtra = boolean(EXTRA_IS_EXTRA)
                ),
                vaultName = string(EXTRA_VAULT_NAME),
                noteFolderPath = string(EXTRA_NOTE_FOLDER_PATH)
            )
        }
    }
}

internal fun noteOpenedEvent(openWith: OpenWith): Pair<String, String> =
    UsageEvent.PARAM_TARGET to openWith.storageValue
