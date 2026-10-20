package com.marfolog.noteswidgetformarkdown.util

import java.util.concurrent.TimeUnit
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedbackScheduleTest {

    private val hour = TimeUnit.HOURS.toMillis(1)
    private val day = TimeUnit.DAYS.toMillis(1)
    private val installedAt = 1_760_000_000_000L
    private val aWeekLater = installedAt + 7 * day

    private fun offer(
        nowMillis: Long = aWeekLater,
        setupComplete: Boolean = true,
        firstSeenAtMillis: Long? = installedAt,
        openedFrom: String? = null,
        answered: Boolean = false,
        neverAgain: Boolean = false,
        snoozedAtMillis: Long? = null
    ) = FeedbackSchedule.shouldOffer(
        nowMillis = nowMillis,
        setupComplete = setupComplete,
        firstSeenAtMillis = firstSeenAtMillis,
        openedFrom = openedFrom,
        answered = answered,
        neverAgain = neverAgain,
        snoozedAtMillis = snoozedAtMillis
    )

    @Test
    fun `a set up user opening settings from the launcher exactly a week in is asked`() {
        assertTrue(offer())
    }

    @Test
    fun `a millisecond short of a week is too early`() {
        assertFalse(offer(nowMillis = aWeekLater - 1))
    }

    @Test
    fun `a user of several months who never answered is asked`() {
        assertTrue(offer(nowMillis = installedAt + 120 * day))
    }

    @Test
    fun `opening settings from the widget or the sync chip still counts`() {
        assertTrue(offer(openedFrom = UsageEvent.FROM_WIDGET))
        assertTrue(offer(openedFrom = UsageEvent.FROM_SYNC_CHIP))
    }

    @Test
    fun `someone sent here by a failed fast note is left to fix it`() {
        assertFalse(offer(openedFrom = UsageEvent.FROM_FAST_NOTE_ERROR))
    }

    @Test
    fun `nobody is asked before the notes folder and the guide are done`() {
        assertFalse(offer(setupComplete = false))
    }

    @Test
    fun `without a recorded first start there is no week to count`() {
        assertFalse(offer(firstSeenAtMillis = null))
    }

    @Test
    fun `a clock set before the first start reads as too early`() {
        assertFalse(offer(nowMillis = installedAt - day))
    }

    @Test
    fun `once sent it never comes back on its own`() {
        assertFalse(offer(answered = true, nowMillis = installedAt + 400 * day))
    }

    @Test
    fun `do not ask again is final`() {
        assertFalse(offer(neverAgain = true, nowMillis = installedAt + 400 * day))
    }

    @Test
    fun `not now holds it back for a full day`() {
        val snoozedAt = aWeekLater + 2 * hour
        assertFalse(offer(nowMillis = snoozedAt + 23 * hour, snoozedAtMillis = snoozedAt))
        assertFalse(offer(nowMillis = snoozedAt + day - 1, snoozedAtMillis = snoozedAt))
        assertTrue(offer(nowMillis = snoozedAt + day, snoozedAtMillis = snoozedAt))
    }

    @Test
    fun `a not now from weeks ago no longer holds it back`() {
        assertTrue(offer(nowMillis = installedAt + 40 * day, snoozedAtMillis = aWeekLater))
    }

    @Test
    fun `a clock set before the last not now reads as still snoozed`() {
        val snoozedAt = installedAt + 20 * day
        assertFalse(offer(nowMillis = snoozedAt - 3 * day, snoozedAtMillis = snoozedAt))
    }

    @Test
    fun `the first ever send is allowed`() {
        assertTrue(FeedbackSchedule.canSend(nowMillis = aWeekLater, lastSentAtMillis = null))
    }

    @Test
    fun `a second send within a day is refused, from exactly a day on it is allowed`() {
        val sentAt = aWeekLater
        assertFalse(FeedbackSchedule.canSend(nowMillis = sentAt, lastSentAtMillis = sentAt))
        assertFalse(FeedbackSchedule.canSend(nowMillis = sentAt + day - 1, lastSentAtMillis = sentAt))
        assertTrue(FeedbackSchedule.canSend(nowMillis = sentAt + day, lastSentAtMillis = sentAt))
    }

    @Test
    fun `a clock set before the last send does not lock feedback out`() {
        assertTrue(FeedbackSchedule.canSend(nowMillis = aWeekLater - 30 * day, lastSentAtMillis = aWeekLater))
    }

    @Test
    fun `nothing on its way means nothing is pending`() {
        assertFalse(FeedbackSchedule.isPending(nowMillis = aWeekLater, pendingSinceMillis = null))
    }

    @Test
    fun `a send that timed out blocks a retry for exactly a minute`() {
        val startedAt = aWeekLater
        val minute = TimeUnit.MINUTES.toMillis(1)
        assertTrue(FeedbackSchedule.isPending(nowMillis = startedAt, pendingSinceMillis = startedAt))
        assertTrue(FeedbackSchedule.isPending(nowMillis = startedAt + minute - 1, pendingSinceMillis = startedAt))
        assertFalse(FeedbackSchedule.isPending(nowMillis = startedAt + minute, pendingSinceMillis = startedAt))
    }

    @Test
    fun `a clock set before the timed out send does not block a retry`() {
        assertFalse(FeedbackSchedule.isPending(nowMillis = aWeekLater - hour, pendingSinceMillis = aWeekLater))
    }
}
