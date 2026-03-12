package com.mnebot.riptide.presentation.task

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import com.mnebot.riptide.domain.model.DayTask
import com.mnebot.riptide.presentation.main.currentDate
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime

private val OceanDeep = Color(0xFF0A1628)
private val OceanMid = Color(0xFF1B3A6B)
private val CardBackground = Color(0x33FFFFFF)
private val CardBorder = Color(0x55FFFFFF)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xB3FFFFFF)
private val SectionLabel = Color(0x80FFFFFF)

@Composable
fun PostponeSheet(
    task: DayTask,
    onPostpone: (LocalDateTime) -> Unit,
    onDismiss: () -> Unit
) {
    var dateText by remember { mutableStateOf(currentDate().toString()) }
    var timeDigits by remember { mutableStateOf("") }
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

            Text("NUEVA FECHA", color = SectionLabel, fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp)
            Spacer(modifier = Modifier.height(8.dp))
            BasicTextField(
                value = dateText,
                onValueChange = { dateText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardBackground)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                textStyle = TextStyle(color = TextPrimary, fontSize = 15.sp),
                decorationBox = { inner ->
                    if (dateText.isEmpty()) Text("YYYY-MM-DD", color = SectionLabel, fontSize = 15.sp)
                    inner()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("NUEVA HORA", color = SectionLabel, fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp)
            Spacer(modifier = Modifier.height(8.dp))
            BasicTextField(
                value = timeDigits,
                onValueChange = { input ->
                    timeDigits = input.filter { it.isDigit() }.take(4)
                },
                modifier = Modifier
                    .width(80.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CardBackground)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                textStyle = TextStyle(
                    color = TextPrimary, fontSize = 16.sp,
                    textAlign = TextAlign.Center, fontWeight = FontWeight.Medium
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                decorationBox = { inner ->
                    if (timeDigits.isEmpty()) Text("----", color = SectionLabel, fontSize = 16.sp,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    inner()
                }
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
                            val date = runCatching { LocalDate.parse(dateText) }.getOrNull()
                            if (date == null) { error = "Fecha no válida"; return@clickable }
                            if (timeDigits.length < 4) { error = "Hora requerida"; return@clickable }
                            val h = timeDigits.substring(0, 2).toIntOrNull() ?: 0
                            val m = timeDigits.substring(2, 4).toIntOrNull() ?: 0
                            if (h !in 0..23 || m !in 0..59) { error = "Hora no válida"; return@clickable }
                            onPostpone(LocalDateTime(date, LocalTime(h, m)))
                        }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) { Text("Posponer", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}