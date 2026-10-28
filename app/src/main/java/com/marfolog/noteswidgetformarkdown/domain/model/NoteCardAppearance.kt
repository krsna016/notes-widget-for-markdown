package com.marfolog.noteswidgetformarkdown.domain.model

data class NoteCardAppearance(
    val size: NoteCardSize = NoteCardSize.Compact,
    val color: NoteCardColor = NoteCardColor.Default,
    val textSize: NoteCardTextSize = NoteCardTextSize.Default,
    val customColorHex: String? = null
)

enum class NoteCardSize(
    val storageValue: String,
    val label: String,
    val heightDp: Int,
    val previewMaxLines: Int
) {
    Compact("compact", "Compact", 120, 4),
    Medium("medium", "Medium", 190, 8),
    Large("large", "Large", 300, 16),
    ExtraLarge("extra_large", "XL", 400, 24),
    Full("full", "Full", 540, 36);

    companion object {
        fun fromStorage(value: String?): NoteCardSize {
            return entries.firstOrNull { it.storageValue == value } ?: Compact
        }
    }
}

enum class NoteCardColor(
    val storageValue: String,
    val label: String
) {
    Default("default", "Default"),
    Obsidian("obsidian", "Obsidian"),
    Oled("oled", "OLED"),
    Nord("nord", "Nord"),
    Rose("rose", "Rose"),
    Red("red", "Red"),
    Amber("amber", "Amber"),
    Orange("orange", "Orange"),
    Mint("mint", "Mint"),
    Green("green", "Green"),
    Sky("sky", "Sky"),
    Blue("blue", "Blue"),
    Lavender("lavender", "Lavender"),
    Custom("custom", "Custom");

    companion object {
        fun fromStorage(value: String?): NoteCardColor {
            return entries.firstOrNull { it.storageValue == value } ?: Default
        }
    }
}

enum class NoteCardTextSize(
    val storageValue: String,
    val label: String,
    val titleSp: Int,
    val previewSp: Int
) {
    Default("default", "Default", 22, 18),
    Large("large", "Large", 26, 22),
    ExtraLarge("extra_large", "Extra", 30, 26),
    Huge("huge", "Huge", 36, 30);

    companion object {
        fun fromStorage(value: String?): NoteCardTextSize {
            return entries.firstOrNull { it.storageValue == value } ?: Default
        }
    }
}
