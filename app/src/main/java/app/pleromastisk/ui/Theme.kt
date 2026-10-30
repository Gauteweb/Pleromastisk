package app.pleromastisk.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Shapes
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.pleromastisk.R

/** Fargene i ett tema. Alle temaene har samme stil: skarpe hjørner og tynn kantlinje i stedet for skygger. */
@Immutable
data class PtColors(
    val bg: Color,
    val text: Color,
    val link: Color,
    val fg: Color,
    val fgText: Color,
    val topBar: Color,
    val topBarText: Color,
    val border: Color,
    val red: Color,
    val blue: Color,
    val green: Color,
    val orange: Color,
    val pressedBg: Color,
    val pressedBorder: Color,
    /** Slør over bakgrunnsbildet, så teksten alltid er lesbar. */
    val overlay: Color,
    /** Lyst tema: mørke ikoner i statuslinjen og Materials lyse fargeskjema. */
    val light: Boolean = false,
    /** Aktivt menypunkt i bunnmenyen. Lysere enn overskriftsfargen der den ligner for mye på de andre. */
    val navActive: Color = topBarText,
)

/** Temaene brukeren kan velge mellom på profilsiden. [id] lagres, så den må ikke endres. */
enum class AppTheme(val id: String, @StringRes val label: Int, val colors: PtColors) {
    /** Pleroma-temaet «gaute.space»: mørke blågrønne flater og tan kantlinje (#AB9977). */
    GREEN(
        "standard", R.string.theme_green,
        PtColors(
            bg = Color(0xFF203237), text = Color(0xFFC2D9D4), link = Color(0xFFAB9977),
            fg = Color(0xFF132B29), fgText = Color(0xFFB5D4CE),
            topBar = Color(0xFF132B29), topBarText = Color(0xFF65C89C), border = Color(0xFFAB9977),
            red = Color(0xFF9E555B), blue = Color(0xFF81A6C4), green = Color(0xFF319195), orange = Color(0xFFD6924C),
            pressedBg = Color(0xFF443D30), pressedBorder = Color(0xFF7D7057), overlay = Color(0x33102022),
        ),
    ),
    BLUE(
        "blue", R.string.theme_blue,
        PtColors(
            bg = Color(0xFF1E2B3D), text = Color(0xFFC9D6E8), link = Color(0xFF8FB8DE),
            fg = Color(0xFF121C2B), fgText = Color(0xFFBCCDE3),
            topBar = Color(0xFF121C2B), topBarText = Color(0xFF7FC4F0), border = Color(0xFF8FB8DE),
            red = Color(0xFFB05C66), blue = Color(0xFF81A6C4), green = Color(0xFF4FA3A8), orange = Color(0xFFD6924C),
            pressedBg = Color(0xFF2A3A52), pressedBorder = Color(0xFF5C7A9E), overlay = Color(0x55101A2C),
            navActive = Color(0xFFD6EEFF),
        ),
    ),
    PURPLE(
        "purple", R.string.theme_purple,
        PtColors(
            bg = Color(0xFF2A2238), text = Color(0xFFDCD0EA), link = Color(0xFFB9A0E0),
            fg = Color(0xFF1C1626), fgText = Color(0xFFCFC2E0),
            topBar = Color(0xFF1C1626), topBarText = Color(0xFFD3A6F2), border = Color(0xFFB9A0E0),
            red = Color(0xFFB05C70), blue = Color(0xFF8FA6D6), green = Color(0xFF5FA39A), orange = Color(0xFFD6924C),
            pressedBg = Color(0xFF3A2E50), pressedBorder = Color(0xFF7E68A3), overlay = Color(0x5520122C),
            navActive = Color(0xFFF2E2FF),
        ),
    ),
    DARK(
        "dark", R.string.theme_dark,
        PtColors(
            bg = Color(0xFF1A1A1A), text = Color(0xFFD6D6D6), link = Color(0xFFAB9977),
            fg = Color(0xFF101010), fgText = Color(0xFFC8C8C8),
            topBar = Color(0xFF101010), topBarText = Color(0xFFD9C9A8), border = Color(0xFFAB9977),
            red = Color(0xFF9E555B), blue = Color(0xFF81A6C4), green = Color(0xFF319195), orange = Color(0xFFD6924C),
            pressedBg = Color(0xFF3A352B), pressedBorder = Color(0xFF7D7057), overlay = Color(0x88000000),
            navActive = Color(0xFFFFF6E3),
        ),
    ),
    LIGHT(
        "light", R.string.theme_light,
        PtColors(
            bg = Color(0xFFE8EEEC), text = Color(0xFF1F2E2C), link = Color(0xFF7D6232),
            fg = Color(0xFFF8FAF9), fgText = Color(0xFF2A3B38),
            topBar = Color(0xFFF8FAF9), topBarText = Color(0xFF1F7A5A), border = Color(0xFFA08A62),
            red = Color(0xFFB0444C), blue = Color(0xFF3F6E96), green = Color(0xFF23787C), orange = Color(0xFFB86E25),
            pressedBg = Color(0xFFE6DCC6), pressedBorder = Color(0xFF7D6232), overlay = Color(0xAAE8EEEC),
            light = true,
        ),
    ),
    ;

    companion object {
        fun fromId(id: String?): AppTheme = entries.firstOrNull { it.id == id } ?: DARK
    }
}

/**
 * Fargene i det valgte temaet. [theme] er Compose-tilstand, så alt som leser fargene tegnes på nytt
 * med en gang brukeren bytter tema.
 */
object PT {
    var theme by mutableStateOf(AppTheme.DARK)

    private val c: PtColors get() = theme.colors

    val Bg: Color get() = c.bg
    val Text: Color get() = c.text
    val Link: Color get() = c.link
    val Fg: Color get() = c.fg
    val FgText: Color get() = c.fgText
    val TopBar: Color get() = c.topBar
    val TopBarText: Color get() = c.topBarText
    val Border: Color get() = c.border
    val Red: Color get() = c.red
    val Blue: Color get() = c.blue
    val Green: Color get() = c.green
    val Orange: Color get() = c.orange
    val PressedBg: Color get() = c.pressedBg
    val PressedBorder: Color get() = c.pressedBorder
    val Overlay: Color get() = c.overlay
    val NavActive: Color get() = c.navActive

    const val PanelAlpha = 0.8f
    val Muted: Color get() = FgText.copy(alpha = 0.65f)
}

private fun schemeFor(c: PtColors) = if (c.light) {
    lightColorScheme(
        primary = c.link, onPrimary = c.fg, secondary = c.topBarText, onSecondary = c.fg, tertiary = c.blue,
        background = c.bg, onBackground = c.text, surface = c.fg, onSurface = c.fgText,
        surfaceVariant = c.fg, onSurfaceVariant = c.fgText, surfaceContainer = c.fg, surfaceContainerHigh = c.fg,
        surfaceContainerHighest = c.fg, surfaceContainerLow = c.fg, surfaceContainerLowest = c.fg,
        outline = c.border, outlineVariant = c.border, error = c.red, onError = c.fg,
    )
} else {
    darkColorScheme(
        primary = c.link, onPrimary = c.fg, secondary = c.topBarText, onSecondary = c.fg, tertiary = c.blue,
        background = c.bg, onBackground = c.text, surface = c.fg, onSurface = c.fgText,
        surfaceVariant = c.fg, onSurfaceVariant = c.fgText, surfaceContainer = c.fg, surfaceContainerHigh = c.fg,
        surfaceContainerHighest = c.fg, surfaceContainerLow = c.fg, surfaceContainerLowest = c.fg,
        outline = c.border, outlineVariant = c.border, error = c.red, onError = c.text,
    )
}

private val SharpShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(0.dp),
    medium = RoundedCornerShape(0.dp),
    large = RoundedCornerShape(0.dp),
    extraLarge = RoundedCornerShape(0.dp),
)

@Composable
fun PleromastiskTheme(content: @Composable () -> Unit) {
    val theme = PT.theme
    val scheme = remember(theme) { schemeFor(theme.colors) }
    MaterialTheme(colorScheme = scheme, shapes = SharpShapes) {
        CompositionLocalProvider(LocalContentColor provides PT.FgText, content = content)
    }
}

// ---------- Byggeklosser ----------

/** Pleroma-«panel»: halvtransparent flate med tynn kantlinje. */
@Composable
fun Panel(
    modifier: Modifier = Modifier,
    borderColor: Color = PT.Border,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(PT.Fg.copy(alpha = PT.PanelAlpha))
            .border(1.dp, borderColor),
        content = content,
    )
}

/** Knapp i temaets stil: flat, kantlinje, mørkere «innrykket» ved trykk. */
@Composable
fun PButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    filled: Boolean = false,
    icon: ImageVector? = null,
    /** Beskrivelse for skjermlesere når knappen bare har ikon. */
    description: String? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val bg = when {
        pressed -> PT.PressedBg
        filled -> PT.Link
        else -> PT.Fg
    }
    val content = if (filled && !pressed) PT.Fg else PT.Link
    Row(
        modifier
            .alpha(if (enabled) 1f else 0.45f)
            .heightIn(min = 40.dp)
            .background(bg)
            .border(BorderStroke(1.dp, if (pressed) PT.PressedBorder else PT.Border.copy(alpha = 0.8f)))
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = if (text.isBlank()) 10.dp else 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = description ?: text.ifBlank { null }, tint = content, modifier = Modifier.size(18.dp))
            if (text.isNotBlank()) Spacer(Modifier.width(6.dp))
        }
        if (text.isNotBlank()) {
            Text(text, color = content, fontWeight = FontWeight.Medium, maxLines = 1)
        }
    }
}

@Composable
fun ptFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = PT.Fg.copy(alpha = 0.85f),
    unfocusedContainerColor = PT.Fg.copy(alpha = 0.85f),
    focusedBorderColor = PT.Border,
    unfocusedBorderColor = PT.Border.copy(alpha = 0.8f),
    focusedTextColor = PT.Text,
    unfocusedTextColor = PT.Text,
    cursorColor = PT.Link,
    focusedLabelColor = PT.Link,
    unfocusedLabelColor = PT.Muted,
    focusedPlaceholderColor = PT.Muted,
    unfocusedPlaceholderColor = PT.Muted,
)
