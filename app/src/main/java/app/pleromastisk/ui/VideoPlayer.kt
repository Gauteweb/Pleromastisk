package app.pleromastisk.ui

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import app.pleromastisk.R
import app.pleromastisk.data.MediaAttachment
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

/**
 * Video rett i feeden. «gifv» (GIF-er som Mastodon har gjort om til video) spilles av med en gang,
 * uten lyd og i loop. Vanlig video viser forhåndsbildet med play-knapp og starter ved trykk; trykk
 * igjen for pause. Spilleren lages først når videoen starter, og frigjøres når den forsvinner fra
 * skjermen eller appen legges i bakgrunnen (pause).
 *
 * [crop]: fyll hele feltet og beskjær (som bildene i «Bilder»), ellers vis hele videoen.
 * [posterModifier] bestemmer størrelsen på forhåndsbildet, og dermed hele feltet.
 * [onPosterRatio] får bredde/høyde-forholdet til forhåndsbildet når det er lastet.
 */
@Composable
fun InlineVideo(
    m: MediaAttachment,
    poster: Any?,
    crop: Boolean,
    modifier: Modifier = Modifier,
    posterModifier: Modifier = Modifier,
    onPosterRatio: (Float) -> Unit = {},
) {
    val loop = m.type == "gifv"
    var started by rememberSaveable(m.id) { mutableStateOf(loop) }

    Box(modifier.clipToBounds(), contentAlignment = Alignment.Center) {
        if (poster != null) {
            AsyncImage(
                model = poster,
                contentDescription = m.description,
                contentScale = if (crop) ContentScale.Crop else ContentScale.Fit,
                modifier = posterModifier,
                onSuccess = { state ->
                    val size = state.painter.intrinsicSize
                    if (size.width > 0f && size.height > 0f) onPosterRatio(size.width / size.height)
                },
            )
        } else {
            Box(posterModifier.background(Color.Black))
        }
        if (started) {
            VideoSurface(m.url, loop, crop, Modifier.matchParentSize())
        } else {
            Box(
                Modifier.matchParentSize().clickable { started = true },
                contentAlignment = Alignment.Center,
            ) { PlayIcon() }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun VideoSurface(url: String, loop: Boolean, crop: Boolean, modifier: Modifier) {
    val ctx = LocalContext.current
    val player = remember(url) {
        ExoPlayer.Builder(ctx).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            repeatMode = if (loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            volume = if (loop) 0f else 1f
            playWhenReady = true
            prepare()
        }
    }
    var playing by remember { mutableStateOf(true) }
    var muted by remember { mutableStateOf(loop) }
    var progress by remember { mutableFloatStateOf(0f) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playing = isPlaying || player.playWhenReady && player.playbackState == Player.STATE_BUFFERING
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    // Pause når appen legges i bakgrunnen
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, player) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) player.pause()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    if (!loop) {
        LaunchedEffect(player) {
            while (true) {
                val duration = player.duration
                progress = if (duration > 0) (player.currentPosition.toFloat() / duration).coerceIn(0f, 1f) else 0f
                delay(250)
            }
        }
    }

    Box(modifier) {
        AndroidView(
            factory = {
                PlayerView(it).apply {
                    useController = false
                    isClickable = false
                    isFocusable = false
                    setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                    resizeMode = if (crop) AspectRatioFrameLayout.RESIZE_MODE_ZOOM else AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            update = { it.player = player },
            onRelease = { it.player = null },
            modifier = Modifier.matchParentSize(),
        )
        // Trykkflate over videoen (Compose tar imot trykket, så rulling i feeden virker som vanlig)
        Box(
            Modifier
                .matchParentSize()
                .clickable {
                    when {
                        player.isPlaying -> player.pause()
                        player.playbackState == Player.STATE_ENDED -> { player.seekTo(0); player.play() }
                        else -> player.play()
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            if (!playing) PlayIcon()
            if (!loop) {
                Icon(
                    if (muted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                    stringResource(if (muted) R.string.video_unmute else R.string.video_mute),
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .size(36.dp)
                        .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                        .clickable {
                            muted = !muted
                            player.volume = if (muted) 0f else 1f
                        }
                        .padding(7.dp),
                )
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(progress)
                        .height(3.dp)
                        .background(Color.White.copy(alpha = 0.8f)),
                )
            }
        }
    }
}

@Composable
private fun PlayIcon() {
    // Play-knapp på mørk, rund bakgrunn så den synes over både lyse og mørke bilder
    Icon(
        Icons.Filled.PlayArrow, stringResource(R.string.video_play),
        tint = Color.White,
        modifier = Modifier
            .size(56.dp)
            .background(Color.Black.copy(alpha = 0.55f), CircleShape)
            .padding(8.dp),
    )
}
