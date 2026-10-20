package app.pleromastisk.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HashtagTest {
    @Test fun mastodonLenke() = assertEquals("NRKno", hashtagFromLink("https://mastodon.social/tags/NRKno", "#NRKno"))
    @Test fun pleromaLenke() = assertEquals("linux", hashtagFromLink("https://social.gaute.space/tag/linux", "#linux"))
    @Test fun pixelfedLenke() = assertEquals("art", hashtagFromLink("https://pixelfed.social/discover/tags/art?x=1", "#art"))
    @Test fun prosentkodetNavn() = assertEquals("høst", hashtagFromLink("https://x.org/tags/h%C3%B8st", "#høst"))
    @Test fun tekstUtenHashErIkkeEmneknagg() = assertNull(hashtagFromLink("https://x.org/tags/foo", "se her"))
    @Test fun vanligLenkeMedHashErIkkeEmneknagg() = assertNull(hashtagFromLink("https://x.org/users/bob", "#bob"))
}
