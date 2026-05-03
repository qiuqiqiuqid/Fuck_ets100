package com.shuaiqiu.fuckets100

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.*

// ============================================================================
// 字体定义 - 用于应用标题的醒目展示效果
// ============================================================================
val RighteousFont = FontFamily(
    Font(resId = R.font.righteous, weight = FontWeight.Normal)
)

// ============================================================================
// 字体定义 - 用于应用标题的醒目展示效果
// ============================================================================

/**
 * 激活模式枚举
 * 定义应用的不同运行授权模式
 */
enum class ActivationMode(
    val title: String,
    val desc: String,
    val sysLabel: String,
    val badge: String,
    val icon: ImageVector,
    val hexColor: Color,
    val isSysOffline: Boolean
) {
    // 默认模式 - 未激活状态
    DEFAULT(
        "未激活", 
        "请选择授权模式以启用核心功能", 
        "SYS_OFFLINE", 
        "Inactive", 
        Icons.Default.Warning, 
        Color(0xFFFFB4AB), 
        true
    ),
    
    // Shizuku 模式 - 推荐使用
    SHIZUKU(
        "Shizuku 已连接", 
        "借助 Shizuku 在 Root 环境下以系统级 API 实现答题增强功能", 
        "SYS_READY", 
        "Recommended", 
        Icons.Default.CheckCircle, 
        Color(0xFF4ADE80), 
        false
    ),
    
    // Root 模式 - 最高权限
    ROOT(
        "Root 权限已获取", 
        "通过 Root 权限直接访问应用数据文件实现答题增强功能", 
        "SYS_READY", 
        "Highest Perm", 
        Icons.Default.Security, 
        Color(0xFFFFB4AB), 
        false
    ),
    
    // 直读模式 - 漏洞读取
    DIRECT_READ(
        "Direct Read 漏洞直读", 
        "利用零宽字符漏洞绕过 Android 限制直接读取应用内部存储实现答题增强", 
        "SYS_DIRECT_READ", 
        "Legacy", 
        Icons.Default.Bolt, 
        Color(0xFFFBBF24), 
        false
    ),
    
    // SAF 模式 - 存储访问框架
    SAF(
        "SAF 已激活", 
        "使用 Android 存储访问框架 (SAF) 授权访问应用数据目录", 
        "SYS_READY", 
        "SAF", 
        Icons.Default.FolderShared, 
        Color(0xFF60A5FA), 
        false
    )
}

/**
 * 屏幕路由密封类
 * 定义应用的所有导航屏幕
 */
sealed class Screen(val route: String, val icon: ImageVector, val label: String) {
    object Home : Screen("home", Icons.Default.Home, "首页")
    object Read : Screen("read", Icons.Default.MenuBook, "答题")
    object Settings : Screen("settings", Icons.Default.Settings, "设置")
    object Activation : Screen("activation", Icons.Default.Build, "激活")
    object GeneralSettings : Screen("general_settings", Icons.Default.Tune, "通用")
    object ThemeSettings : Screen("theme_settings", Icons.Default.Palette, "主题")
    object Debug : Screen("debug", Icons.Default.BugReport, "调试")
}

// ============================================================================
// 主要活动类 - 应用入口
// ============================================================================

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // 启用沉浸式边缘到边缘布局
        
        // 初始化 ThemeManager
        ThemeManager.init(this)
        
        setContent {
            FeAppMain()
        }
    }
}

/**
 * 应用主题包装器
 */
@Composable
fun FeTheme(content: @Composable () -> Unit) {
    val theme = ThemeManager.getSavedTheme()
    val colorScheme = darkColorScheme(
        primary = theme.primary,
        primaryContainer = theme.primaryContainer,
        onPrimary = theme.onPrimary,
        onPrimaryContainer = theme.primary,
        surface = theme.surface,
        onSurface = theme.onSurface,
        onSurfaceVariant = theme.onSurfaceVariant,
        outlineVariant = theme.outlineVariant,
        error = theme.error,
        errorContainer = theme.errorContainer,
        onErrorContainer = theme.error,
        secondary = theme.secondary,
        secondaryContainer = theme.secondaryContainer
    )
    MaterialTheme(colorScheme = colorScheme, content = content)
}

/**
 * 应用主界面组合函数
 */
@Composable
fun FeAppMain() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // 监听 Shizuku 状态变化
    val shizukuState = rememberShizukuState()
    
    // 主题状态 - 主题变更时自动刷新界面
    var currentTheme by remember { mutableStateOf(ThemeManager.getSavedTheme()) }
    
    // 更新弹窗状态 - 使用 snapshotFlow 监听 FeApplication.updateStatus 的变化喵~
    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateDialogStatus by remember { mutableStateOf<com.shuaiqiu.fuckets100.UpdateStatus?>(null) }
    
    // 监听更新状态 Flow，确保每次都能收到通知喵~
    LaunchedEffect(Unit) {
        FeApplication.updateStatusFlow.collect { status ->
            Log.d("FeAppMain", "updateStatusFlow 收到: $status")
            if (status != null && status.showDialog) {
                updateDialogStatus = status
                showUpdateDialog = true
            }
        }
    }
    
    // 当前激活模式 - 从保存的设置或自动检测获取
    var currentMode by remember { 
        mutableStateOf(
            SettingsManager.getSavedActivationMode() ?: ShizukuManager.getCurrentActivationMode()
        )
    }
    
    // SAF 文件夹选择触发器
    var safFolderSelectionTrigger by remember { mutableStateOf(0) }
    
    // SAF 文件夹选择 Activity Result Launcher
    val safFolderPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            // 保存选择的目录
            SAFManager.saveDirectory(uri, null)
            
            // 检查目录是否正确
            if (SAFManager.isCorrectDirectory()) {
                Toast.makeText(context, "目录验证成功~", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "注意：选择的目录不正确，请重新选择 Android/data 目录", Toast.LENGTH_LONG).show()
            }
        } else {
            // 用户取消了选择
            Toast.makeText(context, "未选择任何目录", Toast.LENGTH_SHORT).show()
        }
    }
    
    // 监听 SAF 文件夹选择触发器变化
    LaunchedEffect(safFolderSelectionTrigger) {
        if (safFolderSelectionTrigger > 0) {
            // 获取初始 URI 并启动选择器
            val initialUri = SAFManager.getInitialUri()
            safFolderPickerLauncher.launch(initialUri)
            // 清除初始 URI
            SAFManager.clearInitialUri()
        }
    }
    
    // 设置 SAF 文件夹选择回调
    DisposableEffect(Unit) {
        SAFManager.setOnFolderSelectedCallback {
            // 触发文件夹选择
            safFolderSelectionTrigger++
        }
        onDispose {
            SAFManager.setOnFolderSelectedCallback(null)
        }
    }

    // 自动检测 Shizuku 状态并更新激活模式
    LaunchedEffect(shizukuState.isRunning, shizukuState.isSui) {
        if (!SettingsManager.hasUserSelectedMode()) {
            currentMode = ShizukuManager.getCurrentActivationMode()
        }
    }

    FeThemeWrapper(theme = currentTheme) {
        Scaffold(
            bottomBar = {
                if (currentRoute in listOf("home", "read", "settings")) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        tonalElevation = 8.dp
                    ) {
                        listOf(Screen.Home, Screen.Read, Screen.Settings).forEach { screen ->
                            NavigationBarItem(
                                icon = { Icon(screen.icon, contentDescription = screen.label) },
                                selected = currentRoute == screen.route,
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                    indicatorColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
            ) { innerPadding ->
            NavHost(
                navController = navController, 
                startDestination = Screen.Home.route, 
                modifier = Modifier.padding(innerPadding)
            ) {

                composable(Screen.Home.route) { 
                    HomeScreen(currentMode, shizukuState, navController) 
                }
                
                composable(Screen.Read.route) { 
                    ReadScreen(
                        currentMode = currentMode,
                        onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                    ) 
                }
                
                composable(Screen.Settings.route) { 
                    SettingsScreen(navController) 
                }
                
                composable(Screen.GeneralSettings.route) { 
                    GeneralSettingsScreen(navController) 
                }
                
                composable(Screen.Activation.route) {
                    ActivationSettingsScreen(
                        currentMode = currentMode,
                        shizukuState = shizukuState,
                        onModeSelected = { mode -> 
                            currentMode = mode
                            SettingsManager.saveActivationMode(mode)
                        },
                        navController = navController
                    )
                }
                
                composable(Screen.ThemeSettings.route) {
                    ThemeSettingsScreen(
                        navController = navController,
                        onThemeChanged = { newTheme -> currentTheme = newTheme }
                    )
                }
                
                composable(Screen.Debug.route) {
                    DebugScreen(navController = navController)
                }
            }
        }
        
        // 更新弹窗 - 放在 Scaffold 外面确保能覆盖其他内容喵~
        if (showUpdateDialog && updateDialogStatus != null) {
            Log.d("FeAppMain", "显示更新弹窗: ${updateDialogStatus!!.message}")
            UpdateDialog(
                status = updateDialogStatus!!,
                onDismiss = {
                    showUpdateDialog = false
                    updateDialogStatus = null
                    FeApplication.updateStatus = null
                }
            )
        }
    }
}

/**
 * 主题包装器组件
 */
@Composable
fun FeThemeWrapper(theme: AppTheme, content: @Composable () -> Unit) {
    val colorScheme = if (theme.isDark) {
        darkColorScheme(
            primary = theme.primary,
            primaryContainer = theme.primaryContainer,
            onPrimary = theme.onPrimary,
            onPrimaryContainer = theme.primary,
            surface = theme.surface,
            surfaceContainerLow = theme.surfaceContainerLow,
            surfaceContainer = theme.surfaceContainer,
            surfaceContainerHigh = theme.surfaceContainerHigh,
            surfaceContainerHighest = theme.surfaceContainerHighest,
            onSurface = theme.onSurface,
            onSurfaceVariant = theme.onSurfaceVariant,
            outlineVariant = theme.outlineVariant,
            error = theme.error,
            errorContainer = theme.errorContainer,
            onErrorContainer = theme.error,
            secondary = theme.secondary,
            secondaryContainer = theme.secondaryContainer
        )
    } else {
        lightColorScheme(
            primary = theme.primary,
            primaryContainer = theme.primaryContainer,
            onPrimary = theme.onPrimary,
            onPrimaryContainer = theme.primary,
            surface = theme.surface,
            surfaceContainerLow = theme.surfaceContainerLow,
            surfaceContainer = theme.surfaceContainer,
            surfaceContainerHigh = theme.surfaceContainerHigh,
            surfaceContainerHighest = theme.surfaceContainerHighest,
            onSurface = theme.onSurface,
            onSurfaceVariant = theme.onSurfaceVariant,
            outlineVariant = theme.outlineVariant,
            error = theme.error,
            errorContainer = theme.errorContainer,
            onErrorContainer = theme.error,
            secondary = theme.secondary,
            secondaryContainer = theme.secondaryContainer
        )
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}

// ============================================================================
// UI 组件定义 - 顶部应用栏和设置项
// ============================================================================

/**
 * 应用顶部导航栏
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeTopAppBar(title: String) {
    CenterAlignedTopAppBar(
        title = {
            // 应用标题带发光效果
            val glowColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            val feGlowShadow = Shadow(
                color = glowColor,
                offset = Offset(0f, 0f),
                blurRadius = 24f
            )

            Text(
                text = title,
                fontSize = 44.sp,
                letterSpacing = 0.15.em,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                fontFamily = RighteousFont,
                style = TextStyle(shadow = feGlowShadow),
                modifier = Modifier.padding(bottom = 4.dp)
            )
        },
        navigationIcon = {
            Icon(
                Icons.Default.Terminal,
                contentDescription = null,
                Modifier.padding(start = 16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
    )
}

/**
 * 设置列表项组件
 */
@Composable
fun SettingsListItem(
    icon: ImageVector,
    title: String,
    sub: String,
    hideChevron: Boolean = false,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            if (sub.isNotEmpty()) {
                Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (!hideChevron) {
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}
