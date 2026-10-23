package app.pleromastisk.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature

const val CHAT_URL = "https://chat.gaute.space/"
private val chatHost = Uri.parse(CHAT_URL).host

/** Samme bakgrunn som i [CHAT_THEME_CSS], så det ikke blinker eller blir lyse kanter rundt siden. */
private const val CHAT_BACKGROUND = 0xFF203237.toInt()

/**
 * Gir Cinny appens farger ved å overstyre fargevariablene i Cinny sitt mørke tema (klassen `_15q3ngn1`).
 * Navnene lages når Cinny bygges, og kan endre seg ved oppdatering av Cinny på serveren –
 * da faller chatten stille tilbake til sine egne farger. Rekkefølgen følger Cinny sine fargegrupper:
 * Background, Surface, SurfaceVariant, Primary, Secondary (5 eller 10 verdier hver).
 * Lenkefargen `--tc-link` har et fast navn og påvirkes ikke av slike oppdateringer.
 * `html ._15q3ngn1` slår Cinny sin egen regel selv om stilarket vårt ligger først i dokumentet.
 */
private val CHAT_THEME_CSS = """
    html ._15q3ngn1 {
      --oq6d070: #203237; --oq6d071: #284047; --oq6d072: #2F4A52; --oq6d073: #3A565E; --oq6d074: #C2D9D4;
      --oq6d075: #132B29; --oq6d076: #1A3633; --oq6d077: #21413D; --oq6d078: #AB9977; --oq6d079: #C2D9D4;
      --oq6d07a: #1A3633; --oq6d07b: #21413D; --oq6d07c: #284C47; --oq6d07d: #7D7057; --oq6d07e: #C2D9D4;
      --oq6d07f: #AB9977; --oq6d07g: #B8A786; --oq6d07h: #C2B292; --oq6d07i: #7D7057; --oq6d07j: #132B29;
      --oq6d07k: #443D30; --oq6d07l: #4D4536; --oq6d07m: #574E3D; --oq6d07n: #7D7057; --oq6d07o: #E6DCC8;
      --oq6d07p: #C2D9D4; --oq6d07q: #B5D4CE; --oq6d07r: #A8C7C1; --oq6d07s: #9BBAB4; --oq6d07t: #132B29;
      --oq6d07u: #2F4A52; --oq6d07v: #365259; --oq6d07w: #3D5A61; --oq6d07x: #4A6970; --oq6d07y: #C2D9D4;
    }
    html .dark-theme { --tc-link: #AB9977; }
""".trimIndent()

/**
 * Legger stilarket inn i siden; ved dokumentstart om mulig, så Cinny sine egne farger aldri vises.
 *
 * Skjuler også Cinny sin grønne «Connecting...»-linje, som blir stående til neste svar fra serveren
 * (opptil 30 s) selv når forbindelsen allerede er tilbake. Linjen finnes ut fra teksten, som er fast i
 * Cinny, og oppbyggingen: tekst i en boks med padding, inne i en kolonne med to barn (boksen og en strek).
 * Den gule «Connection Lost! Reconnecting...»-linjen ved ekte brudd vises fortsatt.
 */
private val CHAT_PAGE_JS = """
    (function () {
      if (document.getElementById('pleromastisk-theme')) return;
      var s = document.createElement('style');
      s.id = 'pleromastisk-theme';
      s.textContent = ${org.json.JSONObject.quote(CHAT_THEME_CSS)};
      (document.head || document.documentElement).appendChild(s);

      function hideConnecting(root) {
        if (root.nodeType !== 1 || (root.textContent || '').indexOf('Connecting...') < 0) return;
        var all = [root].concat(Array.prototype.slice.call(root.querySelectorAll('*')));
        all.forEach(function (el) {
          if (el.childElementCount !== 0 || el.textContent !== 'Connecting...') return;
          var box = el.parentElement, banner = box && box.parentElement;
          if (banner && box.style.padding && banner.childElementCount === 2) banner.style.display = 'none';
        });
      }
      new MutationObserver(function (records) {
        records.forEach(function (r) { r.addedNodes.forEach(hideConnecting); });
      }).observe(document.documentElement, { childList: true, subtree: true });
      if (document.body) hideConnecting(document.body);
    })();
""".trimIndent()

/**
 * Holder chatten i live mens man bytter faner, så Matrix-forbindelsen ikke kobles ned og siden
 * ikke må lastes på nytt. WebView-en lages første gang chatten åpnes og lever like lenge som appvinduet.
 */
class ChatSession {
    internal var webView: WebView? = null
    internal var canGoBack by mutableStateOf(false)

    /** Filvelgeren til chat-siden som vises nå; null når chatten ikke er på skjermen. */
    internal var showFileChooser: ((ValueCallback<Array<Uri>>, WebChromeClient.FileChooserParams) -> Boolean)? = null

    fun destroy() {
        webView?.destroy()
        webView = null
    }
}

@Composable
fun rememberChatSession(): ChatSession {
    val session = remember { ChatSession() }
    DisposableEffect(session) { onDispose { session.destroy() } }
    return session
}

/** Chatten på chat.gaute.space, vist i appen. Lenker til andre nettsteder åpnes i nettleseren. */
@Composable
fun ChatScreen(session: ChatSession) {
    // Filvelgeren for bilder og andre vedlegg: svaret må alltid leveres, også ved avbrudd
    var pendingUpload by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    val pickFiles = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        pendingUpload?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(res.resultCode, res.data))
        pendingUpload = null
    }
    DisposableEffect(session) {
        session.showFileChooser = { callback, params ->
            pendingUpload?.onReceiveValue(null)
            pendingUpload = callback
            val started = runCatching { pickFiles.launch(params.createIntent()) }.isSuccess
            if (!started) {
                callback.onReceiveValue(null)
                pendingUpload = null
            }
            true
        }
        onDispose { session.showFileChooser = null }
    }

    // Tilbake går først bakover i chatten, deretter ut av siden
    BackHandler(enabled = session.canGoBack) { session.webView?.goBack() }

    AndroidView(
        // Siden krymper over tastaturet, så skrivefeltet i Cinny ikke havner bak det
        modifier = Modifier.fillMaxSize().imePadding(),
        factory = { ctx ->
            // Samme WebView som sist, flyttet hit fra der den ble vist forrige gang
            session.webView?.also { (it.parent as? ViewGroup)?.removeView(it) }
                ?: createChatWebView(ctx, session).also { session.webView = it }
        },
        onRelease = { (it.parent as? ViewGroup)?.removeView(it) },
    )
}

@SuppressLint("SetJavaScriptEnabled")
private fun createChatWebView(ctx: Context, session: ChatSession) = WebView(ctx).apply {
    // Med WRAP_CONTENT (standard i AndroidView) blir «height: 100%» på siden 0 px, og Cinny vises hvit
    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    setBackgroundColor(CHAT_BACKGROUND)
    settings.javaScriptEnabled = true
    settings.domStorageEnabled = true
    val themeAtStart = WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)
    if (themeAtStart) {
        WebViewCompat.addDocumentStartJavaScript(this, CHAT_PAGE_JS, setOf("https://$chatHost"))
    }
    webViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val url = request.url
            if (url.host == chatHost) return false
            runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, url)) }
            return true
        }

        override fun onPageFinished(view: WebView, url: String?) {
            if (!themeAtStart) view.evaluateJavascript(CHAT_PAGE_JS, null)
        }

        override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
            session.canGoBack = view.canGoBack()
        }
    }
    webChromeClient = object : WebChromeClient() {
        override fun onShowFileChooser(
            view: WebView,
            callback: ValueCallback<Array<Uri>>,
            params: FileChooserParams,
        ): Boolean = session.showFileChooser?.invoke(callback, params) ?: false
    }
    loadUrl(CHAT_URL)
}
