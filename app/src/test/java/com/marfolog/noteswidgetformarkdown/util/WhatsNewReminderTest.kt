package com.marfolog.noteswidgetformarkdown.util

import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsNewReminderTest {

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
    private val installedAt = 1_760_000_000_000L
    private val store = InMemoryStore()

    /** The same order the app uses: this first, then the rate chip's first start. */
    private fun start(versionName: String, at: Long) {
        WhatsNewReminder.recordStart(store, versionName)
        RateReminder.recordStart(store, at)
    }

    private fun show(versionName: String = "1.3.0", openedFrom: String? = null, remoteEnabled: Boolean = true) =
        WhatsNewReminder.shouldShow(store, versionName, setupComplete = true, openedFrom = openedFrom, remoteEnabled = remoteEnabled)

    @Test
    fun `a fresh install of 1_3_0 never sees it`() {
        start("1.3.0", installedAt)
        store.putString("notes_uri", "content://tree/MyNotes")

        assertFalse(show())
        start("1.3.0", installedAt + 10 * day)
        assertFalse(show())
    }

    @Test
    fun `a Play user updating from 1_2_1 sees it`() {
        store.putLong("rating_first_seen_at", installedAt)
        start("1.3.0", installedAt + 90 * day)

        assertTrue(show())
    }

    @Test
    fun `a foss user updating with a notes folder already set sees it`() {
        store.putString("notes_uri", "content://tree/MyNotes")
        start("1.3.0-foss", installedAt + 90 * day)

        assertTrue(show("1.3.0-foss"))
    }

    @Test
    fun `once shown it is gone, even after the next process start`() {
        store.putLong("rating_first_seen_at", installedAt)
        start("1.3.0", installedAt + 90 * day)
        WhatsNewReminder.markShown(store, "1.3.0", installedAt + 90 * day)

        assertFalse(show())
        start("1.3.0", installedAt + 91 * day)
        assertFalse(show())
        assertEquals("1.3.0", store.values["whats_new_seen_version"])
    }

    @Test
    fun `the remote switch or a failed fast note holds it back without using it up`() {
        store.putLong("rating_first_seen_at", installedAt)
        start("1.3.0", installedAt + 90 * day)

        assertFalse(show(remoteEnabled = false))
        assertFalse(show(openedFrom = UsageEvent.FROM_FAST_NOTE_ERROR))
        assertTrue(show())
    }

    @Test
    fun `a fresh install that later updates sees the next release`() {
        start("1.3.0", installedAt)
        store.putString("notes_uri", "content://tree/MyNotes")
        start("1.4.0", installedAt + 60 * day)

        assertEquals("1.4.0", store.values["whats_new_updated_to"])
        assertEquals("1.4.0", store.values["last_version_seen"])
    }

    @Test
    fun `when it was shown is what the rate chip waits on`() {
        assertNull(WhatsNewReminder.shownAt(store))
        WhatsNewReminder.markShown(store, "1.3.0", installedAt + 5 * day)

        assertEquals(installedAt + 5 * day, WhatsNewReminder.shownAt(store))
    }

    @Test
    fun `the rate chip of an updated user waits a week after what's new`() {
        store.putLong("rating_first_seen_at", installedAt)
        val updatedAt = installedAt + 90 * day
        start("1.3.0", updatedAt)
        WhatsNewReminder.markShown(store, "1.3.0", updatedAt + day)

        assertNull(RateReminder.openWindow(store, updatedAt + 2 * day))
        assertNull(RateReminder.openWindow(store, updatedAt + 8 * day - 1))
        assertEquals(0, RateReminder.openWindow(store, updatedAt + 8 * day))
        assertFalse(RateReminder.isSettingAvailable(store, updatedAt + 8 * day - 1))
        assertTrue(RateReminder.isSettingAvailable(store, updatedAt + 8 * day))
    }
}
