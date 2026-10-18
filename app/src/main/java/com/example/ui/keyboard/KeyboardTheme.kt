package com.example.ui.keyboard

import androidx.compose.ui.graphics.Color

enum class KeyboardThemeType {
    DYNAMIC_SYSTEM,
    DARK_MATERIAL,
    OLED_PITCH_BLACK,
    CYBERPUNK_NEON,
    WARM_PASTEL
}

enum class OneHandedMode {
    OFF,
    LEFT,
    RIGHT,
    FLOATING
}

data class KeyboardThemeColors(
    val surface: Color,
    val keyBackground: Color,
    val keyContent: Color,
    val keySubContent: Color,
    val actionKeyBackground: Color,
    val actionKeyContent: Color,
    val primaryAccent: Color,
    val activeKeyHighlight: Color,
    val trailColor: Color
)

object KeyboardThemes {
    val DarkMaterial = KeyboardThemeColors(
        surface = Color(0xFF1E1E24),
        keyBackground = Color(0xFF2A2B36),
        keyContent = Color(0xFFF0F0F0),
        keySubContent = Color(0xFFA0A0B0),
        actionKeyBackground = Color(0xFF353746),
        actionKeyContent = Color(0xFF00E5FF),
        primaryAccent = Color(0xFF00E5FF),
        activeKeyHighlight = Color(0xFF3D4054),
        trailColor = Color(0xFF00E5FF)
    )

    val OledBlack = KeyboardThemeColors(
        surface = Color(0xFF000000),
        keyBackground = Color(0xFF121212),
        keyContent = Color(0xFFFFFFFF),
        keySubContent = Color(0xFF757575),
        actionKeyBackground = Color(0xFF1E1E1E),
        actionKeyContent = Color(0xFFBB86FC),
        primaryAccent = Color(0xFFBB86FC),
        activeKeyHighlight = Color(0xFF2C2C2C),
        trailColor = Color(0xFFBB86FC)
    )

    val Cyberpunk = KeyboardThemeColors(
        surface = Color(0xFF0D0221),
        keyBackground = Color(0xFF190E38),
        keyContent = Color(0xFFFF007F),
        keySubContent = Color(0xFF00F5D4),
        actionKeyBackground = Color(0xFF261454),
        actionKeyContent = Color(0xFF00F5D4),
        primaryAccent = Color(0xFFFF007F),
        activeKeyHighlight = Color(0xFF361A75),
        trailColor = Color(0xFFFF007F)
    )

    val WarmPastel = KeyboardThemeColors(
        surface = Color(0xFFFDF6EC),
        keyBackground = Color(0xFFFFFFFF),
        keyContent = Color(0xFF4A3E3D),
        keySubContent = Color(0xFF9E8A87),
        actionKeyBackground = Color(0xFFF5E6D8),
        actionKeyContent = Color(0xFFD97706),
        primaryAccent = Color(0xFFD97706),
        activeKeyHighlight = Color(0xFFEBDCC0),
        trailColor = Color(0xFFD97706)
    )
}
