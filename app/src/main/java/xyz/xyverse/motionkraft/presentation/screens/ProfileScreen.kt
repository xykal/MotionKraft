package xyz.xyverse.motionkraft.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.xyverse.motionkraft.presentation.Mk
import xyz.xyverse.motionkraft.presentation.components.*

@Composable
fun ProfileScreen(vip: Boolean, onBack: () -> Unit, onGoVip: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Mk.bg)) {
        MkTopBar("Profil", onBack = onBack)
        Column(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())) {
            // Header card
            Box(
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (vip) Brush.linearGradient(listOf(Mk.gold, Color(0xFFF59E0B), Mk.pink))
                                else Brush.linearGradient(listOf(Mk.purple700, Mk.purple, Mk.pink)))
                    .padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(64.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("MotionKraft User", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
                        Text(if (vip) "VIP Member" else "Free Plan", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                        Spacer(Modifier.height(6.dp))
                        if (!vip) {
                            Box(Modifier.clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = 0.3f))
                                .clickable { onGoVip() }
                                .padding(horizontal = 10.dp, vertical = 5.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.WorkspacePremium, null, tint = Mk.gold, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Upgrade VIP", color = Mk.gold, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        } else {
                            VipBadge()
                        }
                    }
                }
            }

            // Stats
            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Stat("24", "Projects", Modifier.weight(1f))
                Stat("1.2k", "Exports", Modifier.weight(1f))
                Stat("38", "Templates", Modifier.weight(1f))
            }

            // Menu items
            Spacer(Modifier.height(8.dp))
            MenuItem(Icons.Default.Settings, "Pengaturan")
            MenuItem(Icons.Default.Cloud, if (vip) "Cloud Storage" else "Cloud (VIP)")
            MenuItem(Icons.Default.WorkspacePremium, "Kelola Langganan") { onGoVip() }
            MenuItem(Icons.Default.Help, "Bantuan & FAQ")
            MenuItem(Icons.Default.Info, "Tentang MotionKraft")
            MenuItem(Icons.Default.Share, "Bagikan ke Teman")

            Spacer(Modifier.height(40.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Bolt, null, tint = Mk.purple400, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("Powered by MotionKraft", color = Mk.dim, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Stat(num: String, label: String, modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(14.dp)).background(Mk.bg2).border(1.dp, Mk.line, RoundedCornerShape(14.dp)).padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(num, color = Mk.text, fontWeight = FontWeight.Black, fontSize = 18.sp)
            Text(label, color = Mk.dim, fontSize = 10.sp)
        }
    }
}

@Composable
private fun MenuItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Mk.purple300, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Text(label, color = Mk.text, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, null, tint = Mk.dim2, modifier = Modifier.size(18.dp))
    }
    Box(Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(1.dp).background(Mk.line))
}
