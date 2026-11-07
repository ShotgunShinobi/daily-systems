package com.dailysystems.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.dailysystems.app.R

val Bricolage = FontFamily(
    Font(R.font.bricolage_medium, FontWeight.Medium),
    Font(R.font.bricolage_bold, FontWeight.Bold),
)

val InstrumentSans = FontFamily(
    Font(R.font.instrument_regular, FontWeight.Normal),
    Font(R.font.instrument_medium, FontWeight.Medium),
    Font(R.font.instrument_semibold, FontWeight.SemiBold),
    Font(R.font.instrument_bold, FontWeight.Bold),
)

val PlexMono = FontFamily(
    Font(R.font.plexmono_regular, FontWeight.Normal),
    Font(R.font.plexmono_medium, FontWeight.Medium),
)

/** Styles Material's Typography has no slot for. */
@Immutable
data class AppType(
    /** Big screen title ("Today", "To-dos"). */
    val screenTitle: TextStyle,
    /** System card / note card headings. */
    val cardTitle: TextStyle,
    /** Editor title inputs. */
    val editorTitle: TextStyle,
    /** Uppercase section headers ("SYSTEMS RUNNING"). */
    val sectionLabel: TextStyle,
    /** Dates and counts. */
    val mono: TextStyle,
    val monoSmall: TextStyle,
)

val AppTypeDefault = AppType(
    screenTitle = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 31.sp, letterSpacing = (-0.02).em),
    cardTitle = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 22.sp, letterSpacing = (-0.01).em),
    editorTitle = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 21.sp, lineHeight = 26.sp),
    sectionLabel = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp, letterSpacing = 0.08.em),
    mono = TextStyle(fontFamily = PlexMono, fontWeight = FontWeight.Medium, fontSize = 13.sp, letterSpacing = (-0.01).em),
    monoSmall = TextStyle(fontFamily = PlexMono, fontWeight = FontWeight.Normal, fontSize = 11.5.sp, letterSpacing = 0.02.em),
)

val LocalAppType = staticCompositionLocalOf { AppTypeDefault }

private val body = TextStyle(fontFamily = InstrumentSans)

val AppTypography = Typography(
    displaySmall = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 32.sp),
    headlineLarge = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 28.sp),
    headlineMedium = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 24.sp),
    headlineSmall = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 20.sp),
    titleLarge = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 19.sp),
    titleMedium = body.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
    titleSmall = body.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    bodyLarge = body.copy(fontSize = 15.sp, lineHeight = 21.sp),
    bodyMedium = body.copy(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = body.copy(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = body.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    labelMedium = body.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
    labelSmall = body.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
)
