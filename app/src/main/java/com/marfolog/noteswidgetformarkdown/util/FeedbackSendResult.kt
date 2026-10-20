package com.marfolog.noteswidgetformarkdown.util

/** How a press of "Send" in the feedback dialog ended. */
enum class FeedbackSendResult {
    Sent,
    Failed,
    TooSoon,

    /** An earlier send timed out and may still arrive; wait before trying again. */
    Pending
}
