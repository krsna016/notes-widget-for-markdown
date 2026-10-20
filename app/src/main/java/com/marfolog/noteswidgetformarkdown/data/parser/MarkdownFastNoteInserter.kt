package com.marfolog.noteswidgetformarkdown.data.parser

import com.marfolog.noteswidgetformarkdown.domain.model.FastNoteStyle

class MarkdownFastNoteInserter {

    fun insert(content: String, noteText: String, style: FastNoteStyle = FastNoteStyle.Bullet): String {
        val trimmedNote = noteText.trim()
        require(trimmedNote.isNotEmpty()) { "Fast note cannot be empty." }

        val bullet = style.linePrefix + trimmedNote
        val normalizedContent = content.replace("\r\n", "\n").replace("\r", "\n")
        if (normalizedContent.isEmpty()) {
            return "$bullet\n"
        }

        val lines = normalizedContent.split("\n").toMutableList()
        val insertIndex = findInsertIndex(lines)
        lines.add(insertIndex, bullet)
        return lines.joinToString("\n")
    }

    private fun findInsertIndex(lines: List<String>): Int {
        val contentStartIndex = findContentStartIndex(lines)
        val headingIndex = lines
            .withIndex()
            .drop(contentStartIndex)
            .firstOrNull { (_, line) -> H1_HEADING_REGEX.matches(line) }
            ?.index

        return headingIndex?.plus(1) ?: contentStartIndex
    }

    private fun findContentStartIndex(lines: List<String>): Int {
        if (lines.firstOrNull()?.trim() != FRONTMATTER_DELIMITER) {
            return 0
        }

        val closingIndex = lines
            .drop(1)
            .indexOfFirst { it.trim() == FRONTMATTER_DELIMITER }

        return if (closingIndex >= 0) {
            closingIndex + 2
        } else {
            0
        }
    }

    companion object {
        private const val FRONTMATTER_DELIMITER = "---"
        private val H1_HEADING_REGEX = Regex("^#\\s+.+")
    }
}
