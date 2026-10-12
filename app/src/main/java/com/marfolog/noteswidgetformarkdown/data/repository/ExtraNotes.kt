package com.marfolog.noteswidgetformarkdown.data.repository

import com.marfolog.noteswidgetformarkdown.domain.model.NoteSummary
import java.net.URLDecoder

/*
 * Extra notes are single Markdown files the user picks one by one, on top of the notes folder.
 * Everything here is plain string work with no Android types, so it runs as a JVM unit test.
 */

/**
 * Which file a SAF uri points at, independent of how it was reached.
 *
 * The same file has two different uris: the folder listing hands out
 * `…/tree/<folder id>/document/<file id>`, while the single-file picker hands out
 * `…/document/<file id>`. What they share is the provider and the document id, so that is what
 * gets compared. Null when the uri is not a document uri at all.
 */
internal fun documentIdentity(uri: String): String? {
    val afterScheme = uri.substringAfter("://", missingDelimiterValue = "")
    val authority = afterScheme.substringBefore('/')
    val encodedId = afterScheme
        .substringAfterLast("/document/", missingDelimiterValue = "")
        .substringBefore('?')
    if (authority.isEmpty() || encodedId.isEmpty()) return null
    // URLDecoder is a form decoder and would turn a literal '+' into a space. Uri.encode never
    // leaves one unescaped, but a hand-built uri might, so keep it a plus.
    val documentId = runCatching {
        URLDecoder.decode(encodedId.replace("+", "%2B"), Charsets.UTF_8.name())
    }.getOrNull() ?: return null
    return "$authority/$documentId"
}

/**
 * Folder notes first, then the extra notes that are not already among them.
 *
 * A file the user added separately can also sit in the notes folder. It is shown once, as the
 * folder note, so it keeps the folder note's appearance and its delete behaviour.
 */
internal fun mergeFolderAndExtraNotes(
    folderNotes: List<NoteSummary>,
    extraNotes: List<NoteSummary>
): List<NoteSummary> {
    val seen = folderNotes.mapTo(mutableSetOf()) { documentIdentity(it.fileUri) ?: it.fileUri }
    return folderNotes + extraNotes.filter { seen.add(documentIdentity(it.fileUri) ?: it.fileUri) }
}

/**
 * Whether a freshly picked file is already on the widget as a folder note.
 *
 * Such a file would be listed once, as the folder note, so "remove from widget" could never make
 * it go away — it is refused at the picker instead. Checked against the folder listing rather
 * than the path: the listing is what the widget actually shows (an Obsidian folder note such as
 * `Projects/Projects.md` sits in the folder but is hidden from it, and can still be added).
 */
internal fun isAlreadyAFolderNote(pickedUri: String, folderNoteUris: List<String>): Boolean {
    val picked = documentIdentity(pickedUri) ?: return pickedUri in folderNoteUris
    return folderNoteUris.any { documentIdentity(it) == picked }
}

/**
 * A name to show for an extra note that can no longer be opened, read from the uri itself.
 *
 * Works for the external storage provider, whose document ids are paths
 * (`primary:Documents/Gifts.md`). Other providers use opaque ids (`msf:31`), and those give null
 * rather than a meaningless number.
 */
internal fun fileNameFromDocumentUri(uri: String): String? {
    val identity = documentIdentity(uri) ?: return null
    val name = identity.substringAfterLast('/').substringAfter(':')
    return name.takeIf { it.contains('.') }
}

/** Same rule the folder listing uses, so an extra note is exactly what a folder note would be. */
internal fun isMarkdownFileName(name: String?): Boolean =
    name?.endsWith(".md", ignoreCase = true) == true
