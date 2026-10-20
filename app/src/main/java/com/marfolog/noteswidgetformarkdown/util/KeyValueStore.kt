package com.marfolog.noteswidgetformarkdown.util

import android.content.SharedPreferences

/** The few SharedPreferences calls pure logic needs, so it can be tested with an in-memory map. */
internal interface KeyValueStore {
    fun contains(key: String): Boolean
    fun getLong(key: String): Long?
    fun putLong(key: String, value: Long)
    fun getInt(key: String): Int?
    fun putInt(key: String, value: Int)
    fun getBoolean(key: String, default: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
    fun remove(key: String)
}

internal class SharedPreferencesStore(private val prefs: SharedPreferences) : KeyValueStore {
    override fun contains(key: String) = prefs.contains(key)
    override fun getLong(key: String) = if (prefs.contains(key)) prefs.getLong(key, 0L) else null
    override fun putLong(key: String, value: Long) = prefs.edit().putLong(key, value).apply()
    override fun getInt(key: String) = if (prefs.contains(key)) prefs.getInt(key, 0) else null
    override fun putInt(key: String, value: Int) = prefs.edit().putInt(key, value).apply()
    override fun getBoolean(key: String, default: Boolean) = prefs.getBoolean(key, default)
    override fun putBoolean(key: String, value: Boolean) = prefs.edit().putBoolean(key, value).apply()
    override fun remove(key: String) = prefs.edit().remove(key).apply()
}
