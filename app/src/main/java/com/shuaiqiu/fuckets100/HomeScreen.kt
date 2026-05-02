package com.shuaiqiu.fuckets100

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

/**
 * 首页主屏幕
 * 显示系统状态、设备信息和激活状态
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(mode: ActivationMode, shizukuState: ShizukuState, navController: NavHostController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // 权限状态 - 使用 mutableStateOf 确保 UI 自动更新
    var hasFilesPerm by remember { mutableStateOf(PermissionsHelper.hasAllFilesAccess()) }
    var hasOverlayPerm by remember { mutableStateOf(PermissionsHelper.hasOverlayPermission(context)) }
    var hasAppListPerm by remember { mutableStateOf(PermissionsHelper.hasAppListPermission()) }
    var hasRootAvailable by remember { mutableStateOf(RootManager.isRootAvailable()) }
    
    // ETS 应用信息状态
    var etsAppInfo by remember { mutableStateOf<Pair<Boolean, String>?>(null) } // (已安装, 版本号)
    
    // 生命周期监听 - 从系统设置返回时自动刷新权限状态
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasFilesPerm = PermissionsHelper.hasAllFilesAccess()
                hasOverlayPerm = PermissionsHelper.hasOverlayPermission(context)
                hasAppListPerm = PermissionsHelper.hasAppListPermission()
                // 刷新 ETS 应用信息
                etsAppInfo = getAppInfo(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    // 首次加载时获取 ETS 应用信息
    LaunchedEffect(Unit) {
        etsAppInfo = getAppInfo(context)
    }
    
    // 检查基础权限是否全部获取
    val hasAllBasicPermissions = hasFilesPerm && hasOverlayPerm && hasAppListPerm
    
    // 判断是否真正激活 - 根据不同模式判断激活条件
    // Direct Read 模式会检测零宽字符漏洞绕过限制，而其他模式需要相应的权限和配置
    val isTrulyActivated = when {
        mode == ActivationMode.SHIZUKU -> shizukuState.isRunning && shizukuState.permissionGranted && hasAllBasicPermissions
        mode == ActivationMode.ROOT -> hasAllBasicPermissions && RootManager.isRootAvailable()
        mode == ActivationMode.SAF -> hasAllBasicPermissions && SAFManager.isConfigured() && SAFManager.isCorrectDirectory()
        mode == ActivationMode.DIRECT_READ -> hasAllBasicPermissions && ZWCHelper.isDirectReadAvailable()
        mode != ActivationMode.DEFAULT -> hasAllBasicPermissions
        else -> false
    }

    // 系统状态标签和颜色
    val sysLabel = if (isTrulyActivated) "SYS_READY" else "SYS_OFFLINE"
    val activeColor = if (isTrulyActivated) mode.hexColor else Color(0xFFDC2626) // 未激活时显示红色警告
    
    Scaffold(
        topBar = { FeTopAppBar(title = "Fe") },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { /* 启动答题功能 */ },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Default.PlayArrow, null) },
                text = { Text("开始答题", fontWeight = FontWeight.Bold) }
            )
        }
    ) { paddingValues ->
        Column(
            Modifier
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // 系统状态指示器 + 模式标签
            val dotColor by animateColorAsState(targetValue = activeColor, animationSpec = tween(500))
            Row(
                Modifier.fillMaxWidth(), 
                horizontalArrangement = Arrangement.End, 
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(6.dp).background(dotColor, CircleShape))
                Spacer(Modifier.width(8.dp))
                Text(
                    sysLabel, 
                    style = MaterialTheme.typography.labelSmall, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 状态卡片 + 设备卡片
            StatusCard(
                mode = mode, 
                shizukuState = shizukuState, 
                isTrulyActivated = isTrulyActivated, 
                activeColor = activeColor, 
                navController = navController
            )
            DeviceCard(activeColor = activeColor, etsAppInfo = etsAppInfo)
        }
    }
}

/**
 * 获取应用信息
 * @param packageName 应用包名
 * @return Pair(已安装, 版本号)
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

/**
 * 状态卡片组件
 * 显示当前激活模式和权限状态
 */
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
    
    // 重新检查基础权限状态
    val hasFilesPerm = PermissionsHelper.hasAllFilesAccess()
    val hasOverlayPerm = PermissionsHelper.hasOverlayPermission(context)
    val hasAppListPerm = PermissionsHelper.hasAppListPermission()
    val hasAllBasicPermissions = hasFilesPerm && hasOverlayPerm && hasAppListPerm

    // 根据当前状态显示不同的标题
    val displayTitle = when {
        // 基础权限未授予时显示等待授权提示
        !hasAllBasicPermissions -> "等待授权"
        mode == ActivationMode.SHIZUKU && shizukuState.isRunning && !shizukuState.permissionGranted -> "Shizuku 等待授权"
        mode == ActivationMode.SHIZUKU && !shizukuState.isRunning -> "Shizuku 未运行"
        mode == ActivationMode.ROOT && !RootManager.isRootAvailable() -> "Root 未获取"
        mode == ActivationMode.SAF && !SAFManager.isConfigured() -> "SAF 等待配置"
        mode == ActivationMode.SAF && !SAFManager.isCorrectDirectory() -> "SAF 目录错误"
        else -> mode.title
    }

    // 选择图标 - 激活时使用模式图标，未激活时使用警告图标
    val displayIcon = if (isTrulyActivated) mode.icon else Icons.Default.Warning

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clickable { navController.navigate(Screen.Activation.route) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 背景渐变效果
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(animatedColor.copy(alpha = 0.2f), Color.Transparent),
                        center = Offset(0f, size.height),
                        radius = size.height * 0.8f
                    )
                )
            }
            
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 顶部行 - 图标和设置按钮
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Box(
                        Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(displayIcon, null, tint = animatedColor)
                    }
                    Icon(
                        Icons.Default.Settings, 
                        null, 
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
                
                // 底部区域 - 状态信息
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "STATUS", 
                        style = MaterialTheme.typography.labelSmall, 
                        color = animatedColor.copy(alpha = 0.8f)
                    )
                    Text(
                        displayTitle, 
                        style = MaterialTheme.typography.displaySmall, 
                        fontWeight = FontWeight.Bold, 
                        color = animatedColor
                    )

                    // 详细说明文字
                    if (!hasAllBasicPermissions) {
                        val subText = when {
                            !hasFilesPerm -> "需要文件访问权限才能读取题库，请授权"
                            !hasOverlayPerm -> "需要悬浮窗权限才能显示答题界面，请授权"
                            !hasAppListPerm -> "需要应用列表权限才能检测ETS应用，请授权"
                            else -> "请授权上述权限后即可使用"
                        }
                        Text(
                            text = subText, 
                            style = MaterialTheme.typography.labelSmall, 
                            color = animatedColor.copy(alpha = 0.6f)
                        )
                    } else if (mode == ActivationMode.SHIZUKU) {
                        val subText = when {
                            !shizukuState.isRunning -> "请启动Shizuku后再返回此处"
                            !shizukuState.permissionGranted -> "请授予Shizuku权限后即可使用"
                            else -> {
                                val uidStr = when (shizukuState.uid) {
                                    0 -> "Root (0)"
                                    2000 -> "ADB (2000)"
                                    else -> shizukuState.uid.toString()
                                }
                                "${shizukuState.getRuntimeTypeName()} v${shizukuState.version} | UID: $uidStr"
                            }
                        }
                        Text(
                            text = subText, 
                            style = MaterialTheme.typography.labelSmall, 
                            color = animatedColor.copy(alpha = 0.6f)
                        )
                    } else if (mode == ActivationMode.SAF) {
                        val subText = when {
                            !SAFManager.isConfigured() -> "请先选择授权目录"
                            !SAFManager.isCorrectDirectory() -> "选择的目录不正确，请重新选择"
                            else -> SAFManager.getSavedDirectoryName() ?: "已授权目录"
                        }
                        Text(
                            text = subText, 
                            style = MaterialTheme.typography.labelSmall, 
                            color = animatedColor.copy(alpha = 0.6f)
                        )
                    } else if (mode == ActivationMode.ROOT) {
                        val subText = when {
                            !RootManager.isRootAvailable() -> "未检测到Root权限，请先获取Root"
                            else -> "Root 权限已获取"
                        }
                        Text(
                            text = subText, 
                            style = MaterialTheme.typography.labelSmall, 
                            color = animatedColor.copy(alpha = 0.6f)
                        )
                    } else if (mode == ActivationMode.DIRECT_READ) {
                        Text(
                            text = "Direct Read 模式已就绪", 
                            style = MaterialTheme.typography.labelSmall, 
                            color = animatedColor.copy(alpha = 0.6f)
                        )
                    } else if (mode == ActivationMode.DEFAULT) {
                        Text(
                            text = "请授权基础权限后选择激活模式", 
                            style = MaterialTheme.typography.labelSmall, 
                            color = animatedColor.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 设备卡片组件
 * 显示设备信息和ETS应用安装状态
 */
@Composable
fun DeviceCard(activeColor: Color, etsAppInfo: Pair<Boolean, String>?) {
    val animatedColor by animateColorAsState(targetValue = activeColor, animationSpec = tween(600))
    val themePrimaryColor = MaterialTheme.colorScheme.primary // 使用主题色
    val successColor = Color(0xFF4ADE80) // 成功状态绿色
    val errorColor = Color(0xFFDC2626) // 错误状态红色
    
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // 第一行：设备名称和系统版本
            val deviceModel = android.os.Build.MODEL
            val androidVersion = android.os.Build.VERSION.RELEASE
            Row(
                modifier = Modifier.fillMaxWidth(), 
                horizontalArrangement = Arrangement.SpaceBetween, 
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Smartphone, null, tint = themePrimaryColor)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            "DEVICE", 
                            style = MaterialTheme.typography.labelSmall, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            deviceModel, 
                            style = MaterialTheme.typography.bodyMedium, 
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "OS", 
                        style = MaterialTheme.typography.labelSmall, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Android $androidVersion", 
                        style = MaterialTheme.typography.bodyMedium, 
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            
            // 第二行：ETS应用安装状态和版本
            Row(
                modifier = Modifier.fillMaxWidth(), 
                horizontalArrangement = Arrangement.SpaceBetween, 
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 根据ETS应用是否安装选择图标颜色
                    Icon(
                        Icons.Default.Memory, 
                        null, 
                        tint = if (etsAppInfo?.first == true) successColor else errorColor
                    )
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            "ETS应用", 
                            style = MaterialTheme.typography.labelSmall, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (etsAppInfo?.first == true) "已安装" else "未安装",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (etsAppInfo?.first == true) successColor else errorColor
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "VERSION", 
                        style = MaterialTheme.typography.labelSmall, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = etsAppInfo?.second?.takeIf { it.isNotEmpty() } ?: "未知",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
