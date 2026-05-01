package com.mnebot.riptide.presentation.navigation

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DotSelected = Color(0xFFFFFFFF)
private val DotUnselected = Color(0x66FFFFFF)

@Composable
fun RiptidePagerIndicator(
    pageCount: Int,
    selectedIndex: Int,
    onDotClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 10.dp, top = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            val isSelected = index == selectedIndex
            val dotWidth by animateDpAsState(
                targetValue = if (isSelected) 18.dp else 6.dp,
                animationSpec = tween(durationMillis = 220),
                label = "dotWidth"
            )
            Box(
                modifier = Modifier
                    .size(width = dotWidth, height = 6.dp)
                    .clip(if (isSelected) RoundedCornerShape(3.dp) else CircleShape)
                    .background(if (isSelected) DotSelected else DotUnselected)
                    .clickable(
                        indication = null,
                        interactionSource = null,
                        onClick = { onDotClick(index) }
                    )
            )
            if (index < pageCount - 1) {
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}
