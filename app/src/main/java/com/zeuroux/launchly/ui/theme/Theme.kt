package com.zeuroux.launchly.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LaunchlyLightColors = lightColorScheme(
    primary = Color(0xFF315DA8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8E2FF),
    onPrimaryContainer = Color(0xFF001A41),
    secondary = Color(0xFF555F71),
    tertiary = Color(0xFF6E5676),
    surface = Color(0xFFF9F9FF),
    surfaceVariant = Color(0xFFE1E2EC)
)

private val LaunchlyDarkColors = darkColorScheme(
    primary = Color(0xFFAEC6FF),
    onPrimary = Color(0xFF002E69),
    primaryContainer = Color(0xFF12458F),
    onPrimaryContainer = Color(0xFFD8E2FF),
    secondary = Color(0xFFBDC7DC),
    tertiary = Color(0xFFDCBCE2),
    surface = Color(0xFF111318),
    surfaceVariant = Color(0xFF44474F)
)

@Composable
fun LaunchlyTheme(content: @Composable () -> Unit) {
    val darkTheme = isSystemInDarkTheme()
    val context = LocalContext.current
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme -> dynamicDarkColorScheme(context)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(context)
        darkTheme -> LaunchlyDarkColors
        else -> LaunchlyLightColors
    }
    MaterialTheme(colorScheme = colors, content = content)
}
