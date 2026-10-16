package xyz.xyverse.motionkraft.presentation.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import xyz.xyverse.motionkraft.presentation.Mk
import xyz.xyverse.motionkraft.presentation.components.*

@Composable
fun TemplatesScreen(onBack: () -> Unit, onPick: (String) -> Unit) {
    val cats = listOf("Semua", "Trending", "Jedag Jedug", "Cinematic", "Music", "Lirik", "3D", "Glitch")
    var cat by remember { mutableStateOf("Semua") }
    Column(Modifier.fillMaxSize().background(Mk.bg)) {
        MkTopBar("Templates", subtitle = "${TEMPLATES.size} template tersedia", onBack = onBack)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.padding(horizontal = 12.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            cats.forEach { c -> MkChip(c, selected = c == cat) { cat = c } }
        }
        Spacer(Modifier.height(10.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(TEMPLATES + TEMPLATES.map { it.copy(id = it.id + "_2") }) { tpl ->
                TemplateCard(tpl, onClick = { onPick(tpl.id) })
            }
        }
    }
}
