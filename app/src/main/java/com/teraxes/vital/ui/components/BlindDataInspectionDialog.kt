package com.teraxes.vital.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.teraxes.vital.domain.BlindedCycleData

@Composable
fun BlindDataInspectionDialog(
    data: BlindedCycleData,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        title = { Text("Blinded Data Inspection") },
        text = { Text("Inspecting redacted PII and metrics.") }
    )
}
