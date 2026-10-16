package xyz.xyverse.motionkraft.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.xyverse.motionkraft.presentation.Mk

// ---------- MkButton: primary purple pill ----------
@Composable
fun MkButton(
    label: String,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(14.dp),
        color = Color.Transparent
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(listOf(Mk.purple600, Mk.purple, Mk.pink)),
                    RoundedCornerShape(14.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 0.5.sp)
            }
        }
    }
}

@Composable
fun MkGhostButton(label: String, icon: ImageVector? = null, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick, modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(12.dp),
        color = Mk.bg2
    ) {
        Box(Modifier.fillMaxSize().border(1.dp, Mk.line, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(icon, null, tint = Mk.purple300, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                }
                Text(label, color = Mk.purple100, fontWeight = FontWeight.Medium, fontSize = 13.sp)
            }
        }
    }
}

// ---------- VipBadge (gold crown tag) ----------
@Composable
fun VipBadge(modifier: Modifier = Modifier, compact: Boolean = false) {
    val height = if (compact) 20.dp else 24.dp
    Box(
        modifier.height(height).clip(RoundedCornerShape(if (compact) 6.dp else 8.dp))
            .background(
                Brush.linearGradient(listOf(Mk.gold, Color(0xFFF59E0B)))
            ).padding(horizontal = if (compact) 6.dp else 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.WorkspacePremium, null, tint = Color(0xFF78350F), modifier = Modifier.size(if (compact) 11.dp else 13.dp))
            Spacer(Modifier.width(3.dp))
            Text("VIP", color = Color(0xFF78350F), fontSize = if (compact) 9.sp else 11.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp)
        }
    }
}

// ---------- TopAppBar custom ----------
@Composable
fun MkTopBar(title: String, subtitle: String? = null, onBack: (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {}) {
    Surface(color = Mk.bg, tonalElevation = 0.dp) {
        Row(
            Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.ArrowBack, null, tint = Mk.text, modifier = Modifier.size(22.dp))
                }
            } else {
                Spacer(Modifier.width(8.dp))
                // Logo mark
                Canvas(Modifier.size(26.dp)) {
                    drawCircle(
                        brush = Brush.linearGradient(listOf(Mk.purple, Mk.pink)),
                        radius = size.minDimension / 2f
                    )
                    drawCircle(color = Color.White, radius = size.minDimension / 2f * 0.35f, center = center)
                }
                Spacer(Modifier.width(10.dp))
            }
            Column {
                Text(title, color = Mk.text, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                if (subtitle != null) Text(subtitle, color = Mk.dim, fontSize = 11.sp)
            }
            Spacer(Modifier.weight(1f))
            actions()
        }
    }
}

// ---------- ToolTile for dock (icon tile, locked if pro) ----------
@Composable
fun ToolTile(
    icon: ImageVector,
    label: String,
    selected: Boolean = false,
    pro: Boolean = false,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(64.dp).clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(
            if (selected) Brush.linearGradient(listOf(Mk.purple600, Mk.pink))
            else SolidColor(Mk.bg3)
        ), contentAlignment = Alignment.Center) {
            Icon(icon, label, tint = if (selected) Color.White else Mk.purple300, modifier = Modifier.size(22.dp))
            if (pro) {
                VipBadge(Modifier.align(Alignment.TopEnd).offset(3.dp, (-3).dp), compact = true)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, color = if (selected) Color.White else Mk.dim, fontSize = 9.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Clip)
    }
}

// ---------- EffectTile ----------
@Composable
fun EffectTile(label: String, color1: Color, color2: Color = color1.copy(alpha = 0.5f), pro: Boolean = false, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(74.dp).clickable { onClick() }) {
        Box(Modifier.size(58.dp).clip(RoundedCornerShape(12.dp))
            .background(Brush.linearGradient(listOf(color1, color2)))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center) {
            // little motion lines
            Canvas(Modifier.fillMaxSize(0.7f)) {
                for (i in 0..3) {
                    drawLine(Color.White.copy(alpha = 0.35f),
                        start = Offset(0f, size.height * (0.2f + i * 0.18f)),
                        end = Offset(size.width, size.height * (0.2f + i * 0.18f)),
                        strokeWidth = 2f)
                }
            }
            if (pro) VipBadge(Modifier.align(Alignment.TopEnd).offset(4.dp, (-4).dp), compact = true)
        }
        Spacer(Modifier.height(5.dp))
        Text(label, color = Mk.text, fontSize = 10.sp, fontWeight = FontWeight.Medium, maxLines = 1, textAlign = TextAlign.Center)
    }
}

// ---------- MkSlider (custom track gradient) ----------
@Composable
fun MkSlider(
    value: Float,
    onValue: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    label: String
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = Mk.text, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.weight(1f))
            Text(String.format("%.0f", value.coerceIn(range.start, range.endInclusive)), color = Mk.purple300, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(2.dp))
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValue,
            valueRange = range.start..range.endInclusive,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Mk.purple,
                inactiveTrackColor = Mk.bg3
            )
        )
    }
}

// ---------- Chip ----------
@Composable
fun MkChip(label: String, selected: Boolean = false, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) Mk.purple else Mk.bg3,
        modifier = Modifier.border(1.dp, if (selected) Color.Transparent else Mk.line, RoundedCornerShape(10.dp))
    ) {
        Text(label, color = if (selected) Color.White else Mk.dim, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
    }
}

// ---------- Section label ----------
@Composable
fun SectionHeader(title: String, subtitle: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(title, color = Mk.text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(Modifier.width(8.dp))
        if (subtitle != null) Text(subtitle, color = Mk.dim, fontSize = 11.sp)
    }
}

// ---------- Project card ----------
@Composable
fun ProjectCard(
    title: String,
    subtitle: String,
    duration: String,
    gradient: List<Color>,
    onClick: () -> Unit
) {
    Box(
        Modifier.aspectRatio(9f / 14f).clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(gradient))
            .clickable { onClick() }
    ) {
        // little play triangle
        Box(Modifier.align(Alignment.Center).size(34.dp).clip(CircleShape)
            .background(Color.White.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.align(Alignment.BottomStart).padding(10.dp)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
        }
        Text(duration, color = Color.White.copy(alpha = 0.85f), fontSize = 10.sp, fontWeight = FontWeight.Medium,
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 3.dp))
    }
}
