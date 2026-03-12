package com.mnebot.riptide.presentation.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mnebot.riptide.domain.model.WorkBlock
import kotlinx.datetime.LocalTime

private val OceanDeep = Color(0xD90A1628)
private val OceanMid = Color(0xD91B3A6B)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xB3FFFFFF)
private val SectionLabel = Color(0x80FFFFFF)
private val DividerColor = Color(0x33FFFFFF)

@Composable
fun MainDrawer(
    blocks: List<WorkBlock>,
    nightSummaryTime: LocalTime,
    onAddTask: () -> Unit,
    onAddBlock: () -> Unit,
    onEditBlock: (String) -> Unit,
    onNightSummaryTimeChanged: (LocalTime) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(OceanDeep, OceanMid)))
            .statusBarsPadding()
            .padding(vertical = 24.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🌊", fontSize = 28.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Text("Riptide", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(color = DividerColor)
        Spacer(modifier = Modifier.height(20.dp))

        SectionTitle("TAREAS")
        Spacer(modifier = Modifier.height(8.dp))
        DrawerItem(icon = "➕", label = "Añadir tarea", onClick = onAddTask)

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider(color = DividerColor)
        Spacer(modifier = Modifier.height(20.dp))

        SectionTitle("BLOQUES")
        Spacer(modifier = Modifier.height(8.dp))
        blocks.forEach { block ->
            DrawerBlockItem(block = block, onClick = { onEditBlock(block.id) })
        }
        Spacer(modifier = Modifier.height(4.dp))
        DrawerItem(icon = "➕", label = "Añadir bloque", onClick = onAddBlock)

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider(color = DividerColor)
        Spacer(modifier = Modifier.height(20.dp))

        SectionTitle("AJUSTES")
        Spacer(modifier = Modifier.height(12.dp))
        NightSummaryTimeSetting(
            currentTime = nightSummaryTime,
            onTimeChanged = onNightSummaryTimeChanged
        )

        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
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
private fun NightSummaryTimeSetting(
    currentTime: LocalTime,
    onTimeChanged: (LocalTime) -> Unit
) {
    var time by remember(currentTime) { mutableStateOf<LocalTime?>(currentTime) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("🌙", fontSize = 18.sp)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "Resumen nocturno",
            color = TextSecondary,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f)
        )
        DrawerTimeTextField(
            time = time,
            onChange = { newTime ->
                time = newTime
                if (newTime != null) onTimeChanged(newTime)
            }
        )
    }
}

@Composable
private fun DrawerTimeTextField(
    time: LocalTime?,
    onChange: (LocalTime?) -> Unit
) {
    var text by remember(time) {
        mutableStateOf(
            time?.let {
                "${it.hour.toString().padStart(2, '0')}${it.minute.toString().padStart(2, '0')}"
            } ?: ""
        )
    }
    val isValid = text.length != 4 || run {
        val h = text.substring(0, 2).toIntOrNull() ?: -1
        val m = text.substring(2, 4).toIntOrNull() ?: -1
        h in 0..23 && m in 0..59
    }

    Column(horizontalAlignment = Alignment.End) {
        BasicTextField(
            value = text,
            onValueChange = { input ->
                val digits = input.filter { it.isDigit() }.take(4)
                text = digits
                if (digits.length == 4) {
                    val h = digits.substring(0, 2).toIntOrNull()
                    val m = digits.substring(2, 4).toIntOrNull()
                    if (h != null && m != null && h in 0..23 && m in 0..59) {
                        onChange(LocalTime(h, m))
                    } else {
                        onChange(null)
                    }
                } else {
                    onChange(null)
                }
            },
            modifier = Modifier
                .width(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isValid) Color(0x22FFFFFF) else Color(0x33EA4335))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            textStyle = TextStyle(
                color = if (isValid) TextPrimary else Color(0xFFEA4335),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            decorationBox = { inner ->
                if (text.isEmpty()) {
                    Text(
                        "--:--",
                        color = SectionLabel,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                inner()
            }
        )
        if (!isValid) {
            Text("Hora inválida", color = Color(0xFFEA4335), fontSize = 10.sp)
        }
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
private fun DrawerItem(icon: String, label: String, onClick: () -> Unit) {
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
private fun DrawerBlockItem(block: WorkBlock, onClick: () -> Unit) {
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