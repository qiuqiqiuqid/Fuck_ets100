package com.shuaiqiu.fuckets100

import android.content.Context
import android.util.Log
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.json.JSONObject

/**
 * ETS100 答案读取器
 * 负责从ETS100FileReader获取的阅读器读取和解析ETS100数据
 * 
 * 使用示例:
 * val papers = ETS100AnswerReader.readPapers(context, currentMode)
 * for (paper in papers) {
 *     for (section in paper.sections) {
 *         for (question in section.questions) {
 *             println("${question.typeName}: ${question.questionText}")
 *         }
 *     }
 * }
 * 
 * 喵~ 根据文档完善了解析逻辑，现在支持所有 5 种题目类型和 3 种 structure_type 喵！
 */
object ETS100AnswerReader {

    private const val TAG = "ETS100AnswerReader"
    private const val DIRECT_READ_PARALLEL_PARSE_LIMIT = 4
    private const val PRIVILEGED_PARALLEL_PARSE_LIMIT = 4
    private const val RESOURCE_SCAN_CACHE_TTL_MS = 15_000L
    private const val VERBOSE_PARSE_LOGS = false
    private val debugTimeFormatter = java.text.SimpleDateFormat(
        "yyyy-MM-dd HH:mm:ss.SSS",
        java.util.Locale.getDefault()
    )
    private var lastResourceScanCache: ResourceScanCache? = null

    internal fun localPaperDisplayNumber(groupIndex: Int, totalGroups: Int): Int {
        return if (totalGroups > 0) {
            totalGroups - groupIndex
        } else {
            groupIndex + 1
        }.coerceAtLeast(1)
    }

    private data class ResourceScanCache(
        val mode: ActivationMode,
        val createdAtMs: Long,
        val folders: List<ETS100FileReader.FileItem>
    )

    private enum class ResourceGroupTemplate(
        val titlePrefix: String,
        val regionLabel: String
    ) {
        GUANGDONG_HIGH("广东高中", "广东高中"),
        GUANGDONG_JUNIOR("广东初中", "广东初中"),
        BEIJING_JUNIOR("北京初中", "北京初中"),
        BEIJING_HIGH("北京高中", "北京高中"),
        GENERIC("练习", "未知")
    }

    private data class ResourceGroupProfile(
        val structureTypes: Set<String>,
        val folderCount: Int,
        val structureTypeCounts: Map<String, Int>,
        val hasBeijingChooseData: Boolean,
        val hasBeijingFillData: Boolean,
        val hasBeijingDialogueData: Boolean
    ) {
        val hasBeijingData: Boolean
            get() = hasBeijingChooseData || hasBeijingFillData || hasBeijingDialogueData

        val roleCount: Int
            get() = structureTypeCounts[StructureType.COLLECTOR_ROLE] ?: 0

        val pictureCount: Int
            get() = structureTypeCounts[StructureType.COLLECTOR_PICTURE] ?: 0

        val hasGuangdongHighData: Boolean
            get() = StructureType.COLLECTOR_3Q5A in structureTypes

        val hasGuangdongJuniorData: Boolean
            get() = !hasBeijingFillData &&
                !hasBeijingDialogueData &&
                roleCount > 0 &&
                pictureCount > 0 &&
                (folderCount == 7 || !hasBeijingChooseData)
    }

    private fun logVerbose(message: String) {
        if (VERBOSE_PARSE_LOGS) {
            Log.d(TAG, message)
        }
    }

    fun clearResourceScanCache(mode: ActivationMode? = null) {
        val cache = lastResourceScanCache ?: return
        if (mode == null || cache.mode == mode) {
            Log.d(TAG, "clearResourceScanCache: clear mode=${cache.mode}")
            lastResourceScanCache = null
        }
    }

    /**
     * structure_type 常量
     * 决定了 content.json 的解析方式
     */
    object StructureType {
        const val COLLECTOR_ROLE = "collector.role"        // 问答题：听说/问答/询问
        const val COLLECTOR_PICTURE = "collector.picture"  // 信息转述
        const val COLLECTOR_READ = "collector.read"        // 模仿朗读
        const val COLLECTOR_3Q5A = "collector.3q5a"        // 广东高中：3问5答
        const val COLLECTOR_CHOOSE = "collector.choose"    // 北京：听后选择
        const val COLLECTOR_FILL = "collector.fill"        // 北京高中：听后记录/填空
        const val COLLECTOR_DIALOGUE = "collector.dialogue" // 北京高中：回答问题
    }

    /**
     * 试卷数据类
     */
    data class Paper(
        val paperId: Long,
        val title: String,
        val dataFileName: String,
        val fileSize: Long,
        val sections: List<Section>,
        val downloadTime: Long = 0L,  // 宝贝添加了下载时间喵~
        val regionLabel: String = "未知",  // 喵~ 添加地区标签：初中/高中/未知
        val paperName: String? = null,  // 喵~ 添加试卷名称（来自resource索引）
        val source: PaperSource? = null
    )

    /**
     * 分区数据类
     * 
     * 注意: 每个 section 可能包含多个不同类型的题目
     */
    data class Section(
        val caption: String,           // 标题：练习/章节的阅读理解名称
        val category: String,           // 类别：read_chapter 或 simple_expression_ufi
        val typeName: String,           // 题目类型名称
        val questions: List<Question>,   // 该 section 下的所有题目
        val originalContent: String?    // 原始内容用于后续解析
    ) {
        val title: String get() = caption
    }

    /**
     * 题目数据类
     */
    data class Question(
        val order: Int,                // 题目序号从1开始计数
        val sectionOrder: Int,          // 当前 section 下的题目序号
        val sectionCaption: String,     // 当前 section 的标题
        val typeName: String,           // 题目类型名称
        val questionText: String,       // 题目文本
        val answers: List<String>,      // 答案列表
        val originalText: String?,      // 原始文本内容
        val category: String = "",       // category 分类
        val displayOrder: Int? = null,
        val content: AnswerContent = AnswerContent.Reading("")  // 用于UI显示
    ) {
        // 兼容性别名 - ReadScreen 使用 question.question
        val question: String get() = ETS100AnswerReader.cleanQuestionText(questionText)
        val answer: String get() = answers.firstOrNull() ?: ""
        val answerList: List<String> get() = answers
        val formattedAnswer: String get() = answers
            .flatMap { ETS100AnswerReader.cleanAnswerText(it).lines() }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString("\n")
        val formattedOriginalText: String get() = originalText
            ?.let { ETS100AnswerReader.cleanOriginalText(it) }
            .orEmpty()
    }

    /**
     * 答案内容类型
     * 用于区分不同类型的答案展示
     */
    sealed class AnswerContent {
        data class Reading(val text: String) : AnswerContent()
        data class Choice(val items: List<ChoiceItem>) : AnswerContent()
        data class QATuple(val pairs: List<QAPair>) : AnswerContent()
    }

    data class ChoiceItem(
        val question: String,
        val options: List<String>,
        val correctAnswer: Int,
        val standardAnswer: String = ""
    )

    data class QAPair(
        val question: String,
        val answer: String
    )

    // 分类映射表 - 将API返回的category映射为可读的中文类型名
    // 喵~ 根据文档这个映射是正确的喵！
    private val categoryMap = mapOf(
        "read_chapter" to "模仿朗读",
        "simple_expression_ufi" to "听说信息",
        "simple_expression_ufk" to "问答信息",
        "topic" to "信息转述",
        "simple_expression_ufj" to "询问信息"
    )

    // category 到 structure_type 的映射
    // 用于当 content.json 中没有 structure_type 时 fallback
    private val categoryToStructureType = mapOf(
        "read_chapter" to StructureType.COLLECTOR_READ,
        "simple_expression_ufi" to StructureType.COLLECTOR_ROLE,
        "simple_expression_ufk" to StructureType.COLLECTOR_ROLE,
        "topic" to StructureType.COLLECTOR_PICTURE,
        "simple_expression_ufj" to StructureType.COLLECTOR_ROLE
    )

    // 习题组 code_id 到 group_name 的映射
    // 喵~ 根据文档，习题组有 7 种题型（st1-st7）喵！
    private val exerciseGroupCodeIdMap = mapOf(
        "st1" to "模仿朗读",
        "st2" to "听选信息",
        "st3" to "听选信息",
        "st4" to "听选信息",
        "st5" to "回答问题",
        "st6" to "信息转述",
        "st7" to "询问信息"
    )

    // 习题组 code_id 到 category 的映射
    private val exerciseGroupCodeIdToCategory = mapOf(
        "st1" to "read_chapter",
        "st2" to "simple_expression_ufi",
        "st3" to "simple_expression_ufi",
        "st4" to "simple_expression_ufi",
        "st5" to "simple_expression_ufk",
        "st6" to "topic",
        "st7" to "simple_expression_ufj"
    )

    /**
     * 通用文本清洗函数
     * 喵~ 移除 ets_th 前缀、HTML 标签、零宽空格喵！
     */
    private fun cleanText(text: String): String {
        if (text.isEmpty()) return ""
        return cleanAnswerText(text
            .replace(Regex("ets_th\\d+\\s*"), "")  // 移除 ets_th 前缀
        )
    }

    internal fun cleanAnswerText(text: String): String {
        return cleanDisplayText(text, splitPipes = true)
    }

    private fun cleanQuestionText(text: String): String {
        return cleanDisplayText(text, splitPipes = false)
    }

    private fun cleanOriginalText(text: String): String {
        return cleanDisplayText(text, splitPipes = false)
    }

    private fun cleanDisplayText(text: String, splitPipes: Boolean): String {
        if (text.isEmpty()) return ""
        val pipeNormalized = if (splitPipes) text.replace("|", "\n") else text
        return pipeNormalized
            .replace(Regex("</p>\\s*<p[^>]*>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<br\\s*/?>|</br>|</p>|<p[^>]*>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<[^>]+>"), "")            // 移除其他 HTML 标签
            .replace("\u200B", "")                    // 移除零宽空格
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString("\n")
    }

    /**
     * 题目文本清洗（专门针对 xt_nr 字段）
     * 喵~ 北京选择题的题目文本需要特殊处理喵！
     */
    private fun cleanQuestion(text: String): String {
        if (text.isEmpty()) return ""
        return cleanQuestionText(text
            .replace(Regex("ets_th\\d+\\s*"), "")  // 移除 ets_th 前缀
        )
    }
    private val questionNumberFields = listOf(
        "xt_xh",
        "xth",
        "xh",
        "th",
        "question_no",
        "questionNo",
        "order",
        "sort",
        "index"
    )

    private val officialQuestionNumberTextPatterns = listOf(
        Regex("ets_th\\s*(\\d+)", RegexOption.IGNORE_CASE)
    )

    private val fallbackQuestionNumberTextPatterns = listOf(
        Regex("\\u7b2c\\s*(\\d+)\\s*\\u9898"),
        Regex("^\\s*(\\d+)\\s*[.\\u3001\\uff0e)\\uff09]")
    )

    private fun parsePositiveInt(value: String): Int? {
        return value.trim().toIntOrNull()?.takeIf { it > 0 }
    }

    private fun extractQuestionNumberFromText(
        patterns: List<Regex>,
        vararg texts: String?
    ): Int? {
        for (text in texts) {
            if (text.isNullOrBlank()) continue
            for (pattern in patterns) {
                val number = pattern.find(text)
                    ?.groupValues
                    ?.getOrNull(1)
                    ?.let(::parsePositiveInt)
                if (number != null) return number
            }
        }
        return null
    }

    private fun extractQuestionNumber(item: JSONObject, vararg textKeys: String): Int? {
        val keys = (textKeys.toList() + listOf("xt_nr", "ask", "question", "text", "topic", "value")).distinct()
        val texts = keys.map { item.optString(it, "") }.toTypedArray()

        extractQuestionNumberFromText(officialQuestionNumberTextPatterns, *texts)?.let { return it }

        for (field in questionNumberFields) {
            parsePositiveInt(item.optString(field, ""))?.let { return it }
        }

        return extractQuestionNumberFromText(fallbackQuestionNumberTextPatterns, *texts)
    }

    private fun extractChooseQuestionNumber(item: JSONObject): Int? {
        val texts = arrayOf(
            item.optString("xt_nr", ""),
            item.optString("xt_value", ""),
            item.optString("xt_wj", "")
        )
        extractQuestionNumberFromText(officialQuestionNumberTextPatterns, *texts)?.let { return it }
        extractQuestionNumberFromText(fallbackQuestionNumberTextPatterns, *texts)?.let { return it }
        return parsePositiveInt(item.optString("xt_xh", ""))
    }

    private val questionOfficialOrderComparator =
        compareBy<Question> { it.displayOrder ?: it.sectionOrder }.thenBy { it.sectionOrder }

    private fun sortQuestionsByOfficialOrder(questions: List<Question>): List<Question> {
        return questions.sortedWith(questionOfficialOrderComparator)
    }

    private fun sortChooseQuestionsByOfficialOrder(questions: List<Question>): List<Question> {
        val displayOrders = questions.mapNotNull { it.displayOrder }
        val hasReliableDisplayOrder = displayOrders.size == questions.size &&
            displayOrders.distinct().size == questions.size &&
            displayOrders.sorted() == (1..questions.size).toList()

        return if (hasReliableDisplayOrder) {
            questions.sortedWith(questionOfficialOrderComparator)
        } else {
            questions.mapIndexed { index, question ->
                question.copy(
                    sectionOrder = index + 1,
                    displayOrder = index + 1
                )
            }
        }
    }

    /**
     * 检查文本是否包含中文
     * 喵~ 用于广东初中区分听选信息和提问喵！
     */
    private fun containsChinese(text: String): Boolean {
        return text.contains(Regex("[\\u4e00-\\u9fff]"))
    }

    private fun getTypeName(category: String): String {
        return categoryMap[category] ?: category
    }

    /**
     * 根据 category 获取 structure_type
     */
    internal fun getStructureType(category: String, json: JSONObject): String {
        // 优先使用 content.json 中的 structure_type
        val structureType = json.optString("structure_type", "")
        if (structureType.isNotEmpty()) {
            return structureType
        }
        // fallback 到 category 映射
        return categoryToStructureType[category] ?: StructureType.COLLECTOR_ROLE
    }

    /**
     * ========== 文档流程实现 ==========
     * Step 1: 扫描 resource/ 目录（优先）
     * Step 2: 按时间分组
     * Step 3: 根据文件夹数量路由
     * Step 4: 遍历每组的 content.json
     * Step 5: 扫描 data/ 目录获取常规习题
     * Step 6: 合并输出
     */

    /**
     * 读取所有试卷 - 按文档流程实现
     */
    fun readPapers(context: Context, mode: ActivationMode): List<Paper> {
        if (!ETS100FileReader.isModeAvailable(mode, context)) {
            Log.w(TAG, "Mode $mode is not available")
            return emptyList()
        }

        val reader = try {
            ETS100FileReader.getReader(mode, context)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get reader", e)
            return emptyList()
        }

        val resourceFolders = scanResourceFolders(reader)
        val resourceOrderMap = buildResourceOrderMap(reader)
        val groupedFolders = groupResourceFolders(reader, resourceFolders)
        Log.i(TAG, "readPapers: 得到 ${groupedFolders.size} 组试卷")

        val exerciseGroupPapers = mutableListOf<Paper>()

        // Step 3 & 4: 根据文件夹数量路由，遍历每组的 content.json
        for ((groupIndex, folderGroup) in groupedFolders.withIndex()) {
            val orderedFolderGroup = orderResourceFoldersByPaperStructure(folderGroup, resourceOrderMap)
            exerciseGroupPapers.addAll(parseResourceGroup(reader, orderedFolderGroup, groupIndex, groupedFolders.size))
        }

        Log.i(TAG, "readPapers: 完成，共 ${exerciseGroupPapers.size} 份试卷")

        return exerciseGroupPapers
    }

    fun readPaperSummaries(context: Context, mode: ActivationMode): List<Paper> {
        if (!ETS100FileReader.isModeAvailable(mode, context)) {
            Log.w(TAG, "Mode $mode is not available")
            return emptyList()
        }

        val reader = try {
            ETS100FileReader.getReader(mode, context)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get reader", e)
            return emptyList()
        }

        val resourceFolders = scanResourceFolders(reader)
        val resourceOrderMap = emptyMap<String, Int>()
        val isPrivilegedMode = mode == ActivationMode.ROOT || mode == ActivationMode.SHIZUKU
        val groupedFolders = if (isPrivilegedMode) {
            groupResourceFoldersFast(resourceFolders)
        } else {
            groupResourceFolders(reader, resourceFolders)
        }

        return groupedFolders.mapIndexed { groupIndex, folderGroup ->
            val orderedFolderGroup = orderResourceFoldersByPaperStructure(folderGroup, resourceOrderMap)
            if (isPrivilegedMode) {
                createFastPaperSummary(orderedFolderGroup, groupIndex, groupedFolders.size, mode)
            } else {
                createPaperSummary(reader, orderedFolderGroup, groupIndex, groupedFolders.size, mode)
            }
        }
    }

    suspend fun readPapersParallel(
        context: Context,
        mode: ActivationMode,
        onGroupParsed: (suspend (Int, List<Paper>) -> Unit)? = null
    ): List<Paper> = coroutineScope {
        if (!ETS100FileReader.isModeAvailable(mode, context)) {
            Log.w(TAG, "Mode $mode is not available")
            return@coroutineScope emptyList()
        }

        val reader = try {
            ETS100FileReader.getReader(mode, context)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get reader", e)
            return@coroutineScope emptyList()
        }

        val resourceFolders = scanResourceFolders(reader)
        val resourceOrderMap = buildResourceOrderMap(reader)
        val groupedFolders = groupResourceFolders(reader, resourceFolders)
        Log.i(TAG, "readPapersParallel: 得到 ${groupedFolders.size} 组试卷")

        val parallelLimit = when (mode) {
            ActivationMode.DIRECT_READ -> DIRECT_READ_PARALLEL_PARSE_LIMIT
            ActivationMode.ROOT,
            ActivationMode.SHIZUKU -> PRIVILEGED_PARALLEL_PARSE_LIMIT
            else -> 1
        }
        Log.i(TAG, "readPapersParallel: $mode 使用 $parallelLimit 路解析")

        val semaphore = Semaphore(parallelLimit)
        groupedFolders.mapIndexed { groupIndex, folderGroup ->
            val orderedFolderGroup = orderResourceFoldersByPaperStructure(folderGroup, resourceOrderMap)
            async(kotlinx.coroutines.Dispatchers.IO) {
                semaphore.withPermit {
                    val startMs = System.currentTimeMillis()
                    Log.i(
                        TAG,
                        "readPapersParallel: START group=${groupIndex + 1}/${groupedFolders.size}, " +
                            "folders=${orderedFolderGroup.size}, mode=$mode, thread=${Thread.currentThread().name}"
                    )
                    val localReader = ETS100FileReader.getReader(mode, context)
                    val papers = parseResourceGroup(localReader, orderedFolderGroup, groupIndex, groupedFolders.size)
                    onGroupParsed?.invoke(groupIndex, papers)
                    Log.i(
                        TAG,
                        "readPapersParallel: END group=${groupIndex + 1}/${groupedFolders.size}, " +
                            "papers=${papers.size}, cost=${System.currentTimeMillis() - startMs}ms, " +
                            "thread=${Thread.currentThread().name}"
                    )
                    GroupParseResult(groupIndex = groupIndex, papers = papers)
                }
            }
        }.awaitAll()
            .sortedBy { it.groupIndex }
            .flatMap { it.papers }
            .also { Log.i(TAG, "readPapersParallel: 完成，共 ${it.size} 份试卷") }
    }

    private data class GroupParseResult(
        val groupIndex: Int,
        val papers: List<Paper>
    )

    private fun detectResourceGroupTemplate(
        reader: ETS100FileReader.Reader,
        folders: List<ETS100FileReader.FileItem>
    ): ResourceGroupTemplate {
        val profile = readResourceGroupProfile(reader, folders)
        return detectResourceGroupTemplate(profile)
    }

    private fun detectResourceGroupTemplate(
        profile: ResourceGroupProfile
    ): ResourceGroupTemplate {
        return when {
            profile.hasBeijingFillData || profile.hasBeijingDialogueData -> ResourceGroupTemplate.BEIJING_HIGH
            profile.hasGuangdongHighData -> ResourceGroupTemplate.GUANGDONG_HIGH
            profile.hasGuangdongJuniorData -> ResourceGroupTemplate.GUANGDONG_JUNIOR
            profile.hasBeijingChooseData -> ResourceGroupTemplate.BEIJING_JUNIOR
            else -> ResourceGroupTemplate.GENERIC
        }
    }

    private fun scoreResourceGroupCandidate(
        reader: ETS100FileReader.Reader,
        folders: List<ETS100FileReader.FileItem>
    ): Int {
        val profile = readResourceGroupProfile(reader, folders)
        val template = detectResourceGroupTemplate(profile)
        val baseScore = when (template) {
            ResourceGroupTemplate.BEIJING_HIGH -> 100
            ResourceGroupTemplate.BEIJING_JUNIOR -> 96
            ResourceGroupTemplate.GUANGDONG_JUNIOR -> 92
            ResourceGroupTemplate.GUANGDONG_HIGH -> 90
            ResourceGroupTemplate.GENERIC -> 0
        }
        val exactSizeBonus = when (template) {
            ResourceGroupTemplate.BEIJING_HIGH -> if (folders.size == 13) 20 else 0
            ResourceGroupTemplate.BEIJING_JUNIOR -> if (folders.size == 10) 20 else 0
            ResourceGroupTemplate.GUANGDONG_JUNIOR -> if (folders.size == 7) 20 else 0
            ResourceGroupTemplate.GUANGDONG_HIGH -> if (folders.size == 3) 20 else 0
            ResourceGroupTemplate.GENERIC -> 0
        }
        return baseScore + exactSizeBonus
    }

    private fun readResourceGroupProfile(
        reader: ETS100FileReader.Reader,
        folders: List<ETS100FileReader.FileItem>
    ): ResourceGroupProfile {
        val structureTypes = mutableSetOf<String>()
        val structureTypeCounts = mutableMapOf<String, Int>()
        var hasBeijingChooseData = false
        var hasBeijingFillData = false
        var hasBeijingDialogueData = false

        for (folder in folders) {
            val contentPath = "${ETS100FileReader.Path.getResourceDir()}/${folder.name}/content.json"
            val json = runCatching {
                val content = reader.readFile(contentPath) ?: return@runCatching null
                JSONObject(content)
            }.getOrNull() ?: continue

            val structureType = json.optString("structure_type", "")
            if (structureType.isNotEmpty()) {
                structureTypes.add(structureType)
                structureTypeCounts[structureType] = (structureTypeCounts[structureType] ?: 0) + 1
            }

            val infoObj = json.optJSONObject("info") ?: continue
            when (structureType) {
                StructureType.COLLECTOR_CHOOSE -> {
                    hasBeijingChooseData = hasBeijingChooseData ||
                        ((infoObj.optJSONArray("xtlist")?.length() ?: 0) > 0)
                }
                StructureType.COLLECTOR_FILL -> {
                    hasBeijingFillData = hasBeijingFillData ||
                        ((infoObj.optJSONArray("std")?.length() ?: 0) > 0)
                }
                StructureType.COLLECTOR_DIALOGUE -> {
                    hasBeijingDialogueData = hasBeijingDialogueData ||
                        ((infoObj.optJSONArray("question")?.length() ?: 0) > 0)
                }
            }
        }

        return ResourceGroupProfile(
            structureTypes = structureTypes,
            folderCount = folders.size,
            structureTypeCounts = structureTypeCounts,
            hasBeijingChooseData = hasBeijingChooseData,
            hasBeijingFillData = hasBeijingFillData,
            hasBeijingDialogueData = hasBeijingDialogueData
        )
    }

    private fun createPaperSummary(
        reader: ETS100FileReader.Reader,
        folders: List<ETS100FileReader.FileItem>,
        groupIndex: Int,
        totalGroups: Int,
        mode: ActivationMode
    ): Paper {
        val template = detectResourceGroupTemplate(reader, folders)
        val firstFolder = folders.firstOrNull()
        val displayNumber = localPaperDisplayNumber(groupIndex, totalGroups)
        return Paper(
            paperId = buildLocalPaperId(folders, groupIndex),
            title = "${template.titlePrefix} #$displayNumber",
            dataFileName = firstFolder?.name.orEmpty(),
            fileSize = folders.sumOf { it.size },
            sections = listOf(
                Section(
                    caption = "答案解析中",
                    category = "local_loading",
                    typeName = "解析中",
                    questions = emptyList(),
                    originalContent = null
                )
            ),
            downloadTime = firstFolder?.lastModified ?: 0L,
            regionLabel = template.regionLabel,
            paperName = null,
            source = localPaperSource(mode, folders)
        )
    }

    private fun createFastPaperSummary(
        folders: List<ETS100FileReader.FileItem>,
        groupIndex: Int,
        totalGroups: Int,
        mode: ActivationMode
    ): Paper {
        val firstFolder = folders.firstOrNull()
        val displayNumber = localPaperDisplayNumber(groupIndex, totalGroups)
        return Paper(
            paperId = buildLocalPaperId(folders, groupIndex),
            title = "试卷 #$displayNumber",
            dataFileName = firstFolder?.name.orEmpty(),
            fileSize = folders.sumOf { it.size },
            sections = listOf(
                Section(
                    caption = "答案解析中",
                    category = "local_loading",
                    typeName = "解析中",
                    questions = emptyList(),
                    originalContent = null
                )
            ),
            downloadTime = firstFolder?.lastModified ?: 0L,
            regionLabel = "解析中",
            paperName = null,
            source = localPaperSource(mode, folders)
        )
    }

    private fun localPaperSource(
        mode: ActivationMode,
        folders: List<ETS100FileReader.FileItem>
    ): PaperSource.Local = PaperSource.Local(
        mode = mode,
        resourceDirectoryNames = folders.map { it.name }.distinct(),
        resourceModifiedTimes = folders.associate { it.name to it.lastModified }
    )

    private fun buildLocalPaperId(
        folders: List<ETS100FileReader.FileItem>,
        groupIndex: Int
    ): Long {
        val identity = folders
            .map { it.name }
            .sorted()
            .joinToString(separator = "|")
            .ifEmpty { "paper_$groupIndex" }
        return identity.hashCode().toLong()
    }

    private fun parseResourceGroup(
        reader: ETS100FileReader.Reader,
        orderedFolderGroup: List<ETS100FileReader.FileItem>,
        groupIndex: Int,
        totalGroups: Int
    ): List<Paper> {
        val processedContentPaths = mutableSetOf<String>()
        val folderCount = orderedFolderGroup.size
        val folderNames = orderedFolderGroup.joinToString(", ") { "${it.name}(${it.lastModified})" }
        logVerbose("╔═══ Step3&4 第 ${groupIndex + 1} 组路由日志 ═══")
        logVerbose("║ 文件夹数量: $folderCount")
        logVerbose("║ 文件夹列表: $folderNames")

        val template = detectResourceGroupTemplate(reader, orderedFolderGroup)
        Log.i(
            TAG,
            "parseResourceGroup: group=${groupIndex + 1}, folders=$folderCount, template=$template"
        )

        val papers = when (template) {
            ResourceGroupTemplate.GUANGDONG_HIGH -> {
                logVerbose("║ 路由: 广东高中解析器 (${folderCount}个文件夹)")
                parseGuangdongHighPapers(reader, orderedFolderGroup, groupIndex, totalGroups, processedContentPaths)
            }
            ResourceGroupTemplate.GUANGDONG_JUNIOR -> {
                logVerbose("║ 路由: 广东初中解析器 (${folderCount}个文件夹)")
                parseGuangdongJuniorPapers(reader, orderedFolderGroup, groupIndex, totalGroups, processedContentPaths)
            }
            ResourceGroupTemplate.BEIJING_JUNIOR -> {
                logVerbose("║ 路由: 北京初中解析器 (${folderCount}个文件夹)")
                parseBeijingJuniorPapers(reader, orderedFolderGroup, groupIndex, totalGroups, processedContentPaths)
            }
            ResourceGroupTemplate.BEIJING_HIGH -> {
                logVerbose("║ 路由: 北京高中解析器 (${folderCount}个文件夹)")
                parseBeijingHighPapers(reader, orderedFolderGroup, groupIndex, totalGroups, processedContentPaths)
            }
            ResourceGroupTemplate.GENERIC -> {
                logVerbose("║ 路由: 通用解析器 (${folderCount}个文件夹，不匹配预设)")
                parseGenericPapers(reader, orderedFolderGroup, groupIndex, totalGroups, processedContentPaths)
            }
        }
        logVerbose("║ 解析得到 ${papers.size} 份试卷")
        logVerbose("╚═══ Step3&4 路由日志结束 ═══")
        return papers
    }

    private fun scanResourceFolders(reader: ETS100FileReader.Reader): List<ETS100FileReader.FileItem> {
        val mode = reader.getMode()
        val now = System.currentTimeMillis()
        lastResourceScanCache?.let { cache ->
            if (cache.mode == mode && now - cache.createdAtMs <= RESOURCE_SCAN_CACHE_TTL_MS) {
                Log.d(TAG, "scanResourceFolders: 使用缓存 mode=$mode, count=${cache.folders.size}")
                return cache.folders
            }
        }

        val resourceDir = ETS100FileReader.Path.getResourceDir()
        val contentFolders = if (reader is ETS100FileReader.ContentFolderReader) {
            reader.listContentFolders(resourceDir)
        } else {
            reader.listFiles(resourceDir)
                .filter { it.isDirectory }
                .mapNotNull { item ->
                    val folderPath = "$resourceDir/${item.name}"
                    val contentJsonPath = "$folderPath/content.json"

                    if (reader.getFileSize(contentJsonPath) <= 0) {
                        return@mapNotNull null
                    }

                    val lastModified = item.lastModified.takeIf { it > 0L }
                        ?: reader.getFileModifiedTime(folderPath)

                    item.copy(lastModified = lastModified)
                }
        }

        val sortedFolders = contentFolders
            .sortedWith(
                compareByDescending<ETS100FileReader.FileItem> { it.lastModified }
                    .thenByDescending { it.name }
            )

        Log.d(TAG, "scanResourceFolders: 找到 ${sortedFolders.size} 个含 content.json 的文件夹")
        lastResourceScanCache = ResourceScanCache(mode, now, sortedFolders)
        sortedFolders.forEachIndexed { index, folder ->
            val readableTime = if (folder.lastModified > 0L) {
                debugTimeFormatter.format(java.util.Date(folder.lastModified))
            } else {
                "0/无效时间"
            }
            Log.d(
                TAG,
                "scanResourceFolders: #${index + 1} name=${folder.name}, " +
                    "lastModified=${folder.lastModified}, time=$readableTime, " +
                    "contentJsonSize=${folder.size}"
            )
        }
        return sortedFolders
    }

    private fun groupResourceFolders(
        reader: ETS100FileReader.Reader,
        folders: List<ETS100FileReader.FileItem>
    ): List<List<ETS100FileReader.FileItem>> {
        if (folders.isEmpty()) return emptyList()
        val hasReliableTime = folders.any { it.lastModified > 0L } &&
            folders.map { it.lastModified }.distinct().size > 1
        val hasMillisecondPrecision = folders.any { it.lastModified % 1000L != 0L }
        Log.d(
            TAG,
            "groupResourceFolders: count=${folders.size}, " +
                "hasReliableTime=$hasReliableTime, hasMillisecondPrecision=$hasMillisecondPrecision"
        )

        if (hasReliableTime) {
            return groupByTime(folders, thresholdMs = 2000L)
        }

        Log.w(TAG, "groupResourceFolders: resource 修改时间不可靠，按 content.json 结构兜底拆分")
        return groupByKnownPaperFolderCounts(reader, folders)
    }

    private fun groupResourceFoldersFast(
        folders: List<ETS100FileReader.FileItem>
    ): List<List<ETS100FileReader.FileItem>> {
        if (folders.isEmpty()) return emptyList()
        val hasReliableTime = folders.any { it.lastModified > 0L } &&
            folders.map { it.lastModified }.distinct().size > 1

        if (hasReliableTime) {
            return groupByTime(folders, thresholdMs = 2000L)
        }

        val groupSizes = listOf(13, 10, 7, 3)
        val groups = mutableListOf<List<ETS100FileReader.FileItem>>()
        var index = 0
        while (index < folders.size) {
            val remaining = folders.size - index
            val size = groupSizes
                .firstOrNull { remaining >= it && canSplitByKnownGroupSizes(remaining - it, groupSizes) }
                ?: groupSizes.firstOrNull { remaining >= it && (remaining == it || (remaining - it) >= 3) }
                ?: remaining.coerceAtMost(13)
            groups.add(folders.subList(index, index + size))
            index += size
        }

        Log.d(TAG, "groupResourceFoldersFast: ${folders.size} 个文件夹快速拆成 ${groups.size} 组")
        return groups
    }

    private fun canSplitByKnownGroupSizes(
        count: Int,
        groupSizes: List<Int>
    ): Boolean {
        if (count == 0) return true
        if (count < (groupSizes.minOrNull() ?: 0)) return false
        return groupSizes.any { count >= it && canSplitByKnownGroupSizes(count - it, groupSizes) }
    }

    private fun groupByKnownPaperFolderCounts(
        reader: ETS100FileReader.Reader,
        folders: List<ETS100FileReader.FileItem>
    ): List<List<ETS100FileReader.FileItem>> {
        val groupSizes = listOf(13, 10, 7, 3)
        val groups = mutableListOf<List<ETS100FileReader.FileItem>>()
        var index = 0

        while (index < folders.size) {
            val remaining = folders.size - index
            val candidateSizes = groupSizes
                .filter { remaining >= it && (remaining == it || (remaining - it) >= 3) }
                .ifEmpty { listOf(remaining.coerceAtMost(13)) }
            val size = candidateSizes
                .map { candidateSize ->
                    val candidate = folders.subList(index, index + candidateSize)
                    val baseScore = scoreResourceGroupCandidate(reader, candidate)
                    val exactGuangdongJuniorBonus = if (
                        remaining >= 10 &&
                        candidateSize == 7 &&
                        detectResourceGroupTemplate(reader, candidate) == ResourceGroupTemplate.GUANGDONG_JUNIOR
                    ) {
                        30
                    } else {
                        0
                    }
                    candidateSize to (baseScore + exactGuangdongJuniorBonus)
                }
                .maxWithOrNull(
                    compareBy<Pair<Int, Int>> { it.second }
                        .thenByDescending { it.first }
                )
                ?.first
                ?: remaining.coerceAtMost(13)
            groups.add(folders.subList(index, index + size))
            index += size
        }

        Log.d(TAG, "groupByKnownPaperFolderCounts: ${folders.size} 个文件夹按结构兜底拆成 ${groups.size} 组")
        return groups
    }

    private fun buildResourceOrderMap(reader: ETS100FileReader.Reader): Map<String, Int> {
        val dataDirPath = ETS100FileReader.Path.getDataDir()
        val dataFiles = reader.listFiles(dataDirPath)
            .filter { !it.isDirectory && it.size > ETS100FileReader.Path.MIN_FILE_SIZE }
            .sortedByDescending { it.lastModified }

        val orderMap = linkedMapOf<String, Int>()
        var order = 0

        for (dataFile in dataFiles) {
            val content = reader.readFile("$dataDirPath/${dataFile.name}") ?: continue
            val json = runCatching { JSONObject(content) }.getOrNull() ?: continue
            val structureType = json.optString("structure_type", "")
            val sectionData = json.optJSONArray("sectionData") ?: continue

            for (sectionIndex in 0 until sectionData.length()) {
                val section = sectionData.optJSONObject(sectionIndex) ?: continue
                val sectionItemData = section.optJSONArray("sectionItemData") ?: continue

                for (itemIndex in 0 until sectionItemData.length()) {
                    val item = sectionItemData.optJSONObject(itemIndex) ?: continue
                    val fileName = item.optString("fileName", "")
                    if (fileName.isNotEmpty() && !orderMap.containsKey(fileName)) {
                        orderMap[fileName] = order++
                    }
                }
            }
        }

        Log.d(TAG, "buildResourceOrderMap: 建立 ${orderMap.size} 个 resource 顺序映射")
        return orderMap
    }

    private fun orderResourceFoldersByPaperStructure(
        folders: List<ETS100FileReader.FileItem>,
        resourceOrderMap: Map<String, Int>
    ): List<ETS100FileReader.FileItem> {
        if (resourceOrderMap.isEmpty()) return folders

        return folders.withIndex()
            .sortedWith(
                compareBy<IndexedValue<ETS100FileReader.FileItem>> {
                    resourceOrderMap[it.value.name] ?: Int.MAX_VALUE
                }.thenBy { it.index }
            )
            .map { it.value }
    }

    /**
     * Step 2: 按时间分组算法
     * 相邻文件夹修改时间间隔 ≤ thresholdMs → 同一组
     */
    private fun groupByTime(
        folders: List<ETS100FileReader.FileItem>,
        thresholdMs: Long = 2000
    ): List<List<ETS100FileReader.FileItem>> {
        if (folders.isEmpty()) return emptyList()

        Log.d(TAG, "╔═══ groupByTime 分组日志 ═══")
        Log.d(TAG, "║ 阈值: ${thresholdMs}ms (${thresholdMs/1000.0}秒)")
        Log.d(TAG, "║ 原始文件夹数量: ${folders.size}")
        
        val groups = mutableListOf<List<ETS100FileReader.FileItem>>()
        var currentGroup = mutableListOf(folders[0])
        var groupIndex = 0
        
        for (i in 1 until folders.size) {
            val prev = folders[i - 1]
            val curr = folders[i]
            val timeDiff = curr.lastModified - prev.lastModified
            // 宝贝注意：因为是降序排列（最新在前），timeDiff 应该是负数
            // 同组条件：时间差的绝对值 <= 阈值（即时间接近）
            val isSameGroup = kotlin.math.abs(timeDiff) <= thresholdMs
            
            Log.d(TAG, "║ [$i] ${curr.name.take(12)}: 时间差=${timeDiff}ms (${timeDiff/1000.0}秒), |差值|=${kotlin.math.abs(timeDiff)}ms, ${if (isSameGroup) "同组" else "新组"}")
            
            if (isSameGroup) {
                currentGroup.add(curr)
            } else {
                groupIndex++
                groups.add(currentGroup)
                currentGroup = mutableListOf(curr)
                Log.d(TAG, "║     → 创建新组 #$groupIndex")
            }
        }
        groups.add(currentGroup)
        
        // 输出每组的统计
        Log.d(TAG, "║ ─── 分组结果 ───")
        groups.forEachIndexed { idx, group ->
            val folderNames = group.joinToString(", ") { it.name.take(8) }
            Log.d(TAG, "║ 组 #$idx: ${group.size}个文件夹 [$folderNames]")
        }
        Log.d(TAG, "║ 总计: ${groups.size} 组")
        Log.d(TAG, "╚═══ groupByTime 结束 ═══")
        
        return groups
    }

    private enum class LocalPaperKind(
        val titlePrefix: String,
        val regionLabel: String,
        val sectionOrder: List<String>
    ) {
        GUANGDONG_HIGH("广东高中", "广东高中", listOf("模仿朗读", "3问5答")),
        GUANGDONG_JUNIOR("广东初中", "广东初中", listOf("模仿朗读", "听选信息", "回答问题", "信息转述", "询问信息")),
        BEIJING_JUNIOR("北京初中", "北京初中", listOf("听后选择", "听后回答", "听后转述", "短文朗读")),
        BEIJING_HIGH("北京高中", "北京高中", listOf("听后选择", "听后记录", "听后转述", "回答问题", "短文朗读")),
        GENERIC("通用练习", "通用", emptyList())
    }

    private data class LocalSectionInfo(
        val title: String,
        val category: String
    )

    private data class LocalParsedSection(
        val info: LocalSectionInfo,
        val structureType: String,
        val sourceOrder: Int,
        val questions: List<Question>,
        val originalContent: String?,
        val firstIndex: Int,
        val templateQuestionNumbers: List<Int> = emptyList()
    )

    private data class LocalResourceTemplateSection(
        val structureType: String,
        val title: String,
        val templateOrder: Int,
        val questionNumbers: List<Int>
    )

    private data class LocalResourceTemplate(
        val folder: ETS100FileReader.FileItem,
        val sections: List<LocalResourceTemplateSection>,
        val profile: ResourceGroupProfile
    )

    private fun parseGuangdongHighPapers(
        reader: ETS100FileReader.Reader,
        folders: List<ETS100FileReader.FileItem>,
        groupIndex: Int,
        totalGroups: Int,
        processedPaths: MutableSet<String>
    ): List<Paper> = parseLocalResourcePapers(
        reader,
        folders,
        groupIndex,
        totalGroups,
        processedPaths,
        LocalPaperKind.GUANGDONG_HIGH
    )

    private fun parseGuangdongJuniorPapers(
        reader: ETS100FileReader.Reader,
        folders: List<ETS100FileReader.FileItem>,
        groupIndex: Int,
        totalGroups: Int,
        processedPaths: MutableSet<String>
    ): List<Paper> = parseLocalResourcePapers(
        reader,
        folders,
        groupIndex,
        totalGroups,
        processedPaths,
        LocalPaperKind.GUANGDONG_JUNIOR
    )

    private fun parseBeijingJuniorPapers(
        reader: ETS100FileReader.Reader,
        folders: List<ETS100FileReader.FileItem>,
        groupIndex: Int,
        totalGroups: Int,
        processedPaths: MutableSet<String>
    ): List<Paper> = parseLocalResourcePapers(
        reader,
        folders,
        groupIndex,
        totalGroups,
        processedPaths,
        LocalPaperKind.BEIJING_JUNIOR
    )

    private fun parseBeijingHighPapers(
        reader: ETS100FileReader.Reader,
        folders: List<ETS100FileReader.FileItem>,
        groupIndex: Int,
        totalGroups: Int,
        processedPaths: MutableSet<String>
    ): List<Paper> = parseLocalResourcePapers(
        reader,
        folders,
        groupIndex,
        totalGroups,
        processedPaths,
        LocalPaperKind.BEIJING_HIGH
    )

    private fun parseGenericPapers(
        reader: ETS100FileReader.Reader,
        folders: List<ETS100FileReader.FileItem>,
        groupIndex: Int,
        totalGroups: Int,
        processedPaths: MutableSet<String>
    ): List<Paper> = parseLocalResourcePapers(
        reader,
        folders,
        groupIndex,
        totalGroups,
        processedPaths,
        LocalPaperKind.GENERIC
    )

    private fun parseLocalResourcePapers(
        reader: ETS100FileReader.Reader,
        folders: List<ETS100FileReader.FileItem>,
        groupIndex: Int,
        totalGroups: Int,
        processedPaths: MutableSet<String>,
        kind: LocalPaperKind
    ): List<Paper> {
        Log.d(TAG, "parseLocalResourcePapers: 第 $groupIndex 组，${folders.size} 个文件夹，kind=$kind")
        val templateSections = readLocalResourceTemplateSections(reader, folders, kind)
        val baseStid = readLocalBaseStid(reader, folders)
        val templateCursors = mutableMapOf<String, Int>()
        val parsedSections = mutableListOf<LocalParsedSection>()
        var questionIndex = 0

        for ((folderOrder, folder) in folders.withIndex()) {
            val contentPath = "${ETS100FileReader.Path.getResourceDir()}/${folder.name}/content.json"
            processedPaths.add(contentPath)

            val content = reader.readFile(contentPath) ?: continue
            val json = runCatching { JSONObject(content) }.getOrNull() ?: continue
            val structureType = json.optString("structure_type", "")
            val templateSection = nextLocalTemplateSection(
                templateSections = templateSections,
                cursors = templateCursors,
                structureType = structureType,
                contentOrder = extractContentTemplateOrder(json, baseStid)
            )
            val sectionInfo = templateSection?.let { localSectionInfoFromTemplate(it, inferLocalSectionInfo(kind, json)) }
                ?: inferLocalSectionInfo(kind, json)
            val sourceOrder = templateSection?.let { templateSections.indexOf(it) } ?: folderOrder
            val (questions, originalContent) = parseContentJson(
                json = json,
                startIndex = questionIndex,
                category = sectionInfo.category,
                typeName = sectionInfo.title,
                sectionCaption = sectionInfo.title
            )

            if (questions.isNotEmpty()) {
                parsedSections.add(
                    LocalParsedSection(
                        info = sectionInfo,
                        structureType = structureType,
                        sourceOrder = sourceOrder,
                        questions = questions,
                        originalContent = originalContent,
                        firstIndex = folderOrder,
                        templateQuestionNumbers = templateSection?.questionNumbers.orEmpty()
                    )
                )
                questionIndex += questions.size
            } else {
                Log.d(TAG, "parseLocalResourcePapers: 跳过空题型 ${folder.name}, order=$folderOrder")
            }
        }

        if (parsedSections.isEmpty()) return emptyList()

        val orderedSections = mergeAndOrderLocalSections(parsedSections, kind.sectionOrder)
        val paperId = buildLocalPaperId(folders, groupIndex)
        val displayNumber = localPaperDisplayNumber(groupIndex, totalGroups)
        return listOf(
            Paper(
                paperId = paperId,
                title = "${kind.titlePrefix} #$displayNumber",
                dataFileName = folders.first().name,
                fileSize = folders.sumOf { it.size },
                sections = orderedSections,
                downloadTime = folders.first().lastModified,
                regionLabel = kind.regionLabel,
                paperName = null,
                source = localPaperSource(reader.getMode(), folders)
            )
        )
    }

    private fun readLocalResourceTemplateSections(
        reader: ETS100FileReader.Reader,
        folders: List<ETS100FileReader.FileItem>,
        kind: LocalPaperKind
    ): List<LocalResourceTemplateSection> {
        val resourceDir = ETS100FileReader.Path.getResourceDir()
        val groupNames = folders.mapTo(mutableSetOf()) { it.name }
        val groupTemplates = folders
            .mapNotNull { readLocalResourceTemplate(reader, resourceDir, it) }
            .filter { isTemplateCompatibleWithKind(it.profile, kind) }

        val templateSections = if (groupTemplates.isNotEmpty()) {
            groupTemplates.flatMap { it.sections }
        } else {
            val groupTimeRange = folders.map { it.lastModified }.filter { it > 0L }
            val candidateTemplates = reader.listFiles(resourceDir)
                .asSequence()
                .filter { it.isDirectory && it.name !in groupNames }
                .map { resolveTemplateFolderModifiedTime(reader, resourceDir, it) }
                .filter { isTemplateNearGroup(it, groupTimeRange) }
                .mapNotNull { readLocalResourceTemplate(reader, resourceDir, it) }
                .filter { isTemplateCompatibleWithKind(it.profile, kind) }
                .toList()

            candidateTemplates
                .minByOrNull { templateDistanceFromGroup(it.folder, groupTimeRange) }
                ?.sections
                .orEmpty()
        }

        if (templateSections.isNotEmpty()) {
            Log.d(TAG, "readLocalResourceTemplateSections: 读取到 ${templateSections.size} 个模板题段")
        }
        return templateSections
    }

    private fun resolveTemplateFolderModifiedTime(
        reader: ETS100FileReader.Reader,
        resourceDir: String,
        folder: ETS100FileReader.FileItem
    ): ETS100FileReader.FileItem {
        if (folder.lastModified > 0L) return folder
        val modifiedTime = reader.getFileModifiedTime("$resourceDir/${folder.name}")
        return folder.copy(lastModified = modifiedTime)
    }

    private fun readLocalResourceTemplate(
        reader: ETS100FileReader.Reader,
        resourceDir: String,
        folder: ETS100FileReader.FileItem
    ): LocalResourceTemplate? {
        val resContent = reader.readFile("$resourceDir/${folder.name}/res.json") ?: return null
        val json = runCatching { JSONObject(resContent) }.getOrNull() ?: return null
        val examTypeList = json.optJSONArray("exam_type_list") ?: return null
        val templateSections = mutableListOf<LocalResourceTemplateSection>()

        for (typeIndex in 0 until examTypeList.length()) {
            val examType = examTypeList.optJSONObject(typeIndex) ?: continue
            val structureType = examType.optString("exam_type_collect", "")
            val title = examType.optString("exam_type_name", structureType)
            val examList = examType.optJSONArray("exam_list")

            if (examList != null && examList.length() > 0) {
                for (examIndex in 0 until examList.length()) {
                    val exam = examList.optJSONObject(examIndex) ?: continue
                    templateSections.add(
                        LocalResourceTemplateSection(
                            structureType = structureType,
                            title = title,
                            templateOrder = extractTemplateOrder(exam, templateSections.size + 1),
                            questionNumbers = extractTemplateQuestionNumbers(exam)
                        )
                    )
                }
            } else if (examType.has("exam_id")) {
                templateSections.add(
                    LocalResourceTemplateSection(
                        structureType = structureType,
                        title = title,
                        templateOrder = extractTemplateOrder(examType, templateSections.size + 1),
                        questionNumbers = extractTemplateQuestionNumbers(examType)
                    )
                )
            }
        }

        if (templateSections.isEmpty()) return null
        return LocalResourceTemplate(
            folder = folder,
            sections = templateSections,
            profile = buildTemplateProfile(templateSections)
        )
    }

    private fun buildTemplateProfile(
        templateSections: List<LocalResourceTemplateSection>
    ): ResourceGroupProfile {
        val structureTypes = templateSections
            .mapTo(mutableSetOf()) { it.structureType }
        val structureTypeCounts = templateSections
            .groupingBy { it.structureType }
            .eachCount()
        val hasChoose = StructureType.COLLECTOR_CHOOSE in structureTypes
        val hasFill = StructureType.COLLECTOR_FILL in structureTypes
        val hasDialogue = StructureType.COLLECTOR_DIALOGUE in structureTypes
        return ResourceGroupProfile(
            structureTypes = structureTypes,
            folderCount = templateSections.size,
            structureTypeCounts = structureTypeCounts,
            hasBeijingChooseData = hasChoose,
            hasBeijingFillData = hasFill,
            hasBeijingDialogueData = hasDialogue
        )
    }

    private fun isTemplateCompatibleWithKind(
        profile: ResourceGroupProfile,
        kind: LocalPaperKind
    ): Boolean {
        return when (kind) {
            LocalPaperKind.GUANGDONG_HIGH -> profile.hasGuangdongHighData
            LocalPaperKind.GUANGDONG_JUNIOR -> profile.hasGuangdongJuniorData
            LocalPaperKind.BEIJING_JUNIOR -> profile.hasBeijingChooseData
            LocalPaperKind.BEIJING_HIGH -> profile.hasBeijingFillData || profile.hasBeijingDialogueData
            LocalPaperKind.GENERIC -> true
        }
    }

    private fun isTemplateNearGroup(
        folder: ETS100FileReader.FileItem,
        groupTimeRange: List<Long>
    ): Boolean {
        if (groupTimeRange.isEmpty() || folder.lastModified <= 0L) return false
        return templateDistanceFromGroup(folder, groupTimeRange) <= 2000L
    }

    private fun templateDistanceFromGroup(
        folder: ETS100FileReader.FileItem,
        groupTimeRange: List<Long>
    ): Long {
        if (groupTimeRange.isEmpty() || folder.lastModified <= 0L) return Long.MAX_VALUE
        return groupTimeRange.minOf { kotlin.math.abs(folder.lastModified - it) }
    }

    private fun extractTemplateOrder(exam: JSONObject, fallback: Int): Int {
        val examId = exam.optString("exam_id", "")
        Regex("st(\\d+)").find(examId)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let { return it }
        return fallback
    }

    private fun extractTemplateQuestionNumbers(exam: JSONObject): List<Int> {
        val numbers = mutableListOf<Int>()
        val examInfo = exam.optJSONArray("exam_info")
        if (examInfo != null) {
            for (index in 0 until examInfo.length()) {
                val item = examInfo.optJSONObject(index) ?: continue
                parsePositiveInt(item.optString("value", "").trim().trimEnd('.', '。'))?.let { numbers.add(it) }
            }
        }
        if (numbers.isNotEmpty()) return numbers

        val count = parsePositiveInt(exam.optString("exam_count", "")) ?: return emptyList()
        val order = parsePositiveInt(exam.optString("exam_order", "")) ?: return emptyList()
        val start = (order - 1) * count + 1
        return (start until start + count).toList()
    }

    private fun readLocalBaseStid(
        reader: ETS100FileReader.Reader,
        folders: List<ETS100FileReader.FileItem>
    ): Int? {
        val resourceDir = ETS100FileReader.Path.getResourceDir()
        return folders.mapNotNull { folder ->
            val content = reader.readFile("$resourceDir/${folder.name}/content.json") ?: return@mapNotNull null
            val json = runCatching { JSONObject(content) }.getOrNull() ?: return@mapNotNull null
            parsePositiveInt(json.optJSONObject("info")?.optString("stid", "").orEmpty())
        }.minOrNull()
    }

    private fun extractContentTemplateOrder(json: JSONObject, baseStid: Int?): Int? {
        val stid = parsePositiveInt(json.optJSONObject("info")?.optString("stid", "").orEmpty()) ?: return null
        return baseStid?.let { stid - it + 1 }?.takeIf { it > 0 }
    }

    private fun nextLocalTemplateSection(
        templateSections: List<LocalResourceTemplateSection>,
        cursors: MutableMap<String, Int>,
        structureType: String,
        contentOrder: Int?
    ): LocalResourceTemplateSection? {
        if (structureType.isEmpty()) return null
        val matches = templateSections.filter { it.structureType == structureType }
        if (matches.isEmpty()) return null

        contentOrder?.let { order ->
            matches.firstOrNull { it.templateOrder == order }?.let { return it }
        }

        val cursor = cursors[structureType] ?: 0
        cursors[structureType] = cursor + 1
        return matches.getOrNull(cursor)
    }

    private fun mergeAndOrderLocalSections(
        parsedSections: List<LocalParsedSection>,
        sectionOrder: List<String>
    ): List<Section> {
        val mergedSections = parsedSections
            .groupBy { it.info.title to it.info.category }
            .map { (key, group) ->
                val orderedGroup = group.sortedBy { it.firstIndex }
                val first = orderedGroup.first()
                val questions = sortLocalQuestionsAcrossPackages(orderedGroup)
                Section(
                    caption = key.first,
                    category = key.second,
                    typeName = key.first,
                    questions = questions,
                    originalContent = first.originalContent
                )
            }
        return orderLocalSections(mergedSections, sectionOrder)
    }

    private fun sortLocalQuestionsAcrossPackages(
        group: List<LocalParsedSection>
    ): List<Question> {
        val questionsByPackage = group.map { section ->
            val sortedQuestions = sortLocalQuestions(section.questions)
            if (section.templateQuestionNumbers.isNotEmpty()) {
                sortedQuestions.mapIndexed { index, question ->
                    val number = section.templateQuestionNumbers.getOrNull(index)
                    if (number != null) {
                        question.copy(sectionOrder = number, displayOrder = number)
                    } else {
                        question
                    }
                }
            } else {
                sortedQuestions
            }
        }
        val allQuestions = questionsByPackage.flatten()
        val displayOrders = allQuestions.mapNotNull { it.displayOrder }
        val hasDuplicatedDisplayOrder = displayOrders.size != displayOrders.distinct().size

        return if (hasDuplicatedDisplayOrder) {
            questionsByPackage.flatten().mapIndexed { index, question ->
                question.copy(
                    sectionOrder = index + 1,
                    displayOrder = index + 1
                )
            }
        } else {
            sortLocalQuestions(allQuestions)
        }
    }

    private fun sortLocalQuestions(questions: List<Question>): List<Question> {
        val hasAnyDisplayOrder = questions.any { it.displayOrder != null }
        return if (hasAnyDisplayOrder) {
            questions.sortedWith(questionOfficialOrderComparator)
        } else {
            questions.sortedBy { it.order }
        }
    }

    private fun localSectionInfoFromTemplate(
        templateSection: LocalResourceTemplateSection,
        fallback: LocalSectionInfo
    ): LocalSectionInfo {
        val title = templateSection.title.ifBlank { fallback.title }
        val category = when (title) {
            "模仿朗读", "短文朗读", "朗读" -> "read_chapter"
            "听选信息", "听后选择", "听说信息", "3问5答", "听后记录", "填空" -> "simple_expression_ufi"
            "回答问题", "听后回答", "问答信息" -> "simple_expression_ufk"
            "信息转述", "听后转述", "转述" -> "topic"
            "询问信息", "提问" -> "simple_expression_ufj"
            else -> fallback.category
        }
        return LocalSectionInfo(title, category)
    }

    private fun inferLocalSectionInfo(kind: LocalPaperKind, json: JSONObject): LocalSectionInfo {
        val structureType = json.optString("structure_type", "")
        return when (kind) {
            LocalPaperKind.GUANGDONG_HIGH -> when (structureType) {
                StructureType.COLLECTOR_READ -> LocalSectionInfo("模仿朗读", "read_chapter")
                StructureType.COLLECTOR_3Q5A -> LocalSectionInfo("3问5答", "simple_expression_ufi")
                else -> inferGenericSectionInfo(json)
            }
            LocalPaperKind.GUANGDONG_JUNIOR -> when (structureType) {
                StructureType.COLLECTOR_READ -> LocalSectionInfo("模仿朗读", "read_chapter")
                StructureType.COLLECTOR_PICTURE -> LocalSectionInfo("信息转述", "topic")
                StructureType.COLLECTOR_ROLE -> inferGuangdongJuniorRoleSectionInfo(json)
                else -> inferGenericSectionInfo(json)
            }
            LocalPaperKind.BEIJING_JUNIOR -> when (structureType) {
                StructureType.COLLECTOR_CHOOSE -> LocalSectionInfo("听后选择", "simple_expression_ufi")
                StructureType.COLLECTOR_ROLE -> LocalSectionInfo("听后回答", "simple_expression_ufk")
                StructureType.COLLECTOR_PICTURE -> LocalSectionInfo("听后转述", "topic")
                StructureType.COLLECTOR_READ -> LocalSectionInfo("短文朗读", "read_chapter")
                else -> inferGenericSectionInfo(json)
            }
            LocalPaperKind.BEIJING_HIGH -> when (structureType) {
                StructureType.COLLECTOR_CHOOSE -> LocalSectionInfo("听后选择", "simple_expression_ufi")
                StructureType.COLLECTOR_FILL -> LocalSectionInfo("听后记录", "simple_expression_ufi")
                StructureType.COLLECTOR_PICTURE -> LocalSectionInfo("听后转述", "topic")
                StructureType.COLLECTOR_DIALOGUE -> LocalSectionInfo("回答问题", "simple_expression_ufk")
                StructureType.COLLECTOR_READ -> LocalSectionInfo("短文朗读", "read_chapter")
                else -> inferGenericSectionInfo(json)
            }
            LocalPaperKind.GENERIC -> inferGenericSectionInfo(json)
        }
    }

    private fun inferGuangdongJuniorRoleSectionInfo(json: JSONObject): LocalSectionInfo {
        val firstQuestion = json.optJSONObject("info")
            ?.optJSONArray("question")
            ?.optJSONObject(0)
        val ask = firstQuestion?.optString("ask", "").orEmpty()
        val askaudio = firstQuestion?.optString("askaudio", "").orEmpty()
        val hasBr = ask.contains("<br>", ignoreCase = true)
        val hasChinese = containsChinese(ask)
        return when {
            hasBr && askaudio.isNotEmpty() -> LocalSectionInfo("听选信息", "simple_expression_ufi")
            askaudio.isNotEmpty() -> LocalSectionInfo("回答问题", "simple_expression_ufk")
            hasChinese -> LocalSectionInfo("提问", "simple_expression_ufj")
            else -> LocalSectionInfo("听选信息", "simple_expression_ufi")
        }
    }

    private fun inferGenericSectionInfo(json: JSONObject): LocalSectionInfo {
        return when (json.optString("structure_type", "")) {
            StructureType.COLLECTOR_READ -> LocalSectionInfo("朗读", "read_chapter")
            StructureType.COLLECTOR_ROLE -> LocalSectionInfo("问答", "simple_expression_ufk")
            StructureType.COLLECTOR_3Q5A -> LocalSectionInfo("3问5答", "simple_expression_ufi")
            StructureType.COLLECTOR_CHOOSE -> LocalSectionInfo("选择题", "simple_expression_ufi")
            StructureType.COLLECTOR_FILL -> LocalSectionInfo("填空", "simple_expression_ufi")
            StructureType.COLLECTOR_DIALOGUE -> LocalSectionInfo("回答问题", "simple_expression_ufk")
            StructureType.COLLECTOR_PICTURE -> LocalSectionInfo("转述", "topic")
            else -> LocalSectionInfo("未知题型", "unknown")
        }
    }

    private fun orderLocalSections(
        sections: List<Section>,
        sectionOrder: List<String>
    ): List<Section> {
        if (sectionOrder.isEmpty()) return sections
        val orderMap = sectionOrder.withIndex().associate { it.value to it.index }
        return sections.withIndex()
            .sortedWith(
                compareBy<IndexedValue<Section>> {
                    orderMap[it.value.typeName] ?: orderMap[it.value.caption] ?: Int.MAX_VALUE
                }.thenBy { it.index }
            )
            .map { it.value }
    }

    /**
     * Step 5: 扫描 data/ 目录获取常规习题
     * 解析 sectionData → fileName → content.json
     * 跳过 Step 4 中已处理的 content.json（去重）
     */
    private fun parseNormalPapersFromData(
        reader: ETS100FileReader.Reader,
        processedPaths: MutableSet<String>
    ): List<Paper> {
        val dataDirPath = ETS100FileReader.Path.getDataDir()
        val dataFiles = reader.listFiles(dataDirPath)
            .filter { !it.isDirectory && it.size > ETS100FileReader.Path.MIN_FILE_SIZE }
            .sortedByDescending { it.lastModified }

        Log.d(TAG, "parseNormalPapersFromData: 找到 ${dataFiles.size} 个 data 文件")

        val papers = mutableListOf<Paper>()
        for (file in dataFiles) {
            try {
                val paper = parsePaper(reader, file, processedPaths)
                if (paper != null) {
                    papers.add(paper)
                }
            } catch (e: Exception) {
                Log.e(TAG, "parseNormalPapersFromData: Failed to parse paper: ${file.name}", e)
            }
        }

        return papers
    }

    /**
     * 解析单个试卷的JSON数据
     * @param processedPaths 用于去重，跳过Step4中已处理的content.json路径
     */
    private fun parsePaper(reader: ETS100FileReader.Reader, dataFile: ETS100FileReader.FileItem, processedPaths: MutableSet<String>): Paper? {
        val dataFilePath = "${ETS100FileReader.Path.getDataDir()}/${dataFile.name}"
        Log.d(TAG, "parsePaper: 解析试卷文件 ${dataFile.name}")

        val content = reader.readFile(dataFilePath)
        if (content == null) {
            Log.w(TAG, "parsePaper: 无法读取 data 文件: ${dataFile.name}")
            return null
        }

        return try {
            val json = JSONObject(content)

            // 喵~ 判断文件类型：常规习题 or 习题组
            val hasSectionData = json.has("sectionData")
            val hasDataArray = json.has("data")
            val newStruct = json.optInt("new_struct", 0)

            Log.d(TAG, "parsePaper: hasSectionData=$hasSectionData, hasDataArray=$hasDataArray, newStruct=$newStruct")

            return when {
                hasSectionData -> {
                    // 常规习题（旧版格式）
                    Log.d(TAG, "parsePaper: 检测到常规习题格式")
                    parseNormalExercise(reader, json, dataFile, processedPaths)
                }
                hasDataArray && newStruct == 1 -> {
                    // 习题组（新版格式）
                    Log.d(TAG, "parsePaper: 检测到习题组格式")
                    parseExerciseGroup(reader, json, dataFile)
                }
                else -> {
                    Log.w(TAG, "parsePaper: 无法识别的数据格式: ${dataFile.name}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "parsePaper: Failed to parse JSON from ${dataFile.name}: ${e.message}", e)
            null
        }
    }

    /**
     * 解析常规习题（sectionData 格式）
     * 喵~ 这是原来的 parsePaper 逻辑，现在独立出来喵！
     * @param processedPaths 用于去重，跳过Step4中已处理的content.json路径
     */
    private fun parseNormalExercise(reader: ETS100FileReader.Reader, json: JSONObject, dataFile: ETS100FileReader.FileItem, processedPaths: MutableSet<String>): Paper? {
        val paperId = json.optLong("paperId", 0L)
        val sectionData = json.optJSONArray("sectionData")

        Log.d(TAG, "parseNormalExercise: paperId=$paperId, sectionData=${sectionData?.length()}")

        if (paperId == 0L || sectionData == null) {
            Log.w(TAG, "parseNormalExercise: Invalid paper data: paperId=$paperId, sectionData=${sectionData == null}")
            return null
        }

        // 解析每个section
        val sections = mutableListOf<Section>()
        var globalQuestionIndex = 0

        // 遍历sectionData
        for (sectionIndex in 0 until sectionData.length()) {
            val section = sectionData.getJSONObject(sectionIndex)
            val caption = section.optString("caption", "")
            val category = section.optString("category", "")
            val typeName = getTypeName(category)
            val sectionItemData = section.optJSONArray("sectionItemData")

            Log.d(TAG, "parseNormalExercise: section $sectionIndex (caption=$caption, category=$category)")

            if (sectionItemData == null) {
                Log.w(TAG, "parseNormalExercise: section $sectionIndex 缺少 sectionItemData")
                continue
            }

            // 遍历sectionItemData，跳过mainItem
            val processedFileNames = mutableSetOf<String>()
            val sectionQuestions = mutableListOf<Question>()
            var sectionOriginalContent: String? = null

            for (itemIndex in 0 until sectionItemData.length()) {
                val item = sectionItemData.getJSONObject(itemIndex)
                
                // 跳过mainItem=true的项
                val mainItem = item.optBoolean("mainItem", false)
                if (mainItem) {
                    Log.d(TAG, "parseNormalExercise: section $sectionIndex, item $itemIndex 跳过mainItem=true")
                    continue
                }
                
                val fileName = item.optString("fileName", "")
                if (fileName.isEmpty()) {
                    continue
                }

                // 跳过已处理的文件名（同一section内的重复）
                if (fileName in processedFileNames) {
                    Log.d(TAG, "parseNormalExercise: section $sectionIndex, item $itemIndex 跳过重复fileName=$fileName")
                    continue
                }
                processedFileNames.add(fileName)

                // 跳过Step4中已处理的content.json（跨section去重）
                val contentPath = "${ETS100FileReader.Path.getResourceDir()}/$fileName/content.json"
                if (processedPaths.contains(contentPath)) {
                    Log.d(TAG, "parseNormalExercise: section $sectionIndex, item $itemIndex 跳过已处理路径=$contentPath")
                    continue
                }
                processedPaths.add(contentPath)

                Log.d(TAG, "parseNormalExercise: 处理文件 fileName=$fileName")

                // 读取并解析content.json获取题目
                val (questions, originalContent) = readAndParseContent(reader, fileName, globalQuestionIndex, category, typeName, caption)
                
                if (originalContent != null && sectionOriginalContent == null) {
                    sectionOriginalContent = originalContent
                }

                for (q in questions) {
                    sectionQuestions.add(q)
                    globalQuestionIndex++
                }
            }

            if (sectionQuestions.isNotEmpty()) {
                sections.add(Section(
                    caption = caption,
                    category = category,
                    typeName = typeName,
                    questions = sortQuestionsByOfficialOrder(sectionQuestions),
                    originalContent = sectionOriginalContent
                ))
            }
        }

        Log.d(TAG, "parseNormalExercise: 完成 ${dataFile.name} 包含 ${sections.size} 个section共${globalQuestionIndex}道题目")

        val regionLabel = "未知"
        val paperName: String? = null

        return Paper(
            paperId = paperId,
            title = "试卷 #$paperId",
            dataFileName = dataFile.name,
            fileSize = dataFile.size,
            sections = sections,
            downloadTime = dataFile.lastModified,
            regionLabel = regionLabel,
            paperName = paperName
        )
    }

    /**
     * 解析习题组（data[] + struct.contents 格式）
     * 喵~ 这是新版习题组格式，通过 data[] 数组组织题型喵！
     *
     * 习题组结构：
     * - base_url: CDN 资源基础 URL
     * - data[]: 题目组数组
     *   - name: 题目名称
     *   - column: 题目分类
     *   - struct.contents[]: 题型内容数组（st1-st7）
     *     - code_id: 题型代码
     *     - group_name: 题型名称
     *     - audio_url: 音频完整 URL
     *     - url: 资源包名称（需结合 base_url 下载）
     */
    private fun parseExerciseGroup(reader: ETS100FileReader.Reader, json: JSONObject, dataFile: ETS100FileReader.FileItem): Paper? {
        val baseUrl = json.optString("base_url", "")
        val dataArray = json.optJSONArray("data")

        if (dataArray == null || dataArray.length() == 0) {
            Log.w(TAG, "parseExerciseGroup: data 数组为空")
            return null
        }

        Log.d(TAG, "parseExerciseGroup: baseUrl=$baseUrl, dataCount=${dataArray.length()}")

        val sections = mutableListOf<Section>()
        var globalQuestionIndex = 0

        // 喵~ 遍历每个题目组
        for (groupIndex in 0 until dataArray.length()) {
            val group = dataArray.getJSONObject(groupIndex)
            val groupName = group.optString("name", "")
            val groupColumn = group.optString("column", "")
            val groupId = group.optString("id", "")

            Log.d(TAG, "parseExerciseGroup: group $groupIndex (name=$groupName, column=$groupColumn, id=$groupId)")

            val struct = group.optJSONObject("struct")
            if (struct == null) {
                Log.w(TAG, "parseExerciseGroup: group $groupIndex 缺少 struct")
                continue
            }

            val contents = struct.optJSONArray("contents")
            if (contents == null || contents.length() == 0) {
                Log.w(TAG, "parseExerciseGroup: group $groupIndex 缺少 contents")
                continue
            }

            // 喵~ 遍历每个题型 (st1-st7)
            for (contentIndex in 0 until contents.length()) {
                val content = contents.getJSONObject(contentIndex)

                val codeId = content.optString("code_id", "")
                val groupNameType = content.optString("group_name", "")
                val audioUrl = content.optString("audio_url", "")
                val resourceUrl = content.optString("url", "")
                val realId = content.optString("real_id", "")

                Log.d(TAG, "parseExerciseGroup: content $contentIndex (codeId=$codeId, groupName=$groupNameType, url=$resourceUrl)")

                // 喵~ 根据 code_id 获取 category
                val category = exerciseGroupCodeIdToCategory[codeId] ?: "unknown"
                val typeName = exerciseGroupCodeIdMap[codeId] ?: groupNameType

                // 喵~ 尝试从 resource 目录读取 content.json（如果已下载）
                // resourceUrl 是 zip 文件名如 "3ec29bc4bb3a10d089ee6fb0a66c259b.zip"
                // resource 目录名是去掉 .zip 后缀的文件名
                val resourceDirName = resourceUrl.removeSuffix(".zip")
                val (questions, originalContent) = readAndParseContent(
                    reader, resourceDirName, globalQuestionIndex, category, typeName, groupName
                )

                if (questions.isNotEmpty()) {
                    sections.add(Section(
                        caption = groupName,
                        category = category,
                        typeName = typeName,
                        questions = sortQuestionsByOfficialOrder(questions),
                        originalContent = originalContent
                    ))
                    globalQuestionIndex += questions.size
                } else {
                    // 喵~ 如果 resource 目录没有 content.json，创建一个占位题目
                    Log.d(TAG, "parseExerciseGroup: 无法读取 resource，创建占位题目 codeId=$codeId")
                    sections.add(Section(
                        caption = groupName,
                        category = category,
                        typeName = typeName,
                        questions = listOf(Question(
                            order = globalQuestionIndex + 1,
                            sectionOrder = 1,
                            sectionCaption = groupName,
                            typeName = typeName,
                            questionText = "[$typeName] $groupName",
                            answers = emptyList(),
                            originalText = null,
                            category = category
                        )),
                        originalContent = null
                    ))
                    globalQuestionIndex++
                }
            }
        }

        Log.d(TAG, "parseExerciseGroup: 完成 ${dataFile.name} 包含 ${sections.size} 个section共${globalQuestionIndex}道题目")

        // 喵~ 习题组没有 paperId，使用文件名作为标识
        val paperId = dataFile.name.hashCode().toLong().let { if (it < 0) -it else it }

        return Paper(
            paperId = paperId,
            title = dataArray.getJSONObject(0).optString("name", "习题组 #${dataFile.name}"),
            dataFileName = dataFile.name,
            fileSize = dataFile.size,
            sections = sections,
            downloadTime = dataFile.lastModified,
            regionLabel = "习题组",
            paperName = dataArray.getJSONObject(0).optString("name", "").ifEmpty { null }
        )
    }

    /**
     * 读取并解析content.json
     * 
     * @return Pair(questions, originalContent)
     */
    private fun readAndParseContent(
        reader: ETS100FileReader.Reader,
        fileName: String,
        startIndex: Int,
        category: String,
        typeName: String,
        sectionCaption: String
    ): Pair<List<Question>, String?> {
        val resourceDirPath = "${ETS100FileReader.Path.getResourceDir()}/$fileName"
        val contentFilePath = "$resourceDirPath/content.json"

        Log.d(TAG, "readAndParseContent: 读取 $contentFilePath")

        val content = reader.readFile(contentFilePath)
        if (content == null) {
            Log.w(TAG, "readAndParseContent: 无法读取content.json: $contentFilePath")
            return Pair(emptyList(), null)
        }

        return try {
            val json = JSONObject(content)
            parseContentJson(json, startIndex, category, typeName, sectionCaption)
        } catch (e: Exception) {
            Log.e(TAG, "readAndParseContent: Failed to parse: ${e.message}", e)
            Pair(emptyList(), null)
        }
    }

    /**
     * 解析content.json JSON
     * 
     * 喵~ 这个方法根据 structure_type 分支处理，正确解析 3 种结构类型：
     * - collector.role: 问答题（听说/问答/询问），答案在 info.question[].std[].value
     * - collector.picture: 信息转述，答案在 info.std[].value，原文在 info.value
     * - collector.read: 模仿朗读，无答案，只有原文在 info.value
     * 
     * 5 种 category 对应关系：
     * - read_chapter → collector.read
     * - simple_expression_ufi → collector.role
     * - simple_expression_ufk → collector.role
     * - topic → collector.picture
     * - simple_expression_ufj → collector.role
     */
    internal fun parseContentJson(
        json: JSONObject,
        startIndex: Int,
        category: String,
        typeName: String,
        sectionCaption: String
    ): Pair<List<Question>, String?> {
        val questions = mutableListOf<Question>()
        var originalContent: String? = null

        val infoObj = json.optJSONObject("info")
        if (infoObj == null) {
            Log.w(TAG, "parseContentJson: infoObj is null")
            return Pair(emptyList(), null)
        }

        // 获取 structure_type，优先用 json 中的，否则用 category 映射
        val structureType = getStructureType(category, json)
        
        Log.d(TAG, "parseContentJson: category=$category, structureType=$structureType")

        // Step 1: 获取 info.value 作为原文（所有类型都可能用到）
        val infoValue = infoObj.optString("value", "")
        if (infoValue.isNotEmpty()) {
            originalContent = infoValue.replace(Regex("<[^>]*>"), "").trim()
        }

        // Step 2: 根据 structure_type 分支处理
        when (structureType) {
            StructureType.COLLECTOR_READ -> {
                // 模仿朗读：无答案，只有原文
                // read_chapter 类型对应这个
                questions.add(Question(
                    order = startIndex + 1,
                    sectionOrder = 1,
                    sectionCaption = sectionCaption,
                    typeName = typeName,
                    questionText = "模仿朗读原文",
                    answers = emptyList<String>(),  // 喵~ 模仿朗读没有答案喵！
                    originalText = originalContent,
                    category = category
                ))
                return Pair(questions, originalContent)
            }

            StructureType.COLLECTOR_PICTURE -> {
                // 信息转述：答案在 info.std[].value，原文在 info.value
                // topic 类型对应这个
                val stdArray = infoObj.optJSONArray("std")
                val answers = if (stdArray != null && stdArray.length() > 0) {
                    (0 until stdArray.length()).map { idx ->
                        stdArray.getJSONObject(idx).optString("value", "")
                            .replace(Regex("<[^>]*>"), "").trim()
                    }
                } else {
                    listOf("暂无标准答案")
                }
                
                // info.topic 是题目主题
                val topicTitle = infoObj.optString("topic", "信息转述")
                
                questions.add(Question(
                    order = startIndex + 1,
                    sectionOrder = 1,
                    sectionCaption = sectionCaption,
                    typeName = typeName,
                    questionText = topicTitle,
                    answers = answers,
                    originalText = originalContent,  // 喵~ 原文是 info.value 喵！
                    category = category,
                    displayOrder = extractQuestionNumber(infoObj, "topic", "value")
                ))
                return Pair(questions, originalContent)
            }

            StructureType.COLLECTOR_ROLE -> {
                // 问答题：答案在 info.question[].std[].value
                // simple_expression_ufi, simple_expression_ufk, simple_expression_ufj 对应这个
                val questionsArray = infoObj.optJSONArray("question") ?: infoObj.optJSONArray("questions")
                if (questionsArray != null && questionsArray.length() > 0) {
                    for (i in 0 until questionsArray.length()) {
                        val q = questionsArray.getJSONObject(i)
                        
                        val askText = q.optString("ask", "").let {
                            if (it.contains(" ")) it.substringAfter(" ") else it
                        }
                        val questionText = askText.ifEmpty { q.optString("question", q.optString("text", "")) }
                        val displayOrder = extractQuestionNumber(q, "ask", "question", "text")
                        
                        val stdArray = q.optJSONArray("std")
                        val answers = if (stdArray != null && stdArray.length() > 0) {
                            (0 until stdArray.length()).map { idx ->
                                stdArray.getJSONObject(idx).optString("value", "")
                                    .replace(Regex("<[^>]*>"), "")
                                    .replace("</br>", "")
                                    .trim()
                            }
                        } else {
                            listOf("暂无标准答案")
                        }

                        // 喵~ 这些类型不需要显示原文
                        val questionOriginalText = when (category) {
                            "read_chapter" -> null
                            "simple_expression_ufj" -> null
                            else -> originalContent
                        }

                        questions.add(Question(
                            order = startIndex + i + 1,
                            sectionOrder = i + 1,
                            sectionCaption = sectionCaption,
                            typeName = typeName,
                            questionText = questionText,
                            answers = answers,
                            originalText = questionOriginalText,
                            category = category,
                            displayOrder = displayOrder
                        ))
                    }
                    return Pair(sortQuestionsByOfficialOrder(questions), originalContent)
                }

                // 如果没有 question 数组但有 value，当作原文处理
                if (originalContent != null) {
                    questions.add(Question(
                        order = startIndex + 1,
                        sectionOrder = 1,
                        sectionCaption = sectionCaption,
                        typeName = typeName,
                        questionText = "阅读理解原文",
                        answers = listOf(originalContent),
                        originalText = originalContent,
                        category = category
                    ))
                    return Pair(questions, originalContent)
                }
            }

            StructureType.COLLECTOR_3Q5A -> {
                // 广东高中：3问5答，每题有多个参考答案
                // 喵~ collector.3q5a 和 collector.role 结构相同，但每题有多个 std 答案喵！
                val questionsArray = infoObj.optJSONArray("question") ?: infoObj.optJSONArray("questions")
                if (questionsArray != null && questionsArray.length() > 0) {
                    for (i in 0 until questionsArray.length()) {
                        val q = questionsArray.getJSONObject(i)
                        val askText = cleanText(q.optString("ask", ""))
                        val questionText = askText.ifEmpty { q.optString("question", q.optString("text", "")) }
                        val displayOrder = extractQuestionNumber(q, "ask", "question", "text")
                        
                        val stdArray = q.optJSONArray("std")
                        val answers = if (stdArray != null && stdArray.length() > 0) {
                            (0 until stdArray.length()).map { idx ->
                                cleanText(stdArray.getJSONObject(idx).optString("value", ""))
                            }
                        } else {
                            listOf("暂无标准答案")
                        }

                        questions.add(Question(
                            order = startIndex + i + 1,
                            sectionOrder = i + 1,
                            sectionCaption = sectionCaption,
                            typeName = typeName,
                            questionText = questionText,
                            answers = answers,
                            originalText = originalContent,
                            category = category,
                            displayOrder = displayOrder
                        ))
                    }
                    return Pair(sortQuestionsByOfficialOrder(questions), originalContent)
                }
            }

            StructureType.COLLECTOR_CHOOSE -> {
                // 北京：听后选择题
                // 喵~ 答案在 info.xtlist[].answer，选项在 info.xtlist[].xxlist 喵！
                val xtlist = infoObj.optJSONArray("xtlist")
                if (xtlist != null && xtlist.length() > 0) {
                    for (i in 0 until xtlist.length()) {
                        val item = xtlist.getJSONObject(i)
                        val displayOrder = extractChooseQuestionNumber(item)
                        val questionText = cleanQuestion(item.optString("xt_nr", ""))
                        val answer = item.optString("answer", "")
                        
                        val xxlist = item.optJSONArray("xxlist")
                        val options = if (xxlist != null && xxlist.length() > 0) {
                            (0 until xxlist.length()).map { j ->
                                val opt = xxlist.getJSONObject(j)
                                "${opt.optString("xx_mc")}. ${cleanText(opt.optString("xx_nr"))}"
                            }
                        } else {
                            emptyList()
                        }
                        
                        // 组装答案：选项 + 正确答案
                        val answerText = if (options.isNotEmpty()) {
                            options.joinToString("\n") + "\n正确答案: $answer"
                        } else {
                            "正确答案: $answer"
                        }

                        questions.add(Question(
                            order = startIndex + i + 1,
                            sectionOrder = i + 1,
                            sectionCaption = sectionCaption,
                            typeName = typeName,
                            questionText = questionText,
                            answers = listOf(answerText),
                            originalText = originalContent,
                            category = category,
                            displayOrder = displayOrder
                        ))
                    }
                    return Pair(sortQuestionsByOfficialOrder(questions), originalContent)
                }
            }

            StructureType.COLLECTOR_FILL -> {
                // 北京高中：听后记录/填空题
                // 喵~ 答案在 info.std[].value，题号在 info.std[].xth 喵！
                val stdArray = infoObj.optJSONArray("std")
                if (stdArray != null && stdArray.length() > 0) {
                    for (i in 0 until stdArray.length()) {
                        val item = stdArray.getJSONObject(i)
                        val number = item.optString("xth", "")
                        val displayOrder = extractQuestionNumber(item, "xth", "value")
                        val answer = cleanText(item.optString("value", ""))
                        
                        questions.add(Question(
                            order = startIndex + i + 1,
                            sectionOrder = i + 1,
                            sectionCaption = sectionCaption,
                            typeName = typeName,
                            questionText = "第${number}题",
                            answers = listOf(answer),
                            originalText = null,
                            category = category,
                            displayOrder = displayOrder
                        ))
                    }
                    return Pair(sortQuestionsByOfficialOrder(questions), originalContent)
                }
            }

            StructureType.COLLECTOR_DIALOGUE -> {
                // 北京高中：回答问题
                // 喵~ 结构同 collector.role，答案在 info.question[].std[].value 喵！
                val questionsArray = infoObj.optJSONArray("question") ?: infoObj.optJSONArray("questions")
                if (questionsArray != null && questionsArray.length() > 0) {
                    for (i in 0 until questionsArray.length()) {
                        val q = questionsArray.getJSONObject(i)
                        val askText = cleanText(q.optString("ask", ""))
                        val questionText = askText.ifEmpty { q.optString("question", q.optString("text", "")) }
                        val displayOrder = extractQuestionNumber(q, "ask", "question", "text")
                        
                        val stdArray = q.optJSONArray("std")
                        val answers = if (stdArray != null && stdArray.length() > 0) {
                            (0 until stdArray.length()).map { idx ->
                                cleanText(stdArray.getJSONObject(idx).optString("value", ""))
                            }
                        } else {
                            listOf("暂无标准答案")
                        }

                        questions.add(Question(
                            order = startIndex + i + 1,
                            sectionOrder = i + 1,
                            sectionCaption = sectionCaption,
                            typeName = typeName,
                            questionText = questionText,
                            answers = answers,
                            originalText = originalContent,
                            category = category,
                            displayOrder = displayOrder
                        ))
                    }
                    return Pair(sortQuestionsByOfficialOrder(questions), originalContent)
                }
            }

            else -> {
                // 未知 structure_type，尝试兼容处理
                Log.w(TAG, "parseContentJson: unknown structureType=$structureType, try fallback")
                
                // 尝试作为 collector.role 处理（有 question 数组）
                val questionsArray = infoObj.optJSONArray("question") ?: infoObj.optJSONArray("questions")
                if (questionsArray != null && questionsArray.length() > 0) {
                    for (i in 0 until questionsArray.length()) {
                        val q = questionsArray.getJSONObject(i)
                        val askText = q.optString("ask", "").let {
                            if (it.contains(" ")) it.substringAfter(" ") else it
                        }
                        val questionText = askText.ifEmpty { q.optString("question", q.optString("text", "")) }
                        val displayOrder = extractQuestionNumber(q, "ask", "question", "text")
                        
                        val stdArray = q.optJSONArray("std")
                        val answers = if (stdArray != null && stdArray.length() > 0) {
                            (0 until stdArray.length()).map { idx ->
                                stdArray.getJSONObject(idx).optString("value", "")
                                    .replace(Regex("<[^>]*>"), "")
                                    .replace("</br>", "")
                                    .trim()
                            }
                        } else {
                            listOf("暂无标准答案")
                        }

                        questions.add(Question(
                            order = startIndex + i + 1,
                            sectionOrder = i + 1,
                            sectionCaption = sectionCaption,
                            typeName = typeName,
                            questionText = questionText,
                            answers = answers,
                            originalText = null,
                            category = category,
                            displayOrder = displayOrder
                        ))
                    }
                    return Pair(sortQuestionsByOfficialOrder(questions), originalContent)
                }

                // 尝试作为 collector.picture 处理（有 std 数组）
                val stdArray = infoObj.optJSONArray("std")
                if (stdArray != null && stdArray.length() > 0) {
                    val answers = (0 until stdArray.length()).map { idx ->
                        stdArray.getJSONObject(idx).optString("value", "")
                            .replace(Regex("<[^>]*>"), "").trim()
                    }
                    val topicTitle = infoObj.optString("topic", "信息转述")
                    
                    questions.add(Question(
                        order = startIndex + 1,
                        sectionOrder = 1,
                        sectionCaption = sectionCaption,
                        typeName = typeName,
                        questionText = topicTitle,
                        answers = answers,
                        originalText = originalContent,
                        category = category,
                        displayOrder = extractQuestionNumber(infoObj, "topic", "value")
                    ))
                    return Pair(questions, originalContent)
                }
            }
        }

        Log.w(TAG, "parseContentJson: 无法解析JSON内容 structureType=$structureType")
        return Pair(emptyList(), null)
    }

    /**
     * 读取单个试卷
     */
    fun readSinglePaper(context: Context, mode: ActivationMode, dataFileName: String): Paper? {
        if (!ETS100FileReader.isModeAvailable(mode, context)) {
            return null
        }

        val reader = try {
            ETS100FileReader.getReader(mode, context)
        } catch (e: Exception) {
            return null
        }

        val file = ETS100FileReader.FileItem(
            name = dataFileName,
            path = "${ETS100FileReader.Path.getDataDir()}/$dataFileName",
            isDirectory = false,
            size = reader.getFileSize("${ETS100FileReader.Path.getDataDir()}/$dataFileName"),
            lastModified = 0L
        )

        return parsePaper(reader, file, mutableSetOf())
    }

    /**
     * 检查是否有可用的试卷
     */
    fun hasAvailablePapers(context: Context, mode: ActivationMode): Boolean {
        if (!ETS100FileReader.isModeAvailable(mode, context)) {
            return false
        }

        val reader = try {
            ETS100FileReader.getReader(mode, context)
        } catch (e: Exception) {
            return false
        }

        val dataDirPath = ETS100FileReader.Path.getDataDir()
        val files = reader.listFiles(dataDirPath)
            .filter { !it.isDirectory && it.size > ETS100FileReader.Path.MIN_FILE_SIZE }

        return files.isNotEmpty()
    }

    /**
     * 获取试卷数量
     */
    fun getPaperCount(context: Context, mode: ActivationMode): Int {
        if (!ETS100FileReader.isModeAvailable(mode, context)) {
            return 0
        }

        val reader = try {
            ETS100FileReader.getReader(mode, context)
        } catch (e: Exception) {
            return 0
        }

        val dataDirPath = ETS100FileReader.Path.getDataDir()
        return reader.listFiles(dataDirPath)
            .filter { !it.isDirectory && it.size > ETS100FileReader.Path.MIN_FILE_SIZE }
            .size
    }
    
    /**
     * 将Paper对象转换为Question列表
     * 用于兼容旧的Question数据结构
     */
    fun Paper.toLegacyQuestions(): List<Question> {
        val result = mutableListOf<Question>()
        for (section in sections) {
            result.addAll(section.questions)
        }
        return result
    }
}
