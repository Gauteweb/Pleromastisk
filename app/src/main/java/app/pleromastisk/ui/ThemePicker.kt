package app.pleromastisk.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.pleromastisk.Graph
import app.pleromastisk.R

/** Velg fargetema. Temaet tas i bruk med en gang og huskes til neste gang. */
@Composable
fun ThemePickerDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.border(1.dp, PT.Border),
        containerColor = PT.Fg,
        title = {
            Text(stringResource(R.string.theme_title), color = PT.TopBarText, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppTheme.entries.forEach { theme ->
                    val active = theme == PT.theme
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .border(1.dp, if (active) PT.Border else PT.Border.copy(alpha = 0.35f))
                            .clickable {
                                PT.theme = theme
                                Graph.store.theme = theme.id
                            }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ThemeSwatch(theme.colors)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            stringResource(theme.label), color = PT.Text,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.weight(1f),
                        )
                        if (active) {
                            Icon(Icons.Filled.Check, null, tint = PT.TopBarText, modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.about_close), color = PT.Link)
            }
        },
    )
}

/** Liten forhåndsvisning av et tema: bakgrunn med en flate, en knapp og overskriftsfargen. */
@Composable
private fun ThemeSwatch(c: PtColors) {
    Row(
        Modifier
            .background(c.bg)
            .border(1.dp, c.border)
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Dot(c.fg, c.border)
        Dot(c.link, c.border)
        Dot(c.topBarText, c.border)
    }
}

@Composable
private fun Dot(fill: Color, border: Color) {
    Box(Modifier.size(14.dp).background(fill).border(1.dp, border))
}
