package com.shauiqiu.fe

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavHostController) {
    var showAboutDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = { FeTopAppBar(title = "Fe") }
    ) { p ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(p)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(8.dp))
            Text("设置", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Medium)
            Text("管理您的终端偏好设置。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))

            // 核心设置分组
            ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column {
                    SettingsListItem(Icons.Default.Build, "激活设置", "配置核心终端行为") {
                        navController.navigate(Screen.Activation.route)
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                    SettingsListItem(Icons.Default.Tune, "通用", "语言、时区和格式") {
                        navController.navigate(Screen.GeneralSettings.route)
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                    SettingsListItem(Icons.Default.Palette, "主题", "莫奈取色与黑白色调") {
                        navController.navigate(Screen.ThemeSettings.route)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // 系统作者状态卡片
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("作者", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(8.dp))
                            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                            val pulseAlpha by infiniteTransition.animateFloat(
                                initialValue = 0.2f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1200, easing = FastOutLinearInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "alpha"
                            )
                            Box(modifier = Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha), CircleShape))
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("帅丘", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                        Text("版本: v2.5.0", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                    // 作者头像
                    Image(
                        painter = painterResource(id = R.drawable.ic_author),
                        contentDescription = "作者头像",
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // 关于与文档区域
            ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column {
                    SettingsListItem(Icons.Default.Info, "关于 Fe", "系统构建信息", hideChevron = true) {
                        showAboutDialog = true
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    SettingsListItem(Icons.Default.Help, "文档", "在线用户手册", hideChevron = true) {
                        // TODO: 跳转外部浏览器
                    }
                }
            }

            Spacer(Modifier.height(88.dp))
        }
    }
    
    // 关于弹窗
    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 关闭按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                }
                
                // 应用图标
                Image(
                    painter = painterResource(id = R.drawable.ic_author),
                    contentDescription = "应用图标",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                )
                
                Spacer(Modifier.height(16.dp))
                
                // 应用名称
                Text(
                    "Fxxk e听说",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(Modifier.height(8.dp))
                
                // 版本号
                Text(
                    "版本: v2.5.0",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(Modifier.height(16.dp))
                
                // 作者信息
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "作者: 帅丘",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Spacer(Modifier.height(12.dp))
                
                // 开源地址
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Code,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "开源地址: (待定)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(Modifier.height(24.dp))
                
                // 关闭按钮
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("关闭")
                }
            }
        }
    }
}
