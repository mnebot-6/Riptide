package com.mnebot.riptide.presentation.aquarium

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mnebot.riptide.domain.model.CreatureRarity
import com.mnebot.riptide.domain.model.MarineCategory
import com.mnebot.riptide.domain.model.MarineCreature
import com.mnebot.riptide.presentation.displayNameRes
import org.jetbrains.compose.resources.stringResource
import riptide.composeapp.generated.resources.*

private val OceanMid = Color(0xFF1B3A6B)
private val Accent = Color(0xFF7EC8E3)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xB3FFFFFF)

@Composable
fun CreatureDetailDialog(
    creature: MarineCreature,
    spec: CreatureSpec,
    onDismiss: () -> Unit,
    onNicknameChanged: (String) -> Unit
) {
    var nickname by remember(creature.id) { mutableStateOf(creature.nickname ?: "") }
    val isDecoration = spec.category == MarineCategory.DECORATION

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(OceanMid)
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CreatureIcon(spec = spec, level = creature.creatureLevel, modifier = Modifier.size(80.dp))

            Spacer(Modifier.height(8.dp))

            // Species name (always shown)
            Text(
                text = stringResource(spec.species.displayNameRes()),
                color = TextPrimary,
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(4.dp))

            val rarityColor = when (spec.rarity) {
                CreatureRarity.COMMON    -> Color(0xFF9E9E9E)
                CreatureRarity.UNCOMMON  -> Color(0xFF4CAF50)
                CreatureRarity.RARE      -> Color(0xFF2196F3)
                CreatureRarity.EPIC      -> Color(0xFF9C27B0)
                CreatureRarity.LEGENDARY -> Color(0xFFFF9800)
            }
            Text(
                text = stringResource(spec.rarity.displayNameRes()),
                color = rarityColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(12.dp))

            // Nickname field — only for non-decoration creatures
            if (!isDecoration) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    BasicTextField(
                        value = nickname,
                        onValueChange = { nickname = it },
                        singleLine = true,
                        cursorBrush = SolidColor(Accent),
                        textStyle = LocalTextStyle.current.copy(
                            color = Accent,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) { innerField ->
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (nickname.isEmpty()) {
                                Text(
                                    text = stringResource(Res.string.placeholder_nickname),
                                    color = TextSecondary,
                                    fontSize = 15.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            innerField()
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .height(1.dp)
                            .background(Accent.copy(alpha = 0.4f))
                    )
                }

                Spacer(Modifier.height(20.dp))
            }

            // xpRequiredForLevel viene de CreatureExtensions.kt
            XpBar(creature = creature)

            Spacer(Modifier.height(16.dp))

            val date = creature.unlockedAt.date
            val day = date.dayOfMonth.toString().padStart(2, '0')
            val month = date.monthNumber.toString().padStart(2, '0')
            val year = date.year
            Text(
                text = stringResource(Res.string.msg_in_tank_since, "$day/$month/$year"),
                color = TextSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(20.dp))

            TextButton(
                onClick = {
                    if (!isDecoration) onNicknameChanged(nickname)
                    onDismiss()
                }
            ) {
                Text(
                    text = stringResource(if (isDecoration) Res.string.btn_close else Res.string.btn_save),
                    color = Accent,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun XpBar(creature: MarineCreature) {
    val xpForCurrent = xpRequiredForLevel(creature.creatureLevel)
    val xpForNext = xpRequiredForLevel(creature.creatureLevel + 1)
    val range = (xpForNext - xpForCurrent).coerceAtLeast(1)
    val progress = ((creature.experience - xpForCurrent).toFloat() / range).coerceIn(0f, 1f)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(Res.string.label_level_full, creature.creatureLevel),
            color = Accent,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(CircleShape)
                .background(TextPrimary.copy(alpha = 0.12f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(Accent)
            )
        }
    }
}