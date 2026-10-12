package com.marfolog.noteswidgetformarkdown.data.repository

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Finds the real Obsidian vault among the folders the user granted us.
 *
 * `obsidian://open` needs the vault name and a path relative to the vault root. Users often
 * point the widget at a subfolder, whose name is not a vault — Obsidian then silently opens
 * whatever note was last open. The vault root is the folder holding `.obsidian`, so we look
 * for that instead of trusting the picked folder's name.
 */
class ObsidianVaultLocator(private val context: Context) {

    data class Vault(val name: String, val documentId: String, val authority: String? = null)

    suspend fun locate(): Vault? = withContext(Dispatchers.IO) {
        context.contentResolver.persistedUriPermissions
            // Extra notes are single-file grants. They cannot hold a vault, and fromTreeUri
            // throws on them.
            .filter { it.isReadPermission && DocumentsContract.isTreeUri(it.uri) }
            .firstNotNullOfOrNull { permission ->
                runCatching {
                    val tree = DocumentFile.fromTreeUri(context, permission.uri)
                        ?: return@runCatching null
                    val holdsVault = tree.findFile(OBSIDIAN_DIR)?.isDirectory == true
                    val name = tree.name
                    if (holdsVault && !name.isNullOrEmpty()) {
                        Vault(
                            name = name,
                            documentId = DocumentsContract.getTreeDocumentId(permission.uri),
                            authority = permission.uri.authority
                        )
                    } else {
                        null
                    }
                }.getOrElse { e ->
                    Log.w(TAG, "Could not inspect ${permission.uri}", e)
                    null
                }
            }
    }

    companion object {
        private const val TAG = "ObsidianVaultLocator"
        private const val OBSIDIAN_DIR = ".obsidian"

        /**
         * Path of the notes folder relative to the vault root, e.g. `Projects`.
         * Empty when the notes folder *is* the vault root; null when it lives outside it.
         */
        fun relativePath(vaultDocumentId: String, notesFolderUri: String?): String? {
            val notesDocId = runCatching {
                DocumentsContract.getTreeDocumentId(Uri.parse(notesFolderUri ?: return null))
            }.getOrNull() ?: return null

            return relativeDocumentPath(vaultDocumentId, notesDocId)
        }

        /**
         * Folder of a single note relative to the vault root, for an extra note that was added
         * on its own. Empty when it sits in the vault root; null when it lives outside the vault
         * or comes from another provider — then there is no deep link to build, and the note is
         * opened as a plain file instead.
         */
        fun folderOfNote(vault: Vault, noteUri: String): String? {
            val uri = runCatching { Uri.parse(noteUri) }.getOrNull() ?: return null
            if (vault.authority != null && uri.authority != vault.authority) return null
            val noteDocId = runCatching { DocumentsContract.getDocumentId(uri) }.getOrNull()
                ?: return null
            return relativeFolderOfDocument(vault.documentId, noteDocId)
        }

        /** [relativeDocumentPath] for a file, minus the file name. Split out for unit tests. */
        internal fun relativeFolderOfDocument(vaultDocumentId: String, fileDocumentId: String): String? =
            relativeDocumentPath(vaultDocumentId, fileDocumentId)
                ?.takeIf { it.isNotEmpty() }
                ?.substringBeforeLast('/', missingDelimiterValue = "")

        /**
         * The comparison itself, with no Android types in sight.
         *
         * Split out from [relativePath] so it can be unit tested: the wrapper only touches
         * `Uri` and `DocumentsContract`, which are stubs on the JVM, and a bad result here used
         * to send every widget tap to whichever note Obsidian had open last.
         */
        internal fun relativeDocumentPath(vaultDocumentId: String, notesDocumentId: String): String? =
            when {
                notesDocumentId == vaultDocumentId -> ""
                notesDocumentId.startsWith("$vaultDocumentId/") ->
                    notesDocumentId.removePrefix("$vaultDocumentId/")
                else -> null
            }
    }
}
