package com.mnebot.riptide.presentation.aquarium

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mnebot.riptide.domain.model.CreatureRarity
import com.mnebot.riptide.domain.model.CreatureSpecies
import com.mnebot.riptide.domain.model.DecorationProgress
import com.mnebot.riptide.domain.model.EcosystemState
import com.mnebot.riptide.domain.model.MarineCategory
import com.mnebot.riptide.domain.model.MarineCreature
import com.mnebot.riptide.presentation.displayNameRes
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import riptide.composeapp.generated.resources.*

private val OceanDeep = Color(0xFF0A1628)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xB3FFFFFF)
private val SectionLabel = Color(0x80FFFFFF)
private val Accent = Color(0xFF7EC8E3)
private val DividerColor = Color(0x33FFFFFF)

private fun rarityColor(rarity: CreatureRarity): Color = when (rarity) {
    CreatureRarity.COMMON    -> Color(0xFF9E9E9E)
    CreatureRarity.UNCOMMON  -> Color(0xFF4CAF50)
    CreatureRarity.RARE      -> Color(0xFF2196F3)
    CreatureRarity.EPIC      -> Color(0xFF9C27B0)
    CreatureRarity.LEGENDARY -> Color(0xFFFF9800)
}

@Composable
fun EcosystemScreen(
    ecosystemByCategory: Map<MarineCategory, EcosystemState>,
    creaturesData: List<MarineCreature>,
    decorationProgress: DecorationProgress = DecorationProgress(0, 0, false),
    onCreatureNicknameChanged: (String, String) -> Unit,
    selectedPond: MarineCategory = MarineCategory.FISH,
    onPondSelected: (MarineCategory) -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    var selectedCreature by remember { mutableStateOf<Pair<MarineCreature, CreatureSpec>?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OceanDeep)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    text = stringResource(Res.string.btn_back),
                    color = Accent,
                    fontSize = 22.sp,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .clickable { onNavigateBack() }
                        .padding(4.dp)
                )
                Text(
                    text = stringResource(Res.string.title_ecosystem),
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // Pond selector
            Text(
                text = stringResource(Res.string.label_select_pond),
                color = SectionLabel,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val selectableCategories = MarineCategory.entries.filter {
                    it != MarineCategory.COMPANION &&
                    ecosystemByCategory[it]?.isUnlocked == true
                }
                selectableCategories.forEach { category ->
                    val isSelected = category == selectedPond
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .then(
                                if (isSelected) Modifier.background(Accent.copy(alpha = 0.25f))
                                else Modifier.border(1.dp, DividerColor, RoundedCornerShape(16.dp))
                            )
                            .clickable { onPondSelected(category) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = stringResource(category.displayNameRes()),
                            color = if (isSelected) Accent else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp)
            ) {
                val unlockedSpeciesSet = remember(creaturesData) {
                    creaturesData.map { it.species }.toSet()
                }

                MarineCategory.entries.forEachIndexed { index, category ->
                    // COMPANION es invisible hasta que Bimba está desbloqueada
                    if (category == MarineCategory.COMPANION &&
                        creaturesData.none { it.species == CreatureSpecies.BIMBA }) return@forEachIndexed

                    val state = ecosystemByCategory[category]
                    val isUnlocked = state?.isUnlocked == true
                    val categoryLevel = state?.currentLevel ?: 0

                    // Ordenar por rareza: desbloqueados primero, luego bloqueados
                    val allSpecs = allCreatures
                        .filter { it.category == category }
                        .sortedBy { it.rarity.ordinal }
                    val unlockedSpecs = allSpecs.filter { it.species in unlockedSpeciesSet }
                    val lockedSpecs = allSpecs.filter { it.species !in unlockedSpeciesSet }
                    val orderedSpecs = unlockedSpecs + lockedSpecs

                    if (index > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(DividerColor)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    EcosystemCategorySection(
                        category = category,
                        isUnlocked = isUnlocked,
                        categoryLevel = categoryLevel,
                        specs = orderedSpecs,
                        creaturesData = creaturesData,
                        unlockedSpeciesSet = unlockedSpeciesSet,
                        decorationProgress = decorationProgress,
                        onCreatureTap = { creature, spec ->
                            selectedCreature = creature to spec
                        }
                    )
                }
            }
        }
    }

    selectedCreature?.let { (creature, spec) ->
        CreatureDetailDialog(
            creature = creature,
            spec = spec,
            onDismiss = { selectedCreature = null },
            onNicknameChanged = { nickname ->
                onCreatureNicknameChanged(creature.id, nickname)
            }
        )
    }
}

@Composable
private fun EcosystemCategorySection(
    category: MarineCategory,
    isUnlocked: Boolean,
    categoryLevel: Int,
    specs: List<CreatureSpec>,
    creaturesData: List<MarineCreature>,
    unlockedSpeciesSet: Set<com.mnebot.riptide.domain.model.CreatureSpecies>,
    decorationProgress: DecorationProgress,
    onCreatureTap: (MarineCreature, CreatureSpec) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        // ── Header con nombre de categoría ──
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp)
        ) {
            Text(
                text = stringResource(category.displayNameRes()).uppercase(),
                color = if (isUnlocked) TextSecondary else SectionLabel,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp,
                modifier = Modifier.weight(1f)
            )
            if (!isUnlocked) {
                Icon(
                    painter = painterResource(Res.drawable.ic_lock),
                    contentDescription = stringResource(Res.string.a11y_locked),
                    tint = SectionLabel,
                    modifier = Modifier.size(11.dp)
                )
            } else if (category != MarineCategory.DECORATION && category != MarineCategory.COMPANION) {
                Text(
                    text = stringResource(Res.string.label_category_level, categoryLevel),
                    color = SectionLabel,
                    fontSize = 10.sp
                )
            }
        }

        // ── Barra de progreso hacia siguiente lootbox (en la categoría) ──
        if (isUnlocked) {
            val unlockLevels = CATEGORY_UNLOCK_LEVELS[category] ?: emptyList()
            val nextUnlockLevel = unlockLevels.firstOrNull { it > categoryLevel }
            val progress = if (nextUnlockLevel != null) {
                (categoryLevel.toFloat() / nextUnlockLevel).coerceIn(0f, 1f)
            } else {
                1f // todas desbloqueadas
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(Color(0x22FFFFFF))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(Accent.copy(alpha = 0.55f))
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        } else {
            Spacer(modifier = Modifier.height(10.dp))
        }

        // ── Grid de criaturas ──
        specs.chunked(3).forEach { rowSpecs ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowSpecs.forEach { spec ->
                    val creature = creaturesData.find { it.species == spec.species }
                    val isCreatureUnlocked = spec.species in unlockedSpeciesSet

                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        if (isCreatureUnlocked && creature != null) {
                            UnlockedCreatureCard(
                                spec = spec,
                                creature = creature,
                                onClick = { onCreatureTap(creature, spec) }
                            )
                        } else {
                            LockedCreatureCard(
                                spec = spec,
                                unlockedCategory = isUnlocked,
                                decorationProgress = decorationProgress
                            )
                        }
                    }
                }
                repeat(3 - rowSpecs.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun UnlockedCreatureCard(
    spec: CreatureSpec,
    creature: MarineCreature,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x22FFFFFF))
            .clickable { onClick() }
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CreatureIcon(spec = spec, level = creature.creatureLevel, modifier = Modifier.size(52.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = creature.nickname ?: stringResource(spec.species.displayNameRes()),
            color = TextPrimary,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(4.dp))
        // Rarity chip con texto
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(rarityColor(spec.rarity).copy(alpha = 0.2f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = stringResource(spec.rarity.displayNameRes()).uppercase(),
                color = rarityColor(spec.rarity),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
private fun LockedCreatureCard(
    spec: CreatureSpec,
    unlockedCategory: Boolean,
    decorationProgress: DecorationProgress = DecorationProgress(0, 0, false)
) {
    // Hint text: specific for each decoration species, generic otherwise
    val hintText: String? = when (spec.species) {
        CreatureSpecies.TREASURE_CHEST ->
            stringResource(Res.string.decoration_hint_treasure_chest, decorationProgress.perfectDaysStreak)
        CreatureSpecies.ANCHOR ->
            stringResource(Res.string.decoration_hint_anchor, decorationProgress.completedTasksTotal)
        CreatureSpecies.SUNKEN_SHIP ->
            stringResource(Res.string.decoration_hint_sunken_ship)
        CreatureSpecies.DIVING_HELMET ->
            stringResource(Res.string.decoration_hint_diving_helmet)
        CreatureSpecies.CORAL_THRONE ->
            stringResource(Res.string.decoration_hint_coral_throne, decorationProgress.longestPerfectStreak)
        CreatureSpecies.GOLDEN_TRIDENT ->
            stringResource(Res.string.decoration_hint_golden_trident)
        else -> if (unlockedCategory) stringResource(Res.string.msg_unlock_hint) else null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x11FFFFFF))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CreatureIcon(
            spec = spec,
            level = 1,
            modifier = Modifier
                .size(40.dp)
                .blur(4.dp)
                .graphicsLayer { alpha = 0.22f }
        )
        Spacer(modifier = Modifier.height(4.dp))
        // Rarity chip tenue
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(rarityColor(spec.rarity).copy(alpha = 0.12f))
                .padding(horizontal = 5.dp, vertical = 2.dp)
        ) {
            Text(
                text = stringResource(spec.rarity.displayNameRes()).uppercase(),
                color = rarityColor(spec.rarity).copy(alpha = 0.5f),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        if (hintText != null) {
            Text(
                text = hintText,
                color = SectionLabel,
                fontSize = 9.sp,
                textAlign = TextAlign.Center,
                lineHeight = 12.sp
            )
        } else {
            Icon(
                painter = painterResource(Res.drawable.ic_lock),
                contentDescription = null,
                tint = SectionLabel,
                modifier = Modifier.size(11.dp)
            )
        }
    }
}
