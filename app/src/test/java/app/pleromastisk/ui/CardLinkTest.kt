package app.pleromastisk.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CardLinkTest {
    private val feber = """<a href="https://feber.se/a/497520/?utm_source=rss&amp;utm_medium=feed"><span class="invisible">https://</span><span class="ellipsis">feber.se/a/4975</span><span class="invisible">20/?utm_source=rss&amp;utm_medium=feed</span></a>"""
    private val feberUrl = "https://feber.se/a/497520/?utm_source=rss&utm_medium=feed"

    @Test fun skjulerLenkeSomStarAleneIEgetAvsnitt() {
        val html = "<p>Rysk satellit</p><p>$feber</p>"
        assertEquals("<p>Rysk satellit</p>", stripCardLink(html, feberUrl))
    }

    @Test fun skjulerLenkeEtterLinjeskift() {
        assertEquals("Tekst", stripCardLink("Tekst<br/><a href=\"https://a.no/x\">a.no/x</a>", "https://a.no/x"))
    }

    @Test fun beholderHashtagsEtterLenken() {
        val tag = "<a href=\"https://m.no/tags/x\" class=\"hashtag\">#x</a>"
        val html = "<p>Hei</p><p><a href=\"https://a.no/x\">a.no/x</a></p><p>$tag</p>"
        assertEquals("<p>Hei</p><p>$tag</p>", stripCardLink(html, "https://a.no/x"))
    }

    @Test fun beholderLenkeMidtISetning() {
        val html = "<p>Se <a href=\"https://a.no/x\">a.no/x</a> for mer</p>"
        assertEquals(html, stripCardLink(html, "https://a.no/x"))
    }

    @Test fun beholderLenkeSomIkkeMatcherKortet() {
        val html = "<p><a href=\"https://a.no/x\">a.no/x</a></p>"
        assertEquals(html, stripCardLink(html, "https://b.no/y"))
    }

    @Test fun ignorererSluttskråstrekOgAmpersandKoding() {
        val html = "<p><a href=\"https://a.no/x/\">x</a></p><p>Tekst</p>"
        assertEquals("<p>Tekst</p>", stripCardLink(html, "https://a.no/x"))
        val html2 = "<p><a href=\"https://a.no/x?a=1&amp;b=2\">x</a></p>"
        assertTrue(isBlankHtml(stripCardLink(html2, "https://a.no/x?a=1&b=2")))
    }

    @Test fun kunLenkeGirTomTekst() {
        assertTrue(isBlankHtml(stripCardLink("<p>$feber</p>", feberUrl)))
    }
}
