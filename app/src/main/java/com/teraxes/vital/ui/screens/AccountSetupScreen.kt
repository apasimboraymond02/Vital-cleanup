package com.teraxes.vital.ui.screens

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.teraxes.vital.ui.viewmodel.VitalViewModel

@Composable
fun AccountSetupScreen(
    viewModel: VitalViewModel,
    onDismiss: () -> Unit
) {
    Surface {
        Text("Account Setup")
    }
}
