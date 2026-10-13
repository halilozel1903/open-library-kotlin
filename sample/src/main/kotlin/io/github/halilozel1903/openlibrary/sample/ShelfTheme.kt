package io.github.halilozel1903.openlibrary.sample

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

private val LightColors = lightColorScheme(
    primary = Color(0xFF2F5D50),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCDE8DD),
    onPrimaryContainer = Color(0xFF0B2019),
    secondary = Color(0xFF8C3B3B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF6DAD5),
    onSecondaryContainer = Color(0xFF3A0B0B),
    tertiary = Color(0xFF8A6A1F),
    tertiaryContainer = Color(0xFFF5E2B5),
    onTertiaryContainer = Color(0xFF2B1F00),
    background = Color(0xFFFBF7F0),
    onBackground = Color(0xFF1F1B16),
    surface = Color(0xFFFBF7F0),
    onSurface = Color(0xFF1F1B16),
    surfaceVariant = Color(0xFFEFE7DA),
    onSurfaceVariant = Color(0xFF51483B),
    surfaceContainer = Color(0xFFF4EEE4),
    surfaceContainerHigh = Color(0xFFEEE7DC),
    outline = Color(0xFF837868),
    outlineVariant = Color(0xFFD6CCBC),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9CCFBE),
    onPrimary = Color(0xFF023829),
    primaryContainer = Color(0xFF1C4A3D),
    onPrimaryContainer = Color(0xFFCDE8DD),
    secondary = Color(0xFFF2B5AC),
    onSecondary = Color(0xFF561D1A),
    secondaryContainer = Color(0xFF6F2C2A),
    onSecondaryContainer = Color(0xFFF6DAD5),
    tertiary = Color(0xFFE3C67E),
    tertiaryContainer = Color(0xFF5A4508),
    onTertiaryContainer = Color(0xFFF5E2B5),
    background = Color(0xFF15130F),
    onBackground = Color(0xFFEAE1D5),
    surface = Color(0xFF15130F),
    onSurface = Color(0xFFEAE1D5),
    surfaceVariant = Color(0xFF2C2822),
    onSurfaceVariant = Color(0xFFD3C7B6),
    surfaceContainer = Color(0xFF211E19),
    surfaceContainerHigh = Color(0xFF2B2722),
    outline = Color(0xFF9C9182),
    outlineVariant = Color(0xFF4D463B),
)

/** Titles in a serif face, like a printed page; everything else in the default sans serif. */
private fun typography(): Typography {
    val base = Typography()
    val serif = FontFamily.Serif
    return base.copy(
        displaySmall = base.displaySmall.copy(fontFamily = serif, fontWeight = FontWeight.SemiBold),
        headlineLarge = base.headlineLarge.copy(fontFamily = serif, fontWeight = FontWeight.SemiBold),
        headlineMedium = base.headlineMedium.copy(fontFamily = serif, fontWeight = FontWeight.SemiBold),
        headlineSmall = base.headlineSmall.copy(fontFamily = serif, fontWeight = FontWeight.SemiBold),
        titleLarge = base.titleLarge.copy(fontFamily = serif, fontWeight = FontWeight.SemiBold),
    )
}

@Composable
fun ShelfTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = typography(),
        content = content,
    )
}
