package com.shuaiqiu.fuckets100

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController

/**
 * 调试页面
 * 用于显示索引文件和 fileIdentifier 等调试信息
 * 喵~ 这个页面默认隐藏，只有在通用设置中开启调试模式后才显示喵！
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(navController: NavHostController) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("调试页面", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    // 刷新按钮
                    IconButton(onClick = { /* 重新加载数据 */ }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 调试说明
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "调试模式已开启，此页面仅用于问题排查喵~",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // 索引管理器状态
            DebugSection(title = "索引管理器状态") {
                DebugInfoRow("BeijingIndexManager", "C系列: ${BeijingIndexManager.getStats()}")
                DebugInfoRow("ResourceIndexManager", "G系列: ${ResourceIndexManager.getStats()}")
                DebugInfoRow("初始化状态", "Beijing: ${BeijingIndexManager.isReady()}, Resource: ${ResourceIndexManager.isReady()}")
            }

            // 索引文件列表
            DebugSection(title = "索引文件 (assets/etsresource/)") {
                val indexFiles = listOf(
                    "beijing-C1.json" to "初中一年级北京索引",
                    "beijing-C2.json" to "初中二年级北京索引",
                    "beijing-C3.json" to "初中三年级北京索引",
                    "beijing-G1.json" to "高中一年级北京索引",
                    "beijing-G2.json" to "高中二年级北京索引",
                    "beijing-G3.json" to "高中三年级北京索引",
                    "resource-C1.json" to "初中一年级资源索引",
                    "resource-C2.json" to "初中二年级资源索引",
                    "resource-C3.json" to "初中三年级资源索引",
                    "resource-G1.json" to "高中一年级资源索引",
                    "resource-G2.json" to "高中二年级资源索引",
                    "resource-G3.json" to "高中三年级资源索引"
                )
                indexFiles.forEach { (fileName, description) ->
                    DebugInfoRow(fileName, description)
                }
            }

            // byFileIdentifier 结构说明
            DebugSection(title = "byFileIdentifier 查询原理") {
                Text(
                    "当设备的 paperId 在索引中不存在时，会尝试通过 fileIdentifier 在 byFileIdentifier 中反向查找喵~",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "数据结构：fileIdentifier → {id, name, fileIdentifiers[]}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // 设备数据路径
            DebugSection(title = "设备数据路径") {
                DebugInfoRow("Data 目录", "/storage/emulated/0/Android/data/com.ets100.secondary/files/Download/ETS_secondary/data/")
                DebugInfoRow("Resource 目录", "/storage/emulated/0/Android/data/com.ets100.secondary/files/Download/ETS_secondary/resource/")
            }

            // 日志输出示例
            DebugSection(title = "解析日志格式") {
                Text(
                    """
                    | parsePaper: paperId=760241, firstFileIdentifier=2b55c4a6bdc3e950cf81f7a0818464d3
                    | parsePaper: 通过 fileIdentifier 找到试卷: 2025-BSD 必修一 U1A
                    | 加载 beijing-G1.json: 660 条记录 (items), 8200 条记录 (byFileIdentifier)
                    """.trimMargin(),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * 调试区块组件
 */
@Composable
private fun DebugSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

/**
 * 调试信息行组件
 */
@Composable
private fun DebugInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.6f)
        )
    }
}