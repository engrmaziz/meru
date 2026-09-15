package app.meru.android.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val MeruVoid = Color(0xFF0B0F14)
val MeruPanel = Color(0xFF121821)
val MeruElevated = Color(0xFF1A2330)
val MeruTeal = Color(0xFF2EE6A6)
val MeruCyan = Color(0xFF3DB9FF)
val MeruAmber = Color(0xFFF5A524)
val MeruText = Color(0xFFF4F7FB)
val MeruMuted = Color(0xFF8B97A8)
val MeruDanger = Color(0xFFFF4D4F)

private val MeruDarkColors = darkColorScheme(
    primary = MeruTeal,
    onPrimary = MeruVoid,
    secondary = MeruCyan,
    onSecondary = MeruVoid,
    tertiary = MeruAmber,
    background = MeruVoid,
    onBackground = MeruText,
    surface = MeruPanel,
    onSurface = MeruText,
    surfaceVariant = MeruElevated,
    onSurfaceVariant = MeruMuted,
    error = MeruDanger,
    onError = MeruText,
)

private val MeruTypography = androidx.compose.material3.Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        letterSpacing = (-0.5).sp,
        color = MeruText,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        color = MeruText,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        color = MeruText,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        color = MeruText,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        color = MeruText,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        color = MeruMuted,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        color = MeruText,
    ),
)

@Composable
fun MeruTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = MeruDarkColors,
        typography = MeruTypography,
        content = content,
    )
}
