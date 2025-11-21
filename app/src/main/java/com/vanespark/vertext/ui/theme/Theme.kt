package com.vanespark.vertext.ui.theme

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

/**
 * VectorText Material You theme with support for custom color themes
 * Supports dynamic colors on Android 12+ and falls back to custom theme colors
 *
 * @param darkTheme Whether to use dark theme
 * @param dynamicColor Whether to use Material You dynamic colors (Android 12+)
 * @param colorTheme The custom color theme to use (ignored if dynamicColor is true on Android 12+)
 * @param amoledBlack Whether to use pure black background for dark theme
 */
@Composable
fun VectorTextTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Changed default to false to show custom themes
    colorTheme: ColorTheme = ColorTheme.DEFAULT,
    amoledBlack: Boolean = false,
    content: @Composable () -> Unit
) {
    var colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> colorTheme.darkScheme
        else -> colorTheme.lightScheme
    }

    // Apply AMOLED black theme
    if (amoledBlack && darkTheme) {
        colorScheme = colorScheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceVariant = Color(0xFF0D0D0D)
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? ComponentActivity
            activity?.enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.auto(
                    lightScrim = android.graphics.Color.TRANSPARENT,
                    darkScrim = android.graphics.Color.TRANSPARENT
                ) { !darkTheme },
                navigationBarStyle = SystemBarStyle.auto(
                    lightScrim = android.graphics.Color.TRANSPARENT,
                    darkScrim = android.graphics.Color.TRANSPARENT
                ) { !darkTheme }
            )
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
