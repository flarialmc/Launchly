package com.zeuroux.launchly.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeuroux.launchly.R

val LaunchlyFontFamily = FontFamily(Font(R.font.space_grotesk_regular))

private val LaunchlyColors = darkColorScheme(
    primary = Color(0xFFFF223C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFF223C),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF695055),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF332C30),
    onSecondaryContainer = Color(0xFFE2D8DC),
    tertiary = Color(0xFFD9CDD2),
    onTertiary = Color(0xFF1B1518),
    background = Color.Black,
    onBackground = Color.White,
    surface = Color(0xFF1B1518),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF332C30),
    onSurfaceVariant = Color(0xFFD9CDD2),
    surfaceContainer = Color(0xFF1B1518),
    surfaceContainerHigh = Color(0xFF332C30),
    surfaceContainerHighest = Color(0xFF4A3037),
    outline = Color(0xFF695055),
    outlineVariant = Color(0xFF33272D),
    error = Color(0xFFFF4A5F),
    onError = Color.White,
    errorContainer = Color(0xFF4A3037),
    onErrorContainer = Color.White,
    scrim = Color(0x8C000000)
)

private fun launchlyTextStyle(
    size: Int,
    lineHeight: Int,
    weight: FontWeight = FontWeight.Normal
) = TextStyle(
    fontFamily = LaunchlyFontFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp
)

private val LaunchlyTypography = Typography(
    displayLarge = launchlyTextStyle(40, 46, FontWeight.Bold),
    displayMedium = launchlyTextStyle(32, 38, FontWeight.Bold),
    displaySmall = launchlyTextStyle(28, 34, FontWeight.Bold),
    headlineLarge = launchlyTextStyle(28, 34, FontWeight.Bold),
    headlineMedium = launchlyTextStyle(25, 31, FontWeight.Bold),
    headlineSmall = launchlyTextStyle(22, 28, FontWeight.Bold),
    titleLarge = launchlyTextStyle(22, 28, FontWeight.Bold),
    titleMedium = launchlyTextStyle(17, 23, FontWeight.Bold),
    titleSmall = launchlyTextStyle(15, 21, FontWeight.Bold),
    bodyLarge = launchlyTextStyle(15, 22),
    bodyMedium = launchlyTextStyle(13, 19),
    bodySmall = launchlyTextStyle(11, 16),
    labelLarge = launchlyTextStyle(15, 20, FontWeight.Bold),
    labelMedium = launchlyTextStyle(12, 16, FontWeight.Bold),
    labelSmall = launchlyTextStyle(10, 14, FontWeight.Bold)
)

private val LaunchlyShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(20.dp)
)

@Composable
fun LaunchlyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LaunchlyColors,
        typography = LaunchlyTypography,
        shapes = LaunchlyShapes,
        content = content
    )
}
