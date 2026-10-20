package com.marfolog.noteswidgetformarkdown.util

import java.util.Locale

/**
 * Which language the app is shown in. Android 13 added a per-app language page to system
 * settings, and the settings row opens it; on older versions the app simply follows the phone.
 */
object AppLanguage {

    /** The same list as `res/xml/locales_config.xml`. The first one is what the app falls back to. */
    val SUPPORTED: List<String> = listOf("en", "cs", "de", "es", "fr", "it", "ja", "pt-BR", "ru", "uk", "zh-CN")

    /**
     * The language picked for the app if there is one, otherwise the first of the phone's
     * languages the app has, otherwise the fallback.
     */
    fun shown(appLocales: List<Locale>, systemLocales: List<Locale>, supported: List<String> = SUPPORTED): Locale {
        val available = supported.map(Locale::forLanguageTag)
        val wanted = appLocales.ifEmpty { systemLocales }
        return wanted.firstNotNullOfOrNull { match(it, available) } ?: available.first()
    }

    /** In its own language, as the system's language list writes it: "Čeština", "Deutsch". */
    fun displayName(locale: Locale): String =
        locale.getDisplayName(locale).replaceFirstChar { it.titlecase(locale) }

    private fun match(locale: Locale, available: List<Locale>): Locale? {
        if (locale.language == CHINESE && isTraditionalChinese(locale)) return null
        return available.firstOrNull { it.language == locale.language && it.country == locale.country }
            ?: available.firstOrNull { it.language == locale.language }
    }

    // Only Simplified Chinese is translated, and Android does not offer it in place of Traditional.
    private fun isTraditionalChinese(locale: Locale): Boolean =
        locale.script == "Hant" || (locale.script.isEmpty() && locale.country in TRADITIONAL_CHINESE_REGIONS)

    private const val CHINESE = "zh"
    private val TRADITIONAL_CHINESE_REGIONS = setOf("TW", "HK", "MO")
}
