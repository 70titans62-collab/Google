package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkBlueprintColorScheme = darkColorScheme(
    primary = CyanAccent,
    onPrimary = Color(0xFF041C2C),
    primaryContainer = CyanContainer,
    onPrimaryContainer = Color(0xFFE0F2FE),
    secondary = AmberDraft,
    onSecondary = Color(0xFF2B1300),
    secondaryContainer = AmberContainer,
    onSecondaryContainer = Color(0xFFFEF3C7),
    tertiary = EmeraldMetric,
    onTertiary = Color(0xFF022C22),
    background = BlueprintNavyBg,
    onBackground = TextOnDark,
    surface = BlueprintSurface,
    onSurface = TextOnDark,
    surfaceVariant = BlueprintSurfaceVariant,
    onSurfaceVariant = TextMutedOnDark,
    outline = BlueprintOutline,
    error = CoralAlert,
    onError = Color.White
)

private val LightDraftingColorScheme = lightColorScheme(
    primary = BlueprintBluePrimary,
    onPrimary = Color.White,
    primaryContainer = BlueprintBlueContainer,
    onPrimaryContainer = Color(0xFF082F49),
    secondary = AmberDraftDark,
    onSecondary = Color.White,
    secondaryContainer = AmberLightContainer,
    onSecondaryContainer = Color(0xFF451A03),
    tertiary = EmeraldDark,
    onTertiary = Color.White,
    background = PaperBg,
    onBackground = TextOnLight,
    surface = PaperSurface,
    onSurface = TextOnLight,
    surfaceVariant = PaperSurfaceVariant,
    onSurfaceVariant = TextMutedOnLight,
    outline = PaperOutline,
    error = CoralAlert,
    onError = Color.White
)

val CsiShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun CsiHomeCraftTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkBlueprintColorScheme else LightDraftingColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = CsiTypography,
        shapes = CsiShapes,
        content = content
    )
}
