package com.shuaiqiu.fuckets100

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.compose.*
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController

// ============================================================================
// 瀛椾綋瀹氫箟 - 鐢ㄤ簬搴旂敤鏍囬鐨勯啋鐩睍绀烘晥鏋?// ============================================================================
val RighteousFont = FontFamily(
    Font(resId = R.font.righteous, weight = FontWeight.Normal)
)

// ============================================================================
// 瀛椾綋瀹氫箟 - 鐢ㄤ簬搴旂敤鏍囬鐨勯啋鐩睍绀烘晥鏋?// ============================================================================

/**
 * 婵€娲绘ā寮忔灇涓? * 瀹氫箟搴旂敤鐨勪笉鍚岃繍琛屾巿鏉冩ā寮? */
enum class ActivationMode(
    val title: String,
    val desc: String,
    val sysLabel: String,
    val badge: String,
    val icon: ImageVector,
    val hexColor: Color,
    val isSysOffline: Boolean
) {
    DEFAULT(
        "未激活",
        "请选择授权模式以启用核心功能",
        "SYS_OFFLINE",
        "Inactive",
        Icons.Default.Warning,
        Color(0xFFFFB4AB),
        true
    ),
    SHIZUKU(
        "Shizuku 已连接",
        "借助 Shizuku 在 Root 环境下以系统级 API 实现答题增强功能",
        "SYS_READY",
        "Recommended",
        Icons.Default.CheckCircle,
        Color(0xFF4ADE80),
        false
    ),
    ROOT(
        "Root 权限已获取",
        "通过 Root 权限直接访问应用数据文件实现答题增强功能",
        "SYS_READY",
        "Highest Perm",
        Icons.Default.Security,
        Color(0xFFFFB4AB),
        false
    ),
    DIRECT_READ(
        "Direct Read 漏洞直读",
        "利用零宽字符漏洞绕过 Android 限制直接读取应用内部存储实现答题增强",
        "SYS_DIRECT_READ",
        "Legacy",
        Icons.Default.Bolt,
        Color(0xFFFBBF24),
        false
    ),
    CLOUD(
        "云端模式",
        "通过 ETS100 云端 API 在线获取作业列表和答案",
        "SYS_READY",
        "Cloud",
        Icons.Default.Cloud,
        Color(0xFF60A5FA),
        false
    )
}

sealed class Screen(val route: String, val icon: ImageVector, val label: String) {
    object Home : Screen("home", Icons.Default.Home, "首页")
    object Read : Screen("read", Icons.AutoMirrored.Filled.MenuBook, "答题")
    object Settings : Screen("settings", Icons.Default.Settings, "设置")
    object Activation : Screen("activation", Icons.Default.Build, "激活")
    object GeneralSettings : Screen("general_settings", Icons.Default.Tune, "通用")
    object ThemeSettings : Screen("theme_settings", Icons.Default.Palette, "主题")
    object Debug : Screen("debug", Icons.Default.BugReport, "调试")
    object CloudActivation : Screen("cloud_activation", Icons.Default.Cloud, "云端激活")
    object Legal : Screen("legal", Icons.Default.Gavel, "法律")
}
// ============================================================================
// 涓昏娲诲姩绫?- 搴旂敤鍏ュ彛
// ============================================================================

class MainActivity : ComponentActivity() {
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra(EXTRA_TARGET_ROUTE)?.let { route ->
            pendingTargetRoute = route
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // 鍚敤娌夋蹈寮忚竟缂樺埌杈圭紭甯冨眬
        intent.getStringExtra(EXTRA_TARGET_ROUTE)?.let { route ->
            pendingTargetRoute = route
        }
        
        // 鍒濆鍖?ThemeManager
        ThemeManager.init(this)
        
        setContent {
            FeAppMain()
        }
    }

    companion object {
        private const val EXTRA_TARGET_ROUTE = "target_route"

        var pendingTargetRoute: String? = null

        fun createIntent(context: android.content.Context, targetRoute: String? = null): Intent {
            return Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                if (targetRoute != null) {
                    putExtra(EXTRA_TARGET_ROUTE, targetRoute)
                    pendingTargetRoute = targetRoute
                }
            }
        }
    }
}

/**
 * 搴旂敤涓婚鍖呰鍣? */
@Composable
fun FeTheme(content: @Composable () -> Unit) {
    val theme = ThemeManager.getSavedTheme()
    val systemDarkMode = isSystemInDarkTheme()
    val effectiveDarkMode = if (ThemeManager.getSavedAutoDarkMode()) {
        systemDarkMode
    } else {
        ThemeManager.getSavedDarkMode()
    }
    val useDynamicColor = ThemeManager.getSavedDynamicColor()

    FeThemeWrapper(
        theme = theme,
        isDarkMode = effectiveDarkMode,
        useDynamicColor = useDynamicColor,
        content = content
    )
}

private val rootTabRoutes = setOf(Screen.Home.route, Screen.Read.route, Screen.Settings.route)

fun NavHostController.navigateRootTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.isRootTabTransition(): Boolean {
    return initialState.destination.route in rootTabRoutes && targetState.destination.route in rootTabRoutes
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.slideEnterTransition(): EnterTransition {
    if (isRootTabTransition()) {
        return EnterTransition.None
    }

    return slideInHorizontally(
        initialOffsetX = { it },
        animationSpec = tween(300, easing = FastOutSlowInEasing)
    ) + fadeIn(
        initialAlpha = 0.85f,
        animationSpec = tween(120, easing = FastOutSlowInEasing)
    )
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.slideExitTransition(): ExitTransition {
    if (isRootTabTransition()) {
        return ExitTransition.None
    }

    return ExitTransition.None
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.slidePopEnterTransition(): EnterTransition {
    if (isRootTabTransition()) {
        return EnterTransition.None
    }

    return EnterTransition.None
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.slidePopExitTransition(): ExitTransition {
    if (isRootTabTransition()) {
        return ExitTransition.None
    }

    return slideOutHorizontally(
        targetOffsetX = { it },
        animationSpec = tween(300, easing = FastOutSlowInEasing)
    ) + fadeOut(
        targetAlpha = 0.85f,
        animationSpec = tween(120, easing = FastOutSlowInEasing)
    )
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.fadeOnlyEnterTransition(): EnterTransition {
    if (isRootTabTransition()) {
        return EnterTransition.None
    }

    return fadeIn()
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.fadeOnlyExitTransition(): ExitTransition {
    if (isRootTabTransition()) {
        return ExitTransition.None
    }

    return fadeOut()
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.fadeAndScalePopExitTransition(): ExitTransition {
    if (isRootTabTransition()) {
        return ExitTransition.None
    }

    return scaleOut(targetScale = 0.9f) + fadeOut()
}

/** 
 * 搴旂敤涓荤晫闈㈢粍鍚堝嚱鏁? */
@Composable
fun FeAppMain() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val rootContentAlpha = remember { Animatable(1f) }
    var previousRouteForRootAnimation by remember { mutableStateOf<String?>(null) }
    val predictiveBackState = rememberAospPredictiveBackState()
    var predictiveBackMode by remember { mutableStateOf(SettingsManager.getPredictiveBackMode()) }
    var compactAnswerDisplay by remember { mutableStateOf(SettingsManager.getCompactAnswerDisplay()) }

    val shizukuState = rememberShizukuState()
    
    var currentTheme by remember { mutableStateOf(ThemeManager.getSavedTheme()) }
    var isDarkMode by remember { mutableStateOf(ThemeManager.getSavedDarkMode()) }
    var isAutoDarkMode by remember { mutableStateOf(ThemeManager.getSavedAutoDarkMode()) }
    var useDynamicColor by remember { mutableStateOf(ThemeManager.getSavedDynamicColor()) }
    val systemDarkMode = isSystemInDarkTheme()
    val effectiveDarkMode = if (isAutoDarkMode) systemDarkMode else isDarkMode
    
    // 鏇存柊寮圭獥鐘舵€?- 浣跨敤 snapshotFlow 鐩戝惉 FeApplication.updateStatus 鐨勫彉鍖栧柕~
    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateDialogStatus by remember { mutableStateOf<com.shuaiqiu.fuckets100.UpdateStatus?>(null) }
    var verificationDialogStatus by remember { mutableStateOf<com.shuaiqiu.fuckets100.UpdateStatus?>(null) }
    var showLegalDialog by remember { mutableStateOf(!SettingsManager.hasAcceptedLegal()) }
    
    // 鐩戝惉鏇存柊鐘舵€?Flow锛岀‘淇濇瘡娆￠兘鑳芥敹鍒伴€氱煡鍠祣
    LaunchedEffect(Unit) {
        FeApplication.updateStatusFlow.collect { status ->
            Log.d("FeAppMain", "updateStatusFlow 鏀跺埌: $status")
            if (status != null && status.showDialog) {
                updateDialogStatus = status
                showUpdateDialog = true
            }
        }
    }

    LaunchedEffect(Unit) {
        FeApplication.remoteStatusFlow.collect { status ->
            if (status != null && status.requiresVerification) {
                verificationDialogStatus = status
            }
        }
    }
    
    var currentMode by remember {
        mutableStateOf(
            SettingsManager.getSavedActivationMode() ?: ShizukuManager.getCurrentActivationMode()
        )
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                currentMode = SettingsManager.getSavedActivationMode() ?: ShizukuManager.getCurrentActivationMode()
                currentTheme = ThemeManager.getSavedTheme()
                isDarkMode = ThemeManager.getSavedDarkMode()
                isAutoDarkMode = ThemeManager.getSavedAutoDarkMode()
                useDynamicColor = ThemeManager.getSavedDynamicColor()
                predictiveBackMode = SettingsManager.getPredictiveBackMode()
                compactAnswerDisplay = SettingsManager.getCompactAnswerDisplay()
                MainActivity.pendingTargetRoute?.let { route ->
                    MainActivity.pendingTargetRoute = null
                    navController.navigateRootTab(route)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        MainActivity.pendingTargetRoute?.let { route ->
            MainActivity.pendingTargetRoute = null
            navController.navigateRootTab(route)
        }
    }
    
    LaunchedEffect(shizukuState.isRunning, shizukuState.isSui) {
        if (!SettingsManager.hasUserSelectedMode()) {
            currentMode = ShizukuManager.getCurrentActivationMode()
        }
    }

    LaunchedEffect(currentRoute) {
        val previousRoute = previousRouteForRootAnimation
        val isRootSwitch = previousRoute != null &&
            previousRoute != currentRoute &&
            previousRoute in rootTabRoutes &&
            currentRoute in rootTabRoutes

        if (isRootSwitch) {
            rootContentAlpha.snapTo(0.05f)
            rootContentAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(650, easing = FastOutSlowInEasing)
            )
        } else if (rootContentAlpha.value != 1f) {
            rootContentAlpha.snapTo(1f)
        }

        previousRouteForRootAnimation = currentRoute
    }

    FeThemeWrapper(
        theme = currentTheme,
        isDarkMode = effectiveDarkMode,
        useDynamicColor = useDynamicColor
    ) {
        Scaffold(
            bottomBar = {
                if (currentRoute in listOf(
                Screen.Home.route,
                Screen.Read.route,
                Screen.Settings.route
            )) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        tonalElevation = 0.dp
                    ) {
                        listOf(Screen.Home, Screen.Read, Screen.Settings).forEach { screen ->
                            NavigationBarItem(
                                icon = { Icon(screen.icon, contentDescription = screen.label) },
                                selected = currentRoute == screen.route,
                                onClick = {
                                    navController.navigateRootTab(screen.route)
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
            ) { innerPadding ->
            BoxWithConstraints(
                modifier = Modifier
                    .padding(innerPadding)
                    .graphicsLayer {
                        alpha = rootContentAlpha.value
                    }
            ) {
                val density = LocalDensity.current
                val containerHeightPx = with(density) { maxHeight.toPx() }
                val predictiveBackOffsetPx = with(density) { 96.dp.toPx() }
                val deviceCornerRadius = rememberDeviceCornerRadius()

                NavHost(
                    navController = navController,
                    startDestination = Screen.Home.route,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (predictiveBackMode == PredictiveBackMode.AOSP) {
                                Modifier.aospPredictiveBackAnimation(
                                    state = predictiveBackState,
                                    containerHeightPx = containerHeightPx,
                                    exitingOffsetPx = predictiveBackOffsetPx,
                                    deviceCornerRadius = deviceCornerRadius
                                )
                            } else {
                                Modifier
                            }
                        ),
                    enterTransition = {
                        when (predictiveBackMode) {
                            PredictiveBackMode.NONE -> EnterTransition.None
                            PredictiveBackMode.KERNELSU_CLASSIC -> fadeOnlyEnterTransition()
                            else -> slideEnterTransition()
                        }
                    },
                    exitTransition = {
                        when (predictiveBackMode) {
                            PredictiveBackMode.NONE -> ExitTransition.None
                            PredictiveBackMode.KERNELSU_CLASSIC -> fadeOnlyExitTransition()
                            else -> slideExitTransition()
                        }
                    },
                    popEnterTransition = {
                        when (predictiveBackMode) {
                            PredictiveBackMode.NONE -> EnterTransition.None
                            PredictiveBackMode.KERNELSU_CLASSIC -> fadeOnlyEnterTransition()
                            else -> slidePopEnterTransition()
                        }
                    },
                    popExitTransition = {
                        when (predictiveBackMode) {
                            PredictiveBackMode.NONE -> ExitTransition.None
                            PredictiveBackMode.KERNELSU_CLASSIC -> fadeAndScalePopExitTransition()
                            else -> slidePopExitTransition()
                        }
                    }
                ) {

                    composable(Screen.Home.route) { 
                        HomeScreen(
                            mode = currentMode,
                            shizukuState = shizukuState,
                            onNavigateToActivation = {
                                context.startActivity(ActivationActivity.createIntent(context))
                            }
                        )
                    }
                    
                    composable(Screen.Read.route) {
                        ReadScreen(
                            currentMode = currentMode,
                            onNavigateToActivation = {
                                context.startActivity(ActivationActivity.createIntent(context))
                            },
                            compactAnswerDisplay = compactAnswerDisplay
                        )
                    }
                    
                    composable(Screen.Settings.route) { 
                        SettingsScreen(navController) 
                    }
                    
                    composable(Screen.Debug.route) {
                        DebugScreen(navController = navController)
                    }
                }
            }
        }

        AospPredictiveBackHandler(
            state = predictiveBackState,
            enabled = predictiveBackMode == PredictiveBackMode.AOSP &&
                navController.previousBackStackEntry != null,
            onBack = { navController.popBackStack() }
        )
        
        // 鏇存柊寮圭獥 - 鏀惧湪 Scaffold 澶栭潰纭繚鑳借鐩栧叾浠栧唴瀹瑰柕~
        if (showUpdateDialog && updateDialogStatus != null) {
            Log.d("FeAppMain", "鏄剧ず鏇存柊寮圭獥: ${updateDialogStatus!!.message}")
            UpdateDialog(
                status = updateDialogStatus!!,
                onDismiss = {
                    showUpdateDialog = false
                    updateDialogStatus = null
                    FeApplication.updateStatus = null
                }
            )
        }

        if (verificationDialogStatus != null) {
            val status = verificationDialogStatus!!
            VerificationDialog(
                status = status,
                onVerified = {
                    verificationDialogStatus = null
                    FeApplication.remoteStatus = status.copy(requiresVerification = false)
                    FeApplication.refreshRemoteStatusAfterVerification()
                }
            )
        } else if (showLegalDialog) {
            LegalAcceptanceDialog(
                onAccepted = {
                    SettingsManager.saveLegalAccepted(true)
                    showLegalDialog = false
                }
            )
        }
    }
}

/**
 * 涓婚鍖呰鍣ㄧ粍浠? */
@Composable
fun FeThemeWrapper(
    theme: AppTheme,
    isDarkMode: Boolean = ThemeManager.getSavedDarkMode(),
    useDynamicColor: Boolean = ThemeManager.getSavedDynamicColor(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val dynamicSeed = if (useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicLightColorScheme(context).primary
    } else {
        null
    }
    val colorScheme = buildFeColorScheme(
        theme = theme,
        isDarkMode = isDarkMode,
        dynamicSeed = dynamicSeed
    )
    MaterialTheme(colorScheme = colorScheme, content = content)
}

private fun buildFeColorScheme(
    theme: AppTheme,
    isDarkMode: Boolean,
    dynamicSeed: Color?
): ColorScheme {
    val seed = dynamicSeed ?: theme.primary
    val seedContainer = monetBlend(seed, Color.White, 0.86f)
    val seedContainerDark = monetBlend(seed, Color(0xFF232934), 0.62f)
    val accentAmount = if (dynamicSeed != null) 0.10f else 0f

    return if (isDarkMode) {
        val primary = monetBlend(seed, Color(0xFF232934), 0.08f)
        val secondary = monetBlend(seed, Color(0xFFC8D5E8), 0.58f)
        val tertiary = monetBlend(seed, Color(0xFFE0C7DF), 0.62f)

        darkColorScheme(
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = seedContainerDark,
            onPrimaryContainer = Color(0xFFEAF1FF),
            secondary = secondary,
            onSecondary = Color(0xFF1B2D44),
            secondaryContainer = Color(0xFF334357),
            onSecondaryContainer = Color(0xFFDCE8F8),
            tertiary = tertiary,
            onTertiary = Color(0xFF34253C),
            tertiaryContainer = Color(0xFF493D55),
            onTertiaryContainer = Color(0xFFF1DFF8),
            background = Color(0xFF232934),
            onBackground = Color(0xFFF2F5FA),
            surface = Color(0xFF272E3A),
            onSurface = Color(0xFFF2F5FA),
            surfaceVariant = Color(0xFF3A4350),
            onSurfaceVariant = Color(0xFFCBD2DD),
            surfaceContainerLow = Color(0xFF2B333F),
            surfaceContainer = Color(0xFF313A47),
            surfaceContainerHigh = Color(0xFF394351),
            surfaceContainerHighest = Color(0xFF424D5C),
            outline = Color(0xFF8D96A5),
            outlineVariant = Color(0xFF566273),
            error = Color(0xFFFFB4AB),
            onError = Color(0xFF690005),
            errorContainer = Color(0xFF5C1A1A),
            onErrorContainer = Color(0xFFFFDAD6)
        )
    } else {
        val primary = monetBlend(seed, Color(0xFF1D1F24), accentAmount)
        val secondary = monetBlend(seed, Color(0xFF56606F), 0.58f)
        val tertiary = monetBlend(seed, Color(0xFF765D75), 0.62f)

        lightColorScheme(
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = seedContainer,
            onPrimaryContainer = monetBlend(seed, Color.Black, 0.38f),
            secondary = secondary,
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFE8EEF8),
            onSecondaryContainer = Color(0xFF1A2D45),
            tertiary = tertiary,
            onTertiary = Color.White,
            tertiaryContainer = Color(0xFFF0E7F5),
            onTertiaryContainer = Color(0xFF2D2434),
            background = Color(0xFFF8F8FF),
            onBackground = Color(0xFF1D1F24),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF1D1F24),
            surfaceVariant = Color(0xFFE8EDF7),
            onSurfaceVariant = Color(0xFF535B67),
            surfaceContainerLow = Color(0xFFFFFFFF),
            surfaceContainer = Color(0xFFEFF2FB),
            surfaceContainerHigh = Color(0xFFE8EDF7),
            surfaceContainerHighest = Color(0xFFE1E7F2),
            outline = Color(0xFF7C8492),
            outlineVariant = Color(0xFFD6DAE4),
            error = Color(0xFFB3261E),
            onError = Color.White,
            errorContainer = Color(0xFFFFEDEA),
            onErrorContainer = Color(0xFF601410)
        )
    }
}

private fun monetBlend(from: Color, to: Color, amount: Float): Color {
    val t = amount.coerceIn(0f, 1f)
    return Color(
        red = from.red + (to.red - from.red) * t,
        green = from.green + (to.green - from.green) * t,
        blue = from.blue + (to.blue - from.blue) * t,
        alpha = from.alpha + (to.alpha - from.alpha) * t
    )
}

// ============================================================================
// UI 缁勪欢瀹氫箟 - 椤堕儴搴旂敤鏍忓拰璁剧疆椤?// ============================================================================

/**
 * 搴旂敤椤堕儴瀵艰埅鏍? */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeTopAppBar(title: String) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = title,
                fontSize = 36.sp,
                letterSpacing = 0.em,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                fontFamily = RighteousFont,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
    )
}

/**
 * 璁剧疆鍒楄〃椤圭粍浠? */
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
