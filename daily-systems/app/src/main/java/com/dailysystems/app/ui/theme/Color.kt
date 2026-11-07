package com.dailysystems.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** App palette (SPEC §5): cool grey-blue neutrals with one accent. */
@Immutable
data class AppColors(
    val background: Color,
    val surface: Color,
    val surface2: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val accent: Color,
    val accentSoft: Color,
    val onAccent: Color,
    val socket: Color,
    val danger: Color,
    val ok: Color,
    val scrim: Color,
    val isDark: Boolean,
)

val LightAppColors = AppColors(
    background = Color(0xFFEDF0F4),
    surface = Color(0xFFFFFFFF),
    surface2 = Color(0xFFF4F6F9),
    ink = Color(0xFF141C26),
    muted = Color(0xFF5B6878),
    line = Color(0xFFD6DDE6),
    accent = Color(0xFF2B4ACB),
    accentSoft = Color(0xFFE4E9FB),
    onAccent = Color(0xFFFFFFFF),
    socket = Color(0xFFA3AEBB),
    danger = Color(0xFFB42335),
    ok = Color(0xFF1E7A55),
    scrim = Color(0x73080C12),
    isDark = false,
)

val DarkAppColors = AppColors(
    background = Color(0xFF0E131A),
    surface = Color(0xFF161D26),
    surface2 = Color(0xFF1C2530),
    ink = Color(0xFFE6EBF1),
    muted = Color(0xFF97A5B6),
    line = Color(0xFF283341),
    accent = Color(0xFF93A8FF),
    accentSoft = Color(0xFF222C50),
    onAccent = Color(0xFF0B1230),
    socket = Color(0xFF4C5969),
    danger = Color(0xFFFF7D8B),
    ok = Color(0xFF62D3A1),
    scrim = Color(0x99000000),
    isDark = true,
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }
