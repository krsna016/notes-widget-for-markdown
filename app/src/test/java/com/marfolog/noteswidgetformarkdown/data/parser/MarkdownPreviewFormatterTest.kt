package com.marfolog.noteswidgetformarkdown.data.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownPreviewFormatterTest {

    private val formatter = MarkdownPreviewFormatter()

    @Test
    fun formatsTaskListItemsWithoutRawCheckboxSyntax() {
        val preview = formatter.format(
            """
            - [ ] Buy milk
            - [x] Pay bills
            """.trimIndent()
        )

        assertEquals(
            """
            ☐ Buy milk
            ☑ Pay bills
            """.trimIndent(),
            preview
        )
        assertFalse(preview.contains("[ ]"))
        assertFalse(preview.contains("[x]"))
    }

    @Test
    fun removesCommonMarkdownSyntaxFromPreview() {
        val preview = formatter.format(
            """
            # Project Notes
            This is **important** and _useful_.
            Open [Obsidian](https://obsidian.md) and `sync`.
            ~~Old idea~~
            """.trimIndent()
        )

        assertEquals(
            """
            Project Notes
            This is important and useful.
            Open Obsidian and sync.
            Old idea
            """.trimIndent(),
            preview
        )
    }

    @Test
    fun normalizesObsidianWikiLinksAndCallouts() {
        val preview = formatter.format(
            """
            ---
            tag: test
            ---
            > [!NOTE] Remember [[Projects/App|the app]]
            Link to [[Daily Note]]
            """.trimIndent()
        )

        assertEquals(
            """
            Remember the app
            Link to Daily Note
            """.trimIndent(),
            preview
        )
    }

    @Test
    fun limitsPreviewLines() {
        val preview = MarkdownPreviewFormatter(previewLineCount = 5).format(
            """
            One
            Two
            Three
            Four
            Five
            Six
            """.trimIndent()
        )

        assertEquals(
            """
            One
            Two
            Three
            Four
            Five
            """.trimIndent(),
            preview
        )
    }

    @Test
    fun `note with only frontmatter yields an empty preview`() {
        val preview = formatter.format(
            """
            ---
            tags:
              - inbox
            ---
            """.trimIndent()
        )

        assertEquals("", preview)
    }

    @Test
    fun `unclosed frontmatter is not silently swallowed`() {
        // With no closing delimiter we cannot tell frontmatter from content; dropping
        // everything would blank the card, so the text must survive.
        val preview = formatter.format("---\ntags: broken\nActual note text")

        assertTrue(preview.contains("Actual note text"))
    }

    @Test
    fun keepsListLineBreaksAndNestedIndentation() {
        val preview = formatter.format(
            """
            - [ ] Parent task
              - [x] Nested task
              - Follow up item
            - Plain item
            """.trimIndent()
        )

        assertEquals(
            """
            ☐ Parent task
              ☑ Nested task
              • Follow up item
            • Plain item
            """.trimIndent(),
            preview
        )
    }

    @Test
    fun headingThatRepeatsTheCardTitleIsLeftOut() {
        val preview = formatter.format(
            """
            # Groceries
            - [ ] Olive oil
            - [ ] Bread
            """.trimIndent(),
            title = "Groceries"
        )

        assertEquals("☐ Olive oil\n☐ Bread", preview)
    }

    @Test
    fun repeatedTitleIsMatchedIgnoringCaseAndSpaces() {
        val preview = formatter.format("#  groceries \nMilk", title = "Groceries")

        assertEquals("Milk", preview)
    }

    @Test
    fun headingWithDifferentTextStaysInThePreview() {
        val preview = formatter.format("# Shopping list\nMilk", title = "Groceries")

        assertEquals("Shopping list\nMilk", preview)
    }

    @Test
    fun titleLaterInTheNoteIsKept() {
        val preview = formatter.format("Intro line\n# Groceries\nMilk", title = "Groceries")

        assertEquals("Intro line\nGroceries\nMilk", preview)
    }

    @Test
    fun headingAfterFrontMatterThatRepeatsTitleIsLeftOut() {
        val preview = formatter.format("---\ntags: [x]\n---\n# Groceries\nMilk", title = "Groceries")

        assertEquals("Milk", preview)
    }

    @Test
    fun richFormattingFormatsHeadingsAndThematicBreaksAndQuotes() {
        val preview = formatter.format(
            """
            # Chapter One
            ## Section A
            ### Details
            ---
            > An inspiring quote
            ```
            println("code")
            ```
            """.trimIndent(),
            richFormatting = true
        )

        assertTrue(preview.contains("§H1§Chapter One"))
        assertTrue(preview.contains("§H2§Section A"))
        assertTrue(preview.contains("§H3§Details"))
        assertTrue(preview.contains("§HR§"))
        assertTrue(preview.contains("§QUOTE§An inspiring quote"))
        assertTrue(preview.contains("§CODE§println(\"code\")"))
    }

    @Test
    fun formatsMarkdownTablesIntoCleanTaskAndBulletLists() {
        val markdown = """
            ## Pillar 1: Reading
            | Book Title | Author | Status | Rating | Core Mental Model |
            | :--- | :--- | :---: | :---: | :--- |
            | **Linux Basics for Hackers** | OccupyTheWeb | `[⏳] In Progress` | — | Everything in Linux is a file. |
            | **Thinking, Fast and Slow** | Daniel Kahneman | `[ ] Queued` | — | System 1 vs System 2. |
            | **How to Read a Book** | Mortimer J. Adler | `[x] Mastered` | ⭐⭐⭐⭐⭐ | The 4 Levels of Reading. |
        """.trimIndent()

        val preview = formatter.format(markdown, richFormatting = true)

        assertFalse(preview.contains("|"))
        assertFalse(preview.contains(":---"))
        assertTrue(preview.contains("§H2§Pillar 1: Reading"))
        assertTrue(preview.contains("☐ ⏳ Linux Basics for Hackers (OccupyTheWeb) — Everything in Linux is a file."))
        assertTrue(preview.contains("☐ Thinking, Fast and Slow (Daniel Kahneman) — System 1 vs System 2."))
        assertTrue(preview.contains("☑ How to Read a Book (Mortimer J. Adler) — ⭐⭐⭐⭐⭐ — The 4 Levels of Reading."))
    }
}
