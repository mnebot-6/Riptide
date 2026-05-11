package com.mnebot.riptide.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mnebot.riptide.domain.model.LoggedInUser
import com.mnebot.riptide.domain.model.SyncStatus
import com.mnebot.riptide.presentation.components.TimeInputField
import kotlinx.datetime.LocalTime
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import riptide.composeapp.generated.resources.*

private val OceanDeep = Color(0xFF0A1628)
private val OceanMid = Color(0xFF1B3A6B)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xB3FFFFFF)
private val SectionLabel = Color(0x80FFFFFF)
private val DividerColor = Color(0x33FFFFFF)

@Composable
fun SettingsScreen(
    nightSummaryTime: LocalTime,
    morningReminderTime: LocalTime?,
    onNightSummaryTimeChanged: (LocalTime) -> Unit,
    onMorningReminderTimeChanged: (LocalTime?) -> Unit,
    wallpaperFps: Int,
    onWallpaperFpsChanged: (Int) -> Unit,
    onSetLiveWallpaper: () -> Unit,
    loggedInUser: LoggedInUser?,
    syncStatus: SyncStatus,
    lastSyncMillis: Long? = null,
    lastSyncError: String? = null,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onSyncNow: () -> Unit,
    onNavigateBack: () -> Unit,
    appVersionName: String = "",
    appVersionCode: Int = 0,
    onOpenUrl: (String) -> Unit = {},
    onDeleteAccount: suspend () -> Result<Unit> = { Result.success(Unit) }
) {
    var showFpsDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val privacyUrl = stringResource(Res.string.url_privacy_policy)
    val termsUrl = stringResource(Res.string.url_terms_of_service)

    if (showDeleteDialog) {
        DeleteAccountDialog(
            onDismiss = { showDeleteDialog = false },
            onConfirm = onDeleteAccount
        )
    }

    if (showFpsDialog) {
        var selectedFps by remember { mutableIntStateOf(wallpaperFps) }
        AlertDialog(
            onDismissRequest = { showFpsDialog = false },
            containerColor = Color(0xFF1B3A6B),
            title = {
                Text(
                    stringResource(Res.string.wallpaper_quality_title),
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(15, 30, 60).forEach { fps ->
                            val isSelected = selectedFps == fps
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) Color(0xFF1A73E8) else Color(0x33FFFFFF))
                                    .clickable { selectedFps = fps }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "${fps} fps",
                                    color = if (isSelected) TextPrimary else TextSecondary,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        stringResource(Res.string.wallpaper_quality_hint),
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onWallpaperFpsChanged(selectedFps)
                    showFpsDialog = false
                    onSetLiveWallpaper()
                }) {
                    Text(
                        stringResource(Res.string.wallpaper_quality_apply),
                        color = Color(0xFF1A73E8),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showFpsDialog = false }) {
                    Text(stringResource(Res.string.btn_cancel), color = TextSecondary)
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(OceanDeep, OceanMid)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0x44FFFFFF))
                        .clickable { onNavigateBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_arrow_left),
                        contentDescription = stringResource(Res.string.btn_back),
                        tint = TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    stringResource(Res.string.title_settings),
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider(color = DividerColor)

            // Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 20.dp)
            ) {
                // Notifications section
                SectionTitle(stringResource(Res.string.section_settings))
                Spacer(modifier = Modifier.height(12.dp))
                NightSummaryTimeSetting(
                    currentTime = nightSummaryTime,
                    onTimeChanged = onNightSummaryTimeChanged
                )
                Spacer(modifier = Modifier.height(4.dp))
                MorningReminderSetting(
                    currentTime = morningReminderTime,
                    onTimeChanged = onMorningReminderTimeChanged
                )

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(modifier = Modifier.height(20.dp))

                // Ecosystem section
                SectionTitle(stringResource(Res.string.section_ecosystem))
                Spacer(modifier = Modifier.height(8.dp))
                SettingsItem(
                    painter = painterResource(Res.drawable.ic_waves),
                    label = stringResource(Res.string.btn_live_wallpaper),
                    onClick = { showFpsDialog = true }
                )

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(modifier = Modifier.height(20.dp))

                // Account section
                AccountSection(
                    loggedInUser = loggedInUser,
                    syncStatus = syncStatus,
                    lastSyncMillis = lastSyncMillis,
                    lastSyncError = lastSyncError,
                    onSignIn = onSignIn,
                    onSignOut = onSignOut,
                    onSyncNow = onSyncNow
                )

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(modifier = Modifier.height(20.dp))

                // Legal & About section
                LegalSection(
                    appVersionName = appVersionName,
                    appVersionCode = appVersionCode,
                    onOpenPrivacy = { onOpenUrl(privacyUrl) },
                    onOpenTerms = { onOpenUrl(termsUrl) }
                )

                // Danger zone — only visible when signed in
                if (loggedInUser != null) {
                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 16.dp))
                    Spacer(modifier = Modifier.height(20.dp))

                    DangerSection(onDeleteAccountClick = { showDeleteDialog = true })
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun LegalSection(
    appVersionName: String,
    appVersionCode: Int,
    onOpenPrivacy: () -> Unit,
    onOpenTerms: () -> Unit
) {
    SectionTitle(stringResource(Res.string.section_legal))
    Spacer(modifier = Modifier.height(8.dp))
    SettingsItem(
        painter = painterResource(Res.drawable.ic_user),
        label = stringResource(Res.string.btn_privacy_policy),
        onClick = onOpenPrivacy
    )
    SettingsItem(
        painter = painterResource(Res.drawable.ic_user),
        label = stringResource(Res.string.btn_terms_of_service),
        onClick = onOpenTerms
    )
    if (appVersionName.isNotEmpty()) {
        Text(
            text = stringResource(Res.string.label_app_version, appVersionName, appVersionCode),
            color = SectionLabel,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun DangerSection(onDeleteAccountClick: () -> Unit) {
    SectionTitle(stringResource(Res.string.section_danger))
    Spacer(modifier = Modifier.height(8.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onDeleteAccountClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_log_out),
            contentDescription = null,
            tint = Color(0xFFE57373),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = stringResource(Res.string.btn_delete_account),
            color = Color(0xFFE57373),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun DeleteAccountDialog(
    onDismiss: () -> Unit,
    onConfirm: suspend () -> Result<Unit>
) {
    val scope = rememberCoroutineScope()
    var confirmText by remember { mutableStateOf(TextFieldValue("")) }
    var isDeleting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val expectedWord = stringResource(Res.string.dialog_delete_account_confirm_word)
    val matches = confirmText.text.trim().equals(expectedWord, ignoreCase = false)

    AlertDialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        containerColor = Color(0xFF1B3A6B),
        title = {
            Text(
                stringResource(Res.string.dialog_delete_account_title),
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column {
                Text(
                    stringResource(Res.string.dialog_delete_account_body),
                    color = TextSecondary,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(Res.string.dialog_delete_account_confirm_hint),
                    color = SectionLabel,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x22FFFFFF))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    BasicTextField(
                        value = confirmText,
                        onValueChange = { confirmText = it },
                        singleLine = true,
                        enabled = !isDeleting,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = TextPrimary,
                            fontSize = 15.sp
                        ),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(Color(0xFFE57373)),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (errorMessage != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(Res.string.msg_delete_account_failed, errorMessage!!),
                        color = Color(0xFFE57373),
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = matches && !isDeleting,
                onClick = {
                    scope.launch {
                        isDeleting = true
                        errorMessage = null
                        val result = onConfirm()
                        isDeleting = false
                        result.onSuccess { onDismiss() }
                            .onFailure { errorMessage = it.message ?: "unknown" }
                    }
                }
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color(0xFFE57373),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        stringResource(Res.string.btn_delete_account_confirm),
                        color = if (matches) Color(0xFFE57373) else Color(0x66FFFFFF),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isDeleting) {
                Text(stringResource(Res.string.btn_cancel), color = TextSecondary)
            }
        }
    )
}

@Composable
private fun NightSummaryTimeSetting(currentTime: LocalTime, onTimeChanged: (LocalTime) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_moon),
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(stringResource(Res.string.label_night_summary), color = TextSecondary, fontSize = 15.sp, modifier = Modifier.weight(1f))
        TimeInputField(
            value = currentTime,
            onValueChange = { it?.let { t -> onTimeChanged(t) } },
            nullable = false,
            compact = true
        )
    }
}

@Composable
private fun MorningReminderSetting(currentTime: LocalTime?, onTimeChanged: (LocalTime?) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_sun),
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            stringResource(Res.string.label_morning_reminder),
            color = TextSecondary,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f)
        )
        if (currentTime != null) {
            TimeInputField(
                value = currentTime,
                onValueChange = { it?.let { t -> onTimeChanged(t) } },
                nullable = false,
                compact = true
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Switch(
            checked = currentTime != null,
            onCheckedChange = { enabled ->
                onTimeChanged(if (enabled) LocalTime(8, 0) else null)
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF4A90D9),
                uncheckedThumbColor = Color(0x99FFFFFF),
                uncheckedTrackColor = Color(0x33FFFFFF)
            )
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        color = SectionLabel,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(horizontal = 24.dp)
    )
}

@Composable
private fun SettingsItem(painter: Painter, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painter,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = label, color = TextSecondary, fontSize = 15.sp)
    }
}

@Composable
private fun AccountSection(
    loggedInUser: LoggedInUser?,
    syncStatus: SyncStatus,
    lastSyncMillis: Long? = null,
    lastSyncError: String? = null,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onSyncNow: () -> Unit
) {
    SectionTitle(stringResource(Res.string.section_account))
    Spacer(modifier = Modifier.height(8.dp))

    if (loggedInUser == null) {
        SettingsItem(
            painter = painterResource(Res.drawable.ic_user),
            label = stringResource(Res.string.btn_sign_in_google),
            onClick = onSignIn
        )
    } else {
        // User info row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val initial = (loggedInUser.displayName?.firstOrNull()
                ?: loggedInUser.email.firstOrNull()
                ?: '?').uppercaseChar()
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4A90D9)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial.toString(),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                loggedInUser.displayName?.let { name ->
                    Text(
                        text = name,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = loggedInUser.email,
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        // Sync now row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSyncNow() }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_refresh_cw),
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(Res.string.btn_sync_now),
                color = TextSecondary,
                fontSize = 15.sp
            )
            SyncStatusBadge(syncStatus, modifier = Modifier.padding(start = 8.dp))
        }

        // Last sync info / error
        if (lastSyncError != null) {
            Text(
                text = stringResource(Res.string.sync_last_error, lastSyncError),
                color = Color(0xFFE57373),
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )
        } else if (lastSyncMillis != null) {
            Text(
                text = stringResource(Res.string.sync_last_synced, formatRelativeTime(lastSyncMillis)),
                color = TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )
        }

        // Sign out
        SettingsItem(
            painter = painterResource(Res.drawable.ic_log_out),
            label = stringResource(Res.string.btn_sign_out),
            onClick = onSignOut
        )
    }
}

private fun formatRelativeTime(millis: Long): String {
    val diff = (kotlin.time.Clock.System.now().toEpochMilliseconds() - millis).coerceAtLeast(0)
    val seconds = diff / 1000
    return when {
        seconds < 60 -> "<1 min"
        seconds < 3600 -> "${seconds / 60} min"
        seconds < 86400 -> "${seconds / 3600} h"
        else -> "${seconds / 86400} d"
    }
}

@Composable
private fun SyncStatusBadge(status: SyncStatus, modifier: Modifier = Modifier) {
    val (text, color) = when (status) {
        SyncStatus.IDLE -> return
        SyncStatus.SYNCING -> stringResource(Res.string.sync_status_syncing) to Color(0xB3FFFFFF)
        SyncStatus.SUCCESS -> stringResource(Res.string.sync_status_success) to Color(0xFF81C784)
        SyncStatus.ERROR -> stringResource(Res.string.sync_status_error) to Color(0xFFE57373)
        SyncStatus.OFFLINE -> stringResource(Res.string.sync_status_offline) to Color(0x80FFFFFF)
    }
    Text(
        text = text,
        color = color,
        fontSize = 12.sp,
        modifier = modifier
    )
}
