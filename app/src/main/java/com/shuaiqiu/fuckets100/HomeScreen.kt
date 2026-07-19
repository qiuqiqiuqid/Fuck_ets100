package com.shuaiqiu.fuckets100

import android.os.SystemClock
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class HomeRuntimeStatus(
    val hasFilesPerm: Boolean,
    val hasOverlayPerm: Boolean,
    val hasAppListPerm: Boolean,
    val hasRootAvailable: Boolean,
    val hasDirectReadAvailable: Boolean,
    val cloudLoggedIn: Boolean
)

private object HomeRuntimeStatusStore {
    private const val CACHE_TTL_MS = 30_000L

    var cached = HomeRuntimeStatus(
        hasFilesPerm = false,
        hasOverlayPerm = false,
        hasAppListPerm = false,
        hasRootAvailable = false,
        hasDirectReadAvailable = false,
        cloudLoggedIn = false
    )
    var hasLoaded = false
    var lastRefreshTime = 0L
    var lastMode: ActivationMode? = null

    fun isFresh(): Boolean {
        return hasLoaded && SystemClock.elapsedRealtime() - lastRefreshTime < CACHE_TTL_MS
    }

    fun update(status: HomeRuntimeStatus, mode: ActivationMode? = lastMode) {
        cached = status
        hasLoaded = true
        lastRefreshTime = SystemClock.elapsedRealtime()
        lastMode = mode
    }
}

/**
 * 首页主屏幕
 * 显示系统状态、设备信息和激活状态
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    mode: ActivationMode,
    shizukuState: ShizukuState,
    onNavigateToActivation: () -> Unit
) {
    val context = LocalContext.current
    val appContext = remember(context) { context.applicationContext }
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val currentModeForRefresh by rememberUpdatedState(mode)
    
    var runtimeStatus by remember {
        mutableStateOf(HomeRuntimeStatusStore.cached)
    }
    var remoteStatus by remember {
        mutableStateOf(FeApplication.remoteStatus)
    }
    
    // 生命周期监听 - 从系统设置返回时自动刷新权限状态
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch {
                    val status = loadHomeRuntimeStatus(appContext, force = true)
                    HomeRuntimeStatusStore.update(status, currentModeForRefresh)
                    runtimeStatus = status
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    LaunchedEffect(mode) {
        val status = loadHomeRuntimeStatus(
            context = appContext,
            force = HomeRuntimeStatusStore.lastMode != mode
        )
        HomeRuntimeStatusStore.update(status, mode)
        runtimeStatus = status
    }

    LaunchedEffect(Unit) {
        FeApplication.remoteStatusFlow.collect { status ->
            remoteStatus = status
        }
    }
    
    // 检查基础权限是否全部获取
    val hasAllBasicPermissions = runtimeStatus.hasFilesPerm &&
        runtimeStatus.hasOverlayPerm &&
        runtimeStatus.hasAppListPerm
    
    // 判断是否真正激活 - 根据不同模式判断激活条件
    // Direct Read 模式会检测零宽字符漏洞绕过限制，而其他模式需要相应的权限和配置
    val isTrulyActivated = when {
        mode == ActivationMode.SHIZUKU -> shizukuState.isRunning && shizukuState.permissionGranted && hasAllBasicPermissions
        mode == ActivationMode.ROOT -> hasAllBasicPermissions && runtimeStatus.hasRootAvailable
        mode == ActivationMode.DIRECT_READ -> hasAllBasicPermissions && runtimeStatus.hasDirectReadAvailable
        mode == ActivationMode.CLOUD -> runtimeStatus.cloudLoggedIn
        mode != ActivationMode.DEFAULT -> hasAllBasicPermissions
        else -> false
    }

    val sysLabel = if (isTrulyActivated) "SYS_READY" else "SYS_OFFLINE"
    val activeColor = if (isTrulyActivated) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

    Scaffold(
        topBar = { FeTopAppBar(title = "Fe") },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        Column(
            Modifier
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
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
                cloudLoggedIn = runtimeStatus.cloudLoggedIn,
                hasFilesPerm = runtimeStatus.hasFilesPerm,
                hasOverlayPerm = runtimeStatus.hasOverlayPerm,
                hasAppListPerm = runtimeStatus.hasAppListPerm,
                hasAllBasicPermissions = hasAllBasicPermissions,
                hasRootAvailable = runtimeStatus.hasRootAvailable,
                onNavigateToActivation = onNavigateToActivation
            )
            HomeRemoteContent(status = remoteStatus)
        }
    }
}

private suspend fun loadHomeRuntimeStatus(
    context: android.content.Context,
    force: Boolean
): HomeRuntimeStatus {
    if (!force && HomeRuntimeStatusStore.isFresh()) {
        return HomeRuntimeStatusStore.cached
    }

    return withContext(Dispatchers.IO) {
        HomeRuntimeStatus(
            hasFilesPerm = PermissionsHelper.hasAllFilesAccess(),
            hasOverlayPerm = PermissionsHelper.hasOverlayPermission(context),
            hasAppListPerm = PermissionsHelper.hasAppListPermission(),
            hasRootAvailable = RootManager.isRootAvailable(),
            hasDirectReadAvailable = ZWCHelper.isDirectReadAvailable(),
            cloudLoggedIn = ETS100AuthManager.isLoggedIn(context)
        )
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
    cloudLoggedIn: Boolean,
    hasFilesPerm: Boolean,
    hasOverlayPerm: Boolean,
    hasAppListPerm: Boolean,
    hasAllBasicPermissions: Boolean,
    hasRootAvailable: Boolean,
    onNavigateToActivation: () -> Unit
) {
    val statusContentColor = if (isTrulyActivated) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onErrorContainer
    }
    val statusIconContainerColor = if (isTrulyActivated) {
        Color.White
    } else {
        MaterialTheme.colorScheme.error
    }
    val statusIconColor = if (isTrulyActivated) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onError
    }

    // 根据当前状态显示不同的标题
    val displayTitle = when {
        mode == ActivationMode.CLOUD && cloudLoggedIn -> "云端模式已激活"
        mode == ActivationMode.CLOUD -> "云端模式未激活"
        // 基础权限未授予时显示等待授权提示
        !hasAllBasicPermissions -> "等待授权"
        mode == ActivationMode.SHIZUKU && shizukuState.isRunning && !shizukuState.permissionGranted -> "Shizuku 等待授权"
        mode == ActivationMode.SHIZUKU && !shizukuState.isRunning -> "Shizuku 未运行"
        mode == ActivationMode.ROOT && !hasRootAvailable -> "Root 未获取"
        else -> mode.title
    }

    // 选择图标 - 激活时使用模式图标，未激活时使用警告图标
    val displayIcon = if (isTrulyActivated) mode.icon else Icons.Default.Warning

    FeStatusCard(
        modifier = Modifier
            .fillMaxWidth(),
        onClick = onNavigateToActivation,
        active = isTrulyActivated
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // 顶部行 - 图标和设置按钮
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Box(
                        Modifier
                            .size(48.dp)
                            .background(statusIconContainerColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(displayIcon, null, tint = statusIconColor, modifier = Modifier.size(28.dp))
                    }
                    Icon(
                        Icons.Default.Settings, 
                        null, 
                        tint = statusContentColor.copy(alpha = 0.88f)
                    )
                }

                // 底部区域 - 状态信息
                Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        displayTitle, 
                        style = MaterialTheme.typography.headlineMedium, 
                        fontWeight = FontWeight.Bold, 
                        color = statusContentColor
                    )

                    // 详细说明文字
                    if (mode == ActivationMode.CLOUD) {
                        // 云端模式首页只保留主状态，避免暴露账号信息。
                    } else if (!hasAllBasicPermissions) {
                        val subText = when {
                            !hasFilesPerm -> "需要文件访问权限才能读取题库，请授权"
                            !hasOverlayPerm -> "需要悬浮窗权限才能显示答题界面，请授权"
                            !hasAppListPerm -> "需要应用列表权限才能检测ETS应用，请授权"
                            else -> "请授权上述权限后即可使用"
                        }
                        Text(
                            text = subText, 
                            style = MaterialTheme.typography.labelSmall, 
                            color = statusContentColor.copy(alpha = 0.88f)
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
                            color = statusContentColor.copy(alpha = 0.88f)
                        )
                    } else if (mode == ActivationMode.ROOT) {
                        val subText = when {
                            !hasRootAvailable -> "未检测到Root权限，请先获取Root"
                            else -> "Root 权限已获取"
                        }
                        Text(
                            text = subText, 
                            style = MaterialTheme.typography.labelSmall, 
                            color = statusContentColor.copy(alpha = 0.88f)
                        )
                    } else if (mode == ActivationMode.DIRECT_READ) {
                        Text(
                            text = "Direct Read 模式已就绪", 
                            style = MaterialTheme.typography.labelSmall, 
                            color = statusContentColor.copy(alpha = 0.88f)
                        )
                    } else if (mode == ActivationMode.DEFAULT) {
                        Text(
                            text = "请授权基础权限后选择激活模式", 
                            style = MaterialTheme.typography.labelSmall, 
                            color = statusContentColor.copy(alpha = 0.88f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeRemoteContent(status: UpdateStatus?) {
    val context = LocalContext.current
    val announcementMessage = status?.announcementMessage.orEmpty()
    val announcementTitle = status?.announcementTitle?.takeIf { it.isNotBlank() } ?: "公告"
    val changelogSummary = status?.changelogSummary?.takeIf { it.isNotBlank() }
        ?: status?.message.orEmpty()
    val changelogTitle = status?.changelogTitle?.takeIf { it.isNotBlank() } ?: "更新日志"
    val donateEnabled = status?.donateEnabled ?: true

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        RemoteOverviewCard(
            announcementTitle = announcementTitle,
            announcementMessage = announcementMessage.ifBlank { "暂无公告" },
            changelogTitle = changelogTitle,
            changelogSummary = changelogSummary.ifBlank { "暂无更新内容" },
            meta = if (status == null) "同步中" else null,
            onClick = {
                context.startActivity(
                    RemoteContentActivity.createIntent(
                        context = context,
                        announcementTitle = announcementTitle,
                        announcementMessage = announcementMessage.ifBlank { "暂无公告" },
                        announcementUpdatedAt = status?.announcementUpdatedAt.orEmpty(),
                        announcementUrl = status?.announcementUrl.orEmpty(),
                        changelogTitle = changelogTitle,
                        changelogSummary = changelogSummary.ifBlank { "暂无更新内容" },
                        changelogUrl = status?.changelogUrl.orEmpty()
                    )
                )
            }
        )

        if (donateEnabled) {
            CompactDonateCard(
                icon = Icons.Default.Favorite,
                title = "捐赠支持",
                onClick = { context.startActivity(DonateActivity.createIntent(context)) }
            )
        }
    }
}

@Composable
private fun RemoteOverviewCard(
    announcementTitle: String,
    announcementMessage: String,
    changelogTitle: String,
    changelogSummary: String,
    meta: String? = null,
    onClick: () -> Unit
) {
    FeFilledCard(
        modifier = Modifier
            .fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FeTonalIconContainer(
                    icon = Icons.Default.Campaign,
                    size = 42.dp,
                    iconSize = 22.dp
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "公告与更新",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (meta != null) {
                        Text(
                            meta,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            RemotePreviewLine(
                title = announcementTitle,
                body = announcementMessage
            )
            FeThinDivider()
            RemotePreviewLine(
                title = changelogTitle,
                body = changelogSummary
            )
        }
    }
}

@Composable
private fun RemotePreviewLine(
    title: String,
    body: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CompactDonateCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit
) {
    FeFilledCard(
        modifier = Modifier
            .fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FeTonalIconContainer(
                icon = icon,
                size = 34.dp,
                iconSize = 20.dp
            )
            Spacer(Modifier.width(12.dp))
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
