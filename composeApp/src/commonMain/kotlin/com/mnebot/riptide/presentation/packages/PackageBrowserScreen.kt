package com.mnebot.riptide.presentation.packages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mnebot.riptide.domain.model.TaskPackage
import com.mnebot.riptide.presentation.main.parseColor
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import riptide.composeapp.generated.resources.*

private val OceanDeep = Color(0xFF0A1628)
private val OceanMid = Color(0xFF1B3A6B)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xB3FFFFFF)
private val CardBackground = Color(0x22FFFFFF)

@Composable
fun PackageBrowserScreen(
    packages: List<TaskPackage>,
    installedIds: Set<String>,
    installing: Set<String>,
    onInstall: (TaskPackage) -> Unit,
    onUninstall: (TaskPackage) -> Unit,
    onNavigateBack: () -> Unit
) {
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
                    stringResource(Res.string.title_packages),
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(packages, key = { it.id }) { pkg ->
                    val isInstalled = pkg.id in installedIds
                    val isInstalling = pkg.id in installing
                    PackageCard(
                        pkg = pkg,
                        isInstalled = isInstalled,
                        isInstalling = isInstalling,
                        onAction = { if (isInstalled) onUninstall(pkg) else onInstall(pkg) }
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun PackageCard(
    pkg: TaskPackage,
    isInstalled: Boolean,
    isInstalling: Boolean,
    onAction: () -> Unit
) {
    val pkgColor = parseColor(pkg.color)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Icon circle
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(pkgColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = pkg.icon,
                    fontSize = 22.sp
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(pkg.nameRes),
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(pkg.descriptionRes),
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Task preview
        pkg.tasks.forEach { task ->
            Row(
                modifier = Modifier.padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(pkgColor)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(task.titleRes),
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Action button
        val buttonBg = if (isInstalled) Color(0x33FFFFFF) else pkgColor
        val buttonText = when {
            isInstalling -> "..."
            isInstalled -> stringResource(Res.string.btn_uninstall)
            else -> stringResource(Res.string.btn_install)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(buttonBg)
                .clickable(enabled = !isInstalling) { onAction() }
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = buttonText,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
