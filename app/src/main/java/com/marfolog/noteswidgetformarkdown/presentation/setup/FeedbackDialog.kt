package com.marfolog.noteswidgetformarkdown.presentation.setup

import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.marfolog.noteswidgetformarkdown.R
import com.marfolog.noteswidgetformarkdown.util.FeedbackForm
import com.marfolog.noteswidgetformarkdown.util.FeedbackIdeas
import com.marfolog.noteswidgetformarkdown.util.FeedbackReminder
import com.marfolog.noteswidgetformarkdown.util.FeedbackSendResult
import com.marfolog.noteswidgetformarkdown.util.FeedbackSender
import com.marfolog.noteswidgetformarkdown.util.FeedbackThumbs
import com.marfolog.noteswidgetformarkdown.util.Telemetry
import com.marfolog.noteswidgetformarkdown.util.UsageEvent
import kotlinx.coroutines.launch

private val FeedbackFormSaver = listSaver<FeedbackForm, Any?>(
    save = { listOf(it.thumbs?.value, ArrayList(it.ideas), it.text) },
    restore = { saved ->
        @Suppress("UNCHECKED_CAST")
        FeedbackForm(
            thumbs = FeedbackThumbs.from(saved[0] as String?),
            ideas = (saved[1] as List<String>).toSet(),
            text = saved[2] as String
        )
    }
)

/** Holds the form across rotation and carries out the three ways out of the dialog. */
@Composable
internal fun FeedbackDialogHost(onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var form by rememberSaveable(stateSaver = FeedbackFormSaver) { mutableStateOf(FeedbackForm()) }
    var errorRes by rememberSaveable { mutableStateOf<Int?>(null) }
    var sending by remember { mutableStateOf(false) }

    fun close(choice: String) {
        Telemetry.event(UsageEvent.FEEDBACK_DIALOG_CHOICE, UsageEvent.PARAM_CHOICE to choice)
        onClose()
    }

    FeedbackDialog(
        form = form,
        onFormChanged = {
            form = it
            errorRes = null
        },
        sending = sending,
        errorRes = errorRes,
        footerRes = FeedbackSender.FOOTER_RES,
        onSend = {
            sending = true
            errorRes = null
            scope.launch {
                when (FeedbackSender.send(context, form)) {
                    FeedbackSendResult.Sent -> {
                        UsageEvent.feedbackSent(form).forEach { (name, param) -> Telemetry.event(name, param) }
                        Toast.makeText(context, R.string.feedback_thanks, Toast.LENGTH_SHORT).show()
                        onClose()
                    }
                    FeedbackSendResult.Failed -> errorRes = FeedbackSender.FAILED_RES
                    FeedbackSendResult.TooSoon -> errorRes = R.string.feedback_too_soon
                }
                sending = false
            }
        },
        onLater = {
            FeedbackReminder.markSnoozed(context, System.currentTimeMillis())
            close(UsageEvent.CHOICE_LATER)
        },
        onNever = {
            FeedbackReminder.markNeverAgain(context)
            close(UsageEvent.CHOICE_NEVER)
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FeedbackDialog(
    form: FeedbackForm,
    onFormChanged: (FeedbackForm) -> Unit,
    sending: Boolean,
    @StringRes errorRes: Int?,
    @StringRes footerRes: Int,
    onSend: () -> Unit,
    onLater: () -> Unit,
    onNever: () -> Unit
) {
    AlertDialog(
        // Only back closes it: a stray tap beside the dialog would throw away what was typed.
        onDismissRequest = { if (!sending) onLater() },
        properties = DialogProperties(dismissOnClickOutside = false),
        title = { Text(stringResource(R.string.feedback_title)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.feedback_body))
                Spacer(modifier = Modifier.height(16.dp))

                Text(stringResource(R.string.feedback_how), style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThumbChip(FeedbackThumbs.Up, "👍", R.string.feedback_thumbs_up, form, onFormChanged)
                    ThumbChip(FeedbackThumbs.Down, "👎", R.string.feedback_thumbs_down, form, onFormChanged)
                }
                Spacer(modifier = Modifier.height(12.dp))

                Text(stringResource(R.string.feedback_ideas), style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FeedbackIdeas.ALL.forEach { idea ->
                        FilterChip(
                            selected = idea.id in form.ideas,
                            onClick = { onFormChanged(form.withIdeaToggled(idea.id)) },
                            label = { Text(stringResource(idea.labelRes)) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = form.text,
                    onValueChange = { onFormChanged(form.withText(it)) },
                    label = { Text(stringResource(R.string.feedback_text)) },
                    supportingText = {
                        Text(
                            stringResource(R.string.feedback_text_counter, form.text.length, FeedbackForm.MAX_TEXT_LENGTH)
                        )
                    },
                    minLines = 2,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorRes != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(errorRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(footerRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onSend, enabled = form.canSend && !sending) {
                Text(stringResource(R.string.feedback_send))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onNever, enabled = !sending) { Text(stringResource(R.string.feedback_never)) }
                TextButton(onClick = onLater, enabled = !sending) { Text(stringResource(R.string.feedback_later)) }
            }
        }
    )
}

@Composable
private fun ThumbChip(
    thumbs: FeedbackThumbs,
    emoji: String,
    @StringRes descriptionRes: Int,
    form: FeedbackForm,
    onFormChanged: (FeedbackForm) -> Unit
) {
    val description = stringResource(descriptionRes)
    FilterChip(
        selected = form.thumbs == thumbs,
        onClick = { onFormChanged(form.withThumbs(thumbs)) },
        label = { Text(emoji, style = MaterialTheme.typography.titleMedium) },
        modifier = Modifier.semantics { contentDescription = description }
    )
}
