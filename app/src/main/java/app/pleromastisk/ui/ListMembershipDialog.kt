package app.pleromastisk.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.pleromastisk.R
import app.pleromastisk.vm.AccountViewModel

/** Legg kontoen til i (eller fjern den fra) mine lister, eller lag en ny liste med den i. */
@Composable
fun ListMembershipDialog(vm: AccountViewModel, onDismiss: () -> Unit) {
    val state by vm.lists.collectAsStateWithLifecycle()
    var newTitle by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(vm) { vm.loadLists() }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.border(1.dp, PT.Border),
        containerColor = PT.Fg,
        title = { Text(stringResource(R.string.lists_add_title), color = PT.TopBarText, fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                val s = state
                when {
                    s == null -> LoadingRow()
                    s.lists.isEmpty() -> Text(stringResource(R.string.lists_none), color = PT.Muted)
                    else -> s.lists.forEach { list ->
                        val member = list.id in s.memberOf
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 44.dp)
                                .clickable { vm.setInList(list.id, add = !member) },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = member, onCheckedChange = null,
                                colors = CheckboxDefaults.colors(checkedColor = PT.Link, uncheckedColor = PT.Link, checkmarkColor = PT.Fg),
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(list.title, color = PT.Text)
                        }
                    }
                }

                HorizontalDivider(color = PT.Border, modifier = Modifier.padding(vertical = 12.dp))
                Text(stringResource(R.string.lists_new), color = PT.TopBarText, fontWeight = FontWeight.Bold)
                Row(
                    Modifier.fillMaxWidth().padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        singleLine = true,
                        placeholder = { Text(stringResource(R.string.lists_new_hint)) },
                        shape = RectangleShape,
                        colors = ptFieldColors(),
                        modifier = Modifier.weight(1f),
                    )
                    PButton(
                        stringResource(R.string.lists_create),
                        { vm.createListAndAdd(newTitle); newTitle = "" },
                        enabled = newTitle.isNotBlank() && state != null,
                        filled = true,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.about_close), color = PT.Link) }
        },
    )
}
