package com.mnebot.riptide.presentation.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import riptide.composeapp.generated.resources.Res
import riptide.composeapp.generated.resources.onboarding_btn_next
import riptide.composeapp.generated.resources.onboarding_btn_skip
import riptide.composeapp.generated.resources.onboarding_btn_start
import riptide.composeapp.generated.resources.onboarding_ecosystem_body
import riptide.composeapp.generated.resources.onboarding_ecosystem_lootbox
import riptide.composeapp.generated.resources.onboarding_ecosystem_nopressure
import riptide.composeapp.generated.resources.onboarding_ecosystem_species
import riptide.composeapp.generated.resources.onboarding_ecosystem_title
import riptide.composeapp.generated.resources.onboarding_how_body
import riptide.composeapp.generated.resources.onboarding_how_day
import riptide.composeapp.generated.resources.onboarding_how_morning
import riptide.composeapp.generated.resources.onboarding_how_night
import riptide.composeapp.generated.resources.onboarding_how_title
import riptide.composeapp.generated.resources.onboarding_ready_body
import riptide.composeapp.generated.resources.onboarding_ready_title
import riptide.composeapp.generated.resources.onboarding_welcome_body
import riptide.composeapp.generated.resources.onboarding_welcome_title

// ── Palette (matches MainScreen palette) ──────────────────────────────────────
private val OceanDeep   = Color(0xFF0A1628)
private val OceanMid    = Color(0xFF1B3A6B)
private val OceanLight  = Color(0xFF2E5F9E)
private val CardBg      = Color(0x33FFFFFF)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xB3FFFFFF)
private val AccentBlue  = Color(0xFF4A90D9)

// ── Data ──────────────────────────────────────────────────────────────────────

private data class OnboardingPage(
    val emoji: String,
    val title: String,
    val body: String,
    val extras: List<Pair<String, String>> = emptyList()
)

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingScreen(onComplete: () -> Unit) {

    val pages = listOf(
        OnboardingPage(
            emoji = "🌊",
            title = stringResource(Res.string.onboarding_welcome_title),
            body  = stringResource(Res.string.onboarding_welcome_body)
        ),
        OnboardingPage(
            emoji = "📅",
            title = stringResource(Res.string.onboarding_how_title),
            body  = stringResource(Res.string.onboarding_how_body),
            extras = listOf(
                "🌅" to stringResource(Res.string.onboarding_how_morning),
                "✅" to stringResource(Res.string.onboarding_how_day),
                "🌙" to stringResource(Res.string.onboarding_how_night)
            )
        ),
        OnboardingPage(
            emoji = "🐠",
            title = stringResource(Res.string.onboarding_ecosystem_title),
            body  = stringResource(Res.string.onboarding_ecosystem_body),
            extras = listOf(
                "🎁" to stringResource(Res.string.onboarding_ecosystem_lootbox),
                "🐙" to stringResource(Res.string.onboarding_ecosystem_species),
                "🔒" to stringResource(Res.string.onboarding_ecosystem_nopressure)
            )
        ),
        OnboardingPage(
            emoji = "✨",
            title = stringResource(Res.string.onboarding_ready_title),
            body  = stringResource(Res.string.onboarding_ready_body)
        )
    )

    var currentStep by remember { mutableIntStateOf(0) }
    var goingForward by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(OceanDeep, OceanMid, OceanLight.copy(alpha = 0.6f))
                )
            )
    ) {

        // ── Botón Saltar (esquina superior derecha, pasos 0–2) ────────────────
        if (currentStep < pages.size - 1) {
            TextButton(
                onClick = { onComplete() },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(end = 8.dp, top = 8.dp)
            ) {
                Text(
                    text = stringResource(Res.string.onboarding_btn_skip),
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }
        }

        // ── Page content ──────────────────────────────────────────────────────
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                if (goingForward) {
                    (slideInHorizontally(tween(280)) { it } + fadeIn(tween(280))) togetherWith
                            (slideOutHorizontally(tween(280)) { -it } + fadeOut(tween(280)))
                } else {
                    (slideInHorizontally(tween(280)) { -it } + fadeIn(tween(280))) togetherWith
                            (slideOutHorizontally(tween(280)) { it } + fadeOut(tween(280)))
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 156.dp)
        ) { step ->
            OnboardingPageContent(page = pages[step])
        }

        // ── Dot indicator ─────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(pages.size) { i ->
                val active = i == currentStep
                Box(
                    Modifier
                        .size(if (active) 10.dp else 7.dp)
                        .clip(CircleShape)
                        .background(
                            if (active) Color.White else Color.White.copy(alpha = 0.35f)
                        )
                )
            }
        }

        // ── Navigation row ────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp)
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (currentStep > 0) {
                TextButton(
                    onClick = {
                        goingForward = false
                        currentStep--
                    }
                ) {
                    Text("←", color = TextSecondary, fontSize = 18.sp)
                }
            } else {
                Spacer(Modifier.width(64.dp))
            }

            Button(
                onClick = {
                    if (currentStep < pages.size - 1) {
                        goingForward = true
                        currentStep++
                    } else {
                        onComplete()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                contentPadding = PaddingValues(horizontal = 28.dp, vertical = 12.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text(
                    text = if (currentStep == pages.size - 1)
                        stringResource(Res.string.onboarding_btn_start)
                    else
                        stringResource(Res.string.onboarding_btn_next),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ── Step content ──────────────────────────────────────────────────────────────

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = page.emoji,
            fontSize = 72.sp,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = page.title,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            lineHeight = 34.sp
        )

        Spacer(Modifier.height(14.dp))

        Text(
            text = page.body,
            fontSize = 16.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )

        if (page.extras.isNotEmpty()) {
            Spacer(Modifier.height(28.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                page.extras.forEach { (emoji, text) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CardBg, shape = RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(emoji, fontSize = 22.sp)
                        Text(
                            text = text,
                            fontSize = 14.sp,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
