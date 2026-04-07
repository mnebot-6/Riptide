package com.mnebot.riptide.presentation.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import riptide.composeapp.generated.resources.*

private val OceanMid = Color(0xFF1B3A6B)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xB3FFFFFF)
private val Accent = Color(0xFF7EC8E3)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncConflictDialog(
    onKeepLocal: () -> Unit,
    onRestoreServer: () -> Unit
) {
    BasicAlertDialog(onDismissRequest = { /* non-dismissable */ }) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = OceanMid,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = stringResource(Res.string.sync_conflict_title),
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(Res.string.sync_conflict_body),
                    color = TextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onRestoreServer) {
                        Text(
                            text = stringResource(Res.string.sync_conflict_restore_server),
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                    TextButton(onClick = onKeepLocal) {
                        Text(
                            text = stringResource(Res.string.sync_conflict_keep_local),
                            color = Accent,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
