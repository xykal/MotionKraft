package xyz.xyverse.motionkraft.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF8B5CF6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF6D28D9),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFFEC4899),
    onSecondary = Color.White,
    tertiary = Color(0xFF22D3EE),
    onTertiary = Color(0xFF0B0B12),
    background = Color(0xFF08060F),
    onBackground = Color(0xFFF5F3FF),
    surface = Color(0xFF140F2C),
    onSurface = Color(0xFFF5F3FF),
    surfaceVariant = Color(0xFF1C1542),
    onSurfaceVariant = Color(0xFFC4B5FD),
    outline = Color(0xFF2D2266),
    error = Color(0xFFEF4444)
)

@Composable
fun MotionKraftTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    // Selalu pakai purple theme kita, tidak pakai dynamic color Android
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography(),
        content = content
    )
}
