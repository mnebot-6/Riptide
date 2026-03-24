package com.mnebot.riptide.presentation.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mnebot.riptide.domain.model.WorkBlock
import com.mnebot.riptide.presentation.components.TimeInputField
import kotlinx.datetime.LocalTime
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import riptide.composeapp.generated.resources.*

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
    morningReminderTime: LocalTime?,
    onAddBlock: () -> Unit,
    onEditBlock: (String) -> Unit,
    onNightSummaryTimeChanged: (LocalTime) -> Unit,
    onMorningReminderTimeChanged: (LocalTime?) -> Unit,
    onNavigateToEcosystem: () -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    val screenHeight = with(androidx.compose.ui.platform.LocalDensity.current) {
        LocalWindowInfo.current.containerSize.height.toDp()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = screenHeight * 0.85f)
            .background(Brush.verticalGradient(listOf(OceanDeep, OceanMid)))
            .statusBarsPadding()
            .padding(vertical = 24.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_waves),
                contentDescription = null,
                tint = TextPrimary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(stringResource(Res.string.app_name), color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(color = DividerColor)
        Spacer(modifier = Modifier.height(20.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
        ) {
            SectionTitle(stringResource(Res.string.section_blocks))
            Spacer(modifier = Modifier.height(8.dp))
            blocks.forEach { block ->
                DrawerBlockItem(block = block, onClick = { onEditBlock(block.id) })
            }
            DrawerItem(painter = painterResource(Res.drawable.ic_plus), label = stringResource(Res.string.btn_add_block), onClick = onAddBlock)

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = DividerColor)
            Spacer(modifier = Modifier.height(20.dp))

            SectionTitle(stringResource(Res.string.section_ecosystem))
            Spacer(modifier = Modifier.height(8.dp))
            DrawerItem(painter = painterResource(Res.drawable.ic_fish), label = stringResource(Res.string.btn_my_ecosystem), onClick = onNavigateToEcosystem)

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = DividerColor)
            Spacer(modifier = Modifier.height(20.dp))

            SectionTitle(stringResource(Res.string.section_progress))
            Spacer(modifier = Modifier.height(8.dp))
            DrawerItem(painter = painterResource(Res.drawable.ic_bar_chart), label = stringResource(Res.string.btn_stats), onClick = onNavigateToStats)
            DrawerItem(painter = painterResource(Res.drawable.ic_history), label = stringResource(Res.string.btn_history), onClick = onNavigateToHistory)

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = DividerColor)
            Spacer(modifier = Modifier.height(20.dp))

            SectionTitle(stringResource(Res.string.section_settings))
            Spacer(modifier = Modifier.height(12.dp))
            NightSummaryTimeSetting(
                currentTime = nightSummaryTime,
                onTimeChanged = onNightSummaryTimeChanged
            )
            Spacer(modifier = Modifier.height(4.dp))
            MorningReminderSetting(
                currentTime = morningReminderTime,
                onTimeChanged = onMorningReminderTimeChanged
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
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
private fun NightSummaryTimeSetting(currentTime: LocalTime, onTimeChanged: (LocalTime) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_moon),
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(stringResource(Res.string.label_night_summary), color = TextSecondary, fontSize = 15.sp, modifier = Modifier.weight(1f))
        TimeInputField(
            value = currentTime,
            onValueChange = { it?.let { t -> onTimeChanged(t) } },
            nullable = false,
            compact = true
        )
    }
}

@Composable
private fun MorningReminderSetting(currentTime: LocalTime?, onTimeChanged: (LocalTime?) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_sun),
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            stringResource(Res.string.label_morning_reminder),
            color = TextSecondary,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f)
        )
        if (currentTime != null) {
            TimeInputField(
                value = currentTime,
                onValueChange = { it?.let { t -> onTimeChanged(t) } },
                nullable = false,
                compact = true
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Switch(
            checked = currentTime != null,
            onCheckedChange = { enabled ->
                onTimeChanged(if (enabled) LocalTime(8, 0) else null)
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF4A90D9),
                uncheckedThumbColor = Color(0x99FFFFFF),
                uncheckedTrackColor = Color(0x33FFFFFF)
            )
        )
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
private fun DrawerItem(painter: Painter, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painter,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = label, color = TextSecondary, fontSize = 15.sp)
    }
}

@Composable
private fun DrawerBlockItem(block: WorkBlock, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(block.icon, fontSize = 18.sp)
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = block.name, color = TextPrimary, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Text("›", color = TextSecondary, fontSize = 20.sp)
    }
}