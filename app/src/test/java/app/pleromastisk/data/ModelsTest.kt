package app.pleromastisk.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ModelsTest {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; isLenient = true }

    private fun status(extra: String) = """
        {"id":"1","content":"<p>hei</p>","favourites_count":3,"reblogs_count":2,"replies_count":0,
         "account":{"id":"a","acct":"gaute","display_name":"Gaute"}$extra}
    """.trimIndent()

    @Test fun leserAppNavn() {
        val s = json.decodeFromString<Status>(status(""","application":{"name":"Pleromastisk","website":"https://example.org"}"""))
        assertEquals("Pleromastisk", s.application?.name)
        assertEquals(3, s.favouritesCount)
        assertEquals(2, s.reblogsCount)
    }

    @Test fun manglendeEllerNullApp() {
        assertNull(json.decodeFromString<Status>(status("")).application)
        assertNull(json.decodeFromString<Status>(status(""","application":null""")).application)
    }

    @Test fun appUtenWebsiteOgNullNavn() {
        val s = json.decodeFromString<Status>(status(""","application":{"name":null,"website":null}"""))
        assertEquals("", s.application?.name)
    }
}
