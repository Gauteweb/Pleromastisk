package app.pleromastisk.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.pleromastisk.R
import app.pleromastisk.data.Account
import app.pleromastisk.data.Notification
import app.pleromastisk.data.Status
import app.pleromastisk.relativeTime
import app.pleromastisk.vm.AccountViewModel
import app.pleromastisk.vm.AuthViewModel
import app.pleromastisk.vm.NotificationFilter
import app.pleromastisk.vm.NotificationsViewModel
import app.pleromastisk.vm.PagedStatusViewModel
import app.pleromastisk.vm.ThreadViewModel
import coil.compose.AsyncImage

// ---------------------------------------------------------------- Innlogging

private fun openCustomTab(context: Context, url: String) {
    val tabs = CustomTabsIntent.Builder()
        .setDefaultColorSchemeParams(
            CustomTabColorSchemeParams.Builder().setToolbarColor(PT.Fg.toArgb()).build(),
        )
        .build()
    tabs.launchUrl(context, Uri.parse(url))
}

@Composable
fun LoginScreen(auth: AuthViewModel, onCancel: (() -> Unit)? = null) {
    val state by auth.state.collectAsStateWithLifecycle()
    // Ved «Legg til bruker» kan man gå tilbake til brukeren man var logget inn som
    if (onCancel != null) BackHandler(onBack = onCancel)
    val context = LocalContext.current
    var host by rememberSaveable { mutableStateOf("social.gaute.space") }
    val submit = { auth.login(host) { url -> openCustomTab(context, url) } }

    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Panel {
            Column(Modifier.padding(20.dp)) {
                Text(
                    stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    color = PT.TopBarText, fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(10.dp))
                if (onCancel != null) {
                    Text(stringResource(R.string.add_account), color = PT.Text, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(stringResource(R.string.add_account_intro), color = PT.Text)
                    Spacer(Modifier.height(10.dp))
                }
                Text(stringResource(R.string.login_intro), color = PT.Text)
                Spacer(Modifier.height(18.dp))
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text(stringResource(R.string.instance_label)) },
                    singleLine = true,
                    shape = RectangleShape,
                    colors = ptFieldColors(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { if (host.isNotBlank() && !state.busy) submit() }),
                    modifier = Modifier.fillMaxWidth(),
                )
                state.error?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(it, color = PT.Orange)
                }
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PButton(
                        stringResource(R.string.login_button),
                        onClick = { submit() },
                        enabled = host.isNotBlank() && !state.busy,
                        filled = true,
                    )
                    if (onCancel != null) {
                        Spacer(Modifier.width(10.dp))
                        PButton(stringResource(R.string.cancel), onCancel)
                    }
                    if (state.busy) {
                        Spacer(Modifier.width(14.dp))
                        CircularProgressIndicator(Modifier.size(24.dp), color = PT.Link, strokeWidth = 2.dp)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Lister

@Composable
fun ErrorPanel(message: String, onRetry: () -> Unit) {
    Panel(borderColor = PT.Red) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(message, color = PT.Text)
            PButton(stringResource(R.string.retry), onRetry)
        }
    }
}

/** Utløses ved dobbelttrykk på toppfeltet: listen som vises ruller helt til toppen. */
val LocalScrollToTop = staticCompositionLocalOf<SharedFlow<Unit>> { MutableSharedFlow() }

@Composable
internal fun ScrollToTopOnRequest(listState: LazyListState) {
    val requests = LocalScrollToTop.current
    LaunchedEffect(requests, listState) {
        requests.collect {
            // Langt nede i listen: hopp først nesten til toppen, så animasjonen blir kort
            if (listState.firstVisibleItemIndex > 10) listState.scrollToItem(10)
            listState.animateScrollToItem(0)
        }
    }
}

/** Utløses ved dobbelttrykk på en menyknapp nederst: feeden som vises lastes på nytt. */
val LocalRefreshRequests = staticCompositionLocalOf<SharedFlow<Unit>> { MutableSharedFlow() }

@Composable
internal fun RefreshOnRequest(onRefresh: () -> Unit) {
    val requests = LocalRefreshRequests.current
    val latest by rememberUpdatedState(onRefresh)
    LaunchedEffect(requests) { requests.collect { latest() } }
}

@Composable
internal fun LoadingRow() {
    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(Modifier.size(28.dp), color = PT.Link, strokeWidth = 2.dp)
    }
}

/** Tidslinje/profil-liste med pull-to-refresh og automatisk «hent flere». */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusFeed(
    vm: PagedStatusViewModel,
    actions: StatusActions,
    modifier: Modifier = Modifier,
    header: (LazyListScope.() -> Unit)? = null,
    emptyText: Int = R.string.empty_timeline,
    card: @Composable (Status, StatusActions) -> Unit = { s, a -> StatusCard(s, a) },
) {
    val items by vm.items.collectAsStateWithLifecycle()
    val paging by vm.paging.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    ScrollToTopOnRequest(listState)
    RefreshOnRequest { vm.refresh() }

    LaunchedEffect(vm) { vm.ensureLoaded() }
    val nearEnd by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && last >= info.totalItemsCount - 4
        }
    }
    LaunchedEffect(nearEnd, items.size) { if (nearEnd && items.isNotEmpty()) vm.loadMore() }

    PullToRefreshBox(isRefreshing = paging.refreshing, onRefresh = { vm.refresh() }, modifier = modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            header?.invoke(this)
            if (paging.error != null && items.isEmpty()) {
                item { ErrorPanel(paging.error ?: "", onRetry = { vm.refresh() }) }
            } else if (items.isEmpty() && !paging.refreshing) {
                item { Panel { Text(stringResource(emptyText), Modifier.padding(14.dp), color = PT.Text) } }
            }
            items(items, key = { it.id }) { card(it, actions) }
            if (paging.loading) item { LoadingRow() }
        }
    }
}

// ---------------------------------------------------------------- Tråd

@Composable
fun ThreadScreen(statusId: String, vm: ThreadViewModel, actions: StatusActions, modifier: Modifier = Modifier) {
    val items by vm.items.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    ScrollToTopOnRequest(listState)
    var scrolled by remember { mutableStateOf(false) }

    LaunchedEffect(vm) { vm.load() }
    LaunchedEffect(items.size) {
        if (!scrolled && items.isNotEmpty()) {
            val index = items.indexOfFirst { (it.reblog ?: it).id == statusId }
            if (index > 0) listState.scrollToItem(index)
            scrolled = true
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (error != null && items.isEmpty()) item { ErrorPanel(error ?: "", onRetry = { vm.reload() }) }
        items(items, key = { it.id }) { s ->
            val isFocus = (s.reblog ?: s).id == statusId
            StatusCard(s, actions, highlighted = isFocus, clickable = !isFocus)
        }
        if (loading) item { LoadingRow() }
    }
}

// ---------------------------------------------------------------- Varsler

@Composable
private fun notificationText(n: Notification): String {
    val name = n.account.name
    return when (n.type) {
        "mention" -> stringResource(R.string.notif_mention, name)
        "reblog" -> stringResource(R.string.notif_reblog, name)
        "favourite" -> stringResource(R.string.notif_favourite, name)
        "follow" -> stringResource(R.string.notif_follow, name)
        "follow_request" -> stringResource(R.string.notif_follow_request, name)
        "poll" -> stringResource(R.string.notif_poll)
        "status" -> stringResource(R.string.notif_status, name)
        "update" -> stringResource(R.string.notif_update, name)
        "pleroma:emoji_reaction" -> stringResource(R.string.notif_reaction, name, n.emoji ?: "")
        else -> stringResource(R.string.notif_other, name, n.type)
    }
}

/** Filterlinje øverst i varsler: Alle / Svar. */
@Composable
private fun NotificationFilterBar(selected: NotificationFilter, onSelect: (NotificationFilter) -> Unit) {
    Column(Modifier.fillMaxWidth().background(PT.Fg.copy(alpha = 0.92f))) {
        Row(
            Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PButton(
                stringResource(R.string.notif_filter_all), { onSelect(NotificationFilter.ALL) },
                Modifier.weight(1f), filled = selected == NotificationFilter.ALL,
            )
            PButton(
                stringResource(R.string.notif_filter_replies), { onSelect(NotificationFilter.REPLIES) },
                Modifier.weight(1f), filled = selected == NotificationFilter.REPLIES,
            )
        }
        HorizontalDivider(color = PT.Border)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    vm: NotificationsViewModel,
    onOpenStatus: (String) -> Unit,
    onOpenProfile: (Account) -> Unit,
    modifier: Modifier = Modifier,
) {
    val items by vm.items.collectAsStateWithLifecycle()
    val paging by vm.paging.collectAsStateWithLifecycle()
    val filter by vm.filter.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    ScrollToTopOnRequest(listState)
    RefreshOnRequest { vm.refresh() }

    LaunchedEffect(vm) { vm.ensureLoaded() }
    LaunchedEffect(filter) { listState.scrollToItem(0) }
    val nearEnd by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && last >= info.totalItemsCount - 4
        }
    }
    LaunchedEffect(nearEnd, items.size) { if (nearEnd && items.isNotEmpty()) vm.loadMore() }

    Column(modifier.fillMaxSize()) {
        NotificationFilterBar(filter, onSelect = { vm.setFilter(it) })
        PullToRefreshBox(
            isRefreshing = paging.refreshing,
            onRefresh = { vm.refresh() },
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (paging.error != null && items.isEmpty()) {
                    item { ErrorPanel(paging.error ?: "", onRetry = { vm.refresh() }) }
                } else if (items.isEmpty() && !paging.refreshing) {
                    item {
                        Panel {
                            Text(
                                stringResource(
                                    if (filter == NotificationFilter.REPLIES) R.string.empty_replies
                                    else R.string.empty_notifications,
                                ),
                                Modifier.padding(14.dp), color = PT.Text,
                            )
                        }
                    }
                }
                items(items, key = { it.id }) { n ->
                    val target = (n.status?.reblog ?: n.status)
                    Panel(
                        Modifier.clickable {
                            if (target != null) onOpenStatus(target.id) else onOpenProfile(n.account)
                        },
                    ) {
                        Column(Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Avatar(n.account.avatar, 36, Modifier.clickable { onOpenProfile(n.account) })
                                Spacer(Modifier.width(10.dp))
                                Icon(
                                    when (n.type) {
                                        "reblog" -> Icons.Filled.Repeat
                                        "favourite" -> Icons.Filled.Star
                                        "pleroma:emoji_reaction" -> Icons.Filled.Favorite
                                        else -> Icons.Filled.Person
                                    },
                                    null,
                                    tint = when (n.type) {
                                        "reblog" -> PT.Green
                                        "favourite" -> PT.Orange
                                        "pleroma:emoji_reaction" -> PT.Red
                                        else -> PT.Blue
                                    },
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                EmojiText(
                                    notificationText(n), n.account.emojis, Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium, color = PT.Text, maxLines = 2,
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(relativeTime(n.createdAt), style = MaterialTheme.typography.bodySmall, color = PT.Muted)
                            }
                            if (target != null && target.content.isNotBlank() && n.type != "poll") {
                                Spacer(Modifier.height(8.dp))
                                RichText(
                                    target.content, target.emojis,
                                    style = MaterialTheme.typography.bodySmall, color = PT.Muted, maxLines = 4,
                                    mentions = target.mentions,
                                )
                            }
                        }
                    }
                }
                if (paging.loading) item { LoadingRow() }
            }
        }
    }
}

// ---------------------------------------------------------------- Profil

/** Profilbildet i stort format, i samme stil som «Bytt bruker». */
@Composable
private fun AvatarDialog(account: Account, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.border(1.dp, PT.Border),
        containerColor = PT.Fg,
        title = {
            EmojiText(
                account.name, account.emojis,
                color = PT.TopBarText, fontWeight = FontWeight.Bold, maxLines = 2,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AsyncImage(
                    model = account.avatar,
                    contentDescription = stringResource(R.string.avatar_description, account.name),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .background(PT.Bg)
                        .border(1.dp, PT.Border),
                )
                Text("@" + account.acct, color = PT.Muted, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.about_close), color = PT.Link)
            }
        },
    )
}

@Composable
fun AccountScreen(
    accountId: String,
    isMe: Boolean,
    vm: AccountViewModel,
    actions: StatusActions,
    onLogout: () -> Unit,
    onSwitchAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val account by vm.account.collectAsStateWithLifecycle()
    val relationship by vm.relationship.collectAsStateWithLifecycle()
    LaunchedEffect(accountId) { vm.loadHeader(isMe) }

    StatusFeed(vm, actions, modifier, header = {
        item {
            account?.let { a ->
                Panel {
                    if (a.header.isNotBlank()) {
                        AsyncImage(
                            model = a.header, contentDescription = null, contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().height(120.dp).background(PT.Fg),
                        )
                    }
                    Column(Modifier.padding(12.dp)) {
                        var showAvatar by rememberSaveable { mutableStateOf(false) }
                        if (showAvatar) AvatarDialog(a, onDismiss = { showAvatar = false })
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Trykk på profilbildet for å se det i stort format
                            Avatar(a.avatar, 64, Modifier.clickable { showAvatar = true })
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                EmojiText(
                                    a.name, a.emojis,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = PT.Text, fontWeight = FontWeight.Bold,
                                )
                                Text("@" + a.acct, color = PT.Muted, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        if (a.note.isNotBlank()) {
                            Spacer(Modifier.height(10.dp))
                            RichText(a.note, a.emojis, style = MaterialTheme.typography.bodyMedium, color = PT.Text)
                        }
                        if (a.fields.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            FieldsTable(a)
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                            Counter(a.statusesCount, stringResource(R.string.profile_posts))
                            Counter(a.followingCount, stringResource(R.string.profile_following))
                            Counter(a.followersCount, stringResource(R.string.profile_followers))
                        }
                        Spacer(Modifier.height(12.dp))
                        if (isMe) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                PButton(stringResource(R.string.logout), onLogout)
                                PButton(stringResource(R.string.switch_account), onSwitchAccount)
                            }
                        } else {
                            val r = relationship
                            val label = when {
                                r?.following == true -> R.string.profile_unfollow
                                r?.requested == true -> R.string.profile_requested
                                else -> R.string.profile_follow
                            }
                            var showLists by rememberSaveable { mutableStateOf(false) }
                            if (showLists) ListMembershipDialog(vm, onDismiss = { showLists = false })
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                PButton(
                                    stringResource(label), { vm.toggleFollow() },
                                    enabled = r != null, filled = r?.following != true && r?.requested != true,
                                )
                                Spacer(Modifier.weight(1f))
                                // Legg til i liste – helt til høyre på samme linje som «Følg»
                                PButton(
                                    stringResource(R.string.lists_button), { showLists = true },
                                    icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                                )
                            }
                        }
                    }
                }
            }
        }
    })
}

@Composable
private fun Counter(value: Int, label: String) {
    Column {
        Text(value.toString(), color = PT.Text, fontWeight = FontWeight.Bold)
        Text(label, color = PT.Muted, style = MaterialTheme.typography.bodySmall)
    }
}

/** Tabell med profilens egne felt (metadata): navn til venstre, verdi (med lenker) til høyre. */
@Composable
private fun FieldsTable(account: Account) {
    Column(Modifier.fillMaxWidth().border(1.dp, PT.Border.copy(alpha = 0.6f))) {
        account.fields.forEachIndexed { i, f ->
            if (i > 0) HorizontalDivider(color = PT.Border.copy(alpha = 0.35f))
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                Box(
                    Modifier
                        .weight(0.34f)
                        .fillMaxHeight()
                        .background(PT.Bg.copy(alpha = 0.35f))
                        .padding(8.dp),
                ) {
                    EmojiText(
                        f.name, account.emojis,
                        style = MaterialTheme.typography.bodySmall,
                        color = PT.Link, fontWeight = FontWeight.Bold,
                    )
                }
                VerticalDivider(color = PT.Border.copy(alpha = 0.35f))
                Row(
                    Modifier.weight(0.66f).padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RichText(
                        f.value, account.emojis, Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall, color = PT.Text,
                    )
                    if (!f.verifiedAt.isNullOrBlank()) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Filled.Check, stringResource(R.string.profile_verified),
                            tint = PT.Green, modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}
