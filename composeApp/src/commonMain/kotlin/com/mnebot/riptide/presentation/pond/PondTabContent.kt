package com.mnebot.riptide.presentation.pond

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mnebot.riptide.domain.model.EcosystemState
import com.mnebot.riptide.domain.model.MarineCategory
import com.mnebot.riptide.domain.model.MarineCreature
import com.mnebot.riptide.presentation.aquarium.CreatureDetailDialog
import com.mnebot.riptide.presentation.aquarium.CreatureFreezeState
import com.mnebot.riptide.presentation.aquarium.CreatureSpec
import org.jetbrains.compose.resources.painterResource
import com.mnebot.riptide.presentation.theme.rememberAdaptiveCardColor
import org.jetbrains.compose.resources.stringResource
import riptide.composeapp.generated.resources.*

private val TextPrimary = Color(0xFFFFFFFF)

@Composable
fun PondTabContent(
    ecosystemByCategory: Map<MarineCategory, EcosystemState>,
    creaturesData: List<MarineCreature>,
    creatureFreezeState: CreatureFreezeState,
    selectedCreature: Pair<MarineCreature, CreatureSpec>?,
    onCreatureTap: (MarineCreature, CreatureSpec) -> Unit,
    onCreatureDismiss: () -> Unit,
    onCreatureNicknameChanged: (String, String) -> Unit,
    onNavigateToEcosystem: () -> Unit,
    modifier: Modifier = Modifier
) {
    var fabExpanded by remember { mutableStateOf(false) }
    val adaptiveBg = rememberAdaptiveCardColor()

    // Full-screen aquarium — background is rendered by MainShellScreen
    Box(modifier = modifier.fillMaxSize()) {
        // Dismiss expanded FAB on tap anywhere
        if (fabExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(indication = null, interactionSource = null) {
                        fabExpanded = false
                    }
            )
        }

        // FAB menu at bottom-right
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 16.dp, bottom = 72.dp)
        ) {
            // Expandable option: Criaturas
            AnimatedVisibility(
                visible = fabExpanded,
                enter = fadeIn(tween(150)) + slideInVertically(tween(150)) { it / 2 },
                exit = fadeOut(tween(100)) + slideOutVertically(tween(100)) { it / 2 }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Text(
                        stringResource(Res.string.btn_my_ecosystem),
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(adaptiveBg)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    SmallFloatingActionButton(
                        onClick = {
                            fabExpanded = false
                            onNavigateToEcosystem()
                        },
                        containerColor = adaptiveBg,
                        contentColor = TextPrimary,
                        shape = CircleShape
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_fish),
                            contentDescription = stringResource(Res.string.btn_my_ecosystem),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Main FAB
            FloatingActionButton(
                onClick = { fabExpanded = !fabExpanded },
                containerColor = adaptiveBg,
                contentColor = TextPrimary,
                shape = CircleShape
            ) {
                Icon(
                    painter = painterResource(if (fabExpanded) Res.drawable.ic_x else Res.drawable.ic_plus),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Creature detail dialog
        selectedCreature?.let { (creature, spec) ->
            CreatureDetailDialog(
                creature = creature,
                spec = spec,
                onDismiss = onCreatureDismiss,
                onNicknameChanged = { nickname ->
                    onCreatureNicknameChanged(creature.id, nickname)
                }
            )
        }
    }
}
