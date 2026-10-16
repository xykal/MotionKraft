package xyz.xyverse.motionkraft.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.xyverse.motionkraft.presentation.EditorViewModel
import xyz.xyverse.motionkraft.presentation.Mk
import xyz.xyverse.motionkraft.presentation.components.*

data class Template(
    val id: String,
    val title: String,
    val tag: String,
    val pro: Boolean,
    val gradient: List<Color>,
    val emoji: String
)

val TEMPLATES = listOf(
    Template("jj_beat", "Jedag Jedug Beat", "Trending", false, listOf(Mk.purple, Mk.pink), "\uD83D\uDD25"),
    Template("cinema_3d", "3D Cinematic", "Pro", true, listOf(Color(0xFF0EA5E9), Mk.purple), "\uD83C\uDFAC"),
    Template("lyric_karaoke", "Lirik Karaoke", "Music", false, listOf(Mk.pink, Color(0xFFF43F5E)), "\uD83C\uDFB5"),
    Template("motion_graph", "Motion Graph", "Pro", true, listOf(Mk.cyan, Mk.green), "\uD83D\uDCCA"),
    Template("morph", "Morph Transition", "Pro", true, listOf(Mk.gold, Mk.pink), "\u2728"),
    Template("slowmo", "Smooth Slowmo", "Cinematic", false, listOf(Color(0xFF334155), Mk.purple), "\uD83C\uDFDE"),
    Template("glitch", "Glitch Vibe", "EDM", false, listOf(Mk.pink, Mk.purple), "\uD83D\uDC7E"),
    Template("neon", "Neon Pulse", "Night", false, listOf(Color(0xFF10B981), Mk.cyan), "\uD83D\uDFE2")
)

@Composable
fun HomeScreen(
    vm: EditorViewModel,
    onNewProject: () -> Unit,
    onOpenTemplate: (String) -> Unit,
    onGoTemplates: () -> Unit,
    onGoProfile: () -> Unit,
    onGoVip: () -> Unit
) {
    val vip by vm.isVip.collectAsState()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // --- HEADER ---
        Box(Modifier.fillMaxWidth().background(
            Brush.verticalGradient(listOf(Mk.purple700, Mk.bg2, Mk.bg))
        )) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).clip(CircleShape).background(Mk.bg3)
                        .clickable { onGoProfile() }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, null, tint = Mk.purple300, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(vm.username, color = Mk.text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Buat video jedag jedug keren", color = Mk.purple100.copy(alpha = 0.7f), fontSize = 11.sp)
                    }
                    Spacer(Modifier.weight(1f))
                    if (vip) VipBadge() else {
                        Box(Modifier.clip(RoundedCornerShape(10.dp))
                            .background(Brush.linearGradient(listOf(Mk.gold, Color(0xFFF59E0B))))
                            .clickable { onGoVip() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.WorkspacePremium, null, tint = Color(0xFF78350F), modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Go VIP", color = Color(0xFF78350F), fontSize = 11.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                // NEW PROJECT BUTTON (large)
                Box(
                    Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(20.dp))
                        .background(Brush.linearGradient(listOf(Mk.purple, Mk.purple700, Mk.pink)))
                        .clickable { onNewProject() }) {
                    Row(Modifier.fillMaxSize().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("New Project", color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp)
                            Spacer(Modifier.height(6.dp))
                            Text("Mulai editing dari kosong atau import media kamu", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                        }
                        Box(Modifier.size(60.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(34.dp))
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                // Quick actions
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickAction(Icons.Default.VideoLibrary, "Draft", Modifier.weight(1f)) { onNewProject() }
                    QuickAction(Icons.Default.AutoAwesome, "AI", Modifier.weight(1f)) { onGoVip() }
                    QuickAction(Icons.Default.MusicNote, "Beat", Modifier.weight(1f)) { onNewProject() }
                    QuickAction(Icons.Default.CloudUpload, "Cloud", Modifier.weight(1f)) { if (!vip) onGoVip() }
                }
            }
        }

        // --- Templates trending ---
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Text("Template Trending", color = Mk.text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.weight(1f))
            Text("Lihat semua", color = Mk.purple300, fontSize = 12.sp, modifier = Modifier.clickable { onGoTemplates() })
        }
        Spacer(Modifier.height(8.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(TEMPLATES.take(6)) { tpl ->
                TemplateCard(tpl, onClick = { if (tpl.pro && !vip) onGoVip() else onOpenTemplate(tpl.id) })
            }
        }

        // --- Feature grid ---
        Spacer(Modifier.height(16.dp))
        SectionHeader("Alat Editing")
        Spacer(Modifier.height(4.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.fillMaxWidth().height(260.dp).padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val tools = listOf(
                Triple(Icons.Default.ViewInAr, "3D Cine", true),
                Triple(Icons.Default.FlashOn, "Jedag Jedug", false),
                Triple(Icons.Default.Filter, "Filter", false),
                Triple(Icons.Default.AutoAwesome, "Efek", false),
                Triple(Icons.Default.QueueMusic, "Lirik", true),
                Triple(Icons.Default.Transform, "Morph", true),
                Triple(Icons.Default.Timeline, "Motion\nGraph", true),
                Triple(Icons.Default.GraphicEq, "Audio", false)
            )
            items(tools) { (ic, lab, pro) ->
                FeatureTile(ic, lab, pro = pro && !vip) { if (pro && !vip) onGoVip() else onNewProject() }
            }
        }

        // --- Recent projects ---
        Spacer(Modifier.height(8.dp))
        SectionHeader("Project Terakhir")
        Spacer(Modifier.height(4.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items((0..4).map { i ->
                val titles = listOf("Jedag jedug", "Cinematic", "Reels", "Short", "Lyrics")
                val colors = listOf(Mk.purple, Mk.pink, Mk.cyan, Mk.gold, Color(0xFF334155))
                Triple("Project ${i+1}", titles[i], colors[i])
            }) { (t, sub, c) ->
                ProjectCard(t, sub, "0:${15+i}", listOf(c, c.copy(alpha = 0.5f))) {}
            }
        }
        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun QuickAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.height(52.dp).clip(RoundedCornerShape(12.dp))
            .background(Mk.bg2)
            .border(1.dp, Mk.line, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Mk.purple300, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, color = Mk.text, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun FeatureTile(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, pro: Boolean = false, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Box(Modifier.size(54.dp).clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(listOf(Mk.purple700, Mk.bg3)))
            .border(1.dp, Mk.purple.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Mk.purple100, modifier = Modifier.size(24.dp))
            if (pro) VipBadge(Modifier.align(Alignment.TopEnd).offset(4.dp, (-4).dp), compact = true)
        }
        Spacer(Modifier.height(4.dp))
        Text(label, color = Mk.text, fontSize = 10.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, lineHeight = 12.sp)
    }
}

@Composable
fun TemplateCard(tpl: Template, onClick: () -> Unit) {
    Box(
        Modifier.width(130.dp).aspectRatio(9f/14f).clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(tpl.gradient))
            .clickable { onClick() }
    ) {
        Text(tpl.emoji, fontSize = 38.sp, modifier = Modifier.align(Alignment.Center))
        Text(tpl.tag.uppercase(), color = Color.White.copy(alpha = 0.9f), fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp,
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 3.dp))
        if (tpl.pro) VipBadge(Modifier.align(Alignment.TopEnd).padding(8.dp), compact = true)
        Column(Modifier.align(Alignment.BottomStart).padding(10.dp)) {
            Text(tpl.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Tap to apply", color = Color.White.copy(alpha = 0.8f), fontSize = 9.sp)
        }
    }
}
