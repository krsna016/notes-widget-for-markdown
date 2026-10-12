package com.marfolog.noteswidgetformarkdown.data.preferences

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.marfolog.noteswidgetformarkdown.presentation.setup.SetupActivity
import com.marfolog.noteswidgetformarkdown.util.AppLog
import org.json.JSONArray

/**
 * Single Markdown files the user added to the widget on top of the notes folder, in the order
 * they were added.
 *
 * Only the uris are stored. The files themselves stay wherever they are, and removing one here
 * never touches the file — it only stops the widget showing it.
 */
class ExtraNotesStore(private val context: Context) {

    private val prefs = context.getSharedPreferences(SetupActivity.PREFS_NAME, Context.MODE_PRIVATE)

    fun getAll(): List<String> = parseUriList(prefs.getString(KEY_EXTRA_NOTES_JSON, null))

    /**
     * Keeps access to a file the picker just handed over and adds it to the list.
     *
     * Write access is asked for too: other editors get it when a note is opened, and without it
     * they could show the note but not save it. Some providers only grant read, so that is the
     * fallback. Returns false when the file was already on the list.
     */
    fun add(fileUri: String): Result<Boolean> = runCatching {
        val uri = Uri.parse(fileUri)
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }.recoverCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }.getOrThrow()

        val current = getAll()
        if (fileUri in current) {
            false
        } else {
            save(current + fileUri)
            AppLog.i(AREA, "Extra note added, ${current.size + 1} in total")
            true
        }
    }

    /**
     * Drops a file from the list and gives its access back. The file is not deleted.
     *
     * The git reader and the vault locator look through every folder we hold access to, so the
     * notes folder, the vault and the git root must never lose theirs — [canReleaseGrant] makes
     * sure only the file's own grant goes.
     */
    fun remove(fileUri: String) {
        save(getAll().filterNot { it == fileUri })
        val protectedUris = setOfNotNull(
            prefs.getString(SetupActivity.KEY_NOTES_URI, null),
            prefs.getString(SetupActivity.KEY_VAULT_URI, null),
            prefs.getString(SetupActivity.KEY_GIT_ROOT_URI, null)
        )
        if (canReleaseGrant(fileUri, protectedUris)) {
            val uri = Uri.parse(fileUri)
            runCatching {
                context.contentResolver.releasePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }.recoverCatching {
                context.contentResolver.releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }.onFailure { AppLog.w(AREA, "Could not release access to a removed extra note", it) }
        }
        AppLog.i(AREA, "Extra note removed from the widget, file left in place")
    }

    private fun save(fileUris: List<String>) {
        prefs.edit()
            .putString(KEY_EXTRA_NOTES_JSON, serializeUriList(fileUris))
            .commit()
    }

    companion object {
        private const val AREA = "ExtraNotes"
        const val KEY_EXTRA_NOTES_JSON = "extra_notes_json"
    }
}

/** Unreadable JSON means no extra notes rather than a crash; blanks and repeats are dropped. */
internal fun parseUriList(raw: String?): List<String> {
    if (raw.isNullOrBlank()) return emptyList()
    return runCatching {
        val array = JSONArray(raw)
        buildList {
            for (index in 0 until array.length()) {
                array.optString(index).takeIf { it.isNotBlank() }?.let { add(it) }
            }
        }.distinct()
    }.getOrElse { emptyList() }
}

internal fun serializeUriList(fileUris: List<String>): String {
    val array = JSONArray()
    fileUris.distinct().forEach { array.put(it) }
    return array.toString()
}

/**
 * Whether dropping an extra note may also give its access back.
 *
 * The picker only ever hands out plain document uris, so a tree uri here would be a folder grant
 * that ended up in the list by mistake — releasing it would cut the widget off from its folder.
 * Same for any uri the rest of the app still relies on.
 */
internal fun canReleaseGrant(fileUri: String, protectedUris: Set<String>): Boolean =
    fileUri !in protectedUris && !isTreeUriString(fileUri)

/** `content://<authority>/tree/…` — what DocumentsContract.isTreeUri checks, without Android. */
private fun isTreeUriString(uri: String): Boolean =
    uri.substringAfter("://").substringAfter('/').startsWith("tree/")
