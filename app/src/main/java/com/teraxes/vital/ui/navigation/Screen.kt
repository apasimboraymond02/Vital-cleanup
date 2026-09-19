package com.teraxes.vital.ui.navigation

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Dashboard : Screen("dashboard")
    object Cycle : Screen("cycle")
    object FertilityFamily : Screen("fertility_family")
    object Health : Screen("health")
    object Settings : Screen("settings")
    object SecurityPin : Screen("security_pin")
}
