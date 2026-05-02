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
 */
object ETS100AnswerReader {

    private const val TAG = "ETS100AnswerReader"

    /**
     * 试卷数据类
     */
    data class Paper(
        val paperId: Long,
        val title: String,
        val dataFileName: String,
        val fileSize: Long,
        val sections: List<Section>,
        val downloadTime: Long = 0L  // 宝贝添加了下载时间喵~
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
        val order: Int,
        val sectionOrder: Int,
        val sectionCaption: String,
        val typeName: String,
        val questionText: String,
        val answers: List<String>,
        val originalText: String?,
        val category: String = "",
        val content: AnswerContent = AnswerContent.Reading("")
    ) {
        val question: String get() = questionText
        val answer: String get() = shortestAnswer
        val answerList: List<String> get() = answers

        val shortestAnswer: String
            get() {
                if (answers.isEmpty()) return ""
                var shortest = answers[0]
                for (a in answers) {
                    if (a.length < shortest.length) {
                        shortest = a
                    }
                }
                return shortest
            }
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
    // 宝贝根据文档修正了映射喵~
    private val categoryMap = mapOf(
        "read_chapter" to "模仿朗读",
        "simple_expression_ufi" to "听说信息",
        "simple_expression_ufk" to "问答信息",
        "topic" to "信息转述",
        "simple_expression_ufj" to "询问信息"
    )

    private fun getTypeName(category: String): String {
        return categoryMap[category] ?: category
    }

    /**
     * 读取所有试卷
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

        val dataDirPath = ETS100FileReader.Path.getDataDir()
        val dataFiles = reader.listFiles(dataDirPath)
            .filter { !it.isDirectory && it.size > ETS100FileReader.Path.MIN_FILE_SIZE }
            .sortedByDescending { it.lastModified }

        Log.i(TAG, "Found ${dataFiles.size} valid data files")

        val papers = mutableListOf<Paper>()
        for (file in dataFiles) {
            try {
                val paper = parsePaper(reader, file)
                if (paper != null) {
                    papers.add(paper)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse paper: ${file.name}", e)
            }
        }

        return papers
    }

    /**
     * 解析单个试卷的JSON数据
     */
    private fun parsePaper(reader: ETS100FileReader.Reader, dataFile: ETS100FileReader.FileItem): Paper? {
        val dataFilePath = "${ETS100FileReader.Path.getDataDir()}/${dataFile.name}"
        Log.d(TAG, "parsePaper: 解析试卷文件 ${dataFile.name}")

        val content = reader.readFile(dataFilePath)
        if (content == null) {
            Log.w(TAG, "parsePaper: 无法读取 data 文件: ${dataFile.name}")
            return null
        }

        return try {
            val json = JSONObject(content)

            val paperId = json.optLong("paperId", 0L)
            val sectionData = json.optJSONArray("sectionData")

            Log.d(TAG, "parsePaper: paperId=$paperId, sectionData=${sectionData?.length()}")

            if (paperId == 0L || sectionData == null) {
                Log.w(TAG, "parsePaper: Invalid paper data: paperId=$paperId, sectionData=${sectionData == null}")
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

                Log.d(TAG, "parsePaper: section $sectionIndex (caption=$caption, category=$category)")

                if (sectionItemData == null) {
                    Log.w(TAG, "parsePaper: section $sectionIndex 缺少 sectionItemData")
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
                        Log.d(TAG, "parsePaper: section $sectionIndex, item $itemIndex 跳过mainItem=true")
                        continue
                    }
                    
                    val fileName = item.optString("fileName", "")
                    if (fileName.isEmpty()) {
                        continue
                    }

                    // 跳过已处理的文件名
                    if (fileName in processedFileNames) {
                        Log.d(TAG, "parsePaper: section $sectionIndex, item $itemIndex 跳过重复fileName=$fileName")
                        continue
                    }
                    processedFileNames.add(fileName)

                    Log.d(TAG, "parsePaper: 处理文件 fileName=$fileName")

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

            Log.d(TAG, "parsePaper: 完成 ${dataFile.name} 包含 ${sections.size} 个section共${globalQuestionIndex}道题目")

            Paper(
                paperId = paperId,
                title = "试卷 #$paperId",
                dataFileName = dataFile.name,
                fileSize = dataFile.size,
                sections = sections,
                downloadTime = dataFile.lastModified  // 宝贝添加了下载时间喵~
            )
        } catch (e: Exception) {
            Log.e(TAG, "parsePaper: Failed to parse JSON from ${dataFile.name}: ${e.message}", e)
            null
        }
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
     * 注意: content.json 可能包含:
     * - info.question[] = 从口语题目获取答案，存储在ask和std[]中
     * - info.value = 阅读理解原文内容
     * - info.std[] = 信息转述答案
     * 
     * 宝贝这个方法现在修复了，可以同时处理 question 和 value 喵！
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
        if (infoObj != null) {
            // 宝贝先获取 info.value 作为原文（如果有的话）
            val value = infoObj.optString("value", "")
            if (value.isNotEmpty()) {
                originalContent = value.replace(Regex("<[^>]*>"), "").trim()
            }
            
            // 检查 structure_type 来决定如何解析
            val structureType = json.optString("structure_type", "")
            
            // 对于 topic 类型 (collector.picture)，使用 info.std[] 和 info.value
            if (structureType == "collector.picture" || category == "topic") {
                // info.std[] 是答案，info.value 是原文
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
                
                // topic 类型的原文是 info.value，答案在 info.std[]
                questions.add(Question(
                    order = startIndex + 1,
                    sectionOrder = 1,
                    sectionCaption = sectionCaption,
                    typeName = typeName,
                    questionText = topicTitle,
                    answers = answers,
                    originalText = originalContent,  // 原文是 info.value 喵~
                    category = category
                ))
                return Pair(questions, originalContent)
            }
            
            // 从info.question[]获取口语题目
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

                    // 宝贝这里修复了：根据 category 决定是否需要原文
                    // read_chapter 和 simple_expression_ufj 类型不需要显示原文
                    val questionOriginalText = when (category) {
                        "read_chapter" -> null  // 模仿朗读不需要原文喵~
                        "simple_expression_ufj" -> null  // 询问信息不需要原文喵~
                        else -> originalContent  // 其他类型如果有原文就显示
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

            // 从info.value获取原文（没有 question 的情况，如read_chapter）
            if (originalContent != null) {
                // read_chapter 只有原文，没有答案
                val answers = if (category == "read_chapter") {
                    emptyList<String>()  // 模仿朗读没有答案喵~
                } else {
                    listOf(originalContent)  // 其他情况把原文当答案
                }
                
                questions.add(Question(
                    order = startIndex + 1,
                    sectionOrder = 1,
                    sectionCaption = sectionCaption,
                    typeName = typeName,
                    questionText = "阅读理解原文",
                    answers = answers,
                    originalText = originalContent,
                    category = category
                ))
                return Pair(questions, originalContent)
            }
        }

        Log.w(TAG, "parseContentJson: 无法解析JSON内容")
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

        return parsePaper(reader, file)
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

    fun generateExportText(paper: Paper, displayMode: AnswerDisplayMode): String {
        val sep = "=".repeat(60)
        val timeStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date())

        val typeCounts = mutableMapOf<String, Int>()
        for (section in paper.sections) {
            val count = section.questions.count { it.answerList.isNotEmpty() }
            if (count > 0) {
                typeCounts[section.typeName] = typeCounts.getOrDefault(section.typeName, 0) + count
            }
        }
        val totalAnswered = typeCounts.values.sum()
        val distribution = typeCounts.entries.joinToString("、") { "${it.key}(${it.value}题)" }

        val sb = StringBuilder()
        sb.appendLine("E听说答案提取报告")
        sb.appendLine(sep)
        sb.appendLine("生成时间: $timeStr")
        sb.appendLine("试卷: ${paper.title} (ID: ${paper.paperId})")
        sb.appendLine("总题数: $totalAnswered")
        sb.appendLine("题型分布: $distribution")
        sb.appendLine(sep)
        sb.appendLine()

        var questionNum = 0
        for (section in paper.sections) {
            for (q in section.questions) {
                if (q.answerList.isEmpty()) continue
                questionNum++

                val answer = when (displayMode) {
                    AnswerDisplayMode.SHORTEST -> q.shortestAnswer
                    AnswerDisplayMode.ALL -> q.answerList.joinToString("\n") { "    - $it" }
                }

                sb.appendLine("【第${questionNum}题】${section.typeName} (${section.caption})")
                sb.appendLine("问题: ${q.questionText}")
                sb.appendLine("答案: $answer")
                sb.appendLine()
            }
        }

        sb.appendLine(sep)
        sb.appendLine("报告结束 - 感谢使用FE")
        sb.appendLine(sep)

        return sb.toString().trimEnd()
    }
}