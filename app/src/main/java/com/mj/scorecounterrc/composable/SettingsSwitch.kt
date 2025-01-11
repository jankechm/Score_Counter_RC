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

@Composable
fun SettingsSwitch(
    onChange: (isChecked: Boolean) -> Unit,
    isChecked: MutableState<Boolean>,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Switch(
        modifier = modifier,
        enabled = enabled,
        checked = isChecked.value,
        onCheckedChange = {
            isChecked.value = it
            onChange(it)
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
