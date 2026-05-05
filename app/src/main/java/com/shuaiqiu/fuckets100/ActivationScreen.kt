package com.shuaiqiu.fuckets100

import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
fun ActivationSettingsScreen(
    currentMode: ActivationMode,
    shizukuState: ShizukuState,
    onModeSelected: (ActivationMode) -> Unit,
    navController: NavHostController
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // 基础权限动态状态
    var hasFilesPerm by remember { mutableStateOf(PermissionsHelper.hasAllFilesAccess()) }
    var hasOverlayPerm by remember { mutableStateOf(PermissionsHelper.hasOverlayPermission(context)) }
    var hasAppListPerm by remember { mutableStateOf(PermissionsHelper.hasAppListPermission()) }

    // Shizuku 权限请求状态
    var pendingPermissionRequest by remember { mutableStateOf(false) }

    // 监听生命周期，从系统设置返回时自动刷新基础权限的绿勾勾
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasFilesPerm = PermissionsHelper.hasAllFilesAccess()
                hasOverlayPerm = PermissionsHelper.hasOverlayPermission(context)
                hasAppListPerm = PermissionsHelper.hasAppListPermission()
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

    val successColor = Color(0xFF4ADE80)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("运行授权与权限", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } }
            )
        }
    ) { p ->
        LazyColumn(Modifier.padding(p).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

            // 当前模式状态卡片
            item {
                FeModeStatusCard(
                    currentMode = currentMode,
                    shizukuState = shizukuState,
                    hasAllBasicPermissions = hasFilesPerm && hasOverlayPerm && hasAppListPerm,
                    context = context
                )
                Spacer(Modifier.height(8.dp))
            }

            // 基础权限申请区域
            item {
                Text("系统基础权限", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
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
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

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
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

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

                OutlinedCard(
                    onClick = { onModeSelected(mode) },
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(if(isSelected) 1.dp else (-1).dp, if(isSelected) mode.hexColor.copy(alpha=0.5f) else Color.Transparent),
                    colors = CardDefaults.outlinedCardColors(containerColor = if(isSelected) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.Top) {
                            RadioButton(selected = isSelected, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = mode.hexColor))
                            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text(mode.name, fontWeight = FontWeight.Bold, color = if(isSelected) mode.hexColor else MaterialTheme.colorScheme.onSurface)
                                    Surface(color = if(mode == ActivationMode.ROOT) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(50)) {
                                        val displayBadge = if (mode == ActivationMode.SHIZUKU && shizukuState.isRunning && shizukuState.permissionGranted) {
                                            "UID: ${shizukuState.uid}"
                                        } else {
                                            mode.badge
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
                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = mode.hexColor.copy(alpha = 0.2f))

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
                                    ActivationMode.SAF -> {
                                        FeSafActivationPanel(context, onOpenFolderPicker = {
                                            SAFManager.requestFolderSelection()
                                        })
                                    }
                                    ActivationMode.DIRECT_READ -> {
                                        FeDirectReadActivationPanel(hasFilesPerm && hasOverlayPerm && hasAppListPerm)
                                    }
                                    ActivationMode.CLOUD -> {
                                        FeCloudActivationPanel(context, navController)
                                    }
                                    else -> {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text("需要手动确认以完成激活", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Button(
                                                onClick = { Toast.makeText(context, "正在请求 ${mode.name} 激活...", Toast.LENGTH_SHORT).show() },
                                                colors = ButtonDefaults.buttonColors(containerColor = mode.hexColor, contentColor = Color.Black),
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

@Composable
fun FeModeStatusCard(
    currentMode: ActivationMode,
    shizukuState: ShizukuState,
    hasAllBasicPermissions: Boolean,
    context: android.content.Context
) {
    var isRootAvailable by remember { mutableStateOf(RootManager.isRootAvailable()) }
    var isSafConfigured by remember { mutableStateOf(SAFManager.isConfigured()) }
    var isSafCorrectDirectory by remember { mutableStateOf(SAFManager.isCorrectDirectory()) }
    var isDirectReadAvailable by remember { mutableStateOf(ZWCHelper.isDirectReadAvailable()) }
    
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isRootAvailable = RootManager.isRootAvailable()
                isSafConfigured = SAFManager.isConfigured()
                isSafCorrectDirectory = SAFManager.isCorrectDirectory()
                isDirectReadAvailable = ZWCHelper.isDirectReadAvailable()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    
    val errorRedColor = Color(0xFFDC2626)
    
    val statusColor = when (currentMode) {
        ActivationMode.DEFAULT -> MaterialTheme.colorScheme.outline
        ActivationMode.SHIZUKU -> if (shizukuState.isRunning && shizukuState.permissionGranted) Color(0xFF4ADE80) else errorRedColor
        ActivationMode.ROOT -> if (hasAllBasicPermissions && isRootAvailable) Color(0xFF4ADE80) else errorRedColor
        ActivationMode.DIRECT_READ -> if (hasAllBasicPermissions && isDirectReadAvailable) Color(0xFF4ADE80) else errorRedColor
        ActivationMode.SAF -> if (hasAllBasicPermissions && isSafConfigured && isSafCorrectDirectory) Color(0xFF4ADE80) else errorRedColor
        ActivationMode.CLOUD -> {
            val isCloudLoggedIn = ETS100AuthManager.isLoggedIn(context)
            if (isCloudLoggedIn) Color(0xFF4ADE80) else MaterialTheme.colorScheme.outline
        }
    }
    
    val isActive = statusColor == Color(0xFF4ADE80)
    val animatedColor by animateColorAsState(statusColor, tween(800))
    
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
        ActivationMode.SAF -> {
            val title = "SAF 模式"
            val desc = when {
                !hasAllBasicPermissions -> "需要基础系统权限"
                !isSafConfigured -> "尚未选择授权目录"
                !isSafCorrectDirectory -> "选择的目录不正确"
                else -> "一切准备就绪"
            }
            Triple(title, desc, null)
        }
        ActivationMode.CLOUD -> {
            val isLoggedIn = ETS100AuthManager.isLoggedIn(context)
            val phone = ETS100AuthManager.getPhone(context)
            val title = if (isLoggedIn) "云端模式" else "云端模式"
            val desc = if (isLoggedIn) "已登录: $phone" else "需要登录 ETS100 账号"
            Triple(title, desc, null)
        }
    }
    
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(animatedColor.copy(alpha = 0.2f), Color.Transparent),
                        center = Offset(size.width, size.height / 2f),
                        radius = size.width * 0.5f
                    )
                )
            }

            Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).background(MaterialTheme.colorScheme.surfaceContainer, CircleShape), Alignment.Center) {
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
    val directReadYellow = Color(0xFFFBBF24)
    val successColor = Color(0xFF4ADE80)
    val errorRedColor = Color(0xFFDC2626)
    
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
                        border = BorderStroke(1.dp, successColor.copy(alpha = 0.5f)),
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
                            if (isZWCAvailable) successColor.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceContainerHighest,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isZWCAvailable) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isZWCAvailable) successColor else errorRedColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isZWCAvailable) "零宽字符漏洞可用" else "零宽字符漏洞不可用",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isZWCAvailable) successColor else errorRedColor
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
                .background(
                    directReadYellow.copy(alpha = 0.1f),
                    RoundedCornerShape(8.dp)
                )
                .padding(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = directReadYellow,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "漏洞直读说明",
                    style = MaterialTheme.typography.labelSmall,
                    color = directReadYellow
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "利用零宽字符绕过 Android 限制，直接读取 Android/data 目录。点击上方按钮测试漏洞可用性。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun FeShizukuStatusCard(shizukuState: ShizukuState, context: android.content.Context) {
    val isRunning = shizukuState.isRunning
    val animatedColor by animateColorAsState(if (isRunning) Color(0xFF4ADE80) else MaterialTheme.colorScheme.error, tween(800))

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(animatedColor.copy(alpha = 0.15f), Color.Transparent),
                        center = Offset(size.width, size.height / 2f),
                        radius = size.width * 0.5f
                    )
                )
            }

            Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).background(MaterialTheme.colorScheme.surfaceContainer, CircleShape), Alignment.Center) {
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
    val successColor = Color(0xFF4ADE80)

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
                    Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("打开 Shizuku 去启动", fontWeight = FontWeight.Bold)
                }
            } else {
                if (shizukuState.permissionGranted) {
                    OutlinedButton(
                        onClick = { },
                        enabled = false,
                        border = BorderStroke(1.dp, successColor.copy(alpha = 0.5f)),
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
                        colors = ButtonDefaults.buttonColors(containerColor = successColor, contentColor = Color.Black),
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
    val successColor = Color(0xFF4ADE80)
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
                    border = BorderStroke(1.dp, successColor.copy(alpha = 0.5f)),
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

@Composable
fun FeSafActivationPanel(
    context: android.content.Context,
    onOpenFolderPicker: () -> Unit
) {
    val safBlue = Color(0xFF60A5FA)
    val successColor = Color(0xFF4ADE80)
    
    var isSafConfigured by remember { mutableStateOf(SAFManager.isConfigured()) }
    var directoryName by remember { mutableStateOf(SAFManager.getSavedDirectoryName()) }
    
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isSafConfigured = SAFManager.isConfigured()
                directoryName = SAFManager.getSavedDirectoryName()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = when {
                    isSafConfigured -> "存储访问框架已就绪"
                    else -> "存储访问框架 - 选择目录授权"
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (isSafConfigured) successColor else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        if (isSafConfigured) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "已授权: ${directoryName ?: "未知"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
        
        Spacer(Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (isSafConfigured) {
                OutlinedButton(
                    onClick = { },
                    enabled = false,
                    border = BorderStroke(1.dp, successColor.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(disabledContentColor = successColor),
                    modifier = Modifier.weight(1f).height(40.dp)
                ) {
                    Icon(Icons.Default.FolderOpen, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("目录已就绪", fontWeight = FontWeight.Bold)
                }
                
                OutlinedButton(
                    onClick = onOpenFolderPicker,
                    border = BorderStroke(1.dp, safBlue.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = safBlue),
                    modifier = Modifier.weight(1f).height(40.dp)
                ) {
                    Icon(Icons.Default.FolderOff, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("更换目录", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = onOpenFolderPicker,
                    colors = ButtonDefaults.buttonColors(containerColor = safBlue, contentColor = Color.White),
                    modifier = Modifier.weight(1f).height(40.dp)
                ) {
                    Icon(Icons.Default.FolderShared, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("选择文件夹", fontWeight = FontWeight.Bold)
                }
            }
        }
        
        if (!isSafConfigured) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "提示：SAF 模式允许您授权访问任意目录，无需 Root 权限。授权后应用可持续访问该目录",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            
            Spacer(Modifier.height(4.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        safBlue.copy(alpha = 0.1f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = safBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "目标路径：/storage/emulated/0/Android/data",
                        style = MaterialTheme.typography.labelSmall,
                        color = safBlue
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = safBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "注意：可能不支持 Android 14 及以上版本",
                        style = MaterialTheme.typography.labelSmall,
                        color = safBlue
                    )
                }
            }
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
    navController: NavHostController
) {
    val cloudColor = Color(0xFF60A5FA)
    val successColor = Color(0xFF4ADE80)
    
    val isLoggedIn = remember { ETS100AuthManager.isLoggedIn(context) }
    val phone = remember { ETS100AuthManager.getPhone(context) }
    
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // 刷新登录状态
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when {
                    isLoggedIn -> "已登录: $phone"
                    else -> "需要登录 ETS100 账号"
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (isLoggedIn) successColor else MaterialTheme.colorScheme.onSurfaceVariant
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
                        Toast.makeText(context, "已退出登录", Toast.LENGTH_SHORT).show()
                    },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.weight(1f).height(40.dp)
                ) {
                    Icon(Icons.Default.Logout, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("退出登录", fontWeight = FontWeight.Bold)
                }
                
                Button(
                    onClick = {
                        // 进入云端首页
                        navController.navigate(Screen.CloudHome.route)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = cloudColor, contentColor = Color.White),
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
                    navController.navigate(Screen.CloudActivation.route)
                },
                colors = ButtonDefaults.buttonColors(containerColor = cloudColor, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth().height(40.dp)
            ) {
                Icon(Icons.Default.Login, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("登录 ETS100 账号", fontWeight = FontWeight.Bold)
            }
        }
        
        if (!isLoggedIn) {
            Spacer(Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        cloudColor.copy(alpha = 0.1f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = null,
                        tint = cloudColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "云端模式说明",
                        style = MaterialTheme.typography.labelSmall,
                        color = cloudColor
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "通过 ETS100 云端 API 在线获取作业列表和答案，无需本地文件",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}
