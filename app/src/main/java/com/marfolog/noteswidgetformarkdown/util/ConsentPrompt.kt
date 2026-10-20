package com.marfolog.noteswidgetformarkdown.util

/**
 * When settings ask whether anonymous stats may be sent. Not on first launch: only once the widget
 * is set up — a notes folder picked and a widget on the home screen — or on the visit after the
 * guide was skipped. Asked once, and an answer either way is final. Which dialog goes first and how
 * often is up to [PromptBudget].
 */
object ConsentPrompt {

    fun isDue(
        canReport: Boolean,
        alreadyAsked: Boolean,
        notesFolderChosen: Boolean,
        widgetsPlaced: Int,
        guideSkipped: Boolean
    ): Boolean {
        if (!canReport || alreadyAsked) return false
        return guideSkipped || (notesFolderChosen && widgetsPlaced > 0)
    }
}
