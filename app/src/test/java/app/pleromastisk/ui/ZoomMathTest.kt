package app.pleromastisk.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ZoomMathTest {
    private val w = 1000f
    private val h = 1000f
    private val eps = 0.001f

    @Test fun zoomIMidtenenGirIngenForskyvning() {
        val s = applyZoomGesture(ZoomState(), 2f, 0f, 0f, 500f, 500f, w, h)
        assertEquals(2f, s.scale, eps); assertEquals(0f, s.offsetX, eps); assertEquals(0f, s.offsetY, eps)
    }

    @Test fun punktetUnderFingreneBlirLiggendeStille() {
        // Fingrene er ved (700, 500). Bildepunktet under dem skal fortsatt ligge på (700, 500) etter 2x zoom.
        val s = applyZoomGesture(ZoomState(), 2f, 0f, 0f, 700f, 500f, w, h)
        val before = w / 2 + 0f + (700f - w / 2 - 0f) / 1f * 1f
        val imagePointFromCenter = (700f - w / 2 - 0f) / 1f
        val after = w / 2 + s.offsetX + imagePointFromCenter * s.scale
        assertEquals(before, after, eps)
        assertEquals(700f, after, eps)
    }

    @Test fun skalaBegrensesTilMinOgMaks() {
        assertEquals(MaxZoom, applyZoomGesture(ZoomState(), 100f, 0f, 0f, 500f, 500f, w, h).scale, eps)
        assertEquals(1f, applyZoomGesture(ZoomState(2f), 0.01f, 0f, 0f, 500f, 500f, w, h).scale, eps)
    }

    @Test fun ingenPanoreringNårIkkeZoomet() {
        val s = applyZoomGesture(ZoomState(), 1f, 300f, -200f, 500f, 500f, w, h)
        assertEquals(0f, s.offsetX, eps); assertEquals(0f, s.offsetY, eps)
    }

    @Test fun panoreringBegrensesTilBildekanten() {
        val s = applyZoomGesture(ZoomState(2f), 1f, 9999f, -9999f, 500f, 500f, w, h)
        assertEquals(500f, s.offsetX, eps)   // (1000 * (2 - 1)) / 2
        assertEquals(-500f, s.offsetY, eps)
    }

    @Test fun zoomUtTilNormalNullstillerForskyvning() {
        val zoomed = applyZoomGesture(ZoomState(), 3f, 0f, 0f, 800f, 200f, w, h)
        assertTrue(zoomed.offsetX != 0f)
        val back = applyZoomGesture(zoomed, 0.0001f, 0f, 0f, 800f, 200f, w, h)
        assertEquals(1f, back.scale, eps); assertEquals(0f, back.offsetX, eps); assertEquals(0f, back.offsetY, eps)
    }

    @Test fun dobbelttrykkZoomerInnOgTilbake() {
        val inn = toggleZoomAt(ZoomState(), 700f, 500f, w, h)
        assertEquals(2.5f, inn.scale, eps)
        assertEquals(ZoomState(), toggleZoomAt(inn, 700f, 500f, w, h))
    }
}
