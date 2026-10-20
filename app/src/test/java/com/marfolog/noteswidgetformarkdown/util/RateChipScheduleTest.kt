package com.marfolog.noteswidgetformarkdown.util

import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RateChipScheduleTest {

    private val day = TimeUnit.DAYS.toMillis(1)
    private val installedAt = 1_760_000_000_000L

    private fun window(
        now: Long,
        firstSeen: Long? = installedAt,
        featureSeen: Long? = installedAt,
        rateTapped: Boolean = false,
        neverAgain: Boolean = false,
        disabled: Boolean = false
    ) = RateChipSchedule.openWindow(
        nowMillis = now,
        firstSeenAtMillis = firstSeen,
        featureSeenAtMillis = featureSeen,
        rateTapped = rateTapped,
        neverAgain = neverAgain,
        disabledInSettings = disabled
    )

    @Test
    fun `new install sees nothing during its first week`() {
        assertNull(window(now = installedAt))
        assertNull(window(now = installedAt + 7 * day - 1))
    }

    @Test
    fun `new install sees the chip exactly seven days after the first start`() {
        assertEquals(0, window(now = installedAt + 7 * day))
    }

    @Test
    fun `first window lasts two days and closes on the dot`() {
        assertEquals(0, window(now = installedAt + 9 * day - 1))
        assertNull(window(now = installedAt + 9 * day))
    }

    @Test
    fun `nothing during the fourteen days between windows`() {
        assertNull(window(now = installedAt + 15 * day))
        assertNull(window(now = installedAt + 23 * day - 1))
    }

    @Test
    fun `second window opens sixteen days after the first`() {
        assertEquals(1, window(now = installedAt + 23 * day))
        assertEquals(1, window(now = installedAt + 25 * day - 1))
        assertNull(window(now = installedAt + 25 * day))
    }

    @Test
    fun `third window is the last one`() {
        assertEquals(2, window(now = installedAt + 39 * day))
        assertEquals(2, window(now = installedAt + 41 * day - 1))
        assertNull(window(now = installedAt + 41 * day))
    }

    @Test
    fun `no fourth window, not even years later`() {
        assertNull(window(now = installedAt + 55 * day))
        assertNull(window(now = installedAt + 56 * day))
        assertNull(window(now = installedAt + 3 * 365 * day))
    }

    @Test
    fun `someone using the app for months sees it two days after updating`() {
        val updatedAt = installedAt + 120 * day
        assertNull(window(now = updatedAt + 2 * day - 1, featureSeen = updatedAt))
        assertEquals(0, window(now = updatedAt + 2 * day, featureSeen = updatedAt))
        assertEquals(1, window(now = updatedAt + 18 * day, featureSeen = updatedAt))
    }

    @Test
    fun `someone who updated on day five already waited long enough for the two day rule`() {
        val updatedAt = installedAt + 5 * day
        assertEquals(installedAt + 7 * day, RateChipSchedule.firstShowAt(installedAt, updatedAt))
        assertEquals(0, window(now = installedAt + 7 * day, featureSeen = updatedAt))
    }

    @Test
    fun `someone who updated on day three still waits for the full week`() {
        val updatedAt = installedAt + 3 * day
        assertNull(window(now = updatedAt + 2 * day, featureSeen = updatedAt))
        assertEquals(0, window(now = installedAt + 7 * day, featureSeen = updatedAt))
    }

    @Test
    fun `tapping rate hides the chip for good`() {
        assertNull(window(now = installedAt + 7 * day, rateTapped = true))
        assertNull(window(now = installedAt + 23 * day, rateTapped = true))
    }

    @Test
    fun `dont ask again hides the chip for good`() {
        assertNull(window(now = installedAt + 7 * day, neverAgain = true))
        assertNull(window(now = installedAt + 39 * day, neverAgain = true))
    }

    @Test
    fun `switching the reminder off in settings hides it`() {
        assertNull(window(now = installedAt + 8 * day, disabled = true))
    }

    @Test
    fun `missing timestamps never show the chip`() {
        assertNull(window(now = installedAt + 8 * day, firstSeen = null))
        assertNull(window(now = installedAt + 8 * day, featureSeen = null))
        assertNull(window(now = installedAt + 8 * day, firstSeen = null, featureSeen = null))
    }

    @Test
    fun `a clock that went backwards shows nothing`() {
        assertNull(window(now = installedAt - day))
        val updatedAt = installedAt + 30 * day
        assertNull(window(now = updatedAt - 1, featureSeen = updatedAt))
    }

    @Test
    fun `shown is reported for the first redraw of each window only`() {
        assertTrue(RateChipSchedule.isFirstShowOfWindow(window = 0, lastReportedWindow = null))
        assertFalse(RateChipSchedule.isFirstShowOfWindow(window = 0, lastReportedWindow = 0))
        assertTrue(RateChipSchedule.isFirstShowOfWindow(window = 1, lastReportedWindow = 0))
        assertTrue(RateChipSchedule.isFirstShowOfWindow(window = 2, lastReportedWindow = 1))
        assertFalse(RateChipSchedule.isFirstShowOfWindow(window = 1, lastReportedWindow = 2))
    }

    @Test
    fun `nothing to report while no window is open`() {
        assertFalse(RateChipSchedule.isFirstShowOfWindow(window = null, lastReportedWindow = null))
        assertFalse(RateChipSchedule.isFirstShowOfWindow(window = null, lastReportedWindow = 0))
    }

    private fun available(now: Long, firstSeen: Long? = installedAt, featureSeen: Long? = installedAt) =
        RateChipSchedule.isSettingAvailable(now, firstSeen, featureSeen)

    @Test
    fun `setting stays hidden for a new install until day seven`() {
        assertFalse(available(now = installedAt))
        assertFalse(available(now = installedAt + 7 * day - 1))
    }

    @Test
    fun `setting appears exactly when the first window opens`() {
        assertTrue(available(now = installedAt + 7 * day))
    }

    @Test
    fun `existing user gets the setting two days after the update`() {
        val updatedAt = installedAt + 90 * day
        assertFalse(available(now = updatedAt + 2 * day - 1, featureSeen = updatedAt))
        assertTrue(available(now = updatedAt + 2 * day, featureSeen = updatedAt))
    }

    @Test
    fun `setting stays hidden when a timestamp is missing`() {
        val later = installedAt + 400 * day
        assertFalse(available(now = later, firstSeen = null))
        assertFalse(available(now = later, featureSeen = null))
        assertFalse(available(now = later, firstSeen = null, featureSeen = null))
    }

    @Test
    fun `setting stays hidden when the clock is set before the first start`() {
        assertFalse(available(now = installedAt - 30 * day))
    }

    @Test
    fun `setting stays visible after every window has closed`() {
        assertTrue(available(now = installedAt + 7 * day + 100 * day))
        assertTrue(available(now = installedAt + 1000 * day))
    }
}
