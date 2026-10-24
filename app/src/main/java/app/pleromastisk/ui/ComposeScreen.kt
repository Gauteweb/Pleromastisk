package app.pleromastisk.ui

import android.net.Uri
import android.text.Html
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pleromastisk.Graph
import app.pleromastisk.R
import app.pleromastisk.data.Emoji
import app.pleromastisk.userMessage
import coil.compose.AsyncImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Et vedlegg i innlegget – enten nylig lastet opp fra telefonen eller eksisterende (ved redigering). */
private data class Attachment(
    val key: String,
    val uri: Uri?,
    val previewUrl: String?,
    val id: String?,
    val description: String = "",
)

private val Visibilities = listOf("public", "unlisted", "private", "direct")

/** Stor forbokstav i starten av hver setning og hver ny linje (tastaturet tar seg av det). */
private val SentenceCase = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)

@Composable
private fun languageName(code: String): String = stringResource(
    when (code) {
        "nn" -> R.string.lang_nn
        "en" -> R.string.lang_en
        else -> R.string.lang_nb
    },
)

@Composable
private fun visibilityLabel(v: String): String = stringResource(
    when (v) {
        "unlisted" -> R.string.vis_unlisted
        "private" -> R.string.vis_private
        "direct" -> R.string.vis_direct
        else -> R.string.vis_public
    },
)

@Composable
fun ComposeScreen(
    replyId: String?,
    replyMentions: String?,
    replyVisibility: String?,
    editId: String?,
    maxChars: Int,
    altLimit: Int,
    postFormats: List<String>,
    onClose: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val focus = remember { FocusRequester() }
    val editing = editId != null

    val initial = replyMentions?.split(" ")?.filter { it.isNotBlank() }
        ?.joinToString(" ") { "@$it" }?.plus(" ").orEmpty()
    var text by remember { mutableStateOf(TextFieldValue(initial, TextRange(initial.length))) }
    var cwOn by rememberSaveable { mutableStateOf(false) }
    var cw by rememberSaveable { mutableStateOf("") }
    var visibility by rememberSaveable { mutableStateOf(replyVisibility ?: "public") }
    var markdown by rememberSaveable { mutableStateOf(false) }
    // Språk: nye innlegg starter med sist brukte valg. Ved redigering brukes innleggets eget språk
    // (null hvis det er et annet språk enn de tre vi tilbyr – da endres det ikke).
    var language by rememberSaveable { mutableStateOf<String?>(if (editId != null) null else Graph.store.language) }
    var originalLanguage by remember { mutableStateOf<String?>(null) }
    var langMenu by remember { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }
    var loadingEdit by remember { mutableStateOf(editing) }
    var visMenu by remember { mutableStateOf(false) }
    var emojiOpen by rememberSaveable { mutableStateOf(false) }
    var altTarget by remember { mutableStateOf<Attachment?>(null) }
    var altSaving by remember { mutableIntStateOf(0) }
    val keyboard = LocalSoftwareKeyboardController.current
    val attachments = remember { mutableStateListOf<Attachment>() }

    val markdownSupported = "text/markdown" in postFormats
    val length = text.text.codePointCount(0, text.text.length)
    val over = length > maxChars
    val uploading = attachments.any { it.id == null }
    val canSend = !sending && !loadingEdit && !over && !uploading && altSaving == 0 &&
        (text.text.isNotBlank() || attachments.isNotEmpty())

    // Redigering: hent innleggets kildetekst og eksisterende vedlegg.
    LaunchedEffect(editId) {
        if (editId == null) return@LaunchedEffect
        try {
            val st = Graph.api.status(editId)
            val src = runCatching { Graph.api.statusSource(editId) }.getOrNull()
            val body = src?.text
                ?: Html.fromHtml(st.content, Html.FROM_HTML_MODE_LEGACY).toString().trim()
            text = TextFieldValue(body, TextRange(body.length))
            cw = src?.spoilerText ?: st.spoilerText
            cwOn = cw.isNotBlank()
            visibility = st.visibility
            markdown = src?.contentType == "text/markdown"
            originalLanguage = normalizePostLanguage(st.language)
            language = originalLanguage
            attachments.clear()
            st.mediaAttachments.forEach { m ->
                attachments.add(
                    Attachment(
                        key = m.id, uri = null, previewUrl = m.previewUrl ?: m.url,
                        id = m.id, description = m.description.orEmpty(),
                    ),
                )
            }
            loadingEdit = false
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Graph.toast(e.userMessage())
            onClose()
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(4)) { uris ->
        uris.forEach { uri ->
            if (attachments.size >= 4) return@forEach
            val key = uri.toString()
            attachments.add(Attachment(key, uri, null, null))
            scope.launch {
                try {
                    val up = Graph.api.uploadMedia(context.contentResolver, uri)
                    val i = attachments.indexOfFirst { it.key == key }
                    if (i >= 0) attachments[i] = attachments[i].copy(id = up.id)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    attachments.removeAll { it.key == key }
                    Graph.toast(Graph.str(R.string.compose_upload_failed) + ": " + e.userMessage())
                }
            }
        }
    }

    LaunchedEffect(Unit) { if (!editing) focus.requestFocus() }

    /** Setter inn :kortkode: der markøren står, med mellomrom rundt slik at instansen gjenkjenner den. */
    fun insertEmoji(e: Emoji) {
        val t = text.text
        val start = text.selection.min.coerceIn(0, t.length)
        val end = text.selection.max.coerceIn(0, t.length)
        val before = if (start > 0 && !t[start - 1].isWhitespace()) " " else ""
        val insert = "$before:${e.shortcode}: "
        text = TextFieldValue(
            t.replaceRange(start, end, insert),
            TextRange(start + insert.length),
        )
    }

    fun saveAlt(target: Attachment, description: String) {
        val i = attachments.indexOfFirst { it.key == target.key }
        if (i >= 0) attachments[i] = attachments[i].copy(description = description)
        val id = target.id ?: return
        scope.launch {
            altSaving++
            try {
                Graph.api.updateMedia(id, description)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Graph.toast(Graph.str(R.string.alt_update_failed) + ": " + e.userMessage())
            } finally {
                altSaving--
            }
        }
    }

    fun send() {
        scope.launch {
            sending = true
            try {
                val contentType = if (postFormats.isEmpty()) null
                else if (markdown && markdownSupported) "text/markdown" else "text/plain"
                val mediaIds = attachments.mapNotNull { it.id }
                if (editId != null) {
                    Graph.api.editStatus(
                        id = editId,
                        text = text.text,
                        spoiler = if (cwOn) cw else null,
                        mediaIds = mediaIds,
                        contentType = contentType,
                        // Språk sendes bare hvis brukeren har endret det
                        language = language?.takeIf { it != originalLanguage },
                    )
                    Graph.toast(Graph.str(R.string.compose_updated))
                } else {
                    Graph.api.postStatus(
                        text = text.text,
                        visibility = visibility,
                        spoiler = if (cwOn) cw else null,
                        replyTo = replyId,
                        mediaIds = mediaIds,
                        contentType = contentType,
                        language = language,
                    )
                    language?.let { Graph.store.language = it }
                    Graph.toast(Graph.str(R.string.compose_published))
                }
                Graph.posted.tryEmit(Unit)
                onClose()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Graph.toast(e.userMessage())
            } finally {
                sending = false
            }
        }
    }

    altTarget?.let { target ->
        AltTextDialog(
            attachment = target,
            limit = altLimit,
            onSave = { desc -> altTarget = null; saveAlt(target, desc) },
            onDismiss = { altTarget = null },
        )
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        // Topp: tittel
        Column(Modifier.fillMaxWidth().background(PT.TopBar).statusBarsPadding()) {
            Text(
                stringResource(
                    when {
                        editing -> R.string.compose_edit_title
                        replyId != null -> R.string.compose_reply_title
                        else -> R.string.compose_title
                    },
                ),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                color = PT.TopBarText, fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
            )
            HorizontalDivider(color = PT.Border)
        }

        // Midten: tekstfelt
        if (loadingEdit) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PT.Link, strokeWidth = 2.dp, modifier = Modifier.size(32.dp))
            }
        } else {
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (cwOn) {
                    OutlinedTextField(
                        value = cw, onValueChange = { cw = it },
                        label = { Text(stringResource(R.string.compose_cw_label)) },
                        singleLine = true, shape = RectangleShape, colors = ptFieldColors(),
                        keyboardOptions = SentenceCase,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                OutlinedTextField(
                    value = text, onValueChange = { text = it },
                    placeholder = { Text(stringResource(R.string.compose_hint)) },
                    // Temaet bruker monospace i inndatafelt
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace),
                    keyboardOptions = SentenceCase,
                    shape = RectangleShape, colors = ptFieldColors(),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp).focusRequester(focus),
                )
                Text(
                    stringResource(R.string.char_counter, length, maxChars),
                    // Boks i samme stil som «Skriv»-knappen, så telleren synes uansett bakgrunnsbilde
                    color = PT.Fg,
                    fontWeight = if (over) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier
                        .align(Alignment.End)
                        .background(if (over) PT.Orange else PT.Link)
                        .border(1.dp, PT.Border)
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                )
                if (attachments.isNotEmpty()) {
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        attachments.forEach { a ->
                            Box(
                                Modifier
                                    .size(96.dp)
                                    .border(1.dp, PT.Border)
                                    .background(PT.Fg)
                                    // Trykk på bildet for å legge inn alt-tekst
                                    .clickable(enabled = a.id != null) { altTarget = a },
                            ) {
                                AsyncImage(
                                    model = a.uri ?: a.previewUrl, contentDescription = a.description,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                                if (a.id == null) {
                                    CircularProgressIndicator(
                                        Modifier.size(24.dp).align(Alignment.Center),
                                        color = PT.Link, strokeWidth = 2.dp,
                                    )
                                }
                                // ALT-merke: grønt når alt-tekst er satt, oransje «+ ALT» når den mangler
                                val hasAlt = a.description.isNotBlank()
                                Text(
                                    stringResource(if (hasAlt) R.string.alt_badge else R.string.alt_add_badge),
                                    color = if (hasAlt) PT.Fg else PT.Orange,
                                    fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .background(if (hasAlt) PT.TopBarText else PT.Fg.copy(alpha = 0.85f))
                                        .padding(horizontal = 6.dp, vertical = 3.dp),
                                )
                                Icon(
                                    Icons.Filled.Close, stringResource(R.string.compose_remove_attachment), tint = PT.Text,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .background(PT.Fg.copy(alpha = 0.8f))
                                        .clickable { attachments.remove(a) }
                                        .padding(4.dp)
                                        .size(18.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        if (emojiOpen) EmojiPicker(onPick = ::insertEmoji)

        // Bunn: verktøy + knapper
        Column(Modifier.fillMaxWidth().background(PT.Fg).navigationBarsPadding()) {
            HorizontalDivider(color = PT.Border)
            Row(
                // Rullbar som sikkerhet på smale skjermer
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                PButton(
                    text = "", icon = Icons.Filled.Image,
                    description = stringResource(R.string.compose_attach),
                    onClick = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                    },
                    enabled = attachments.size < 4 && !loadingEdit,
                )
                PButton(
                    text = "", icon = Icons.Filled.Warning,
                    description = stringResource(R.string.compose_cw_toggle),
                    onClick = { cwOn = !cwOn }, filled = cwOn,
                )
                PButton(
                    text = "", icon = Icons.Filled.EmojiEmotions,
                    description = stringResource(R.string.compose_emoji),
                    onClick = {
                        emojiOpen = !emojiOpen
                        if (emojiOpen) keyboard?.hide()
                    },
                    filled = emojiOpen,
                )
                Box {
                    PButton(
                        text = language?.uppercase() ?: "–",
                        icon = Icons.Filled.Translate,
                        description = stringResource(R.string.language_label) +
                            (language?.let { ": " + languageName(it) } ?: ""),
                        onClick = { langMenu = true },
                        enabled = !loadingEdit,
                    )
                    DropdownMenu(expanded = langMenu, onDismissRequest = { langMenu = false }) {
                        PostLanguages.forEach { code ->
                            val selected = code == language
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        languageName(code) + " ($code)",
                                        color = if (selected) PT.TopBarText else PT.FgText,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    )
                                },
                                onClick = { language = code; langMenu = false },
                            )
                        }
                    }
                }
                Box {
                    // Synlighet kan ikke endres når man redigerer et eksisterende innlegg
                    // Bare ikon (uten tekst) – valgene med navn vises i menyen når du trykker
                    PButton(
                        text = "", icon = visibilityIcon(visibility),
                        description = stringResource(R.string.visibility) + ": " + visibilityLabel(visibility),
                        onClick = { visMenu = true },
                        enabled = !editing,
                    )
                    DropdownMenu(expanded = visMenu, onDismissRequest = { visMenu = false }) {
                        Visibilities.forEach { v ->
                            DropdownMenuItem(
                                text = { Text(visibilityLabel(v)) },
                                leadingIcon = { Icon(visibilityIcon(v), null) },
                                onClick = { visibility = v; visMenu = false },
                            )
                        }
                    }
                }
                if (markdownSupported) {
                    PButton(
                        text = stringResource(R.string.compose_markdown),
                        onClick = { markdown = !markdown }, filled = markdown,
                    )
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PButton(stringResource(R.string.cancel), onClose, Modifier.weight(1f))
                PButton(
                    stringResource(if (editing) R.string.compose_update else R.string.compose_publish),
                    { send() }, Modifier.weight(1f),
                    enabled = canSend, filled = true,
                )
            }
        }
    }
}

/** Dialog for alt-tekst: forhåndsvisning av bildet, tekstfelt og teller mot instansens grense. */
@Composable
private fun AltTextDialog(
    attachment: Attachment,
    limit: Int,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember(attachment.key) { mutableStateOf(attachment.description) }
    val over = value.length > limit
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.border(1.dp, PT.Border),
        containerColor = PT.Fg,
        title = {
            Text(stringResource(R.string.alt_title), color = PT.TopBarText, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AsyncImage(
                    model = attachment.uri ?: attachment.previewUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 180.dp)
                        .background(PT.Bg)
                        .border(1.dp, PT.Border),
                )
                OutlinedTextField(
                    value = value, onValueChange = { value = it },
                    placeholder = { Text(stringResource(R.string.alt_hint)) },
                    minLines = 3, maxLines = 8,
                    keyboardOptions = SentenceCase,
                    shape = RectangleShape, colors = ptFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    stringResource(R.string.char_counter, value.length, limit),
                    color = if (over) PT.Orange else PT.Muted,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.End),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(value.trim()) }, enabled = !over) {
                Text(stringResource(R.string.alt_save), color = PT.Link)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = PT.Link) }
        },
    )
}
