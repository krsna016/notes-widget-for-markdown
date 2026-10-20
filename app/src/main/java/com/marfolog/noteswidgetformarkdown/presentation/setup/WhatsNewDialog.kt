package com.marfolog.noteswidgetformarkdown.presentation.setup

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.marfolog.noteswidgetformarkdown.BuildConfig
import com.marfolog.noteswidgetformarkdown.R
import com.marfolog.noteswidgetformarkdown.util.RateReminder
import com.marfolog.noteswidgetformarkdown.util.RatingPrompt
import com.marfolog.noteswidgetformarkdown.util.Telemetry
import com.marfolog.noteswidgetformarkdown.util.UsageEvent
import com.marfolog.noteswidgetformarkdown.util.WhatsNew
import com.marfolog.noteswidgetformarkdown.util.WhatsNewIcon
import com.marfolog.noteswidgetformarkdown.util.WhatsNewItem

/** Carries out the three ways out of the "What's new" dialog. */
@Composable
internal fun WhatsNewDialogHost(onClose: () -> Unit) {
    val context = LocalContext.current
    val release = WhatsNew.release(BuildConfig.VERSION_NAME)

    fun close(choice: String) {
        Telemetry.event(
            UsageEvent.WHATS_NEW_CHOICE,
            UsageEvent.PARAM_CHOICE to choice,
            UsageEvent.PARAM_VERSION to release
        )
        onClose()
    }

    WhatsNewDialog(
        release = release,
        items = WhatsNew.itemsFor(release),
        rateButtonRes = RatingPrompt.RATE_BUTTON_RES,
        footerRes = RatingPrompt.WHATS_NEW_FOOTER_RES,
        onRate = {
            RateReminder.markRateTapped(context)
            RatingPrompt.openRatePage(context)
            close(UsageEvent.CHOICE_RATE)
        },
        onLater = { close(UsageEvent.CHOICE_LATER) },
        onDismiss = { close(UsageEvent.CHOICE_DISMISSED) }
    )
}

@Composable
private fun WhatsNewDialog(
    release: String,
    items: List<WhatsNewItem>,
    @StringRes rateButtonRes: Int,
    @StringRes footerRes: Int,
    onRate: () -> Unit,
    onLater: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.whats_new_title, release)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items.forEach { item -> WhatsNewRow(item) }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(footerRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onRate) { Text(stringResource(rateButtonRes)) }
        },
        dismissButton = {
            TextButton(onClick = onLater) { Text(stringResource(R.string.whats_new_later)) }
        }
    )
}

@Composable
private fun WhatsNewRow(item: WhatsNewItem) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = item.icon.vector(),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Text(stringResource(item.textRes), style = MaterialTheme.typography.bodyMedium)
    }
}

private fun WhatsNewIcon.vector(): ImageVector = when (this) {
    WhatsNewIcon.Palette -> Icons.Outlined.Palette
    WhatsNewIcon.Checklist -> Icons.Outlined.Checklist
    WhatsNewIcon.Chat -> Icons.Outlined.ChatBubbleOutline
}
