package app.pleromastisk.vm

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pleromastisk.Graph
import app.pleromastisk.R
import app.pleromastisk.data.Account
import app.pleromastisk.data.ApiException
import app.pleromastisk.data.SessionStore
import app.pleromastisk.userMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthState(
    val loggedIn: Boolean,
    val me: Account? = null,
    val busy: Boolean = false,
    val error: String? = null,
    val maxChars: Int = SessionStore.DEFAULT_MAX_CHARS,
    val postFormats: List<String> = emptyList(),
    val altLimit: Int = SessionStore.DEFAULT_ALT_LIMIT,
    val canEdit: Boolean = false,
)

class AuthViewModel : ViewModel() {
    private val store get() = Graph.store
    private val api get() = Graph.api

    private val _state = MutableStateFlow(
        AuthState(
            loggedIn = Graph.store.token != null,
            maxChars = Graph.store.maxChars,
            postFormats = Graph.store.postFormats,
            altLimit = Graph.store.altLimit,
            canEdit = "editing" in Graph.store.features,
        )
    )
    val state: StateFlow<AuthState> = _state.asStateFlow()

    private val handledCodes = mutableSetOf<String>()

    init {
        if (store.token != null) {
            viewModelScope.launch {
                try {
                    loadSession()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: ApiException) {
                    if (e.code == 401) logout()
                } catch (_: Exception) {
                    // Offline ved oppstart: bruk det som er lagret.
                }
            }
        }
    }

    /** Registrerer appen hos instansen og ber [openUrl] åpne innloggingssiden. */
    fun login(input: String, openUrl: (String) -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null) }
            try {
                val base = try {
                    api.normalizeInstance(input)
                } catch (_: IllegalArgumentException) {
                    _state.update { it.copy(busy = false, error = Graph.str(R.string.login_invalid_instance)) }
                    return@launch
                }
                val reg = api.registerApp(base)
                store.instance = base
                store.clientId = reg.clientId
                store.clientSecret = reg.clientSecret
                store.token = null
                openUrl(api.authorizeUrl(base, reg.clientId))
                _state.update { it.copy(busy = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(busy = false, error = e.userMessage()) }
            }
        }
    }

    /** Kalles når nettleseren sender oss tilbake til pleromastisk://oauth?code=… */
    fun handleRedirect(uri: Uri) {
        if (uri.scheme != "pleromastisk") return
        val code = uri.getQueryParameter("code")
        if (code == null) {
            _state.update { it.copy(busy = false, error = Graph.str(R.string.login_denied)) }
            return
        }
        if (!handledCodes.add(code)) return
        val base = store.instance
        val id = store.clientId
        val secret = store.clientSecret
        if (base == null || id == null || secret == null) return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null) }
            try {
                store.token = api.exchangeToken(base, id, secret, code)
                loadSession()
                _state.update { it.copy(loggedIn = true, busy = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                store.token = null
                _state.update { it.copy(busy = false, loggedIn = false, error = e.userMessage()) }
            }
        }
    }

    fun logout() {
        store.clear()
        _state.value = AuthState(loggedIn = false)
    }

    private suspend fun loadSession() {
        val me = api.verifyCredentials()
        val info = runCatching { api.instanceInfo() }.getOrNull()
        // Tegngrense: konfigurasjon (Mastodon/Pleroma) → max_toot_chars (Pleroma) → 5000.
        val reported = info?.configuration?.statuses?.maxCharacters ?: info?.maxTootChars
        val max = reported?.takeIf { it > 0 } ?: SessionStore.DEFAULT_MAX_CHARS
        val formats = info?.pleroma?.metadata?.postFormats ?: emptyList()
        val features = info?.pleroma?.metadata?.features ?: emptyList()
        val altLimit = info?.descriptionLimit?.takeIf { it > 0 } ?: SessionStore.DEFAULT_ALT_LIMIT
        store.maxChars = max
        store.postFormats = formats
        store.features = features
        store.altLimit = altLimit
        _state.update {
            it.copy(
                me = me, maxChars = max, postFormats = formats,
                altLimit = altLimit, canEdit = "editing" in features,
            )
        }
    }
}
