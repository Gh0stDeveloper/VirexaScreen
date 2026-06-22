package com.virexa.screen.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.virexa.screen.data.ThemeMode

private val VirexaDark = darkColorScheme(
    primary = Color(0xFF96A2FF),
    onPrimary = Color(0xFF0B1020),
    primaryContainer = Color(0xFF1A2143),
    onPrimaryContainer = Color(0xFFE7E9FF),
    secondary = Color(0xFF7DE3D3),
    onSecondary = Color(0xFF04201B),
    secondaryContainer = Color(0xFF12342F),
    onSecondaryContainer = Color(0xFFD8FFF7),
    tertiary = Color(0xFF8CC9FF),
    onTertiary = Color(0xFF04182A),
    background = Color(0xFF060810),
    onBackground = Color(0xFFF2F4FA),
    surface = Color(0xFF0F1320),
    onSurface = Color(0xFFF2F4FA),
    surfaceVariant = Color(0xFF182033),
    onSurfaceVariant = Color(0xFFB5BDD4),
    outline = Color(0xFF313A52),
    outlineVariant = Color(0xFF22293A),
    error = Color(0xFFFF7D86),
    onError = Color(0xFF3A0A11),
    errorContainer = Color(0xFF3A1620),
    onErrorContainer = Color(0xFFFFD7DB),
)

private val VirexaLight = lightColorScheme(
    primary = Color(0xFF2F53FF),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE6EAFF),
    onPrimaryContainer = Color(0xFF0F1A55),
    secondary = Color(0xFF0B9E8A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD7F4EF),
    onSecondaryContainer = Color(0xFF042E29),
    tertiary = Color(0xFF256DFF),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF6F8FC),
    onBackground = Color(0xFF10131C),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF10131C),
    surfaceVariant = Color(0xFFE8EBF5),
    onSurfaceVariant = Color(0xFF525B70),
    outline = Color(0xFFC5CAD9),
    outlineVariant = Color(0xFFD9DEEA),
    error = Color(0xFFE13D4D),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFE2E5),
    onErrorContainer = Color(0xFF5A1020),
)

private val VirexaShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(30.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

private val VirexaTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 40.sp,
        lineHeight = 46.sp,
        letterSpacing = (-0.4).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.3).sp,
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.2).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.2).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.15).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
    ),
)

@Composable
fun VirexaTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit,
) {
    val useDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    MaterialTheme(
        colorScheme = if (useDark) VirexaDark else VirexaLight,
        typography = VirexaTypography,
        shapes = VirexaShapes,
        content = content,
    )
}
