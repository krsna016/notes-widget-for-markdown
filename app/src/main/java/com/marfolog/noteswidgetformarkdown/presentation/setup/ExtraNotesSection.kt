package com.marfolog.noteswidgetformarkdown.presentation.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.marfolog.noteswidgetformarkdown.R
import com.marfolog.noteswidgetformarkdown.ui.theme.NotesWidgetForMarkdownTheme

/** One row of the extra notes list. [name] is null when not even the uri gives one away. */
internal data class ExtraNoteEntry(
    val fileUri: String,
    val name: String?,
    val isAvailable: Boolean
)

/**
 * Single files shown on top of the notes folder. Purely optional, so it stays a short list with
 * one button under it — nothing here is needed for the widget to work.
 */
@Composable
internal fun ExtraNotesSection(
    entries: List<ExtraNoteEntry>,
    onAddFile: () -> Unit,
    onRemove: (ExtraNoteEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.extra_notes_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.extra_notes_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (entries.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                entries.forEach { entry ->
                    // Swipe either way, same as a card below; the icon stays for anyone who
                    // doesn't think to swipe. Both only ask first, never delete.
                    SwipeToDelete(
                        onDelete = { onRemove(entry) },
                        hintOffset = 0f,
                        removesFromWidgetOnly = true
                    ) {
                        ExtraNoteRow(entry = entry, onRemove = { onRemove(entry) })
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        OutlinedButton(onClick = onAddFile, modifier = Modifier.fillMaxWidth()) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(text = stringResource(R.string.extra_notes_add))
        }
    }
}

@Composable
private fun ExtraNoteRow(entry: ExtraNoteEntry, onRemove: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.name ?: stringResource(R.string.extra_notes_unknown_name),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!entry.isAvailable) {
                    Text(
                        text = stringResource(R.string.extra_notes_unavailable),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            // Deliberately not the trash can the folder notes use: this never deletes anything.
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.RemoveCircleOutline,
                    contentDescription = stringResource(R.string.card_remove_from_widget),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * The counterpart of the delete dialog for folder notes. Worded so nobody can read it as
 * deleting: the file stays exactly where it is.
 */
@Composable
internal fun RemoveExtraNoteDialog(
    name: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(imageVector = Icons.Default.RemoveCircleOutline, contentDescription = null) },
        title = {
            Text(
                stringResource(
                    R.string.remove_extra_note_title,
                    name ?: stringResource(R.string.extra_notes_unknown_name)
                )
            )
        },
        text = { Text(stringResource(R.string.remove_extra_note_body)) },
        confirmButton = {
            Button(onClick = onConfirm) { Text(stringResource(R.string.action_remove_from_widget)) }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

@Preview(showBackground = true, name = "Extra notes")
@Composable
private fun ExtraNotesSectionPreview() {
    NotesWidgetForMarkdownTheme(dynamicColor = false) {
        Surface {
            ExtraNotesSection(
                entries = listOf(
                    ExtraNoteEntry("content://preview/document/Gifts.md", "Gifts.md", isAvailable = true),
                    ExtraNoteEntry("content://preview/document/Projects.md", "Projects.md", isAvailable = false)
                ),
                onAddFile = {},
                onRemove = {},
                modifier = Modifier.padding(24.dp)
            )
        }
    }
}

@Preview(showBackground = true, name = "Extra notes - Dark - Empty")
@Composable
private fun ExtraNotesSectionEmptyDarkPreview() {
    NotesWidgetForMarkdownTheme(dynamicColor = false, darkTheme = true) {
        Surface {
            ExtraNotesSection(
                entries = emptyList(),
                onAddFile = {},
                onRemove = {},
                modifier = Modifier.padding(24.dp)
            )
        }
    }
}

@Preview(showBackground = true, name = "Remove extra note dialog")
@Composable
private fun RemoveExtraNoteDialogPreview() {
    NotesWidgetForMarkdownTheme(dynamicColor = false) {
        RemoveExtraNoteDialog(name = "Gifts.md", onConfirm = {}, onDismiss = {})
    }
}
