package com.marfolog.noteswidgetformarkdown.presentation.widget

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetInventoryTest {

    @Test
    fun `a widget placed next to known ones is the only new one`() {
        assertEquals(setOf(42), WidgetInventory.newIds(current = setOf(7, 12, 42), known = setOf(7, 12)))
    }

    @Test
    fun `a routine update of known widgets adds nothing`() {
        assertEquals(emptySet<Int>(), WidgetInventory.newIds(current = setOf(7, 12), known = setOf(7, 12)))
    }

    @Test
    fun `the very first widget is new`() {
        assertEquals(setOf(3), WidgetInventory.newIds(current = setOf(3), known = emptySet()))
    }

    @Test
    fun `an update for a subset of widgets reports none of them`() {
        assertEquals(emptySet<Int>(), WidgetInventory.newIds(current = setOf(12), known = setOf(7, 12)))
    }
}
