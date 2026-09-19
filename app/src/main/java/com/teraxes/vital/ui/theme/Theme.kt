package com.teraxes.vital.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = RosePrimary,
    onPrimary = Color.White,
    primaryContainer = RosePrimaryContainer,
    onPrimaryContainer = RoseTertiary,
    secondary = RoseSecondary,
    onSecondary = Color.White,
    background = WarmBackground,
    onBackground = Color(0xFF1F1A1C),
    surface = WarmSurface,
    onSurface = Color(0xFF1F1A1C),
    outline = CardBorder
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB2C9),
    onPrimary = Color(0xFF65002A),
    primaryContainer = Color(0xFF8C003D),
    onPrimaryContainer = Color(0xFFFFD9E2),
    secondary = Color(0xFFE2B8C4),
    onSecondary = Color(0xFF422933),
    background = Color(0xFF191113),
    onBackground = Color(0xFFEFE0E3),
    surface = Color(0xFF21191B),
    onSurface = Color(0xFFEFE0E3),
    outline = Color(0xFF514347)
)

@Composable
fun VitalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
