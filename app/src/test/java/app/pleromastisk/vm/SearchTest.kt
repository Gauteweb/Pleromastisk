package app.pleromastisk.vm

import org.junit.Assert.assertEquals
import org.junit.Test

class SearchTest {
    @Test fun skrevetEmneknaggKommerFørst() {
        assertEquals(listOf("nytt", "linux"), withTypedTag("nytt", listOf("linux")))
    }

    @Test fun ingenDuplikatOgTreffFlyttesFørst() {
        assertEquals(listOf("Linux", "linuxqc"), withTypedTag("linux", listOf("linuxqc", "Linux")))
    }

    @Test fun ugyldigNavnLeggesIkkeTil() {
        assertEquals(listOf("a"), withTypedTag("to ord", listOf("a")))
        assertEquals(listOf("a"), withTypedTag("a-b!", listOf("a")))
    }

    @Test fun støtterNorskeBokstaverOgTall() {
        assertEquals(listOf("høst2026"), withTypedTag("høst2026", emptyList()))
    }
}
