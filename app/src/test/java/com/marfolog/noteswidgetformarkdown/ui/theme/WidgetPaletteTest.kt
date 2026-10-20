package com.marfolog.noteswidgetformarkdown.ui.theme

import androidx.compose.ui.graphics.Color
import com.marfolog.noteswidgetformarkdown.domain.model.NoteCardColor
import com.marfolog.noteswidgetformarkdown.domain.model.WidgetColor
import com.marfolog.noteswidgetformarkdown.domain.model.WidgetColorChoice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetPaletteTest {

    private val fixedColors = WidgetColor.entries - WidgetColor.System - WidgetColor.Custom

    @Test
    fun `system keeps the theme colours`() {
        assertNull(WidgetPalette.background(WidgetColorChoice()))
        assertNull(WidgetPalette.colors(WidgetColorChoice(WidgetColor.System, customHex = "#8E24AA")))
    }

    @Test
    fun `the pastels are exactly the note card colours`() {
        assertEquals(NoteCardPalette.background(NoteCardColor.Amber), WidgetPalette.background(WidgetColorChoice(WidgetColor.Amber)))
        assertEquals(NoteCardPalette.background(NoteCardColor.Sky), WidgetPalette.background(WidgetColorChoice(WidgetColor.Sky)))
        assertEquals(NoteCardPalette.background(NoteCardColor.Lavender), WidgetPalette.background(WidgetColorChoice(WidgetColor.Lavender)))
    }

    @Test
    fun `every fixed colour has a background`() {
        fixedColors.forEach { color -> assertNotNull(color.name, WidgetPalette.background(WidgetColorChoice(color))) }
    }

    @Test
    fun `a custom colour is taken from its hex with or without the hash`() {
        assertEquals(Color(0xFF8E24AA), WidgetPalette.background(WidgetColorChoice(WidgetColor.Custom, "#8E24AA")))
        assertEquals(Color(0xFF8E24AA), WidgetPalette.background(WidgetColorChoice(WidgetColor.Custom, " 8e24aa ")))
    }

    @Test
    fun `a custom colour that is missing or not a colour shows the system one`() {
        listOf(null, "", "#8E24A", "#8E24AAFF", "purple", "#GG24AA").forEach { hex ->
            val choice = WidgetColorChoice(WidgetColor.Custom, hex)
            assertNull(hex, WidgetPalette.colors(choice))
            assertEquals(hex, WidgetColor.System, WidgetPalette.shown(choice))
        }
    }

    @Test
    fun `what the widget shows is the choice whenever it resolves to a colour`() {
        assertEquals(WidgetColor.Graphite, WidgetPalette.shown(WidgetColorChoice(WidgetColor.Graphite)))
        assertEquals(WidgetColor.Custom, WidgetPalette.shown(WidgetColorChoice(WidgetColor.Custom, "#8E24AA")))
        assertEquals(WidgetColor.System, WidgetPalette.shown(WidgetColorChoice()))
    }

    @Test
    fun `text and icons are readable on every offered colour and on its bar`() {
        fixedColors.forEach { color ->
            val colors = WidgetPalette.colors(WidgetColorChoice(color))!!
            assertTrue(color.name, ColorContrast.contrast(colors.content, colors.background) >= 4.5)
            assertTrue(color.name, ColorContrast.contrast(colors.error, colors.background) >= 4.5)
            assertTrue(color.name, ColorContrast.contrast(colors.onBar, colors.bar) >= 4.5)
        }
    }

    @Test
    fun `pastels get dark text and the dark tones light text`() {
        val dark = Color(0xFF1B1B1F)
        val light = Color(0xFFF2F2F5)
        assertEquals(dark, WidgetPalette.colors(WidgetColorChoice(WidgetColor.Amber))!!.content)
        assertEquals(dark, WidgetPalette.colors(WidgetColorChoice(WidgetColor.Sky))!!.onBar)
        assertEquals(light, WidgetPalette.colors(WidgetColorChoice(WidgetColor.Graphite))!!.content)
        assertEquals(light, WidgetPalette.colors(WidgetColorChoice(WidgetColor.Forest))!!.onBar)
    }

    @Test
    fun `the bar is darker than a pastel and lighter than a dark tone`() {
        val amber = WidgetPalette.colors(WidgetColorChoice(WidgetColor.Amber))!!
        val graphite = WidgetPalette.colors(WidgetColorChoice(WidgetColor.Graphite))!!

        assertTrue(ColorContrast.luminance(amber.bar) < ColorContrast.luminance(amber.background))
        assertTrue(ColorContrast.luminance(graphite.bar) > ColorContrast.luminance(graphite.background))
    }

    @Test
    fun `a saturated custom purple gets light text and the palest error red`() {
        val colors = WidgetPalette.colors(WidgetColorChoice(WidgetColor.Custom, "#8E24AA"))!!

        assertEquals(Color(0xFFF2F2F5), colors.content)
        assertEquals(Color(0xFFFFDAD6), colors.error)
    }

    @Test
    fun `errors stay the theme's red on a pastel and its light red on a dark tone`() {
        assertEquals(Color(0xFFB3261E), WidgetPalette.colors(WidgetColorChoice(WidgetColor.Sky))!!.error)
        assertEquals(Color(0xFFF2B8B5), WidgetPalette.colors(WidgetColorChoice(WidgetColor.Graphite))!!.error)
    }

    @Test
    fun `a mid grey custom colour where no preset text is readable falls back to black`() {
        val colors = WidgetPalette.colors(WidgetColorChoice(WidgetColor.Custom, "#777777"))!!

        assertEquals(Color.Black, colors.content)
        assertTrue(ColorContrast.contrast(colors.error, colors.background) >= 4.5)
    }

    @Test
    fun `colours derived from a see-through input are opaque`() {
        val colors = WidgetColors.on(Color(0xFF2B2D31).copy(alpha = 0.45f))

        assertEquals(1f, colors.background.alpha, 0f)
        assertEquals(1f, colors.bar.alpha, 0f)
    }

    @Test
    fun `typing a custom colour redraws the widget only once the colour is complete`() {
        val typing = WidgetColorChoice(WidgetColor.Custom, "#8E24A")
        val done = WidgetColorChoice(WidgetColor.Custom, "#8E24AA")

        assertFalse(WidgetPalette.changesWidget(WidgetColorChoice(WidgetColor.Custom, "#8E2"), typing))
        assertTrue(WidgetPalette.changesWidget(typing, done))
        assertTrue(WidgetPalette.changesWidget(done, typing))
    }

    @Test
    fun `picking custom before typing anything leaves the system widget alone`() {
        assertFalse(WidgetPalette.changesWidget(WidgetColorChoice(), WidgetColorChoice(WidgetColor.Custom)))
    }

    @Test
    fun `switching between two colours redraws the widget`() {
        assertTrue(WidgetPalette.changesWidget(WidgetColorChoice(WidgetColor.Amber), WidgetColorChoice(WidgetColor.Sky)))
        assertTrue(WidgetPalette.changesWidget(WidgetColorChoice(), WidgetColorChoice(WidgetColor.Graphite)))
    }
}
