package xyz.xyverse.motionkraft.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Clip colours on the timeline, keyed by clip type. */
object ClipPalette {
    val Video = Color(0xFF2A8C92)
    val Image = Color(0xFF4062C9)
    val Text = Color(0xFFB7791F)
    val Playhead = Color(0xFFFF5C8A)
}

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF9C8CFF),
    onPrimary = Color(0xFF1A1040),
    primaryContainer = Color(0xFF2E2470),
    onPrimaryContainer = Color(0xFFE4DEFF),
    secondary = Color(0xFFFF6B9A),
    onSecondary = Color(0xFF3D0020),
    background = Color(0xFF0D0D12),
    onBackground = Color(0xFFECECF3),
    surface = Color(0xFF15151D),
    onSurface = Color(0xFFECECF3),
    surfaceVariant = Color(0xFF1F1F2A),
    onSurfaceVariant = Color(0xFF9A9AB0),
    outline = Color(0xFF3A3A50),
    error = Color(0xFFFF6B6B)
)

@Composable
fun MotionKraftTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkScheme, content = content)
}
