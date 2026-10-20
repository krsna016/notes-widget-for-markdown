package com.marfolog.noteswidgetformarkdown.util

enum class FeedbackThumbs(val value: String) {
    Up("up"),
    Down("down");

    companion object {
        fun from(value: String?): FeedbackThumbs? = entries.firstOrNull { it.value == value }
    }
}

/** What has been filled in the feedback dialog so far. Every part is optional. */
data class FeedbackForm(
    val thumbs: FeedbackThumbs? = null,
    val text: String = ""
) {

    val canSend: Boolean get() = thumbs != null || text.isNotBlank()

    val trimmedText: String get() = capText(text.trim())

    /** Tapping the selected thumb again takes the answer back. */
    fun withThumbs(choice: FeedbackThumbs): FeedbackForm =
        copy(thumbs = if (thumbs == choice) null else choice)

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
