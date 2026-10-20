package com.marfolog.noteswidgetformarkdown.util

import java.io.FileNotFoundException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class CrashRedactorTest {

    private val treeUri = "content://com.android.externalstorage.documents/tree/primary%3ADocuments%2FMyNotes" +
        "/document/primary%3ADocuments%2FMyNotes%2FGroceries.md"

    @Test
    fun `a content uri with encoded folder and note names is replaced`() {
        val cleaned = CrashRedactor.message("Failed to open $treeUri for writing")

        assertEquals("Failed to open <storage location> for writing", cleaned)
        assertFalse(cleaned.contains("MyNotes"))
        assertFalse(cleaned.contains("Groceries"))
    }

    @Test
    fun `file uris and absolute paths are replaced too`() {
        assertEquals("open <storage location>", CrashRedactor.message("open file:///sdcard/Projects/plan.md"))
        assertEquals(
            "<storage location>: open failed: ENOENT",
            CrashRedactor.message("/storage/emulated/0/MyNotes/Groceries.md: open failed: ENOENT")
        )
        assertEquals("at <storage location>", CrashRedactor.message("at /mnt/media_rw/1234-ABCD/MyNotes"))
        assertEquals("cache <storage location>", CrashRedactor.message("cache /data/user/0/app/files/x"))
    }

    @Test
    fun `a message without a location is unchanged`() {
        val text = "Index 3 out of bounds for length 2"

        assertEquals(text, CrashRedactor.message(text))
        assertEquals("", CrashRedactor.message(""))
    }

    @Test
    fun `a crash keeps its type in the text and its stack trace`() {
        val original = FileNotFoundException("No such file: $treeUri")

        val redacted = CrashRedactor.redact(original)

        assertEquals("java.io.FileNotFoundException: No such file: <storage location>", redacted.message)
        assertArrayEquals(original.stackTrace, redacted.stackTrace)
    }

    @Test
    fun `every cause in the chain is cleaned`() {
        val root = SecurityException("Permission Denial: reading $treeUri")
        val original = IllegalStateException("Could not refresh", root)

        val redacted = CrashRedactor.redact(original)

        assertEquals("java.lang.IllegalStateException: Could not refresh", redacted.message)
        val cause = redacted.cause!!
        assertEquals("java.lang.SecurityException: Permission Denial: reading <storage location>", cause.message)
        assertArrayEquals(root.stackTrace, cause.stackTrace)
        assertNull(cause.cause)
    }

    @Test
    fun `a crash without a message keeps just its type`() {
        assertEquals("java.lang.NullPointerException", CrashRedactor.redact(NullPointerException()).message)
    }

    @Test
    fun `cleaning twice changes nothing, so a non-fatal from the log is not wrapped again`() {
        val once = CrashRedactor.redact(RuntimeException("boom at $treeUri"))

        assertSame(once, CrashRedactor.redact(once))
    }

    @Test
    fun `a cause chain that loops back ends instead of running forever`() {
        val first = RuntimeException("first")
        val second = RuntimeException("second", first)
        first.initCause(second)

        val redacted = CrashRedactor.redact(first)

        assertTrue(redacted.message!!.endsWith("first"))
        assertTrue(redacted.cause!!.message!!.endsWith("second"))
        assertNull(redacted.cause!!.cause)
    }

    @Test
    fun `a path with spaces is replaced whole`() {
        val cleaned = CrashRedactor.message("/storage/emulated/0/My Notes/Weekly Groceries.md: open failed: ENOENT")

        assertEquals("<storage location>: open failed: ENOENT", cleaned)
    }

    @Test
    fun `a path with spaces ends at a quote or an opening parenthesis`() {
        assertEquals("cannot read '<storage location>' now", CrashRedactor.message("cannot read '/sdcard/My Notes/Big Plan.md' now"))
        assertEquals("<storage location> (Permission denied)", CrashRedactor.message("/data/user/0/app/My Files/x.md (Permission denied)"))
    }

    @Test
    fun `a path with spaces runs to the end of its line`() {
        assertEquals("open <storage location>\nnext line", CrashRedactor.message("open /mnt/media_rw/AB-12/My Notes/a b.md\nnext line"))
    }

    @Test
    fun `a very long path without a delimiter is handled in linear time`() {
        val text = "/storage/" + "a ".repeat(50_000)

        assertEquals("<storage location>", CrashRedactor.message(text))
    }

    @Test
    fun `redaction failure forwards the original throwable`() {
        val original = IllegalStateException("boom")

        assertSame(original, CrashRedactor.redactedOrOriginal(original) { throw IllegalArgumentException("redactor") })
    }

    @Test
    fun `redaction success forwards the redacted copy`() {
        val original = FileNotFoundException("/storage/emulated/0/MyNotes/Groceries.md: open failed")

        val forwarded = CrashRedactor.redactedOrOriginal(original)

        assertTrue(forwarded is CrashRedactor.RedactedThrowable)
        assertFalse(forwarded.message!!.contains("Groceries"))
    }

    @Test
    fun `forward hands the previous handler the redacted copy`() {
        val received = mutableListOf<Throwable>()
        val thread = Thread.currentThread()

        CrashRedactor.forward({ _, e -> received += e }, thread, FileNotFoundException("/sdcard/MyNotes/Groceries.md: gone"))

        assertEquals(1, received.size)
        assertFalse(received.single().message!!.contains("Groceries"))
    }

    @Test
    fun `forward still crashes with the original when the redactor fails`() {
        val received = mutableListOf<Throwable>()
        val original = RuntimeException("boom")

        CrashRedactor.forward({ _, e -> received += e }, Thread.currentThread(), original) { throw IllegalStateException() }

        assertSame(original, received.single())
    }

    @Test
    fun `forward without a previous handler does nothing`() {
        CrashRedactor.forward(null, Thread.currentThread(), RuntimeException("boom"))
    }
}
