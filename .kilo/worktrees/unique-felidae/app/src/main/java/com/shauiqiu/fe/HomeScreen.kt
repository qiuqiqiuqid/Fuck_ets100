package com.shauiqiu.fe

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavHostController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(mode: ActivationMode, shizukuState: ShizukuState, navController: NavHostController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // 权限状态 - 使用 mutableStateOf 以便刷新 UI
    var hasFilesPerm by remember { mutableStateOf(PermissionsHelper.hasAllFilesAccess()) }
    var hasOverlayPerm by remember { mutableStateOf(PermissionsHelper.hasOverlayPermission(context)) }
    var hasAppListPerm by remember { mutableStateOf(PermissionsHelper.hasAppListPermission()) }
    var hasRootAvailable by remember { mutableStateOf(RootManager.isRootAvailable()) }
    
    // e听说应用信息状态
    var etsAppInfo by remember { mutableStateOf<Pair<Boolean, String>?>(null) } // (是否安装, 版本号)
    
    // 监听生命周期，从设置返回时自动刷新权限状态
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasFilesPerm = PermissionsHelper.hasAllFilesAccess()
                hasOverlayPerm = PermissionsHelper.hasOverlayPermission(context)
                hasAppListPerm = PermissionsHelper.hasAppListPermission()
                // 刷新 e听说应用信息
                etsAppInfo = getAppInfo(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    // 初始化时获取 e听说应用信息
    LaunchedEffect(Unit) {
        etsAppInfo = getAppInfo(context)
    }
    
    val hasAllBasicPermissions = hasFilesPerm && hasOverlayPerm && hasAppListPerm
    
    // 动态计算最真实的激活状态（必须同时满足运行状态和三个基础权限）
    // Direct Read 模式需要额外检测直读可用性（零宽字符漏洞或全文件访问权限）
    val isTrulyActivated = when {
        mode == ActivationMode.SHIZUKU -> shizukuState.isRunning && shizukuState.permissionGranted && hasAllBasicPermissions
        mode == ActivationMode.ROOT -> hasAllBasicPermissions && RootManager.isRootAvailable()
        mode == ActivationMode.SAF -> hasAllBasicPermissions && SAFManager.isConfigured() && SAFManager.isCorrectDirectory()
        mode == ActivationMode.DIRECT_READ -> hasAllBasicPermissions && ZWCHelper.isDirectReadAvailable()
        mode != ActivationMode.DEFAULT -> hasAllBasicPermissions
        else -> false
    }

    val sysLabel = if (isTrulyActivated) "SYS_READY" else "SYS_OFFLINE"
    val activeColor = if (isTrulyActivated) mode.hexColor else Color(0xFFDC2626) // 深红色用于未授权状态

    Scaffold(
        topBar = { FeTopAppBar(title = "Fe") },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { /* 启动读取逻辑 */ },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Default.PlayArrow, null) },
                text = { Text("开始读取", fontWeight = FontWeight.Bold) }
            )
        }
    ) { p ->
        Column(Modifier.padding(p).padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(20.dp)) {

            // 右上角呼吸灯动画 + 真实状态显示
            val dotColor by animateColorAsState(targetValue = activeColor, animationSpec = tween(500))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(6.dp).background(dotColor, CircleShape))
                Spacer(Modifier.width(8.dp))
                Text(sysLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // 核心组件区
            StatusCard(mode = mode, shizukuState = shizukuState, isTrulyActivated = isTrulyActivated, activeColor = activeColor, navController = navController)
            DeviceCard(activeColor = activeColor, etsAppInfo = etsAppInfo)
        }
    }
}

/**
 * 获取指定应用的信息
 * @param packageName 应用包名
 * @return Pair(是否安装, 版本号)
 */
private fun getAppInfo(context: android.content.Context, packageName: String = "com.ets100.secondary"): Pair<Boolean, String>? {
    return try {
        val packageManager = context.packageManager
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        val versionName = packageInfo.versionName ?: "未知"
        Pair(true, versionName)
    } catch (e: android.content.pm.PackageManager.NameNotFoundException) {
        Pair(false, "")
    } catch (e: Exception) {
        null
    }
}

@Composable
fun StatusCard(
    mode: ActivationMode,
    shizukuState: ShizukuState,
    isTrulyActivated: Boolean,
    activeColor: Color,
    navController: NavHostController
) {
    val context = LocalContext.current
    val animatedColor by animateColorAsState(targetValue = activeColor, animationSpec = tween(600))
    
    // 检查三个基础权限是否都已授权
    val hasFilesPerm = PermissionsHelper.hasAllFilesAccess()
    val hasOverlayPerm = PermissionsHelper.hasOverlayPermission(context)
    val hasAppListPerm = PermissionsHelper.hasAppListPermission()
    val hasAllBasicPermissions = hasFilesPerm && hasOverlayPerm && hasAppListPerm

    val displayTitle = when {
        // 三个基础权限未完全授权时，所有模式都显示待激活
        !hasAllBasicPermissions -> "待激活"
        mode == ActivationMode.SHIZUKU && shizukuState.isRunning && !shizukuState.permissionGranted -> "Shizuku 待授权"
        mode == ActivationMode.SHIZUKU && !shizukuState.isRunning -> "Shizuku 未运行"
        mode == ActivationMode.ROOT && !RootManager.isRootAvailable() -> "Root 未获取"
        mode == ActivationMode.SAF && !SAFManager.isConfigured() -> "SAF 待配置"
        mode == ActivationMode.SAF && !SAFManager.isCorrectDirectory() -> "SAF 路径不正确"
        else -> mode.title
    }

    val displayIcon = if (isTrulyActivated) mode.icon else Icons.Default.Warning

    ElevatedCard(
        modifier = Modifier.fillMaxWidth().height(220.dp).clickable { navController.navigate(Screen.Activation.route) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(animatedColor.copy(alpha = 0.2f), Color.Transparent),
                        center = Offset(0f, size.height),
                        radius = size.height * 0.8f
                    )
                )
            }
            Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Box(Modifier.size(48.dp).background(MaterialTheme.colorScheme.surfaceContainer, CircleShape), Alignment.Center) {
                        Icon(displayIcon, null, tint = animatedColor)
                    }
                    Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                }
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
                    Text("STATUS", style = MaterialTheme.typography.labelSmall, color = animatedColor.copy(alpha = 0.8f))
                    Text(displayTitle, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = animatedColor)

                    // 基础权限未授权时的引导文案
                    if (!hasAllBasicPermissions) {
                        val subText = when {
                            !hasFilesPerm -> "全文件访问权限未授权，点击进行配置"
                            !hasOverlayPerm -> "悬浮窗权限未授权，点击进行配置"
                            !hasAppListPerm -> "应用列表权限未授权，点击进行配置"
                            else -> "点击配置系统权限"
                        }
                        Text(text = subText, style = MaterialTheme.typography.labelSmall, color = animatedColor.copy(alpha = 0.6f))
                    } else if (mode == ActivationMode.SHIZUKU) {
                        val subText = when {
                            !shizukuState.isRunning -> "底层服务未启动，点击进行配置"
                            !shizukuState.permissionGranted -> "尚未授予应用权限，点击进行配置"
                            else -> {
                                val uidStr = when (shizukuState.uid) {
                                    0 -> "Root (0)"
                                    2000 -> "ADB (2000)"
                                    else -> shizukuState.uid.toString()
                                }
                                "${shizukuState.getRuntimeTypeName()} v${shizukuState.version} • UID: $uidStr"
                            }
                        }
                        Text(text = subText, style = MaterialTheme.typography.labelSmall, color = animatedColor.copy(alpha = 0.6f))
                    } else if (mode == ActivationMode.SAF) {
                        val subText = when {
                            !SAFManager.isConfigured() -> "尚未选择授权目录，点击进行配置"
                            !SAFManager.isCorrectDirectory() -> "选择的目录不正确，点击重新选择"
                            else -> SAFManager.getSavedDirectoryName() ?: "已授权目录"
                        }
                        Text(text = subText, style = MaterialTheme.typography.labelSmall, color = animatedColor.copy(alpha = 0.6f))
                    } else if (mode == ActivationMode.ROOT) {
                        val subText = when {
                            !RootManager.isRootAvailable() -> "未获取 Root 权限，点击进行配置"
                            else -> "Root 权限已就绪"
                        }
                        Text(text = subText, style = MaterialTheme.typography.labelSmall, color = animatedColor.copy(alpha = 0.6f))
                    } else if (mode == ActivationMode.DIRECT_READ) {
                        Text(text = "Direct Read 直读模式已就绪", style = MaterialTheme.typography.labelSmall, color = animatedColor.copy(alpha = 0.6f))
                    } else if (mode == ActivationMode.DEFAULT) {
                        Text(text = "点击配置系统权限", style = MaterialTheme.typography.labelSmall, color = animatedColor.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceCard(activeColor: Color, etsAppInfo: Pair<Boolean, String>?) {
    val animatedColor by animateColorAsState(targetValue = activeColor, animationSpec = tween(600))
    val themePrimaryColor = MaterialTheme.colorScheme.primary // 使用主题主色
    val successColor = Color(0xFF4ADE80) // 绿色表示已安装
    val errorColor = Color(0xFFDC2626) // 红色表示未安装
    
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Smartphone, null, tint = themePrimaryColor) // 主题主色
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("DEVICE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("OnePlus", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("OS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Android 16", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Memory, null, tint = if (etsAppInfo?.first == true) successColor else errorColor) // 已安装绿色，未安装红色
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("e听说", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = if (etsAppInfo?.first == true) "已安装" else "未安装",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (etsAppInfo?.first == true) successColor else errorColor
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("VERSION", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = etsAppInfo?.second?.takeIf { it.isNotEmpty() } ?: "—",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
