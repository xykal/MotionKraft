package xyz.xyverse.motionkraft.presentation

import android.graphics.Color as AndroidColor
import android.net.Uri
import android.view.TextureView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import xyz.xyverse.motionkraft.data.model.*
import xyz.xyverse.motionkraft.presentation.screens.*
import xyz.xyverse.motionkraft.presentation.components.*
import java.io.File

// ---------- Design tokens (Purple default, no stock look) ----------
object Mk {
    val bg0 = Color(0xFF08060F)
    val bg  = Color(0xFF0D0A1E)
    val bg2 = Color(0xFF140F2C)
    val bg3 = Color(0xFF1C1542)
    val card = Color(0xFF181136)
    val card2 = Color(0xFF231854)
    val line = Color(0xFF2D2266)
    val purple50 = Color(0xFFF5F3FF)
    val purple100 = Color(0xFFEDE9FE)
    val purple300 = Color(0xFFC4B5FD)
    val purple400 = Color(0xFFA78BFA)
    val purple = Color(0xFF8B5CF6)
    val purple600 = Color(0xFF7C3AED)
    val purple700 = Color(0xFF6D28D9)
    val pink = Color(0xFFEC4899)
    val gold = Color(0xFFFBBF24)
    val cyan = Color(0xFF22D3EE)
    val green = Color(0xFF10B981)
    val red = Color(0xFFEF4444)
    val text = Color(0xFFF5F3FF)
    val dim = Color(0xFF9B8FC9)
    val dim2 = Color(0xFF6C5FA3)
}

// ---------- Root App ----------
@Composable
fun MotionKraftRoot(
    onPickMedia: () -> Unit,
    onPickImage: () -> Unit,
    vm: EditorViewModel
) {
    val nav = rememberNavController()
    val vip by vm.isVip.collectAsState()

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Mk.purple,
            onPrimary = Color.White,
            secondary = Mk.pink,
            tertiary = Mk.cyan,
            background = Mk.bg,
            surface = Mk.card,
            surfaceVariant = Mk.bg2,
            onBackground = Mk.text,
            onSurface = Mk.text,
            outline = Mk.line,
            error = Mk.red
        )
    ) {
        Box(Modifier.fillMaxSize().background(Mk.bg)) {
            NavHost(navController = nav, startDestination = "home") {
                composable("home") {
                    HomeScreen(
                        vm = vm,
                        onNewProject = { nav.navigate("editor") },
                        onOpenTemplate = { nav.navigate("editor?template=$it") },
                        onGoTemplates = { nav.navigate("templates") },
                        onGoProfile = { nav.navigate("profile") },
                        onGoVip = { nav.navigate("vip") }
                    )
                }
                composable("templates") {
                    TemplatesScreen(
                        onBack = { nav.popBackStack() },
                        onPick = { nav.navigate("editor?template=$it") }
                    )
                }
                composable("profile") {
                    ProfileScreen(
                        vip = vip,
                        onBack = { nav.popBackStack() },
                        onGoVip = { nav.navigate("vip") }
                    )
                }
                composable("vip") {
                    VipScreen(onBack = { nav.popBackStack() }, onSubscribe = { plan ->
                        vm.setVip(true)
                        nav.popBackStack()
                    })
                }
                composable(
                    "editor?template={template}",
                    arguments = listOf(navArgument("template") { nullable = true; defaultValue = null; type = NavType.StringType })
                ) {
                    EditorScreen(
                        vm = vm,
                        onBack = { nav.popBackStack() },
                        onPickMedia = onPickMedia,
                        onPickImage = onPickImage,
                        onGoVip = { nav.navigate("vip") }
                    )
                }
            }
        }
    }
}
