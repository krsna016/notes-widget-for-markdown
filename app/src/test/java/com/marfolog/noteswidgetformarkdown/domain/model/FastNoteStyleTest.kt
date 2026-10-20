package com.marfolog.noteswidgetformarkdown.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class FastNoteStyleTest {

    @Test
    fun `stored values read back as the same style`() {
        assertEquals(FastNoteStyle.Bullet, FastNoteStyle.from("bullet"))
        assertEquals(FastNoteStyle.Checkbox, FastNoteStyle.from("checkbox"))
    }

    @Test
    fun `nothing stored keeps the bullet everyone had before`() {
        assertEquals(FastNoteStyle.Bullet, FastNoteStyle.from(null))
    }

    @Test
    fun `an unknown stored value falls back to the bullet`() {
        assertEquals(FastNoteStyle.Bullet, FastNoteStyle.from("numbered"))
    }
}
