package com.marfolog.noteswidgetformarkdown.util

import java.io.File
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageTest {

    private fun locales(vararg tags: String) = tags.map(Locale::forLanguageTag)

    private fun shown(app: List<Locale> = emptyList(), system: List<Locale>) =
        AppLanguage.shown(app, system).toLanguageTag()

    @Test
    fun `a phone in Czech shows the app in Czech`() {
        assertEquals("cs", shown(system = locales("cs-CZ")))
    }

    @Test
    fun `a language picked for the app wins over the phone's`() {
        assertEquals("de", shown(app = locales("de"), system = locales("cs-CZ", "en-US")))
    }

    @Test
    fun `the first of the phone's languages the app has is the one shown`() {
        assertEquals("fr", shown(system = locales("pl-PL", "fr-FR", "de-DE")))
    }

    @Test
    fun `a phone language the app does not have falls back to English`() {
        assertEquals("en", shown(system = locales("pl-PL", "sk-SK")))
        assertEquals("en", shown(system = emptyList()))
    }

    @Test
    fun `Portuguese from Portugal still gets the Brazilian translation`() {
        assertEquals("pt-BR", shown(system = locales("pt-PT")))
        assertEquals("pt-BR", shown(system = locales("pt-BR")))
    }

    @Test
    fun `simplified Chinese gets the Chinese translation, traditional does not`() {
        assertEquals("zh-CN", shown(system = locales("zh-Hans-CN")))
        assertEquals("zh-CN", shown(system = locales("zh-SG")))
        assertEquals("en", shown(system = locales("zh-TW")))
        assertEquals("en", shown(system = locales("zh-Hant-HK")))
        assertEquals("ja", shown(system = locales("zh-Hant-TW", "ja-JP")))
    }

    @Test
    fun `English in any region is English`() {
        assertEquals("en", shown(system = locales("en-GB")))
    }

    @Test
    fun `the languages the app knows are exactly the ones offered in system settings`() {
        val config = File("src/main/res/xml/locales_config.xml").readText()
        val offered = Regex("""android:name="([^"]+)"""").findAll(config).map { it.groupValues[1] }.toList()

        assertEquals(offered.sorted(), AppLanguage.SUPPORTED.sorted())
        assertEquals("en", AppLanguage.SUPPORTED.first())
    }

    @Test
    fun `the row names the language in that language, capitalised`() {
        assertEquals("English", AppLanguage.displayName(Locale.forLanguageTag("en")))
        assertEquals("Čeština", AppLanguage.displayName(Locale.forLanguageTag("cs")))
        assertEquals("Deutsch", AppLanguage.displayName(Locale.forLanguageTag("de")))
        assertEquals("Українська", AppLanguage.displayName(Locale.forLanguageTag("uk")))
    }
}
