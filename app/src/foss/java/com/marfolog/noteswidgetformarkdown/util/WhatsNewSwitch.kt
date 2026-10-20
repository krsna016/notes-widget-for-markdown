package com.marfolog.noteswidgetformarkdown.util

import android.content.Context

/** FOSS flavour: no network, so no remote switch — the "What's new" dialog is always allowed. */
object WhatsNewSwitch {

    fun init(context: Context) = Unit

    fun isEnabled(context: Context): Boolean = true
}
