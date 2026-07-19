package com.shuaiqiu.fuckets100

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.Manifest
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.charset.StandardCharsets

private const val TAG = "ReadScreen"
private const val LOCAL_PAPER_LOADING_CATEGORY = "local_loading"

// ============================================================================
// 调试日志系统 - 用于跟踪应用内数据和答案的读取过程
// ============================================================================

/**
 * 日志级别枚举
 * 定义日志的重要程度和显示颜色
 */
private enum class LogLevel(val label: String, val colorHex: Long) {
    DEBUG("调试", 0xFF60A5FA),      // 蓝色 - 调试信息
    INFO("信息", 0xFFFFD93D),       // 黄色 - 一般信息
    SUCCESS("成功", 0xFF2DD4BF),    // 青色 - 成功状态
    WARN("警告", 0xFFFB923C),       // 橙色 - 警告信息
    ERROR("错误", 0xFFFF6B6B),      // 红色 - 错误信息
    INIT("初始化", 0xFFE879F9)      // 紫色 - 初始化相关
}

/**
 * 日志类别枚举
 * 用于分组显示不同类型的数据
 */
private enum class LogCategory(val label: String) {
    SYSTEM("系统"),
    FILE("文件"),
    PAPER("试卷"),
    SECTION("分区"),
    QUESTION("题目"),
    ANSWER("答案")
}

/**
 * 日志条目数据结构
 * 用于结构化存储日志信息，支持彩色显示
 */
private data class LogEntry(
    val timestamp: String,
    val level: LogLevel,
    val category: LogCategory,
    val message: String
) {
    /**
     * 将日志条目格式化为显示文本
     * 格式: [时间] [级别] [类别] 消息
     */
    fun toDisplayString(): String = "[$timestamp] [${level.label}] [${category.label}] $message"
}

/**
 * 初始化结果密封类
 * 用于安全地在 IO 线程和主线程之间传递数据
 */
private sealed class InitResult {
    data class Success(
        val readerInfo: String,
        val dataFiles: List<ETS100FileReader.FileItem>,
        val resourceFiles: List<ETS100FileReader.FileItem>,
        val papers: List<ETS100AnswerReader.Paper>
    ) : InitResult()
    
    data class Error(val message: String) : InitResult()
}

private enum class LocalParsePhase {
    IDLE,
    SCANNING,
    PARSING,
    COMPLETED,
    FAILED
}

private object LocalReadProgressState {
    var phase by mutableStateOf(LocalParsePhase.IDLE)
    var doneCount by mutableIntStateOf(0)
    var totalCount by mutableIntStateOf(0)
    var sectionCount by mutableIntStateOf(0)
    var questionCount by mutableIntStateOf(0)
    var parsedGroupIndexes by mutableStateOf(emptySet<Int>())
    var message by mutableStateOf("")
    var parseJob: Job? = null

    fun reset() {
        parseJob?.cancel()
        parseJob = null
        phase = LocalParsePhase.IDLE
        doneCount = 0
        totalCount = 0
        sectionCount = 0
        questionCount = 0
        parsedGroupIndexes = emptySet()
        message = ""
    }
}

private fun List<ETS100AnswerReader.Paper>.totalSectionCount(): Int = sumOf { it.sections.size }

private fun List<ETS100AnswerReader.Paper>.totalQuestionCount(): Int =
    sumOf { paper -> paper.sections.sumOf { section -> section.questions.size } }

private fun ETS100AnswerReader.Paper.isLocalAnswerLoading(): Boolean {
    return sections.any { it.category == LOCAL_PAPER_LOADING_CATEGORY }
}

// ========== 云端模式辅助函数喵~ ==========

/**
 * 创建云端作业的占位 Paper 对象
 * 用于在列表中显示"未加载"状态喵~
 */
private fun createCloudHomeworkPlaceholder(homework: ETS100ApiClient.HomeworkInfo): ETS100AnswerReader.Paper {
    val identity = cloudHomeworkIdentity(homework)
    val placeholderSections = homework.contents.mapIndexed { index, content ->
        val title = content.groupName.ifBlank { "云端内容 ${index + 1}" }
        ETS100AnswerReader.Section(
            caption = title,
            category = cloudHomeworkCategoryFromGroupName(title),
            typeName = title,
            questions = listOf(
                ETS100AnswerReader.Question(
                    order = index + 1,
                    sectionOrder = 1,
                    sectionCaption = title,
                    typeName = title,
                    questionText = "未下载",
                    answers = emptyList(),
                    originalText = null,
                    category = cloudHomeworkCategoryFromGroupName(title),
                    displayOrder = index + 1
                )
            ),
            originalContent = null
        )
    }.ifEmpty {
        listOf(
            ETS100AnswerReader.Section(
                caption = "云端作业",
                category = "cloud_homework",
                typeName = "未加载",
                questions = emptyList(),
                originalContent = null
            )
        )
    }

    return ETS100AnswerReader.Paper(
        paperId = identity.hashCode().toLong(),
        title = homework.name,
        dataFileName = "",
        fileSize = 0L,
        sections = placeholderSections,
        downloadTime = 0L,
        regionLabel = "云端",
        paperName = homework.name
    )
}

private fun cloudHomeworkCategoryFromGroupName(groupName: String): String {
    return when {
        groupName.contains("朗读") -> "read_chapter"
        groupName.contains("转述") -> "topic"
        groupName.contains("询问") || groupName.contains("提问") -> "simple_expression_ufj"
        groupName.contains("回答") || groupName.contains("问答") -> "simple_expression_ufk"
        groupName.contains("选择") || groupName.contains("听选") || groupName.contains("记录") -> "simple_expression_ufi"
        else -> "cloud_homework"
    }
}

private fun cloudHomeworkStatusLabel(status: String): String {
    return if (status == CloudHomeworkState.STATUS_HISTORY) "历史作业" else "当前作业"
}

private fun cloudHomeworkRequestStatuses(status: String): List<String> {
    return if (status == CloudHomeworkState.STATUS_HISTORY) {
        listOf(CloudHomeworkState.STATUS_EXPIRED, CloudHomeworkState.STATUS_HISTORY)
    } else {
        listOf(status)
    }
}

private fun cloudHomeworkIdentity(homework: ETS100ApiClient.HomeworkInfo): String {
    if (homework.id.isNotBlank()) return homework.id
    val contentSignature = homework.contents.joinToString("|") { "${it.groupName}:${it.url}" }
    return "${homework.name}:$contentSignature"
}

private fun cloudHomeworkCacheKey(status: String, homework: ETS100ApiClient.HomeworkInfo): String =
    "$status:${cloudHomeworkIdentity(homework)}"

private fun sortCloudQuestionsByOfficialOrder(
    questions: List<ETS100AnswerReader.Question>
): List<ETS100AnswerReader.Question> {
    val displayOrders = questions.mapNotNull { it.displayOrder }
    val hasReliableDisplayOrder = displayOrders.size == questions.size &&
        displayOrders.distinct().size == questions.size &&
        displayOrders.sorted() == (1..questions.size).toList()

    if (!hasReliableDisplayOrder) {
        return questions.mapIndexed { index, question ->
            question.copy(
                sectionOrder = index + 1,
                displayOrder = index + 1
            )
        }
    }

    return questions
        .withIndex()
        .sortedWith(
            compareBy<IndexedValue<ETS100AnswerReader.Question>> {
                it.value.displayOrder ?: Int.MAX_VALUE
            }.thenBy { it.index }
        )
        .mapIndexed { index, indexedQuestion ->
            indexedQuestion.value.copy(sectionOrder = index + 1)
        }
}

private fun normalizeCloudParsedSections(
    sections: List<ETS100AnswerReader.Section>
): List<ETS100AnswerReader.Section> {
    val chooseSections = sections.filter { it.category == ETS100AnswerReader.StructureType.COLLECTOR_CHOOSE }
    if (chooseSections.size <= 1) {
        return sections.map { section ->
            if (section.category == ETS100AnswerReader.StructureType.COLLECTOR_CHOOSE) {
                section.copy(questions = sortCloudQuestionsByOfficialOrder(section.questions))
            } else {
                section
            }
        }
    }

    val firstChooseIndex = sections.indexOfFirst { it.category == ETS100AnswerReader.StructureType.COLLECTOR_CHOOSE }
    val mergedChooseSection = chooseSections.first().copy(
        questions = sortCloudQuestionsByOfficialOrder(chooseSections.flatMap { it.questions }),
        originalContent = chooseSections.firstNotNullOfOrNull { it.originalContent }
    )

    val normalizedSections = mutableListOf<ETS100AnswerReader.Section>()
    for ((index, section) in sections.withIndex()) {
        if (index == firstChooseIndex) {
            normalizedSections.add(mergedChooseSection)
        }
        if (section.category != ETS100AnswerReader.StructureType.COLLECTOR_CHOOSE) {
            normalizedSections.add(section)
        }
    }
    return normalizedSections
}

@Composable
private fun answerCategoryStyle(
    category: String,
    fallbackAccent: Color = MaterialTheme.colorScheme.primary
): AnswerCategoryColor {
    return answerCategoryPalette()[category] ?: fallbackAnswerCategoryStyle(fallbackAccent)
}

@Composable
private fun fallbackAnswerCategoryStyle(accent: Color): AnswerCategoryColor {
    return AnswerCategoryColor(
        accent = accent,
        container = MaterialTheme.colorScheme.secondaryContainer,
        onContainer = MaterialTheme.colorScheme.onSecondaryContainer
    )
}

private fun displayAnswerList(
    question: ETS100AnswerReader.Question,
    compactAnswerDisplay: Boolean
): List<String> {
    if (!compactAnswerDisplay) return question.answerList

    val isZhuanShu = question.typeName.contains("转述") || question.category == "topic"
    return question.answerList.take(if (isZhuanShu) 1 else 2)
}

private fun formatAnswerForDisplay(
    question: ETS100AnswerReader.Question,
    compactAnswerDisplay: Boolean
): String {
    if (!compactAnswerDisplay) return question.formattedAnswer

    val isZhuanShu = question.typeName.contains("转述") || question.category == "topic"
    val cleaned = displayAnswerList(question, compactAnswerDisplay).map { answer ->
        ETS100AnswerReader.cleanAnswerText(answer).lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    return if (isZhuanShu) {
        cleaned.flatten().joinToString(" ")
    } else {
        cleaned.joinToString("\n") { it.joinToString(" ") }
    }
}

/**
 * 阅读界面 - 显示ETS 100答案的阅读界面
 * 支持多种激活模式（Shizuku、Root、SAF等）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadScreen(
    currentMode: ActivationMode,
    onNavigateToActivation: () -> Unit,
    compactAnswerDisplay: Boolean = SettingsManager.getCompactAnswerDisplay()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cachedLocalSnapshot = remember(currentMode) {
        ReadPageStateStore.loadLocal(context)?.takeIf { it.mode == currentMode }
    }
    val cachedCloudSnapshot = remember {
        ReadPageStateStore.loadCloud(context)?.also { snapshot ->
            if (CloudHomeworkState.homeworkListsByStatus.isEmpty() &&
                CloudHomeworkState.downloadedPapers.isEmpty()
            ) {
                val validDownloadedPapers = snapshot.downloadedPapers.filterValues { papers ->
                    papers.isNotEmpty() && papers.all { it.source is PaperSource.Cloud }
                }
                CloudHomeworkState.selectedStatus = snapshot.selectedStatus
                CloudHomeworkState.homeworkListsByStatus = snapshot.homeworkListsByStatus
                CloudHomeworkState.cloudBaseUrl = snapshot.cloudBaseUrl
                CloudHomeworkState.downloadedPapers = validDownloadedPapers
                CloudHomeworkState.downloadedHomeworkNames =
                    snapshot.downloadedHomeworkNames.intersect(validDownloadedPapers.keys)
                CloudHomeworkState.failedCloudHomeworks = snapshot.failedCloudHomeworks
            }
        }
    }

    fun openPaperDetail(paper: ETS100AnswerReader.Paper) {
        val paperKey = PaperStore.put(paper)
        context.startActivity(AnswerActivity.createIntent(context, paperKey))
    }

    fun copyPaperText(paper: ETS100AnswerReader.Paper) {
        val text = formatPaperAsText(paper)
        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText(paper.title, text))
        android.widget.Toast.makeText(context, "已复制答案到剪贴板", android.widget.Toast.LENGTH_SHORT).show()
    }
    
    // 调试相关状态 - 使用结构化日志系统
    var showDebugPanel by remember { mutableStateOf(false) }
    var debugLog by remember { mutableStateOf(listOf<LogEntry>()) }
    var showDataDetails by remember { mutableStateOf(false) }  // 是否显示详细数据信息
    
    // 文件列表状态
    var dataFiles by remember {
        mutableStateOf(if (currentMode != ActivationMode.CLOUD) cachedLocalSnapshot?.dataFiles.orEmpty() else emptyList())
    }
    var resourceFiles by remember {
        mutableStateOf(if (currentMode != ActivationMode.CLOUD) cachedLocalSnapshot?.resourceFiles.orEmpty() else emptyList())
    }
    var readerInfo by remember {
        mutableStateOf(if (currentMode != ActivationMode.CLOUD) cachedLocalSnapshot?.readerInfo.orEmpty() else "")
    }
    
    // 试卷和题目状态
    var papers by remember {
        mutableStateOf(if (currentMode != ActivationMode.CLOUD) cachedLocalSnapshot?.papers.orEmpty() else emptyList())
    }
    var selectedPaper by remember { mutableStateOf<ETS100AnswerReader.Paper?>(null) }
    var selectedSectionIndex by remember { mutableIntStateOf(-1) }
    var selectedQuestionIndex by remember { mutableIntStateOf(-1) }
    
    // 二级页面导航状态 - 宝贝用于切换试卷详情页面喵~
    var showPaperDetail by remember { mutableStateOf(false) }
    
    // 加载状态
    var isLoading by remember {
        mutableStateOf(currentMode != ActivationMode.CLOUD && cachedLocalSnapshot == null)
    }
    var isParsingLocalAnswers by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var sourceExportPaper by remember { mutableStateOf<ETS100AnswerReader.Paper?>(null) }
    var showLocalDirectoryExport by remember { mutableStateOf(false) }
    var reloadTrigger by remember { mutableIntStateOf(0) }  // 宝贝添加了重新加载触发器喵~
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }  // 宝贝添加了删除确认对话框状态喵~
    
    // 展开状态 - 宝贝现在使用 PaperListItem，不需要这些展开状态了喵~
    // var expandedPapers by remember { mutableStateOf(setOf<Int>()) }
    // var expandedSections by remember { mutableStateOf(setOf<Pair<Int, Int>>()) }
    // 搜索相关状态 - 已移除喵~
    // var questionSearchQuery by remember { mutableStateOf("") }
    // var showOnlyAnswered by remember { mutableStateOf(false) }
    
    // 可展开 FAB 相关状态
    var isFabExpanded by remember { mutableStateOf(false) }
    var showCloudReadConfirmDialog by remember { mutableStateOf(false) }

    // ========== 云端模式状态喵~ 宝贝这些状态现在保存在单例中，Tab 切换不丢失喵~
    var selectedCloudHomeworkStatus by remember { mutableStateOf(CloudHomeworkState.selectedStatus) }
    var homeworkListsByStatus by remember { mutableStateOf(CloudHomeworkState.homeworkListsByStatus) }
    var isLoadingCloudHomework by remember { mutableStateOf(CloudHomeworkState.isLoading) }
    var cloudHomeworkError by remember { mutableStateOf(CloudHomeworkState.error) }
    var changyanTokenExpired by remember { mutableStateOf(false) }
    var cloudBaseUrl by remember { mutableStateOf(CloudHomeworkState.cloudBaseUrl) }
    var downloadedPapers by remember { mutableStateOf(CloudHomeworkState.downloadedPapers) }
    var downloadedHomeworkNames by remember { mutableStateOf(CloudHomeworkState.downloadedHomeworkNames) }
    var cloudDownloadingHomeworks by remember { mutableStateOf(CloudHomeworkState.cloudDownloadingHomeworks) }
    var cloudDownloadProgress by remember { mutableStateOf(CloudHomeworkState.cloudDownloadProgress) }
    var failedCloudHomeworks by remember { mutableStateOf(CloudHomeworkState.failedCloudHomeworks) }

    // 宝贝新增：云端作业占位符列表喵~ 现在显示所有作业，包括已下载的喵~
    val homeworkList by remember {
        derivedStateOf {
            homeworkListsByStatus[selectedCloudHomeworkStatus].orEmpty()
        }
    }
    val selectedCloudHomeworkLabel by remember {
        derivedStateOf {
            cloudHomeworkStatusLabel(selectedCloudHomeworkStatus)
        }
    }
    val cloudHomeworkPlaceholders by remember {
        derivedStateOf {
            homeworkList.map { hw -> createCloudHomeworkPlaceholder(hw) }
        }
    }
    val loadedLocalSectionCount by remember { derivedStateOf { papers.totalSectionCount() } }
    val loadedLocalQuestionCount by remember { derivedStateOf { papers.totalQuestionCount() } }
    val downloadedCloudPapers by remember {
        derivedStateOf { downloadedPapers.values.flatten() }
    }
    val downloadedCloudQuestionCount by remember {
        derivedStateOf { downloadedCloudPapers.totalQuestionCount() }
    }
    val shouldOpenChangyanLoginDirectly by remember {
        derivedStateOf {
            currentMode == ActivationMode.CLOUD &&
                cloudHomeworkError != null &&
                ETS100AuthManager.isChangyanWebLogin(context)
        }
    }
    
    val categoryColors = answerCategoryColors()
    
    // 时间戳格式化器
    val timeFormatter = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault())
    
    /**
     * 添加结构化调试日志
     * 宝贝这个函数会自动带上时间戳和分类信息喵~
     */
    fun addLog(
        level: LogLevel = LogLevel.INFO,
        category: LogCategory = LogCategory.SYSTEM,
        message: String
    ) {
        val timestamp = timeFormatter.format(java.util.Date())
        val entry = LogEntry(timestamp = timestamp, level = level, category = category, message = message)
        debugLog = debugLog + entry
        val logcatMessage = "[${category.label}] $message"
        when (level) {
            LogLevel.ERROR -> Log.e(TAG, logcatMessage)
            LogLevel.WARN -> Log.w(TAG, logcatMessage)
            else -> Log.i(TAG, logcatMessage)
        }
    }

    fun saveCloudReadState() {
        ReadPageStateStore.saveCloud(
            context,
            ReadPageStateStore.CloudSnapshot(
                selectedStatus = selectedCloudHomeworkStatus,
                homeworkListsByStatus = homeworkListsByStatus,
                cloudBaseUrl = cloudBaseUrl,
                downloadedPapers = downloadedPapers,
                downloadedHomeworkNames = downloadedHomeworkNames,
                failedCloudHomeworks = failedCloudHomeworks
            )
        )
    }

    fun updateCloudDownloadProgress(
        cacheKey: String,
        progress: CloudHomeworkState.DownloadProgress?
    ) {
        cloudDownloadProgress = if (progress == null) {
            cloudDownloadProgress - cacheKey
        } else {
            cloudDownloadProgress + (cacheKey to progress)
        }
        CloudHomeworkState.cloudDownloadProgress = cloudDownloadProgress
    }

    // ========== 云端模式辅助函数喵~ ==========

    /**
     * 加载云端作业列表
     */
    suspend fun loadCloudHomeworkList(status: String = selectedCloudHomeworkStatus) {
        selectedCloudHomeworkStatus = status
        CloudHomeworkState.selectedStatus = status

        if (!ETS100AuthManager.isLoggedIn(context)) {
            cloudHomeworkError = "未登录，请先在设置中登录云端账号"
            addLog(LogLevel.ERROR, LogCategory.SYSTEM, "✗ 未登录云端账号")
            return
        }

        val savedToken = ETS100AuthManager.getToken(context)
        val parentId = ETS100AuthManager.getParentAccountId(context)

        if (savedToken == null || parentId == null) {
            cloudHomeworkError = "登录信息不完整，请重新登录"
            addLog(LogLevel.ERROR, LogCategory.SYSTEM, "✗ 登录信息不完整")
            return
        }

        isLoadingCloudHomework = true
        cloudHomeworkError = null
        changyanTokenExpired = false
        CloudHomeworkState.isLoading = true
        CloudHomeworkState.error = null
        addLog(LogLevel.INFO, LogCategory.SYSTEM, "☁️ 开始加载${cloudHomeworkStatusLabel(status)}列表")

        val requestStatuses = cloudHomeworkRequestStatuses(status)
        var lastError: Throwable? = null
        var success = false

        suspend fun requestHomeworkLists(tokenForRequest: String): List<ETS100ApiClient.HomeworkListResponse> {
            return coroutineScope {
                requestStatuses.map { requestStatus ->
                    async {
                        requestStatus to ETS100ApiClient.getHomeworkList(tokenForRequest, parentId, requestStatus)
                    }
                }.awaitAll().mapNotNull { (requestStatus, result) ->
                    result.onSuccess { response ->
                        addLog(LogLevel.SUCCESS, LogCategory.SYSTEM, "✓ status=$requestStatus 获取到 ${response.homeworks.size} 个作业")
                        addLog(LogLevel.INFO, LogCategory.SYSTEM, "📡 API 返回的 base_url = ${response.baseUrl}")
                        response.homeworks.forEachIndexed { index, homework ->
                            addLog(
                                LogLevel.DEBUG,
                                LogCategory.PAPER,
                                "   ├─ 作业[$index] id=${homework.id.ifBlank { "<empty>" }}, " +
                                    "name=${homework.name}, contents=${homework.contents.size}"
                            )
                            homework.contents.forEachIndexed { contentIndex, content ->
                                addLog(
                                    LogLevel.DEBUG,
                                    LogCategory.PAPER,
                                    "   │  ├─ content[$contentIndex] group=${content.groupName}, url=${content.url}"
                                )
                            }
                        }
                    }.onFailure { e ->
                        lastError = e
                        addLog(LogLevel.ERROR, LogCategory.SYSTEM, "✗ status=$requestStatus 获取作业列表失败: ${e.message}")
                    }.getOrNull()
                }
            }
        }

        addLog(LogLevel.INFO, LogCategory.SYSTEM, "🔐 使用已保存 Token 请求作业列表")
        var responses = requestHomeworkLists(savedToken)

        if (responses.isEmpty()) {
            if (ETS100AuthManager.isChangyanWebLogin(context)) {
                val login = ETS100AuthManager.getPhone(context).orEmpty()
                cloudHomeworkError = "企业账号 Token 已失效，请重新进行讯飞登录"
                changyanTokenExpired = true
                CloudHomeworkState.error = cloudHomeworkError
                addLog(
                    LogLevel.WARN,
                    LogCategory.SYSTEM,
                    "企业登录 Token 请求失败，不执行账号密码重登；login=$login，保留已选账号上下文，等待重新讯飞登录"
                )
                isLoadingCloudHomework = false
                CloudHomeworkState.isLoading = false
                return
            }

            addLog(LogLevel.WARN, LogCategory.SYSTEM, "保存的 Token 请求失败，尝试重新登录后重试")
            val phone = ETS100AuthManager.getPhone(context)
            val savedPassword = ETS100AuthManager.getPassword(context)
            val deviceCode = ETS100AuthManager.getDeviceCode(context)

            if (phone != null && savedPassword != null) {
                try {
                    val loginResult = ETS100ApiClient.login(phone, savedPassword, deviceCode)
                    loginResult.onSuccess { loginResponse ->
                        val ecardResult = ETS100ApiClient.getEcardList(loginResponse.token)
                        ecardResult.onSuccess { parentAccountId ->
                            ETS100AuthManager.saveLoginInfo(context, phone, loginResponse.token, parentAccountId)
                            addLog(LogLevel.INFO, LogCategory.SYSTEM, "✓ 登录成功，Token 已更新，正在重试作业列表")
                            responses = requestHomeworkLists(loginResponse.token)
                        }.onFailure { e ->
                            lastError = e
                            addLog(LogLevel.ERROR, LogCategory.SYSTEM, "✗ 获取父账户ID失败: ${e.message}")
                        }
                    }.onFailure { e ->
                        lastError = e
                        addLog(LogLevel.ERROR, LogCategory.SYSTEM, "✗ 登录失败: ${e.message}")
                        addLog(LogLevel.ERROR, LogCategory.SYSTEM, "请在设置中重新登录")
                    }
                } catch (e: ETS100ApiClient.DeviceBindRequiredException) {
                    lastError = e
                    addLog(LogLevel.ERROR, LogCategory.SYSTEM, "✗ 设备需要重新绑定，请在设置中重新登录")
                }
            } else {
                addLog(LogLevel.ERROR, LogCategory.SYSTEM, "✗ 未保存密码，请先在设置中登录")
            }
        }

        val mergedHomeworks = responses.flatMap { it.homeworks }
        val latestBaseUrl = responses.lastOrNull()?.baseUrl ?: cloudBaseUrl
        val hasAnySuccessfulRequest = responses.isNotEmpty()

        if (hasAnySuccessfulRequest) {
            changyanTokenExpired = false
            homeworkListsByStatus = homeworkListsByStatus + (status to mergedHomeworks)
            cloudBaseUrl = latestBaseUrl
            CloudHomeworkState.homeworkListsByStatus = homeworkListsByStatus
            CloudHomeworkState.cloudBaseUrl = latestBaseUrl
            saveCloudReadState()
            addLog(LogLevel.SUCCESS, LogCategory.SYSTEM, "✓ 合并得到 ${mergedHomeworks.size} 个${cloudHomeworkStatusLabel(status)}")
            mergedHomeworks.forEachIndexed { index, homework ->
                addLog(
                    LogLevel.INFO,
                    LogCategory.PAPER,
                    "📚 合并作业[$index]: id=${homework.id.ifBlank { "<empty>" }}, " +
                        "name=${homework.name}, contents=${homework.contents.size}, " +
                        "key=${cloudHomeworkCacheKey(status, homework)}"
                )
            }
            success = true
        }

        if (!success) {
            cloudHomeworkError = lastError?.message ?: "获取作业列表失败"
            CloudHomeworkState.error = cloudHomeworkError
            addLog(LogLevel.ERROR, LogCategory.SYSTEM, "✗ 获取作业列表失败: $cloudHomeworkError")
        }

        isLoadingCloudHomework = false
        CloudHomeworkState.isLoading = false
    }

    fun selectCloudHomeworkStatus(status: String) {
        if (status == selectedCloudHomeworkStatus) return

        selectedCloudHomeworkStatus = status
        CloudHomeworkState.selectedStatus = status
        selectedPaper = null
        showPaperDetail = false
        cloudHomeworkError = CloudHomeworkState.error
        saveCloudReadState()
    }

    /**
     * 清除云端作业缓存
     * 宝贝删除所有已下载的云端作业文件喵~
     */
    fun clearCloudHomeworkCache() {
        if (isDeleting) return
        isDeleting = true
        scope.launch {
            try {
                addLog(LogLevel.INFO, LogCategory.SYSTEM, "🗑️ 开始后台清除云端缓存")
                withContext(Dispatchers.IO) {
                    val cacheDir = File(context.cacheDir, "cloud_homework")
                    if (cacheDir.exists()) {
                        cacheDir.deleteRecursively()
                    }
                }
                addLog(LogLevel.INFO, LogCategory.SYSTEM, "🗑️ 已删除云端缓存目录")
                downloadedPapers = emptyMap()
                downloadedHomeworkNames = emptySet()
                cloudDownloadingHomeworks = emptySet()
                cloudDownloadProgress = emptyMap()
                failedCloudHomeworks = emptySet()
                papers = emptyList()
                homeworkListsByStatus = emptyMap()
                selectedCloudHomeworkStatus = CloudHomeworkState.STATUS_CURRENT
                CloudHomeworkState.clear()
                ReadPageStateStore.clearCloud(context)
                Toast.makeText(context, "已清除所有云端缓存", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                addLog(LogLevel.ERROR, LogCategory.SYSTEM, "✗ 清除云端缓存失败: ${e.message}")
                Toast.makeText(context, "删除失败: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                isDeleting = false
            }
        }
    }

    /**
     * 下载并解析云端作业
     * 宝贝这个函数处理下载、解压和解析的全流程喵~
     */
    suspend fun downloadAndParseHomework(
        homeworkInfo: ETS100ApiClient.HomeworkInfo,
        status: String = selectedCloudHomeworkStatus
    ) {
        addLog(LogLevel.INFO, LogCategory.SYSTEM, "📥 开始下载作业: ${homeworkInfo.name}, 共 ${homeworkInfo.contents.size} 个内容")

        val cacheKey = cloudHomeworkCacheKey(status, homeworkInfo)
        if (downloadedPapers.containsKey(cacheKey)) {
            addLog(LogLevel.INFO, LogCategory.SYSTEM, "📁 缓存命中，作业已下载喵~")
            Toast.makeText(context, "该作业已下载喵~", Toast.LENGTH_SHORT).show()
            return
        }

        cloudDownloadingHomeworks = cloudDownloadingHomeworks + cacheKey
        failedCloudHomeworks = failedCloudHomeworks - cacheKey
        CloudHomeworkState.cloudDownloadingHomeworks = cloudDownloadingHomeworks
        CloudHomeworkState.failedCloudHomeworks = failedCloudHomeworks
        updateCloudDownloadProgress(
            cacheKey,
            CloudHomeworkState.DownloadProgress(currentFileName = homeworkInfo.name)
        )

        try {
            val cacheDir = PaperSourceExporter.cloudCacheDirectory(
                context,
                status,
                cloudHomeworkIdentity(homeworkInfo),
                cloudBaseUrl,
                homeworkInfo.contents.map { CloudContent(it.groupName, it.url) }
            ).apply { mkdirs() }
            val allSections = mutableListOf<ETS100AnswerReader.Section>()
            var questionIndex = 0

            for ((contentIndex, content) in homeworkInfo.contents.withIndex()) {
                // 1. 构造完整 URL
                val zipUrl = if (content.url.startsWith("http")) {
                    content.url.replaceFirst("http://", "https://")
                } else {
                    var baseUrl = cloudBaseUrl
                    if (baseUrl.startsWith("http://")) {
                        baseUrl = baseUrl.replaceFirst("http://", "https://")
                    }
                    val fullUrl = if (content.url.startsWith("/")) content.url else "/${content.url}"
                    baseUrl.trimEnd('/') + fullUrl
                }
                val zipFileName = zipUrl.substringAfterLast('/').substringBefore('?')
                if (zipFileName.isEmpty()) {
                    addLog(LogLevel.WARN, LogCategory.SYSTEM, "⚠️ 无法从 URL 提取文件名: ${content.url}")
                    continue
                }
                addLog(
                    LogLevel.INFO,
                    LogCategory.SYSTEM,
                    "⬇️ 内容 ${contentIndex + 1}/${homeworkInfo.contents.size}: " +
                        "group=${content.groupName}, url=${content.url}, zip=$zipFileName"
                )

                val zipFile = PaperSourceExporter.cloudCachedFile(cacheDir, content.url, zipFileName)
                if (PaperSourceExporter.isUsableZip(zipFile)) {
                    addLog(LogLevel.INFO, LogCategory.SYSTEM, "📦 文件已存在，跳过下载: ${zipFileName}")
                    updateCloudDownloadProgress(
                        cacheKey,
                        CloudHomeworkState.DownloadProgress(
                            downloadedBytes = zipFile.length(),
                            totalBytes = zipFile.length(),
                            currentFileName = zipFileName
                        )
                    )
                } else {
                    val temporaryFile = File(cacheDir, "${zipFile.name}.part").also { it.delete() }
                    val downloadResult = ETS100ApiClient.downloadFile(zipUrl, temporaryFile) { downloadedBytes, totalBytes ->
                        withContext(Dispatchers.Main) {
                            updateCloudDownloadProgress(
                                cacheKey,
                                CloudHomeworkState.DownloadProgress(
                                    downloadedBytes = downloadedBytes,
                                    totalBytes = totalBytes,
                                    currentFileName = zipFileName
                                )
                            )
                        }
                    }
                    downloadResult.onFailure { e ->
                        addLog(LogLevel.ERROR, LogCategory.SYSTEM, "✗ 下载失败 ${content.groupName}: ${e.message}")
                    }
                    if (downloadResult.isFailure) {
                        temporaryFile.delete()
                        addLog(LogLevel.WARN, LogCategory.SYSTEM, "⏭️ 跳过失败的下载: ${content.groupName}")
                        continue
                    }
                    if (!PaperSourceExporter.isUsableZip(temporaryFile)) {
                        temporaryFile.delete()
                        addLog(LogLevel.ERROR, LogCategory.SYSTEM, "✗ 下载结果不是完整 ZIP: $zipFileName")
                        continue
                    }
                    if (!temporaryFile.renameTo(zipFile)) {
                        temporaryFile.copyTo(zipFile, overwrite = true)
                        temporaryFile.delete()
                    }
                    addLog(LogLevel.INFO, LogCategory.SYSTEM, "✅ 下载完成: ${zipFile.length()} bytes")
                }

                // 2. 验证 ZIP Magic
                val fis = java.io.FileInputStream(zipFile)
                val magic = ByteArray(4)
                fis.read(magic)
                fis.close()
                val magicHex = magic.joinToString("") { "%02X".format(it) }
                if (magicHex != "504B0304" && magicHex != "504B0506" && magicHex != "504B0708") {
                    addLog(LogLevel.ERROR, LogCategory.SYSTEM, "⚠️ 文件不是有效 ZIP: $magicHex")
                    continue
                }

                // 3. 生成密码 + 解压
                val password = ZipPasswordGenerator.generatePassword(zipFile.absolutePath)
                if (password == null) {
                    addLog(LogLevel.ERROR, LogCategory.SYSTEM, "✗ 无法生成解压密码: ${zipFileName}")
                    continue
                }
                addLog(LogLevel.DEBUG, LogCategory.SYSTEM, "🔑 密码: ${password.substring(0, 8)}...")

                val extractDirName = zipFileName.removeSuffix(".zip")
                val extractDir = File(cacheDir, extractDirName)
                if (extractDir.exists()) {
                    extractDir.deleteRecursively()
                }
                extractDir.mkdirs()

                try {
                    val zip4jFile = net.lingala.zip4j.ZipFile(zipFile)
                    zip4jFile.setPassword(password.toCharArray())
                    zip4jFile.extractAll(extractDir.absolutePath)
                    addLog(LogLevel.DEBUG, LogCategory.SYSTEM, "📦 解压完成: $extractDirName")
                } catch (e: Exception) {
                    addLog(LogLevel.ERROR, LogCategory.SYSTEM, "✗ 解压失败 ${content.groupName}: ${e.message}")
                    continue
                }

                // 4. 直接从解压目录读取 content.json（不需要找子文件夹）
                addLog(LogLevel.INFO, LogCategory.SYSTEM, "📂 解压目录: ${extractDir.name}")

                // 列出解压目录中的所有文件/文件夹（帮助调试）
                extractDir.listFiles()?.forEach { item ->
                    val type = if (item.isDirectory) "📁" else "📄"
                    addLog(LogLevel.DEBUG, LogCategory.FILE, "  $type ${item.name}")
                }

                val contentJsonFile = File(extractDir, "content.json")
                if (!contentJsonFile.exists()) {
                    addLog(LogLevel.WARN, LogCategory.FILE, "⚠️ 解压目录中无 content.json，跳过")
                } else {
                    val contentJson = contentJsonFile.readText()
                    try {
                        val json = org.json.JSONObject(contentJson)
                        val structureType = json.optString("structure_type", "")
                        val questionCountInJson = json.optJSONObject("info")?.optJSONArray("question")?.length() ?: 0
                        val chooseCountInJson = json.optJSONObject("info")?.optJSONArray("xtlist")?.length() ?: 0
                        val stdCountInJson = json.optJSONObject("info")?.optJSONArray("std")?.length() ?: 0

                        addLog(LogLevel.INFO, LogCategory.SECTION, "📄 解析 content.json")
                        addLog(LogLevel.INFO, LogCategory.SECTION, "   ├─ structure_type: $structureType")
                        addLog(LogLevel.INFO, LogCategory.SECTION, "   ├─ group_name: ${content.groupName}")
                        addLog(
                            LogLevel.INFO,
                            LogCategory.SECTION,
                            "   ├─ raw数量: question=$questionCountInJson, xtlist=$chooseCountInJson, std=$stdCountInJson"
                        )

                        val (questions, originalContent) = ETS100AnswerReader.parseContentJson(
                            json = json,
                            startIndex = questionIndex,
                            category = "",
                            typeName = content.groupName,
                            sectionCaption = content.groupName
                        )

                        if (questions.isEmpty()) {
                            addLog(LogLevel.WARN, LogCategory.QUESTION, "   └─ ⚠️ 未解析到任何题目！")
                        } else {
                            addLog(
                                LogLevel.SUCCESS,
                                LogCategory.QUESTION,
                                "   ├─ ✅ content ${contentIndex + 1}/${homeworkInfo.contents.size} 解析到 ${questions.size} 道题"
                            )
                            for ((i, q) in questions.withIndex()) {
                                val isLast = i == questions.size - 1
                                val prefix = if (isLast) "   │   └─" else "   │   ├─"
                                addLog(LogLevel.INFO, LogCategory.QUESTION, "$prefix 第${q.order}题: ${q.questionText.take(60)}")
                                if (q.answers.isNotEmpty()) {
                                    for ((j, ans) in q.answers.withIndex()) {
                                        val ansPrefix = if (isLast) "   │       └─" else "   │       ├─"
                                        addLog(LogLevel.INFO, LogCategory.ANSWER, "$ansPrefix 答案${j+1}: ${ans.take(80)}")
                                    }
                                } else {
                                    val ansPrefix = if (isLast) "   │       └─" else "   │       ├─"
                                    addLog(LogLevel.WARN, LogCategory.ANSWER, "$ansPrefix 无标准答案")
                                }
                            }

                            allSections.add(ETS100AnswerReader.Section(
                                caption = content.groupName,
                                category = structureType,
                                typeName = content.groupName,
                                questions = questions,
                                originalContent = originalContent
                            ))
                            questionIndex += questions.size
                            addLog(
                                LogLevel.INFO,
                                LogCategory.SYSTEM,
                                "   └─ 累计: sections=${allSections.size}, questions=$questionIndex"
                            )
                        }
                    } catch (e: Exception) {
                        addLog(LogLevel.ERROR, LogCategory.FILE, "   └─ ✗ 解析 content.json 失败: ${e.message}")
                    }
                }
            }

            if (allSections.isEmpty()) {
                throw Exception("未能解析到任何题目")
            }

            val normalizedSections = normalizeCloudParsedSections(allSections)
            if (normalizedSections.size != allSections.size) {
                addLog(LogLevel.INFO, LogCategory.SECTION, "🔀 已合并云端听后选择分包并按正式题号排序")
            }

            addLog(LogLevel.SUCCESS, LogCategory.SYSTEM, "📝 共解析到 ${normalizedSections.size} 个题型，${questionIndex} 道题")

            val paper = ETS100AnswerReader.Paper(
                paperId = cacheKey.hashCode().toLong(),
                title = homeworkInfo.name,
                dataFileName = cacheKey,
                fileSize = 0L,
                sections = normalizedSections,
                source = PaperSource.Cloud(
                    status = status,
                    homeworkIdentity = cloudHomeworkIdentity(homeworkInfo),
                    baseUrl = cloudBaseUrl,
                    contents = homeworkInfo.contents.map { CloudContent(it.groupName, it.url) }
                )
            )
            downloadedPapers = downloadedPapers + (cacheKey to listOf(paper))
            downloadedHomeworkNames = downloadedHomeworkNames + cacheKey
            cloudDownloadingHomeworks = cloudDownloadingHomeworks - cacheKey
            cloudDownloadProgress = cloudDownloadProgress - cacheKey
            CloudHomeworkState.downloadedPapers = downloadedPapers
            CloudHomeworkState.downloadedHomeworkNames = downloadedHomeworkNames
            CloudHomeworkState.cloudDownloadingHomeworks = cloudDownloadingHomeworks
            CloudHomeworkState.cloudDownloadProgress = cloudDownloadProgress
            saveCloudReadState()
            // 宝贝下载成功后不再切换页面，只标记已下载，停留作业列表喵~
            Toast.makeText(context, "下载并解析成功！", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            addLog(LogLevel.ERROR, LogCategory.SYSTEM, "✗ 下载解析失败: ${e.message}")
            Toast.makeText(context, "下载失败: ${e.message}", Toast.LENGTH_LONG).show()
            cloudDownloadingHomeworks = cloudDownloadingHomeworks - cacheKey
            cloudDownloadProgress = cloudDownloadProgress - cacheKey
            failedCloudHomeworks = failedCloudHomeworks + cacheKey
            CloudHomeworkState.cloudDownloadingHomeworks = cloudDownloadingHomeworks
            CloudHomeworkState.cloudDownloadProgress = cloudDownloadProgress
            CloudHomeworkState.failedCloudHomeworks = failedCloudHomeworks
            saveCloudReadState()
        }
    }

    // 初始化加载 - 使用 rememberUpdatedState 确保最新的 currentMode
    // 宝贝添加了 reloadTrigger 依赖，这样点击读取按钮时就可以重新加载喵~
    val currentModeRef = remember { mutableStateOf(currentMode) }
    LaunchedEffect(currentMode) {
        if (currentMode != ActivationMode.CLOUD) return@LaunchedEffect
        while (true) {
            cloudDownloadingHomeworks = CloudHomeworkState.cloudDownloadingHomeworks
            cloudDownloadProgress = CloudHomeworkState.cloudDownloadProgress
            downloadedPapers = CloudHomeworkState.downloadedPapers
            downloadedHomeworkNames = CloudHomeworkState.downloadedHomeworkNames
            failedCloudHomeworks = CloudHomeworkState.failedCloudHomeworks
            delay(500)
        }
    }

    LaunchedEffect(currentMode, reloadTrigger) {
        currentModeRef.value = currentMode
        isParsingLocalAnswers = false

        if (reloadTrigger == 0 &&
            currentMode != ActivationMode.CLOUD &&
            (LocalReadProgressState.phase == LocalParsePhase.SCANNING ||
                LocalReadProgressState.phase == LocalParsePhase.PARSING)
        ) {
            isLoading = false
            isParsingLocalAnswers = true
            addLog(LogLevel.INFO, LogCategory.SYSTEM, "已恢复正在进行的本地答案解析进度")
            return@LaunchedEffect
        }
        
        // 云端模式不执行本地加载，等待用户点击读取按钮喵~
        if (currentMode == ActivationMode.CLOUD) {
            addLog(LogLevel.INFO, LogCategory.SYSTEM, "☁️ 云端模式，等待用户点击读取按钮加载作业列表")
            isLoading = false
            isParsingLocalAnswers = false
            return@LaunchedEffect
        }

        if (reloadTrigger == 0 && cachedLocalSnapshot != null) {
            readerInfo = cachedLocalSnapshot.readerInfo
            dataFiles = cachedLocalSnapshot.dataFiles
            resourceFiles = cachedLocalSnapshot.resourceFiles
            papers = cachedLocalSnapshot.papers
            errorMessage = null
            isLoading = false
            isParsingLocalAnswers = false
            addLog(LogLevel.INFO, LogCategory.SYSTEM, "已恢复上次读取的本地试卷内容")
            return@LaunchedEffect
        }

        LocalReadProgressState.reset()
        
        isLoading = true
        errorMessage = null
        debugLog = emptyList()  // 清空之前的日志喵~
        LocalReadProgressState.phase = LocalParsePhase.SCANNING
        LocalReadProgressState.message = "正在扫描试卷列表"
        
        addLog(LogLevel.INIT, LogCategory.SYSTEM, "=".repeat(50))
        addLog(LogLevel.INIT, LogCategory.SYSTEM, "🚀 开始初始化 ETS 100 数据加载")
        addLog(LogLevel.INIT, LogCategory.SYSTEM, "当前激活模式: ${currentMode.name} (${currentMode.title})")
        addLog(LogLevel.INIT, LogCategory.SYSTEM, "=".repeat(50))
        
        try {
            // 检查是否启用了强执读取模式
            val forceReadMode = SettingsManager.getForceReadMode()
            if (forceReadMode) {
                addLog(LogLevel.WARN, LogCategory.SYSTEM, "⚡ 强执读取模式已启用，跳过权限检查")
            }
            
            // 检查模式是否可用（除非强制读取模式启用）
            addLog(LogLevel.DEBUG, LogCategory.SYSTEM, "检查模式可用性...")
            if (!forceReadMode && !ETS100FileReader.isModeAvailable(currentMode, context)) {
                addLog(LogLevel.ERROR, LogCategory.SYSTEM, "当前模式不可用: $currentMode")
                errorMessage = "当前模式不可用: $currentMode"
                isLoading = false
                return@LaunchedEffect
            }
            addLog(LogLevel.SUCCESS, LogCategory.SYSTEM, "✓ 模式检查通过" + if (forceReadMode) " (强执模式跳过检查)" else "")
            
            // 在 IO 线程执行文件扫描，先把试卷列表显示出来
            addLog(LogLevel.INFO, LogCategory.SYSTEM, "开始扫描试卷列表...")
            val summaryStartMs = System.currentTimeMillis()
            val summaryResult = withContext(Dispatchers.IO) {
                try {
                    // 获取阅读器
                    addLog(LogLevel.DEBUG, LogCategory.FILE, "获取文件阅读器...")
                    val reader = ETS100FileReader.getReader(currentMode, context)
                    addLog(LogLevel.SUCCESS, LogCategory.FILE, "✓ 阅读器创建成功: ${reader.javaClass.simpleName}")
                    
                    // 获取路径信息
                    val dataDirPath = ETS100FileReader.Path.getDataDir()
                    val resourceDirPath = ETS100FileReader.Path.getResourceDir()
                    addLog(LogLevel.DEBUG, LogCategory.FILE, "数据目录: $dataDirPath")
                    addLog(LogLevel.DEBUG, LogCategory.FILE, "资源目录: $resourceDirPath")

                    val shouldScanDebugFileLists = currentMode == ActivationMode.DIRECT_READ
                    
                    val dataFilesList = if (shouldScanDebugFileLists) {
                        addLog(LogLevel.INFO, LogCategory.FILE, "扫描 data 目录...")
                        reader.listFiles(dataDirPath)
                            .filter { file -> !file.isDirectory && file.size > ETS100FileReader.Path.MIN_FILE_SIZE }
                            .sortedByDescending { it.lastModified }
                            .also { files ->
                                addLog(LogLevel.SUCCESS, LogCategory.FILE, "✓ 找到 ${files.size} 个数据文件")
                                if (files.isNotEmpty()) {
                                    addLog(LogLevel.DEBUG, LogCategory.FILE, "  最新文件: ${files.first().name} (${formatFileSize(files.first().size)})")
                                }
                            }
                    } else {
                        addLog(LogLevel.INFO, LogCategory.FILE, "Root/Shizuku 列表阶段跳过 data 明细扫描")
                        emptyList()
                    }
                    
                    val resourceFilesList = if (shouldScanDebugFileLists) {
                        addLog(LogLevel.INFO, LogCategory.FILE, "扫描 resource 目录...")
                        reader.listFiles(resourceDirPath).also { files ->
                            addLog(LogLevel.SUCCESS, LogCategory.FILE, "✓ 找到 ${files.size} 个资源文件")
                        }
                    } else {
                        addLog(LogLevel.INFO, LogCategory.FILE, "Root/Shizuku 列表阶段跳过 resource 明细扫描")
                        emptyList()
                    }
                    
                    // 先轻量读取试卷列表，不在这里解析完整答案
                    addLog(LogLevel.INFO, LogCategory.PAPER, "=" .repeat(40))
                    addLog(LogLevel.INFO, LogCategory.PAPER, "📚 开始读取试卷列表...")
                    val paperList = ETS100AnswerReader.readPaperSummaries(context, currentMode)
                    addLog(LogLevel.SUCCESS, LogCategory.PAPER, "✓ 成功扫描到 ${paperList.size} 份试卷")
                    
                    InitResult.Success(
                        readerInfo = reader.toString(),
                        dataFiles = dataFilesList,
                        resourceFiles = resourceFilesList,
                        papers = paperList
                    )
                } catch (e: Exception) {
                    addLog(LogLevel.ERROR, LogCategory.SYSTEM, "✗ IO 操作失败: ${e.message}")
                    InitResult.Error(e.message ?: "未知错误")
                }
            }
            val summaryCostMs = System.currentTimeMillis() - summaryStartMs
            
            addLog(LogLevel.INFO, LogCategory.SYSTEM, "处理试卷列表扫描结果...")
            when (summaryResult) {
                is InitResult.Success -> {
                    readerInfo = summaryResult.readerInfo
                    dataFiles = summaryResult.dataFiles
                    resourceFiles = summaryResult.resourceFiles
                    papers = summaryResult.papers
                    errorMessage = null
                    isLoading = false
                    LocalReadProgressState.totalCount = summaryResult.papers.size
                    LocalReadProgressState.doneCount = 0
                    LocalReadProgressState.parsedGroupIndexes = emptySet()
                    LocalReadProgressState.message = "列表扫描耗时 ${summaryCostMs}ms，正在后台解析答案"
                    addLog(
                        LogLevel.SUCCESS,
                        LogCategory.SYSTEM,
                        "✓ 已显示 ${summaryResult.papers.size} 份试卷，列表扫描耗时 ${summaryCostMs}ms，开始后台解析答案"
                    )

                    if (summaryResult.papers.isNotEmpty()) {
                        isParsingLocalAnswers = true
                        LocalReadProgressState.phase = LocalParsePhase.PARSING
                        LocalReadProgressState.message = "列表扫描耗时 ${summaryCostMs}ms，正在后台解析答案"
                        LocalReadProgressState.parseJob = AppCoroutineScope.scope.launch {
                            delay(32)
                            val parseStartMs = System.currentTimeMillis()
                            val fullResult = withContext(Dispatchers.IO) {
                                try {
                                    val paperList = ETS100AnswerReader.readPapersParallel(
                                        context = context,
                                        mode = currentMode
                                    ) { groupIndex, parsedGroupPapers ->
                                        withContext(Dispatchers.Main) {
                                            LocalReadProgressState.parsedGroupIndexes =
                                                LocalReadProgressState.parsedGroupIndexes + groupIndex
                                            LocalReadProgressState.doneCount =
                                                LocalReadProgressState.parsedGroupIndexes.size
                                            if (parsedGroupPapers.isNotEmpty()) {
                                                papers = papers.toMutableList().also { currentPapers ->
                                                    parsedGroupPapers.forEachIndexed { offset, parsedPaper ->
                                                        val existingIndex = currentPapers.indexOfFirst {
                                                            it.paperId == parsedPaper.paperId
                                                        }
                                                        if (existingIndex >= 0) {
                                                            currentPapers[existingIndex] = parsedPaper
                                                        } else {
                                                            val insertIndex = (groupIndex + offset)
                                                                .coerceIn(0, currentPapers.size)
                                                            currentPapers.add(insertIndex, parsedPaper)
                                                        }
                                                    }
                                                }
                                                LocalReadProgressState.sectionCount = papers.totalSectionCount()
                                                LocalReadProgressState.questionCount = papers.totalQuestionCount()
                                                addLog(
                                                    LogLevel.SUCCESS,
                                                    LogCategory.PAPER,
                                                    "✓ 已解析第 ${groupIndex + 1} 份试卷 " +
                                                        "(${LocalReadProgressState.doneCount}/${LocalReadProgressState.totalCount})，" +
                                                        "累计 ${LocalReadProgressState.sectionCount} 个分区、${LocalReadProgressState.questionCount} 道题"
                                                )
                                            }
                                        }
                                    }
                                    InitResult.Success(
                                        readerInfo = summaryResult.readerInfo,
                                        dataFiles = summaryResult.dataFiles,
                                        resourceFiles = summaryResult.resourceFiles,
                                        papers = paperList
                                    )
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    InitResult.Error(e.message ?: "未知错误")
                                }
                            }

                            try {
                                when (fullResult) {
                                    is InitResult.Success -> {
                                        if (fullResult.papers.isNotEmpty()) {
                                            papers = fullResult.papers
                                            ReadPageStateStore.saveLocal(
                                                context,
                                                ReadPageStateStore.LocalSnapshot(
                                                    mode = currentMode,
                                                    readerInfo = fullResult.readerInfo,
                                                    dataFiles = fullResult.dataFiles,
                                                    resourceFiles = fullResult.resourceFiles,
                                                    papers = fullResult.papers
                                                )
                                            )

                                            addLog(LogLevel.INIT, LogCategory.PAPER, "📊 试卷统计:")
                                            addLog(LogLevel.INIT, LogCategory.PAPER, "   总试卷数: ${fullResult.papers.size}")
                                            val totalSections = fullResult.papers.sumOf { it.sections.size }
                                            val totalQuestions = fullResult.papers.sumOf { it.sections.sumOf { s -> s.questions.size } }
                                            val answeredQuestions = fullResult.papers.sumOf { it.sections.sumOf { s -> s.questions.count { q -> q.answer.isNotEmpty() } } }
                                            val answeredPercent = if (totalQuestions > 0) {
                                                (answeredQuestions * 100.0 / totalQuestions).toInt()
                                            } else {
                                                0
                                            }
                                            addLog(LogLevel.INIT, LogCategory.SECTION, "   总分区数: $totalSections")
                                            addLog(LogLevel.INIT, LogCategory.QUESTION, "   总题目数: $totalQuestions")
                                            addLog(LogLevel.SUCCESS, LogCategory.ANSWER, "   已答题目: $answeredQuestions ($answeredPercent%)")
                                            val parseCostMs = System.currentTimeMillis() - parseStartMs
                                            addLog(LogLevel.SUCCESS, LogCategory.SYSTEM, "✓ 答案后台解析完成，耗时 ${parseCostMs}ms")
                                            LocalReadProgressState.doneCount = LocalReadProgressState.totalCount
                                            LocalReadProgressState.sectionCount = totalSections
                                            LocalReadProgressState.questionCount = totalQuestions
                                            LocalReadProgressState.phase = LocalParsePhase.COMPLETED
                                            LocalReadProgressState.message = "共 ${fullResult.papers.size} 份试卷、$totalSections 个分区、$totalQuestions 道题"
                                        } else {
                                            addLog(LogLevel.WARN, LogCategory.SYSTEM, "后台解析未返回完整试卷，保留当前试卷列表")
                                            LocalReadProgressState.phase = LocalParsePhase.FAILED
                                            LocalReadProgressState.message = "后台解析未返回完整试卷"
                                        }
                                    }
                                    is InitResult.Error -> {
                                        addLog(LogLevel.ERROR, LogCategory.SYSTEM, "✗ 后台解析答案失败: ${fullResult.message}")
                                        LocalReadProgressState.phase = LocalParsePhase.FAILED
                                        LocalReadProgressState.message = "后台解析答案失败: ${fullResult.message}"
                                    }
                                }
                            } finally {
                                isParsingLocalAnswers = false
                                LocalReadProgressState.parseJob = null
                            }
                        }
                    } else {
                        addLog(LogLevel.WARN, LogCategory.SYSTEM, "未扫描到试卷，跳过后台答案解析")
                        LocalReadProgressState.phase = LocalParsePhase.COMPLETED
                        LocalReadProgressState.message = "未扫描到试卷"
                    }
                }
                is InitResult.Error -> {
                    addLog(LogLevel.ERROR, LogCategory.SYSTEM, "✗ 加载失败: ${summaryResult.message}")
                    errorMessage = "加载失败: ${summaryResult.message}"
                    LocalReadProgressState.phase = LocalParsePhase.FAILED
                    LocalReadProgressState.message = summaryResult.message
                }
            }
        } catch (e: Exception) {
            addLog(LogLevel.ERROR, LogCategory.SYSTEM, "✗ 初始化异常: ${e.message}")
            errorMessage = "初始化异常: ${e.message}"
            LocalReadProgressState.phase = LocalParsePhase.FAILED
            LocalReadProgressState.message = e.message ?: "未知错误"
        } finally {
            isLoading = false
            addLog(LogLevel.INIT, LogCategory.SYSTEM, "初始化流程结束喵~")
        }
    }
    
    // 调试面板 - 使用 Box 覆盖而不是直接 return
    if (showDebugPanel) {
        Box(modifier = Modifier.fillMaxSize()) {
            DebugPanel(
                debugLog = debugLog,
                papers = papers,
                showDataDetails = showDataDetails,
                onToggleDataDetails = { showDataDetails = !showDataDetails },
                onClear = { debugLog = emptyList() },
                onClose = { showDebugPanel = false }
            )
        }
        return@ReadScreen
    }
    
    Scaffold(
        topBar = {
            FeTopAppBar(title = "Fe")
        },
        floatingActionButton = {
            // 宝贝右下角的可展开圆形十字按钮喵~
            val hideDebugButton = SettingsManager.getHideDebugButton()
            ExpandableCrossFab(
                isExpanded = isFabExpanded,
                hideDebugButton = hideDebugButton,
                onToggleExpand = { isFabExpanded = !isFabExpanded },
                onDebugClick = {
                    isFabExpanded = false
                    showDebugPanel = true
                },
                onDeleteClick = {
                    isFabExpanded = false
                    showDeleteConfirmDialog = true  // 宝贝显示删除确认对话框喵~
                },
                onReadClick = {
                    isFabExpanded = false
                    if (currentMode == ActivationMode.CLOUD) {
                        showCloudReadConfirmDialog = true
                    } else {
                        // 本地模式：重新加载本地试卷喵~
                        ETS100AnswerReader.clearResourceScanCache(currentMode)
                        ReadPageStateStore.clearLocal(context)
                        reloadTrigger++
                    }
                },
                onExportClick = if (currentMode in setOf(
                        ActivationMode.ROOT,
                        ActivationMode.SHIZUKU,
                        ActivationMode.DIRECT_READ
                    )
                ) {
                    {
                        isFabExpanded = false
                        showLocalDirectoryExport = true
                    }
                } else {
                    null
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                // 1. 加载中（本地模式 isLoading 或 云端模式 isLoadingCloudHomework）喵~
                isLoading || (currentMode == ActivationMode.CLOUD && isLoadingCloudHomework) -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            val loadingTitle = if (currentMode == ActivationMode.CLOUD) {
                                "正在加载云端作业..."
                            } else {
                                when (LocalReadProgressState.phase) {
                                    LocalParsePhase.SCANNING -> "正在扫描试卷列表..."
                                    LocalParsePhase.PARSING -> "正在解析答案..."
                                    else -> "正在加载试卷..."
                                }
                            }
                            Text(
                                loadingTitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            val statsText = if (currentMode == ActivationMode.CLOUD) {
                                "已下载 ${downloadedCloudPapers.size} 份作业，累计 $downloadedCloudQuestionCount 道题"
                            } else {
                                "已发现 ${papers.size} 份试卷，${loadedLocalSectionCount} 个分区，${loadedLocalQuestionCount} 道题"
                            }
                            Text(
                                statsText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // 2. 错误状态（本地 errorMessage 或 云端 cloudHomeworkError）喵~
                errorMessage != null || cloudHomeworkError != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                errorMessage ?: cloudHomeworkError ?: "未知错误",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = {
                                    if (shouldOpenChangyanLoginDirectly) {
                                        context.startActivity(ChangyanWebLoginActivity.createIntent(context))
                                    } else {
                                        onNavigateToActivation()
                                    }
                                }
                            ) {
                                Text(if (shouldOpenChangyanLoginDirectly) "前往登录" else "前往激活")
                            }
                        }
                    }
                }

                // 3. 云端模式：作业列表非空，显示作业列表或答案详情喵~
                currentMode == ActivationMode.CLOUD -> {
                    // 宝贝用 Crossfade 在作业列表和答案详情之间切换喵~
                    Crossfade(
                        targetState = showPaperDetail && selectedPaper != null,
                        animationSpec = tween(300),
                        label = "cloudPaperDetailCrossfade"
                    ) { showDetail ->
                        if (showDetail && selectedPaper != null) {
                            val paper = selectedPaper!!
                            PaperDetailScreen(
                                paper = paper,
                                onBack = {
                                    showPaperDetail = false
                                    selectedPaper = null
                                },
                                categoryColors = categoryColors,
                                compactAnswerDisplay = compactAnswerDisplay,
                                onCopyText = {
                                    copyPaperText(paper)
                                }
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                item {
                                    CloudModeInfoCard(
                                        selectedStatus = selectedCloudHomeworkStatus,
                                        onStatusChange = { selectCloudHomeworkStatus(it) }
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    CloudHomeworkStatusToggle(
                                        selectedStatus = selectedCloudHomeworkStatus,
                                        onStatusChange = { selectCloudHomeworkStatus(it) }
                                    )
                                }
                                if (homeworkListsByStatus.containsKey(selectedCloudHomeworkStatus) && homeworkList.isEmpty()) {
                                    item {
                                        EmptyCloudHomeworkCard(label = selectedCloudHomeworkLabel)
                                    }
                                }
                                itemsIndexed(homeworkList) { paperIndex, homeworkInfo ->
                                    val homeworkKey = cloudHomeworkCacheKey(selectedCloudHomeworkStatus, homeworkInfo)
                                    val isDownloaded = downloadedHomeworkNames.contains(homeworkKey)
                                    val isDownloading = cloudDownloadingHomeworks.contains(homeworkKey)
                                    val isFailed = failedCloudHomeworks.contains(homeworkKey)
                                    val downloadProgress = cloudDownloadProgress[homeworkKey]
                                    val paper = createCloudHomeworkPlaceholder(homeworkInfo)
                                    val downloadedPaper = downloadedPapers[homeworkKey]?.firstOrNull()
                                    PaperListItem(
                                        paper = paper,
                                        paperIndex = paperIndex,
                                        onClick = {
                                            if (isDownloaded) {
                                                // 宝贝已下载，点击查看详情喵~
                                                downloadedPapers[homeworkKey]?.firstOrNull()?.let { downloadedPaper ->
                                                    openPaperDetail(downloadedPaper)
                                                }
                                            } else if (!isDownloading) {
                                                // 宝贝未下载，开始下载喵~
                                                AppCoroutineScope.scope.launch {
                                                    downloadAndParseHomework(homeworkInfo, selectedCloudHomeworkStatus)
                                                }
                                            }
                                        },
                                        categoryColors = categoryColors,
                                        isLoading = isDownloading,
                                        isFailed = isFailed,
                                        isDownloaded = isDownloaded,
                                        downloadProgress = downloadProgress,
                                        isCloudMode = true,
                                        onExportSource = downloadedPaper?.let { downloaded ->
                                            { sourceExportPaper = downloaded }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. 空列表（本地和云端统一处理）喵~
                papers.isEmpty() && homeworkList.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                Icons.Default.FolderOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "未找到任何试卷",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "请确保ETS应用数据已正确配置",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                else -> {
                    // 宝贝使用 Crossfade 实现流畅的切换动画喵~
                    Crossfade(
                        targetState = showPaperDetail && selectedPaper != null,
                        animationSpec = tween(300),
                        label = "paperDetailCrossfade"
                    ) { showDetail ->
                        if (showDetail && selectedPaper != null) {
                            // 宝贝保存到局部变量避免 smart cast 问题喵~
                            val paper = selectedPaper!!
                            // 二级页面：显示试卷详情
                            PaperDetailScreen(
                                paper = paper,
                                onBack = {
                                    showPaperDetail = false
                                    selectedPaper = null
                                },
                                categoryColors = categoryColors,
                                compactAnswerDisplay = compactAnswerDisplay,
                                onCopyText = {
                                    copyPaperText(paper)
                                }
                            )
                        } else {
                            // 一级页面：显示试卷列表
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (LocalReadProgressState.phase != LocalParsePhase.IDLE) {
                                    item {
                                        LocalAnswerParsingCard(
                                            phase = LocalReadProgressState.phase,
                                            doneCount = LocalReadProgressState.doneCount,
                                            totalCount = LocalReadProgressState.totalCount,
                                            sectionCount = LocalReadProgressState.sectionCount,
                                            questionCount = LocalReadProgressState.questionCount,
                                            message = LocalReadProgressState.message
                                        )
                                    }
                                }
                                itemsIndexed(papers) { paperIndex, paper ->
                                    val isLocalAnswerLoading = paper.isLocalAnswerLoading()
                                    PaperListItem(
                                        paper = paper,
                                        paperIndex = paperIndex,
                                        onClick = {
                                            if (!isLocalAnswerLoading) {
                                                openPaperDetail(paper)
                                            }
                                        },
                                        categoryColors = categoryColors,
                                        isLoading = isLocalAnswerLoading,
                                        isClickEnabled = !isLocalAnswerLoading,
                                        onExportSource = paper.source?.let { { sourceExportPaper = paper } }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // 宝贝删除确认对话框喵~
            if (showDeleteConfirmDialog) {
                DeleteConfirmDialog(
                    onDismiss = { showDeleteConfirmDialog = false },
                    onConfirm = {
                        showDeleteConfirmDialog = false
                        // 宝贝检查模式喵~
                        if (currentMode == ActivationMode.CLOUD) {
                            // 云端模式：清除缓存喵~
                            clearCloudHomeworkCache()
                        } else {
                            if (isDeleting) return@DeleteConfirmDialog
                            isDeleting = true
                            scope.launch {
                                try {
                                    // 本地模式：删除 data 和 resource 目录喵~
                                    addLog(LogLevel.WARN, LogCategory.FILE, "🗑️ 开始后台删除 data 和 resource 目录...")

                                    val currentModeValue = currentMode
                                    addLog(LogLevel.INFO, LogCategory.FILE, "   ├─ 删除模式: ${currentModeValue.title}")

                                    val deleteResult = withContext(Dispatchers.IO) {
                                        val dataPath = ETS100FileReader.Path.getDataDir()
                                        val resourcePath = ETS100FileReader.Path.getResourceDir()
                                        val dataDeleted = ETS100FileReader.deleteDirectory(currentModeValue, dataPath, context)
                                        val resourceDeleted = ETS100FileReader.deleteDirectory(currentModeValue, resourcePath, context)
                                        dataDeleted to resourceDeleted
                                    }

                                    if (deleteResult.first) {
                                        addLog(LogLevel.SUCCESS, LogCategory.FILE, "✅ data 目录删除成功")
                                    } else {
                                        addLog(LogLevel.ERROR, LogCategory.FILE, "❌ data 目录删除失败")
                                    }

                                    if (deleteResult.second) {
                                        addLog(LogLevel.SUCCESS, LogCategory.FILE, "✅ resource 目录删除成功")
                                    } else {
                                        addLog(LogLevel.ERROR, LogCategory.FILE, "❌ resource 目录删除失败")
                                    }

                                    ReadPageStateStore.clearLocal(context)
                                    papers = emptyList()
                                    selectedPaper = null
                                    showPaperDetail = false

                                    // 刷新页面重新读取
                                    reloadTrigger++
                                } catch (e: Exception) {
                                    addLog(LogLevel.ERROR, LogCategory.FILE, "❌ 删除失败: ${e.message}")
                                    Toast.makeText(context, "删除失败: ${e.message}", Toast.LENGTH_LONG).show()
                                } finally {
                                    isDeleting = false
                                }
                            }
                        }
                    },
                    isCloudMode = currentMode == ActivationMode.CLOUD
                )
            }

            if (showCloudReadConfirmDialog) {
                CloudReadConfirmDialog(
                    onDismiss = { showCloudReadConfirmDialog = false },
                    onConfirm = {
                        showCloudReadConfirmDialog = false
                        cloudHomeworkError = null
                        scope.launch { loadCloudHomeworkList() }
                    }
                )
            }

            if (isDeleting) {
                DeletingOverlay()
            }
        }
    }

    SourceExportDialogHost(
        paper = sourceExportPaper,
        onDismissRequest = { sourceExportPaper = null }
    )
    if (showLocalDirectoryExport) {
        LocalDirectoryExportDialogHost(
            mode = currentMode,
            onDismissRequest = { showLocalDirectoryExport = false }
        )
    }
}

@Composable
private fun LocalDirectoryExportDialogHost(
    mode: ActivationMode,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showConfirmation by remember { mutableStateOf(true) }
    var inspection by remember { mutableStateOf<PaperSourceExporter.LocalDirectoryInspection?>(null) }
    var inspectionError by remember { mutableStateOf<String?>(null) }
    var isPreparing by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }

    fun shareExport(result: PaperSourceExporter.LocalDirectoryExportResult) {
        Toast.makeText(context, "已保存到 下载/Fe/${result.fileName}", Toast.LENGTH_LONG).show()
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, result.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { context.startActivity(Intent.createChooser(intent, "分享本地题目文件")) }
            .onFailure { Toast.makeText(context, "文件已保存，但没有可用的分享应用", Toast.LENGTH_LONG).show() }
    }

    fun inspectAndExport() {
        if (isPreparing || isExporting) return
        showConfirmation = false
        inspection = null
        inspectionError = null
        isPreparing = true
        scope.launch {
            runCatching { PaperSourceExporter.inspectLocalDirectoryExport(context, mode) }
                .onSuccess { result ->
                    inspection = result
                    if (result.canExport) {
                        isPreparing = false
                        isExporting = true
                        runCatching { PaperSourceExporter.exportLocalDirectory(context, result) }
                            .onSuccess(::shareExport)
                            .onFailure { error ->
                                if (error is CancellationException) throw error
                                Log.e(TAG, "本地目录导出失败", error)
                                Toast.makeText(context, "导出失败：${error.message ?: "未知错误"}", Toast.LENGTH_LONG).show()
                            }
                        isExporting = false
                        onDismissRequest()
                    }
                }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    Log.e(TAG, "本地目录预检失败", error)
                    inspectionError = error.message ?: "未知错误"
                }
            isPreparing = false
        }
    }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) inspectAndExport()
        else Toast.makeText(context, "需要存储权限才能写入下载文件夹", Toast.LENGTH_LONG).show()
    }

    fun beginExport() {
        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.P &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            inspectAndExport()
        }
    }

    if (showConfirmation) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            icon = { Icon(Icons.Default.Archive, contentDescription = null) },
            title = { Text("导出本地题目文件？") },
            text = {
                Text(
                    "将导出当前本地目录中 data 和 resource 的全部文件，并附上文件对应关系及可解析题目结构。\n\n" +
                        "请确认目录内只保留需要提交给作者处理的内容；不需要的试卷请先删除，之后可重新下载。"
                )
            },
            confirmButton = { TextButton(onClick = ::beginExport) { Text("确认导出") } },
            dismissButton = { TextButton(onClick = onDismissRequest) { Text("取消") } }
        )
    }

    inspection?.takeUnless { it.canExport }?.let { result ->
        AlertDialog(
            onDismissRequest = onDismissRequest,
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    if (result.isEmpty) "未找到可导出的题目文件" else "当前目录不适合导出"
                )
            },
            text = {
                val text = when {
                    result.isEmpty ->
                        "请先完整下载需要反馈的题目文件。若已下载但仍未检测到文件，可能是该试卷类型暂不支持，或读取出现异常；请截图题目类型后反馈作者。"
                    else -> buildString {
                        append("检测到")
                        if (result.hasTooManyParsedPapers) {
                            append(" ${result.parsedPaperCount} 套可解析试卷")
                        }
                        if (result.hasTooManyParsedPapers && result.exceedsSizeLimit) append("，且")
                        if (result.exceedsSizeLimit) {
                            append(" 本地文件总大小超过 150 MiB")
                        }
                        append("。\n\n仅保留需要答案或无法解析的内容。已可解析且不需要额外处理的试卷请不要导出；文件过多请分批处理。整理方法：全部删除后，手动完整下载需要提交给作者的无法解析试卷。")
                    }
                }
                Text(text)
            },
            confirmButton = { TextButton(onClick = onDismissRequest) { Text("我知道了") } }
        )
    }

    inspectionError?.let { error ->
        AlertDialog(
            onDismissRequest = onDismissRequest,
            icon = { Icon(Icons.Default.ErrorOutline, contentDescription = null) },
            title = { Text("无法检查本地目录") },
            text = { Text("预检失败：$error") },
            confirmButton = { TextButton(onClick = onDismissRequest) { Text("关闭") } }
        )
    }

    if (isPreparing || isExporting) {
        Dialog(onDismissRequest = {}) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                    Column {
                        Text(if (isExporting) "正在导出本地文件" else "正在检查本地目录", style = MaterialTheme.typography.titleMedium)
                        Text("请勿关闭页面", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun DeletingOverlay() {
    Dialog(onDismissRequest = {}) {
        FeOutlinedCard(
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "正在删除...",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "请稍等，文件清理在后台执行",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LocalAnswerParsingCard(
    phase: LocalParsePhase,
    doneCount: Int,
    totalCount: Int,
    sectionCount: Int,
    questionCount: Int,
    message: String
) {
    val isFinished = phase == LocalParsePhase.COMPLETED
    val isFailed = phase == LocalParsePhase.FAILED
    val progress = if (totalCount > 0) {
        (doneCount.toFloat() / totalCount).coerceIn(0f, 1f)
    } else {
        null
    }
    val title = when (phase) {
        LocalParsePhase.SCANNING -> "正在扫描试卷列表"
        LocalParsePhase.PARSING -> "正在后台解析答案"
        LocalParsePhase.COMPLETED -> "答案解析完成"
        LocalParsePhase.FAILED -> "答案解析异常"
        LocalParsePhase.IDLE -> "读取准备中"
    }
    val description = when (phase) {
        LocalParsePhase.SCANNING -> message.ifBlank { "正在批量读取资源目录并生成试卷列表" }
        LocalParsePhase.PARSING -> {
            val countText = if (totalCount > 0) "（$doneCount/$totalCount）" else ""
            message.ifBlank { "试卷列表已可查看，答案完成后会自动更新" } + countText
        }
        LocalParsePhase.COMPLETED -> message.ifBlank { "全部试卷已经解析完成" }
        LocalParsePhase.FAILED -> message.ifBlank { "后台解析失败，请重新读取" }
        LocalParsePhase.IDLE -> message
    }
    val statsText = "已解析 $sectionCount 个分区 · $questionCount 道题"
    val iconColor = when {
        isFinished -> MaterialTheme.colorScheme.primary
        isFailed -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }
    val containerColor = if (isFailed) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    val textColor = if (isFailed) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    FeOutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = containerColor,
        borderColor = if (isFailed) MaterialTheme.colorScheme.error else Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                when {
                    isFinished -> {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    isFailed -> {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    else -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = iconColor
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = iconColor
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor
                    )
                    if (sectionCount > 0 || questionCount > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = statsText,
                            style = MaterialTheme.typography.labelMedium,
                            color = textColor
                        )
                    }
                }
            }
            if (!isFinished && !isFailed) {
                Spacer(modifier = Modifier.height(12.dp))
                if (progress != null) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun EmptyCloudHomeworkCard(label: String) {
    FeOutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.FolderOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "未找到$label",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CloudHomeworkStatusToggle(
    selectedStatus: String,
    onStatusChange: (String) -> Unit
) {
    val isHistory = selectedStatus == CloudHomeworkState.STATUS_HISTORY

    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        SegmentedButton(
            selected = !isHistory,
            onClick = { onStatusChange(CloudHomeworkState.STATUS_CURRENT) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            colors = cloudHomeworkSegmentedButtonColors(),
            icon = {}
        ) {
            Text("当前作业")
        }
        SegmentedButton(
            selected = isHistory,
            onClick = { onStatusChange(CloudHomeworkState.STATUS_HISTORY) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            colors = cloudHomeworkSegmentedButtonColors(),
            icon = {}
        ) {
            Text("历史作业")
        }
    }
}

@Composable
private fun cloudHomeworkSegmentedButtonColors(): SegmentedButtonColors {
    return SegmentedButtonDefaults.colors(
        activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
        activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledActiveContainerColor = MaterialTheme.colorScheme.primaryContainer,
        disabledActiveContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        disabledInactiveContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        disabledInactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.58f)
    )
}

/**
 * 可展开的圆形十字按钮组件
 * 右下角悬浮，点击向上展开本地或云端可用的操作按钮。
 * 喵~ 这个按钮会旋转和缩放动画哦！
 */
@Composable
private fun ExpandableCrossFab(
    isExpanded: Boolean,
    hideDebugButton: Boolean = false,
    onToggleExpand: () -> Unit,
    onDebugClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onReadClick: () -> Unit,
    onExportClick: (() -> Unit)? = null
) {
    // 主按钮旋转动画 - 展开时旋转45度变成X形状
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 45f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "fab_rotation"
    )
    
    // 子按钮的间距动画
    val subButtonSpacing = 16.dp
    
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 三个子按钮，向上展开喵~
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(tween(200)) + slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ),
            exit = fadeOut(tween(150)) + slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(200, easing = FastOutSlowInEasing)
            )
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 读取按钮 - 最上面
                SubFabItem(
                    icon = Icons.Default.Book,
                    label = "读取",
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                    onClick = onReadClick
                )
                
                if (onExportClick != null) {
                    SubFabItem(
                        icon = Icons.Default.Archive,
                        label = "导出",
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        onClick = onExportClick
                    )
                }

                // 删除按钮 - 中间
                SubFabItem(
                    icon = Icons.Default.Delete,
                    label = "删除",
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                    onClick = onDeleteClick
                )
                
                // 调试按钮 - 最下面（除非 hideDebugButton 为 true 才显示）
                if (!hideDebugButton) {
                    SubFabItem(
                        icon = Icons.Default.BugReport,
                        label = "调试",
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                        onClick = onDebugClick
                    )
                }
            }
        }
        
        // 主十字按钮喵~
        FloatingActionButton(
            onClick = onToggleExpand,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp,
                focusedElevation = 0.dp,
                hoveredElevation = 0.dp
            ),
            modifier = Modifier.size(56.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.rotate(rotationAngle)
            ) {
                // 十字图标 - 两条线交叉
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = if (isExpanded) "收回菜单" else "展开菜单",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * 子按钮组件
 * 小小的圆形按钮，带图标和文字标签
 */
@Composable
internal fun SubFabItem(
    icon: ImageVector,
    label: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
        modifier = Modifier.padding(end = 4.dp)
    ) {
        // 文字标签
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // 小圆形按钮
        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = containerColor,
            contentColor = contentColor,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp,
                focusedElevation = 0.dp,
                hoveredElevation = 0.dp
            ),
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun PaperCard(
    paper: ETS100AnswerReader.Paper,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    expandedSections: Set<Pair<Int, Int>>,
    onSectionToggle: (Int) -> Unit,
    selectedSectionIndex: Int,
    selectedQuestionIndex: Int,
    onQuestionSelect: (Int, Int) -> Unit,
    categoryColors: Map<String, Color>,
    searchQuery: String,
    showOnlyAnswered: Boolean
) {
    FeOutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "收起" else "展开",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            paper.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "${paper.sections.size} 个分区 · ${paper.sections.sumOf { it.questions.size }} 道题目",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Badge(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Text("${paper.sections.size}")
                }
            }
            
            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                FeThinDivider()
                Spacer(modifier = Modifier.height(12.dp))
                
                paper.sections.forEachIndexed { sectionIndex, section ->
                    val sectionKey = Pair(paper.sections.indexOf(section), sectionIndex)
                    val isSectionExpanded = sectionKey in expandedSections
                    
                    SectionItem(
                        section = section,
                        sectionIndex = sectionIndex,
                        isExpanded = isSectionExpanded,
                        onToggleExpand = { onSectionToggle(sectionIndex) },
                        selectedQuestionIndex = if (selectedSectionIndex == sectionIndex) selectedQuestionIndex else -1,
                        onQuestionSelect = { onQuestionSelect(sectionIndex, it) },
                        categoryColor = categoryColors[section.category] ?: MaterialTheme.colorScheme.primary,
                        searchQuery = searchQuery,
                        showOnlyAnswered = showOnlyAnswered
                    )
                    
                    if (sectionIndex < paper.sections.size - 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionItem(
    section: ETS100AnswerReader.Section,
    sectionIndex: Int,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    selectedQuestionIndex: Int,
    onQuestionSelect: (Int) -> Unit,
    categoryColor: Color,
    searchQuery: String,
    showOnlyAnswered: Boolean
) {
    var expanded by remember { mutableStateOf(isExpanded) }
    val categoryStyle = answerCategoryStyle(section.category, categoryColor)
    
    LaunchedEffect(isExpanded) {
        expanded = isExpanded
    }
    
    FeOutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(4.dp, 24.dp)
                            .background(categoryStyle.accent, RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            section.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "${section.questions.size} 道题目",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        section.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = categoryStyle.accent
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "收起" else "展开",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                
                section.questions.forEachIndexed { questionIndex, question ->
                    val matchesSearch = searchQuery.isEmpty() || 
                        question.question.contains(searchQuery, ignoreCase = true) ||
                        question.answer.contains(searchQuery, ignoreCase = true)
                    
                    val hasAnswer = question.answer.isNotEmpty()
                    
                    if (matchesSearch && (!showOnlyAnswered || hasAnswer)) {
                        QuestionItem(
                            question = question,
                            questionIndex = questionIndex,
                            isSelected = selectedQuestionIndex == questionIndex,
                            onClick = { onQuestionSelect(questionIndex) },
                            categoryStyle = categoryStyle
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestionItem(
    question: ETS100AnswerReader.Question,
    questionIndex: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    categoryStyle: AnswerCategoryColor
) {
    FeOutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        containerColor = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.38f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        borderColor = if (isSelected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        } else {
            Color.Transparent
        },
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Q${question.displayOrder ?: questionIndex + 1}",
                    style = MaterialTheme.typography.labelMedium,
                    color = categoryStyle.accent,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    question.question,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            if (question.answer.isNotEmpty()) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "有答案",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Icon(
                    Icons.AutoMirrored.Filled.HelpOutline,
                    contentDescription = "无答案",
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun DebugPanel(
    debugLog: List<LogEntry>,
    papers: List<ETS100AnswerReader.Paper>,
    showDataDetails: Boolean,
    onToggleDataDetails: () -> Unit,
    onClear: () -> Unit,
    onClose: () -> Unit
) {
    // 宝贝去除数据详情标签页喵~
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "🐛 调试面板",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Row {
                    TextButton(onClick = onClear) {
                        Text("清空", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = onClose) {
                        Text("关闭", color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 宝贝只保留实时日志喵~
            LogViewerPanel(debugLog = debugLog)
        }
    }
}

/**
 * Tab 按钮组件
 */
@Composable
private fun RowScope.TabButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 日志查看器面板
 * 使用颜色高亮不同级别的日志
 */
@Composable
private fun LogViewerPanel(debugLog: List<LogEntry>) {
    // 宝贝只显示 FILE 和 SYSTEM 类别的日志喵~
    val filteredLog = debugLog.filter { 
        it.category == LogCategory.FILE || it.category == LogCategory.SYSTEM 
    }
    
    if (filteredLog.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "📝 暂无操作日志",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "文件操作日志将显示在这里喵~",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        Column {
            // 日志统计信息 - 只显示 FILE 和 SYSTEM
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(16.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val fileCount = filteredLog.count { it.category == LogCategory.FILE }
                val systemCount = filteredLog.count { it.category == LogCategory.SYSTEM }
                val successCount = filteredLog.count { it.level == LogLevel.SUCCESS }
                val warnCount = filteredLog.count { it.level == LogLevel.WARN }
                val errorCount = filteredLog.count { it.level == LogLevel.ERROR }
                
                LogStatBadge("文件", fileCount, LogLevel.INFO)
                LogStatBadge("系统", systemCount, LogLevel.DEBUG)
                LogStatBadge("成功", successCount, LogLevel.SUCCESS)
                LogStatBadge("警告", warnCount, LogLevel.WARN)
                LogStatBadge("错误", errorCount, LogLevel.ERROR)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 日志列表 - 只显示过滤后的日志
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(16.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                itemsIndexed(filteredLog) { _, entry ->
                    LogEntryItem(entry = entry)
                }
            }
        }
    }
}

/**
 * 日志统计徽章
 */
@Composable
private fun LogStatBadge(label: String, count: Int, level: LogLevel) {
    val badgeColor = Color(level.colorHex)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$count",
            style = MaterialTheme.typography.labelMedium,
            color = badgeColor
        )
    }
}

/**
 * 单条日志条目显示
 */
@Composable
private fun LogEntryItem(entry: LogEntry) {
    val textColor = Color(entry.level.colorHex)
    val categoryStyle = when (entry.category) {
        LogCategory.SYSTEM -> AnswerCategoryColor(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        LogCategory.FILE -> AnswerCategoryColor(
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )
        LogCategory.PAPER -> answerCategoryStyle("topic", MaterialTheme.colorScheme.tertiary)
        LogCategory.SECTION -> answerCategoryStyle("simple_expression_ufi", MaterialTheme.colorScheme.tertiary)
        LogCategory.QUESTION -> answerCategoryStyle("simple_expression_ufk", MaterialTheme.colorScheme.tertiary)
        LogCategory.ANSWER -> answerCategoryStyle("simple_expression_ufj", MaterialTheme.colorScheme.tertiary)
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
    ) {
        // 时间戳
        Text(
            text = entry.timestamp,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF6B7280),
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            modifier = Modifier.width(85.dp)
        )
        
        // 级别标签
        Box(
            modifier = Modifier
                .width(50.dp)
                .background(textColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                .padding(horizontal = 4.dp, vertical = 1.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = entry.level.label,
                style = MaterialTheme.typography.labelSmall,
                color = textColor
            )
        }
        
        Spacer(modifier = Modifier.width(6.dp))
        
        // 类别标签
        Box(
            modifier = Modifier
                .width(60.dp)
                .background(categoryStyle.container.copy(alpha = 0.72f), RoundedCornerShape(8.dp))
                .padding(horizontal = 4.dp, vertical = 1.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = entry.category.label,
                style = MaterialTheme.typography.labelSmall,
                color = categoryStyle.onContainer
            )
        }
        
        Spacer(modifier = Modifier.width(6.dp))
        
        // 消息内容
        Text(
            text = entry.message,
            style = MaterialTheme.typography.bodySmall,
            color = if (entry.level == LogLevel.ERROR) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * 数据详情面板
 * 展示试卷、分区、题目的层级结构
 */
@Composable
private fun DataDetailsPanel(
    papers: List<ETS100AnswerReader.Paper>,
    showDataDetails: Boolean,
    onToggleDataDetails: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // 控制栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                .clickable(onClick = onToggleDataDetails)
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (showDataDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "📂 显示完整数据结构",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Switch(
                checked = showDataDetails,
                onCheckedChange = { onToggleDataDetails() },
                modifier = Modifier
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        if (papers.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "暂无试卷数据",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(16.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 总体统计卡片
                item {
                    val totalSections = papers.sumOf { it.sections.size }
                    val totalQuestions = papers.sumOf { it.sections.sumOf { s -> s.questions.size } }
                    val answeredQuestions = papers.sumOf { it.sections.sumOf { s -> s.questions.count { q -> q.answer.isNotEmpty() } } }
                    
                    FeOutlinedCard(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "📊 数据统计总览",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                StatItem("试卷", papers.size, MaterialTheme.colorScheme.primary)
                                StatItem("分区", totalSections, MaterialTheme.colorScheme.secondary)
                                StatItem("题目", totalQuestions, MaterialTheme.colorScheme.tertiary)
                                StatItem("已答", answeredQuestions, MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
                
                // 每份试卷详情
                papers.forEachIndexed { paperIndex, paper ->
                    item {
                        PaperDetailCard(paper = paper, paperIndex = paperIndex)
                    }
                }
            }
        }
    }
}

/**
 * 统计项组件
 */
@Composable
private fun StatItem(label: String, value: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$value",
            style = MaterialTheme.typography.titleLarge,
            color = color,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 试卷详情卡片
 */
@Composable
private fun PaperDetailCard(
    paper: ETS100AnswerReader.Paper,
    paperIndex: Int
) {
    var expanded by remember { mutableStateOf(false) }
    
    FeOutlinedCard(containerColor = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.padding(12.dp)) {
            // 试卷标题行
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "📄",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = paper.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "ID: ${paper.paperId}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ) {
                        Text("${paper.sections.size} 分区")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // 展开的分区详情
            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                FeThinDivider()
                Spacer(modifier = Modifier.height(12.dp))
                
                paper.sections.forEachIndexed { sectionIndex, section ->
                    SectionDetailItem(section = section, sectionIndex = sectionIndex)
                    if (sectionIndex < paper.sections.size - 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

/**
 * 分区详情项
 */
@Composable
private fun SectionDetailItem(
    section: ETS100AnswerReader.Section,
    sectionIndex: Int
) {
    var expanded by remember { mutableStateOf(false) }
    
    val categoryStyle = answerCategoryStyle(section.category, MaterialTheme.colorScheme.secondary)
    
    FeOutlinedCard(containerColor = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(24.dp)
                            .background(categoryStyle.accent, RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = section.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = section.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = categoryStyle.accent
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${section.questions.size} 题",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            
            // 展开的题目列表
            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                FeThinDivider()
                Spacer(modifier = Modifier.height(8.dp))
                
                section.questions.forEachIndexed { qIndex, question ->
                    QuestionDetailItem(
                        question = question,
                        questionIndex = qIndex,
                        categoryStyle = categoryStyle
                    )
                    if (qIndex < section.questions.size - 1) {
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}

/**
 * 题目详情项
 */
@Composable
private fun QuestionDetailItem(
    question: ETS100AnswerReader.Question,
    questionIndex: Int,
    categoryStyle: AnswerCategoryColor,
    compactAnswerDisplay: Boolean = SettingsManager.getCompactAnswerDisplay()
) {
    var expanded by remember { mutableStateOf(false) }
    val hasAnswer = question.answer.isNotEmpty()
    
    FeOutlinedCard(
        modifier = Modifier.clickable { expanded = !expanded },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .background(categoryStyle.container.copy(alpha = 0.76f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Q${question.displayOrder ?: questionIndex + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = categoryStyle.onContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            if (hasAnswer) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (hasAnswer) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = question.question,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = if (expanded) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
            
            // 展开时显示完整信息
            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                FeThinDivider()
                Spacer(modifier = Modifier.height(8.dp))
                
                // 选项列表
                if (question.answerList.isNotEmpty()) {
                    Text(
                        text = "📝 选项列表:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    question.answerList.forEachIndexed { index, answer ->
                        Row(
                            modifier = Modifier.padding(vertical = 2.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "${index + 1}.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.width(20.dp)
                            )
                            Text(
                                text = answer,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                
                // 答案信息
                if (hasAnswer) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(16.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "答案:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = formatAnswerForDisplay(question, compactAnswerDisplay),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * 可折叠项组件
 * 宝贝这个组件用于显示原文和答案的折叠项喵~
 */
@Composable
private fun CollapsibleItem(
    title: String,
    content: String,
    defaultExpanded: Boolean = false
) {
    var expanded by remember { mutableStateOf(defaultExpanded) }
    
    FeOutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "收起" else "展开",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

/**
 * 题目块组件
 * 宝贝每个题目显示为一个块，包含题目信息和两个折叠项（原文、答案）喵~
 */
@Composable
private fun QuestionBlock(
    questionIndex: Int,
    sectionTitle: String,
    question: ETS100AnswerReader.Question,
    categoryColor: Color,
    defaultOriginalExpanded: Boolean = false,
    defaultAnswerExpanded: Boolean = false,  // 宝贝答案默认折叠喵~
    compactAnswerDisplay: Boolean = SettingsManager.getCompactAnswerDisplay()
) {
    val categoryStyle = fallbackAnswerCategoryStyle(categoryColor)

    FeOutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 题目头部
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(4.dp, 32.dp)
                        .background(categoryStyle.accent, RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Q${question.displayOrder ?: questionIndex + 1}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = categoryStyle.accent
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = categoryStyle.container.copy(alpha = 0.76f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                sectionTitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = categoryStyle.onContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                
                if (question.answer.isNotEmpty()) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "有答案",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 题目内容
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = question.question,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(12.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 原文折叠项
            if (question.formattedOriginalText.isNotEmpty()) {
                CollapsibleItem(
                    title = "原文",
                    content = question.formattedOriginalText,
                    defaultExpanded = defaultOriginalExpanded
                )
            }
            
            // 答案折叠项
            if (question.answerList.isNotEmpty()) {
                CollapsibleItem(
                    title = "答案",
                    content = formatAnswerForDisplay(question, compactAnswerDisplay),
                    defaultExpanded = defaultAnswerExpanded || compactAnswerDisplay
                )
            }
        }
    }
}

/**
 * 试卷详情二级页面
 * 宝贝这个页面显示一个试卷的所有题目喵~
 * 已移除搜索功能，显示所有题目喵~
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaperDetailScreen(
    paper: ETS100AnswerReader.Paper,
    onBack: () -> Unit,
    categoryColors: Map<String, Color>,
    compactAnswerDisplay: Boolean = SettingsManager.getCompactAnswerDisplay(),
    onCopyText: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    var sourceExportPaper by remember { mutableStateOf<ETS100AnswerReader.Paper?>(null) }
    val context = LocalContext.current
    val defaultPrimaryColor = MaterialTheme.colorScheme.primary
    val categoryPalette = answerCategoryPalette()
    val fallbackCategoryStyle = fallbackAnswerCategoryStyle(defaultPrimaryColor)
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Horizontal
                )
            )
    ) {
        LargeTopAppBar(
            title = {
                Column {
                    Text(
                        text = paper.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = paper.sections.sumOf { it.questions.size }.toString() + " \u9053\u9898\u76ee",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "\u8fd4\u56de"
                    )
                }
            },
            actions = {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "\u66f4\u591a\u64cd\u4f5c"
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("\u590d\u5236\u7b54\u6848") },
                            onClick = {
                                showMenu = false
                                onCopyText()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.ContentCopy, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("\u5bfc\u51fa\u56fe\u7247") },
                            onClick = {
                                showMenu = false
                                exportPaperAsImage(context, paper)
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Image, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("源文件导出") },
                            onClick = {
                                showMenu = false
                                sourceExportPaper = paper
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Archive, contentDescription = null)
                            }
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                scrolledContainerColor = MaterialTheme.colorScheme.surface
            ),
            scrollBehavior = scrollBehavior
        )

        FeThinDivider()

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 16.dp,
                end = 16.dp,
                bottom = 16.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            paper.sections.forEachIndexed { _, section ->
                val categoryStyle = categoryPalette[section.category]
                    ?: fallbackCategoryStyle.copy(accent = categoryColors[section.category] ?: defaultPrimaryColor)

                if (section.questions.isNotEmpty()) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp, 20.dp)
                                    .background(categoryStyle.accent, RoundedCornerShape(2.dp))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = section.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = categoryStyle.accent
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = categoryStyle.container.copy(alpha = 0.76f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = section.category,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = categoryStyle.onContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    val groupedQuestions = section.questions.groupBy { it.originalText }

                    groupedQuestions.forEach { (_, questionsInGroup) ->
                        item {
                            MergedQuestionBlock(
                                sectionTitle = section.title,
                                questions = questionsInGroup,
                                categoryStyle = categoryStyle,
                                compactAnswerDisplay = compactAnswerDisplay
                            )
                        }
                    }
                }
            }
        }
    }

    SourceExportDialogHost(
        paper = sourceExportPaper,
        onDismissRequest = { sourceExportPaper = null }
    )
}

@Composable
private fun SourceExportDialogHost(
    paper: ETS100AnswerReader.Paper?,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showConfirmation by remember(paper) { mutableStateOf(paper != null) }
    var sourceInspection by remember(paper) { mutableStateOf<PaperSourceExporter.Inspection?>(null) }
    var isExportingSource by remember(paper) { mutableStateOf(false) }

    fun shareExport(result: PaperSourceExporter.ExportResult) {
        Toast.makeText(context, "已保存到 下载/Fe/${result.fileName}", Toast.LENGTH_LONG).show()
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, result.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { context.startActivity(Intent.createChooser(intent, "分享答案源文件")) }
            .onFailure { Toast.makeText(context, "文件已保存，但没有可用的分享应用", Toast.LENGTH_LONG).show() }
    }

    fun exportSource(allowPartial: Boolean) {
        val currentPaper = paper ?: return
        if (isExportingSource) return
        sourceInspection = null
        isExportingSource = true
        scope.launch {
            runCatching { PaperSourceExporter.export(context, currentPaper, allowPartial) }
                .onSuccess(::shareExport)
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    Log.e(TAG, "源文件导出失败", error)
                    Toast.makeText(context, "导出失败：${error.message ?: "未知错误"}", Toast.LENGTH_LONG).show()
                }
            isExportingSource = false
            onDismissRequest()
        }
    }

    fun inspectAndExport() {
        val currentPaper = paper ?: return
        val source = currentPaper.source
        if (source == null) {
            Toast.makeText(context, "来源信息已失效，请重新读取该试卷", Toast.LENGTH_LONG).show()
            onDismissRequest()
            return
        }
        isExportingSource = true
        scope.launch {
            runCatching { PaperSourceExporter.inspect(context, source) }
                .onSuccess { inspection ->
                    if (inspection.isComplete) {
                        runCatching { PaperSourceExporter.export(context, currentPaper, false) }
                            .onSuccess(::shareExport)
                            .onFailure { error ->
                                if (error is CancellationException) throw error
                                Toast.makeText(context, "导出失败：${error.message}", Toast.LENGTH_LONG).show()
                            }
                        onDismissRequest()
                    } else {
                        sourceInspection = inspection
                    }
                }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    Toast.makeText(context, "无法核验源文件：${error.message}", Toast.LENGTH_LONG).show()
                    onDismissRequest()
                }
            isExportingSource = false
        }
    }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) inspectAndExport()
        else Toast.makeText(context, "需要存储权限才能写入下载文件夹", Toast.LENGTH_LONG).show()
    }

    fun beginSourceExport() {
        showConfirmation = false
        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.P &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            inspectAndExport()
        }
    }

    if (paper != null && showConfirmation) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            icon = { Icon(Icons.Default.Archive, contentDescription = null) },
            title = { Text("导出答案源文件？") },
            text = {
                Text("源文件用于提交给作者适配新题型，不适合作为常规答案分享。\n\n应用将只打包当前整套试卷关联的源文件，并保存到 下载/Fe。")
            },
            confirmButton = { TextButton(onClick = ::beginSourceExport) { Text("确认导出") } },
            dismissButton = { TextButton(onClick = onDismissRequest) { Text("取消") } }
        )
    }

    sourceInspection?.let { inspection ->
        AlertDialog(
            onDismissRequest = onDismissRequest,
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("源文件不完整") },
            text = {
                Column(
                    modifier = Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("缺少或冲突的源文件：")
                    (inspection.missing + inspection.conflicts).distinct().forEach { Text("• $it") }
                    Text("仍然导出时，导出说明中会记录以上缺失项。")
                }
            },
            confirmButton = {
                val source = paper?.source
                if (source is PaperSource.Cloud) {
                    TextButton(onClick = {
                        sourceInspection = null
                        isExportingSource = true
                        scope.launch {
                            val updated = runCatching {
                                PaperSourceExporter.downloadMissingCloudFiles(context, source)
                            }.getOrElse {
                                if (it is CancellationException) throw it
                                Toast.makeText(context, "下载缺失文件失败：${it.message}", Toast.LENGTH_LONG).show()
                                PaperSourceExporter.inspect(context, source)
                            }
                            isExportingSource = false
                            if (updated.isComplete) exportSource(false) else sourceInspection = updated
                        }
                    }) { Text("下载缺失文件") }
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = onDismissRequest) { Text("取消") }
                    TextButton(onClick = { exportSource(true) }) { Text("仍然导出") }
                }
            }
        )
    }

    if (isExportingSource) {
        Dialog(onDismissRequest = {}) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                    Column {
                        Text("正在准备源文件", style = MaterialTheme.typography.titleMedium)
                        Text("请勿关闭页面", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun MergedQuestionBlock(
    sectionTitle: String,
    questions: List<ETS100AnswerReader.Question>,
    categoryStyle: AnswerCategoryColor,
    compactAnswerDisplay: Boolean
) {
    FeOutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 块头部 - 显示原文和题目数量
            val originalText = questions.firstOrNull()?.formattedOriginalText.orEmpty()
            if (originalText.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(4.dp, 32.dp)
                            .background(categoryStyle.accent, RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "📖 原文 (${questions.size} 题)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = categoryStyle.accent
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = categoryStyle.container.copy(alpha = 0.76f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    sectionTitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = categoryStyle.onContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    
                    val hasAllAnswers = questions.all { it.answer.isNotEmpty() }
                    if (hasAllAnswers) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "全部有答案",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 原文内容
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = originalText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                FeThinDivider()
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // 题目列表
            questions.forEachIndexed { index, question ->
                if (index > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    FeThinDivider()
                    Spacer(modifier = Modifier.height(12.dp))
                }
                
                // 单个题目
                QuestionItemSimple(
                    questionIndex = index,
                    question = question,
                    categoryStyle = categoryStyle,
                    compactAnswerDisplay = compactAnswerDisplay
                )
            }
        }
    }
}

/**
 * 简化题目项组件
 * 宝贝用于在合并块中显示单个题目喵~
 */
@Composable
private fun QuestionItemSimple(
    questionIndex: Int,
    question: ETS100AnswerReader.Question,
    categoryStyle: AnswerCategoryColor,
    compactAnswerDisplay: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // 题目编号和题目内容
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .background(categoryStyle.container.copy(alpha = 0.76f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    "Q${question.displayOrder ?: questionIndex + 1}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = categoryStyle.onContainer
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 题目内容
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = question.question,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(12.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 答案显示 - 使用可折叠组件喵~
        if (question.answerList.isNotEmpty()) {
            CollapsibleItem(
                title = "✅ 答案",
                content = formatAnswerForDisplay(question, compactAnswerDisplay),
                defaultExpanded = compactAnswerDisplay
            )
        } else {
            // 模仿朗读类型显示特殊提示喵~
            val noAnswerText = if (question.category == "read_chapter") "（该类型无答案）" else "暂无答案"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(8.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.HelpOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    noAnswerText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

private fun ETS100AnswerReader.Paper.firstQuestionPreviewText(): String {
    val firstQuestion = sections
        .asSequence()
        .flatMap { it.questions.asSequence() }
        .firstOrNull { question ->
            question.category != LOCAL_PAPER_LOADING_CATEGORY &&
                question.questionText != "未下载"
        } ?: return ""

    return when {
        firstQuestion.category == "read_chapter" -> {
            firstQuestion.formattedOriginalText.firstSentenceOrLine()
                .ifBlank { firstQuestion.question.firstSentenceOrLine() }
        }
        else -> {
            val choiceOption = (firstQuestion.content as? ETS100AnswerReader.AnswerContent.Choice)
                ?.items
                ?.asSequence()
                ?.flatMap { it.options.asSequence() }
                ?.firstOrNull { it.isNotBlank() }
                ?.trim()
                ?: "${firstQuestion.question}\n${firstQuestion.formattedOriginalText}".firstChoiceOption()
            choiceOption ?: firstQuestion.question.ifBlank { firstQuestion.formattedOriginalText }.firstSentenceOrLine()
        }
    }.normalizePreviewText()
}

private fun String.firstChoiceOption(): String? {
    val match = Regex(
        pattern = """(?is)(?:^|[\s\n])(?:A|①|1)[.．、)]?\s*(.+?)(?=[\s\n](?:B|②|2)[.．、)]?\s*)"""
    ).find(this)

    return match
        ?.groupValues
        ?.getOrNull(1)
        ?.normalizePreviewText()
        ?.takeIf { it.isNotBlank() }
}

private fun String.firstSentenceOrLine(): String {
    val normalized = normalizePreviewText()
    if (normalized.isBlank()) return ""

    val sentenceEnd = listOf("。", "！", "？", ".", "!", "?")
        .map { normalized.indexOf(it) }
        .filter { it >= 0 }
        .minOrNull()

    return if (sentenceEnd != null) {
        normalized.take(sentenceEnd + 1)
    } else {
        normalized.lineSequence().firstOrNull { it.isNotBlank() }?.trim().orEmpty()
    }
}

private fun String.normalizePreviewText(): String =
    replace(Regex("\\s+"), " ").trim()

private fun ETS100AnswerReader.Paper.localDisplayNumberFromTitle(): Int? =
    Regex("""#(\d+)\s*$""").find(title)?.groupValues?.getOrNull(1)?.toIntOrNull()

/**
 * 试卷列表项组件
 * 宝贝这是一个简单的试卷项目，点击后进入二级页面喵~
 */
@Composable
private fun PaperListItem(
    paper: ETS100AnswerReader.Paper,
    paperIndex: Int,
    onClick: () -> Unit,
    categoryColors: Map<String, Color>,
    isLoading: Boolean = false,
    isFailed: Boolean = false,
    isDownloaded: Boolean = false,  // 宝贝标记是否已下载喵~
    isClickEnabled: Boolean = true,
    downloadProgress: CloudHomeworkState.DownloadProgress? = null,
    isCloudMode: Boolean = false,
    onExportSource: (() -> Unit)? = null
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val localPreviewText = remember(paper) { paper.firstQuestionPreviewText() }
    var showMenu by remember { mutableStateOf(false) }
    val listNumber = if (isCloudMode) {
        paperIndex + 1
    } else {
        paper.localDisplayNumberFromTitle() ?: (paperIndex + 1)
    }
    
    FeOutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = isClickEnabled,
                onClick = onClick
            ),
        containerColor = MaterialTheme.colorScheme.surface,
        borderColor = if (isFailed) MaterialTheme.colorScheme.error else Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // 宝贝根据状态显示不同的图标喵~
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            when {
                                isLoading -> MaterialTheme.colorScheme.primaryContainer
                                isFailed -> MaterialTheme.colorScheme.errorContainer
                                else -> MaterialTheme.colorScheme.surfaceContainer
                            },
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isLoading -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        isFailed -> {
                            Icon(
                                Icons.Default.ErrorOutline,
                                contentDescription = "加载失败",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        else -> {
                            Text(
                                text = "$listNumber",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    // 宝贝显示地区标签和标题喵~
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 宝贝添加地区标签 Badge 喵~
                        if (paper.regionLabel != "未知") {
                            val regionContainerColor = when (paper.regionLabel) {
                                "初中" -> MaterialTheme.colorScheme.secondaryContainer
                                "高中" -> MaterialTheme.colorScheme.secondaryContainer
                                else -> MaterialTheme.colorScheme.surfaceContainer
                            }
                            val regionContentColor = when (paper.regionLabel) {
                                "初中" -> MaterialTheme.colorScheme.onSecondaryContainer
                                "高中" -> MaterialTheme.colorScheme.onSecondaryContainer
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            Surface(
                                color = regionContainerColor,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = paper.regionLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = regionContentColor,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = paper.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        // 宝贝添加已下载标记喵~
                        if (isDownloaded) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                modifier = Modifier.widthIn(min = 48.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "已下载",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    maxLines = 1,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    if (!isCloudMode && localPreviewText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = localPreviewText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    // 宝贝根据状态显示不同的信息喵~
                    when {
                        isLoading -> {
                            val progressText = downloadProgress?.let { progress ->
                                val totalText = if (progress.totalBytes > 0L) {
                                    " / ${formatFileSize(progress.totalBytes)}"
                                } else {
                                    ""
                                }
                                "下载中 ${formatFileSize(progress.downloadedBytes)}$totalText"
                            } ?: "正在加载..."
                            Text(
                                text = progressText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            downloadProgress?.progressFraction?.let { progressFraction ->
                                LinearProgressIndicator(
                                    progress = { progressFraction },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } ?: LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            if (!downloadProgress?.currentFileName.isNullOrBlank()) {
                                Text(
                                    text = downloadProgress?.currentFileName.orEmpty(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        isFailed -> {
                            Text(
                                text = "加载失败，点击重试",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        else -> {
                            val countText = "${paper.sections.size} 个分区 · ${paper.sections.sumOf { it.questions.size }} 个内容"
                            Text(
                                text = countText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (isCloudMode) {
                                Text(
                                    text = if (isDownloaded) "已下载" else "未下载",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    text = "已加载",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            if (onExportSource != null) {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "更多操作",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("源文件导出") },
                            onClick = {
                                showMenu = false
                                onExportSource()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Archive, contentDescription = null)
                            }
                        )
                    }
                }
            } else if (!isLoading && !isFailed) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "进入",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 宝贝云端模式信息卡片喵~
 * 显示当前为云端作业模式
 */
@Composable
private fun CloudModeInfoCard(
    selectedStatus: String,
    onStatusChange: (String) -> Unit
) {
    val isHistory = selectedStatus == CloudHomeworkState.STATUS_HISTORY
    FeOutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FeIconContainer(
                icon = Icons.Default.Cloud,
                tint = MaterialTheme.colorScheme.primary,
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.54f),
                size = 44.dp,
                iconSize = 24.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "云端作业模式",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "点击作业项目开始下载喵~",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 宝贝删除确认对话框喵~
 * 用户点击删除按钮后会弹出这个对话框确认
 */
@Composable
private fun DeleteConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    isCloudMode: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text("确认删除？")
        },
        text = {
            if (isCloudMode) {
                Text("这将清除所有云端缓存的作业数据。\n\n此操作不可撤销！")
            } else {
                Text("这将删除以下目录中的所有文件：\n\n• data/ 目录\n• resource/ 目录\n\n此操作不可撤销！")
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("确认删除")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun CloudReadConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text("确认读取云端作业？")
        },
        text = {
            Text(
                "读取云端作业可能会导致 E听说官方客户端退出登录。\n\n" +
                    "请先确认没有正在进行的 E听说练习或考试。建议读取完答案后，再去 E听说做题；否则可能导致 E听说考试或练习连接断开。"
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("确认读取")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}


/**
 * 将试卷格式化为纯文本，用于复制到剪贴板
 */
private const val FE_SHARE_SIGNATURE = "Fe守护你的答案喵~ 官网:lastudio.cc"

internal fun exportPaperAsImage(context: android.content.Context, paper: ETS100AnswerReader.Paper) {
    try {
        val width = 1080
        val pad = 60
        val cw = width - pad * 2

        val titleP = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 48f; typeface = Typeface.DEFAULT_BOLD; color = AndroidColor.parseColor("#333333") }
        val secP = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 36f; typeface = Typeface.DEFAULT_BOLD; color = AndroidColor.parseColor("#007AFF") }
        val qP = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 30f; color = AndroidColor.parseColor("#333333") }
        val footP = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 24f; color = AndroidColor.parseColor("#999999") }

        val lg = 16f; val sg = 32f

        fun ms(text: String, paint: TextPaint): Float {
            return StaticLayout.Builder.obtain(text, 0, text.length, paint, cw).setAlignment(Layout.Alignment.ALIGN_NORMAL).setLineSpacing(lg, 1f).build().height.toFloat()
        }

        var th = pad.toFloat()
        th += ms(paper.title, titleP) + sg
        for (sec in paper.sections) {
            if (sec.questions.isEmpty()) continue
            if (sec.category == "read_chapter") continue
            th += ms(sec.title, secP) + lg
            for (q in sec.questions) {
                val isZS = sec.category == "topic"
                val ans = q.answerList.take(if (isZS) 1 else 2)
                val cleaned = ans.map { a -> ETS100AnswerReader.cleanAnswerText(a).lines().map { it.trim() }.filter { it.isNotEmpty() } }
                val at = if (isZS) cleaned.flatten().joinToString(" ") else cleaned.joinToString("\n") { it.joinToString(" ") }.ifEmpty { "" }
                th += ms("Q" + q.order + ". " + at, qP) + lg
            }
            th += sg
        }
        th += ms(FE_SHARE_SIGNATURE, footP) + pad

        val bmp = Bitmap.createBitmap(width, th.toInt(), Bitmap.Config.ARGB_8888)
        val cvs = Canvas(bmp)
        cvs.drawColor(AndroidColor.WHITE)
        var y = pad.toFloat()

        fun drawTL(text: String, paint: TextPaint, align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL): Float {
            val l = StaticLayout.Builder.obtain(text, 0, text.length, paint, cw).setAlignment(align).setLineSpacing(lg, 1f).build()
            cvs.save(); cvs.translate(pad.toFloat(), y); l.draw(cvs); cvs.restore()
            y += l.height; return l.height.toFloat()
        }

        drawTL(paper.title, titleP, Layout.Alignment.ALIGN_CENTER); y += sg
        for (sec in paper.sections) {
            if (sec.questions.isEmpty()) continue
            if (sec.category == "read_chapter") continue
            drawTL(sec.title, secP); y += lg
            for (q in sec.questions) {
                val isZS = sec.category == "topic"
                val ans = q.answerList.take(if (isZS) 1 else 2)
                val cleaned = ans.map { a -> ETS100AnswerReader.cleanAnswerText(a).lines().map { it.trim() }.filter { it.isNotEmpty() } }
                val at = if (isZS) cleaned.flatten().joinToString(" ") else cleaned.joinToString("\n") { it.joinToString(" ") }.ifEmpty { "" }
                drawTL("Q" + q.order + ". " + at, qP); y += lg
            }
            y += sg - lg
        }
        drawTL(FE_SHARE_SIGNATURE, footP, Layout.Alignment.ALIGN_CENTER)

        val fn = "Fe_Answer_" + System.currentTimeMillis() + ".png"
        val resolver = context.contentResolver
        val contentValues = android.content.ContentValues().apply {
            put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, fn)
            put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/Fe")
        }
        val uri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        if (uri != null) {
            resolver.openOutputStream(uri).use { out ->
                if (out != null) bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            bmp.recycle()
            android.widget.Toast.makeText(context, "已保存到图片/Fe", android.widget.Toast.LENGTH_SHORT).show()
            val si = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                putExtra(Intent.EXTRA_TEXT, paper.title)
            }
            try {
                context.startActivity(Intent.createChooser(si, "导出图片"))
            } catch (e: android.content.ActivityNotFoundException) {
                android.widget.Toast.makeText(context, "没有可用的分享应用", android.widget.Toast.LENGTH_SHORT).show()
            }
        } else {
            bmp.recycle()
            android.widget.Toast.makeText(context, "保存失败", android.widget.Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        android.util.Log.e("ExportImage", "Export failed", e)
        android.widget.Toast.makeText(context, "导出失败", android.widget.Toast.LENGTH_SHORT).show()
    }
}

internal fun formatPaperAsText(paper: ETS100AnswerReader.Paper): String {
    val sb = StringBuilder()
    sb.appendLine(paper.title)
    sb.appendLine()

    for (section in paper.sections) {
        if (section.questions.isEmpty()) continue
        if (section.category == "read_chapter") continue
        sb.appendLine(section.title)
        sb.appendLine()

        val isZhuanShu = section.typeName.contains("转述") || section.category == "topic"
        val takeCount = if (isZhuanShu) 1 else 2

        for (question in section.questions) {
            val answers = question.answerList.take(takeCount)
            val cleaned = answers.map { a ->
                ETS100AnswerReader.cleanAnswerText(a).lines()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
            }
            val answerText = if (isZhuanShu) {
                cleaned.flatten().joinToString(" ")
            } else {
                cleaned.joinToString("\n") { it.joinToString(" ") }
            }
            sb.append("Q").append(question.order).append(". ")
            if (answerText.isNotEmpty()) sb.appendLine(answerText) else sb.appendLine()
        }
        sb.appendLine()
    }
    sb.appendLine(FE_SHARE_SIGNATURE)
    return sb.toString().trimEnd()
}
private fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> "${size / 1024} KB"
        else -> "${size / (1024 * 1024)} MB"
    }
}
