package com.teraxes.vital.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teraxes.vital.ui.viewmodel.VitalViewModel
import kotlinx.coroutines.delay

@Composable
fun SecurityPinScreen(viewModel: VitalViewModel) {
    val lockoutUntil by viewModel.lockoutUntilMs.collectAsState()
    var remainingSeconds by remember { mutableIntStateOf(viewModel.getRemainingLockoutSeconds()) }

    LaunchedEffect(lockoutUntil) {
        while (viewModel.isCurrentlyLockedOut()) {
            remainingSeconds = viewModel.getRemainingLockoutSeconds()
            delay(1000)
        }
        remainingSeconds = 0
    }

    SecurityPinScreen(
        onUnlock = { viewModel.unlockApp(it) },
        isLockedOut = remainingSeconds > 0,
        lockoutSeconds = remainingSeconds
    )
}

@Composable
fun SecurityPinScreen(
    onUnlock: (String) -> Boolean,
    isLockedOut: Boolean = false,
    lockoutSeconds: Int = 0
) {
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(
                            if (isLockedOut) MaterialTheme.colorScheme.errorContainer 
                            else MaterialTheme.colorScheme.primaryContainer
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (isLockedOut) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isLockedOut) "Temporarily Locked" else "Vital Security Lock",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )

                Text(
                    text = if (isLockedOut) "Too many failed attempts. Please wait $lockoutSeconds s." 
                           else "Enter your 4-digit PIN to unlock health data",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (isLockedOut) MaterialTheme.colorScheme.error else Color.Gray
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = enteredPin,
                    onValueChange = {
                        if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                            enteredPin = it
                            errorMessage = ""
                        }
                    },
                    enabled = !isLockedOut,
                    label = { Text("PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage.isNotEmpty() && !isLockedOut) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.Red)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val success = onUnlock(enteredPin)
                        if (!success) {
                            errorMessage = "Incorrect PIN. Try again."
                            enteredPin = ""
                        }
                    },
                    enabled = !isLockedOut && enteredPin.length == 4,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (isLockedOut) "Locked ($lockoutSeconds s)" else "Unlock")
                }
            }
        }
    }
}
