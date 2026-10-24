package com.pdfsaathi.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// Colors - Stitch Warm Orange Design System
val PrimaryOrange = Color(0xFFEA580C)
val PrimaryOrangeContainer = Color(0xFFFFEDD5)
val SecondaryOrange = Color(0xFFC2410C)
val LightSurface = Color(0xFFFAFAFA)
val DarkSurface = Color(0xFF121417)
val SepiaSurface = Color(0xFFFBF0D9)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFB923C), // Vibrant Light Orange for Dark Mode
    primaryContainer = Color(0xFF7C2D12),
    onPrimary = Color(0xFF431407),
    onPrimaryContainer = Color(0xFFFFDBC8),
    secondary = Color(0xFFF97316),
    secondaryContainer = Color(0xFF431407),
    onSecondaryContainer = Color(0xFFFFDBC8),
    tertiary = Color(0xFFFBBF24),
    background = Color(0xFF121417),
    surface = Color(0xFF1A1D23),
    onBackground = Color(0xFFF1F5F9),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF262A33),
    onSurfaceVariant = Color(0xFF94A3B8)
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryOrange,
    primaryContainer = PrimaryOrangeContainer,
    onPrimary = Color.White,
    onPrimaryContainer = Color(0xFF9A3412),
    secondary = SecondaryOrange,
    secondaryContainer = Color(0xFFFFF7ED),
    onSecondaryContainer = PrimaryOrange,
    tertiary = Color(0xFFD97706),
    background = LightSurface,
    surface = Color.White,
    onBackground = Color(0xFF131B2E),
    onSurface = Color(0xFF131B2E),
    surfaceVariant = Color(0xFFF3F4F6),
    onSurfaceVariant = Color(0xFF64748B)
)

val AppTypography = Typography(
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
)

@Composable
fun PDFSaathiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
