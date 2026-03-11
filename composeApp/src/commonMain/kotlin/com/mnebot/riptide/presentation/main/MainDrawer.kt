package com.mnebot.riptide.presentation.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mnebot.riptide.domain.model.WorkBlock

private val OceanDeep = Color(0xD90A1628)
private val OceanMid = Color(0xD91B3A6B)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xB3FFFFFF)
private val CardBackground = Color(0x33FFFFFF)
private val SectionLabel = Color(0x80FFFFFF)
private val DividerColor = Color(0x33FFFFFF)

@Composable
fun MainDrawer(
    blocks: List<WorkBlock>,
    onAddTask: () -> Unit,
    onAddBlock: () -> Unit,
    onEditBlock: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(OceanDeep, OceanMid)
                )
            )
            .statusBarsPadding()
            .padding(vertical = 24.dp)
    ) {
        // Cabecera
        Row(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🌊", fontSize = 28.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Riptide",
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(color = DividerColor)
        Spacer(modifier = Modifier.height(20.dp))

        // Sección tareas
        SectionTitle("TAREAS")
        Spacer(modifier = Modifier.height(8.dp))
        DrawerItem(icon = "➕", label = "Añadir tarea", onClick = onAddTask)

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider(color = DividerColor)
        Spacer(modifier = Modifier.height(20.dp))

        // Sección bloques
        SectionTitle("BLOQUES")
        Spacer(modifier = Modifier.height(8.dp))

        blocks.forEach { block ->
            DrawerBlockItem(
                block = block,
                onClick = { onEditBlock(block.id) }
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        DrawerItem(icon = "➕", label = "Añadir bloque", onClick = onAddBlock)

        Spacer(modifier = Modifier.height(16.dp))

        // Asa visual para indicar que se puede cerrar deslizando hacia arriba
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
        ) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0x55FFFFFF))
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        color = SectionLabel,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(horizontal = 24.dp)
    )
}

@Composable
private fun DrawerItem(
    icon: String,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 18.sp)
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = label, color = TextSecondary, fontSize = 15.sp)
    }
}

@Composable
private fun DrawerBlockItem(
    block: WorkBlock,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(block.icon, fontSize = 18.sp)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = block.name,
            color = TextPrimary,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f)
        )
        Text("›", color = TextSecondary, fontSize = 20.sp)
    }
}