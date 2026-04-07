package com.mnebot.riptide.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0x80FFFFFF)
private val SelectedGlow = Color(0x337EC8E3)

@Composable
fun RiptideBottomBar(
    selectedTab: BottomNavTab,
    onTabSelected: (BottomNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0x000A1628), Color(0xCC0A1628))
                )
            )
            .navigationBarsPadding()
            .height(56.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomNavTab.entries.forEach { tab ->
            val isSelected = tab == selectedTab
            BottomNavItem(
                tab = tab,
                isSelected = isSelected,
                onClick = { onTabSelected(tab) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    tab: BottomNavTab,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .semantics(mergeDescendants = true) {}
            .clickable(
                indication = null,
                interactionSource = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .then(
                    if (isSelected) Modifier.background(SelectedGlow).padding(horizontal = 12.dp, vertical = 2.dp)
                    else Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(tab.icon),
                contentDescription = stringResource(tab.labelRes),
                tint = if (isSelected) TextPrimary else TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = stringResource(tab.labelRes),
            color = if (isSelected) TextPrimary else TextSecondary,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
