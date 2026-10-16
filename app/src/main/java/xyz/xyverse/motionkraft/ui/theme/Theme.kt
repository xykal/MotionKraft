package xyz.xyverse.motionkraft.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF6366F1),
    secondary = Color(0xFFEC4899),
    tertiary = Color(0xFF14B8A6),
    background = Color(0xFF0B0B12),
    surface = Color(0xFF15151F),
    surfaceVariant = Color(0xFF1E1E2E),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6366F1),
    secondary = Color(0xFFEC4899),
    tertiary = Color(0xFF14B8A6),
    background = Color(0xFFF8F9FC),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1A1A2E),
    onSurface = Color(0xFF1A1A2E)
)

@Composable
fun MotionKraftTheme(
    darkTheme: Boolean = true, // Default dark untuk editing seperti CapCut/AM
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme -> dynamicDarkColorScheme(LocalContext.current)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !darkTheme -> dynamicLightColorScheme(LocalContext.current)
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
