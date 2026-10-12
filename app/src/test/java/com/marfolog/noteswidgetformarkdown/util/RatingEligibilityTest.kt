package com.marfolog.noteswidgetformarkdown.util

import java.util.concurrent.TimeUnit
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RatingEligibilityTest {

    private val day = TimeUnit.DAYS.toMillis(1)
    private val firstSeen = 1_000_000_000_000L

    private fun eligible(
        daysSinceFirstSeen: Long,
        daysSinceLastRequest: Long? = null,
        firstSeenAt: Long? = firstSeen
    ) = RatingEligibility.isEligible(
        nowMillis = firstSeen + daysSinceFirstSeen * day,
        firstSeenAtMillis = firstSeenAt,
        lastRequestAtMillis = daysSinceLastRequest?.let { firstSeen + (daysSinceFirstSeen - it) * day }
    )

    @Test
    fun `five days after first seen, never asked, is eligible`() {
        assertTrue(eligible(daysSinceFirstSeen = 5))
    }

    @Test
    fun `too soon after first seen is not eligible`() {
        assertFalse(eligible(daysSinceFirstSeen = 4))
    }

    /** An existing user who updated has no first start recorded yet — that is not day zero. */
    @Test
    fun `no recorded first start is not eligible`() {
        assertFalse(eligible(daysSinceFirstSeen = 400, firstSeenAt = null))
    }

    @Test
    fun `asked within the last 90 days is not eligible`() {
        assertFalse(eligible(daysSinceFirstSeen = 200, daysSinceLastRequest = 89))
    }

    @Test
    fun `90 days after the last request it may ask again`() {
        assertTrue(eligible(daysSinceFirstSeen = 200, daysSinceLastRequest = 90))
    }

    @Test
    fun `a clock that went backwards is not eligible`() {
        assertFalse(eligible(daysSinceFirstSeen = -10))
        assertFalse(eligible(daysSinceFirstSeen = 200, daysSinceLastRequest = -5))
    }
}
