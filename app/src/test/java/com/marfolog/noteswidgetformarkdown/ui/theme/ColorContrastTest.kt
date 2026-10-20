package com.marfolog.noteswidgetformarkdown.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorContrastTest {

    @Test
    fun `luminance runs from black to white with red weighted as WCAG defines it`() {
        assertEquals(0.0, ColorContrast.luminance(Color.Black), 1e-9)
        assertEquals(1.0, ColorContrast.luminance(Color.White), 1e-9)
        assertEquals(0.2126, ColorContrast.luminance(Color(0xFFFF0000)), 1e-4)
        assertEquals(0.7152, ColorContrast.luminance(Color(0xFF00FF00)), 1e-4)
    }

    @Test
    fun `black on white is the strongest contrast there is and a colour on itself the weakest`() {
        assertEquals(21.0, ColorContrast.contrast(Color.Black, Color.White), 1e-6)
        assertEquals(1.0, ColorContrast.contrast(Color(0xFF2B2D31), Color(0xFF2B2D31)), 1e-9)
    }

    @Test
    fun `contrast is the same whichever colour is the text`() {
        val amber = Color(0xFFFFDEA6)
        val ink = Color(0xFF1B1B1F)
        assertEquals(ColorContrast.contrast(amber, ink), ColorContrast.contrast(ink, amber), 1e-12)
    }

    @Test
    fun `the familiar grey pair sits on either side of the text threshold`() {
        assertEquals(4.54, ColorContrast.contrast(Color(0xFF767676), Color.White), 0.01)
        assertEquals(4.48, ColorContrast.contrast(Color(0xFF777777), Color.White), 0.01)
    }

    @Test
    fun `a candidate just reaching the threshold is used`() {
        assertEquals(Color(0xFF767676), ColorContrast.readableOn(Color.White, listOf(Color(0xFF767676))))
    }

    @Test
    fun `a candidate just short of the threshold gives way to black on a light background`() {
        assertEquals(Color.Black, ColorContrast.readableOn(Color.White, listOf(Color(0xFF777777))))
    }

    @Test
    fun `the first readable candidate wins even when a later one has more contrast`() {
        assertEquals(Color(0xFF767676), ColorContrast.readableOn(Color.White, listOf(Color(0xFF777777), Color(0xFF767676), Color.Black)))
    }

    @Test
    fun `dark text is taken on a light colour and light text on a dark one`() {
        val dark = Color(0xFF1B1B1F)
        val light = Color(0xFFF2F2F5)
        assertEquals(dark, ColorContrast.readableOn(Color(0xFFC9E6FF), listOf(dark, light)))
        assertEquals(light, ColorContrast.readableOn(Color(0xFF1B2333), listOf(dark, light)))
    }

    @Test
    fun `a mid grey where neither candidate is readable falls back to black or white`() {
        val dark = Color(0xFF1B1B1F)
        val light = Color(0xFFF2F2F5)
        assertEquals(Color.Black, ColorContrast.readableOn(Color(0xFF777777), listOf(dark, light)))
        assertEquals(Color.White, ColorContrast.readableOn(Color(0xFF6A6A6A), listOf(Color(0xFF5A5A5A))))
    }

    @Test
    fun `with no candidates the answer is plain black or white`() {
        assertEquals(Color.Black, ColorContrast.readableOn(Color(0xFFFFDEA6)))
        assertEquals(Color.White, ColorContrast.readableOn(Color(0xFF1E2B22)))
    }

    @Test
    fun `pastels count as light and the dark tones as dark`() {
        assertTrue(ColorContrast.isLight(Color(0xFFFFDAD6)))
        assertTrue(ColorContrast.isLight(Color(0xFFBCECCB)))
        assertFalse(ColorContrast.isLight(Color(0xFF2B2D31)))
        assertFalse(ColorContrast.isLight(Color(0xFF8E24AA)))
    }

    @Test
    fun `a light colour shifts darker and a dark one lighter`() {
        val sky = Color(0xFFC9E6FF)
        val graphite = Color(0xFF2B2D31)

        assertTrue(ColorContrast.luminance(ColorContrast.tonalShift(sky)) < ColorContrast.luminance(sky))
        assertTrue(ColorContrast.luminance(ColorContrast.tonalShift(graphite)) > ColorContrast.luminance(graphite))
    }

    @Test
    fun `the shift is twelve percent towards black and fifteen towards white`() {
        val fromWhite = ColorContrast.tonalShift(Color.White)
        val fromBlack = ColorContrast.tonalShift(Color.Black)

        assertEquals(0.88f, fromWhite.red, 0.002f)
        assertEquals(0.88f, fromWhite.blue, 0.002f)
        assertEquals(0.15f, fromBlack.green, 0.002f)
    }

    @Test
    fun `shifting the extremes stays inside the colour range`() {
        listOf(Color.White, Color.Black, Color(0xFFFF0000), Color(0xFF00FFFF)).forEach { color ->
            val shifted = ColorContrast.tonalShift(color)
            listOf(shifted.red, shifted.green, shifted.blue).forEach { channel ->
                assertTrue("$color -> $shifted", channel in 0f..1f)
            }
        }
    }

    @Test
    fun `the shifted colour is opaque even when the input is see-through`() {
        assertEquals(1f, ColorContrast.tonalShift(Color(0xFF2B2D31).copy(alpha = 0.45f)).alpha, 0f)
    }
}
