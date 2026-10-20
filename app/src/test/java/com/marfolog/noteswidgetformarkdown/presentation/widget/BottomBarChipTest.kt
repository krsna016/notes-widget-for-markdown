package com.marfolog.noteswidgetformarkdown.presentation.widget

import com.marfolog.noteswidgetformarkdown.R
import com.marfolog.noteswidgetformarkdown.presentation.widget.GitSyncLabel.Severity
import org.junit.Assert.assertEquals
import org.junit.Test

class BottomBarChipTest {

    private val synced = GitSyncLabel.Label(R.string.sync_synced, Severity.Ok, "09:41")
    private val stale = GitSyncLabel.Label(R.string.sync_synced, Severity.Stale, "3 d")
    private val stuck = GitSyncLabel.Label(R.string.sync_stuck, Severity.Problem)
    private val noInfo = GitSyncLabel.Label(R.string.sync_no_info, Severity.Off)

    @Test
    fun `stale sync keeps the sync chip and hides the rate chip`() {
        assertEquals(BottomBarChip.Sync(stale), bottomBarChip(rateWindowOpen = true, syncLabel = stale))
    }

    @Test
    fun `broken sync keeps the sync chip and hides the rate chip`() {
        assertEquals(BottomBarChip.Sync(stuck), bottomBarChip(rateWindowOpen = true, syncLabel = stuck))
    }

    @Test
    fun `healthy sync becomes a rate chip that keeps the green dot`() {
        assertEquals(BottomBarChip.Rate(okDot = true), bottomBarChip(rateWindowOpen = true, syncLabel = synced))
    }

    @Test
    fun `sync without information gives way to a plain rate chip`() {
        assertEquals(BottomBarChip.Rate(okDot = false), bottomBarChip(rateWindowOpen = true, syncLabel = noInfo))
    }

    @Test
    fun `sync chip switched off leaves the corner to a plain rate chip`() {
        assertEquals(BottomBarChip.Rate(okDot = false), bottomBarChip(rateWindowOpen = true, syncLabel = null))
    }

    @Test
    fun `outside a rate window the sync chip shows as it always did`() {
        listOf(synced, stale, stuck, noInfo).forEach { label ->
            assertEquals(BottomBarChip.Sync(label), bottomBarChip(rateWindowOpen = false, syncLabel = label))
        }
    }

    @Test
    fun `outside a rate window with the sync chip off the corner stays empty`() {
        assertEquals(BottomBarChip.None, bottomBarChip(rateWindowOpen = false, syncLabel = null))
    }
}
