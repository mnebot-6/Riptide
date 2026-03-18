package com.mnebot.riptide.presentation.task

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mnebot.riptide.domain.model.DayTask
import com.mnebot.riptide.presentation.components.DateInputField
import com.mnebot.riptide.presentation.components.TimeInputField
import com.mnebot.riptide.presentation.main.currentDate
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

private val OceanDeep = Color(0xFF0A1628)
private val OceanMid = Color(0xFF1B3A6B)
private val CardBackground = Color(0x33FFFFFF)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xB3FFFFFF)
private val SectionLabel = Color(0x80FFFFFF)

@Composable
fun PostponeSheet(
    task: DayTask,
    onPostpone: (LocalDate, LocalTime?) -> Unit,
    onDismiss: () -> Unit
) {
    var date by remember { mutableStateOf<LocalDate?>(currentDate()) }
    var time by remember { mutableStateOf<LocalTime?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(OceanDeep, OceanMid)),
                RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            )
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp, bottom = 32.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color(0x55FFFFFF))
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Posponer tarea", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(task.title, color = TextSecondary, fontSize = 14.sp)

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "NUEVA FECHA",
                color = SectionLabel,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            DateInputField(
                value = date,
                onValueChange = { date = it },
                nullable = false
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "NUEVA HORA (opcional)",
                color = SectionLabel,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            TimeInputField(
                value = time,
                onValueChange = { time = it },
                nullable = true,
                compact = false
            )

            if (error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(error!!, color = Color(0xFFEA4335), fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardBackground)
                        .clickable { onDismiss() }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) { Text("Cancelar", color = TextSecondary, fontSize = 15.sp) }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1A73E8))
                        .clickable {
                            if (date == null) {
                                error = "La fecha es obligatoria"
                                return@clickable
                            }
                            onPostpone(date!!, time)
                        }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Posponer",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}