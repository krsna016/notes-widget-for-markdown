package com.marfolog.noteswidgetformarkdown.domain.model

/**
 * The colour behind the whole widget. [System] keeps the theme's own background — Material You on
 * Android 12 and newer — and is what everyone had before there was a choice. Any other choice is
 * one fixed colour, the same in light and dark mode.
 */
enum class WidgetColor(val storageValue: String) {
    System("system"),
    Rose("rose"),
    Red("red"),
    Amber("amber"),
    Orange("orange"),
    Mint("mint"),
    Green("green"),
    Sky("sky"),
    Blue("blue"),
    Lavender("lavender"),
    Graphite("graphite"),
    Midnight("midnight"),
    Forest("forest"),
    Custom("custom");

    companion object {
        fun fromStorage(value: String?): WidgetColor = entries.firstOrNull { it.storageValue == value } ?: System
    }
}

/** [customHex] is kept even while another colour is picked, so switching back restores it. */
data class WidgetColorChoice(
    val color: WidgetColor = WidgetColor.System,
    val customHex: String? = null
)
