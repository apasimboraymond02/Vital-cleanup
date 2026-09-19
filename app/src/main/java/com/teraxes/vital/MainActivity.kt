package com.teraxes.vital

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teraxes.vital.notification.NotificationHelper
import com.teraxes.vital.ui.screens.*
import com.teraxes.vital.ui.theme.VitalTheme
import com.teraxes.vital.ui.viewmodel.VitalViewModel

enum class NavItem(val label: String, val icon: ImageVector) {
    DASHBOARD("Today", Icons.Default.Favorite),
    CYCLE("Cycle", Icons.Default.CalendarMonth),
    FERTILITY_FAMILY("Fertility & Baby", Icons.Default.ChildCare),
    HEALTH("Health", Icons.Default.MonitorHeart),
    SETTINGS("Settings", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {

    private val viewModel: VitalViewModel by viewModels()
    private val currentNavTarget = mutableStateOf<NavItem?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Ensure notification channels are registered with Android OS
        NotificationHelper.createNotificationChannels(this)

        // Parse initial launch intent target from notification
        handleIntent(intent)

        setContent {
            VitalTheme {
                val isLocked by viewModel.isLocked.collectAsStateWithLifecycle()
                val isAccountCreated by viewModel.isAccountCreated.collectAsStateWithLifecycle()

                // Permission launcher for Android 13+ (POST_NOTIFICATIONS)
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        viewModel.onNotificationPermissionGranted()
                    }
                }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val hasPerm = ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                        if (!hasPerm) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                if (isLocked) {
                    SecurityPinScreen(viewModel = viewModel)
                } else if (!isAccountCreated) {
                    OnboardingScreen(
                        viewModel = viewModel,
                        onComplete = { /* automatically updates isAccountCreated StateFlow */ }
                    )
                } else {
                    MainAppLayout(
                        viewModel = viewModel,
                        deepLinkNav = currentNavTarget.value
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val target = intent?.getStringExtra("NAV_TARGET") ?: return
        currentNavTarget.value = when (target) {
            "CYCLE" -> NavItem.CYCLE
            "FERTILITY" -> NavItem.FERTILITY_FAMILY
            "HEALTH" -> NavItem.HEALTH
            "SETTINGS" -> NavItem.SETTINGS
            else -> NavItem.DASHBOARD
        }
    }
}

@Composable
fun MainAppLayout(viewModel: VitalViewModel, deepLinkNav: NavItem? = null) {
    var selectedNav by remember { mutableStateOf(deepLinkNav ?: NavItem.DASHBOARD) }
    var showAccountSetupScreen by remember { mutableStateOf(false) }

    LaunchedEffect(deepLinkNav) {
        if (deepLinkNav != null) {
            selectedNav = deepLinkNav
        }
    }

    if (showAccountSetupScreen) {
        AccountSetupScreen(
            viewModel = viewModel,
            onDismiss = { showAccountSetupScreen = false }
        )
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar(modifier = Modifier.testTag("bottom_nav_bar")) {
                    NavItem.entries.forEach { item ->
                        NavigationBarItem(
                            selected = selectedNav == item,
                            onClick = { selectedNav = item },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (selectedNav) {
                    NavItem.DASHBOARD -> DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToCycle = { selectedNav = NavItem.CYCLE },
                        onNavigateToPregnancy = { selectedNav = NavItem.FERTILITY_FAMILY },
                        onOpenAccountSetup = { showAccountSetupScreen = true }
                    )
                    NavItem.CYCLE -> CycleScreen(viewModel = viewModel)
                    NavItem.FERTILITY_FAMILY -> FertilityFamilyScreen(viewModel = viewModel)
                    NavItem.HEALTH -> HealthMetricsScreen(viewModel = viewModel)
                    NavItem.SETTINGS -> SettingsScreen(
                        viewModel = viewModel,
                        onOpenAccountSetup = { showAccountSetupScreen = true }
                    )
                }
            }
        }
    }
}
