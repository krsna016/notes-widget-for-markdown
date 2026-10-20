package com.marfolog.noteswidgetformarkdown.presentation.widget

import com.marfolog.noteswidgetformarkdown.domain.model.WidgetColor
import com.marfolog.noteswidgetformarkdown.domain.model.WidgetColorChoice
import com.marfolog.noteswidgetformarkdown.util.KeyValueStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetBackgroundTest {

    private class InMemoryStore : KeyValueStore {
        val values = mutableMapOf<String, Any>()
        override fun contains(key: String) = key in values
        override fun getLong(key: String) = values[key] as Long?
        override fun putLong(key: String, value: Long) { values[key] = value }
        override fun getInt(key: String) = values[key] as Int?
        override fun putInt(key: String, value: Int) { values[key] = value }
        override fun getString(key: String) = values[key] as String?
        override fun putString(key: String, value: String) { values[key] = value }
        override fun getBoolean(key: String, default: Boolean) = values[key] as Boolean? ?: default
        override fun putBoolean(key: String, value: Boolean) { values[key] = value }
        override fun remove(key: String) { values.remove(key) }
    }

    private val store = InMemoryStore()

    @Test
    fun `a user who never touched the slider gets the slightly see-through default`() {
        assertEquals(85, WidgetBackground.percent(store))
        assertTrue(WidgetBackground.needsBacking(WidgetBackground.percent(store)))
    }

    @Test
    fun `someone who explicitly saved fully opaque keeps it after the default changed`() {
        WidgetBackground.save(store, 100)

        assertEquals(100, WidgetBackground.percent(store))
    }

    @Test
    fun `fully transparent is a real choice, not a missing value`() {
        WidgetBackground.save(store, 0)

        assertEquals(0, WidgetBackground.percent(store))
    }

    @Test
    fun `a saved opacity is read back as saved`() {
        WidgetBackground.save(store, 40)

        assertEquals(40, store.values["widget_background_percent"])
        assertEquals(40, WidgetBackground.percent(store))
    }

    @Test
    fun `saving a value between steps stores the nearest step`() {
        WidgetBackground.save(store, 63)

        assertEquals(65, store.values["widget_background_percent"])
    }

    @Test
    fun `a stored value outside the range reads as the default`() {
        store.putInt("widget_background_percent", 250)
        assertEquals(85, WidgetBackground.percent(store))

        store.putInt("widget_background_percent", -30)
        assertEquals(85, WidgetBackground.percent(store))

        store.putInt("widget_background_percent", 101)
        assertEquals(85, WidgetBackground.percent(store))
    }

    @Test
    fun `a stored value off the step grid is read as the nearest step`() {
        store.putInt("widget_background_percent", 42)

        assertEquals(40, WidgetBackground.percent(store))
    }

    @Test
    fun `snapping rounds half a step up and keeps both ends`() {
        assertEquals(0, WidgetBackground.snap(0))
        assertEquals(0, WidgetBackground.snap(2))
        assertEquals(5, WidgetBackground.snap(3))
        assertEquals(95, WidgetBackground.snap(97))
        assertEquals(100, WidgetBackground.snap(98))
        assertEquals(100, WidgetBackground.snap(100))
        assertEquals(0, WidgetBackground.snap(Int.MIN_VALUE))
        assertEquals(100, WidgetBackground.snap(Int.MAX_VALUE))
    }

    @Test
    fun `a slider released between stops lands on the nearest step`() {
        assertEquals(70, WidgetBackground.fromSlider(71.9f))
        assertEquals(75, WidgetBackground.fromSlider(72.6f))
        assertEquals(0, WidgetBackground.fromSlider(0f))
        assertEquals(100, WidgetBackground.fromSlider(100f))
    }

    @Test
    fun `a slider value outside its range or not a number never escapes 0 to 100`() {
        assertEquals(0, WidgetBackground.fromSlider(-12f))
        assertEquals(100, WidgetBackground.fromSlider(130f))
        assertEquals(100, WidgetBackground.fromSlider(Float.POSITIVE_INFINITY))
        assertEquals(0, WidgetBackground.fromSlider(Float.NEGATIVE_INFINITY))
        assertEquals(85, WidgetBackground.fromSlider(Float.NaN))
    }

    @Test
    fun `slider steps give one stop for every five percent`() {
        assertEquals(19, WidgetBackground.SLIDER_STEPS)
    }

    @Test
    fun `percent maps to the alpha the renderer applies`() {
        assertEquals(1f, WidgetBackground.alpha(100), 0f)
        assertEquals(0.4f, WidgetBackground.alpha(40), 0.0001f)
        assertEquals(0f, WidgetBackground.alpha(0), 0f)
        assertEquals(1f, WidgetBackground.alpha(180), 0f)
        assertEquals(0f, WidgetBackground.alpha(-5), 0f)
    }

    @Test
    fun `only a fully opaque setting keeps the theme colour untouched`() {
        assertTrue(WidgetBackground.isOpaque(100))
        assertTrue(WidgetBackground.isOpaque(99))
        assertFalse(WidgetBackground.isOpaque(95))
        assertFalse(WidgetBackground.isOpaque(0))
    }

    @Test
    fun `text on the default widget gets no extra backing`() {
        assertFalse(WidgetBackground.needsBacking(100))
        assertFalse(WidgetBackground.needsBacking(98))
        assertFalse(WidgetBackground.needsBacking(150))
    }

    @Test
    fun `text on a see-through widget gets its own backing down to fully transparent`() {
        assertTrue(WidgetBackground.needsBacking(95))
        assertTrue(WidgetBackground.needsBacking(40))
        assertTrue(WidgetBackground.needsBacking(0))
        assertTrue(WidgetBackground.needsBacking(-20))
    }

    @Test
    fun `a user who never picked a colour keeps the system one`() {
        assertEquals(WidgetColorChoice(), WidgetBackground.color(store))
    }

    @Test
    fun `a picked colour is read back as picked`() {
        WidgetBackground.saveColor(store, WidgetColorChoice(WidgetColor.Graphite))

        assertEquals("graphite", store.values["widget_color"])
        assertEquals(WidgetColorChoice(WidgetColor.Graphite), WidgetBackground.color(store))
    }

    @Test
    fun `a custom colour keeps its hex across a switch to a preset and back`() {
        WidgetBackground.saveColor(store, WidgetColorChoice(WidgetColor.Custom, "#8E24AA"))
        WidgetBackground.saveColor(store, WidgetColorChoice(WidgetColor.Amber, "#8E24AA"))

        assertEquals(WidgetColorChoice(WidgetColor.Amber, "#8E24AA"), WidgetBackground.color(store))
    }

    @Test
    fun `an unknown stored colour from a newer or older version reads as system`() {
        store.putString("widget_color", "neon")

        assertEquals(WidgetColor.System, WidgetBackground.color(store).color)
    }

    @Test
    fun `an empty stored hex reads as no custom colour at all`() {
        WidgetBackground.saveColor(store, WidgetColorChoice(WidgetColor.Custom, null))

        assertEquals("", store.values["widget_color_hex"])
        assertEquals(WidgetColorChoice(WidgetColor.Custom, null), WidgetBackground.color(store))
    }
}
