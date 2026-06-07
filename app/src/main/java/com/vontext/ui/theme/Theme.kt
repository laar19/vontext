package com.vontext.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
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
import androidx.core.view.WindowCompat

// ═══════════════════════════════════════════════════════════════════════════
// DARK COLOR SCHEME
// ═══════════════════════════════════════════════════════════════════════════
private val DarkColorScheme = darkColorScheme(
    primary = GreenVontext,
    onPrimary = OnGreenVontext,
    primaryContainer = GreenDark,
    onPrimaryContainer = GreenLight,
    
    secondary = BlueFAB,
    onSecondary = OnBlueFAB,
    secondaryContainer = BlueFABDark,
    onSecondaryContainer = BlueFABLight,
    
    tertiary = GreenMid,
    tertiaryContainer = GreenDark,
    onTertiaryContainer = GreenLight,
    
    error = Error,
    onError = OnError,
    errorContainer = ErrorBg,
    onErrorContainer = OnErrorBg,
    
    background = Color(0xFF121212),
    onBackground = Color(0xFFE8EAED),
    
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE8EAED),
    surfaceVariant = Color(0xFF2D2D2D),
    onSurfaceVariant = Ink2,
    
    outline = Ink3,
    outlineVariant = Divider,
    
    inverseSurface = Color(0xFFE8EAED),
    inverseOnSurface = Ink,
    inversePrimary = GreenVontext
)

// ═══════════════════════════════════════════════════════════════════════════
// LIGHT COLOR SCHEME (Maps Style)
// ═══════════════════════════════════════════════════════════════════════════
private val LightColorScheme = lightColorScheme(
    primary = GreenVontext,
    onPrimary = OnGreenVontext,
    primaryContainer = GreenLight,
    onPrimaryContainer = OnGreenLight,
    
    secondary = BlueFAB,
    onSecondary = OnBlueFAB,
    secondaryContainer = BlueFABLight,
    onSecondaryContainer = BlueFABDark,
    
    tertiary = GreenMid,
    tertiaryContainer = GreenLight,
    onTertiaryContainer = GreenDark,
    
    error = Error,
    onError = OnError,
    errorContainer = ErrorBg,
    onErrorContainer = OnErrorBg,
    
    background = Background,
    onBackground = Ink,
    
    surface = Surface,
    onSurface = Ink,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = Ink2,
    
    outline = Ink3,
    outlineVariant = Divider,
    
    inverseSurface = Ink,
    inverseOnSurface = Color(0xFFE8EAED),
    inversePrimary = GreenVontext
)

// ═══════════════════════════════════════════════════════════════════════════
// THEME COMPOSABLE
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun VontextTheme(
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
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
