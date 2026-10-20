package app.pleromastisk.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pleromastisk.R
import app.pleromastisk.data.MediaAttachment
import coil.compose.AsyncImage

/** Lar hvilken som helst skjerm be appen om å åpne bildevisningen (som ligger øverst i appen). */
val LocalImageViewer = staticCompositionLocalOf<(List<MediaAttachment>, Int) -> Unit> { { _, _ -> } }

/**
 * Bildevisning som dekker hele skjermen: sveip mellom bildene, klyp eller dobbelttrykk for å zoome,
 * og alt-teksten ligger som en fast del under bildet. Tegnes inne i appens eget vindu (ikke en
 * dialog), slik at status- og navigasjonsfeltet tas hensyn til og alt-teksten aldri skjules.
 */
@Composable
fun ImageViewer(images: List<MediaAttachment>, startIndex: Int, onDismiss: () -> Unit) {
    if (images.isEmpty()) return
    BackHandler(onBack = onDismiss)
    val pager = rememberPagerState(initialPage = startIndex.coerceIn(0, images.lastIndex)) { images.size }
    val context = LocalContext.current
    val current = images[pager.currentPage.coerceIn(0, images.lastIndex)]

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xF5000000))
            // Hindrer at trykk går gjennom til innholdet bak
            .pointerInput(Unit) { detectTapGestures { } },
    ) {
        Column(Modifier.fillMaxWidth().background(PT.TopBar).statusBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, stringResource(R.string.viewer_close), tint = PT.TopBarText)
                }
                if (images.size > 1) {
                    Text(
                        "${pager.currentPage + 1} / ${images.size}",
                        color = PT.TopBarText, fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.weight(1f))
                TextButton(
                    onClick = {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(current.url)))
                        }
                    },
                ) { Text(stringResource(R.string.viewer_open_original), color = PT.Link) }
            }
            HorizontalDivider(color = PT.Border)
        }

        HorizontalPager(
            state = pager,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) { page -> ZoomableImage(images[page]) }

        AltPanel(current.description)
    }
}

@Composable
private fun ZoomableImage(m: MediaAttachment) {
    var zoom by remember { mutableStateOf(ZoomState()) }
    var box by remember { mutableStateOf(IntSize.Zero) }

    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { box = it }
            .clipToBounds()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { tap ->
                        zoom = toggleZoomAt(zoom, tap.x, tap.y, box.width.toFloat(), box.height.toFloat())
                    },
                )
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.count { it.pressed }
                        // Med to fingre, eller når bildet allerede er zoomet inn, håndterer vi gesten selv.
                        // Ett finger på uzoomet bilde slippes videre, slik at sidene kan sveipes.
                        if (pressed >= 2 || zoom.scale > 1.01f) {
                            val centroid = event.calculateCentroid(useCurrent = true)
                            if (centroid.isSpecified) {
                                zoom = applyZoomGesture(
                                    zoom,
                                    event.calculateZoom(),
                                    event.calculatePan().x, event.calculatePan().y,
                                    centroid.x, centroid.y,
                                    box.width.toFloat(), box.height.toFloat(),
                                )
                            }
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
            .graphicsLayer {
                scaleX = zoom.scale
                scaleY = zoom.scale
                translationX = zoom.offsetX
                translationY = zoom.offsetY
            },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = m.url,
            contentDescription = m.description,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun AltPanel(description: String?) {
    val has = !description.isNullOrBlank()
    Column(Modifier.fillMaxWidth().background(PT.Fg).navigationBarsPadding()) {
        HorizontalDivider(color = PT.Border)
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 200.dp)
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(stringResource(R.string.alt_badge), color = PT.Link, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(
                if (has) description.orEmpty() else stringResource(R.string.alt_none),
                color = if (has) PT.Text else PT.Muted,
                fontSize = 16.sp,
            )
        }
    }
}
