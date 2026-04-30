package com.shauiqiu.fe

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
    ) { p ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(p)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("常规偏好", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)

            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Column {
                    GeneralSettingsRow(Icons.Default.Language, "语言 (Language)", "跟随系统 (zh-CN)") {
                        // TODO: 语言切换逻辑
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    GeneralSettingsRow(Icons.Default.Schedule, "时区", "自动获取 (Asia/Shanghai)") {
                        // TODO: 时区切换逻辑
                    }
                }
            }
        }
    }
}

// ✨ 专属私有化列表行组件，断绝一切报错！
@Composable
private fun GeneralSettingsRow(
    icon: ImageVector,
    title: String,
    sub: String,
    onClick: () -> Unit = {}
) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceContainer, CircleShape), Alignment.Center) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f).padding(start = 16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            if (sub.isNotEmpty()) {
                Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.outlineVariant)
    }
}