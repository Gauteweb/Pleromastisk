package app.pleromastisk.ui

import app.pleromastisk.data.Mention
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MentionTest {
    private val gaute = Mention("1", "gaute", "gaute", "https://social.gaute.space/users/gaute")
    private val bob = Mention("2", "bob", "bob@mastodon.social", "https://mastodon.social/@bob")
    private val bob2 = Mention("3", "bob", "bob@pixelfed.social", "https://pixelfed.social/bob")
    private val all = listOf(gaute, bob)

    @Test fun treffPaaAdresse() = assertEquals(bob, mentionFor("https://mastodon.social/@bob", "@bob", all))
    @Test fun treffPaaAcct() = assertEquals(bob, mentionFor("https://annen.lenke/x", "@bob@mastodon.social", all))
    @Test fun treffPaaBrukernavn() = assertEquals(gaute, mentionFor("https://annen.lenke/y", "@gaute", all))
    @Test fun tvetydigBrukernavnGirIngenting() = assertNull(mentionFor("https://annen.lenke/z", "@bob", listOf(bob, bob2)))
    @Test fun ukjentGirIngenting() = assertNull(mentionFor("https://x.org/@alice", "@alice", all))
}
