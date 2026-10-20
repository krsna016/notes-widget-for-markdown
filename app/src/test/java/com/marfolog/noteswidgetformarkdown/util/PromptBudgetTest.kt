package com.marfolog.noteswidgetformarkdown.util

import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PromptBudgetTest {

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

    private val day = TimeUnit.DAYS.toMillis(1)
    private val openedAt = 1_760_000_000_000L
    private val everything = setOf(AutoPrompt.Consent, AutoPrompt.WhatsNew, AutoPrompt.Feedback)

    private fun pick(
        due: Set<AutoPrompt>,
        nowMillis: Long = openedAt,
        lastAutoPromptAtMillis: Long? = null,
        promptedThisVisit: Boolean = false
    ) = PromptBudget.pick(due, nowMillis, lastAutoPromptAtMillis, promptedThisVisit)

    @Test
    fun `nothing due means no dialog`() {
        assertNull(pick(emptySet()))
    }

    @Test
    fun `the only dialog due is shown when none came before`() {
        assertEquals(AutoPrompt.Feedback, pick(setOf(AutoPrompt.Feedback)))
        assertEquals(AutoPrompt.WhatsNew, pick(setOf(AutoPrompt.WhatsNew)))
    }

    @Test
    fun `the reporting question goes before everything else`() {
        assertEquals(AutoPrompt.Consent, pick(everything))
    }

    @Test
    fun `what's new goes before the feedback question`() {
        assertEquals(AutoPrompt.WhatsNew, pick(setOf(AutoPrompt.Feedback, AutoPrompt.WhatsNew)))
    }

    @Test
    fun `a second dialog in the same visit is refused, rotation included`() {
        assertNull(pick(setOf(AutoPrompt.Feedback), promptedThisVisit = true))
        assertNull(pick(everything, promptedThisVisit = true))
    }

    @Test
    fun `three days of quiet after any automatic dialog, from exactly three days on it is allowed`() {
        val lastAt = openedAt - 3 * day
        assertNull(pick(setOf(AutoPrompt.Feedback), nowMillis = lastAt + day, lastAutoPromptAtMillis = lastAt))
        assertNull(pick(setOf(AutoPrompt.Feedback), nowMillis = lastAt + 3 * day - 1, lastAutoPromptAtMillis = lastAt))
        assertEquals(
            AutoPrompt.Feedback,
            pick(setOf(AutoPrompt.Feedback), nowMillis = lastAt + 3 * day, lastAutoPromptAtMillis = lastAt)
        )
    }

    @Test
    fun `the quiet days hold back even the reporting question`() {
        assertNull(pick(everything, nowMillis = openedAt + day, lastAutoPromptAtMillis = openedAt))
    }

    @Test
    fun `a clock set before the last dialog reads as not yet`() {
        assertNull(pick(setOf(AutoPrompt.Feedback), nowMillis = openedAt - 10 * day, lastAutoPromptAtMillis = openedAt))
    }

    @Test
    fun `a dialog months ago no longer holds anything back`() {
        assertEquals(
            AutoPrompt.WhatsNew,
            pick(setOf(AutoPrompt.WhatsNew), nowMillis = openedAt + 90 * day, lastAutoPromptAtMillis = openedAt)
        )
    }

    @Test
    fun `what's new today makes the feedback question wait three days`() {
        val store = InMemoryStore()
        val both = setOf(AutoPrompt.WhatsNew, AutoPrompt.Feedback)

        assertEquals(AutoPrompt.WhatsNew, PromptBudget.pick(store, both, openedAt, promptedThisVisit = false))
        PromptBudget.markShown(store, openedAt)

        val nextVisit = openedAt + day
        assertNull(PromptBudget.pick(store, setOf(AutoPrompt.Feedback), nextVisit, promptedThisVisit = false))
        assertEquals(
            AutoPrompt.Feedback,
            PromptBudget.pick(store, setOf(AutoPrompt.Feedback), openedAt + 3 * day, promptedThisVisit = false)
        )
    }

    @Test
    fun `the time of the last automatic dialog is what gets stored`() {
        val store = InMemoryStore()
        PromptBudget.markShown(store, openedAt)

        assertEquals(openedAt, store.values["last_auto_prompt_at"])
    }

    @Test
    fun `a fresh store holds nothing back`() {
        assertEquals(AutoPrompt.Feedback, PromptBudget.pick(InMemoryStore(), setOf(AutoPrompt.Feedback), openedAt, false))
    }
}
