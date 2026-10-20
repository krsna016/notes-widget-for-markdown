package com.marfolog.noteswidgetformarkdown.util

/**
 * Cleans what could reach a crash report. A throwable's own message is not ours to trust: SAF
 * failures put the whole tree URI in it, which spells out the vault folder and the note file name.
 * The type and the stack trace are what make a report useful, so those are kept and only the text
 * is rewritten.
 */
object CrashRedactor {

    const val PLACEHOLDER = "<storage location>"

    // Plain paths too: providers and the framework hand those back in messages just as often.
    private val STORAGE_LOCATION = Regex(
        """((content|file)://\S*|(/storage/|/sdcard/|/mnt/|/data/)[^\n"']*?(?=: | \(|["']|$))""",
        RegexOption.MULTILINE
    )

    fun message(text: String): String = STORAGE_LOCATION.replace(text, PLACEHOLDER)

    /** The same failure with every message in its cause chain cleaned. */
    fun redact(error: Throwable): Throwable = redact(error, mutableSetOf())

    private fun redact(error: Throwable, seen: MutableSet<Throwable>): Throwable {
        if (error is RedactedThrowable) return error
        seen += error
        val cause = error.cause?.takeIf { it !in seen }?.let { redact(it, seen) }
        val text = error.message?.let { "${error.javaClass.name}: ${message(it)}" } ?: error.javaClass.name
        return RedactedThrowable(text, cause).also { it.stackTrace = error.stackTrace }
    }

    fun forward(
        previous: Thread.UncaughtExceptionHandler?,
        thread: Thread,
        error: Throwable,
        redactor: (Throwable) -> Throwable = ::redact
    ) {
        previous?.uncaughtException(thread, redactedOrOriginal(error, redactor))
    }

    fun redactedOrOriginal(error: Throwable, redactor: (Throwable) -> Throwable = ::redact): Throwable =
        try {
            redactor(error)
        } catch (_: Throwable) {
            error
        }

    /** Carries a cleaned message; the original type is kept at the start of the text. */
    class RedactedThrowable(message: String, cause: Throwable?) : Throwable(message, cause)
}
