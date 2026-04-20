package com.antigravity.contactnetworkingpro.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary              = Copper,
    onPrimary            = TextPrimary,
    primaryContainer     = CopperContainer,
    onPrimaryContainer   = CopperLight,
    secondary            = CopperLight,
    onSecondary          = Background,
    secondaryContainer   = CopperContainer,
    onSecondaryContainer = CopperLight,
    background           = Background,
    onBackground         = TextPrimary,
    surface              = SurfaceDark,
    onSurface            = TextPrimary,
    surfaceVariant       = SurfaceVariant,
    onSurfaceVariant     = TextSecondary,
    outline              = Border,
    error                = ErrorRed,
    onError              = TextPrimary
)

@Composable
fun ContactNetworkingProTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = Typography,
        content     = content
    )
}
