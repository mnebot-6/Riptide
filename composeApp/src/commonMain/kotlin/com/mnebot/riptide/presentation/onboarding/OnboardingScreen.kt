package com.mnebot.riptide.presentation.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import riptide.composeapp.generated.resources.*

private val OceanDeep = Color(0xFF0A1628)
private val OceanMid = Color(0xFF1B3A6B)
private val OceanLight = Color(0xFF2E5F9E)
private val CardBg = Color(0x33FFFFFF)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xB3FFFFFF)
private val AccentBlue = Color(0xFF4A90D9)
private val ChipSelected = Color(0xFF1A73E8)
private val ChipBorder = Color(0x44FFFFFF)

private const val TOTAL_PAGES = 8  // Welcome + 5 quiz + summary + ready

/**
 * Onboarding with integrated quiz. [onComplete] receives the quiz state
 * if the user completed the quiz, or null if they skipped.
 *
 * The final page is a consent gate: the "Start" button is disabled until the user
 * checks the Privacy Policy + ToS acceptance box. [onOpenUrl] opens legal URLs.
 */
@Composable
fun OnboardingScreen(
    onComplete: (OnboardingQuizState?) -> Unit,
    onOpenUrl: (String) -> Unit = {}
) {

    var currentStep by remember { mutableIntStateOf(0) }
    var goingForward by remember { mutableStateOf(true) }
    var quizState by remember { mutableStateOf(OnboardingQuizState()) }
    var consentAccepted by remember { mutableStateOf(false) }
    val isOnConsentPage = { currentStep == TOTAL_PAGES - 1 }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(OceanDeep, OceanMid, OceanLight.copy(alpha = 0.6f))
                )
            )
    ) {
        // Skip button (not on last page)
        if (currentStep < TOTAL_PAGES - 1) {
            TextButton(
                onClick = { onComplete(null) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(end = 8.dp, top = 8.dp)
            ) {
                Text(
                    text = stringResource(Res.string.quiz_skip),
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }
        }

        // Page content
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
            when (step) {
                0 -> WelcomePage()
                1 -> OccupationPage(quizState) { quizState = quizState.copy(occupation = it) }
                2 -> LifeAreasPage(quizState) { quizState = quizState.copy(lifeAreas = it) }
                3 -> WakeTimePage(quizState) { quizState = quizState.copy(wakeHour = it) }
                4 -> BedTimePage(quizState) { quizState = quizState.copy(bedHour = it) }
                5 -> StructurePage(quizState) { quizState = quizState.copy(structure = it) }
                6 -> SummaryPage(quizState)
                7 -> ConsentReadyPage(
                    accepted = consentAccepted,
                    onAcceptedChange = { consentAccepted = it },
                    onOpenUrl = onOpenUrl
                )
            }
        }

        // Dot indicator
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(TOTAL_PAGES) { i ->
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

        // Navigation row
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
                    Text("\u2190", color = TextSecondary, fontSize = 18.sp)
                }
            } else {
                Spacer(Modifier.width(64.dp))
            }

            Button(
                onClick = {
                    if (currentStep < TOTAL_PAGES - 1) {
                        goingForward = true
                        currentStep++
                    } else {
                        onComplete(quizState)
                    }
                },
                enabled = !isOnConsentPage() || consentAccepted,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentBlue,
                    disabledContainerColor = Color(0x33FFFFFF)
                ),
                contentPadding = PaddingValues(horizontal = 28.dp, vertical = 12.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text(
                    text = when (currentStep) {
                        TOTAL_PAGES - 1 -> stringResource(Res.string.quiz_start)
                        else -> stringResource(Res.string.quiz_next)
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ── Pages ────────────────────────────────────────────────────────────────────

@Composable
private fun WelcomePage() {
    CenteredPage(
        emoji = "\uD83C\uDF0A",
        title = stringResource(Res.string.quiz_welcome_title),
        body = stringResource(Res.string.quiz_welcome_body)
    )
}

@Composable
private fun ConsentReadyPage(
    accepted: Boolean,
    onAcceptedChange: (Boolean) -> Unit,
    onOpenUrl: (String) -> Unit
) {
    val privacyUrl = stringResource(Res.string.url_privacy_policy)
    val termsUrl = stringResource(Res.string.url_terms_of_service)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "\u2728", fontSize = 56.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(Res.string.onboarding_ready_title),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            lineHeight = 32.sp
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = stringResource(Res.string.consent_body),
            fontSize = 14.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Text(
                text = stringResource(Res.string.consent_open_privacy),
                color = AccentBlue,
                fontSize = 13.sp,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable { onOpenUrl(privacyUrl) }
            )
            Text(
                text = stringResource(Res.string.consent_open_terms),
                color = AccentBlue,
                fontSize = 13.sp,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable { onOpenUrl(termsUrl) }
            )
        }
        Spacer(Modifier.height(20.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { onAcceptedChange(!accepted) }
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Checkbox(
                checked = accepted,
                onCheckedChange = onAcceptedChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = AccentBlue,
                    uncheckedColor = TextSecondary,
                    checkmarkColor = Color.White
                )
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = stringResource(Res.string.consent_checkbox),
                color = TextPrimary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun OccupationPage(state: OnboardingQuizState, onChanged: (Occupation) -> Unit) {
    QuizPage(title = stringResource(Res.string.quiz_occupation)) {
        val options = listOf(
            Occupation.STUDENT to stringResource(Res.string.quiz_occ_student),
            Occupation.WORKER to stringResource(Res.string.quiz_occ_worker),
            Occupation.FREELANCER to stringResource(Res.string.quiz_occ_freelancer),
            Occupation.OTHER to stringResource(Res.string.quiz_occ_other)
        )
        options.forEach { (value, label) ->
            SelectableChip(
                text = label,
                selected = state.occupation == value,
                onClick = { onChanged(value) }
            )
        }
    }
}

@Composable
private fun LifeAreasPage(state: OnboardingQuizState, onChanged: (Set<LifeArea>) -> Unit) {
    QuizPage(title = stringResource(Res.string.quiz_life_areas)) {
        val options = listOf(
            LifeArea.HEALTH to stringResource(Res.string.quiz_area_health),
            LifeArea.FITNESS to stringResource(Res.string.quiz_area_fitness),
            LifeArea.STUDY to stringResource(Res.string.quiz_area_study),
            LifeArea.WORK to stringResource(Res.string.quiz_area_work),
            LifeArea.PERSONAL to stringResource(Res.string.quiz_area_personal),
            LifeArea.CREATIVE to stringResource(Res.string.quiz_area_creative),
            LifeArea.MINDFULNESS to stringResource(Res.string.quiz_area_mindfulness)
        )
        options.forEach { (value, label) ->
            val selected = value in state.lifeAreas
            SelectableChip(
                text = label,
                selected = selected,
                onClick = {
                    onChanged(
                        if (selected) state.lifeAreas - value
                        else state.lifeAreas + value
                    )
                }
            )
        }
    }
}

@Composable
private fun WakeTimePage(state: OnboardingQuizState, onChanged: (Int) -> Unit) {
    QuizPage(title = stringResource(Res.string.quiz_wake_time)) {
        val ranges = listOf(5 to "5-6", 6 to "6-7", 7 to "7-8", 8 to "8-9", 9 to "9+")
        ranges.forEach { (hour, label) ->
            SelectableChip(
                text = label,
                selected = state.wakeHour == hour,
                onClick = { onChanged(hour) }
            )
        }
    }
}

@Composable
private fun BedTimePage(state: OnboardingQuizState, onChanged: (Int) -> Unit) {
    QuizPage(title = stringResource(Res.string.quiz_bed_time)) {
        val ranges = listOf(21 to "21-22", 22 to "22-23", 23 to "23-00", 0 to "00+")
        ranges.forEach { (hour, label) ->
            SelectableChip(
                text = label,
                selected = state.bedHour == hour,
                onClick = { onChanged(hour) }
            )
        }
    }
}

@Composable
private fun StructurePage(state: OnboardingQuizState, onChanged: (TaskStructure) -> Unit) {
    QuizPage(title = stringResource(Res.string.quiz_structure)) {
        val options = listOf(
            TaskStructure.FIXED to stringResource(Res.string.quiz_struct_fixed),
            TaskStructure.FLEXIBLE to stringResource(Res.string.quiz_struct_flexible),
            TaskStructure.MIX to stringResource(Res.string.quiz_struct_mix)
        )
        options.forEach { (value, label) ->
            SelectableChip(
                text = label,
                selected = state.structure == value,
                onClick = { onChanged(value) }
            )
        }
    }
}

@Composable
private fun SummaryPage(state: OnboardingQuizState) {
    CenteredPage(
        emoji = "\uD83C\uDF89",
        title = stringResource(Res.string.quiz_summary),
        body = stringResource(Res.string.quiz_summary_body)
    )
}

// ── Reusable components ─────────────────────────────────────────────────────

@Composable
private fun CenteredPage(emoji: String, title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = emoji, fontSize = 72.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Text(
            text = title,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            lineHeight = 34.sp
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = body,
            fontSize = 16.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
    }
}

@Composable
private fun QuizPage(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp)
            .padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            lineHeight = 30.sp
        )
        Spacer(Modifier.height(28.dp))
        content()
    }
}

@Composable
private fun SelectableChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .then(
                if (selected) Modifier.background(ChipSelected)
                else Modifier.border(1.dp, ChipBorder, RoundedCornerShape(14.dp))
            )
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text,
            color = if (selected) TextPrimary else TextSecondary,
            fontSize = 16.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
