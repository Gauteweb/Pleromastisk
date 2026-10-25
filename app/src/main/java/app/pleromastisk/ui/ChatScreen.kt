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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.ScriptHandler
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature

const val CHAT_URL = "https://chat.gaute.space/"
private val chatHost = Uri.parse(CHAT_URL).host

/**
 * Gir Cinny fargene i appens tema ved å overstyre Cinny sine fargevariabler. Variabelnavnene lages når
 * Cinny bygges, og kan endre seg ved oppdatering av Cinny på serveren – da faller chatten stille
 * tilbake til sine egne farger. Rekkefølgen følger Cinny sine fargegrupper: Background, Surface,
 * SurfaceVariant, Primary og Secondary (5 eller 10 verdier hver). Klassene er Cinny sine temaer
 * (lyst, mørkt, «butter» og grunntemaet), så fargene gjelder uansett hvilket tema som er valgt i Cinny.
 * `html .klasse` slår Cinny sine egne regler selv om stilarket vårt ligger først i dokumentet.
 * Lenkefargen `--tc-link` har et fast navn og påvirkes ikke av slike oppdateringer.
 */
private fun chatThemeCss(c: PtColors): String {
    fun hex(color: Color) = String.format("#%06X", color.toArgb() and 0xFFFFFF)
    fun mix(a: Color, b: Color, f: Float) = hex(lerp(a, b, f))
    val vars = listOf(
        // Background
        hex(c.bg), mix(c.bg, c.text, 0.06f), mix(c.bg, c.text, 0.12f), mix(c.bg, c.text, 0.2f), hex(c.text),
        // Surface
        hex(c.fg), mix(c.fg, c.text, 0.06f), mix(c.fg, c.text, 0.12f), hex(c.border), hex(c.text),
        // SurfaceVariant
        mix(c.fg, c.text, 0.06f), mix(c.fg, c.text, 0.12f), mix(c.fg, c.text, 0.18f), hex(c.pressedBorder), hex(c.text),
        // Primary
        hex(c.link), mix(c.link, c.text, 0.15f), mix(c.link, c.text, 0.25f), hex(c.pressedBorder), hex(c.fg),
        hex(c.pressedBg), mix(c.pressedBg, c.link, 0.1f), mix(c.pressedBg, c.link, 0.2f), hex(c.pressedBorder), hex(c.text),
        // Secondary
        hex(c.text), hex(c.fgText), mix(c.text, c.bg, 0.15f), mix(c.text, c.bg, 0.25f), hex(c.fg),
        mix(c.bg, c.text, 0.12f), mix(c.bg, c.text, 0.16f), mix(c.bg, c.text, 0.2f), mix(c.bg, c.text, 0.3f), hex(c.text),
    )
    val names = "0123456789abcdefghijklmnopqrstuvwxy".map { "--oq6d07$it" }
    val declarations = names.zip(vars).joinToString(" ") { (name, value) -> "$name: $value;" }
    return """
        html ._15q3ngn0, html ._15q3ngn1, html ._15q3ngn2, html .oq6d071w { $declarations }
        html body { --tc-link: ${hex(c.link)} !important; }
    """.trimIndent()
}

/**
 * Legger stilarket inn i siden, eller bytter det ut når temaet endres. Kjøres ved dokumentstart om
 * mulig, så Cinny sine egne farger aldri vises.
 *
 * Skjuler også Cinny sin grønne «Connecting...»-linje, som blir stående til neste svar fra serveren
 * (opptil 30 s) selv når forbindelsen allerede er tilbake. Linjen finnes ut fra teksten, som er fast i
 * Cinny, og oppbyggingen: tekst i en boks med padding, inne i en kolonne med to barn (boksen og en strek).
 * Den gule «Connection Lost! Reconnecting...»-linjen ved ekte brudd vises fortsatt.
 */
private fun chatPageJs(css: String) = """
    (function () {
      var s = document.getElementById('pleromastisk-theme');
      if (!s) {
        s = document.createElement('style');
        s.id = 'pleromastisk-theme';
        (document.head || document.documentElement).appendChild(s);
      }
      s.textContent = ${org.json.JSONObject.quote(css)};

      if (window.pleromastiskObserver) return;
      function hideConnecting(root) {
        if (root.nodeType !== 1 || (root.textContent || '').indexOf('Connecting...') < 0) return;
        var all = [root].concat(Array.prototype.slice.call(root.querySelectorAll('*')));
        all.forEach(function (el) {
          if (el.childElementCount !== 0 || el.textContent !== 'Connecting...') return;
          var box = el.parentElement, banner = box && box.parentElement;
          if (banner && box.style.padding && banner.childElementCount === 2) banner.style.display = 'none';
        });
      }
      window.pleromastiskObserver = new MutationObserver(function (records) {
        records.forEach(function (r) { r.addedNodes.forEach(hideConnecting); });
      });
      window.pleromastiskObserver.observe(document.documentElement, { childList: true, subtree: true });
      if (document.body) hideConnecting(document.body);
    })();
""".trimIndent()

private val themeAtStart get() = WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)

/**
 * Holder chatten i live mens man bytter faner, så Matrix-forbindelsen ikke kobles ned og siden
 * ikke må lastes på nytt. WebView-en lages første gang chatten åpnes og lever like lenge som appvinduet.
 */
class ChatSession {
    internal var webView: WebView? = null
    internal var canGoBack by mutableStateOf(false)

    /** Skriptet som legger inn temafargene; byttes ut når temaet endres. */
    internal var pageJs = chatPageJs(chatThemeCss(PT.theme.colors))
    private var startScript: ScriptHandler? = null
    private var appliedTheme: PtColors? = null

    /** Gir chatten fargene i [c], både på siden som vises nå og ved neste innlasting. */
    internal fun applyTheme(c: PtColors) {
        val wv = webView ?: return
        if (c == appliedTheme) return
        appliedTheme = c
        pageJs = chatPageJs(chatThemeCss(c))
        wv.setBackgroundColor(c.bg.toArgb())
        if (themeAtStart) {
            startScript?.remove()
            startScript = WebViewCompat.addDocumentStartJavaScript(wv, pageJs, setOf("https://$chatHost"))
        }
        wv.evaluateJavascript(pageJs, null)
    }

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

    // Temaet kan ha blitt byttet mens chatten lå i bakgrunnen
    val colors = PT.theme.colors
    LaunchedEffect(colors) { session.applyTheme(colors) }

    // Tilbake går først bakover i chatten, deretter ut av siden
    BackHandler(enabled = session.canGoBack) { session.webView?.goBack() }

    AndroidView(
        // Siden krymper over tastaturet, så skrivefeltet i Cinny ikke havner bak det
        modifier = Modifier.fillMaxSize().imePadding(),
        factory = { ctx ->
            // Samme WebView som sist, flyttet hit fra der den ble vist forrige gang
            session.webView?.also { (it.parent as? ViewGroup)?.removeView(it) }
                ?: createChatWebView(ctx, session).also {
                    session.webView = it
                    // Temaet legges inn før siden lastes, så Cinny sine egne farger aldri vises
                    session.applyTheme(PT.theme.colors)
                    it.loadUrl(CHAT_URL)
                }
        },
        onRelease = { (it.parent as? ViewGroup)?.removeView(it) },
    )
}

@SuppressLint("SetJavaScriptEnabled")
private fun createChatWebView(ctx: Context, session: ChatSession) = WebView(ctx).apply {
    // Med WRAP_CONTENT (standard i AndroidView) blir «height: 100%» på siden 0 px, og Cinny vises hvit
    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    setBackgroundColor(PT.theme.colors.bg.toArgb())
    settings.javaScriptEnabled = true
    settings.domStorageEnabled = true
    webViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val url = request.url
            if (url.host == chatHost) return false
            runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, url)) }
            return true
        }

        override fun onPageFinished(view: WebView, url: String?) {
            if (!themeAtStart) view.evaluateJavascript(session.pageJs, null)
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
}
