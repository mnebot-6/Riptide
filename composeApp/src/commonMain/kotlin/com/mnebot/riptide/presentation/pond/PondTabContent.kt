package com.mnebot.riptide.presentation.pond

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import org.jetbrains.compose.resources.stringResource
import riptide.composeapp.generated.resources.*

private val CardBackground = Color(0x44FFFFFF)
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
    // Full-screen aquarium — background is rendered by MainShellScreen
    // Only show the ecosystem button overlay
    Box(modifier = modifier.fillMaxSize()) {
        // Ecosystem collection button at top-right
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 16.dp, end = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(CardBackground)
                .clickable { onNavigateToEcosystem() }
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(Res.drawable.ic_box),
                    contentDescription = stringResource(Res.string.btn_my_ecosystem),
                    tint = TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    stringResource(Res.string.btn_my_ecosystem),
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
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
