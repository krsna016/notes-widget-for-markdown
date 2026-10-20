package com.marfolog.noteswidgetformarkdown.ui.theme

import androidx.compose.ui.graphics.Color
import com.marfolog.noteswidgetformarkdown.domain.model.NoteCardColor
import com.marfolog.noteswidgetformarkdown.domain.model.WidgetColor
import com.marfolog.noteswidgetformarkdown.domain.model.WidgetColorChoice

/**
 * Everything the widget's own surfaces need from one background colour. The bar and the round
 * buttons sit one tone away from the background and stay opaque when the background fades.
 */
data class WidgetColors(
    val background: Color,
    val content: Color,
    val error: Color,
    val bar: Color,
    val onBar: Color
) {
    companion object {
        fun on(background: Color): WidgetColors {
            val opaque = background.copy(alpha = 1f)
            val bar = ColorContrast.tonalShift(opaque)
            return WidgetColors(
                background = opaque,
                content = ColorContrast.readableOn(opaque, CONTENT),
                error = ColorContrast.readableOn(opaque, ERROR),
                bar = bar,
                onBar = ColorContrast.readableOn(bar, CONTENT)
            )
        }

        private val CONTENT = listOf(Color(0xFF1B1B1F), Color(0xFFF2F2F5))
        /** Reddest first; the palest one only where the others are not readable. */
        private val ERROR = listOf(Color(0xFFB3261E), Color(0xFFF2B8B5), Color(0xFFFFDAD6))
    }
}

/** The pastels are the note card ones, so a card and the widget can match exactly. */
object WidgetPalette {

    fun background(choice: WidgetColorChoice): Color? = when (choice.color) {
        WidgetColor.System -> null
        WidgetColor.Rose -> NoteCardPalette.background(NoteCardColor.Rose)
        WidgetColor.Red -> NoteCardPalette.background(NoteCardColor.Red)
        WidgetColor.Amber -> NoteCardPalette.background(NoteCardColor.Amber)
        WidgetColor.Orange -> NoteCardPalette.background(NoteCardColor.Orange)
        WidgetColor.Mint -> NoteCardPalette.background(NoteCardColor.Mint)
        WidgetColor.Green -> NoteCardPalette.background(NoteCardColor.Green)
        WidgetColor.Sky -> NoteCardPalette.background(NoteCardColor.Sky)
        WidgetColor.Blue -> NoteCardPalette.background(NoteCardColor.Blue)
        WidgetColor.Lavender -> NoteCardPalette.background(NoteCardColor.Lavender)
        WidgetColor.Graphite -> Color(0xFF2B2D31)
        WidgetColor.Forest -> Color(0xFF1E2B22)
        WidgetColor.Custom -> NoteCardPalette.parseCustom(choice.customHex)
    }

    /** Null keeps the theme's colours, which is also where a half-typed custom colour lands. */
    fun colors(choice: WidgetColorChoice): WidgetColors? = background(choice)?.let(WidgetColors::on)

    /** What the widget actually shows: a custom colour that does not parse is still System. */
    fun shown(choice: WidgetColorChoice): WidgetColor =
        if (background(choice) == null) WidgetColor.System else choice.color

    /** Typing a custom colour only redraws the widget once that changes what it shows. */
    fun changesWidget(before: WidgetColorChoice, after: WidgetColorChoice): Boolean =
        background(before) != background(after)
}
