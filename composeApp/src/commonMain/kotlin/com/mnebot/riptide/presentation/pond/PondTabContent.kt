package com.mnebot.riptide.presentation.pond

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.mnebot.riptide.domain.model.EcosystemState
import com.mnebot.riptide.domain.model.MarineCategory
import com.mnebot.riptide.domain.model.MarineCreature
import com.mnebot.riptide.presentation.aquarium.CreatureDetailDialog
import com.mnebot.riptide.presentation.aquarium.CreatureFreezeState
import com.mnebot.riptide.presentation.aquarium.CreaturePosition
import com.mnebot.riptide.presentation.aquarium.CreatureSpec
import com.mnebot.riptide.presentation.aquarium.allCreatures
import com.mnebot.riptide.presentation.aquarium.findHitCreature
import com.mnebot.riptide.presentation.theme.rememberAdaptiveCardColor
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import riptide.composeapp.generated.resources.*

private val TextPrimary = Color(0xFFFFFFFF)

@Composable
fun PondTabContent(
    creaturesData: List<MarineCreature>,
    creaturePositions: State<List<CreaturePosition>>,
    creatureFreezeState: CreatureFreezeState,
    selectedCreature: Pair<MarineCreature, CreatureSpec>?,
    ecosystemByCategory: Map<MarineCategory, EcosystemState> = emptyMap(),
    onCreatureTap: (MarineCreature, CreatureSpec) -> Unit,
    onCreatureDismiss: () -> Unit,
    onCreatureNicknameChanged: (String, String) -> Unit,
    onNavigateToEcosystem: () -> Unit,
    modifier: Modifier = Modifier
) {
    val adaptiveBg = rememberAdaptiveCardColor()

    Box(modifier = modifier.fillMaxSize()) {
        // Tap interceptor: only consumes the up event when it lands on a creature.
        // Drags and taps on empty space are ignored, so the parent HorizontalPager
        // handles tab swipes and the FAB rendered above still receives clicks.
        Box(
            modifier = Modifier
                .fillMaxSize()
                // Keyed on `creaturesData` only: `creaturePositions` is rewritten every
                // frame by the aquarium draw pass, and keying on it restarted this gesture
                // handler ~60x/s, so no tap ever reached its up event.
                .pointerInput(creaturesData) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val hit = findHitCreature(down.position, creaturePositions.value)
                            ?: return@awaitEachGesture
                        // Wait for the up. If the user drags or another node consumes,
                        // `waitForUpOrCancellation` returns null and we skip — the parent
                        // pager keeps the gesture for tab navigation.
                        val up = waitForUpOrCancellation() ?: return@awaitEachGesture
                        val spec = allCreatures.firstOrNull { it.species == hit }
                            ?: return@awaitEachGesture
                        val creature = creaturesData.firstOrNull { it.species == hit }
                            ?: return@awaitEachGesture
                        creatureFreezeState.freeze(hit)
                        onCreatureTap(creature, spec)
                        up.consume()
                    }
                }
        )

        FloatingActionButton(
            onClick = onNavigateToEcosystem,
            containerColor = adaptiveBg.copy(alpha = 1f),
            contentColor = TextPrimary,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 16.dp, bottom = 72.dp)
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_fish),
                contentDescription = stringResource(Res.string.btn_my_ecosystem),
                modifier = Modifier.size(22.dp)
            )
        }

        selectedCreature?.let { (creature, spec) ->
            CreatureDetailDialog(
                creature = creature,
                spec = spec,
                categoryState = ecosystemByCategory[spec.category],
                onDismiss = onCreatureDismiss,
                onNicknameChanged = { nickname ->
                    onCreatureNicknameChanged(creature.id, nickname)
                }
            )
        }
    }
}
