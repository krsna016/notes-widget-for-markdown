package com.marfolog.noteswidgetformarkdown.presentation.widget

/**
 * The one chip in the right corner of the widget's bottom bar. There is room for one, so the rate
 * chip gives way whenever sync needs attention, and otherwise takes the sync chip's place — keeping
 * its green dot when sync is fine, so that reassurance does not disappear for two days.
 */
internal sealed interface BottomBarChip {
    data object None : BottomBarChip
    data class Sync(val label: GitSyncLabel.Label) : BottomBarChip
    data class Rate(val okDot: Boolean) : BottomBarChip
}

/** [syncLabel] is null when the sync chip is switched off in settings. */
internal fun bottomBarChip(rateWindowOpen: Boolean, syncLabel: GitSyncLabel.Label?): BottomBarChip {
    val severity = syncLabel?.severity
    return when {
        syncLabel != null && (!rateWindowOpen || severity == GitSyncLabel.Severity.Stale ||
            severity == GitSyncLabel.Severity.Problem) -> BottomBarChip.Sync(syncLabel)
        !rateWindowOpen -> BottomBarChip.None
        else -> BottomBarChip.Rate(okDot = severity == GitSyncLabel.Severity.Ok)
    }
}
