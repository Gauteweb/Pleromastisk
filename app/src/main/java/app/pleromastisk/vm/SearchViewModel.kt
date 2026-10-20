package app.pleromastisk.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pleromastisk.Graph
import app.pleromastisk.data.Account
import app.pleromastisk.userMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchState(
    val query: String = "",
    val loading: Boolean = false,
    val accounts: List<Account> = emptyList(),
    val hashtags: List<String> = emptyList(),
    val error: String? = null,
    val searched: Boolean = false,
)

private val ValidTag = Regex("^[\\p{L}\\p{N}_]+$")

/**
 * Når brukeren skriver «#navn», skal «#navn» alltid være første treff – også hvis tjeneren ikke
 * kjenner emneknaggen ennå (da vises ingen innlegg, men den kan fortsatt åpnes).
 */
fun withTypedTag(typed: String, found: List<String>): List<String> {
    val t = typed.trim()
    if (!ValidTag.matches(t)) return found
    val rest = found.filterNot { it.equals(t, ignoreCase = true) }
    val exact = found.firstOrNull { it.equals(t, ignoreCase = true) } ?: t
    return listOf(exact) + rest
}

/**
 * Søk etter brukere og emneknagger.
 *  - «@navn» søker bare etter brukere
 *  - «#navn» søker bare etter emneknagger
 *  - vanlig tekst søker etter begge deler
 */
class SearchViewModel : ViewModel() {
    private val _state = MutableStateFlow(SearchState())
    val state: StateFlow<SearchState> = _state.asStateFlow()
    private var job: Job? = null

    fun onQueryChange(q: String) {
        _state.update { it.copy(query = q) }
        job?.cancel()
        if (q.isBlank()) {
            _state.value = SearchState()
            return
        }
        job = viewModelScope.launch {
            delay(450) // vent litt mens brukeren skriver
            run(q)
        }
    }

    fun submit() {
        val q = _state.value.query
        if (q.isBlank()) return
        job?.cancel()
        job = viewModelScope.launch { run(q) }
    }

    fun clear() {
        job?.cancel()
        _state.value = SearchState()
    }

    private suspend fun run(raw: String) {
        val q = raw.trim()
        val wantUsers = !q.startsWith("#")
        val wantTags = !q.startsWith("@")
        val term = q.removePrefix("@").removePrefix("#").trim()
        if (term.isBlank()) {
            _state.update { it.copy(loading = false, accounts = emptyList(), hashtags = emptyList(), searched = false) }
            return
        }
        _state.update { it.copy(loading = true, error = null) }
        try {
            coroutineScope {
                val users = if (wantUsers) async { Graph.api.search(term, "accounts", resolve = '@' in term) } else null
                val tags = if (wantTags) async { Graph.api.search(term, "hashtags", resolve = false) } else null
                val accounts = users?.await()?.accounts.orEmpty()
                var tagNames = tags?.await()?.tagNames.orEmpty()
                if (q.startsWith("#")) tagNames = withTypedTag(term, tagNames)
                _state.update {
                    it.copy(loading = false, accounts = accounts, hashtags = tagNames, searched = true)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _state.update { it.copy(loading = false, error = e.userMessage()) }
        }
    }
}
