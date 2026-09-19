package com.teraxes.vital.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

@Composable
fun CorrectPredictionDialog(
    currentAvgCycleDays: Int,
    onDismiss: () -> Unit,
    onConfirmCorrection: (Long, String, Int?) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirmCorrection(System.currentTimeMillis(), "Medium", currentAvgCycleDays) }) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = { Text("Correct Prediction") },
        text = { Text("Current average cycle is $currentAvgCycleDays days. Adjust your data if needed.") }
    )
}
