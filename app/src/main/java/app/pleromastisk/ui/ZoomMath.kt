package app.pleromastisk.ui

/** Tilstand for zoom/panorering av ett bilde. Forskyvningen er målt fra midten av visningsflaten. */
data class ZoomState(val scale: Float = 1f, val offsetX: Float = 0f, val offsetY: Float = 0f)

const val MaxZoom = 6f

/** Holder bildet innenfor visningsflaten (ingen tomrom utover det zoomingen gir plass til). */
fun clampZoom(state: ZoomState, width: Float, height: Float): ZoomState {
    val maxX = width * (state.scale - 1f) / 2f
    val maxY = height * (state.scale - 1f) / 2f
    return state.copy(
        offsetX = state.offsetX.coerceIn(-maxX, maxX),
        offsetY = state.offsetY.coerceIn(-maxY, maxY),
    )
}

/**
 * Bruker en klyp-/dra-gest: zoomer rundt fingrenes midtpunkt slik at punktet under fingrene blir
 * liggende stille, og flytter bildet med fingrene.
 * [centroidX]/[centroidY] er midtpunktet i visningsflatens koordinater.
 */
fun applyZoomGesture(
    state: ZoomState,
    zoomChange: Float,
    panX: Float,
    panY: Float,
    centroidX: Float,
    centroidY: Float,
    width: Float,
    height: Float,
): ZoomState {
    val newScale = (state.scale * zoomChange).coerceIn(1f, MaxZoom)
    val ratio = newScale / state.scale
    val rx = centroidX - width / 2f
    val ry = centroidY - height / 2f
    val raw = ZoomState(
        scale = newScale,
        offsetX = rx + panX - (rx - state.offsetX) * ratio,
        offsetY = ry + panY - (ry - state.offsetY) * ratio,
    )
    return clampZoom(raw, width, height)
}

/** Dobbelttrykk: tilbake til normal hvis zoomet inn, ellers zoom inn mot trykkpunktet. */
fun toggleZoomAt(state: ZoomState, tapX: Float, tapY: Float, width: Float, height: Float, target: Float = 2.5f): ZoomState =
    if (state.scale > 1.01f) ZoomState()
    else applyZoomGesture(state, target / state.scale, 0f, 0f, tapX, tapY, width, height)
