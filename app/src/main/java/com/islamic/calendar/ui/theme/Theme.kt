package com.islamic.calendar.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColors = darkColorScheme(
    primary = AccentGold,
    onPrimary = NightDeep,
    primaryContainer = NightMid,
    onPrimaryContainer = AccentSoft,
    secondary = Starlight,
    onSecondary = NightDeep,
    background = NightDeep,
    onBackground = AccentSoft,
    surface = NightMid,
    onSurface = AccentSoft,
    surfaceVariant = NightMid,
    onSurfaceVariant = Starlight,
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF6B4E16),
    onPrimary = Color(0xFFFFF8E7),
    primaryContainer = Color(0xFFF4E4BC),
    onPrimaryContainer = Color(0xFF3D2E0A),
    secondary = Color(0xFF415A77),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFFDF8F0),
    onBackground = Color(0xFF1B263B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1B263B),
    surfaceVariant = Color(0xFFE8E0D5),
    onSurfaceVariant = Color(0xFF415A77),
)

@Composable
fun IslamicCalendarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
