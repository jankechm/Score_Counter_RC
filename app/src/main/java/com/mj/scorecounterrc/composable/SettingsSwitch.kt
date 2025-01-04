package com.mj.scorecounterrc.composable

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import com.mj.scorecounterrc.viewmodel.SettingsViewModel

@Composable
fun SettingsSwitch(
    onEvent: (event: SettingsViewModel.SettingsViewModelEvent) -> Unit,
    isChecked: MutableState<Boolean>,
    switchType: SwitchType,
    modifier: Modifier = Modifier,
) {
    Switch(
        modifier = modifier,
        checked = isChecked.value,
        onCheckedChange = {
            isChecked.value = it
            when (switchType) {
                SwitchType.SHOW_SCORE -> onEvent(
                    SettingsViewModel.SettingsViewModelEvent.ShowScoreChangedEvent(
                        it
                    )
                )

                SwitchType.SHOW_TIME -> onEvent(
                    SettingsViewModel.SettingsViewModelEvent.ShowTimeChangedEvent(
                        it
                    )
                )
            }
        },
        thumbContent = if (isChecked.value) {
            {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(SwitchDefaults.IconSize),
                )
            }
        } else {
            null
        }
    )
}

enum class SwitchType {
    SHOW_SCORE, SHOW_TIME
}