package app.pleromastisk.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.pleromastisk.R
import app.pleromastisk.data.SavedAccount

/** «Bytt bruker»: velg en av de innloggede brukerne, eller legg til en ny. */
@Composable
fun AccountSwitchDialog(
    accounts: List<SavedAccount>,
    activeKey: String?,
    onSelect: (String) -> Unit,
    onAdd: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.border(1.dp, PT.Border),
        containerColor = PT.Fg,
        title = {
            Text(stringResource(R.string.switch_account), color = PT.TopBarText, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                accounts.forEach { a ->
                    val active = a.key == activeKey
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .border(1.dp, if (active) PT.Border else PT.Border.copy(alpha = 0.35f))
                            .clickable { if (active) onDismiss() else onSelect(a.key) }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Avatar(a.avatar, 40)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                a.displayName.ifBlank { a.acct.ifBlank { a.handle } },
                                color = PT.Text, fontWeight = FontWeight.Bold,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                "@" + a.handle, color = PT.Muted, style = MaterialTheme.typography.bodySmall,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (active) {
                            Icon(
                                Icons.Filled.Check, stringResource(R.string.switch_account_active),
                                tint = PT.TopBarText, modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.width(0.dp))
                PButton(stringResource(R.string.add_account), onAdd, icon = Icons.Filled.PersonAdd)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = PT.Link)
            }
        },
    )
}
