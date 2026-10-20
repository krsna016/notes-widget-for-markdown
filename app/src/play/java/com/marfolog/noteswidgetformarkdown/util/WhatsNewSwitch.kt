package com.marfolog.noteswidgetformarkdown.util

import android.content.Context
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.marfolog.noteswidgetformarkdown.BuildConfig
import java.util.concurrent.TimeUnit

/**
 * Play flavour: a remote off switch for the "What's new" dialog, in case a release needs it gone
 * without another release. Fetched in the background at start and only used from the next check
 * on — the dialog never waits for the network.
 *
 * Fetching registers an installation with Firebase, so it only happens with reporting switched
 * on; everyone else gets the built-in default. The foss twin has no switch at all.
 */
object WhatsNewSwitch {

    private const val AREA = "WhatsNew"
    private const val PARAM_ENABLED = "whats_new_enabled"
    private const val DEFAULT_ENABLED = true

    private val FETCH_INTERVAL_SECONDS = if (BuildConfig.DEBUG) 0L else TimeUnit.HOURS.toSeconds(1)

    fun init(context: Context) {
        if (!Telemetry.isEnabled(context)) return
        runCatching {
            val config = FirebaseRemoteConfig.getInstance()
            config.setConfigSettingsAsync(
                remoteConfigSettings { minimumFetchIntervalInSeconds = FETCH_INTERVAL_SECONDS }
            )
            config.setDefaultsAsync(mapOf(PARAM_ENABLED to DEFAULT_ENABLED))
            config.fetchAndActivate()
                .addOnFailureListener { AppLog.d(AREA, "Remote switch not fetched: ${it.javaClass.simpleName}") }
        }.onFailure { AppLog.d(AREA, "Remote switch unavailable: ${it.javaClass.simpleName}") }
    }

    /** Only a value that actually came from the server can switch it off. */
    fun isEnabled(context: Context): Boolean {
        if (!Telemetry.isEnabled(context)) return DEFAULT_ENABLED
        return runCatching {
            val value = FirebaseRemoteConfig.getInstance().getValue(PARAM_ENABLED)
            if (value.source == FirebaseRemoteConfig.VALUE_SOURCE_REMOTE) value.asBoolean() else DEFAULT_ENABLED
        }.getOrDefault(DEFAULT_ENABLED)
    }
}
