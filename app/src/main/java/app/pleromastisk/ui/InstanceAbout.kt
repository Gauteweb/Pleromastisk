package app.pleromastisk.ui

import android.content.Intent
import android.text.TextUtils
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.pleromastisk.Graph
import app.pleromastisk.R
import app.pleromastisk.userMessage
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * «Om»-siden til instansen brukeren er logget inn på, vist i appen (med vanlige menyer rundt).
 * Pleroma: det samme som Pleroma-FE sin /about, altså panel-teksten og tjenestevilkårene.
 * Ellers (Mastodon m.fl.): det samme som Mastodons /about, hentet fra API-et.
 */
@Composable
fun InstanceAboutScreen(pleroma: Boolean) {
    var attempt by remember { mutableIntStateOf(0) }
    val result by produceState<Result<List<String>>?>(null, attempt) {
        value = null
        value = runCatching { if (pleroma) Graph.api.instanceAboutHtml() else mastodonAbout() }
    }

    val r = result
    Box(Modifier.fillMaxSize().padding(8.dp)) {
        when {
            r == null -> LoadingRow()
            r.isFailure -> ErrorPanel(r.exceptionOrNull()!!.userMessage()) { attempt++ }
            r.getOrThrow().isEmpty() -> Panel {
                Text(stringResource(R.string.instance_about_empty), color = PT.Text, modifier = Modifier.padding(14.dp))
            }
            else -> Panel(Modifier.fillMaxSize()) { AboutWebView(r.getOrThrow()) }
        }
    }
}

@Composable
private fun AboutWebView(sections: List<String>) {
    val base = Graph.store.instance.orEmpty() + "/"
    val page = remember(sections) { aboutPage(sections) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    val scrollToTop = LocalScrollToTop.current
    LaunchedEffect(scrollToTop) { scrollToTop.collect { webView?.pageUp(true) } }
    key(page) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    settings.javaScriptEnabled = false
                    // Lenker åpnes i nettleseren, ikke inne i «Om»-visningen
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                            runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, request.url)) }
                            return true
                        }
                    }
                    loadDataWithBaseURL(base, page, "text/html", "utf-8", null)
                    webView = this
                }
            },
        )
    }
}

/**
 * Seksjonene på Mastodons /about: navn og kort beskrivelse, den lange beskrivelsen, serverreglene
 * og hvem som administrerer instansen.
 */
private suspend fun mastodonAbout(): List<String> = coroutineScope {
    val info = async { Graph.api.instanceInfo() }
    val extended = async { Graph.api.extendedDescription() }
    val rules = async { Graph.api.instanceRules() }
    val i = info.await()
    val long = extended.await().trim()
    buildList {
        val short = i.description.trim()
        val intro = when {
            short.isBlank() || short == long -> ""
            '<' in short -> short
            else -> "<p>${enc(short)}</p>"
        }
        val title = i.title.takeIf { it.isNotBlank() }?.let { "<h1>${enc(it)}</h1>" }.orEmpty()
        if (title.isNotEmpty() || intro.isNotEmpty()) add(title + intro)
        if (long.isNotBlank()) add(long)
        rules.await().filter { it.text.isNotBlank() }.takeIf { it.isNotEmpty() }?.let { list ->
            add(
                "<h2>${enc(Graph.str(R.string.instance_about_rules))}</h2><ol>" +
                    list.joinToString("") { r ->
                        "<li>" + enc(r.text) + (if (r.hint.isNotBlank()) "<br><small>${enc(r.hint)}</small>" else "") + "</li>"
                    } + "</ol>",
            )
        }
        val admin = i.contactAccount?.takeIf { it.acct.isNotBlank() }
            ?.let { "<a href=\"${enc(it.url)}\">@${enc(it.acct)}</a>" }
        val mail = i.email.takeIf { it.isNotBlank() }?.let { "<a href=\"mailto:${enc(it)}\">${enc(it)}</a>" }
        if (admin != null || mail != null) {
            add("<h2>${enc(Graph.str(R.string.instance_about_contact))}</h2><p>" + listOfNotNull(admin, mail).joinToString("<br>") + "</p>")
        }
    }
}

/** Ren tekst trygt inn i HTML, med linjeskift bevart. */
private fun enc(text: String) = TextUtils.htmlEncode(text).replace("\n", "<br>")

private fun css(c: Color) = "#%06X".format(c.toArgb() and 0xFFFFFF)

/** Setter seksjonene sammen til én side med appens farger. */
private fun aboutPage(sections: List<String>): String = """
    <!DOCTYPE html>
    <html><head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <style>
      body { margin: 0; padding: 14px; background: transparent; color: ${css(PT.Text)};
             font-family: sans-serif; font-size: 15px; line-height: 1.45; word-wrap: break-word; }
      a { color: ${css(PT.Link)}; }
      h1, h2, h3 { color: ${css(PT.TopBarText)}; line-height: 1.2; }
      h1 { font-size: 1.35em; } h2 { font-size: 1.2em; } h3 { font-size: 1.05em; }
      img, video { max-width: 100%; height: auto; }
      table { border-collapse: collapse; max-width: 100%; }
      td, th { border: 1px solid ${css(PT.Border)}; padding: 4px; }
      hr, .section { border: 0; border-top: 1px solid ${css(PT.Border)}; margin: 18px 0; }
    </style>
    </head><body>
    ${sections.joinToString("\n<hr class=\"section\">\n")}
    </body></html>
""".trimIndent()
