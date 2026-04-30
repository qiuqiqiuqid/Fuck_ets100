package com.shauiqiu.fe

import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
// 🌟 自定义字体配置
// ============================================================================
val RighteousFont = FontFamily(
    Font(resId = R.font.righteous, weight = FontWeight.Normal)
)

// ============================================================================
// 🌟 第一部分：核心状态模型与页面路由 (从 AppModels.kt 合并过来)
// ============================================================================

enum class ActivationMode(
    val title: String,
    val desc: String,
    val sysLabel: String,
    val badge: String,
    val icon: ImageVector,
    val hexColor: Color,
    val isSysOffline: Boolean
) {
    DEFAULT("未激活", "尚未配置核心终端行为。", "SYS_OFFLINE", "Inactive", Icons.Default.Warning, Color(0xFFFFB4AB), true),
    SHIZUKU("Shizuku 已连接", "无需 Root 即可获得高权限运行。适合大多数现代 Android 设备。", "SYS_READY", "Recommended", Icons.Default.CheckCircle, Color(0xFF4ADE80), false),
    ROOT("Root 已激活", "需要设备已解锁并获取 Root 权限。提供绝对的底层访问控制。", "SYS_READY", "Highest Perm", Icons.Default.Security, Color(0xFFFFB4AB), false),
    DIRECT_READ("Direct Read 直读模式", "仅适用于低版本 Android 系统。通过直接读取系统文件获取数据。", "SYS_DIRECT_READ", "Legacy", Icons.Default.Bolt, Color(0xFFFBBF24), false),
    SAF("SAF Active", "使用 Android 存储访问框架 (SAF) 授权目录访问。", "SYS_READY", "SAF", Icons.Default.FolderShared, Color(0xFF60A5FA), false)
}

sealed class Screen(val route: String, val icon: ImageVector, val label: String) {
    object Home : Screen("home", Icons.Default.Home, "首页")
    object Read : Screen("read", Icons.Default.MenuBook, "读取")
    object Settings : Screen("settings", Icons.Default.Settings, "设置")
    object Activation : Screen("activation", Icons.Default.Build, "激活")
    object GeneralSettings : Screen("general_settings", Icons.Default.Tune, "通用")
    object ThemeSettings : Screen("theme_settings", Icons.Default.Palette, "主题")
}

// ============================================================================
// 🌟 第二部分：大门入口、主题与导航大厅
// ============================================================================

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // 开启沉浸式体验
        
        // 初始化 ThemeManager
        ThemeManager.init(this)
        
        setContent {
            FeAppMain()
        }
    }
}

@Composable
fun FeTheme(content: @Composable () -> Unit) {
    val theme = ThemeManager.getSavedTheme()
    val colorScheme = darkColorScheme(
        primary = theme.primary,
        primaryContainer = theme.primaryContainer,
        onPrimary = theme.onPrimary,
        onPrimaryContainer = theme.primary,
        surface = theme.surface,
        background = theme.background,
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

@Composable
fun FeAppMain() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // 召唤 Shizuku 状态管家！
    val shizukuState = rememberShizukuState()
    
    // 响应式主题状态 - 主题更改时会触发重组
    var currentTheme by remember { mutableStateOf(ThemeManager.getSavedTheme()) }
    
    // 初始化激活模式：优先使用保存的用户选择，否则自动检测
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
            // 用户选择了目录
            SAFManager.saveDirectory(uri, null)
            
            // 检查选择的目录是否正确
            if (SAFManager.isCorrectDirectory()) {
                Toast.makeText(context, "目录授权成功喵~", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "⚠️ 目录不正确！请重新选择 Android/data 目录", Toast.LENGTH_LONG).show()
            }
        } else {
            // 用户取消了选择
            Toast.makeText(context, "未选择任何目录", Toast.LENGTH_SHORT).show()
        }
    }
    
    // 监听 SAF 文件夹选择触发器
    LaunchedEffect(safFolderSelectionTrigger) {
        if (safFolderSelectionTrigger > 0) {
            // 获取初始 URI（如果有设置的话）
            val initialUri = SAFManager.getInitialUri()
            safFolderPickerLauncher.launch(initialUri)
            // 清除初始 URI
            SAFManager.clearInitialUri()
        }
    }
    
    // 创建 SAF 文件夹选择请求回调
    DisposableEffect(Unit) {
        SAFManager.setOnFolderSelectedCallback {
            // 触发文件夹选择
            safFolderSelectionTrigger++
        }
        onDispose {
            SAFManager.setOnFolderSelectedCallback(null)
        }
    }

    // 联动：当 Shizuku 状态变化时，如果用户没有手动选择过模式，则自动更新
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
            NavHost(navController = navController, startDestination = Screen.Home.route, modifier = Modifier.padding(innerPadding)) {

                composable(Screen.Home.route) { HomeScreen(currentMode, shizukuState, navController) }
                composable(Screen.Read.route) { ReadScreen() }
                composable(Screen.Settings.route) { SettingsScreen(navController) }
                composable(Screen.GeneralSettings.route) { GeneralSettingsScreen(navController) }
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
            }
        }
    }
}

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
            background = theme.background,
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
            background = theme.background,
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
// 🌟 第三部分：全局复用组件 (标题栏、列表行)
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeTopAppBar(title: String) {
    CenterAlignedTopAppBar(
        title = {
            // 动态光晕颜色跟随主题
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

@Composable
fun SettingsListItem(
    icon: ImageVector,
    title: String,
    sub: String,
    hideChevron: Boolean = false,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit = {}
) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceContainer, CircleShape), Alignment.Center) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f).padding(start = 16.dp)) {
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