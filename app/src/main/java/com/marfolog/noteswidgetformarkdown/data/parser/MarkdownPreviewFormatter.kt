package com.marfolog.noteswidgetformarkdown.data.parser

import org.commonmark.Extension
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.ext.task.list.items.TaskListItemMarker
import org.commonmark.ext.task.list.items.TaskListItemsExtension
import org.commonmark.node.AbstractVisitor
import org.commonmark.node.BlockQuote
import org.commonmark.node.BulletList
import org.commonmark.node.Code
import org.commonmark.node.Document
import org.commonmark.node.Emphasis
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.HardLineBreak
import org.commonmark.node.Heading
import org.commonmark.node.IndentedCodeBlock
import org.commonmark.node.Link
import org.commonmark.node.ListItem
import org.commonmark.node.Node
import org.commonmark.node.OrderedList
import org.commonmark.node.Paragraph
import org.commonmark.node.SoftLineBreak
import org.commonmark.node.StrongEmphasis
import org.commonmark.node.Text
import org.commonmark.node.ThematicBreak
import org.commonmark.parser.Parser

class MarkdownPreviewFormatter(
    private val previewLineCount: Int = DEFAULT_PREVIEW_LINE_COUNT
) {
    private val parser = Parser.builder()
        .extensions(extensions)
        .build()

    /** A first heading that only repeats [title] is left out; the card already shows the title. */
    fun format(source: String, title: String? = null, richFormatting: Boolean = false): String {
        val markdown = source
            .lineSequence()
            .dropYamlFrontMatter()
            .let { transformMarkdownTables(it, richFormatting) }
            .map { normalizeObsidianMarkdown(it, richFormatting) }
            .joinToString("\n")

        val document = parser.parse(markdown)
        val firstBlock = document.firstChild
        if (title != null && firstBlock is Heading &&
            PreviewVisitor(richFormatting = false).render(firstBlock).trim().equals(title.trim(), ignoreCase = true)
        ) {
            firstBlock.unlink()
        }
        return PreviewVisitor(richFormatting).render(document)
            .lineSequence()
            .map { cleanupRenderedText(it.trimEnd(), richFormatting) }
            .filter { it.isNotBlank() }
            .take(previewLineCount)
            .joinToString("\n")
    }

    private fun normalizeObsidianMarkdown(line: String, richFormatting: Boolean = false): String {
        val processedLine = if (richFormatting) {
            val calloutMatch = calloutPatternRegex.find(line)
            if (calloutMatch != null) {
                val (type, rest) = calloutMatch.destructured
                val icon = when (type.lowercase()) {
                    "tip", "hint" -> "✨ TIP"
                    "warning", "caution", "danger", "bug" -> "⚠ WARNING"
                    "important" -> "★ IMPORTANT"
                    "quote" -> "❝ QUOTE"
                    "todo" -> "☑ TODO"
                    else -> "💡 NOTE"
                }
                if (rest.isNotBlank()) "> **$icon**: $rest" else "> **$icon**"
            } else {
                line
            }
        } else {
            line.replace(calloutMarkerRegex, "")
        }

        return processedLine
            .replace(wikiLinkWithAliasRegex) { match -> match.groupValues[2] }
            .replace(wikiLinkRegex) { match -> match.groupValues[1] }
    }

    private fun cleanupRenderedText(line: String, richFormatting: Boolean = false): String {
        if (richFormatting && (line.startsWith("§CODE§") || line.startsWith("§H1§") || line.startsWith("§H2§") || line.startsWith("§H3§") || line.startsWith("§H4§") || line.startsWith("§QUOTE§") || line == "§HR§")) {
            return line.trimEnd()
        }
        return line
            .replace(renderedLinkRegex) { match -> match.groupValues[1] }
            .replace(renderedQuotedInlineRegex) { match -> match.groupValues[1] }
            .replace(renderedWrappedInlineRegex) { match -> match.groupValues[1] }
            .trimEnd()
    }

    private fun transformMarkdownTables(source: Sequence<String>, richFormatting: Boolean): Sequence<String> = sequence {
        val lines = source.toList()
        var inCodeBlock = false
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trim()
            if (trimmed.startsWith("```")) {
                inCodeBlock = !inCodeBlock
                yield(line)
                i++
                continue
            }
            if (inCodeBlock) {
                yield(line)
                i++
                continue
            }

            val isCurrentRow = isPotentialTableRow(trimmed)
            val nextTrimmed = if (i + 1 < lines.size) lines[i + 1].trim() else ""
            val isNextSeparator = isTableSeparator(nextTrimmed)

            if (isCurrentRow && isNextSeparator) {
                // Table header found, skip header and separator
                i += 2
                // Process table data rows
                while (i < lines.size) {
                    val rowLine = lines[i].trim()
                    if (!isPotentialTableRow(rowLine)) {
                        break
                    }
                    if (isTableSeparator(rowLine)) {
                        i++
                        continue
                    }
                    val cells = splitTableCells(rowLine)
                    if (cells.isNotEmpty()) {
                        val formatted = formatTableRow(cells, richFormatting)
                        if (formatted.isNotBlank()) {
                            yield(formatted)
                        }
                    }
                    i++
                }
                continue
            }

            if (isTableSeparator(trimmed)) {
                // Isolated separator line, skip
                i++
                continue
            }

            yield(line)
            i++
        }
    }

    private fun isPotentialTableRow(line: String): Boolean {
        return line.startsWith("|") && line.endsWith("|") && line.length > 2
    }

    private fun splitTableCells(line: String): List<String> {
        val trimmed = line.trim().removePrefix("|").removeSuffix("|")
        return trimmed.split("|").map { it.trim() }
    }

    private fun isTableSeparator(line: String): Boolean {
        if (!line.contains("|")) return false
        val cells = splitTableCells(line)
        return cells.isNotEmpty() && cells.all { it.matches(tableSeparatorRegex) }
    }

    private fun formatTableRow(cells: List<String>, richFormatting: Boolean): String {
        var hasChecked = false
        var hasUnchecked = false
        var hasInProgress = false

        for (c in cells) {
            when {
                Regex("""\[[xX]\]""").containsMatchIn(c) -> hasChecked = true
                Regex("""\[\s*\]""").containsMatchIn(c) -> hasUnchecked = true
                c.contains("[⏳]") || c.contains("⏳") -> hasInProgress = true
            }
        }

        val prefix = when {
            hasChecked -> "- [x] "
            hasInProgress -> "- [ ] ⏳ "
            hasUnchecked -> "- [ ] "
            else -> "- "
        }

        val meaningfulCells = cells.map { cell ->
            cell.replace("`", "")
                .replace(Regex("""\[[ xX⏳]\]"""), "")
                .trim()
        }.filter { cell ->
            cell.isNotBlank() && cell != "—" && cell != "-" && !cell.equals("n/a", ignoreCase = true) &&
                !cell.equals("queued", ignoreCase = true) &&
                (!hasInProgress || !cell.equals("in progress", ignoreCase = true)) &&
                (!hasChecked || !cell.equals("mastered", ignoreCase = true))
        }

        if (meaningfulCells.isEmpty()) return ""

        val title = if (meaningfulCells[0].startsWith("**")) meaningfulCells[0] else "**${meaningfulCells[0]}**"

        return when {
            meaningfulCells.size == 1 -> "$prefix$title"
            meaningfulCells.size == 2 -> "$prefix$title — ${meaningfulCells[1]}"
            else -> "$prefix$title (${meaningfulCells[1]}) — ${meaningfulCells.drop(2).joinToString(" — ")}"
        }
    }

    private fun Sequence<String>.dropYamlFrontMatter(): Sequence<String> {
        val lines = toList()
        if (lines.firstOrNull()?.trim() != "---") {
            return lines.asSequence()
        }

        val endIndex = lines.drop(1).indexOfFirst { it.trim() == "---" }
        return if (endIndex == -1) {
            lines.asSequence()
        } else {
            lines.drop(endIndex + 2).asSequence()
        }
    }

    private class PreviewVisitor(private val richFormatting: Boolean = false) : AbstractVisitor() {
        private val builder = StringBuilder()
        private var listDepth = 0
        private val orderedCounters = mutableListOf<Int>()

        fun render(node: Node): String {
            node.accept(this)
            return builder.toString()
        }

        override fun visit(document: Document) {
            visitChildren(document)
        }

        override fun visit(heading: Heading) {
            appendNewLineIfNeeded()
            if (richFormatting) {
                val level = heading.level.coerceIn(1, 4)
                builder.append("§H${level}§")
            }
            visitChildren(heading)
            appendNewLineIfNeeded()
        }

        override fun visit(thematicBreak: ThematicBreak) {
            appendNewLineIfNeeded()
            if (richFormatting) {
                builder.append("§HR§\n")
            }
        }

        override fun visit(blockQuote: BlockQuote) {
            appendNewLineIfNeeded()
            if (richFormatting) {
                val innerVisitor = PreviewVisitor(richFormatting = false)
                val innerText = innerVisitor.render(blockQuote).trim()
                innerText.lines().forEach { line ->
                    if (line.isNotBlank()) {
                        builder.append("§QUOTE§").append(line.trim()).append('\n')
                    }
                }
            } else {
                visitChildren(blockQuote)
                appendNewLineIfNeeded()
            }
        }

        override fun visit(fencedCodeBlock: FencedCodeBlock) {
            appendNewLineIfNeeded()
            val text = fencedCodeBlock.literal.trim()
            if (richFormatting) {
                text.lines().forEach { line ->
                    if (line.isNotBlank()) {
                        builder.append("§CODE§").append(line).append('\n')
                    }
                }
            } else {
                builder.append(text)
                appendNewLineIfNeeded()
            }
        }

        override fun visit(indentedCodeBlock: IndentedCodeBlock) {
            appendNewLineIfNeeded()
            val text = indentedCodeBlock.literal.trim()
            if (richFormatting) {
                text.lines().forEach { line ->
                    if (line.isNotBlank()) {
                        builder.append("§CODE§").append(line).append('\n')
                    }
                }
            } else {
                builder.append(text)
                appendNewLineIfNeeded()
            }
        }

        override fun visit(paragraph: Paragraph) {
            if (paragraph.parent is ListItem) {
                visitChildren(paragraph)
            } else {
                appendLineFromChildren(paragraph)
            }
        }

        override fun visit(bulletList: BulletList) {
            if (bulletList.parent is ListItem) {
                appendNewLineIfNeeded()
            }
            listDepth += 1
            visitChildren(bulletList)
            listDepth -= 1
        }

        override fun visit(orderedList: OrderedList) {
            if (orderedList.parent is ListItem) {
                appendNewLineIfNeeded()
            }
            orderedCounters.add(orderedList.markerStartNumber ?: 1)
            listDepth += 1
            visitChildren(orderedList)
            listDepth -= 1
            orderedCounters.removeAt(orderedCounters.lastIndex)
        }

        override fun visit(listItem: ListItem) {
            val prefix = buildListPrefix(listItem)
            builder.append("  ".repeat((listDepth - 1).coerceAtLeast(0)))
            builder.append(prefix)
            visitChildren(listItem)
            appendNewLineIfNeeded()
        }

        override fun visit(text: Text) {
            builder.append(text.literal)
        }

        override fun visit(code: Code) {
            builder.append(code.literal)
        }

        override fun visit(link: Link) {
            visitChildren(link)
        }

        override fun visit(emphasis: Emphasis) {
            visitChildren(emphasis)
        }

        override fun visit(strongEmphasis: StrongEmphasis) {
            visitChildren(strongEmphasis)
        }

        override fun visit(softLineBreak: SoftLineBreak) {
            builder.append('\n')
        }

        override fun visit(hardLineBreak: HardLineBreak) {
            builder.append('\n')
        }

        private fun appendLineFromChildren(node: Node) {
            visitChildren(node)
            appendNewLineIfNeeded()
        }

        private fun appendNewLineIfNeeded() {
            if (builder.isNotEmpty() && builder.last() != '\n') {
                builder.append('\n')
            }
        }

        private fun buildListPrefix(listItem: ListItem): String {
            findTaskMarker(listItem)?.let { marker ->
                return if (marker.isChecked) "☑ " else "☐ "
            }

            val text = firstTextChild(listItem)
            val taskMatch = text?.literal?.let { parsedTaskItemRegex.find(it) }
            if (taskMatch != null) {
                val checked = taskMatch.groupValues[1].equals("x", ignoreCase = true)
                text.literal = text.literal.replaceFirst(parsedTaskItemRegex, "")
                return if (checked) "☑ " else "☐ "
            }

            val orderedCounterIndex = orderedCounters.lastIndex
            if (orderedCounterIndex >= 0 && listItem.parent is OrderedList) {
                val number = orderedCounters[orderedCounterIndex]
                orderedCounters[orderedCounterIndex] = number + 1
                return "$number. "
            }

            return "• "
        }

        private fun findTaskMarker(node: Node): TaskListItemMarker? {
            var current = node.firstChild
            while (current != null) {
                if (current is TaskListItemMarker) {
                    return current
                }
                findTaskMarker(current)?.let { return it }
                current = current.next
            }
            return null
        }

        private fun firstTextChild(node: Node): Text? {
            var current = node.firstChild
            while (current != null) {
                if (current is Text) {
                    return current
                }
                firstTextChild(current)?.let { return it }
                current = current.next
            }
            return null
        }
    }

    companion object {
        private const val DEFAULT_PREVIEW_LINE_COUNT = 500
        private val tableSeparatorRegex = Regex("^:?-{2,}:?$")
        private val extensions: List<Extension> = listOf(
            TaskListItemsExtension.create(),
            StrikethroughExtension.create()
        )
        private val parsedTaskItemRegex = Regex("^\\s*\\[([ xX])]\\s+")
        private val calloutMarkerRegex = Regex("^\\s*>\\s*\\[![A-Za-z]+]\\s*")
        private val calloutPatternRegex = Regex("^\\s*>\\s*\\[!([A-Za-z]+)\\]\\s*(.*)$")
        private val wikiLinkWithAliasRegex = Regex("\\[\\[([^]|]+)\\|([^]]+)]]")
        private val wikiLinkRegex = Regex("\\[\\[([^]]+)]]")
        private val renderedLinkRegex = Regex("\"([^\"]+)\" \\([^)]*\\)")
        private val renderedQuotedInlineRegex = Regex("\"([^\"]+)\"")
        private val renderedWrappedInlineRegex = Regex("^[/_](.+)[/_]$")
    }
}
