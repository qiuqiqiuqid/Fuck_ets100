package com.shuaiqiu.fuckets100

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri
import androidx.navigation.NavHostController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavHostController) {
    val context = LocalContext.current
    var showAboutDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = { FeTopAppBar(title = "Fe") },
        containerColor = MaterialTheme.colorScheme.surface
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
            Text("配置应用行为与个性化选项", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))

            // 运行授权设置
            FeFilledCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    SettingsListItem(Icons.Default.Build, "运行授权", "配置 Shizuku、Root 或其他模式") {
                        context.startActivity(ActivationActivity.createIntent(context))
                    }
                    FeThinDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsListItem(Icons.Default.Tune, "通用设置", "语言、时区等常规选项") {
                        context.startActivity(GeneralSettingsActivity.createIntent(context))
                    }
                    FeThinDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsListItem(Icons.Default.Palette, "主题", "彩色主题与明暗模式") {
                        context.startActivity(ThemeSettingsActivity.createIntent(context))
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // 其他选项
            FeFilledCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    SettingsListItem(Icons.Default.Info, "关于 Fe", "应用信息与致谢", hideChevron = true) {
                        showAboutDialog = true
                    }
                    FeThinDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsListItem(Icons.Default.Gavel, "法律信息与使用守则", "使用前请阅读并遵守", hideChevron = false) {
                        context.startActivity(LegalActivity.createIntent(context))
                    }
                    FeThinDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsListItem(Icons.AutoMirrored.Filled.Help, "访问官网", "lastudio.cc", hideChevron = true) {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://lastudio.cc"))
                        context.startActivity(intent)
                    }
                }
            }

            Spacer(Modifier.height(88.dp))
        }
    }
    
    // 关于对话框
    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        FeFilledCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
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
                    painter = painterResource(id = R.drawable.fe_logo),
                    contentDescription = "应用图标",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                )
                
                Spacer(Modifier.height(16.dp))
                
                // 应用名称
                Text(
                    "Fuck ets100",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(Modifier.height(24.dp))
                
                // 软件信息
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "软件信息",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Fuck ETS100",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "版本: ${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "开源协议: GPL 3.0",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        FeInfoSurface(
                            modifier = Modifier
                                .clickable {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://github.com/qiuqiqiuqid/Fuck_ets100")
                                )
                                context.startActivity(intent)
                                },
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ) {
                            Icon(
                                Icons.Default.Code,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "开源地址",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "查看项目仓库",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "打开开源仓库",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                }
                
                Spacer(Modifier.height(16.dp))
                FeThinDivider()
                Spacer(Modifier.height(16.dp))
                
                // 关于作者
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "关于作者",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Box(modifier = Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_author),
                                contentDescription = "作者头像",
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "帅丘",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        FeInfoSurface(
                            modifier = Modifier
                                .clickable {
                                    context.startActivity(DonateActivity.createIntent(context))
                                },
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ) {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "捐赠支持",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "感谢你的支持",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = "进入捐赠页面",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
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
