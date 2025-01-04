package com.mj.scorecounterrc.composable

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.mj.scorecounterrc.viewmodel.SettingsViewModel

@Composable
fun SettingsRadioRow(
    onEvent: (event: SettingsViewModel.SettingsViewModelEvent) -> Unit,
    selectedTextViewBehaviour: MutableState<SettingsViewModel.TextViewBehaviour>,
    textViewBehaviour: SettingsViewModel.TextViewBehaviour
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(60.dp)
            .selectable(
                selected =
                selectedTextViewBehaviour.value == textViewBehaviour,
                onClick = {
                    selectedTextViewBehaviour.value = textViewBehaviour
                    onEvent(
                        SettingsViewModel.SettingsViewModelEvent.TextViewBehaviourChangedEvent(
                            textViewBehaviour
                        )
                    )
                },
                role = Role.RadioButton
            )
    ) {
        RadioButton(
            selected = selectedTextViewBehaviour.value == textViewBehaviour,
            onClick = null // The row handles onClick
        )
        Spacer(modifier = Modifier.size(5.dp))
        Text(
            text = textViewBehaviour.text,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}