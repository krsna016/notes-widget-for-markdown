package com.marfolog.noteswidgetformarkdown.util

import com.marfolog.noteswidgetformarkdown.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsNewTest {

    private val nextRelease = mapOf(
        "1.3.0" to WhatsNew.RELEASES.getValue("1.3.0"),
        "1.4.0" to listOf(WhatsNewItem(WhatsNewIcon.Chat, R.string.whats_new_130_feedback))
    )

    private fun show(
        versionName: String = "1.3.0",
        updatedToVersion: String? = "1.3.0",
        seenVersion: String? = null,
        openedFrom: String? = null,
        remoteEnabled: Boolean = true,
        setupComplete: Boolean = true,
        releases: Map<String, List<WhatsNewItem>> = WhatsNew.RELEASES
    ) = WhatsNew.shouldShow(versionName, updatedToVersion, seenVersion, openedFrom, remoteEnabled, setupComplete, releases)

    @Test
    fun `1_3_0 lists the background, fast note, feedback and new languages news in that order`() {
        assertEquals(
            listOf(
                WhatsNewItem(WhatsNewIcon.Palette, R.string.whats_new_130_background),
                WhatsNewItem(WhatsNewIcon.Checklist, R.string.whats_new_130_fast_note),
                WhatsNewItem(WhatsNewIcon.Chat, R.string.whats_new_130_feedback),
                WhatsNewItem(WhatsNewIcon.Translate, R.string.whats_new_130_languages)
            ),
            WhatsNew.itemsFor("1.3.0")
        )
    }

    @Test
    fun `the foss build of a release finds the same list`() {
        assertEquals(WhatsNew.itemsFor("1.3.0"), WhatsNew.itemsFor("1.3.0-foss"))
        assertEquals("1.3.0", WhatsNew.release("1.3.0-foss"))
    }

    @Test
    fun `a release without an entry has nothing to list`() {
        assertTrue(WhatsNew.itemsFor("1.2.1").isEmpty())
        assertTrue(WhatsNew.itemsFor("").isEmpty())
    }

    @Test
    fun `someone who updated to 1_3_0 and is set up sees it`() {
        assertTrue(show())
    }

    @Test
    fun `opening settings from the widget or the sync chip still counts`() {
        assertTrue(show(openedFrom = UsageEvent.FROM_WIDGET))
        assertTrue(show(openedFrom = UsageEvent.FROM_SYNC_CHIP))
    }

    @Test
    fun `a fresh install never sees it`() {
        assertFalse(show(updatedToVersion = null))
    }

    @Test
    fun `once seen for this release it stays away`() {
        assertFalse(show(seenVersion = "1.3.0"))
        assertFalse(show(versionName = "1.3.0-foss", updatedToVersion = "1.3.0", seenVersion = "1.3.0"))
    }

    @Test
    fun `the next release with its own news shows again`() {
        assertTrue(show(versionName = "1.4.0", updatedToVersion = "1.4.0", seenVersion = "1.3.0", releases = nextRelease))
    }

    @Test
    fun `an update to a release without news shows nothing`() {
        assertFalse(show(versionName = "1.3.1", updatedToVersion = "1.3.1", seenVersion = "1.3.0"))
    }

    @Test
    fun `an update recorded for an older release does not count for this one`() {
        assertFalse(show(versionName = "1.4.0", updatedToVersion = "1.3.0", seenVersion = "1.3.0", releases = nextRelease))
    }

    @Test
    fun `the remote switch turned off holds it back`() {
        assertFalse(show(remoteEnabled = false))
    }

    @Test
    fun `someone sent here by a failed fast note is left to fix it`() {
        assertFalse(show(openedFrom = UsageEvent.FROM_FAST_NOTE_ERROR))
    }

    @Test
    fun `nobody sees it before setup is complete`() {
        assertFalse(show(setupComplete = false))
    }

    @Test
    fun `with no version recorded, earlier data marks an update and its absence a fresh install`() {
        assertTrue(WhatsNew.arrivedByUpdate(lastVersionSeen = null, versionName = "1.3.0", installedBefore = true))
        assertFalse(WhatsNew.arrivedByUpdate(lastVersionSeen = null, versionName = "1.3.0", installedBefore = false))
    }

    @Test
    fun `a recorded older version means an update, the same one does not`() {
        assertTrue(WhatsNew.arrivedByUpdate("1.3.0", "1.4.0", installedBefore = true))
        assertFalse(WhatsNew.arrivedByUpdate("1.3.0", "1.3.0-foss", installedBefore = true))
    }
}
