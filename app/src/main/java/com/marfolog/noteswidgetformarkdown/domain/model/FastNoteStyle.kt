package com.marfolog.noteswidgetformarkdown.domain.model

/** How a fast note lands in the target file. */
enum class FastNoteStyle(val storageValue: String, val linePrefix: String) {
    Bullet("bullet", "- "),
    Checkbox("checkbox", "- [ ] ");

    companion object {
        fun from(value: String?): FastNoteStyle =
            entries.firstOrNull { it.storageValue == value } ?: Bullet
    }
}
