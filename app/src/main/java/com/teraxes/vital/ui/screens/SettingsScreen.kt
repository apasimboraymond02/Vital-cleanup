package com.teraxes.vital.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.teraxes.vital.notification.NotificationHelper
import com.teraxes.vital.ui.theme.RosePrimary
import com.teraxes.vital.ui.theme.RoseTertiary
import com.teraxes.vital.ui.viewmodel.VitalViewModel

@Composable
fun SettingsScreen(
    viewModel: VitalViewModel,
    onOpenAccountSetup: () -> Unit = {}
) {
    val userPin by viewModel.userPin.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val userEmail by viewModel.userEmail.collectAsState()
    val avgCycleLength by viewModel.avgCycleLength.collectAsState()
    val avgPeriodLength by viewModel.avgPeriodLength.collectAsState()
    val isAccountCreated by viewModel.isAccountCreated.collectAsState()
    val isCycleReminderEnabled by viewModel.isCycleReminderEnabled.collectAsState()
    val reminderDaysBefore by viewModel.reminderDaysBefore.collectAsState()
    val isFertileReminderEnabled by viewModel.isFertileReminderEnabled.collectAsState()
    val isDiscreetMode by viewModel.isDiscreetMode.collectAsState()
    val isDailyCheckInReminderEnabled by viewModel.isDailyCheckInReminderEnabled.collectAsState()
    val isMedicationReminderEnabled by viewModel.isMedicationReminderEnabled.collectAsState()

    SettingsScreenContent(
        userName = userName,
        userEmail = userEmail,
        avgCycleLength = avgCycleLength,
        avgPeriodLength = avgPeriodLength,
        userPin = userPin,
        isAccountCreated = isAccountCreated,
        isCycleReminderEnabled = isCycleReminderEnabled,
        reminderDaysBefore = reminderDaysBefore,
        isFertileReminderEnabled = isFertileReminderEnabled,
        isDiscreetMode = isDiscreetMode,
        isDailyCheckInReminderEnabled = isDailyCheckInReminderEnabled,
        isMedicationReminderEnabled = isMedicationReminderEnabled,
        onSetPin = { viewModel.setPin(it) },
        onLockApp = { viewModel.lockApp() },
        onResetAccount = { viewModel.resetAccount() },
        onOpenAccountSetup = onOpenAccountSetup,
        onToggleCycleReminder = { viewModel.setCycleReminderEnabled(it) },
        onChangeReminderDays = { viewModel.setReminderDaysBefore(it) },
        onToggleFertileReminder = { viewModel.setFertileReminderEnabled(it) },
        onToggleDiscreetMode = { viewModel.setDiscreetMode(it) },
        onToggleDailyCheckIn = { viewModel.setDailyCheckInReminder(it) },
        onToggleMedicationReminder = { viewModel.setMedicationReminder(it) },
        onSendTestNotification = { type -> viewModel.sendTestNotification(type) },
        onPermissionGranted = { viewModel.onNotificationPermissionGranted() }
    )
}

@Composable
fun SettingsScreenContent(
    userName: String,
    userEmail: String,
    avgCycleLength: Int,
    avgPeriodLength: Int,
    userPin: String,
    isAccountCreated: Boolean,
    isCycleReminderEnabled: Boolean = true,
    reminderDaysBefore: Int = 2,
    isFertileReminderEnabled: Boolean = true,
    isDiscreetMode: Boolean = false,
    isDailyCheckInReminderEnabled: Boolean = true,
    isMedicationReminderEnabled: Boolean = false,
    onSetPin: (String) -> Unit,
    onLockApp: () -> Unit = {},
    onResetAccount: () -> Unit,
    onOpenAccountSetup: () -> Unit = {},
    onToggleCycleReminder: (Boolean) -> Unit = {},
    onChangeReminderDays: (Int) -> Unit = {},
    onToggleFertileReminder: (Boolean) -> Unit = {},
    onToggleDiscreetMode: (Boolean) -> Unit = {},
    onToggleDailyCheckIn: (Boolean) -> Unit = {},
    onToggleMedicationReminder: (Boolean) -> Unit = {},
    onSendTestNotification: (String) -> Boolean = { true },
    onPermissionGranted: () -> Unit = {}
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var showPinDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                NotificationManagerCompat.from(context).areNotificationsEnabled()
            }
        )
    }

    // Notification permission launcher for Android 13+ (API 33+)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (isGranted) {
            onToggleCycleReminder(true)
            onPermissionGranted()
            Toast.makeText(context, "✓ Notifications enabled successfully", Toast.LENGTH_SHORT).show()
        } else {
            onToggleCycleReminder(false)
            Toast.makeText(context, "Notification permission is required for cycle alerts", Toast.LENGTH_LONG).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Settings & Privacy",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
        )

        // 1. User Account Profile Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isAccountCreated) userName.ifEmpty { "Vital User" } else "Visitor (Sample Baseline)",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (isAccountCreated) userEmail.ifEmpty { "Personal Profile" } else "Sample baseline preview mode",
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
                        )
                    }

                    FilledTonalButton(
                        onClick = onOpenAccountSetup,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (isAccountCreated) Icons.Default.Edit else Icons.Default.PersonAdd,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isAccountCreated) "Edit" else "Setup")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Average Cycle Length",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                        )
                        Text(
                            text = "$avgCycleLength days",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Average Period Length",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                        )
                        Text(
                            text = "$avgPeriodLength days",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                if (isAccountCreated) {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = { showResetDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reset Local Account")
                    }
                }
            }
        }

        // 2. Discreet & Privacy Camouflage Mode
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDiscreetMode) Color(0xFFF1F5F9) else MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = if (isDiscreetMode) Color(0xFF475569) else MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Discreet Mode (Camouflage)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Switch(
                        checked = isDiscreetMode,
                        onCheckedChange = { onToggleDiscreetMode(it) },
                        modifier = Modifier.testTag("discreet_mode_switch")
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (isDiscreetMode)
                        "Discreet Mode is ACTIVE. Menstrual & cycle terms are masked into neutral wellness wording on your lock screen and overview."
                    else
                        "Hide explicit period and menstrual references on widget cards and lock-screen notifications for privacy in public spaces.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
                )
            }
        }

        // 3. Biometric & PIN Vault Lock
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Biometric / PIN Vault Lock",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Switch(
                        checked = userPin.isNotEmpty(),
                        onCheckedChange = { checked ->
                            if (checked) {
                                showPinDialog = true
                            } else {
                                onSetPin("")
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (userPin.isNotEmpty())
                        "PIN Protection is ACTIVE. The app auto-locks on startup and brute-force protection is enabled (max 5 attempts)."
                    else
                        "Protect your cycle, fertility biometrics, and private notes with a 4-digit security PIN.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
                )

                if (userPin.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onLockApp,
                        colors = ButtonDefaults.buttonColors(containerColor = RosePrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Lock Vault Now")
                    }
                }
            }
        }

        // 4. Smart Notifications & Reminders Suite
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("notifications_settings_card")
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Smart Health Reminders",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Switch(
                        checked = isCycleReminderEnabled,
                        onCheckedChange = { checked ->
                            if (checked) {
                                if (!hasNotificationPermission) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        onToggleCycleReminder(true)
                                    }
                                } else {
                                    onToggleCycleReminder(true)
                                }
                            } else {
                                onToggleCycleReminder(false)
                            }
                        },
                        modifier = Modifier.testTag("cycle_reminder_switch")
                    )
                }

                // Notification Permission Status Banner
                if (!hasNotificationPermission) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Notifications Disabled",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Vital requires system notification access to deliver cycle predictions and medication alerts on time.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            try {
                                                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                                }
                                                context.startActivity(intent)
                                            } catch (_: Exception) {}
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Grant Permission", fontSize = 12.sp)
                                }
                                OutlinedButton(
                                    onClick = {
                                        try {
                                            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                            }
                                            context.startActivity(intent)
                                        } catch (_: Exception) {}
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("System Settings", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "System notifications active & background alarms scheduled",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Text(
                    text = "Receive local on-device alarms for upcoming cycles, ovulation windows, evening symptom logging, and medications.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
                )

                if (isCycleReminderEnabled) {
                    HorizontalDivider()

                    // Cycle Approaching Days selector
                    Text(
                        text = "Notify me before predicted cycle:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(1, 2, 3, 5).forEach { days ->
                            FilterChip(
                                selected = reminderDaysBefore == days,
                                onClick = { onChangeReminderDays(days) },
                                label = { Text("$days day${if (days > 1) "s" else ""} before") }
                            )
                        }
                    }

                    // Fertile Window Alert Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Fertile Window & Ovulation Alerts (9:00 AM)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Alert when entering estimated high fertility phase", fontSize = 12.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = isFertileReminderEnabled,
                            onCheckedChange = { onToggleFertileReminder(it) }
                        )
                    }

                    // Daily Check-in Reminder Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Evening Wellness Check-In (8:00 PM)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Daily prompt to record symptoms, moods & vitals", fontSize = 12.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = isDailyCheckInReminderEnabled,
                            onCheckedChange = { onToggleDailyCheckIn(it) }
                        )
                    }

                    // Medication / Pill Reminder Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Medication / Contraceptive (9:00 AM)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Daily reminder for vitamins or pill", fontSize = 12.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = isMedicationReminderEnabled,
                            onCheckedChange = { onToggleMedicationReminder(it) }
                        )
                    }

                    HorizontalDivider()

                    // Instant Verification Section
                    Text(
                        text = "Instant Notification Verification:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Send an immediate test alert directly to your notification tray to verify sound, icons, and navigation.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (!hasNotificationPermission) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                } else {
                                    val success = onSendTestNotification("cycle")
                                    if (success) {
                                        Toast.makeText(context, "🌸 Cycle alert sent! Check notification tray", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("test_cycle_notification_btn"),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                        ) {
                            Text("Test Cycle Alert", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                if (!hasNotificationPermission) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                } else {
                                    val success = onSendTestNotification("fertile")
                                    if (success) {
                                        Toast.makeText(context, "✨ Fertile window alert sent! Check notification tray", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("test_fertile_notification_btn"),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                        ) {
                            Text("Test Fertility", fontSize = 12.sp)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (!hasNotificationPermission) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                } else {
                                    val success = onSendTestNotification("daily")
                                    if (success) {
                                        Toast.makeText(context, "📝 Daily check-in prompt sent! Check notification tray", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("test_daily_notification_btn"),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                        ) {
                            Text("Test Daily Log", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                if (!hasNotificationPermission) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                } else {
                                    val success = onSendTestNotification("medication")
                                    if (success) {
                                        Toast.makeText(context, "💊 Medication alert sent! Check notification tray", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("test_medication_notification_btn"),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                        ) {
                            Text("Test Medication", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 5. Data Privacy Guarantee
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Zero-Knowledge Local Storage",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Vital stores your biometric health logs exclusively on your device in a secure SQLite database. Your cycle history, thermal curves, symptoms, and pregnancy records are private and never monetized.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray, lineHeight = 20.sp)
                )
            }
        }
    }

    // Set PIN Dialog
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Set 4-Digit Security PIN") },
            text = {
                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) pinInput = it },
                    label = { Text("Enter 4 digits") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinInput.length == 4) {
                            onSetPin(pinInput)
                            pinInput = ""
                            showPinDialog = false
                        }
                    }
                ) {
                    Text("Set PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Reset Account Confirmation Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Local Account?") },
            text = { Text("This will clear your stored profile and cycle parameters, allowing you to re-run the account setup process.") },
            confirmButton = {
                Button(
                    onClick = {
                        onResetAccount()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Reset Account")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
