package app.pleromastisk.ui

import java.net.URLDecoder

private val TagPath = Regex("""/(?:tags?|hashtag)/([^/?#]+)""", RegexOption.IGNORE_CASE)

/**
 * Hvis lenken er en emneknagg-lenke (teksten starter med «#» og adressen har /tag/ eller /tags/),
 * returneres navnet på emneknaggen – ellers null. Fungerer for Pleroma, Mastodon, Pixelfed m.fl.
 */
fun hashtagFromLink(url: String, label: String): String? {
    if (!label.trimStart().startsWith("#")) return null
    val match = TagPath.findAll(url).lastOrNull() ?: return null
    return runCatching { URLDecoder.decode(match.groupValues[1], "UTF-8") }
        .getOrNull()
        ?.takeIf { it.isNotBlank() }
}
