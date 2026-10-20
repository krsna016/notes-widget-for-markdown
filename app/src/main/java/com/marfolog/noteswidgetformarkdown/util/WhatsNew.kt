package com.marfolog.noteswidgetformarkdown.util

import androidx.annotation.StringRes
import com.marfolog.noteswidgetformarkdown.R

enum class WhatsNewIcon { Palette, Checklist, Chat, Translate }

data class WhatsNewItem(val icon: WhatsNewIcon, @StringRes val textRes: Int)

/**
 * The one-time "What's new" dialog after an update: what it lists for each release, and when
 * settings show it. A release without an entry here shows nothing, so a release adds its lines
 * to [RELEASES] and nothing else.
 */
object WhatsNew {

    internal val RELEASES: Map<String, List<WhatsNewItem>> = mapOf(
        "1.3.0" to listOf(
            WhatsNewItem(WhatsNewIcon.Palette, R.string.whats_new_130_background),
            WhatsNewItem(WhatsNewIcon.Checklist, R.string.whats_new_130_fast_note),
            WhatsNewItem(WhatsNewIcon.Chat, R.string.whats_new_130_feedback),
            WhatsNewItem(WhatsNewIcon.Translate, R.string.whats_new_130_languages)
        )
    )

    /** "1.3.0-foss" and "1.3.0" are the same release. */
    fun release(versionName: String): String = versionName.substringBefore('-').trim()

    fun itemsFor(versionName: String, releases: Map<String, List<WhatsNewItem>> = RELEASES): List<WhatsNewItem> =
        releases[release(versionName)].orEmpty()

    /**
     * Whether this start brought a new release to someone who already had the app. With no
     * version recorded yet, data from before is what tells an update from a fresh install.
     */
    internal fun arrivedByUpdate(lastVersionSeen: String?, versionName: String, installedBefore: Boolean): Boolean =
        if (lastVersionSeen == null) installedBefore else release(lastVersionSeen) != release(versionName)

    /**
     * Once per release, for someone who updated to it, set up and not sent here to fix a failed
     * fast note. Fresh installs never see it: everything in it is new to them anyway.
     */
    internal fun shouldShow(
        versionName: String,
        updatedToVersion: String?,
        seenVersion: String?,
        openedFrom: String?,
        remoteEnabled: Boolean,
        setupComplete: Boolean,
        releases: Map<String, List<WhatsNewItem>> = RELEASES
    ): Boolean {
        if (!setupComplete || !remoteEnabled || openedFrom == UsageEvent.FROM_FAST_NOTE_ERROR) return false
        val current = release(versionName)
        if (itemsFor(current, releases).isEmpty()) return false
        if (updatedToVersion == null || release(updatedToVersion) != current) return false
        return seenVersion == null || release(seenVersion) != current
    }
}
