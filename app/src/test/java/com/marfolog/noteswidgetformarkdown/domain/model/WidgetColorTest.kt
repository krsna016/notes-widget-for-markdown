package com.marfolog.noteswidgetformarkdown.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetColorTest {

    @Test
    fun `every colour reads back from what it stores`() {
        WidgetColor.entries.forEach { color ->
            assertEquals(color, WidgetColor.fromStorage(color.storageValue))
        }
    }

    @Test
    fun `nothing stored, an unknown value or a different case is the system colour`() {
        listOf(null, "", "neon", "Graphite", " graphite").forEach { value ->
            assertEquals(value, WidgetColor.System, WidgetColor.fromStorage(value))
        }
    }

    @Test
    fun `someone who had picked the retired midnight colour gets graphite`() {
        assertEquals(WidgetColor.Graphite, WidgetColor.fromStorage("midnight"))
    }

    @Test
    fun `midnight is no longer offered`() {
        assertTrue(WidgetColor.entries.none { it.storageValue == "midnight" })
    }

    @Test
    fun `stored values double as analytics values`() {
        WidgetColor.entries.forEach { color ->
            assertTrue(color.storageValue, color.storageValue.matches(Regex("[a-z]{3,36}")))
        }
    }
}
