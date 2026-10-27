package com.greentycoon.game.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = PlantLightGreen,
    secondary = WaterBlue,
    tertiary = HudYellow,
    background = WallTerracotta,
    surface = WallBeige
)

@Composable
fun GreenTycoonTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
