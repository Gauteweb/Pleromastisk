package app.pleromastisk.ui

private val Opts = setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
private val AnchorRegex = Regex("""<a\b[^>]*?\bhref=["']([^"']+)["'][^>]*>.*?</a>""", Opts)
private val BeforeOk = Regex("""(?:^|<p>|<br\s*/?>)\s*$""", RegexOption.IGNORE_CASE)
private val AfterOk = Regex("""^\s*(?:</p>|<br\s*/?>|$)""", RegexOption.IGNORE_CASE)
private val TrailingBr = Regex("""(?:\s*<br\s*/?>)+\s*(?=</p>|$)""", RegexOption.IGNORE_CASE)
private val EmptyP = Regex("""<p>\s*</p>""", RegexOption.IGNORE_CASE)
private val LeadingBr = Regex("""^(?:\s*<br\s*/?>)+""", RegexOption.IGNORE_CASE)
private val Tags = Regex("<[^>]*>")

private fun normUrl(u: String) = u.replace("&amp;", "&").trim().trimEnd('/')

/**
 * Skjuler lenken som forhåndsvisningskortet viser – men kun når lenken står alene på sin
 * egen linje/avsnitt. Står den midt i en setning, beholdes den slik at teksten henger sammen.
 */
fun stripCardLink(html: String, cardUrl: String): String {
    val target = normUrl(cardUrl)
    for (m in AnchorRegex.findAll(html)) {
        if (normUrl(m.groupValues[1]) != target) continue
        val before = html.substring(0, m.range.first)
        val after = html.substring(m.range.last + 1)
        if (!BeforeOk.containsMatchIn(before) || !AfterOk.containsMatchIn(after)) return html
        return (before + after)
            .replace(TrailingBr, "")
            .replace(EmptyP, "")
            .replace(LeadingBr, "")
            .trim()
    }
    return html
}

/** True hvis HTML-en ikke inneholder synlig tekst. */
fun isBlankHtml(html: String): Boolean = html.replace(Tags, "").isBlank()
