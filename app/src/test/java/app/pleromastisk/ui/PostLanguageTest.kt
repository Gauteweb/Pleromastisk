package app.pleromastisk.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PostLanguageTest {
    @Test fun valgeneErNynorskBokmålOgEngelsk() = assertEquals(listOf("nn", "nb", "en"), PostLanguages)
    @Test fun kjenteKoder() {
        assertEquals("nb", normalizePostLanguage("nb"))
        assertEquals("nn", normalizePostLanguage("nn"))
        assertEquals("en", normalizePostLanguage("en"))
    }
    @Test fun regionOgStoreBokstaver() {
        assertEquals("nb", normalizePostLanguage("nb-NO"))
        assertEquals("en", normalizePostLanguage("EN_us"))
    }
    @Test fun norskRegnesSomBokmål() = assertEquals("nb", normalizePostLanguage("no"))
    @Test fun andreSpråkOgTomtGirNull() {
        assertNull(normalizePostLanguage("sv"))
        assertNull(normalizePostLanguage(""))
        assertNull(normalizePostLanguage(null))
    }
}
