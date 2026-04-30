package com.shauiqiu.fe

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadScreen() {
    val safBlue = Color(0xFF60A5FA)
    var isFabExpanded by remember { mutableStateOf(false) }
    
    // 获取 SAF 状态
    val safState = rememberSAFState()
    
    // 获取当前激活模式
    val currentMode = SettingsManager.getSavedActivationMode() ?: ActivationMode.DEFAULT
    val isSafMode = currentMode == ActivationMode.SAF
    
    Scaffold(
        topBar = { 
            FeTopAppBar(title = if (isSafMode && safState.isConfigured) {
                safState.directoryName ?: "已授权目录"
            } else {
                "Fe"
            })
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // 子菜单 FABs
                AnimatedVisibility(
                    visible = isFabExpanded,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(bottom = 16.dp)) {
                        SmallFloatingActionButton(onClick = { isFabExpanded = false }, containerColor = MaterialTheme.colorScheme.surfaceContainerHigh) {
                            Icon(Icons.Default.MenuBook, "Read", tint = MaterialTheme.colorScheme.onSurface)
                        }
                        SmallFloatingActionButton(onClick = { isFabExpanded = false }, containerColor = MaterialTheme.colorScheme.surfaceContainerHigh) {
                            Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                        SmallFloatingActionButton(onClick = { isFabExpanded = false }, containerColor = MaterialTheme.colorScheme.surfaceContainerHigh) {
                            Icon(Icons.Default.Refresh, "Refresh", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                // 主 FAB
                FloatingActionButton(
                    onClick = { isFabExpanded = !isFabExpanded },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    val rotation by animateFloatAsState(if (isFabExpanded) 45f else 0f)
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.rotate(rotation))
                }
            }
        }
    ) { p ->
        val glowColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        Box(modifier = Modifier.fillMaxSize()) {
            // 右上角环境光晕特效
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(glowColor, Color.Transparent),
                        center = Offset(size.width, 0f),
                        radius = size.width * 0.8f
                    )
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(p).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }
                
                // 如果是 SAF 模式且已配置，显示文件列表
                if (isSafMode) {
                    if (safState.isConfigured) {
                        // 显示目录标题
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                                Icon(
                                    Icons.Default.FolderOpen,
                                    null,
                                    tint = safBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "SAF 已授权目录",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        
                        // 显示文件数量统计
                        item {
                            val folderCount = safState.files.count { it.isDirectory }
                            val fileCount = safState.files.count { !it.isDirectory }
                            Text(
                                text = "$folderCount 个文件夹，$fileCount 个文件",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        
                        // 文件列表
                        if (safState.files.isEmpty()) {
                            item {
                                EmptyDirectoryCard()
                            }
                        } else {
                            items(safState.files) { file ->
                                SAFFileCard(file = file)
                            }
                        }
                    } else {
                        // 未配置 SAF 目录
                        item {
                            SAFNotConfiguredCard()
                        }
                    }
                } else {
                    // 非 SAF 模式，显示示例数据
                    item {
                        QuestionCard(
                            num = "01",
                            title = "Listening Comprehension Part A",
                            desc = "The man is asking for directions to the nearest subway station...",
                            color = MaterialTheme.colorScheme.primary,
                            badge = "Parsed"
                        )
                    }
                    item {
                        QuestionCard(
                            num = "02",
                            title = "Reading Passage 1: Climate Ecology",
                            desc = "Recent studies show a significant shift in migratory patterns...",
                            color = MaterialTheme.colorScheme.error,
                            badge = "Review",
                            isWarning = true
                        )
                    }
                    item {
                        QuestionCard(
                            num = "03",
                            title = "Vocabulary & Structure",
                            desc = "Choose the correct word to complete the sentence: The committee...",
                            color = MaterialTheme.colorScheme.primary,
                            badge = "Parsed"
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(88.dp)) } // 避开 FAB 和 BottomNav
            }
        }
    }
}

/**
 * SAF 未配置提示卡片
 */
@Composable
fun SAFNotConfiguredCard() {
    val safBlue = Color(0xFF60A5FA)
    
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = safBlue.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.FolderOff,
                null,
                tint = safBlue,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "尚未配置 SAF 访问目录",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "请在「设置 → 激活授权」中选择 SAF 模式并授权访问目录",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 空目录提示卡片
 */
@Composable
fun EmptyDirectoryCard() {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Folder,
                null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "目录为空",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "此目录中没有文件或文件夹",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * SAF 文件卡片
 */
@Composable
fun SAFFileCard(file: SAFManager.FileInfo) {
    val safBlue = Color(0xFF60A5FA)
    val folderIcon = if (file.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile
    val iconTint = if (file.isDirectory) safBlue else MaterialTheme.colorScheme.onSurfaceVariant
    
    // 日期格式化
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()) }
    val dateStr = remember(file.lastModified) {
        if (file.lastModified > 0) {
            dateFormat.format(Date(file.lastModified))
        } else {
            ""
        }
    }
    
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().clickable { },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 文件/文件夹图标
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (file.isDirectory) safBlue.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    folderIcon,
                    null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(Modifier.width(12.dp))
            
            // 文件信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!file.isDirectory) {
                    Spacer(Modifier.height(2.dp))
                    Row {
                        Text(
                            text = file.getReadableSize(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (dateStr.isNotEmpty()) {
                            Text(
                                text = " • ",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = dateStr,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "文件夹",
                        style = MaterialTheme.typography.labelSmall,
                        color = safBlue
                    )
                }
            }
            
            // 右侧图标
            Icon(
                if (file.isDirectory) Icons.Default.ChevronRight else Icons.Default.MoreVert,
                null,
                tint = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun QuestionCard(num: String, title: String, desc: String, color: Color, badge: String, isWarning: Boolean = false) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().clickable { },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // 使用 drawBehind 绘制极细的左侧状态线
                .drawBehind { drawRect(color = color, size = Size(4.dp.toPx(), size.height)) }
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // 序号块
            Column(
                Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainer),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Q NUM", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(num, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
            }
            Spacer(Modifier.width(16.dp))
            // 内容区
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(4.dp))
                Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            // 标签 Badge
            Surface(
                color = if (isWarning) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(50)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                    if (isWarning) {
                        Icon(Icons.Default.Warning, null, tint = color, modifier = Modifier.size(12.dp).padding(end = 2.dp))
                    }
                    Text(badge, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = if(isWarning) color else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
