package com.shuaiqiu.fuckets100

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

object ReadPageStateStore {
    private const val PREFS_NAME = "read_page_state"
    private const val KEY_LOCAL = "local_snapshot"
    private const val KEY_CLOUD = "cloud_snapshot"

    data class LocalSnapshot(
        val mode: ActivationMode,
        val readerInfo: String,
        val dataFiles: List<ETS100FileReader.FileItem>,
        val resourceFiles: List<ETS100FileReader.FileItem>,
        val papers: List<ETS100AnswerReader.Paper>
    )

    data class CloudSnapshot(
        val selectedStatus: String,
        val homeworkListsByStatus: Map<String, List<ETS100ApiClient.HomeworkInfo>>,
        val cloudBaseUrl: String,
        val downloadedPapers: Map<String, List<ETS100AnswerReader.Paper>>,
        val downloadedHomeworkNames: Set<String>,
        val failedCloudHomeworks: Set<String>
    )

    fun saveLocal(context: Context, snapshot: LocalSnapshot) {
        prefs(context).edit {
            putString(KEY_LOCAL, snapshot.toJson().toString())
        }
    }

    fun loadLocal(context: Context): LocalSnapshot? {
        val raw = prefs(context).getString(KEY_LOCAL, null) ?: return null
        return runCatching { parseLocalSnapshot(JSONObject(raw)) }.getOrNull()
    }

    fun clearLocal(context: Context) {
        prefs(context).edit { remove(KEY_LOCAL) }
    }

    fun saveCloud(context: Context, snapshot: CloudSnapshot) {
        prefs(context).edit {
            putString(KEY_CLOUD, snapshot.toJson().toString())
        }
    }

    fun loadCloud(context: Context): CloudSnapshot? {
        val raw = prefs(context).getString(KEY_CLOUD, null) ?: return null
        return runCatching { parseCloudSnapshot(JSONObject(raw)) }.getOrNull()
    }

    fun clearCloud(context: Context) {
        prefs(context).edit { remove(KEY_CLOUD) }
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun LocalSnapshot.toJson(): JSONObject = JSONObject()
        .put("mode", mode.name)
        .put("readerInfo", readerInfo)
        .put("dataFiles", dataFiles.toFileJsonArray())
        .put("resourceFiles", resourceFiles.toFileJsonArray())
        .put("papers", papers.toPaperJsonArray())

    private fun CloudSnapshot.toJson(): JSONObject = JSONObject()
        .put("selectedStatus", selectedStatus)
        .put("homeworkListsByStatus", homeworkListsByStatus.toHomeworkMapJson())
        .put("cloudBaseUrl", cloudBaseUrl)
        .put("downloadedPapers", downloadedPapers.toPaperMapJson())
        .put("downloadedHomeworkNames", JSONArray(downloadedHomeworkNames.toList()))
        .put("failedCloudHomeworks", JSONArray(failedCloudHomeworks.toList()))

    private fun parseLocalSnapshot(json: JSONObject): LocalSnapshot {
        val mode = json.optString("mode", "").let { rawMode ->
            ActivationMode.entries.firstOrNull { it.name == rawMode }
        } ?: error("Local snapshot missing activation mode")

        return LocalSnapshot(
            mode = mode,
            readerInfo = json.optString("readerInfo", ""),
            dataFiles = parseFileItems(json.optJSONArray("dataFiles")),
            resourceFiles = parseFileItems(json.optJSONArray("resourceFiles")),
            papers = parsePapers(json.optJSONArray("papers"))
        )
    }

    private fun parseCloudSnapshot(json: JSONObject): CloudSnapshot = CloudSnapshot(
        selectedStatus = json.optString("selectedStatus", CloudHomeworkState.STATUS_CURRENT),
        homeworkListsByStatus = parseHomeworkMap(json.optJSONObject("homeworkListsByStatus")),
        cloudBaseUrl = json.optString("cloudBaseUrl", ETS100ApiClient.Config.CDN_BASE_URL),
        downloadedPapers = parsePaperMap(json.optJSONObject("downloadedPapers")),
        downloadedHomeworkNames = parseStringSet(json.optJSONArray("downloadedHomeworkNames")),
        failedCloudHomeworks = parseStringSet(json.optJSONArray("failedCloudHomeworks"))
    )

    private fun List<ETS100FileReader.FileItem>.toFileJsonArray(): JSONArray {
        val array = JSONArray()
        forEach { file ->
            array.put(
                JSONObject()
                    .put("name", file.name)
                    .put("path", file.path)
                    .put("isDirectory", file.isDirectory)
                    .put("size", file.size)
                    .put("lastModified", file.lastModified)
            )
        }
        return array
    }

    private fun parseFileItems(array: JSONArray?): List<ETS100FileReader.FileItem> {
        if (array == null) return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            array.optJSONObject(index)?.let { json ->
                ETS100FileReader.FileItem(
                    name = json.optString("name"),
                    path = json.optString("path"),
                    isDirectory = json.optBoolean("isDirectory"),
                    size = json.optLong("size"),
                    lastModified = json.optLong("lastModified")
                )
            }
        }
    }

    private fun List<ETS100AnswerReader.Paper>.toPaperJsonArray(): JSONArray {
        val array = JSONArray()
        forEach { paper -> array.put(paper.toJson()) }
        return array
    }

    private fun ETS100AnswerReader.Paper.toJson(): JSONObject = JSONObject()
        .put("paperId", paperId)
        .put("title", title)
        .put("dataFileName", dataFileName)
        .put("fileSize", fileSize)
        .put("sections", sections.toSectionJsonArray())
        .put("downloadTime", downloadTime)
        .put("regionLabel", regionLabel)
        .put("paperName", paperName)
        .put("source", source?.toJson())

    private fun parsePapers(array: JSONArray?): List<ETS100AnswerReader.Paper> {
        if (array == null) return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            array.optJSONObject(index)?.let(::parsePaper)
        }
    }

    private fun parsePaper(json: JSONObject): ETS100AnswerReader.Paper =
        ETS100AnswerReader.Paper(
            paperId = json.optLong("paperId"),
            title = json.optString("title"),
            dataFileName = json.optString("dataFileName"),
            fileSize = json.optLong("fileSize"),
            sections = parseSections(json.optJSONArray("sections")),
            downloadTime = json.optLong("downloadTime"),
            regionLabel = json.optString("regionLabel", "未知"),
            paperName = json.optNullableString("paperName"),
            source = parsePaperSource(json.optJSONObject("source"))
        )

    private fun PaperSource.toJson(): JSONObject = when (this) {
        is PaperSource.Local -> JSONObject()
            .put("type", "local")
            .put("mode", mode.name)
            .put("resourceDirectoryNames", JSONArray(resourceDirectoryNames))
            .put("resourceModifiedTimes", JSONObject().also { times ->
                resourceModifiedTimes.forEach { (name, value) -> times.put(name, value) }
            })

        is PaperSource.Cloud -> JSONObject()
            .put("type", "cloud")
            .put("status", status)
            .put("homeworkIdentity", homeworkIdentity)
            .put("baseUrl", baseUrl)
            .put("contents", JSONArray().also { array ->
                contents.forEach { content ->
                    array.put(
                        JSONObject()
                            .put("groupName", content.groupName)
                            .put("url", content.url)
                    )
                }
            })
    }

    private fun parsePaperSource(json: JSONObject?): PaperSource? {
        if (json == null) return null
        return when (json.optString("type")) {
            "local" -> {
                val mode = ActivationMode.entries.firstOrNull { it.name == json.optString("mode") }
                    ?: return null
                val modifiedTimes = mutableMapOf<String, Long>()
                json.optJSONObject("resourceModifiedTimes")?.let { times ->
                    val keys = times.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        modifiedTimes[key] = times.optLong(key)
                    }
                }
                PaperSource.Local(
                    mode = mode,
                    resourceDirectoryNames = parseStringList(json.optJSONArray("resourceDirectoryNames")),
                    resourceModifiedTimes = modifiedTimes
                )
            }
            "cloud" -> PaperSource.Cloud(
                status = json.optString("status"),
                homeworkIdentity = json.optString("homeworkIdentity"),
                baseUrl = json.optString("baseUrl"),
                contents = json.optJSONArray("contents")?.let { array ->
                    (0 until array.length()).mapNotNull { index ->
                        array.optJSONObject(index)?.let { content ->
                            CloudContent(content.optString("groupName"), content.optString("url"))
                        }
                    }
                }.orEmpty()
            )
            else -> null
        }
    }

    private fun List<ETS100AnswerReader.Section>.toSectionJsonArray(): JSONArray {
        val array = JSONArray()
        forEach { section ->
            array.put(
                JSONObject()
                    .put("caption", section.caption)
                    .put("category", section.category)
                    .put("typeName", section.typeName)
                    .put("questions", section.questions.toQuestionJsonArray())
                    .put("originalContent", section.originalContent)
            )
        }
        return array
    }

    private fun parseSections(array: JSONArray?): List<ETS100AnswerReader.Section> {
        if (array == null) return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            array.optJSONObject(index)?.let { json ->
                ETS100AnswerReader.Section(
                    caption = json.optString("caption"),
                    category = json.optString("category"),
                    typeName = json.optString("typeName"),
                    questions = parseQuestions(json.optJSONArray("questions")),
                    originalContent = json.optNullableString("originalContent")
                )
            }
        }
    }

    private fun List<ETS100AnswerReader.Question>.toQuestionJsonArray(): JSONArray {
        val array = JSONArray()
        forEach { question -> array.put(question.toJson()) }
        return array
    }

    private fun ETS100AnswerReader.Question.toJson(): JSONObject = JSONObject()
        .put("order", order)
        .put("sectionOrder", sectionOrder)
        .put("sectionCaption", sectionCaption)
        .put("typeName", typeName)
        .put("questionText", questionText)
        .put("answers", JSONArray(answers))
        .put("originalText", originalText)
        .put("category", category)
        .put("displayOrder", displayOrder)
        .put("content", content.toJson())

    private fun parseQuestions(array: JSONArray?): List<ETS100AnswerReader.Question> {
        if (array == null) return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            array.optJSONObject(index)?.let { json ->
                ETS100AnswerReader.Question(
                    order = json.optInt("order"),
                    sectionOrder = json.optInt("sectionOrder"),
                    sectionCaption = json.optString("sectionCaption"),
                    typeName = json.optString("typeName"),
                    questionText = json.optString("questionText"),
                    answers = parseStringList(json.optJSONArray("answers")),
                    originalText = json.optNullableString("originalText"),
                    category = json.optString("category"),
                    displayOrder = json.optIntOrNull("displayOrder"),
                    content = parseAnswerContent(json.optJSONObject("content"))
                )
            }
        }
    }

    private fun ETS100AnswerReader.AnswerContent.toJson(): JSONObject = when (this) {
        is ETS100AnswerReader.AnswerContent.Reading -> JSONObject()
            .put("type", "reading")
            .put("text", text)

        is ETS100AnswerReader.AnswerContent.Choice -> JSONObject()
            .put("type", "choice")
            .put("items", items.toChoiceItemJsonArray())

        is ETS100AnswerReader.AnswerContent.QATuple -> JSONObject()
            .put("type", "qa")
            .put("pairs", pairs.toQAPairJsonArray())
    }

    private fun parseAnswerContent(json: JSONObject?): ETS100AnswerReader.AnswerContent {
        if (json == null) return ETS100AnswerReader.AnswerContent.Reading("")
        return when (json.optString("type")) {
            "choice" -> ETS100AnswerReader.AnswerContent.Choice(parseChoiceItems(json.optJSONArray("items")))
            "qa" -> ETS100AnswerReader.AnswerContent.QATuple(parseQAPairs(json.optJSONArray("pairs")))
            else -> ETS100AnswerReader.AnswerContent.Reading(json.optString("text", ""))
        }
    }

    private fun List<ETS100AnswerReader.ChoiceItem>.toChoiceItemJsonArray(): JSONArray {
        val array = JSONArray()
        forEach { item ->
            array.put(
                JSONObject()
                    .put("question", item.question)
                    .put("options", JSONArray(item.options))
                    .put("correctAnswer", item.correctAnswer)
                    .put("standardAnswer", item.standardAnswer)
            )
        }
        return array
    }

    private fun parseChoiceItems(array: JSONArray?): List<ETS100AnswerReader.ChoiceItem> {
        if (array == null) return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            array.optJSONObject(index)?.let { json ->
                ETS100AnswerReader.ChoiceItem(
                    question = json.optString("question"),
                    options = parseStringList(json.optJSONArray("options")),
                    correctAnswer = json.optInt("correctAnswer"),
                    standardAnswer = json.optString("standardAnswer")
                )
            }
        }
    }

    private fun List<ETS100AnswerReader.QAPair>.toQAPairJsonArray(): JSONArray {
        val array = JSONArray()
        forEach { pair ->
            array.put(
                JSONObject()
                    .put("question", pair.question)
                    .put("answer", pair.answer)
            )
        }
        return array
    }

    private fun parseQAPairs(array: JSONArray?): List<ETS100AnswerReader.QAPair> {
        if (array == null) return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            array.optJSONObject(index)?.let { json ->
                ETS100AnswerReader.QAPair(
                    question = json.optString("question"),
                    answer = json.optString("answer")
                )
            }
        }
    }

    private fun Map<String, List<ETS100ApiClient.HomeworkInfo>>.toHomeworkMapJson(): JSONObject {
        val json = JSONObject()
        forEach { (key, value) -> json.put(key, value.toHomeworkJsonArray()) }
        return json
    }

    private fun List<ETS100ApiClient.HomeworkInfo>.toHomeworkJsonArray(): JSONArray {
        val array = JSONArray()
        forEach { homework ->
            array.put(
                JSONObject()
                    .put("id", homework.id)
                    .put("name", homework.name)
                    .put("contents", homework.contents.toHomeworkContentJsonArray())
            )
        }
        return array
    }

    private fun List<ETS100ApiClient.HomeworkContent>.toHomeworkContentJsonArray(): JSONArray {
        val array = JSONArray()
        forEach { content ->
            array.put(
                JSONObject()
                    .put("groupName", content.groupName)
                    .put("url", content.url)
            )
        }
        return array
    }

    private fun parseHomeworkMap(json: JSONObject?): Map<String, List<ETS100ApiClient.HomeworkInfo>> {
        if (json == null) return emptyMap()
        val result = mutableMapOf<String, List<ETS100ApiClient.HomeworkInfo>>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            result[key] = parseHomeworks(json.optJSONArray(key))
        }
        return result
    }

    private fun parseHomeworks(array: JSONArray?): List<ETS100ApiClient.HomeworkInfo> {
        if (array == null) return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            array.optJSONObject(index)?.let { json ->
                ETS100ApiClient.HomeworkInfo(
                    id = json.optString("id"),
                    name = json.optString("name"),
                    contents = parseHomeworkContents(json.optJSONArray("contents"))
                )
            }
        }
    }

    private fun parseHomeworkContents(array: JSONArray?): List<ETS100ApiClient.HomeworkContent> {
        if (array == null) return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            array.optJSONObject(index)?.let { json ->
                ETS100ApiClient.HomeworkContent(
                    groupName = json.optString("groupName"),
                    url = json.optString("url")
                )
            }
        }
    }

    private fun Map<String, List<ETS100AnswerReader.Paper>>.toPaperMapJson(): JSONObject {
        val json = JSONObject()
        forEach { (key, value) -> json.put(key, value.toPaperJsonArray()) }
        return json
    }

    private fun parsePaperMap(json: JSONObject?): Map<String, List<ETS100AnswerReader.Paper>> {
        if (json == null) return emptyMap()
        val result = mutableMapOf<String, List<ETS100AnswerReader.Paper>>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            result[key] = parsePapers(json.optJSONArray(key))
        }
        return result
    }

    private fun parseStringList(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        return (0 until array.length()).map { index -> array.optString(index) }
    }

    private fun parseStringSet(array: JSONArray?): Set<String> = parseStringList(array).toSet()

    private fun JSONObject.optIntOrNull(name: String): Int? =
        if (has(name) && !isNull(name)) optInt(name) else null

    private fun JSONObject.optNullableString(name: String): String? =
        if (has(name) && !isNull(name)) optString(name) else null
}
