package com.marfolog.noteswidgetformarkdown.util

import androidx.annotation.StringRes
import com.marfolog.noteswidgetformarkdown.R

enum class FeedbackThumbs(val value: String) {
    Up("up"),
    Down("down");

    companion object {
        fun from(value: String?): FeedbackThumbs? = entries.firstOrNull { it.value == value }
    }
}

/**
 * The ideas the feedback dialog offers to tick. Change the list each release; the ids are what
 * gets sent, the labels are only shown.
 */
object FeedbackIdeas {

    data class Idea(val id: String, @StringRes val labelRes: Int)

    val ALL = listOf(
        Idea("pinned_note", R.string.feedback_idea_pinned_note),
        Idea("fast_note_time", R.string.feedback_idea_fast_note_time),
        Idea("multi_folder", R.string.feedback_idea_multi_folder),
        Idea("search", R.string.feedback_idea_search),
        Idea("monospace", R.string.feedback_idea_monospace),
        Idea("images", R.string.feedback_idea_images)
    )

    /** Ticked ids in the order the dialog lists them; anything not in the list is dropped. */
    fun ordered(ticked: Set<String>): List<String> = ALL.map { it.id }.filter { it in ticked }

    @StringRes
    fun labelRes(id: String): Int? = ALL.firstOrNull { it.id == id }?.labelRes
}

/** What has been filled in the feedback dialog so far. Every part is optional. */
data class FeedbackForm(
    val thumbs: FeedbackThumbs? = null,
    val ideas: Set<String> = emptySet(),
    val text: String = ""
) {

    val canSend: Boolean get() = thumbs != null || ideas.isNotEmpty() || text.isNotBlank()

    val orderedIdeas: List<String> get() = FeedbackIdeas.ordered(ideas)

    val trimmedText: String get() = capText(text.trim())

    /** Tapping the selected thumb again takes the answer back. */
    fun withThumbs(choice: FeedbackThumbs): FeedbackForm =
        copy(thumbs = if (thumbs == choice) null else choice)

    fun withIdeaToggled(id: String): FeedbackForm =
        copy(ideas = if (id in ideas) ideas - id else ideas + id)

    fun withText(value: String): FeedbackForm = copy(text = capText(value))

    companion object {
        const val MAX_TEXT_LENGTH = 1000

        /** Cut to [MAX_TEXT_LENGTH] without leaving half of an emoji at the end. */
        fun capText(value: String): String {
            if (value.length <= MAX_TEXT_LENGTH) return value
            val end = if (Character.isHighSurrogate(value[MAX_TEXT_LENGTH - 1])) MAX_TEXT_LENGTH - 1 else MAX_TEXT_LENGTH
            return value.substring(0, end)
        }
    }
}
