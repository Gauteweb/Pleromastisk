package app.pleromastisk.vm

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pleromastisk.Graph
import app.pleromastisk.R
import app.pleromastisk.data.Account
import app.pleromastisk.data.ApiException
import app.pleromastisk.data.SavedAccount
import app.pleromastisk.data.SessionStore
import app.pleromastisk.userMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthState(
    val loggedIn: Boolean,
    /** Den aktive brukeren; skjermene lages på nytt når den byttes. */
    val activeKey: String? = null,
    val accounts: List<SavedAccount> = emptyList(),
    /** Innloggingsskjermen vises for å legge til en bruker, selv om man er innlogget. */
    val addingAccount: Boolean = false,
    val me: Account? = null,
    val busy: Boolean = false,
    val error: String? = null,
    val maxChars: Int = SessionStore.DEFAULT_MAX_CHARS,
    val postFormats: List<String> = emptyList(),
    val altLimit: Int = SessionStore.DEFAULT_ALT_LIMIT,
    val canEdit: Boolean = false,
    /** Instansen er Pleroma (har «Om»-side med panel og tjenestevilkår). */
    val isPleroma: Boolean = false,
)

/**
 * Kan innlegg redigeres? Pleroma sier fra med «editing» i funksjonslisten sin. Mastodon (fra 3.5) og
 * GoToSocial (fra 0.18) har ingen slik liste, men støtter redigering, så der regnes det som støttet.
 */
private fun editAllowed(features: List<String>, isPleroma: Boolean?) = "editing" in features || isPleroma == false

class AuthViewModel : ViewModel() {
    private val store get() = Graph.store
    private val api get() = Graph.api

    private val _state = MutableStateFlow(stateFromStore())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    private val handledCodes = mutableSetOf<String>()
    private var sessionJob: Job? = null

    init {
        if (store.token != null) startSession()
    }

    /** Tilstand for den aktive brukeren slik den er lagret (før noe er hentet fra instansen). */
    private fun stateFromStore(): AuthState {
        val active = store.active
        return AuthState(
            loggedIn = active != null,
            activeKey = active?.key,
            accounts = store.accounts,
            maxChars = store.maxChars,
            postFormats = store.postFormats,
            altLimit = store.altLimit,
            canEdit = editAllowed(store.features, active?.isPleroma),
            isPleroma = active?.isPleroma == true,
        )
    }

    /** Henter brukeren og instansinfo for den aktive brukeren i bakgrunnen. */
    private fun startSession() {
        val key = store.activeKey ?: return
        sessionJob?.cancel()
        sessionJob = viewModelScope.launch {
            try {
                loadSession(key)
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiException) {
                if (e.code == 401 && store.activeKey == key) logout()
            } catch (_: Exception) {
                // Offline ved oppstart: bruk det som er lagret.
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
                store.setPendingLogin(base, reg.clientId, reg.clientSecret)
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
        val (base, id, secret) = store.pendingLogin() ?: return
        val previous = store.activeKey
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null) }
            var added: SavedAccount? = null
            try {
                val token = api.exchangeToken(base, id, secret, code)
                sessionJob?.cancel()
                added = store.addAccount(base, id, secret, token)
                store.clearPendingLogin()
                loadSession(added.key)
                // Samme bruker som før på samme instans: behold bare den nye innloggingen
                store.removeDuplicatesOf(added.key)
                _state.update {
                    it.copy(
                        loggedIn = true, activeKey = added.key, accounts = store.accounts,
                        addingAccount = false, busy = false,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                added?.let { store.removeAccount(it.key) }
                store.activeKey = previous
                _state.update { it.copy(busy = false, accounts = store.accounts, error = e.userMessage()) }
                if (previous != null) startSession()
            }
        }
    }

    /** Viser innloggingsskjermen for å legge til en bruker til. */
    fun startAddingAccount() {
        _state.update { it.copy(addingAccount = true, error = null) }
    }

    fun cancelAddingAccount() {
        store.clearPendingLogin()
        _state.update { it.copy(addingAccount = false, busy = false, error = null) }
    }

    fun switchTo(key: String) {
        if (key == store.activeKey || store.accounts.none { it.key == key }) return
        store.activeKey = key
        _state.value = stateFromStore()
        startSession()
    }

    /** Logger ut den aktive brukeren. Finnes det flere, byttes det til en av dem. */
    fun logout() {
        sessionJob?.cancel()
        store.activeKey?.let { store.removeAccount(it) }
        _state.value = stateFromStore()
        if (store.token != null) startSession()
    }

    private suspend fun loadSession(key: String) {
        val me = api.verifyCredentials()
        val info = runCatching { api.instanceInfo() }.getOrNull()
        // Tegngrense: konfigurasjon (Mastodon/Pleroma) → max_toot_chars (Pleroma) → 5000.
        val reported = info?.configuration?.statuses?.maxCharacters ?: info?.maxTootChars
        val max = reported?.takeIf { it > 0 } ?: SessionStore.DEFAULT_MAX_CHARS
        // Tekstformater: Pleroma oppgir dem i sine metadata, GoToSocial og glitch-soc i konfigurasjonen
        val formats = info?.pleroma?.metadata?.postFormats
            ?: info?.configuration?.statuses?.supportedMimeTypes
            ?: emptyList()
        val features = info?.pleroma?.metadata?.features ?: emptyList()
        val altLimit = info?.descriptionLimit?.takeIf { it > 0 } ?: SessionStore.DEFAULT_ALT_LIMIT
        store.updateAccount(key) {
            it.copy(
                userId = me.id, acct = me.acct, displayName = me.name, avatar = me.avatar,
                maxChars = max, postFormats = formats, altLimit = altLimit, features = features,
                // Uten svar fra instansen beholdes det som ble funnet sist
                isPleroma = info?.isPleroma ?: it.isPleroma,
            )
        }
        val savedPleroma = store.accounts.firstOrNull { it.key == key }?.isPleroma
        val isPleroma = savedPleroma == true
        // Brukeren kan ha byttet til en annen konto mens dette ble hentet
        if (store.activeKey != key) return
        _state.update {
            it.copy(
                me = me, accounts = store.accounts, maxChars = max, postFormats = formats,
                altLimit = altLimit, canEdit = editAllowed(features, savedPleroma), isPleroma = isPleroma,
            )
        }
    }
}
