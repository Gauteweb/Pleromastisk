package app.pleromastisk.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pleromastisk.Graph
import app.pleromastisk.R
import app.pleromastisk.data.Account
import app.pleromastisk.data.Card
import app.pleromastisk.data.MediaAttachment
import app.pleromastisk.data.Poll
import app.pleromastisk.data.Status
import app.pleromastisk.relativeTime
import coil.compose.AsyncImage
import coil.request.videoFrameMillis
import coil.request.ImageRequest

/** Alt et kort kan gjøre – skjermene kobler dette til sin ViewModel/navigasjon. */
class StatusActions(
    val myId: String?,
    val onOpenStatus: (Status) -> Unit,
    val onOpenProfile: (Account) -> Unit,
    val onReply: (Status) -> Unit,
    val onBoost: (Status) -> Unit,
    val onFavourite: (Status) -> Unit,
    val onBookmark: (Status) -> Unit,
    val onVote: (Status, Poll, List<Int>) -> Unit,
    val onDelete: (Status) -> Unit,
    val canEdit: Boolean = false,
    val onEdit: (Status) -> Unit = {},
)

@Composable
fun Avatar(url: String, size: Int, modifier: Modifier = Modifier) {
    AsyncImage(
        model = url,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size.dp)
            .background(PT.Fg)
            .border(1.dp, PT.Border),
    )
}

fun visibilityIcon(visibility: String): ImageVector = when (visibility) {
    "unlisted" -> Icons.Filled.LockOpen
    "private" -> Icons.Filled.Lock
    "direct" -> Icons.Filled.Email
    else -> Icons.Filled.Public
}

@Composable
fun StatusCard(
    status: Status,
    actions: StatusActions,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    clickable: Boolean = true,
) {
    val s = status.reblog ?: status
    var revealed by rememberSaveable(s.id) { mutableStateOf(s.spoilerText.isBlank()) }

    // Lenkeforhåndsvisning (laget av instansen). Vises ikke sammen med medier eller avstemning.
    // Avsender-app: serverens opplysning først, ellers «Pleromastisk» for innlegg postet herfra.
    val appName = s.application?.name?.takeIf { it.isNotBlank() }
        ?: if (Graph.store.wasPostedHere(s.id)) Graph.str(R.string.app_name) else null

    val card = s.card?.takeIf {
        it.url.startsWith("http") && (it.title.isNotBlank() || !it.image.isNullOrBlank()) &&
            s.mediaAttachments.isEmpty() && s.poll == null
    }
    val contentHtml = remember(s.content, card?.url) {
        if (card != null) stripCardLink(s.content, card.url) else s.content
    }

    Panel(
        modifier.then(if (clickable) Modifier.clickable { actions.onOpenStatus(s) } else Modifier),
        borderColor = if (highlighted) PT.TopBarText else PT.Border,
    ) {
        Column(Modifier.padding(10.dp)) {
            if (status.reblog != null) {
                Row(Modifier.padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Repeat, null, tint = PT.Green, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    EmojiText(
                        stringResource(R.string.boosted_by, status.account.name),
                        status.account.emojis,
                        style = MaterialTheme.typography.bodySmall,
                        color = PT.Muted,
                        maxLines = 1,
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(s.account.avatar, 44, Modifier.clickable { actions.onOpenProfile(s.account) })
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    EmojiText(
                        s.account.name, s.account.emojis,
                        style = MaterialTheme.typography.bodyMedium,
                        color = PT.Text, fontWeight = FontWeight.Bold, maxLines = 1,
                    )
                    Text(
                        "@" + s.account.acct,
                        style = MaterialTheme.typography.bodySmall,
                        color = PT.Muted, maxLines = 1,
                    )
                }
                Spacer(Modifier.width(6.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(visibilityIcon(s.visibility), null, tint = PT.Muted, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            relativeTime(s.createdAt) +
                                if (!s.editedAt.isNullOrBlank()) " · " + stringResource(R.string.edited) else "",
                            style = MaterialTheme.typography.bodySmall, color = PT.Muted,
                        )
                    }
                    // «via <app>» når instansen oppgir hvilken app innlegget ble postet fra
                    appName?.let { app ->
                        Text(
                            stringResource(R.string.via_app, app),
                            fontSize = 11.sp, color = PT.Muted, maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 130.dp).padding(top = 2.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            if (s.spoilerText.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Warning, null, tint = PT.Orange, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    EmojiText(s.spoilerText, s.emojis, Modifier.weight(1f), color = PT.Text, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(6.dp))
                    PButton(
                        text = stringResource(if (revealed) R.string.show_less else R.string.show_more),
                        onClick = { revealed = !revealed },
                    )
                }
                Spacer(Modifier.height(6.dp))
            }

            if (revealed) {
                if (contentHtml.isNotBlank() && !isBlankHtml(contentHtml)) {
                    RichText(
                        contentHtml, s.emojis, style = MaterialTheme.typography.bodyMedium, color = PT.Text,
                        mentions = s.mentions,
                    )
                }
                MediaGrid(s.mediaAttachments, s.sensitive)
                s.poll?.let { PollView(s, it, actions) }
                card?.let { LinkCard(it) }
            }

            ActionRow(s, actions)
        }
    }
}

/**
 * Kort i «Bilder»-fanen, slik Pixelfed og Instagram viser innlegg: avsender øverst, så bildene
 * (eller videoen) i full bredde, knapperaden, og til slutt teksten.
 */
@Composable
fun MediaPostCard(status: Status, actions: StatusActions, modifier: Modifier = Modifier) {
    val s = status.reblog ?: status
    var revealed by rememberSaveable(s.id) { mutableStateOf(s.spoilerText.isBlank()) }
    val media = remember(s.mediaAttachments) { s.mediaAttachments.filter { it.isVisual } }

    Panel(modifier.clickable { actions.onOpenStatus(s) }) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            if (status.reblog != null) {
                Row(Modifier.padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Repeat, null, tint = PT.Green, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    EmojiText(
                        stringResource(R.string.boosted_by, status.account.name),
                        status.account.emojis,
                        style = MaterialTheme.typography.bodySmall,
                        color = PT.Muted,
                        maxLines = 1,
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(s.account.avatar, 34, Modifier.clickable { actions.onOpenProfile(s.account) })
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    EmojiText(
                        s.account.name, s.account.emojis,
                        style = MaterialTheme.typography.bodyMedium,
                        color = PT.Text, fontWeight = FontWeight.Bold, maxLines = 1,
                    )
                    Text(
                        "@" + s.account.acct,
                        style = MaterialTheme.typography.bodySmall,
                        color = PT.Muted, maxLines = 1,
                    )
                }
                Spacer(Modifier.width(6.dp))
                Icon(visibilityIcon(s.visibility), null, tint = PT.Muted, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(relativeTime(s.createdAt), style = MaterialTheme.typography.bodySmall, color = PT.Muted)
            }
        }

        // Innholdsvarsel skjuler også bildene, med varselteksten som etikett
        val hiddenLabel = when {
            s.spoilerText.isNotBlank() -> s.spoilerText.takeIf { !revealed }
            s.sensitive -> ""
            else -> null
        }
        MediaCarousel(media, hiddenLabel) { revealed = true }

        Column(Modifier.padding(horizontal = 10.dp)) {
            ActionRow(s, actions)
            if (s.spoilerText.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Warning, null, tint = PT.Orange, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    EmojiText(s.spoilerText, s.emojis, Modifier.weight(1f), color = PT.Text, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(6.dp))
                    PButton(
                        text = stringResource(if (revealed) R.string.show_less else R.string.show_more),
                        onClick = { revealed = !revealed },
                    )
                }
                Spacer(Modifier.height(6.dp))
            }
            if (revealed) {
                if (s.content.isNotBlank() && !isBlankHtml(s.content)) {
                    RichText(
                        s.content, s.emojis, style = MaterialTheme.typography.bodyMedium, color = PT.Text,
                        mentions = s.mentions,
                    )
                }
                s.poll?.let { PollView(s, it, actions) }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

/**
 * Bildene i et innlegg i full bredde. Flere bilder blas sidelengs, med «2/3» i hjørnet.
 * [hiddenLabel] ulik null: bildene er skjult bak et trykkbart felt (tom tekst = «sensitivt innhold»).
 */
@Composable
private fun MediaCarousel(items: List<MediaAttachment>, hiddenLabel: String?, onReveal: () -> Unit) {
    if (items.isEmpty()) return
    var shownSensitive by rememberSaveable(items.first().id) { mutableStateOf(false) }
    val open = rememberMediaOpener(items)
    // Pleroma oppgir ofte ikke størrelsen; da måles første bilde når det er lastet
    var measured by rememberSaveable(items.first().id) { mutableStateOf<Float?>(null) }
    // Høyden følger første bilde, innenfor Instagrams grenser (4:5 til 1,91:1); resten tilpasses
    val ratio = (items.first().aspectRatio ?: measured)?.coerceIn(0.8f, 1.91f) ?: 1f
    val pager = rememberPagerState { items.size }

    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(ratio)
            .background(PT.Bg)
            .clipToBounds(),
    ) {
        if (hiddenLabel != null && !(hiddenLabel.isEmpty() && shownSensitive)) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable { if (hiddenLabel.isEmpty()) shownSensitive = true else onReveal() },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    hiddenLabel.ifEmpty { stringResource(R.string.sensitive_media) },
                    color = PT.Orange, modifier = Modifier.padding(16.dp),
                )
            }
        } else {
            HorizontalPager(pager, Modifier.fillMaxSize(), key = { items[it].id }) { page ->
                val m = items[page]
                Box(Modifier.fillMaxSize().clickable { open(m) }, contentAlignment = Alignment.Center) {
                    val shown = mediaModel(m)
                    if (shown != null) {
                        AsyncImage(
                            model = shown,
                            contentDescription = m.description,
                            // Fyller feltet helt; bilder utenfor 4:5–1,91:1 beskjæres som hos Instagram
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            onSuccess = { state ->
                                if (page == 0 && m.aspectRatio == null) {
                                    val size = state.painter.intrinsicSize
                                    if (size.width > 0f && size.height > 0f) measured = size.width / size.height
                                }
                            },
                        )
                    }
                    MediaOverlays(m)
                }
            }
            if (items.size > 1) {
                Text(
                    "${pager.currentPage + 1}/${items.size}",
                    color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }
    }
}

@Composable
private fun ActionRow(s: Status, actions: StatusActions) {
    val canBoost = s.visibility == "public" || s.visibility == "unlisted" || s.reblogged
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ActionButton(
            Icons.AutoMirrored.Filled.Reply, stringResource(R.string.action_reply),
            s.repliesCount, PT.FgText,
        ) { actions.onReply(s) }
        ActionButton(
            Icons.Filled.Repeat, stringResource(R.string.action_boost),
            s.reblogsCount, if (s.reblogged) PT.Green else PT.Muted, enabled = canBoost,
        ) { actions.onBoost(s) }
        ActionButton(
            if (s.favourited) Icons.Filled.Star else Icons.Filled.StarBorder,
            stringResource(R.string.action_favourite),
            s.favouritesCount, if (s.favourited) PT.Orange else PT.Muted,
        ) { actions.onFavourite(s) }
        ActionButton(
            if (s.bookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
            stringResource(R.string.action_bookmark),
            null, if (s.bookmarked) PT.Blue else PT.Muted,
        ) { actions.onBookmark(s) }
        MoreMenu(s, actions)
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    description: String,
    count: Int?,
    tint: Color,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .heightIn(min = 40.dp)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, description, tint = if (enabled) tint else tint.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
        if (count != null) {
            // Antall vises alltid: tydelig når > 0, dempet når 0.
            val numberColor = when {
                count == 0 -> PT.Muted.copy(alpha = 0.55f)
                tint == PT.Muted -> PT.FgText
                else -> tint
            }
            Spacer(Modifier.width(4.dp))
            Text(
                count.toString(), color = numberColor, fontSize = 13.sp,
                fontWeight = if (count > 0) FontWeight.Medium else FontWeight.Normal,
            )
        }
    }
}

@Composable
private fun MoreMenu(s: Status, actions: StatusActions) {
    var open by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val link = s.url ?: s.uri

    Box {
        ActionButton(Icons.Filled.MoreVert, stringResource(R.string.action_more), null, PT.Muted) { open = true }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_open_browser)) },
                onClick = {
                    open = false
                    runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link))) }
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_copy_link)) },
                onClick = {
                    open = false
                    clipboard.setText(AnnotatedString(link))
                    Graph.toast(Graph.str(R.string.link_copied))
                },
            )
            if (actions.myId != null && actions.myId == s.account.id && actions.canEdit) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_edit)) },
                    onClick = { open = false; actions.onEdit(s) },
                )
            }
            if (actions.myId != null && actions.myId == s.account.id) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_delete), color = PT.Orange) },
                    onClick = { open = false; confirmDelete = true },
                )
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = PT.Fg,
            title = { Text(stringResource(R.string.delete_confirm_title), color = PT.Text) },
            text = { Text(stringResource(R.string.delete_confirm_text)) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; actions.onDelete(s) }) {
                    Text(stringResource(R.string.action_delete), color = PT.Orange)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.cancel), color = PT.Link)
                }
            },
        )
    }
}

@Composable
private fun MediaGrid(items: List<MediaAttachment>, sensitive: Boolean) {
    if (items.isEmpty()) return
    var hidden by rememberSaveable(items.firstOrNull()?.id) { mutableStateOf(sensitive) }
    val open = rememberMediaOpener(items)

    Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (hidden) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .background(PT.Fg)
                    .border(1.dp, PT.Border)
                    .clickable { hidden = false },
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.sensitive_media), color = PT.Orange, modifier = Modifier.padding(8.dp))
            }
        } else if (items.size == 1) {
            // Ett bilde: full bredde, høyden følger bildets egne proporsjoner (ingen beskjæring).
            MediaTile(items[0], single = true, onClick = { open(items[0]) }, modifier = Modifier.fillMaxWidth())
        } else {
            // Flere bilder: to kolonner. Hvert bilde vises hele (tilpasset en rute), ikke beskåret.
            items.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    row.forEach { m -> MediaTile(m, single = false, onClick = { open(m) }, modifier = Modifier.weight(1f)) }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * Hva som skal tegnes for et vedlegg: bilder i full oppløsning, andre typer (video m.m.) via
 * forhåndsvisningen. Pleroma lager ofte ingen egen forhåndsvisning for video (preview_url peker da
 * på selve videofilen) – da henter Coil et stillbilde fra videoen (se VideoFrameDecoder i appen).
 */
@Composable
private fun mediaModel(m: MediaAttachment): Any? {
    val isVideo = m.type == "video" || m.type == "gifv"
    return when {
        m.type == "image" -> m.url
        isVideo -> (m.previewUrl?.takeIf { it.isNotBlank() } ?: m.url.takeIf { it.isNotBlank() })?.let { src ->
            ImageRequest.Builder(LocalContext.current).data(src).videoFrameMillis(1000).build()
        }
        else -> m.previewUrl?.takeIf { it.isNotBlank() }
    }
}

/** Play-knapp for video og ALT-merke for bilder med alt-tekst, oppå selve bildet. */
@Composable
private fun BoxScope.MediaOverlays(m: MediaAttachment) {
    if (m.type != "image") {
        // Play-knapp på mørk, rund bakgrunn så den synes over både lyse og mørke bilder
        Icon(
            Icons.Filled.PlayArrow, stringResource(R.string.attachment_open),
            tint = Color.White,
            modifier = Modifier
                .align(Alignment.Center)
                .size(56.dp)
                .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                .padding(8.dp),
        )
    }
    if (!m.description.isNullOrBlank()) {
        Text(
            stringResource(R.string.alt_badge),
            color = PT.Fg, fontSize = 10.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(4.dp)
                .background(PT.TopBarText)
                .padding(horizontal = 5.dp, vertical = 2.dp),
        )
    }
}

/** Bilder åpnes i appens egen visning (med alt-tekst); video og annet åpnes i en annen app. */
@Composable
private fun rememberMediaOpener(items: List<MediaAttachment>): (MediaAttachment) -> Unit {
    val openViewer = LocalImageViewer.current
    val ctx = LocalContext.current
    val images = remember(items) { items.filter { it.type == "image" } }
    return { m ->
        if (m.type == "image") openViewer(images, images.indexOf(m).coerceAtLeast(0))
        else runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(m.url))) }
    }
}

@Composable
private fun MediaTile(m: MediaAttachment, single: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shown = mediaModel(m)
    val ratio = if (single) m.aspectRatio?.coerceIn(0.4f, 3f) else 1f
    Box(
        modifier
            .background(PT.Fg)
            .border(1.dp, PT.Border)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (shown != null) {
            AsyncImage(
                model = shown,
                contentDescription = m.description,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (ratio != null) Modifier.aspectRatio(ratio) else Modifier.heightIn(min = 120.dp)),
            )
        } else {
            Box(Modifier.fillMaxWidth().height(90.dp))
        }
        MediaOverlays(m)
    }
}

@Composable
private fun PollView(owner: Status, poll: Poll, actions: StatusActions) {
    val showResults = poll.voted || poll.expired
    var selected by remember(poll.id) { mutableStateOf(setOf<Int>()) }
    val total = poll.options.sumOf { it.votesCount ?: 0 }.coerceAtLeast(1)

    Column(Modifier.padding(top = 8.dp)) {
        poll.options.forEachIndexed { i, option ->
            if (showResults) {
                val pct = (option.votesCount ?: 0) * 100 / total
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .border(1.dp, PT.Border.copy(alpha = 0.5f)),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(pct / 100f)
                            .height(34.dp)
                            .background(PT.Green.copy(alpha = 0.5f)),
                    )
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(34.dp)
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(option.title, Modifier.weight(1f), color = PT.Text, maxLines = 1)
                        Text("$pct %", color = PT.Text)
                    }
                }
            } else {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            selected = if (poll.multiple) {
                                if (i in selected) selected - i else selected + i
                            } else setOf(i)
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (poll.multiple) Checkbox(checked = i in selected, onCheckedChange = null)
                    else RadioButton(selected = i in selected, onClick = null)
                    Spacer(Modifier.width(8.dp))
                    Text(option.title, color = PT.Text)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!showResults) {
                PButton(
                    stringResource(R.string.poll_vote),
                    onClick = { actions.onVote(owner, poll, selected.toList()) },
                    enabled = selected.isNotEmpty(),
                )
                Spacer(Modifier.width(10.dp))
            }
            Text(
                stringResource(R.string.poll_votes, poll.votesCount) +
                    if (poll.expired) " · " + stringResource(R.string.poll_closed) else "",
                style = MaterialTheme.typography.bodySmall,
                color = PT.Muted,
            )
        }
    }
}

/** Forhåndsvisning av en lenke: bilde + sidetittel. Hele boksen er en lenke. */
@Composable
private fun LinkCard(card: Card) {
    val ctx = LocalContext.current
    val host = card.providerName?.takeIf { it.isNotBlank() } ?: Uri.parse(card.url).host.orEmpty()
    Column(
        Modifier
            .padding(top = 8.dp)
            .fillMaxWidth()
            .background(PT.Fg)
            .border(1.dp, PT.Border)
            .clickable {
                runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(card.url))) }
            },
    ) {
        if (!card.image.isNullOrBlank()) {
            AsyncImage(
                model = card.image,
                contentDescription = card.title,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 80.dp, max = 240.dp)
                    .background(PT.Bg),
            )
            Box(Modifier.fillMaxWidth().height(1.dp).background(PT.Border.copy(alpha = 0.6f)))
        }
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (host.isNotBlank()) {
                Text(host, style = MaterialTheme.typography.bodySmall, color = PT.Link, maxLines = 1)
            }
            if (card.title.isNotBlank()) {
                Text(
                    card.title, color = PT.Text, fontWeight = FontWeight.Bold, maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
            if (card.description.isNotBlank()) {
                Text(
                    card.description, style = MaterialTheme.typography.bodySmall, color = PT.Muted, maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
        }
    }
}
