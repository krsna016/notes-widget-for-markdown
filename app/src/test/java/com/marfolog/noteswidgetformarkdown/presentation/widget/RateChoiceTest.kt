package com.marfolog.noteswidgetformarkdown.presentation.widget

import org.junit.Assert.assertEquals
import org.junit.Test

class RateChoiceTest {

    @Test
    fun `each choice is reported under its own fixed name`() {
        assertEquals(
            listOf("rate", "idea", "later", "never"),
            RateChoice.entries.map { it.eventValue }
        )
    }

    @Test
    fun `only rate and dont ask again take the chip away`() {
        assertEquals(
            listOf(RateChoice.Rate, RateChoice.Never),
            RateChoice.entries.filter { it.hidesChip }
        )
    }
}
