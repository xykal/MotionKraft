package xyz.xyverse.motionkraft.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.xyverse.motionkraft.presentation.Mk
import xyz.xyverse.motionkraft.presentation.components.*

data class VipPlan(val id: String, val name: String, val price: String, val per: String, val highlight: Boolean = false, val oldPrice: String? = null)

@Composable
fun VipScreen(onBack: () -> Unit, onSubscribe: (String) -> Unit) {
    val plans = listOf(
        VipPlan("weekly", "Mingguan", "Rp 9.900", "/minggu"),
        VipPlan("monthly", "Bulanan", "Rp 29.900", "/bulan", highlight = true, oldPrice = "Rp 49.000"),
        VipPlan("yearly", "Tahunan", "Rp 199.000", "/tahun (hemat 45%)", oldPrice = "Rp 358.800")
    )
    var selected by remember { mutableStateOf("monthly") }

    Column(Modifier.fillMaxSize().background(Mk.bg)) {
        MkTopBar("MotionKraft VIP", onBack = onBack) {
            IconButton(onClick = onBack) { Icon(Icons.Default.Close, null, tint = Mk.text, modifier = Modifier.size(22.dp)) }
        }
        Column(Modifier.verticalScroll(rememberScrollState()).weight(1f)) {
            // Hero banner
            Box(Modifier.padding(horizontal = 16.dp, vertical = 6.dp).fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Brush.linearGradient(listOf(Mk.purple700, Mk.pink, Mk.gold)))
                .padding(20.dp)) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        VipBadge()
                        Spacer(Modifier.width(8.dp))
                        Text("UNLOCK SEMUA FITUR", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp, letterSpacing = 1.sp)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("Jadi kreator profesional", color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Tanpa watermark, template premium tak terbatas, efek pro, 3D cinematic, morph, motion graph, cloud storage, export 4K 60fps.", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
                }
            }

            // Benefits
            Spacer(Modifier.height(14.dp))
            val perks = listOf(
                Icons.Default.AutoAwesome to "1000+ template premium (JJ, cinematic, lirik)",
                Icons.Default.ViewInAr to "3D Camera & Cinematic Moving",
                Icons.Default.Transform to "Morph & Motion Graph",
                Icons.Default.CloudUpload to "Cloud storage proyek tak terbatas",
                Icons.Default.Hd to "Export 4K 60fps tanpa watermark",
                Icons.Default.Block to "No ads sepenuhnya"
            )
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                perks.forEach { (ic, t) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(28.dp).clip(CircleShape).background(Mk.purple.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                            Icon(ic, null, tint = Mk.purple300, modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(t, color = Mk.text, fontSize = 13.sp)
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            SectionHeader("Pilih paket")
            Row(Modifier.padding(horizontal = 14.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                plans.forEach { p ->
                    val sel = selected == p.id
                    Box(
                        Modifier.weight(1f).aspectRatio(0.78f).clip(RoundedCornerShape(16.dp))
                            .background(if (sel) Brush.linearGradient(listOf(Mk.purple, Mk.pink)) else Brush.linearGradient(listOf(Mk.bg2, Mk.bg2)))
                            .border(
                                width = if (sel) 2.dp else 1.dp,
                                color = if (sel) Mk.gold else Mk.line,
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { selected = p.id },
                        contentAlignment = Alignment.Center
                    ) {
                        if (p.highlight) {
                            Box(Modifier.align(Alignment.TopCenter).offset(y = (-8).dp)
                                .clip(RoundedCornerShape(6.dp)).background(Mk.gold).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                Text("POPULER", color = Color(0xFF78350F), fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp)
                            }
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 10.dp)) {
                            Text(p.name.uppercase(), color = if (sel) Color.White else Mk.dim, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                            Spacer(Modifier.height(10.dp))
                            if (p.oldPrice != null) {
                                Text(p.oldPrice, color = if (sel) Color.White.copy(alpha = 0.6f) else Mk.dim2, fontSize = 11.sp,
                                    textDecoration = TextDecoration.LineThrough)
                            }
                            Text(p.price, color = if (sel) Color.White else Mk.text, fontWeight = FontWeight.Black, fontSize = 16.sp)
                            Text(p.per, color = if (sel) Color.White.copy(alpha = 0.8f) else Mk.dim, fontSize = 10.sp)
                            Spacer(Modifier.height(8.dp))
                            if (sel) {
                                Box(Modifier.size(20.dp).clip(CircleShape).background(Mk.gold), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Check, null, tint = Color(0xFF78350F), modifier = Modifier.size(14.dp))
                                }
                            } else {
                                Box(Modifier.size(20.dp).clip(CircleShape).border(2.dp, Mk.line, CircleShape))
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("Langganan akan diperpanjang otomatis. Batalkan kapan saja dari Google Play.", color = Mk.dim, fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp))
            Spacer(Modifier.height(16.dp))
        }
        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).background(Mk.bg)) {
            MkButton("Langganan Sekarang", icon = Icons.Default.WorkspacePremium, modifier = Modifier.fillMaxWidth()) {
                onSubscribe(selected)
            }
        }
    }
}
