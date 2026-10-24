package app.pleromastisk.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.pleromastisk.Graph
import app.pleromastisk.R
import app.pleromastisk.data.MastoList
import app.pleromastisk.data.MediaAttachment
import app.pleromastisk.data.Status
import app.pleromastisk.data.TimelineKind
import app.pleromastisk.vm.AccountViewModel
import app.pleromastisk.vm.AuthState
import app.pleromastisk.vm.AuthViewModel
import app.pleromastisk.vm.ListTimelineViewModel
import app.pleromastisk.vm.ListsViewModel
import app.pleromastisk.vm.NotificationsViewModel
import app.pleromastisk.vm.SearchViewModel
import app.pleromastisk.vm.TagViewModel
import app.pleromastisk.vm.PagedStatusViewModel
import app.pleromastisk.vm.StatusListViewModel
import app.pleromastisk.vm.ThreadViewModel
import app.pleromastisk.vm.TimelineViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

private const val HOME = "home"
/** Spesialverdi for «valgt feed» i Hjem: lokal tidslinje (kolliderer ikke med liste-ID-er). */
private const val LOCAL_FEED = "__local__"
private const val FEDERATED = "federated"
private const val NOTIFICATIONS = "notifications"
private const val SEARCH = "search"
private const val PROFILE = "profile"
private const val INSTANCE_ABOUT = "instance_about"
private const val CHAT = "chat"
private const val COMPOSE_BASE = "compose"
private const val COMPOSE =
    "compose?replyId={replyId}&replyMentions={replyMentions}&replyVis={replyVis}&editId={editId}"

private val TopLevel = setOf(HOME, FEDERATED, NOTIFICATIONS, SEARCH, PROFILE)

/** Fanene i bunnmenyen, i samme rekkefølge som knappene (brukes ved sveiping). */
private val TabOrder = listOf(HOME, FEDERATED, NOTIFICATIONS, SEARCH, PROFILE)

/**
 * Sveip til venstre/høyre på en fane bytter til neste/forrige fane i bunnmenyen.
 * Loddrett rulling og ting som selv ruller sidelengs har forrang.
 */
private fun Modifier.swipeBetweenTabs(route: String?, onNavigate: (String) -> Unit): Modifier {
    val index = TabOrder.indexOf(route)
    if (index < 0) return this
    return pointerInput(route) {
        val threshold = 80.dp.toPx()
        var total = 0f
        detectHorizontalDragGestures(
            onDragStart = { total = 0f },
            onHorizontalDrag = { change, dx ->
                change.consume() // ellers tolkes slutten av sveipet som et trykk på innlegget
                total += dx
            },
            onDragEnd = {
                val next = when {
                    total < -threshold -> index + 1
                    total > threshold -> index - 1
                    else -> -1
                }
                TabOrder.getOrNull(next)?.let(onNavigate)
            },
        )
    }
}

/** Retning (+1/-1) mellom to faner, eller null når en av dem ikke er en fane. */
private fun tabDirection(from: NavBackStackEntry, to: NavBackStackEntry): Int? {
    val a = TabOrder.indexOf(from.destination.route)
    val b = TabOrder.indexOf(to.destination.route)
    return if (a < 0 || b < 0 || a == b) null else if (b > a) 1 else -1
}

@Composable
fun PleromastiskRoot(auth: AuthViewModel) {
    val state by auth.state.collectAsStateWithLifecycle()
    var viewer by remember { mutableStateOf<Pair<List<MediaAttachment>, Int>?>(null) }
    // Chatten er ikke knyttet til Pleroma-brukeren, og holdes i live også når man bytter bruker
    val chat = rememberChatSession()
    CompositionLocalProvider(LocalImageViewer provides { images, index -> viewer = images to index }) {
        Box(Modifier.fillMaxSize()) {
            if (!state.loggedIn) {
                LoginScreen(auth)
            } else {
                // Ny bruker: alle skjermer (og ViewModels) lages på nytt
                key(state.activeKey) { MainScaffold(auth, state, chat) }
                if (state.addingAccount) {
                    Box(Modifier.fillMaxSize().background(PT.Bg)) {
                        LoginScreen(auth, onCancel = auth::cancelAddingAccount)
                    }
                }
            }
            // Bildevisningen ligger øverst, over alt annet (også bunnlinjen)
            viewer?.let { (images, index) -> ImageViewer(images, index) { viewer = null } }
        }
    }
}

@Composable
private fun MainScaffold(auth: AuthViewModel, state: AuthState, chat: ChatSession) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val showChrome = route != null && !route.startsWith(COMPOSE_BASE)
    val me = state.me

    // Valgt liste i «Hjem» (null = vanlig hjemmetidslinje)
    val listsVm: ListsViewModel = viewModel(key = "lists_${state.activeKey}")
    val lists by listsVm.lists.collectAsStateWithLifecycle()
    var selectedListId by rememberSaveable { mutableStateOf<String?>(null) }
    var showAbout by rememberSaveable { mutableStateOf(false) }
    var showSwitcher by rememberSaveable { mutableStateOf(false) }
    val selectedList = lists.firstOrNull { it.id == selectedListId }

    fun go(target: String) = nav.navigate(target) {
        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }

    fun actionsFor(vm: StatusListViewModel) = StatusActions(
        myId = me?.id,
        onOpenStatus = { nav.navigate("thread/${it.id}") },
        onOpenProfile = { nav.navigate("account/${it.id}") },
        onReply = { s -> nav.navigate(replyRoute(s, me?.acct)) },
        onBoost = vm::toggleBoost,
        onFavourite = vm::toggleFavourite,
        onBookmark = vm::toggleBookmark,
        onVote = vm::vote,
        onDelete = vm::delete,
        canEdit = state.canEdit,
        onEdit = { nav.navigate("compose?editId=${Uri.encode(it.id)}") },
    )

    if (showAbout) AboutDialog(onDismiss = { showAbout = false })
    if (showSwitcher) {
        AccountSwitchDialog(
            accounts = state.accounts,
            activeKey = state.activeKey,
            onSelect = { showSwitcher = false; auth.switchTo(it) },
            onAdd = { showSwitcher = false; auth.startAddingAccount() },
            onDismiss = { showSwitcher = false },
        )
    }

    // Dobbelttrykk på tomt område i toppfeltet ruller listen på skjermen til toppen
    val scrollToTop = remember { MutableSharedFlow<Unit>(extraBufferCapacity = 1) }
    // Dobbelttrykk på en menyknapp nederst ruller til toppen og laster feeden på nytt
    val refreshRequests = remember { MutableSharedFlow<Unit>(extraBufferCapacity = 1) }

    // Trykk på @omtale åpner profilen i appen. Ukjent konto slås opp via egen instans;
    // finnes den ikke der, åpnes lenken i nettleseren som før.
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val openProfileLink = remember(nav) {
        { id: String?, url: String ->
            if (id != null) {
                nav.navigate("account/$id")
            } else {
                scope.launch {
                    val found = runCatching { Graph.api.search(url, "accounts", resolve = true).accounts.firstOrNull() }
                        .getOrNull()
                    if (found != null) nav.navigate("account/${found.id}")
                    else runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                }
            }
            Unit
        }
    }

    // Trykk på #emneknagg i et innlegg åpner emneknaggens tidslinje
    val openTag = remember(nav) { { tag: String -> nav.navigate("tag/${Uri.encode(tag)}") } }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            if (showChrome) {
                PTopBar(
                    title = when {
                        route == HOME && selectedListId == LOCAL_FEED -> stringResource(R.string.menu_local_feed)
                        route == HOME && selectedList != null -> selectedList.title
                        route?.startsWith("tag/") == true -> "#" + entry?.arguments?.getString("tag").orEmpty()
                        else -> titleFor(route)
                    },
                    onBack = if (route in TopLevel) null else ({ nav.popBackStack() }),
                    onDoubleTap = { scrollToTop.tryEmit(Unit) },
                    titleContent = if (route == HOME) { title ->
                        ListMenuTitle(
                            title = title,
                            lists = lists,
                            selectedId = selectedListId,
                            onOpen = { listsVm.refresh() },
                            onSelect = { selectedListId = it },
                        )
                    } else null,
                    actions = {
                        if (route == NOTIFICATIONS) entry?.let { NewNotificationsBadge(viewModel(it)) }
                        if (route == HOME && selectedListId == LOCAL_FEED) {
                            // «Om»-siden til instansen brukeren er logget inn på
                            TextButton(onClick = { nav.navigate(INSTANCE_ABOUT) }) {
                                Text(stringResource(R.string.instance_about_button), color = PT.TopBarText)
                            }
                        } else if (route == HOME) {
                            IconButton(onClick = { nav.navigate(CHAT) }) {
                                Icon(Icons.AutoMirrored.Filled.Chat, stringResource(R.string.chat_button), tint = PT.TopBarText)
                            }
                        }
                        if (route == PROFILE) {
                            IconButton(onClick = { showAbout = true }) {
                                Icon(Icons.Filled.Info, stringResource(R.string.about_button), tint = PT.TopBarText)
                            }
                        }
                    },
                )
            }
        },
        // Navigasjonsknapper og «skriv»-knappen ligger alltid nederst på siden.
        bottomBar = { if (showChrome) PBottomBar(
                route,
                onNavigate = ::go,
                onDoubleTap = { scrollToTop.tryEmit(Unit); refreshRequests.tryEmit(Unit) },
                onCompose = { nav.navigate(COMPOSE_BASE) },
            ) },
    ) { pad ->
        CompositionLocalProvider(
            LocalHashtagClick provides openTag,
            LocalProfileLinkClick provides openProfileLink,
            LocalScrollToTop provides scrollToTop,
            LocalRefreshRequests provides refreshRequests,
        ) {
        NavHost(
            navController = nav,
            startDestination = HOME,
            // consumeWindowInsets: imePadding() på sidene trekker fra bunnmenyen, som tastaturet uansett dekker
            modifier = (if (showChrome) Modifier.padding(pad).consumeWindowInsets(pad) else Modifier).swipeBetweenTabs(route, ::go),
            // Mellom faner glir siden inn fra siden den ligger på; ellers vanlig toning
            enterTransition = {
                tabDirection(initialState, targetState)?.let { dir -> slideInHorizontally { w -> dir * w } }
                    ?: fadeIn(tween(700))
            },
            exitTransition = {
                tabDirection(initialState, targetState)?.let { dir -> slideOutHorizontally { w -> -dir * w } }
                    ?: fadeOut(tween(700))
            },
        ) {
            composable(HOME) {
                val listId = selectedListId
                val vm: PagedStatusViewModel = when (listId) {
                    null -> viewModel<TimelineViewModel>(
                        key = "tl_$HOME",
                        factory = viewModelFactory { initializer { TimelineViewModel(TimelineKind.HOME) } },
                    )
                    LOCAL_FEED -> viewModel<TimelineViewModel>(
                        key = "tl_local",
                        factory = viewModelFactory { initializer { TimelineViewModel(TimelineKind.LOCAL) } },
                    )
                    else -> viewModel<ListTimelineViewModel>(
                        key = "list_$listId",
                        factory = viewModelFactory { initializer { ListTimelineViewModel(listId) } },
                    )
                }
                StatusFeed(vm, actionsFor(vm))
            }
            for ((r, kind) in listOf(FEDERATED to TimelineKind.FEDERATED)) {
                composable(r) {
                    val vm: TimelineViewModel = viewModel(
                        key = "tl_$r",
                        factory = viewModelFactory { initializer { TimelineViewModel(kind) } },
                    )
                    StatusFeed(vm, actionsFor(vm))
                }
            }
            composable(NOTIFICATIONS) {
                val vm: NotificationsViewModel = viewModel()
                // Telleren for nye varsler nullstilles når man går vekk fra varselsiden
                DisposableEffect(vm) { onDispose { vm.markSeen() } }
                NotificationsScreen(
                    vm,
                    onOpenStatus = { nav.navigate("thread/$it") },
                    onOpenProfile = { nav.navigate("account/${it.id}") },
                )
            }
            composable(PROFILE) {
                if (me == null) {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        androidx.compose.material3.CircularProgressIndicator(color = PT.Link)
                    }
                } else {
                    val vm: AccountViewModel = viewModel(
                        key = "me_${me.id}",
                        factory = viewModelFactory { initializer { AccountViewModel(me.id) } },
                    )
                    AccountScreen(
                        me.id, true, vm, actionsFor(vm),
                        onLogout = { auth.logout() },
                        onSwitchAccount = { showSwitcher = true },
                    )
                }
            }
            composable(SEARCH) {
                val vm: SearchViewModel = viewModel()
                SearchScreen(
                    vm,
                    onOpenAccount = { nav.navigate("account/${it.id}") },
                    onOpenTag = { nav.navigate("tag/${Uri.encode(it)}") },
                )
            }
            composable(INSTANCE_ABOUT) { InstanceAboutScreen() }
            composable(CHAT) { ChatScreen(chat) }
            composable("tag/{tag}", arguments = listOf(navArgument("tag") { type = NavType.StringType })) { e ->
                val tag = e.arguments?.getString("tag").orEmpty()
                val vm: TagViewModel = viewModel(
                    key = "tag_$tag",
                    factory = viewModelFactory { initializer { TagViewModel(tag) } },
                )
                StatusFeed(vm, actionsFor(vm))
            }
            composable("account/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { e ->
                val id = e.arguments?.getString("id").orEmpty()
                val vm: AccountViewModel = viewModel(
                    key = "acc_$id",
                    factory = viewModelFactory { initializer { AccountViewModel(id) } },
                )
                AccountScreen(
                    id, id == me?.id, vm, actionsFor(vm),
                    onLogout = { auth.logout() },
                    onSwitchAccount = { showSwitcher = true },
                )
            }
            composable("thread/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { e ->
                val id = e.arguments?.getString("id").orEmpty()
                val vm: ThreadViewModel = viewModel(
                    key = "thr_$id",
                    factory = viewModelFactory { initializer { ThreadViewModel(id) } },
                )
                ThreadScreen(id, vm, actionsFor(vm))
            }
            composable(
                COMPOSE,
                arguments = listOf(
                    navArgument("replyId") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("replyMentions") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("replyVis") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("editId") { type = NavType.StringType; nullable = true; defaultValue = null },
                ),
            ) { e ->
                ComposeScreen(
                    replyId = e.arguments?.getString("replyId"),
                    replyMentions = e.arguments?.getString("replyMentions"),
                    replyVisibility = e.arguments?.getString("replyVis"),
                    editId = e.arguments?.getString("editId"),
                    maxChars = state.maxChars,
                    altLimit = state.altLimit,
                    postFormats = state.postFormats,
                    onClose = { nav.popBackStack() },
                )
            }
        }
        }
    }
}

private fun replyRoute(s: Status, myAcct: String?): String {
    val handles = (listOf(s.account.acct) + s.mentions.map { it.acct })
        .filter { it.isNotBlank() && it != myAcct }
        .distinct()
        .joinToString(" ")
    return "compose?replyId=${Uri.encode(s.id)}&replyMentions=${Uri.encode(handles)}&replyVis=${Uri.encode(s.visibility)}"
}

@Composable
private fun titleFor(route: String?): String = stringResource(
    when {
        route == HOME -> R.string.title_home
        route == FEDERATED -> R.string.title_federated
        route == NOTIFICATIONS -> R.string.title_notifications
        route == SEARCH -> R.string.title_search
        route == PROFILE -> R.string.title_profile
        route == INSTANCE_ABOUT -> R.string.instance_about_title
        route == CHAT -> R.string.title_chat
        route?.startsWith("thread") == true -> R.string.title_thread
        else -> R.string.title_account
    },
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PTopBar(
    title: String,
    onBack: (() -> Unit)?,
    onDoubleTap: () -> Unit = {},
    titleContent: (@Composable (String) -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    // Knapper og tittelmeny tar sine egne trykk; dobbelttrykk på resten av feltet fanges her
    Column(Modifier.pointerInput(Unit) { detectTapGestures(onDoubleTap = { onDoubleTap() }) }) {
        TopAppBar(
            title = {
                if (titleContent != null) titleContent(title)
                else Text(title, color = PT.TopBarText, fontWeight = FontWeight.Bold)
            },
            navigationIcon = {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), tint = PT.TopBarText)
                    }
                }
            },
            actions = actions,
            colors = TopAppBarDefaults.topAppBarColors(containerColor = PT.TopBar),
        )
        HorizontalDivider(color = PT.Border)
    }
}

/** Bunnlinje: Hjem · Føderert · Varsler · Søk · [Skriv] · Profil */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PBottomBar(
    route: String?,
    onNavigate: (String) -> Unit,
    onDoubleTap: () -> Unit,
    onCompose: () -> Unit,
) {
    // Hold inne en vilkårlig menyknapp for å skjule/vise tekstene under ikonene
    var compact by remember { mutableStateOf(Graph.store.compactNav) }
    val haptic = LocalHapticFeedback.current
    val toggle = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        compact = !compact
        Graph.store.compactNav = compact
    }
    // Første trykk navigerer med en gang; et nytt trykk på samme knapp like etter er et dobbelttrykk
    val doubleTapTimeout = LocalViewConfiguration.current.doubleTapTimeoutMillis
    var lastTap by remember { mutableStateOf<Pair<String, Long>?>(null) }
    val tap = { target: String ->
        val now = System.currentTimeMillis()
        val prev = lastTap
        onNavigate(target)
        if (prev != null && prev.first == target && now - prev.second <= doubleTapTimeout) {
            lastTap = null
            onDoubleTap()
        } else {
            lastTap = target to now
        }
    }
    Column(Modifier.fillMaxWidth().background(PT.TopBar)) {
        HorizontalDivider(color = PT.Border)
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().height(58.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavItem(Icons.Filled.Home, R.string.nav_home, route == HOME, compact, toggle) { tap(HOME) }
            NavItem(Icons.Filled.Public, R.string.nav_federated, route == FEDERATED, compact, toggle) { tap(FEDERATED) }
            NavItem(Icons.Filled.Notifications, R.string.nav_notifications, route == NOTIFICATIONS, compact, toggle) {
                tap(NOTIFICATIONS)
            }
            NavItem(Icons.Filled.Search, R.string.nav_search, route == SEARCH, compact, toggle) { tap(SEARCH) }
            // «Skriv nytt innlegg»: fremhevet knapp midt i linjen
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(46.dp)
                        .background(PT.Link)
                        .border(1.dp, PT.Border)
                        .combinedClickable(onLongClick = toggle, onClick = onCompose),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Edit, stringResource(R.string.nav_compose), tint = PT.Fg)
                }
            }
            NavItem(Icons.Filled.Person, R.string.nav_profile, route == PROFILE, compact, toggle) { tap(PROFILE) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RowScope.NavItem(
    icon: ImageVector,
    label: Int,
    selected: Boolean,
    compact: Boolean,
    onLongClick: () -> Unit,
    onClick: () -> Unit,
) {
    val tint = if (selected) PT.TopBarText else PT.Link
    Column(
        Modifier
            .weight(1f)
            .fillMaxWidth()
            .height(58.dp)
            .combinedClickable(onLongClick = onLongClick, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        Icon(icon, stringResource(label), tint = tint, modifier = Modifier.size(24.dp))
        // Uten tekst står ikonet alene, midt mellom topp- og bunnkanten
        if (!compact) Text(stringResource(label), color = tint, fontSize = 10.sp, maxLines = 1)
    }
}

/** «X nye varsler» øverst til høyre på varselsiden, etter oppdatering. */
@Composable
private fun NewNotificationsBadge(vm: NotificationsViewModel) {
    val count by vm.newCount.collectAsStateWithLifecycle()
    if (count > 0) {
        Text(
            pluralStringResource(R.plurals.new_notifications, count, count),
            color = PT.TopBarText,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.padding(end = 12.dp),
        )
    }
}

/** «Hjem ▾» – trykk for å bytte mellom hjemmetidslinjen og egne lister. */
@Composable
private fun ListMenuTitle(
    title: String,
    lists: List<MastoList>,
    selectedId: String?,
    onOpen: () -> Unit,
    onSelect: (String?) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier
                .clickable { open = true; onOpen() }
                .padding(vertical = 8.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, color = PT.TopBarText, fontWeight = FontWeight.Bold, maxLines = 1)
            Icon(
                Icons.Filled.ArrowDropDown, stringResource(R.string.lists_choose),
                tint = PT.TopBarText,
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(R.string.title_home),
                        color = if (selectedId == null) PT.TopBarText else PT.FgText,
                        fontWeight = if (selectedId == null) FontWeight.Bold else FontWeight.Normal,
                    )
                },
                leadingIcon = { Icon(Icons.Filled.Home, null, tint = PT.Link) },
                onClick = { open = false; onSelect(null) },
            )
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(R.string.menu_local_feed),
                        color = if (selectedId == LOCAL_FEED) PT.TopBarText else PT.FgText,
                        fontWeight = if (selectedId == LOCAL_FEED) FontWeight.Bold else FontWeight.Normal,
                    )
                },
                leadingIcon = { Icon(Icons.Filled.People, null, tint = PT.Link) },
                onClick = { open = false; onSelect(LOCAL_FEED) },
            )
            HorizontalDivider(color = PT.Border.copy(alpha = 0.5f))
            DropdownMenuItem(
                enabled = false,
                text = { Text(stringResource(R.string.lists_header), color = PT.Muted, fontSize = 12.sp) },
                onClick = {},
            )
            if (lists.isEmpty()) {
                DropdownMenuItem(
                    enabled = false,
                    text = { Text(stringResource(R.string.lists_none), color = PT.Muted) },
                    onClick = {},
                )
            }
            lists.forEach { l ->
                val selected = l.id == selectedId
                DropdownMenuItem(
                    text = {
                        Text(
                            l.title,
                            color = if (selected) PT.TopBarText else PT.FgText,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.List, null, tint = PT.Link) },
                    onClick = { open = false; onSelect(l.id) },
                )
            }
        }
    }
}

private const val REPO_URL = "https://gaute.space/forgejo/gaute/Pleromastisk"

/** Infoboks som åpnes fra info-ikonet øverst til høyre på «Hjem». */
@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.border(1.dp, PT.Border),
        containerColor = PT.Fg,
        title = {
            Text(stringResource(R.string.about_title), color = PT.TopBarText, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(R.string.about_p1), color = PT.Text)
                Text(stringResource(R.string.about_p2), color = PT.Text)
                Text(
                    buildAnnotatedString {
                        append(stringResource(R.string.about_p3))
                        append(" ")
                        withLink(
                            LinkAnnotation.Url(
                                REPO_URL,
                                TextLinkStyles(SpanStyle(color = PT.Link, textDecoration = TextDecoration.Underline)),
                            ),
                        ) { append(REPO_URL) }
                    },
                    color = PT.Text,
                )
                Text(stringResource(R.string.about_p4), color = PT.Text)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.about_close), color = PT.Link)
            }
        },
    )
}
