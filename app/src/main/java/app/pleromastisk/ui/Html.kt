package app.pleromastisk.ui

import android.graphics.Typeface
import android.text.Html
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.text.style.URLSpan
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.em
import app.pleromastisk.data.Emoji
import app.pleromastisk.data.Mention
import coil.compose.AsyncImage

private val EmojiRegex = Regex(":([A-Za-z0-9_\\-]+):")

/** Gjør instansens HTML om til stilet tekst med klikkbare lenker og egendefinerte emojier. */
fun htmlToRich(
    html: String,
    emojis: List<Emoji>,
    linkColor: Color,
    onHashtag: (String) -> Unit = {},
    mentions: List<Mention> = emptyList(),
    onProfile: (id: String?, url: String) -> Unit = { _, _ -> },
): Pair<AnnotatedString, Map<String, InlineTextContent>> {
    val cleaned = html
        .replace(Regex("<span class=\"invisible\">.*?</span>", RegexOption.DOT_MATCHES_ALL), "")
        .replace(Regex("<span class=\"ellipsis\">(.*?)</span>", RegexOption.DOT_MATCHES_ALL), "\$1…")
    val spanned = Html.fromHtml(cleaned, Html.FROM_HTML_MODE_LEGACY)
    val plain = spanned.toString().trimEnd()
    val base = buildAnnotatedString {
        append(plain)
        for (span in spanned.getSpans(0, spanned.length, Any::class.java)) {
            val start = spanned.getSpanStart(span).coerceIn(0, plain.length)
            val end = spanned.getSpanEnd(span).coerceIn(0, plain.length)
            if (end <= start) continue
            when (span) {
                is URLSpan -> {
                    val style = TextLinkStyles(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline))
                    val linkText = plain.substring(start, end)
                    val tag = hashtagFromLink(span.url, linkText)
                    if (tag == null && (linkText.startsWith("@") || mentions.any { it.url == span.url })) {
                        // @omtaler åpner profilen i appen (via egen instans) i stedet for nettsiden
                        val id = mentionFor(span.url, linkText, mentions)?.id
                        addLink(
                            LinkAnnotation.Clickable(
                                tag = "mention:${span.url}",
                                styles = style,
                                linkInteractionListener = { onProfile(id, span.url) },
                            ),
                            start, end,
                        )
                    } else if (tag != null) {
                        // Emneknagger åpnes i appen i stedet for i nettleseren
                        addLink(
                            LinkAnnotation.Clickable(
                                tag = "hashtag:$tag",
                                styles = style,
                                linkInteractionListener = { onHashtag(tag) },
                            ),
                            start, end,
                        )
                    } else {
                        addLink(LinkAnnotation.Url(span.url, style), start, end)
                    }
                }
                is StyleSpan -> when (span.style) {
                    Typeface.BOLD -> addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, end)
                    Typeface.ITALIC -> addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, end)
                    Typeface.BOLD_ITALIC -> addStyle(
                        SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic), start, end,
                    )
                }
                is TypefaceSpan -> if (span.family == "monospace") {
                    addStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = Color(0x33000000)), start, end)
                }
                is StrikethroughSpan -> addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough), start, end)
            }
        }
    }
    return withEmojis(base, emojis)
}

/** Finner omtalen en lenke peker på: først på adresse, deretter på «@navn» / «@navn@domene». */
internal fun mentionFor(url: String, linkText: String, mentions: List<Mention>): Mention? {
    mentions.firstOrNull { it.url == url }?.let { return it }
    val name = linkText.removePrefix("@").trim()
    if (name.isEmpty()) return null
    mentions.firstOrNull { it.acct.equals(name, ignoreCase = true) }?.let { return it }
    return mentions.filter { it.username.equals(name.substringBefore('@'), ignoreCase = true) }.singleOrNull()
}

fun withEmojis(src: AnnotatedString, emojis: List<Emoji>): Pair<AnnotatedString, Map<String, InlineTextContent>> {
    if (emojis.isEmpty()) return src to emptyMap()
    val byCode = emojis.associateBy { it.shortcode }
    val inline = mutableMapOf<String, InlineTextContent>()
    val out = buildAnnotatedString {
        var last = 0
        for (m in EmojiRegex.findAll(src.text)) {
            val emoji = byCode[m.groupValues[1]] ?: continue
            append(src.subSequence(last, m.range.first))
            val id = "emoji_${emoji.shortcode}"
            appendInlineContent(id, ":${emoji.shortcode}:")
            inline.getOrPut(id) {
                InlineTextContent(
                    Placeholder(1.3.em, 1.3.em, PlaceholderVerticalAlign.TextCenter),
                ) {
                    AsyncImage(model = emoji.url, contentDescription = emoji.shortcode, modifier = Modifier.fillMaxSize())
                }
            }
            last = m.range.last + 1
        }
        append(src.subSequence(last, src.length))
    }
    return out to inline
}

/** Lar innlegg åpne en emneknagg-tidslinje når brukeren trykker på #emneknagg. */
val LocalHashtagClick = staticCompositionLocalOf<(String) -> Unit> { {} }

/** Trykk på en @omtale: konto-ID hvis den er kjent, ellers lenken (slås opp via instansen). */
val LocalProfileLinkClick = staticCompositionLocalOf<(id: String?, url: String) -> Unit> { { _, _ -> } }

/** HTML-innhold fra et innlegg eller en profil. */
@Composable
fun RichText(
    html: String,
    emojis: List<Emoji>,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = PT.FgText,
    maxLines: Int = Int.MAX_VALUE,
    mentions: List<Mention> = emptyList(),
) {
    val onHashtag = LocalHashtagClick.current
    val onProfile = LocalProfileLinkClick.current
    val (text, inline) = remember(html, emojis, onHashtag, mentions, onProfile) {
        htmlToRich(html, emojis, PT.Link, onHashtag, mentions, onProfile)
    }
    Text(
        text, modifier, style = style, color = color, maxLines = maxLines,
        overflow = TextOverflow.Ellipsis, inlineContent = inline,
    )
}

/** Vanlig tekst (f.eks. visningsnavn) som kan inneholde :emoji:-koder. */
@Composable
fun EmojiText(
    text: String,
    emojis: List<Emoji>,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = PT.FgText,
    fontWeight: FontWeight? = null,
    maxLines: Int = Int.MAX_VALUE,
) {
    val (rich, inline) = remember(text, emojis) { withEmojis(AnnotatedString(text), emojis) }
    Text(
        rich, modifier, style = style, color = color, fontWeight = fontWeight, maxLines = maxLines,
        overflow = TextOverflow.Ellipsis, inlineContent = inline,
    )
}
