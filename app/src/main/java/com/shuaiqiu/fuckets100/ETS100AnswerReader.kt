package com.shuaiqiu.fuckets100

import android.content.Context
import android.util.Log
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
        val paperName: String? = null  // 喵~ 添加试卷名称（来自resource索引）
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
        val content: AnswerContent = AnswerContent.Reading("")  // 用于UI显示
    ) {
        // 兼容性别名 - ReadScreen 使用 question.question
        val question: String get() = questionText
        val answer: String get() = answers.firstOrNull() ?: ""
        val answerList: List<String> get() = answers
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
        return text
            .replace(Regex("ets_th\\d+\\s*"), "")  // 移除 ets_th 前缀
            .replace("</p><p>", "\n")                // 替换 </p><p> 为换行
            .replace(Regex("<[^>]+>"), "")            // 移除 HTML 标签
            .replace("\u200B", "")                    // 移除零宽空格
            .trim()
    }

    /**
     * 题目文本清洗（专门针对 xt_nr 字段）
     * 喵~ 北京选择题的题目文本需要特殊处理喵！
     */
    private fun cleanQuestion(text: String): String {
        if (text.isEmpty()) return ""
        return text
            .replace(Regex("ets_th\\d+\\s*"), "")  // 移除 ets_th 前缀
            .replace(Regex("<[^>]+>"), "")            // 移除 HTML 标签
            .replace("\u200B", "")                    // 移除零宽空格
            .trim()
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
    private fun getStructureType(category: String, json: JSONObject): String {
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

        // Step 1: 扫描 resource/ 目录
        // 获取所有含 content.json 的子目录，按修改时间降序排序
        val resourceFolders = scanResourceFolders(reader)
        Log.i(TAG, "readPapers: Step1 扫描 resource/ 获取 ${resourceFolders.size} 个文件夹")

        // Step 2: 按时间分组（相邻文件夹修改时间间隔 ≤ 0.5秒 → 同一组）
        val groupedFolders = groupByTime(resourceFolders, thresholdMs = 2000)
        Log.i(TAG, "readPapers: Step2 时间分组得到 ${groupedFolders.size} 组")

        // 用于 Step 5 去重：记录已处理的 content.json 路径
        val processedContentPaths = mutableSetOf<String>()
        val exerciseGroupPapers = mutableListOf<Paper>()

        // Step 3 & 4: 根据文件夹数量路由，遍历每组的 content.json
        for ((groupIndex, folderGroup) in groupedFolders.withIndex()) {
            val folderCount = folderGroup.size
            
            // 宝贝添加详细日志：显示每组的文件夹名称和时间
            val folderNames = folderGroup.joinToString(", ") { "${it.name}(${it.lastModified})" }
            Log.d(TAG, "╔═══ Step3&4 第 ${groupIndex + 1} 组路由日志 ═══")
            Log.d(TAG, "║ 文件夹数量: $folderCount")
            Log.d(TAG, "║ 文件夹列表: $folderNames")
            
            // 根据文件夹数量路由
            val papers = when (folderCount) {
                3 -> {
                    Log.d(TAG, "║ 路由: 广东高中解析器 (3个文件夹)")
                    parseGuangdongHighPapers(reader, folderGroup, groupIndex, processedContentPaths)
                }
                7 -> {
                    Log.d(TAG, "║ 路由: 广东初中解析器 (7个文件夹)")
                    parseGuangdongJuniorPapers(reader, folderGroup, groupIndex, processedContentPaths)
                }
                10 -> {
                    Log.d(TAG, "║ 路由: 北京初中解析器 (10个文件夹)")
                    parseBeijingJuniorPapers(reader, folderGroup, groupIndex, processedContentPaths)
                }
                13 -> {
                    Log.d(TAG, "║ 路由: 北京高中解析器 (13个文件夹)")
                    parseBeijingHighPapers(reader, folderGroup, groupIndex, processedContentPaths)
                }
                else -> {
                    Log.d(TAG, "║ 路由: 通用解析器 (${folderCount}个文件夹，不匹配预设)")
                    parseGenericPapers(reader, folderGroup, groupIndex, processedContentPaths)
                }
            }
            Log.d(TAG, "║ 解析得到 ${papers.size} 份试卷")
            Log.d(TAG, "╚═══ Step3&4 路由日志结束 ═══")
            
            exerciseGroupPapers.addAll(papers)
        }

        Log.i(TAG, "readPapers: 完成，共 ${exerciseGroupPapers.size} 份试卷")

        return exerciseGroupPapers
    }

    /**
     * Step 1: 扫描 resource/ 目录
     * 获取所有含 content.json 的子目录，按修改时间降序排序
     *
     * 喵~ 按照文档方式，直接对每个目录调用 getFileModifiedTime 获取时间！
     */
    private fun scanResourceFolders(reader: ETS100FileReader.Reader): List<ETS100FileReader.FileItem> {
        val resourceDir = ETS100FileReader.Path.getResourceDir()
        val allItems = reader.listFiles(resourceDir)

        // 筛选出是目录的项，然后检查 content.json 并获取时间
        val contentFolders = allItems
            .filter { it.isDirectory }
            .mapNotNull { item ->
                val folderPath = "$resourceDir/${item.name}"
                val contentJsonPath = "$folderPath/content.json"
                
                // 检查是否有 content.json
                if (reader.getFileSize(contentJsonPath) <= 0) {
                    return@mapNotNull null
                }
                
                // 喵~ 直接调用 getFileModifiedTime 获取目录修改时间！
                val lastModified = reader.getFileModifiedTime(folderPath)
                
                item.copy(lastModified = lastModified)
            }
            .sortedByDescending { it.lastModified }

        Log.d(TAG, "scanResourceFolders: 找到 ${contentFolders.size} 个含 content.json 的文件夹")
        return contentFolders
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

    /**
     * 广东高中解析器（3个文件夹）
     * 遍历每个 content.json：
     * - collector.3q5a → 遍历 info.question[]，每题从 std[] 提取多个参考答案
     */
    private fun parseGuangdongHighPapers(
        reader: ETS100FileReader.Reader,
        folders: List<ETS100FileReader.FileItem>,
        groupIndex: Int,
        processedPaths: MutableSet<String>
    ): List<Paper> {
        Log.d(TAG, "parseGuangdongHighPapers: 第 $groupIndex 组，${folders.size} 个文件夹")
        val sections = mutableListOf<Section>()
        var questionIndex = 0

        for (folder in folders) {
            val contentPath = "${ETS100FileReader.Path.getResourceDir()}/${folder.name}/content.json"
            processedPaths.add(contentPath)

            val content = reader.readFile(contentPath) ?: continue
            val json = JSONObject(content)
            val (questions, _) = parseContentJson(json, questionIndex, "simple_expression_ufi", "3问5答", "广东高中练习")

            if (questions.isNotEmpty()) {
                sections.add(Section(
                    caption = "广东高中练习",
                    category = "simple_expression_ufi",
                    typeName = "3问5答",
                    questions = questions,
                    originalContent = null
                ))
                questionIndex += questions.size
            }
        }

        if (sections.isEmpty()) return emptyList()

        val paperId = folders.first().name.hashCode().toLong()
        return listOf(Paper(
            paperId = paperId,
            title = "广东高中 #${groupIndex + 1}",
            dataFileName = folders.first().name,
            fileSize = folders.sumOf { it.size },
            sections = sections,
            downloadTime = folders.first().lastModified,
            regionLabel = "广东高中",
            paperName = null
        ))
    }

    /**
     * 广东初中解析器（7个文件夹）
     * 遍历每个 content.json：
     * - collector.role → 遍历 info.question[]，按 askaudio/中文条件分为听选信息、回答问题、提问
     * - collector.picture → 从 info.std[].value 提取信息转述答案
     * - collector.read → 跳过
     * 输出顺序：听选信息 → 回答问题 → 信息转述 → 提问
     */
    private fun parseGuangdongJuniorPapers(
        reader: ETS100FileReader.Reader,
        folders: List<ETS100FileReader.FileItem>,
        groupIndex: Int,
        processedPaths: MutableSet<String>
    ): List<Paper> {
        Log.d(TAG, "parseGuangdongJuniorPapers: 第 $groupIndex 组，${folders.size} 个文件夹")
        val listeningSelectQuestions = mutableListOf<Question>()  // 听选信息
        val answerQuestions = mutableListOf<Question>()          // 回答问题
        val pictureQuestions = mutableListOf<Question>()         // 信息转述
        val askQuestions = mutableListOf<Question>()            // 提问
        var questionIndex = 0

        for (folder in folders) {
            val contentPath = "${ETS100FileReader.Path.getResourceDir()}/${folder.name}/content.json"
            processedPaths.add(contentPath)

            val content = reader.readFile(contentPath) ?: continue
            val json = JSONObject(content)
            val structureType = json.optString("structure_type", "")

            when (structureType) {
                "collector.role" -> {
                    // 需要进一步根据 askaudio/中文条件细分
                    val infoObj = json.optJSONObject("info") ?: continue
                    val questionsArray = infoObj.optJSONArray("question") ?: continue

                    for (i in 0 until questionsArray.length()) {
                        val q = questionsArray.getJSONObject(i)
                        val ask = q.optString("ask", "")
                        val askaudio = q.optString("askaudio", "")
                        val hasBr = ask.contains("<br>")
                        val hasChinese = containsChinese(ask)

                        val stdArray = q.optJSONArray("std")
                        val answers = if (stdArray != null && stdArray.length() > 0) {
                            (0 until stdArray.length()).map { idx ->
                                cleanText(stdArray.getJSONObject(idx).optString("value", ""))
                            }
                        } else listOf("暂无标准答案")

                        val questionText = ask.substringAfter(" ").ifEmpty { ask }

                        when {
                            hasBr && askaudio.isNotEmpty() -> {
                                // 听选信息
                                listeningSelectQuestions.add(Question(
                                    order = questionIndex + 1,
                                    sectionOrder = listeningSelectQuestions.size + 1,
                                    sectionCaption = "听选信息",
                                    typeName = "听选信息",
                                    questionText = questionText,
                                    answers = answers,
                                    originalText = null,
                                    category = "simple_expression_ufi"
                                ))
                            }
                            askaudio.isNotEmpty() && !hasBr -> {
                                // 回答问题
                                answerQuestions.add(Question(
                                    order = questionIndex + 1,
                                    sectionOrder = answerQuestions.size + 1,
                                    sectionCaption = "回答问题",
                                    typeName = "回答问题",
                                    questionText = questionText,
                                    answers = answers,
                                    originalText = null,
                                    category = "simple_expression_ufk"
                                ))
                            }
                            askaudio.isEmpty() && hasChinese -> {
                                // 提问
                                askQuestions.add(Question(
                                    order = questionIndex + 1,
                                    sectionOrder = askQuestions.size + 1,
                                    sectionCaption = "提问",
                                    typeName = "提问",
                                    questionText = questionText,
                                    answers = answers,
                                    originalText = null,
                                    category = "simple_expression_ufj"
                                ))
                            }
                        }
                        questionIndex++
                    }
                }
                "collector.picture" -> {
                    // 信息转述
                    val infoObj = json.optJSONObject("info") ?: continue
                    val stdArray = infoObj.optJSONArray("std")
                    val answers = if (stdArray != null && stdArray.length() > 0) {
                        (0 until stdArray.length()).map { idx ->
                            cleanText(stdArray.getJSONObject(idx).optString("value", ""))
                        }
                    } else listOf("暂无标准答案")
                    val topicTitle = infoObj.optString("topic", "信息转述")

                    pictureQuestions.add(Question(
                        order = questionIndex + 1,
                        sectionOrder = pictureQuestions.size + 1,
                        sectionCaption = "信息转述",
                        typeName = "信息转述",
                        questionText = topicTitle,
                        answers = answers,
                        originalText = infoObj.optString("value", ""),
                        category = "topic"
                    ))
                    questionIndex++
                }
                // collector.read 跳过
            }
        }

        // 按文档顺序组装：听选信息 → 回答问题 → 信息转述 → 提问
        val sections = mutableListOf<Section>()
        if (listeningSelectQuestions.isNotEmpty()) {
            sections.add(Section("听选信息", "simple_expression_ufi", "听选信息", listeningSelectQuestions, null))
        }
        if (answerQuestions.isNotEmpty()) {
            sections.add(Section("回答问题", "simple_expression_ufk", "回答问题", answerQuestions, null))
        }
        if (pictureQuestions.isNotEmpty()) {
            sections.add(Section("信息转述", "topic", "信息转述", pictureQuestions, null))
        }
        if (askQuestions.isNotEmpty()) {
            sections.add(Section("提问", "simple_expression_ufj", "提问", askQuestions, null))
        }

        if (sections.isEmpty()) return emptyList()

        val paperId = folders.first().name.hashCode().toLong()
        return listOf(Paper(
            paperId = paperId,
            title = "广东初中 #${groupIndex + 1}",
            dataFileName = folders.first().name,
            fileSize = folders.sumOf { it.size },
            sections = sections,
            downloadTime = folders.first().lastModified,
            regionLabel = "广东初中",
            paperName = null
        ))
    }

    /**
     * 北京初中解析器（10个文件夹）
     * 遍历每个 content.json：
     * - collector.choose → 从 info.xtlist[] 提取题目、选项、答案
     * - collector.role → 从 info.question[].std[].value 提取听后回答
     * - collector.picture → 从 info.std[].value 提取听后转述
     * 输出顺序：听后选择 → 听后回答 → 听后转述
     */
    private fun parseBeijingJuniorPapers(
        reader: ETS100FileReader.Reader,
        folders: List<ETS100FileReader.FileItem>,
        groupIndex: Int,
        processedPaths: MutableSet<String>
    ): List<Paper> {
        Log.d(TAG, "parseBeijingJuniorPapers: 第 $groupIndex 组，${folders.size} 个文件夹")
        val chooseQuestions = mutableListOf<Question>()
        val roleQuestions = mutableListOf<Question>()
        val pictureQuestions = mutableListOf<Question>()
        var questionIndex = 0

        for (folder in folders) {
            val contentPath = "${ETS100FileReader.Path.getResourceDir()}/${folder.name}/content.json"
            processedPaths.add(contentPath)

            val content = reader.readFile(contentPath) ?: continue
            val json = JSONObject(content)
            val structureType = json.optString("structure_type", "")

            when (structureType) {
                "collector.choose" -> {
                    val infoObj = json.optJSONObject("info") ?: continue
                    val xtlist = infoObj.optJSONArray("xtlist") ?: continue

                    for (i in 0 until xtlist.length()) {
                        val item = xtlist.getJSONObject(i)
                        val questionText = cleanQuestion(item.optString("xt_nr", ""))
                        val answer = item.optString("answer", "")
                        val xxlist = item.optJSONArray("xxlist")
                        val options = if (xxlist != null && xxlist.length() > 0) {
                            (0 until xxlist.length()).map { j ->
                                val opt = xxlist.getJSONObject(j)
                                "${opt.optString("xx_mc")}. ${cleanText(opt.optString("xx_nr"))}"
                            }
                        } else emptyList()

                        val answerText = if (options.isNotEmpty()) {
                            options.joinToString("\n") + "\n正确答案: $answer"
                        } else "正确答案: $answer"

                        chooseQuestions.add(Question(
                            order = questionIndex + 1,
                            sectionOrder = chooseQuestions.size + 1,
                            sectionCaption = "听后选择",
                            typeName = "听后选择",
                            questionText = questionText,
                            answers = listOf(answerText),
                            originalText = null,
                            category = "simple_expression_ufi"
                        ))
                        questionIndex++
                    }
                }
                "collector.role" -> {
                    val infoObj = json.optJSONObject("info") ?: continue
                    val questionsArray = infoObj.optJSONArray("question") ?: continue

                    for (i in 0 until questionsArray.length()) {
                        val q = questionsArray.getJSONObject(i)
                        val askText = cleanText(q.optString("ask", ""))
                        val stdArray = q.optJSONArray("std")
                        val answers = if (stdArray != null && stdArray.length() > 0) {
                            (0 until stdArray.length()).map { idx ->
                                cleanText(stdArray.getJSONObject(idx).optString("value", ""))
                            }
                        } else listOf("暂无标准答案")

                        roleQuestions.add(Question(
                            order = questionIndex + 1,
                            sectionOrder = roleQuestions.size + 1,
                            sectionCaption = "听后回答",
                            typeName = "听后回答",
                            questionText = askText,
                            answers = answers,
                            originalText = null,
                            category = "simple_expression_ufk"
                        ))
                        questionIndex++
                    }
                }
                "collector.picture" -> {
                    val infoObj = json.optJSONObject("info") ?: continue
                    val stdArray = infoObj.optJSONArray("std")
                    val answers = if (stdArray != null && stdArray.length() > 0) {
                        (0 until stdArray.length()).map { idx ->
                            cleanText(stdArray.getJSONObject(idx).optString("value", ""))
                        }
                    } else listOf("暂无标准答案")
                    val topicTitle = infoObj.optString("topic", "听后转述")

                    pictureQuestions.add(Question(
                        order = questionIndex + 1,
                        sectionOrder = pictureQuestions.size + 1,
                        sectionCaption = "听后转述",
                        typeName = "听后转述",
                        questionText = topicTitle,
                        answers = answers,
                        originalText = infoObj.optString("value", ""),
                        category = "topic"
                    ))
                    questionIndex++
                }
            }
        }

        // 按文档顺序组装：听后选择 → 听后回答 → 听后转述
        val sections = mutableListOf<Section>()
        if (chooseQuestions.isNotEmpty()) {
            sections.add(Section("听后选择", "simple_expression_ufi", "听后选择", chooseQuestions, null))
        }
        if (roleQuestions.isNotEmpty()) {
            sections.add(Section("听后回答", "simple_expression_ufk", "听后回答", roleQuestions, null))
        }
        if (pictureQuestions.isNotEmpty()) {
            sections.add(Section("听后转述", "topic", "听后转述", pictureQuestions, null))
        }

        if (sections.isEmpty()) return emptyList()

        val paperId = folders.first().name.hashCode().toLong()
        return listOf(Paper(
            paperId = paperId,
            title = "北京初中 #${groupIndex + 1}",
            dataFileName = folders.first().name,
            fileSize = folders.sumOf { it.size },
            sections = sections,
            downloadTime = folders.first().lastModified,
            regionLabel = "北京初中",
            paperName = null
        ))
    }

    /**
     * 北京高中解析器（13个文件夹）
     * 遍历每个 content.json：
     * - collector.choose → 听后选择
     * - collector.fill → 从 info.std[] 提取填空答案
     * - collector.picture → 听后转述
     * - collector.dialogue → 从 info.question[].std[].value 提取回答问题
     * - collector.read → 跳过
     * 输出顺序：听后选择 → 听后记录 → 听后转述 → 回答问题
     */
    private fun parseBeijingHighPapers(
        reader: ETS100FileReader.Reader,
        folders: List<ETS100FileReader.FileItem>,
        groupIndex: Int,
        processedPaths: MutableSet<String>
    ): List<Paper> {
        Log.d(TAG, "parseBeijingHighPapers: 第 $groupIndex 组，${folders.size} 个文件夹")
        val chooseQuestions = mutableListOf<Question>()
        val fillQuestions = mutableListOf<Question>()
        val pictureQuestions = mutableListOf<Question>()
        val dialogueQuestions = mutableListOf<Question>()
        var questionIndex = 0

        for (folder in folders) {
            val contentPath = "${ETS100FileReader.Path.getResourceDir()}/${folder.name}/content.json"
            processedPaths.add(contentPath)

            val content = reader.readFile(contentPath) ?: continue
            val json = JSONObject(content)
            val structureType = json.optString("structure_type", "")

            when (structureType) {
                "collector.choose" -> {
                    val infoObj = json.optJSONObject("info") ?: continue
                    val xtlist = infoObj.optJSONArray("xtlist") ?: continue

                    for (i in 0 until xtlist.length()) {
                        val item = xtlist.getJSONObject(i)
                        val questionText = cleanQuestion(item.optString("xt_nr", ""))
                        val answer = item.optString("answer", "")
                        val xxlist = item.optJSONArray("xxlist")
                        val options = if (xxlist != null && xxlist.length() > 0) {
                            (0 until xxlist.length()).map { j ->
                                val opt = xxlist.getJSONObject(j)
                                "${opt.optString("xx_mc")}. ${cleanText(opt.optString("xx_nr"))}"
                            }
                        } else emptyList()

                        val answerText = if (options.isNotEmpty()) {
                            options.joinToString("\n") + "\n正确答案: $answer"
                        } else "正确答案: $answer"

                        chooseQuestions.add(Question(
                            order = questionIndex + 1,
                            sectionOrder = chooseQuestions.size + 1,
                            sectionCaption = "听后选择",
                            typeName = "听后选择",
                            questionText = questionText,
                            answers = listOf(answerText),
                            originalText = null,
                            category = "simple_expression_ufi"
                        ))
                        questionIndex++
                    }
                }
                "collector.fill" -> {
                    val infoObj = json.optJSONObject("info") ?: continue
                    val stdArray = infoObj.optJSONArray("std") ?: continue

                    for (i in 0 until stdArray.length()) {
                        val item = stdArray.getJSONObject(i)
                        val number = item.optString("xth", "")
                        val answer = cleanText(item.optString("value", ""))

                        fillQuestions.add(Question(
                            order = questionIndex + 1,
                            sectionOrder = fillQuestions.size + 1,
                            sectionCaption = "听后记录",
                            typeName = "听后记录",
                            questionText = "第${number}题",
                            answers = listOf(answer),
                            originalText = null,
                            category = "simple_expression_ufi"
                        ))
                        questionIndex++
                    }
                }
                "collector.picture" -> {
                    val infoObj = json.optJSONObject("info") ?: continue
                    val stdArray = infoObj.optJSONArray("std")
                    val answers = if (stdArray != null && stdArray.length() > 0) {
                        (0 until stdArray.length()).map { idx ->
                            cleanText(stdArray.getJSONObject(idx).optString("value", ""))
                        }
                    } else listOf("暂无标准答案")
                    val topicTitle = infoObj.optString("topic", "听后转述")

                    pictureQuestions.add(Question(
                        order = questionIndex + 1,
                        sectionOrder = pictureQuestions.size + 1,
                        sectionCaption = "听后转述",
                        typeName = "听后转述",
                        questionText = topicTitle,
                        answers = answers,
                        originalText = infoObj.optString("value", ""),
                        category = "topic"
                    ))
                    questionIndex++
                }
                "collector.dialogue" -> {
                    val infoObj = json.optJSONObject("info") ?: continue
                    val questionsArray = infoObj.optJSONArray("question") ?: continue

                    for (i in 0 until questionsArray.length()) {
                        val q = questionsArray.getJSONObject(i)
                        val askText = cleanText(q.optString("ask", ""))
                        val stdArray = q.optJSONArray("std")
                        val answers = if (stdArray != null && stdArray.length() > 0) {
                            (0 until stdArray.length()).map { idx ->
                                cleanText(stdArray.getJSONObject(idx).optString("value", ""))
                            }
                        } else listOf("暂无标准答案")

                        dialogueQuestions.add(Question(
                            order = questionIndex + 1,
                            sectionOrder = dialogueQuestions.size + 1,
                            sectionCaption = "回答问题",
                            typeName = "回答问题",
                            questionText = askText,
                            answers = answers,
                            originalText = null,
                            category = "simple_expression_ufk"
                        ))
                        questionIndex++
                    }
                }
                // collector.read 跳过
            }
        }

        // 按文档顺序组装：听后选择 → 听后记录 → 听后转述 → 回答问题
        val sections = mutableListOf<Section>()
        if (chooseQuestions.isNotEmpty()) {
            sections.add(Section("听后选择", "simple_expression_ufi", "听后选择", chooseQuestions, null))
        }
        if (fillQuestions.isNotEmpty()) {
            sections.add(Section("听后记录", "simple_expression_ufi", "听后记录", fillQuestions, null))
        }
        if (pictureQuestions.isNotEmpty()) {
            sections.add(Section("听后转述", "topic", "听后转述", pictureQuestions, null))
        }
        if (dialogueQuestions.isNotEmpty()) {
            sections.add(Section("回答问题", "simple_expression_ufk", "回答问题", dialogueQuestions, null))
        }

        if (sections.isEmpty()) return emptyList()

        val paperId = folders.first().name.hashCode().toLong()
        return listOf(Paper(
            paperId = paperId,
            title = "北京高中 #${groupIndex + 1}",
            dataFileName = folders.first().name,
            fileSize = folders.sumOf { it.size },
            sections = sections,
            downloadTime = folders.first().lastModified,
            regionLabel = "北京高中",
            paperName = null
        ))
    }

    /**
     * 通用解析器（文件夹数量不匹配）
     * 根据 structure_type 分发处理
     */
    private fun parseGenericPapers(
        reader: ETS100FileReader.Reader,
        folders: List<ETS100FileReader.FileItem>,
        groupIndex: Int,
        processedPaths: MutableSet<String>
    ): List<Paper> {
        Log.d(TAG, "parseGenericPapers: 第 $groupIndex 组，${folders.size} 个文件夹（通用解析）")
        val allQuestions = mutableListOf<Question>()
        var questionIndex = 0

        for (folder in folders) {
            val contentPath = "${ETS100FileReader.Path.getResourceDir()}/${folder.name}/content.json"
            processedPaths.add(contentPath)

            val content = reader.readFile(contentPath) ?: continue
            val json = JSONObject(content)
            val (questions, _) = parseContentJson(json, questionIndex, "unknown", "未知题型", "通用练习")

            if (questions.isNotEmpty()) {
                allQuestions.addAll(questions)
                questionIndex += questions.size
            }
        }

        if (allQuestions.isEmpty()) return emptyList()

        val sections = listOf(Section(
            caption = "通用练习",
            category = "unknown",
            typeName = "通用练习",
            questions = allQuestions,
            originalContent = null
        ))

        val paperId = folders.first().name.hashCode().toLong()
        return listOf(Paper(
            paperId = paperId,
            title = "通用练习 #${groupIndex + 1}",
            dataFileName = folders.first().name,
            fileSize = folders.sumOf { it.size },
            sections = sections,
            downloadTime = folders.first().lastModified,
            regionLabel = "通用",
            paperName = null
        ))
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
                    questions = sectionQuestions,
                    originalContent = sectionOriginalContent
                ))
            }
        }

        Log.d(TAG, "parseNormalExercise: 完成 ${dataFile.name} 包含 ${sections.size} 个section共${globalQuestionIndex}道题目")

        // 喵~ 查询 BeijingIndexManager 获取试卷名称（优先）和地区标签
        val paperIdStr = paperId.toString()
        
        // 获取 fileNameData 中的 fileIdentifier（用于 fileIdentifier 查询）
        val fileNameData = json.optJSONArray("fileNameData")
        val firstFileIdentifier = if (fileNameData != null && fileNameData.length() > 0) {
            fileNameData.getString(0)
        } else null
        
        Log.d(TAG, "parseNormalExercise: paperId=$paperIdStr, firstFileIdentifier=$firstFileIdentifier")
        
        // 优先从 BeijingIndexManager 获取（beijing-*.json 有完整的试卷名称）
        var regionLabel = BeijingIndexManager.getRegionLabel(paperIdStr)
        var paperName = BeijingIndexManager.getPaperName(paperIdStr)
        
        // 如果 paperId 查询失败，尝试使用 fileIdentifier 查询
        if (regionLabel == "未知" && firstFileIdentifier != null) {
            val fiEntry = BeijingIndexManager.getEntryByFileIdentifier(firstFileIdentifier)
            if (fiEntry != null) {
                regionLabel = fiEntry.regionType.displayName
                paperName = fiEntry.name
                Log.d(TAG, "parseNormalExercise: 通过 fileIdentifier 找到试卷: ${fiEntry.name}")
            }
        }
        
        // 如果 BeijingIndexManager 没找到，fallback 到 ResourceIndexManager
        if (regionLabel == "未知" || paperName == null) {
            val resourceRegionLabel = ResourceIndexManager.getRegionLabel(paperIdStr)
            val resourcePaperName = ResourceIndexManager.getPaperName(paperIdStr)
            
            // 如果 paperId 查询失败，尝试使用 fileIdentifier 查询
            if ((resourceRegionLabel == "未知" || resourcePaperName == null) && firstFileIdentifier != null) {
                val fiEntry = ResourceIndexManager.getEntryByFileIdentifier(firstFileIdentifier)
                if (fiEntry != null) {
                    val fiRegionLabel = fiEntry.regionType.displayName
                    val fiPaperName = fiEntry.name
                    if (resourceRegionLabel == "未知" && fiRegionLabel != "未知") {
                        // 不要覆盖，因为这个可能更准确
                    }
                    if (resourcePaperName == null && fiPaperName != null) {
                        // 同样保留已有的
                    }
                }
            }
            
            if (regionLabel == "未知" && resourceRegionLabel != "未知") {
                regionLabel = resourceRegionLabel
            }
            if (paperName == null && resourcePaperName != null) {
                paperName = resourcePaperName
            }
        }

        return Paper(
            paperId = paperId,
            title = paperName ?: "试卷 #$paperId",  // 优先使用索引中的名称喵~
            dataFileName = dataFile.name,
            fileSize = dataFile.size,
            sections = sections,
            downloadTime = dataFile.lastModified,  // 宝贝添加了下载时间喵~
            regionLabel = regionLabel,  // 喵~ 添加地区标签
            paperName = paperName  // 喵~ 添加试卷名称
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
                        questions = questions,
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
    private fun parseContentJson(
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
                    category = category
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
                            category = category
                        ))
                    }
                    return Pair(questions, originalContent)
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
                            category = category
                        ))
                    }
                    return Pair(questions, originalContent)
                }
            }

            StructureType.COLLECTOR_CHOOSE -> {
                // 北京：听后选择题
                // 喵~ 答案在 info.xtlist[].answer，选项在 info.xtlist[].xxlist 喵！
                val xtlist = infoObj.optJSONArray("xtlist")
                if (xtlist != null && xtlist.length() > 0) {
                    for (i in 0 until xtlist.length()) {
                        val item = xtlist.getJSONObject(i)
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
                            category = category
                        ))
                    }
                    return Pair(questions, originalContent)
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
                        val answer = cleanText(item.optString("value", ""))
                        
                        questions.add(Question(
                            order = startIndex + i + 1,
                            sectionOrder = i + 1,
                            sectionCaption = sectionCaption,
                            typeName = typeName,
                            questionText = "第${number}题",
                            answers = listOf(answer),
                            originalText = null,
                            category = category
                        ))
                    }
                    return Pair(questions, originalContent)
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
                            category = category
                        ))
                    }
                    return Pair(questions, originalContent)
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
                            category = category
                        ))
                    }
                    return Pair(questions, originalContent)
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
                        category = category
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