package com.marfolog.noteswidgetformarkdown.presentation.setup

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.marfolog.noteswidgetformarkdown.util.FeedbackForm
import com.marfolog.noteswidgetformarkdown.util.FeedbackSendResult
import com.marfolog.noteswidgetformarkdown.util.FeedbackSender
import kotlinx.coroutines.launch

/**
 * Runs the send outside the dialog, so rotating the screen mid-send neither cancels it nor
 * re-enables Send while the first one is still on its way.
 */
internal class FeedbackSendViewModel(application: Application) : AndroidViewModel(application) {

    data class Outcome(val form: FeedbackForm, val result: FeedbackSendResult)

    var sending by mutableStateOf(false)
        private set

    var outcome by mutableStateOf<Outcome?>(null)
        private set

    fun send(form: FeedbackForm) {
        if (sending) return
        sending = true
        outcome = null
        viewModelScope.launch {
            outcome = Outcome(form, FeedbackSender.send(getApplication(), form))
            sending = false
        }
    }

    fun outcomeHandled() {
        outcome = null
    }
}
