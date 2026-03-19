package com.mnebot.riptide.presentation.aquarium

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mnebot.riptide.domain.model.EcosystemState
import com.mnebot.riptide.domain.model.MarineCategory
import com.mnebot.riptide.domain.model.MarineCreature

private val OceanDeep = Color(0xFF0A1628)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xB3FFFFFF)
private val SectionLabel = Color(0x80FFFFFF)
private val Accent = Color(0xFF7EC8E3)
private val DividerColor = Color(0x33FFFFFF)

private fun MarineCategory.displayName(): String = when (this) {
    MarineCategory.FISH       -> "Peces"
    MarineCategory.FLORA      -> "Flora"
    MarineCategory.CRUSTACEAN -> "Crustáceos"
    MarineCategory.MOLLUSK    -> "Moluscos"
    MarineCategory.PELAGIC    -> "Pelágicos"
    MarineCategory.CEPHALOPOD -> "Cefalópodos"
    MarineCategory.REPTILE    -> "Reptiles"
    MarineCategory.MAMMAL     -> "Mamíferos"
    MarineCategory.DECORATION -> "Decoración"
}

@Composable
fun EcosystemScreen(
    ecosystemByCategory: Map<MarineCategory, EcosystemState>,
    creaturesData: List<MarineCreature>,
    onCreatureNicknameChanged: (String, String) -> Unit,
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
                    text = "←",
                    color = Accent,
                    fontSize = 22.sp,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .clickable { onNavigateBack() }
                        .padding(4.dp)
                )
                Text(
                    text = "Mi ecosistema",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp)
            ) {
                MarineCategory.entries.forEachIndexed { index, category ->
                    val state = ecosystemByCategory[category]
                    val isUnlocked = state?.isUnlocked == true
                    val categoryLevel = state?.currentLevel ?: 0

                    val specs = allCreatures
                        .filter { it.category == category }
                        .sortedBy { it.unlockLevel }

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
                        specs = specs,
                        creaturesData = creaturesData,
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
    onCreatureTap: (MarineCreature, CreatureSpec) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
        ) {
            Text(
                text = category.displayName().uppercase(),
                color = if (isUnlocked) TextSecondary else SectionLabel,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp,
                modifier = Modifier.weight(1f)
            )
            if (!isUnlocked) Text("🔒", fontSize = 11.sp)
        }

        specs.chunked(3).forEach { rowSpecs ->
            // IntrinsicSize.Max iguala la altura de todas las cards de la fila
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowSpecs.forEach { spec ->
                    val creature = creaturesData.find { it.species == spec.species }
                    val isCreatureUnlocked = isUnlocked && categoryLevel >= spec.unlockLevel

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
                                categoryLevel = categoryLevel,
                                isUnlocked = isUnlocked
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
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = creature.nickname ?: spec.displayName,
            color = TextPrimary,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(6.dp))
        val dots = creature.creatureLevel.coerceAtMost(5)
        Row(horizontalArrangement = Arrangement.Center) {
            repeat(dots) { i ->
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(Accent)
                )
                if (i < dots - 1) Spacer(Modifier.width(3.dp))
            }
        }
    }
}

@Composable
private fun LockedCreatureCard(
    spec: CreatureSpec,
    categoryLevel: Int,
    isUnlocked: Boolean
) {
    val progress = if (!isUnlocked) 0f
    else (categoryLevel.toFloat() / spec.unlockLevel.coerceAtLeast(1)).coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x11FFFFFF))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = spec.emoji,
            fontSize = 32.sp,
            color = Color.White.copy(alpha = 0.10f)
        )
        Spacer(modifier = Modifier.height(6.dp))
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
                    .background(Accent.copy(alpha = 0.45f))
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (!isUnlocked) "🔒" else "Nv. ${spec.unlockLevel}",
            color = SectionLabel,
            fontSize = 10.sp,
            textAlign = TextAlign.Center
        )
    }
}