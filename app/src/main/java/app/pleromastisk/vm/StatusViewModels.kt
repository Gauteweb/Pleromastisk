package app.pleromastisk.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pleromastisk.Graph
import app.pleromastisk.data.Account
import app.pleromastisk.data.MastoList
import app.pleromastisk.data.Notification
import app.pleromastisk.data.Poll
import app.pleromastisk.data.Relationship
import app.pleromastisk.data.Status
import app.pleromastisk.data.TimelineKind
import app.pleromastisk.userMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Felles håndtering av favoritt/del/bokmerke/avstemning/sletting for alle lister med innlegg. */
abstract class StatusListViewModel : ViewModel() {
    protected val _items = MutableStateFlow<List<Status>>(emptyList())
    val items: StateFlow<List<Status>> = _items.asStateFlow()

    protected fun launchAction(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Graph.toast(e.userMessage())
            }
        }
    }

    /** [updated] kan være en «wrapper» rundt et delt innlegg – da er det `reblog` som er originalen. */
    private fun replaceStatus(updated: Status) {
        val orig = updated.reblog ?: updated
        _items.update { list ->
            list.map { s ->
                when {
                    s.reblog?.id == orig.id -> s.copy(reblog = orig)
                    s.id == orig.id -> orig
                    else -> s
                }
            }
        }
    }

    fun toggleFavourite(s: Status) = launchAction { replaceStatus(Graph.api.favourite(s.id, !s.favourited)) }
    fun toggleBoost(s: Status) = launchAction { replaceStatus(Graph.api.reblog(s.id, !s.reblogged)) }
    fun toggleBookmark(s: Status) = launchAction { replaceStatus(Graph.api.bookmark(s.id, !s.bookmarked)) }

    fun vote(owner: Status, poll: Poll, choices: List<Int>) = launchAction {
        val updated = Graph.api.vote(poll.id, choices)
        _items.update { list ->
            list.map { x ->
                val inner = x.reblog ?: x
                when {
                    inner.id != owner.id -> x
                    x.reblog != null -> x.copy(reblog = inner.copy(poll = updated))
                    else -> inner.copy(poll = updated)
                }
            }
        }
    }

    fun delete(s: Status) = launchAction {
        Graph.api.deleteStatus(s.id)
        _items.update { list -> list.filterNot { it.id == s.id || it.reblog?.id == s.id } }
    }
}

data class Paging(
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val endReached: Boolean = false,
    val error: String? = null,
)

/** Liste med «hent flere»-paginering (max_id) og pull-to-refresh. */
abstract class PagedStatusViewModel : StatusListViewModel() {
    private val _paging = MutableStateFlow(Paging())
    val paging: StateFlow<Paging> = _paging.asStateFlow()
    private var refreshJob: Job? = null

    protected abstract suspend fun fetch(maxId: String?): List<Status>

    init {
        viewModelScope.launch { Graph.posted.collect { if (_items.value.isNotEmpty()) refresh() } }
    }

    fun ensureLoaded() {
        if (_items.value.isEmpty() && !_paging.value.refreshing) refresh()
    }

    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _paging.update { it.copy(refreshing = true, error = null) }
            try {
                val page = fetch(null)
                _items.value = page
                _paging.update { it.copy(refreshing = false, loading = false, endReached = page.isEmpty()) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _paging.update { it.copy(refreshing = false, error = e.userMessage()) }
            }
        }
    }

    fun loadMore() {
        val p = _paging.value
        val last = _items.value.lastOrNull() ?: return
        if (p.loading || p.refreshing || p.endReached) return
        viewModelScope.launch {
            _paging.update { it.copy(loading = true) }
            try {
                val page = fetch(last.id)
                val known = _items.value.map { it.id }.toSet()
                _items.update { it + page.filter { s -> s.id !in known } }
                _paging.update { it.copy(loading = false, endReached = page.isEmpty()) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _paging.update { it.copy(loading = false, error = e.userMessage()) }
            }
        }
    }
}

class TimelineViewModel(private val kind: TimelineKind) : PagedStatusViewModel() {
    override suspend fun fetch(maxId: String?) = Graph.api.timeline(kind, maxId)
}

class ListTimelineViewModel(private val listId: String) : PagedStatusViewModel() {
    override suspend fun fetch(maxId: String?) = Graph.api.listTimeline(listId, maxId)
}

/** Brukerens egne lister (vises som nedtrekksmeny i toppen av «Hjem»). */
class ListsViewModel : ViewModel() {
    private val _lists = MutableStateFlow<List<MastoList>>(emptyList())
    val lists: StateFlow<List<MastoList>> = _lists.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            try {
                _lists.value = Graph.api.lists().sortedBy { it.title.lowercase() }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Lister er en bonus – feil her skal ikke forstyrre resten av appen.
            }
        }
    }
}

class AccountViewModel(private val accountId: String) : PagedStatusViewModel() {
    private val _account = MutableStateFlow<Account?>(null)
    val account: StateFlow<Account?> = _account.asStateFlow()
    private val _relationship = MutableStateFlow<Relationship?>(null)
    val relationship: StateFlow<Relationship?> = _relationship.asStateFlow()

    override suspend fun fetch(maxId: String?) = Graph.api.accountStatuses(accountId, maxId)

    fun loadHeader(isMe: Boolean) = launchAction {
        _account.value = Graph.api.account(accountId)
        if (!isMe) _relationship.value = Graph.api.relationship(accountId)
    }

    fun toggleFollow() = launchAction {
        val following = _relationship.value?.let { it.following || it.requested } ?: false
        _relationship.value = Graph.api.follow(accountId, !following)
        _account.value = Graph.api.account(accountId)
    }
}

class ThreadViewModel(private val id: String) : StatusListViewModel() {
    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    private var started = false

    init {
        viewModelScope.launch { Graph.posted.collect { if (started) reload() } }
    }

    fun load() {
        if (started) return
        started = true
        reload()
    }

    fun reload() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                coroutineScope {
                    val st = async { Graph.api.status(id) }
                    val ctx = async { Graph.api.statusContext(id) }
                    val context = ctx.await()
                    _items.value = context.ancestors + st.await() + context.descendants
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                started = false
                _error.value = e.userMessage()
            } finally {
                _loading.value = false
            }
        }
    }
}

class NotificationsViewModel : ViewModel() {
    private val _items = MutableStateFlow<List<Notification>>(emptyList())
    val items: StateFlow<List<Notification>> = _items.asStateFlow()
    private val _paging = MutableStateFlow(Paging())
    val paging: StateFlow<Paging> = _paging.asStateFlow()
    private var refreshJob: Job? = null

    fun ensureLoaded() {
        if (_items.value.isEmpty() && !_paging.value.refreshing) refresh()
    }

    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _paging.update { it.copy(refreshing = true, error = null) }
            try {
                val page = Graph.api.notifications(null)
                _items.value = page
                _paging.update { it.copy(refreshing = false, loading = false, endReached = page.isEmpty()) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _paging.update { it.copy(refreshing = false, error = e.userMessage()) }
            }
        }
    }

    fun loadMore() {
        val p = _paging.value
        val last = _items.value.lastOrNull() ?: return
        if (p.loading || p.refreshing || p.endReached) return
        viewModelScope.launch {
            _paging.update { it.copy(loading = true) }
            try {
                val page = Graph.api.notifications(last.id)
                val known = _items.value.map { it.id }.toSet()
                _items.update { it + page.filter { n -> n.id !in known } }
                _paging.update { it.copy(loading = false, endReached = page.isEmpty()) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _paging.update { it.copy(loading = false, error = e.userMessage()) }
            }
        }
    }
}
