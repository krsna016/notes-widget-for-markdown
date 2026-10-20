package com.marfolog.noteswidgetformarkdown.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.pow

/** WCAG 2 contrast arithmetic, so readability is computed rather than eyeballed per colour. */
object ColorContrast {

    /** What WCAG asks of normal-size text. */
    const val MIN_TEXT_CONTRAST = 4.5

    private const val DARKEN_LIGHT = 0.12f
    private const val LIGHTEN_DARK = 0.15f

    fun luminance(color: Color): Double =
        0.2126 * linear(color.red) + 0.7152 * linear(color.green) + 0.0722 * linear(color.blue)

    /** From 1 (the same colour) to 21 (black on white); the order of the two does not matter. */
    fun contrast(first: Color, second: Color): Double {
        val a = luminance(first)
        val b = luminance(second)
        return (maxOf(a, b) + 0.05) / (minOf(a, b) + 0.05)
    }

    /** Light means dark text reads better on it than light text does. */
    fun isLight(color: Color): Boolean = contrast(color, Color.Black) >= contrast(color, Color.White)

    /**
     * The first of [candidates], in order of preference, that reaches [MIN_TEXT_CONTRAST] on
     * [background]. If none does, black or white, whichever is stronger.
     */
    fun readableOn(background: Color, candidates: List<Color> = emptyList()): Color =
        candidates.firstOrNull { contrast(it, background) >= MIN_TEXT_CONTRAST }
            ?: if (isLight(background)) Color.Black else Color.White

    /** One tone away from [color]: darker on a light colour, lighter on a dark one. Always opaque. */
    fun tonalShift(color: Color): Color =
        if (isLight(color)) mix(color, Color.Black, DARKEN_LIGHT) else mix(color, Color.White, LIGHTEN_DARK)

    private fun mix(from: Color, to: Color, amount: Float): Color = Color(
        red = (from.red + (to.red - from.red) * amount).coerceIn(0f, 1f),
        green = (from.green + (to.green - from.green) * amount).coerceIn(0f, 1f),
        blue = (from.blue + (to.blue - from.blue) * amount).coerceIn(0f, 1f)
    )

    private fun linear(channel: Float): Double =
        if (channel <= 0.04045f) channel / 12.92 else ((channel + 0.055) / 1.055).pow(2.4)
}
