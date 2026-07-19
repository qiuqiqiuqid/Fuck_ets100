package com.shuaiqiu.fuckets100

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalScreen(onBack: () -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("法律信息与使用守则", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LegalContent(showHeader = true)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun LegalAcceptanceDialog(onAccepted: () -> Unit) {
    var checked by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = {},
        icon = {
            Icon(
                Icons.Default.Gavel,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(36.dp)
            )
        },
        title = { Text("使用守则与法律声明") },
        text = {
            Column {
                Column(
                    modifier = Modifier
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    LegalContent(showHeader = false)
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(checked = checked, onCheckedChange = { checked = it })
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "我已阅读并同意上述使用守则与法律声明",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onAccepted,
                enabled = checked
            ) {
                Text("同意并继续")
            }
        }
    )
}

@Composable
fun LegalContent(showHeader: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (showHeader) {
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "请先阅读",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "使用本软件前，请确认你理解并接受以下限制。违反学校、平台或法律规定造成的后果由使用者自行承担。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        LegalSection(
            title = "使用守则",
            icon = Icons.Default.Gavel,
            content = "1. 本软件仅供学习交流、数据解析研究和个人备份参考使用。\n" +
                "2. 请勿将本软件用于考试作弊、规避教学管理、批量攻击接口或其他违规用途。\n" +
                "3. 使用前请确保你已拥有 e听说 / ETS100 的合法账号和正版服务。\n" +
                "4. 云端模式可能导致 E听说官方客户端退出登录，请确认没有正在考试、练习或录音提交。\n" +
                "5. 使用本软件即表示你愿意自行承担使用结果。"
        )

        LegalSection(
            title = "隐私与数据",
            icon = Icons.Default.Security,
            content = "1. 本地读取模式不会上传你的本地文件。\n" +
                "2. 云端模式需要使用你的 ETS100 账号登录 ETS100 接口，并在本机保存登录信息用于后续读取。\n" +
                "3. 请妥善保管自己的账号和设备，不要在不可信设备上登录。\n" +
                "4. 第三方工具或服务的数据处理规则以其自身政策为准。"
        )

        LegalSection(
            title = "免责声明",
            icon = Icons.Default.Warning,
            content = "本软件按现状提供，不承诺适用于任何特定用途。作者不对因使用、误用、账号状态变化、考试中断、数据丢失或平台规则变化导致的任何直接或间接损失承担责任。"
        )
    }
}

@Composable
private fun LegalSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                content,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.35f
            )
        }
    }
}
