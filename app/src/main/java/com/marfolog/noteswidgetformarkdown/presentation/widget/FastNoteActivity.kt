package com.marfolog.noteswidgetformarkdown.presentation.widget

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import com.marfolog.noteswidgetformarkdown.R
import com.marfolog.noteswidgetformarkdown.data.preferences.NoteCardSettingsStore
import com.marfolog.noteswidgetformarkdown.domain.model.FastNoteStyle
import com.marfolog.noteswidgetformarkdown.domain.model.NoteSummary
import com.marfolog.noteswidgetformarkdown.domain.usecase.AppendFastNoteUseCase
import com.marfolog.noteswidgetformarkdown.domain.usecase.GetNotesUseCase
import com.marfolog.noteswidgetformarkdown.presentation.setup.SetupActivity
import com.marfolog.noteswidgetformarkdown.ui.theme.NotesWidgetForMarkdownTheme
import com.marfolog.noteswidgetformarkdown.util.AppLog
import com.marfolog.noteswidgetformarkdown.util.RatingPrompt
import com.marfolog.noteswidgetformarkdown.util.Telemetry
import com.marfolog.noteswidgetformarkdown.util.UsageEvent
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.get

class FastNoteActivity : ComponentActivity() {

    /** Off once the line is saved, so a rating dialog from Play is not drawn over a stale form. */
    private var dialogVisible by mutableStateOf(true)
    private var targetUri by mutableStateOf<String?>(null)
    private var targetName by mutableStateOf<String?>(null)
    private var folderNotes by mutableStateOf<Result<List<NoteSummary>>?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences(SetupActivity.PREFS_NAME, MODE_PRIVATE)
        val notesFolderUri = prefs.getString(SetupActivity.KEY_NOTES_URI, null)
        targetUri = prefs.getString(SetupActivity.KEY_FAST_NOTE_TARGET_URI, null)
        // Show where the line will land. The target is set once and then forgotten, so without
        // this the dialog asks you to type into an unknown file.
        targetName = targetUri
            ?.let { runCatching { DocumentFile.fromSingleUri(this, Uri.parse(it))?.name }.getOrNull() }

        if (targetUri.isNullOrBlank() && !notesFolderUri.isNullOrBlank()) {
            lifecycleScope.launch { folderNotes = loadFolderNotes(notesFolderUri) }
        }

        setContent {
            NotesWidgetForMarkdownTheme {
                if (dialogVisible) {
                    when (val step = fastNoteStep(targetUri, notesFolderUri, folderNotes)) {
                        FastNoteStep.Write -> FastNoteDialog(
                            targetName = targetName,
                            onCancel = { finish() },
                            onSave = ::save
                        )
                        FastNoteStep.Loading -> LoadingDialog(onCancel = { finish() })
                        is FastNoteStep.PickTarget -> PickTargetDialog(
                            notes = step.notes,
                            onPick = ::pickTarget,
                            onCancel = { finish() }
                        )
                        FastNoteStep.NoNotesFolder -> NoTargetDialog(
                            messageRes = R.string.fast_note_no_folder,
                            onOpenSettings = ::openSettings,
                            onCancel = { finish() }
                        )
                        FastNoteStep.NoNotesInFolder -> NoTargetDialog(
                            messageRes = R.string.fast_note_folder_empty,
                            onOpenSettings = ::openSettings,
                            onCancel = { finish() }
                        )
                        FastNoteStep.NotesUnreadable -> NoTargetDialog(
                            messageRes = R.string.notes_load_failed,
                            onOpenSettings = ::openSettings,
                            onCancel = { finish() }
                        )
                    }
                }
            }
        }
    }

    /** Same notes, in the same order, as the target chips in settings. */
    private suspend fun loadFolderNotes(notesFolderUri: String): Result<List<NoteSummary>> = runCatching {
        val getNotesUseCase = get<GetNotesUseCase>(GetNotesUseCase::class.java)
        NoteCardSettingsStore(applicationContext)
            .applyOrder(getNotesUseCase(notesFolderUri).firstOrNull().orEmpty())
    }.onFailure { AppLog.w(AREA, "Could not list notes to pick a fast note target", it) }

    private fun pickTarget(note: NoteSummary) {
        getSharedPreferences(SetupActivity.PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putString(SetupActivity.KEY_FAST_NOTE_TARGET_URI, note.fileUri)
            .apply()
        Telemetry.event(UsageEvent.FAST_TARGET_SET)
        targetName = note.fileName
        targetUri = note.fileUri
    }

    private fun openSettings() {
        startActivity(SetupActivity.intent(this, UsageEvent.FROM_FAST_NOTE_ERROR))
        finish()
    }

    private fun save(noteText: String, onError: (String) -> Unit) {
        val target = targetUri
        if (target.isNullOrBlank()) {
            onError(getString(R.string.fast_note_pick_target_first))
            return
        }
        val style = FastNoteStyle.from(
            getSharedPreferences(SetupActivity.PREFS_NAME, MODE_PRIVATE)
                .getString(SetupActivity.KEY_FAST_NOTE_STYLE, null)
        )
        lifecycleScope.launch {
            val appendFastNoteUseCase = get<AppendFastNoteUseCase>(AppendFastNoteUseCase::class.java)
            appendFastNoteUseCase(target, noteText, style)
                .onSuccess { saved ->
                    if (saved) {
                        Telemetry.event(UsageEvent.FAST_NOTE_SAVED, UsageEvent.PARAM_STYLE to style.storageValue)
                        Toast.makeText(
                            this@FastNoteActivity,
                            getString(R.string.fast_note_saved),
                            Toast.LENGTH_SHORT
                        ).show()
                        // Use applicationContext: the widget render is async and outlives this
                        // Activity, which finish() destroys.
                        NotesWidget.updateAll(applicationContext)
                        // A note just saved is a fair moment to ask. Not time yet returns at once,
                        // so this closes as fast as it always did; otherwise finish waits for Play.
                        dialogVisible = false
                        RatingPrompt.maybeAsk(this@FastNoteActivity)
                        finish()
                    } else {
                        onError(getString(R.string.fast_note_write_failed))
                    }
                }
                .onFailure { error ->
                    onError(error.message ?: getString(R.string.fast_note_failed))
                }
        }
    }

    private companion object {
        const val AREA = "FastNote"
    }
}

@Composable
private fun LoadingDialog(onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.fast_note_pick_title)) },
        text = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        },
        confirmButton = {
            TextButton(onClick = onCancel) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

@Composable
private fun PickTargetDialog(
    notes: List<NoteSummary>,
    onPick: (NoteSummary) -> Unit,
    onCancel: () -> Unit
) {
    Dialog(onDismissRequest = onCancel) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(vertical = 24.dp)) {
                Text(
                    text = stringResource(R.string.fast_note_pick_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.fast_note_pick_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    notes.forEach { note -> TargetRow(note.fileName, onClick = { onPick(note) }) }
                }
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(horizontal = 12.dp)
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        }
    }
}

@Composable
private fun TargetRow(fileName: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Description,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = fileName,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun NoTargetDialog(messageRes: Int, onOpenSettings: () -> Unit, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.fast_note_target_missing)) },
        text = { Text(stringResource(messageRes)) },
        confirmButton = {
            TextButton(onClick = onOpenSettings) {
                Text(stringResource(R.string.action_open_settings_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
private fun FastNoteDialog(
    targetName: String?,
    onCancel: () -> Unit,
    onSave: (String, (String) -> Unit) -> Unit
) {
    var noteText by rememberSaveable { mutableStateOf("") }
    var isSaving by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = {
            if (!isSaving) {
                onCancel()
            }
        },
        title = {
            Column {
                Text(stringResource(R.string.fast_note_title))
                targetName?.let { name ->
                    Text(
                        text = stringResource(R.string.fast_note_adds_line_to, name),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            val focusRequester = remember { FocusRequester() }
            val keyboard = LocalSoftwareKeyboardController.current

            // The whole point of a fast note is typing straight away — opening the dialog and then
            // tapping the field first defeats it.
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
                keyboard?.show()
            }

            OutlinedTextField(
                value = noteText,
                onValueChange = {
                    noteText = it
                    errorMessage = null
                },
                label = { Text(stringResource(R.string.fast_note_field_label)) },
                // Only speak up when something went wrong. The old hint explained the sync model
                // to someone who just wants to jot a line down.
                supportingText = errorMessage?.let { message -> { Text(message) } },
                isError = errorMessage != null,
                minLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
        },
        confirmButton = {
            TextButton(
                enabled = noteText.isNotBlank() && !isSaving,
                onClick = {
                    isSaving = true
                    onSave(noteText) { message ->
                        errorMessage = message
                        isSaving = false
                    }
                }
            ) {
                Text(stringResource(if (isSaving) R.string.fast_note_saving else R.string.fast_note_add))
            }
        },
        dismissButton = {
            TextButton(
                enabled = !isSaving,
                onClick = onCancel
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
