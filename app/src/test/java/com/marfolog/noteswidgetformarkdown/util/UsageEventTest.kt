package com.marfolog.noteswidgetformarkdown.util

import com.marfolog.noteswidgetformarkdown.domain.model.FastNoteStyle
import com.marfolog.noteswidgetformarkdown.presentation.widget.OpenWith
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageEventTest {

    @Test
    fun `widget type tells cards, list and both apart`() {
        assertEquals("cards", UsageEvent.widgetType(cardWidgets = 1, listWidgets = 0))
        assertEquals("list", UsageEvent.widgetType(cardWidgets = 0, listWidgets = 2))
        assertEquals("both", UsageEvent.widgetType(cardWidgets = 3, listWidgets = 1))
        assertEquals("none", UsageEvent.widgetType(cardWidgets = 0, listWidgets = 0))
    }

    @Test
    fun `settings opened from a known entry point keeps it`() {
        assertEquals("widget", UsageEvent.openedFrom("widget"))
        assertEquals("sync_chip", UsageEvent.openedFrom("sync_chip"))
        assertEquals("fast_note_error", UsageEvent.openedFrom("fast_note_error"))
    }

    @Test
    fun `settings opened without an extra came from the launcher`() {
        assertEquals("launcher", UsageEvent.openedFrom(null))
    }

    @Test
    fun `an unexpected extra is never sent as is`() {
        assertEquals("launcher", UsageEvent.openedFrom("Groceries.md"))
    }

    @Test
    fun `switches and flags map to fixed words`() {
        assertEquals("on", UsageEvent.onOff(true))
        assertEquals("off", UsageEvent.onOff(false))
        assertEquals("yes", UsageEvent.yesNo(true))
        assertEquals("no", UsageEvent.yesNo(false))
    }

    @Test
    fun `user properties describe the setup in fixed values`() {
        assertEquals(
            listOf(
                "widget_type" to "both",
                "sync_mode" to "git",
                "open_with" to "default",
                "has_extra_notes" to "yes",
                "fast_note_style" to "checkbox"
            ),
            UsageProperties.of(
                cardWidgets = 1,
                listWidgets = 1,
                syncMode = "git",
                openWith = OpenWith.DefaultApp,
                hasExtraNotes = true,
                fastNoteStyle = FastNoteStyle.Checkbox
            )
        )
    }

    @Test
    fun `an unknown sync mode in prefs is reported as none`() {
        val properties = UsageProperties.of(0, 0, "syncthing", OpenWith.Obsidian, false, FastNoteStyle.Bullet).toMap()
        assertEquals("none", properties["sync_mode"])
        assertEquals("none", properties["widget_type"])
        assertEquals("no", properties["has_extra_notes"])
    }

    @Test
    fun `user property names and values stay within the Firebase limits`() {
        OpenWith.entries.forEach { openWith ->
            FastNoteStyle.entries.forEach { style ->
                UsageProperties.of(1, 1, "other", openWith, true, style).forEach { (name, value) ->
                    assertTrue(name, name.length <= 24)
                    assertTrue(value, value.length <= 36)
                }
            }
        }
    }

    @Test
    fun `event and parameter names stay within the Firebase limit`() {
        val names = UsageEvent::class.java.declaredFields
            .filter { it.type == String::class.java && java.lang.reflect.Modifier.isStatic(it.modifiers) }
            .map { it.isAccessible = true; it.get(null) as String }
        assertTrue(names.isNotEmpty())
        names.forEach { name ->
            assertTrue(name, name.length <= 40)
            assertTrue(name, name.matches(Regex("[a-z][a-z0-9_]*")))
        }
    }
}
