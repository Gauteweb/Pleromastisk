package app.pleromastisk.ui

import android.content.Intent
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
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

/**
 * «Om»-siden til instansen brukeren er logget inn på, vist i appen (med vanlige menyer rundt).
 * Viser det samme som Pleroma-FE sin /about: panel-teksten og tjenestevilkårene.
 */
@Composable
fun InstanceAboutScreen() {
    var attempt by remember { mutableIntStateOf(0) }
    val result by produceState<Result<List<String>>?>(null, attempt) {
        value = null
        value = runCatching { Graph.api.instanceAboutHtml() }
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
                }
            },
        )
    }
}

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
