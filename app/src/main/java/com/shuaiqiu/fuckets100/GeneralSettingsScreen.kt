package com.shuaiqiu.fuckets100

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

/**
 * 通用设置页面
 * 包含语言设置、时区设置等通用选项
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralSettingsScreen(navController: NavHostController) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("通用设置", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
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
            // 通用设置分组标题
            Text("通用设置", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)

            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Column {
                    // 语言设置
                    GeneralSettingsRow(
                        icon = Icons.Default.Language, 
                        title = "语言设置 (Language)", 
                        sub = "简体中文 (zh-CN)"
                    ) {
                        // TODO: 语言设置功能待实现
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp), 
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                    )
                    // 时区设置
                    GeneralSettingsRow(
                        icon = Icons.Default.Schedule, 
                        title = "时区设置", 
                        sub = "自动获取设备时区 (Asia/Shanghai)"
                    ) {
                        // TODO: 时区设置功能待实现
                    }
                }
            }
        }
    }
}

/**
 * 通用设置行组件
 * 用于显示单个设置项
 *
 * @param icon 设置项图标
 * @param title 设置项标题
 * @param sub 设置项副标题/描述
 * @param onClick 点击事件处理
 */
@Composable
private fun GeneralSettingsRow(
    icon: ImageVector,
    title: String,
    sub: String,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 图标容器
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon, 
                null, 
                tint = MaterialTheme.colorScheme.primary, 
                modifier = Modifier.size(20.dp)
            )
        }
        
        // 标题和副标题
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        ) {
            Text(
                title, 
                style = MaterialTheme.typography.titleMedium, 
                color = MaterialTheme.colorScheme.onSurface
            )
            if (sub.isNotEmpty()) {
                Text(
                    sub, 
                    style = MaterialTheme.typography.bodySmall, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // 箭头指示
        Icon(
            Icons.Default.ChevronRight, 
            null, 
            tint = MaterialTheme.colorScheme.outlineVariant
        )
    }
}
