package com.shuaiqiu.fuckets100

import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivationSettingsScreen(
    currentMode: ActivationMode,
    shizukuState: ShizukuState,
    onModeSelected: (ActivationMode) -> Unit,
    onBack: () -> Unit,
    onNavigateToCloudActivation: () -> Unit,
    onNavigateToRead: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // 基础权限动态状态
    var hasFilesPerm by remember { mutableStateOf(PermissionsHelper.hasAllFilesAccess()) }
    var hasOverlayPerm by remember { mutableStateOf(PermissionsHelper.hasOverlayPermission(context)) }
    var hasAppListPerm by remember { mutableStateOf(PermissionsHelper.hasAppListPermission()) }
    var cloudLoggedIn by remember { mutableStateOf(ETS100AuthManager.isLoggedIn(context)) }
    var cloudPhone by remember { mutableStateOf(ETS100AuthManager.getPhone(context)) }
    var etsAppInfo by remember { mutableStateOf(getEtsAppInfo(context)) }

    // Shizuku 权限请求状态
    var pendingPermissionRequest by remember { mutableStateOf(false) }

    fun refreshCloudAuthState() {
        cloudLoggedIn = ETS100AuthManager.isLoggedIn(context)
        cloudPhone = ETS100AuthManager.getPhone(context)
    }

    // 监听生命周期，从系统设置返回时自动刷新基础权限的绿勾勾
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasFilesPerm = PermissionsHelper.hasAllFilesAccess()
                hasOverlayPerm = PermissionsHelper.hasOverlayPermission(context)
                hasAppListPerm = PermissionsHelper.hasAppListPermission()
                etsAppInfo = getEtsAppInfo(context)
                refreshCloudAuthState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // 处理 Shizuku 权限请求回调
    LaunchedEffect(Unit) {
        val permissionListener: (Int, Int) -> Unit = { requestCode, grantResult ->
            if (requestCode == ShizukuManager.REQUEST_CODE_SHIZUKU_PERMISSION) {
                pendingPermissionRequest = false
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(context, "Shizuku 权限授权成功！", Toast.LENGTH_SHORT).show()
                    onModeSelected(ActivationMode.SHIZUKU)
                } else {
                    Toast.makeText(context, "Shizuku 权限被拒绝了", Toast.LENGTH_SHORT).show()
                }
            }
        }
        ShizukuManager.addPermissionResultListener(permissionListener)
    }

    val successColor = MaterialTheme.colorScheme.primary
    val hasLocalBasicPermissions = hasFilesPerm && hasAppListPerm

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("运行授权与权限", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        }
    ) { p ->
        LazyColumn(Modifier.padding(p).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

            // 当前模式状态卡片
            item {
                FeModeStatusCard(
                    currentMode = currentMode,
                    shizukuState = shizukuState,
                    hasAllBasicPermissions = hasLocalBasicPermissions,
                    cloudLoggedIn = cloudLoggedIn,
                    cloudPhone = cloudPhone,
                    context = context
                )
                Spacer(Modifier.height(8.dp))
            }

            item {
                ActivationDeviceCard(etsAppInfo = etsAppInfo)
            }

            // 基础权限申请区域
            item {
                Text("系统基础权限", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                FeFilledCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        // 权限 1：全文件访问
                        SettingsListItem(
                            icon = if (hasFilesPerm) Icons.Default.CheckCircle else Icons.Default.FolderSpecial,
                            iconTint = if (hasFilesPerm) successColor else MaterialTheme.colorScheme.primary,
                            title = "全文件访问",
                            sub = if (hasFilesPerm) "已授权" else "读取设备底层目录结构"
                        ) {
                            if (!hasFilesPerm) PermissionsHelper.requestAllFilesAccess(context)
                            else Toast.makeText(context, "已经拥有全文件访问权限啦！", Toast.LENGTH_SHORT).show()
                        }
                        FeThinDivider(Modifier.padding(horizontal = 16.dp))

                        // 权限 2：悬浮窗
                        SettingsListItem(
                            icon = if (hasOverlayPerm) Icons.Default.CheckCircle else Icons.Default.WebAsset,
                            iconTint = if (hasOverlayPerm) successColor else MaterialTheme.colorScheme.primary,
                            title = "悬浮窗权限",
                            sub = if (hasOverlayPerm) "已授权" else "允许在其他应用上层显示组件"
                        ) {
                            if (!hasOverlayPerm) PermissionsHelper.requestOverlayPermission(context)
                            else Toast.makeText(context, "悬浮窗已经开启啦！", Toast.LENGTH_SHORT).show()
                        }
                        FeThinDivider(Modifier.padding(horizontal = 16.dp))

                        // 权限 3：应用列表
                        SettingsListItem(
                            icon = if (hasAppListPerm) Icons.Default.CheckCircle else Icons.Default.Apps,
                            iconTint = if (hasAppListPerm) successColor else MaterialTheme.colorScheme.primary,
                            title = "应用列表权限",
                            sub = if (hasAppListPerm) "已授权" else "获取设备已安装的软件包信息"
                        ) {
                            if (!hasAppListPerm) PermissionsHelper.requestAppListPermission(context)
                            else Toast.makeText(context, "已经可以读取应用列表啦！", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            item { Text("选择工作授权模式", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) }

            // 动态展开的授权模式卡片区
            items(ActivationMode.values().filter { it != ActivationMode.DEFAULT }.size) { index ->
                val mode = ActivationMode.values().filter { it != ActivationMode.DEFAULT }[index]
                val isSelected = currentMode == mode

                FeFilledCard(
                    onClick = { onModeSelected(mode) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    containerColor = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    },
                    borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.Top) {
                            RadioButton(selected = isSelected, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary))
                            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text(mode.name, fontWeight = FontWeight.Bold, color = if(isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                                    val badgeContainerColor = when (mode) {
                                        ActivationMode.ROOT -> MaterialTheme.colorScheme.errorContainer
                                        ActivationMode.CLOUD -> if (cloudLoggedIn) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                                        else -> MaterialTheme.colorScheme.primaryContainer
                                    }
                                    val badgeContentColor = when (mode) {
                                        ActivationMode.ROOT -> MaterialTheme.colorScheme.onErrorContainer
                                        ActivationMode.CLOUD -> if (cloudLoggedIn) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                                        else -> MaterialTheme.colorScheme.onPrimaryContainer
                                    }
                                    Surface(color = badgeContainerColor, contentColor = badgeContentColor, shape = RoundedCornerShape(50)) {
                                        val displayBadge = when {
                                            mode == ActivationMode.SHIZUKU && shizukuState.isRunning && shizukuState.permissionGranted -> "UID: ${shizukuState.uid}"
                                            mode == ActivationMode.CLOUD && cloudLoggedIn -> "已激活"
                                            mode == ActivationMode.CLOUD -> "未激活"
                                            else -> mode.badge
                                        }
                                        Text(displayBadge, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(mode.desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        // 核心交互：选中后丝滑展开的执行面板
                        AnimatedVisibility(
                            visible = isSelected,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column {
                                FeThinDivider(modifier = Modifier.padding(vertical = 12.dp))

                                when (mode) {
                                    ActivationMode.SHIZUKU -> {
                                        FeShizukuActivationPanel(
                                            shizukuState = shizukuState,
                                            onRequestPermission = {
                                                if (shizukuState.isRunning) {
                                                    if (ShizukuManager.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                                                        Toast.makeText(context, "Shizuku 已授权！", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        pendingPermissionRequest = true
                                                        ShizukuManager.requestPermission(ShizukuManager.REQUEST_CODE_SHIZUKU_PERMISSION)
                                                    }
                                                } else {
                                                    ShizukuManager.openShizukuApp(context)
                                                }
                                            },
                                            onOpenShizuku = { ShizukuManager.openShizukuApp(context) }
                                        )
                                    }
                                    ActivationMode.ROOT -> {
                                        FeRootActivationPanel(context)
                                    }
                                    ActivationMode.DIRECT_READ -> {
                                        FeDirectReadActivationPanel(hasLocalBasicPermissions)
                                    }
                                    ActivationMode.CLOUD -> {
                                        FeCloudActivationPanel(
                                            context = context,
                                            isLoggedIn = cloudLoggedIn,
                                            phone = cloudPhone,
                                            onCloudAuthChanged = { refreshCloudAuthState() },
                                            onNavigateToCloudActivation = onNavigateToCloudActivation,
                                            onNavigateToRead = onNavigateToRead
                                        )
                                    }
                                    else -> {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text("需要手动确认以完成激活", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Button(
                                                onClick = { Toast.makeText(context, "正在请求 ${mode.name} 激活...", Toast.LENGTH_SHORT).show() },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), modifier = Modifier.height(32.dp)
                                            ) { Text("执行激活", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

private fun getEtsAppInfo(
    context: android.content.Context,
    packageName: String = "com.ets100.secondary"
): Pair<Boolean, String>? {
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
private fun ActivationDeviceCard(etsAppInfo: Pair<Boolean, String>?) {
    val themePrimaryColor = MaterialTheme.colorScheme.primary
    val successColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error

    FeFilledCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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
                            Build.MODEL ?: "Unknown",
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
                        "Android ${Build.VERSION.RELEASE}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            FeThinDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
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

@Composable
fun FeModeStatusCard(
    currentMode: ActivationMode,
    shizukuState: ShizukuState,
    hasAllBasicPermissions: Boolean,
    cloudLoggedIn: Boolean,
    cloudPhone: String?,
    context: android.content.Context
) {
    var isRootAvailable by remember { mutableStateOf(RootManager.isRootAvailable()) }
    var isDirectReadAvailable by remember { mutableStateOf(ZWCHelper.isDirectReadAvailable()) }
    
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isRootAvailable = RootManager.isRootAvailable()
                isDirectReadAvailable = ZWCHelper.isDirectReadAvailable()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    
    val errorRedColor = MaterialTheme.colorScheme.error
    
    val statusColor = when (currentMode) {
        ActivationMode.DEFAULT -> MaterialTheme.colorScheme.outline
        ActivationMode.SHIZUKU -> if (shizukuState.isRunning && shizukuState.permissionGranted) MaterialTheme.colorScheme.primary else errorRedColor
        ActivationMode.ROOT -> if (hasAllBasicPermissions && isRootAvailable) MaterialTheme.colorScheme.primary else errorRedColor
        ActivationMode.DIRECT_READ -> if (hasAllBasicPermissions && isDirectReadAvailable) MaterialTheme.colorScheme.primary else errorRedColor
        ActivationMode.CLOUD -> if (cloudLoggedIn) MaterialTheme.colorScheme.primary else errorRedColor
    }
    
    val isActive = statusColor == MaterialTheme.colorScheme.primary
    val animatedColor by animateColorAsState(statusColor, tween(800))
    val statusContainerColor = if (isActive) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }
    
    val (statusTitle, statusDesc, statusDetail) = when (currentMode) {
        ActivationMode.DEFAULT -> Triple("未激活", "尚未配置核心终端行为", null)
        ActivationMode.SHIZUKU -> {
            val title = if (shizukuState.isRunning) "Shizuku 运行中" else "Shizuku 未运行"
            val desc = when {
                !shizukuState.isRunning -> "底层服务未连接"
                !shizukuState.permissionGranted -> "等待权限授权"
                else -> "一切准备就绪"
            }
            val detail = if (shizukuState.isRunning) {
                val uidStr = when (shizukuState.uid) {
                    0 -> "Root (0)"
                    2000 -> "ADB (2000)"
                    else -> shizukuState.uid.toString()
                }
                "v${shizukuState.version} • UID: $uidStr"
            } else null
            Triple(title, desc, detail)
        }
        ActivationMode.ROOT -> {
            val title = "Root 模式"
            val desc = when {
                !hasAllBasicPermissions -> "需要基础系统权限"
                !isRootAvailable -> "未获取 Root 权限"
                else -> "一切准备就绪"
            }
            Triple(title, desc, null)
        }
        ActivationMode.DIRECT_READ -> {
            val title = "漏洞直读模式"
            val desc = if (hasAllBasicPermissions) "通过零宽字符漏洞绕过限制" else "需要全文件访问权限"
            Triple(title, desc, null)
        }
        ActivationMode.CLOUD -> {
            val title = if (cloudLoggedIn) "云端模式已激活" else "云端模式未激活"
            val desc = if (cloudLoggedIn) "已登录: ${maskPhoneNumber(cloudPhone)}" else "需要登录 ETS100 账号"
            Triple(title, desc, null)
        }
    }
    
    FeFilledCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).background(statusContainerColor, CircleShape), Alignment.Center) {
                    Icon(
                        imageVector = if (isActive) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = animatedColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = currentMode.icon,
                            contentDescription = null,
                            tint = animatedColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(statusTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(statusDesc, style = MaterialTheme.typography.bodySmall, color = animatedColor)

                    if (statusDetail != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(statusDetail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                if (!isActive && currentMode == ActivationMode.SHIZUKU && !shizukuState.isRunning) {
                    FilledTonalButton(
                        onClick = { ShizukuManager.openShizukuDownloadPage(context) },
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
                    ) {
                        Text("去下载", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun FeDirectReadActivationPanel(hasAllBasicPermissions: Boolean) {
    val context = LocalContext.current
    val directReadYellow = MaterialTheme.colorScheme.tertiary
    val successColor = MaterialTheme.colorScheme.primary
    val errorRedColor = MaterialTheme.colorScheme.error
    
    var isZWCAvailable by remember { mutableStateOf(false) }
    var isDirectReadAvailable by remember { mutableStateOf(false) }
    var isTesting by remember { mutableStateOf(false) }
    
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isZWCAvailable = ZWCHelper.isVulnerabilityAvailable()
                isDirectReadAvailable = ZWCHelper.isDirectReadAvailable()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = when {
                    isDirectReadAvailable -> "漏洞直读就绪"
                    isTesting -> "正在测试..."
                    else -> "点击测试以检测漏洞直读可用性"
                },
                style = MaterialTheme.typography.labelSmall,
                color = when {
                    isDirectReadAvailable -> successColor
                    isTesting -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
        
        Spacer(Modifier.height(16.dp))

        if (hasAllBasicPermissions) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (isDirectReadAvailable) {
                    OutlinedButton(
                        onClick = { },
                        enabled = false,
                        border = BorderStroke(1.dp, successColor),
                        colors = ButtonDefaults.outlinedButtonColors(disabledContentColor = successColor),
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("漏洞直读已就绪", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = {
                            isTesting = true
                            isZWCAvailable = ZWCHelper.isVulnerabilityAvailable()
                            isDirectReadAvailable = ZWCHelper.isDirectReadAvailable()
                            isTesting = false
                            
                            if (isDirectReadAvailable) {
                                Toast.makeText(context, "漏洞直读测试通过！", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "暂不支持漏洞直读，请授权全文件访问或使用其他模式", Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isTesting) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                            contentColor = if (isTesting) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.weight(1f).height(40.dp),
                        enabled = !isTesting
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.BugReport, null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(if (isTesting) "测试中..." else "测试漏洞直读", fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            if (isTesting || isDirectReadAvailable) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isZWCAvailable) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLow
                            },
                            RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isZWCAvailable) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isZWCAvailable) MaterialTheme.colorScheme.onPrimaryContainer else errorRedColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isZWCAvailable) "零宽字符漏洞可用" else "零宽字符漏洞不可用",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isZWCAvailable) MaterialTheme.colorScheme.onPrimaryContainer else errorRedColor
                    )
                }
            }
        } else {
            Text(
                text = "请先完成上方基础权限的授权，然后返回此处测试漏洞",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "漏洞直读说明",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "利用零宽字符绕过 Android 限制，直接读取 Android/data 目录。点击上方按钮测试漏洞可用性。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun FeShizukuStatusCard(shizukuState: ShizukuState, context: android.content.Context) {
    val isRunning = shizukuState.isRunning
    val animatedColor by animateColorAsState(if (isRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, tween(800))

    FeFilledCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(48.dp)
                        .background(if (isRunning) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer, CircleShape),
                    Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = animatedColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Shizuku 核心状态", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(4.dp))
                    Text(shizukuState.getRuntimeTypeName(), style = MaterialTheme.typography.bodySmall, color = animatedColor)

                    if (isRunning) {
                        Spacer(Modifier.height(4.dp))
                        val uidStr = when (shizukuState.uid) {
                            0 -> "Root (0)"
                            2000 -> "ADB (2000)"
                            else -> shizukuState.uid.toString()
                        }
                        Text("版本: v${shizukuState.version} • UID: $uidStr", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                if (!isRunning) {
                    FilledTonalButton(
                        onClick = { ShizukuManager.openShizukuDownloadPage(context) },
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
                    ) {
                        Text("去下载", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun FeShizukuActivationPanel(
    shizukuState: ShizukuState,
    onRequestPermission: () -> Unit,
    onOpenShizuku: () -> Unit
) {
    val successColor = MaterialTheme.colorScheme.primary

    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = when {
                    !shizukuState.isRunning -> "底层服务未启动，无法工作"
                    shizukuState.permissionGranted -> "底层通信已就绪，享受极客体验"
                    else -> "需要授权以桥接系统底层 API"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!shizukuState.isRunning) {
                Button(
                    onClick = onOpenShizuku,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                    modifier = Modifier.weight(1f).height(40.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("打开 Shizuku 去启动", fontWeight = FontWeight.Bold)
                }
            } else {
                if (shizukuState.permissionGranted) {
                    OutlinedButton(
                        onClick = { },
                        enabled = false,
                        border = BorderStroke(1.dp, successColor),
                        colors = ButtonDefaults.outlinedButtonColors(disabledContentColor = successColor),
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("已授权，一切准备就绪", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onRequestPermission,
                        colors = ButtonDefaults.buttonColors(containerColor = successColor, contentColor = MaterialTheme.colorScheme.onPrimary),
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) {
                        Icon(Icons.Default.Security, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("点击请求应用授权", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun FeRootActivationPanel(context: android.content.Context) {
    val successColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error
    
    var isRootAvailable by remember { mutableStateOf(RootManager.isRootAvailable()) }
    var isChecking by remember { mutableStateOf(false) }
    
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isRootAvailable = RootManager.isRootAvailable()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = when {
                    isChecking -> "正在检测 Root 权限..."
                    isRootAvailable -> "Root 权限已获取，享受最高权限"
                    else -> "设备未获取 Root 权限"
                },
                style = MaterialTheme.typography.labelSmall,
                color = when {
                    isChecking -> MaterialTheme.colorScheme.onSurfaceVariant
                    isRootAvailable -> successColor
                    else -> errorColor
                }
            )
        }
        Spacer(Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (isRootAvailable) {
                OutlinedButton(
                    onClick = { },
                    enabled = false,
                    border = BorderStroke(1.dp, successColor),
                    colors = ButtonDefaults.outlinedButtonColors(disabledContentColor = successColor),
                    modifier = Modifier.weight(1f).height(40.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Root 已就绪", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = {
                        isChecking = true
                        RootManager.clearCache()
                        isRootAvailable = RootManager.isRootAvailable()
                        isChecking = false
                        
                        if (!isRootAvailable) {
                            Toast.makeText(context, "未检测到 Root 权限...", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "检测到 Root 权限！", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError),
                    modifier = Modifier.weight(1f).height(40.dp),
                    enabled = !isChecking
                ) {
                    if (isChecking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onError,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(if (isChecking) "检测中..." else "检测 Root 权限", fontWeight = FontWeight.Bold)
                }
            }
        }
        
        if (!isRootAvailable) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "提示：Root 权限需要通过 Magisk 或 SuperSU 等工具获取，并在授权管理中允许本应用使用 Root",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * 云端模式激活面板
 * 喵~ 用于显示登录状态和跳转到登录页面喵！
 */
@Composable
fun FeCloudActivationPanel(
    context: android.content.Context,
    isLoggedIn: Boolean,
    phone: String?,
    onCloudAuthChanged: () -> Unit,
    onNavigateToCloudActivation: () -> Unit,
    onNavigateToRead: () -> Unit
) {
    val cloudColor = MaterialTheme.colorScheme.tertiary
    val successColor = MaterialTheme.colorScheme.primary
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when {
                    isLoggedIn -> "云端模式已激活"
                    else -> "云端模式未激活"
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (isLoggedIn) successColor else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (isLoggedIn && !phone.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "已登录: ${maskPhoneNumber(phone)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(Modifier.height(16.dp))
        
        if (isLoggedIn) {
            // 已登录，显示进入云端首页按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        // 登出
                        ETS100AuthManager.logout(context)
                        onCloudAuthChanged()
                        Toast.makeText(context, "已退出登录", Toast.LENGTH_SHORT).show()
                    },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.weight(1f).height(40.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("退出登录", fontWeight = FontWeight.Bold)
                }
                
                Button(
                    onClick = {
                        // 宝贝直接跳转到答题页面，云端模式入口喵~
                        onNavigateToRead()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = cloudColor, contentColor = MaterialTheme.colorScheme.onTertiary),
                    modifier = Modifier.weight(1f).height(40.dp)
                ) {
                    Icon(Icons.Default.Cloud, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("进入云端", fontWeight = FontWeight.Bold)
                }
            }
        } else {
            // 未登录，显示登录按钮
            Button(
                onClick = {
                    // 跳转到登录页面
                    onNavigateToCloudActivation()
                },
                colors = ButtonDefaults.buttonColors(containerColor = cloudColor, contentColor = MaterialTheme.colorScheme.onTertiary),
                modifier = Modifier.fillMaxWidth().height(40.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Login, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("登录 ETS100 账号", fontWeight = FontWeight.Bold)
            }
        }
        
        if (!isLoggedIn) {
            Spacer(Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "云端模式说明",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "通过 ETS100 云端 API 在线获取作业列表和答案，无需本地文件",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
