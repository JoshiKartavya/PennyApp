package com.kartavya.penny

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

@Immutable
data class PennyColors(
    val background: Color,
    val card: Color,
    val text: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val textPlaceholder: Color,
    val border: Color,
    val borderSecondary: Color,
    val primary: Color,
    val primaryText: Color,
    val danger: Color,
    val success: Color,
    val successLight: Color,
    val iconPlaceholder: Color,
    val overlay: Color,
    val isDark: Boolean
)

val LightPennyColors = PennyColors(
    background = Color(0xFFFCFCFC),
    card = Color(0xFFFFFFFF),
    text = Color(0xFF000000),
    textSecondary = Color(0xFF888888),
    textMuted = Color(0xFFBBBBBB),
    textPlaceholder = Color(0xFFCCCCCC),
    border = Color(0xFFF0F0F0),
    borderSecondary = Color(0xFFF2F2F2),
    primary = Color(0xFF000000),
    primaryText = Color(0xFFFFFFFF),
    danger = Color(0xFFC56A67),
    success = Color(0xFF388E3C),
    successLight = Color(0xFF6A9C78),
    iconPlaceholder = Color(0xFFF0F0F0),
    overlay = Color(0x4D000000),
    isDark = false
)

val DarkPennyColors = PennyColors(
    background = Color(0xFF121212),
    card = Color(0xFF1E1E1E),
    text = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFAAAAAA),
    textMuted = Color(0xFF888888),
    textPlaceholder = Color(0xFF666666),
    border = Color(0xFF2C2C2C),
    borderSecondary = Color(0xFF333333),
    primary = Color(0xFFFFFFFF),
    primaryText = Color(0xFF000000),
    danger = Color(0xFFFF8A84),
    success = Color(0xFF4CAF50),
    successLight = Color(0xFF81C784),
    iconPlaceholder = Color(0xFF2C2C2C),
    overlay = Color(0x99000000),
    isDark = true
)

val LocalPennyColors = staticCompositionLocalOf { LightPennyColors }

object PennyTheme {
    val colors: PennyColors
        @Composable
        @ReadOnlyComposable
        get() = LocalPennyColors.current
}

@Composable
fun PennyAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkPennyColors else LightPennyColors
    CompositionLocalProvider(
        LocalPennyColors provides colors,
        content = content
    )
}
