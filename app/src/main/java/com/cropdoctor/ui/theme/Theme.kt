package com.cropdoctor.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val FarmGreenSecondary = FarmGreenLight
val FarmGreenTertiary = FarmGreenDark
val CreamBackground = BackgroundWhite
val SurfaceCard = CardSurface
val HighContrastText = TextPrimary

private val LightColorScheme = lightColorScheme(
    primary = FarmGreenPrimary,
    secondary = FarmGreenLight,
    tertiary = FarmGreenDark,
    background = BackgroundWhite,
    surface = CardSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary
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
