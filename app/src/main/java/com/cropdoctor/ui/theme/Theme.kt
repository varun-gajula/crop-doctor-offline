package com.cropdoctor.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = FarmGreenPrimary,
    secondary = FarmGreenSecondary,
    tertiary = FarmGreenTertiary,
    background = CreamBackground,
    surface = SurfaceCard,
    onPrimary = SurfaceCard,
    onSecondary = SurfaceCard,
    onBackground = HighContrastText,
    onSurface = HighContrastText
)

@Composable
fun CropDoctorTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}

