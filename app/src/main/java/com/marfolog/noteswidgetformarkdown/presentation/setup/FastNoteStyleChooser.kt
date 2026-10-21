package com.marfolog.noteswidgetformarkdown.presentation.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.marfolog.noteswidgetformarkdown.R
import com.marfolog.noteswidgetformarkdown.domain.model.FastNoteStyle

@Composable
fun FastNoteStyleChooser(selected: FastNoteStyle, onSelected: (FastNoteStyle) -> Unit) {
    Text(
        text = stringResource(R.string.fast_note_style_title),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(4.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FAST_NOTE_STYLE_CHOICES.forEach { (style, labelRes) ->
            FilterChip(
                selected = selected == style,
                onClick = { onSelected(style) },
                label = { Text(stringResource(labelRes)) }
            )
        }
    }
}

private val FAST_NOTE_STYLE_CHOICES = listOf(
    FastNoteStyle.Bullet to R.string.fast_note_style_bullet,
    FastNoteStyle.Checkbox to R.string.fast_note_style_checkbox
)
