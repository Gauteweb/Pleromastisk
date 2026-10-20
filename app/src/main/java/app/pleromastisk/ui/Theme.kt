package app.pleromastisk.ui

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Farger og former fra Pleroma-temaet «gaute.space»:
 * skarpe hjørner (alle radier = 0), 1 dp tan kantlinje (#AB9977) i stedet for skygger,
 * mørke blågrønne flater og bakgrunn med 80 % dekkevne over bakgrunnsbildet.
 */
object PT {
    val Bg = Color(0xFF203237)
    val Text = Color(0xFFC2D9D4)
    val Link = Color(0xFFAB9977)
    val Fg = Color(0xFF132B29)
    val FgText = Color(0xFFB5D4CE)
    val TopBar = Color(0xFF132B29)
    val TopBarText = Color(0xFF65C89C)
    val Border = Color(0xFFAB9977)
    val Red = Color(0xFF9E555B)
    val Blue = Color(0xFF81A6C4)
    val Green = Color(0xFF319195)
    val Orange = Color(0xFFD6924C)
    val PressedBg = Color(0xFF443D30)
    val PressedBorder = Color(0xFF7D7057)

    const val PanelAlpha = 0.8f
    val Muted = FgText.copy(alpha = 0.65f)
}

private val Scheme = darkColorScheme(
    primary = PT.Link,
    onPrimary = PT.Fg,
    secondary = PT.TopBarText,
    onSecondary = PT.Fg,
    tertiary = PT.Blue,
    background = PT.Bg,
    onBackground = PT.Text,
    surface = PT.Fg,
    onSurface = PT.FgText,
    surfaceVariant = PT.Fg,
    onSurfaceVariant = PT.FgText,
    surfaceContainer = PT.Fg,
    surfaceContainerHigh = PT.Fg,
    surfaceContainerHighest = PT.Fg,
    surfaceContainerLow = PT.Fg,
    surfaceContainerLowest = PT.Fg,
    outline = PT.Border,
    outlineVariant = PT.Border,
    error = PT.Red,
    onError = PT.Text,
)

private val SharpShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(0.dp),
    medium = RoundedCornerShape(0.dp),
    large = RoundedCornerShape(0.dp),
    extraLarge = RoundedCornerShape(0.dp),
)

@Composable
fun PleromastiskTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, shapes = SharpShapes) {
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
