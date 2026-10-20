package app.pleromastisk.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.pleromastisk.R
import app.pleromastisk.data.Account
import app.pleromastisk.vm.SearchViewModel

/** Søk etter brukere (@) og emneknagger (#). Vanlig tekst søker etter begge deler. */
@Composable
fun SearchScreen(
    vm: SearchViewModel,
    onOpenAccount: (Account) -> Unit,
    onOpenTag: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) { if (state.query.isBlank()) focus.requestFocus() }

    Column(modifier.fillMaxSize().imePadding()) {
        Column(Modifier.fillMaxWidth().background(PT.Fg.copy(alpha = 0.92f))) {
            OutlinedTextField(
                value = state.query,
                onValueChange = { vm.onQueryChange(it) },
                singleLine = true,
                placeholder = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = { Icon(Icons.Filled.Search, null, tint = PT.Link) },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { vm.clear() }) {
                            Icon(Icons.Filled.Close, stringResource(R.string.search_clear), tint = PT.Link)
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { vm.submit(); keyboard?.hide() }),
                shape = RectangleShape,
                colors = ptFieldColors(),
                modifier = Modifier.fillMaxWidth().padding(8.dp).focusRequester(focus),
            )
            HorizontalDivider(color = PT.Border)
        }

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val nothing = state.accounts.isEmpty() && state.hashtags.isEmpty()
            when {
                state.query.isBlank() -> item {
                    Panel { Text(stringResource(R.string.search_start), Modifier.padding(14.dp), color = PT.Text) }
                }
                state.error != null -> item { ErrorPanel(state.error ?: "", onRetry = { vm.submit() }) }
                state.loading -> item { LoadingRow() }
                state.searched && nothing -> item {
                    Panel { Text(stringResource(R.string.search_empty), Modifier.padding(14.dp), color = PT.Text) }
                }
            }

            if (state.accounts.isNotEmpty()) {
                item(key = "h_users") { SectionHeader(stringResource(R.string.search_users)) }
                items(state.accounts, key = { "a_" + it.id }) { acc ->
                    Panel(Modifier.clickable { onOpenAccount(acc) }) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Avatar(acc.avatar, 44)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                EmojiText(
                                    acc.name, acc.emojis,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = PT.Text, fontWeight = FontWeight.Bold, maxLines = 1,
                                )
                                Text(
                                    "@" + acc.acct, style = MaterialTheme.typography.bodySmall,
                                    color = PT.Muted, maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }

            if (state.hashtags.isNotEmpty()) {
                item(key = "h_tags") { SectionHeader(stringResource(R.string.search_tags)) }
                items(state.hashtags, key = { "t_$it" }) { tag ->
                    Panel(Modifier.clickable { onOpenTag(tag) }) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("#", color = PT.Link, fontWeight = FontWeight.Bold)
                            Text(tag, color = PT.Text, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        Modifier.padding(start = 4.dp, top = 6.dp),
        color = PT.TopBarText, fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.bodySmall,
    )
}
