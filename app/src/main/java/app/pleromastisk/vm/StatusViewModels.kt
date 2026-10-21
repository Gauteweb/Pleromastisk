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

class TagViewModel(private val tag: String) : PagedStatusViewModel() {
    override suspend fun fetch(maxId: String?) = Graph.api.tagTimeline(tag, maxId)
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

    /** Mine lister og hvilke av dem kontoen ligger i (null til de er hentet). */
    private val _lists = MutableStateFlow<ListMembership?>(null)
    val lists: StateFlow<ListMembership?> = _lists.asStateFlow()

    fun loadLists() = launchAction {
        _lists.value = null
        coroutineScope {
            val all = async { Graph.api.lists() }
            val mine = async { Graph.api.accountLists(accountId) }
            _lists.value = ListMembership(all.await().sortedBy { it.title.lowercase() }, mine.await().map { it.id }.toSet())
        }
    }

    fun setInList(listId: String, add: Boolean) = launchAction {
        Graph.api.setInList(listId, accountId, add)
        _lists.update { m -> m?.copy(memberOf = if (add) m.memberOf + listId else m.memberOf - listId) }
    }

    fun createListAndAdd(title: String) = launchAction {
        val list = Graph.api.createList(title.trim())
        Graph.api.setInList(list.id, accountId, add = true)
        _lists.update { m ->
            m?.copy(lists = (m.lists + list).sortedBy { it.title.lowercase() }, memberOf = m.memberOf + list.id)
        }
    }
}

data class ListMembership(val lists: List<MastoList>, val memberOf: Set<String>)

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

enum class NotificationFilter { ALL, REPLIES }

class NotificationsViewModel : ViewModel() {
    private val _items = MutableStateFlow<List<Notification>>(emptyList())
    val items: StateFlow<List<Notification>> = _items.asStateFlow()
    private val _paging = MutableStateFlow(Paging())
    val paging: StateFlow<Paging> = _paging.asStateFlow()
    private val _filter = MutableStateFlow(NotificationFilter.ALL)
    val filter: StateFlow<NotificationFilter> = _filter.asStateFlow()

    /**
     * Varsler som er nyere enn det øverste brukeren hadde sett sist hen forlot varselsiden.
     * Telles ved hver lasting/oppdatering (også første gang), så varsler som kom mens man var
     * et annet sted – eller mens appen var lukket – kommer med.
     */
    private val _newCount = MutableStateFlow(0)
    val newCount: StateFlow<Int> = _newCount.asStateFlow()

    /** Kalles når brukeren går vekk fra varselsiden: alt som vises nå regnes som sett. */
    fun markSeen() {
        if (_filter.value == NotificationFilter.ALL) {
            _items.value.firstOrNull()?.let { Graph.store.lastSeenNotification = it.id }
        }
        _newCount.value = 0
    }

    private fun countNew(list: List<Notification>): Int {
        if (_filter.value != NotificationFilter.ALL) return 0
        val seen = Graph.store.lastSeenNotification ?: return 0 // aldri vært her før
        val idx = list.indexOfFirst { it.id == seen }
        return if (idx >= 0) idx else list.size // ikke funnet: alle på første side er nye
    }

    private var refreshJob: Job? = null
    private var moreJob: Job? = null

    /** Siste varsel serveren sendte (før filtrering i appen) – brukes til «hent flere». */
    private var cursor: String? = null

    /** Sikkerhetsnett: viser bare svar/omtaler selv om en tjener skulle ignorere filteret. */
    private fun matches(n: Notification, f: NotificationFilter) =
        f == NotificationFilter.ALL || n.type == "mention"

    fun setFilter(f: NotificationFilter) {
        if (f == _filter.value) return
        _filter.value = f
        _items.value = emptyList()
        cursor = null
        _paging.value = Paging()
        refresh()
    }

    fun ensureLoaded() {
        if (_items.value.isEmpty() && !_paging.value.refreshing) refresh()
    }

    fun refresh() {
        refreshJob?.cancel()
        moreJob?.cancel()
        val f = _filter.value
        refreshJob = viewModelScope.launch {
            _paging.update { it.copy(refreshing = true, loading = false, error = null) }
            try {
                val raw = Graph.api.notifications(null, f == NotificationFilter.REPLIES)
                cursor = raw.lastOrNull()?.id
                val fresh = raw.filter { matches(it, f) }
                _items.value = fresh
                _newCount.value = countNew(fresh)
                _paging.update { it.copy(refreshing = false, endReached = raw.isEmpty()) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _paging.update { it.copy(refreshing = false, error = e.userMessage()) }
            }
        }
    }

    fun loadMore() {
        val p = _paging.value
        val from = cursor ?: return
        if (p.loading || p.refreshing || p.endReached) return
        val f = _filter.value
        moreJob = viewModelScope.launch {
            _paging.update { it.copy(loading = true) }
            try {
                val raw = Graph.api.notifications(from, f == NotificationFilter.REPLIES)
                cursor = raw.lastOrNull()?.id ?: from
                val known = _items.value.map { it.id }.toSet()
                _items.update { it + raw.filter { n -> matches(n, f) && n.id !in known } }
                _paging.update { it.copy(loading = false, endReached = raw.isEmpty()) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _paging.update { it.copy(loading = false, error = e.userMessage()) }
            }
        }
    }
}
