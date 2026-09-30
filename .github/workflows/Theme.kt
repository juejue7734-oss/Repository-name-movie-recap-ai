package com.movierecap.ai.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val Purple = Color(0xFF635BFF)
private val LightColors = lightColorScheme(primary = Purple, secondary = Color(0xFF7A72FF), background = Color(0xFFF8F7FC), surface = Color.White)
private val DarkColors = darkColorScheme(primary = Color(0xFFB9B3FF), secondary = Color(0xFFCCC8FF))

@Composable fun MovieRecapTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, typography = Typography(), shapes = Shapes(extraLarge = RoundedCornerShape(24.dp)), content = content)
}
