package com.marfolog.noteswidgetformarkdown.data.parser

import com.marfolog.noteswidgetformarkdown.domain.model.FastNoteStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownFastNoteInserterTest {

    private val inserter = MarkdownFastNoteInserter()

    @Test
    fun insertsBulletBelowFirstTopLevelHeading() {
        val updated = inserter.insert(
            """
            # Inbox
            Existing text
            """.trimIndent(),
            "Buy milk"
        )

        assertEquals(
            """
            # Inbox
            - Buy milk
            Existing text
            """.trimIndent(),
            updated
        )
    }

    @Test
    fun insertsBulletAfterFrontmatterAndHeading() {
        val updated = inserter.insert(
            """
            ---
            tags:
              - inbox
            ---
            # Inbox
            Existing text
            """.trimIndent(),
            "Call bank"
        )

        assertEquals(
            """
            ---
            tags:
              - inbox
            ---
            # Inbox
            - Call bank
            Existing text
            """.trimIndent(),
            updated
        )
    }

    @Test
    fun insertsBulletAtTopWhenHeadingIsMissing() {
        val updated = inserter.insert(
            """
            Existing text
            - Existing item
            """.trimIndent(),
            "New idea"
        )

        assertEquals(
            """
            - New idea
            Existing text
            - Existing item
            """.trimIndent(),
            updated
        )
    }

    @Test
    fun trimsNoteTextBeforeInserting() {
        val updated = inserter.insert("# Inbox", "  Follow up  ")

        assertEquals(
            """
            # Inbox
            - Follow up
            """.trimIndent(),
            updated
        )
    }

    @Test
    fun rejectsBlankNoteText() {
        val result = runCatching {
            inserter.insert("# Inbox", "   ")
        }

        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `empty file becomes a single bullet`() {
        assertEquals("- First thought\n", inserter.insert("", "First thought"))
    }

    @Test
    fun `file with only frontmatter gets the bullet after it, not inside it`() {
        val updated = inserter.insert(
            """
            ---
            tags:
              - inbox
            ---
            """.trimIndent(),
            "New idea"
        )

        assertEquals(
            """
            ---
            tags:
              - inbox
            ---
            - New idea
            """.trimIndent(),
            updated
        )
    }

    @Test
    fun `unclosed frontmatter is treated as plain content`() {
        // A half-written frontmatter block has no safe insert point inside it; falling back
        // to the top of the file must not corrupt the YAML any further.
        val updated = inserter.insert("---\ntags: broken", "New idea")

        assertEquals("- New idea\n---\ntags: broken", updated)
    }

    @Test
    fun `heading-like line inside frontmatter does not attract the bullet`() {
        val updated = inserter.insert(
            """
            ---
            # a yaml comment, not a heading
            ---
            Existing text
            """.trimIndent(),
            "New idea"
        )

        assertEquals(
            """
            ---
            # a yaml comment, not a heading
            ---
            - New idea
            Existing text
            """.trimIndent(),
            updated
        )
    }

    @Test
    fun normalizesLineEndings() {
        val updated = inserter.insert("# Inbox\r\nExisting text", "Fast note")

        assertEquals(
            """
            # Inbox
            - Fast note
            Existing text
            """.trimIndent(),
            updated
        )
    }

    @Test
    fun `bullet style is the plain list item it always was`() {
        assertEquals(
            "# Inbox\n- Buy milk",
            inserter.insert("# Inbox", "Buy milk", FastNoteStyle.Bullet)
        )
    }

    @Test
    fun `checkbox style adds an open task below the first heading`() {
        val updated = inserter.insert(
            """
            # Groceries
            - [x] Bread
            """.trimIndent(),
            "Buy milk",
            FastNoteStyle.Checkbox
        )

        assertEquals(
            """
            # Groceries
            - [ ] Buy milk
            - [x] Bread
            """.trimIndent(),
            updated
        )
    }

    @Test
    fun `checkbox style lands after frontmatter and heading like a bullet does`() {
        val updated = inserter.insert(
            """
            ---
            tags:
              - todo
            ---
            # Projects
            Existing text
            """.trimIndent(),
            "Call the plumber",
            FastNoteStyle.Checkbox
        )

        assertEquals(
            """
            ---
            tags:
              - todo
            ---
            # Projects
            - [ ] Call the plumber
            Existing text
            """.trimIndent(),
            updated
        )
    }

    @Test
    fun `checkbox style goes to the top when there is no heading`() {
        assertEquals(
            "- [ ] New idea\nExisting text",
            inserter.insert("Existing text", "New idea", FastNoteStyle.Checkbox)
        )
    }

    @Test
    fun `checkbox style in an empty file becomes a single open task`() {
        assertEquals("- [ ] First thought\n", inserter.insert("", "  First thought ", FastNoteStyle.Checkbox))
    }

    @Test
    fun `checkbox style still refuses blank text`() {
        val result = runCatching { inserter.insert("# Inbox", " ", FastNoteStyle.Checkbox) }

        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }
}
