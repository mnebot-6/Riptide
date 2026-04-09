package com.mnebot.riptide.presentation.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mnebot.riptide.presentation.history.HistoryScreen
import com.mnebot.riptide.presentation.history.HistoryUiState
import com.mnebot.riptide.presentation.stats.StatsScreen
import com.mnebot.riptide.presentation.stats.StatsRange
import com.mnebot.riptide.presentation.stats.StatsUiState
import org.jetbrains.compose.resources.stringResource
import riptide.composeapp.generated.resources.*

private val OceanDeep = Color(0xFF0A1628)
private val OceanMid = Color(0xFF1B3A6B)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0x80FFFFFF)
private val CardBackground = Color(0x22FFFFFF)
private val SelectedBackground = Color(0xFF1A73E8)

enum class ProgressSubTab { STATS, HISTORY }

@Composable
fun ProgressTabContent(
    statsUiState: StatsUiState,
    historyUiState: HistoryUiState,
    onRangeSelected: (StatsRange) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onBlockFilterChanged: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSubTab by rememberSaveable { mutableStateOf(ProgressSubTab.STATS) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(OceanDeep, OceanMid)))
    ) {
        // Sub-tab toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            ProgressToggle(
                label = stringResource(Res.string.btn_stats),
                isSelected = selectedSubTab == ProgressSubTab.STATS,
                onClick = { selectedSubTab = ProgressSubTab.STATS }
            )
            Spacer(modifier = Modifier.width(8.dp))
            ProgressToggle(
                label = stringResource(Res.string.btn_history),
                isSelected = selectedSubTab == ProgressSubTab.HISTORY,
                onClick = { selectedSubTab = ProgressSubTab.HISTORY }
            )
        }

        // Content
        when (selectedSubTab) {
            ProgressSubTab.STATS -> StatsScreen(
                uiState = statsUiState,
                onRangeSelected = onRangeSelected,
                onNavigateBack = null
            )
            ProgressSubTab.HISTORY -> HistoryScreen(
                uiState = historyUiState,
                onNavigateBack = null,
                onSearchQueryChanged = onSearchQueryChanged,
                onBlockFilterChanged = onBlockFilterChanged
            )
        }
    }
}

@Composable
private fun ProgressToggle(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) SelectedBackground else CardBackground)
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) TextPrimary else TextSecondary,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
