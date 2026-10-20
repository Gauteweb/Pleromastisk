package app.pleromastisk.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.pleromastisk.Graph
import app.pleromastisk.R
import app.pleromastisk.data.Emoji
import app.pleromastisk.userMessage
import coil.compose.AsyncImage
import kotlinx.coroutines.CancellationException

/** Velger for instansens egendefinerte emojier, gruppert etter kategori. */
@Composable
fun EmojiPicker(onPick: (Emoji) -> Unit, modifier: Modifier = Modifier) {
    var emojis by remember { mutableStateOf(Graph.cachedEmojis()) }
    var error by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        if (emojis == null) {
            try {
                emojis = Graph.loadEmojis()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.userMessage()
            }
        }
    }

    Column(modifier.fillMaxWidth().height(270.dp).background(PT.Fg)) {
        HorizontalDivider(color = PT.Border)
        OutlinedTextField(
            value = query, onValueChange = { query = it },
            placeholder = { Text(stringResource(R.string.emoji_search)) },
            singleLine = true, shape = RectangleShape, colors = ptFieldColors(),
            modifier = Modifier.fillMaxWidth().padding(8.dp),
        )
        val list = emojis
        when {
            error != null -> Text(error ?: "", Modifier.padding(12.dp), color = PT.Orange)
            list == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PT.Link, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
            }
            else -> {
                val shown = remember(list, query) {
                    val q = query.trim().lowercase()
                    if (q.isEmpty()) list else list.filter { it.shortcode.lowercase().contains(q) }
                }
                if (shown.isEmpty()) {
                    Text(stringResource(R.string.emoji_none), Modifier.padding(12.dp), color = PT.Muted)
                } else {
                    val groups = remember(shown) { shown.groupBy { it.category.orEmpty() } }
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(46.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        groups.forEach { (category, group) ->
                            val title = category.removePrefix("pack:")
                            if (title.isNotBlank()) {
                                item(span = { GridItemSpan(maxLineSpan) }, key = "h_$category") {
                                    Text(
                                        title,
                                        Modifier.padding(top = 8.dp, bottom = 4.dp),
                                        color = PT.Link, fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                            items(group, key = { "e_${category}_${it.shortcode}" }) { e ->
                                Box(
                                    Modifier.size(46.dp).clickable { onPick(e) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    AsyncImage(
                                        model = e.staticUrl.ifBlank { e.url },
                                        contentDescription = ":${e.shortcode}:",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.size(34.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
