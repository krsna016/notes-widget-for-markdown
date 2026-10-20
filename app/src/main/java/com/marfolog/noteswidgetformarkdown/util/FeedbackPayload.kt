package com.marfolog.noteswidgetformarkdown.util

/**
 * The one document the Play build sends when feedback is submitted. Field names match
 * `firestore.rules` in the repository root; the server adds the time itself.
 */
internal object FeedbackPayload {

    const val FIELD_APP_VERSION = "appVersion"
    const val FIELD_VERSION_CODE = "versionCode"
    const val FIELD_CREATED_AT = "createdAt"
    const val FIELD_THUMBS = "thumbs"
    const val FIELD_IDEAS = "ideas"
    const val FIELD_TEXT = "text"

    fun of(form: FeedbackForm, versionName: String, versionCode: Int): Map<String, Any?> = mapOf(
        FIELD_APP_VERSION to versionName,
        FIELD_VERSION_CODE to versionCode,
        FIELD_THUMBS to form.thumbs?.value,
        FIELD_IDEAS to form.orderedIdeas,
        FIELD_TEXT to form.trimmedText
    )
}
