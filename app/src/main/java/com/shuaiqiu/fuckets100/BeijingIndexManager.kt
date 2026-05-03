package com.shuaiqiu.fuckets100

import android.content.Context
import android.util.Log
import org.json.JSONObject

/**
 * 北京索引管理器
 * 负责解析 beijing-*.json 文件，根据 paperId 查询试卷名称
 * 
 * 喵~ beijing-*.json 使用 items 数组格式，包含完整的试卷名称喵！
 */
object BeijingIndexManager {

    private const val TAG = "BeijingIndexManager"

    /**
     * 地区类型
     */
    enum class RegionType(val displayName: String) {
        MIDDLE_SCHOOL("初中"),
        HIGH_SCHOOL("高中"),
        UNKNOWN("未知")
    }

    /**
     * 试卷条目数据类
     */
    data class PaperEntry(
        val id: String,                    // 试卷ID
        val name: String,                  // 试卷名称（如 "2025-BSD 必修一 U1A"）
        val fileIdentifiers: List<String>, // 文件标识符列表
        val regionType: RegionType         // 地区类型
    )

    // C系列索引（初中）- 按ID索引
    private var cSeriesById: MutableMap<String, PaperEntry> = mutableMapOf()
    
    // G系列索引（高中）- 按ID索引
    private var gSeriesById: MutableMap<String, PaperEntry> = mutableMapOf()
    
    // C系列索引（初中）- 按fileIdentifier索引
    private var cSeriesByFileIdentifier: MutableMap<String, PaperEntry> = mutableMapOf()
    
    // G系列索引（高中）- 按fileIdentifier索引
    private var gSeriesByFileIdentifier: MutableMap<String, PaperEntry> = mutableMapOf()
    
    // 是否已初始化
    private var isInitialized = false

    /**
     * 从名称中解析地区类型
     * C系列格式: "(新教材)7年级(上)U1A" - 包含"年级"
     * G系列格式: "2025-BSD 必修一 U1A" - 包含"必修"或"高一"等
     */
    private fun parseRegionType(name: String): RegionType {
        return when {
            name.contains("年级") -> RegionType.MIDDLE_SCHOOL  // 初中
            name.contains("必修") || name.contains("选择性必修") -> RegionType.HIGH_SCHOOL  // 高中
            name.contains("高一") || name.contains("高二") || name.contains("高三") -> RegionType.HIGH_SCHOOL  // 高中
            else -> RegionType.UNKNOWN
        }
    }

    /**
     * 初始化索引管理器
     * 需要在 Application 启动时调用
     */
    fun initialize(context: Context) {
        if (isInitialized) {
            Log.d(TAG, "BeijingIndexManager 已初始化，跳过")
            return
        }

        Log.i(TAG, "初始化 BeijingIndexManager...")
        
        // 从 assets 加载 C系列 和 G系列 beijing 索引
        loadBeijingIndex(context, "beijing-C1.json", cSeriesById)
        loadBeijingIndex(context, "beijing-C2.json", cSeriesById)
        loadBeijingIndex(context, "beijing-C3.json", cSeriesById)
        
        loadBeijingIndex(context, "beijing-G1.json", gSeriesById)
        loadBeijingIndex(context, "beijing-G2.json", gSeriesById)
        loadBeijingIndex(context, "beijing-G3.json", gSeriesById)

        isInitialized = true
        Log.i(TAG, "BeijingIndexManager 初始化完成: C系列=${cSeriesById.size}条, G系列=${gSeriesById.size}条")
    }

    /**
     * 从 assets 加载 beijing 索引文件（items 数组格式 + byFileIdentifier 对象格式）
     * 喵~ 现在同时解析 items[] 和 byFileIdentifier{} 两部分喵！
     */
    private fun loadBeijingIndex(context: Context, fileName: String, targetMap: MutableMap<String, PaperEntry>) {
        try {
            val jsonString = context.assets.open("etsresource/$fileName").bufferedReader().use { it.readText() }
            val json = JSONObject(jsonString)
            
            // 解析 items 数组（主索引）
            val items = json.optJSONArray("items")
            if (items != null) {
                for (i in 0 until items.length()) {
                    val item = items.getJSONObject(i)
                    val id = item.optString("id", "")
                    val name = item.optString("name", "")
                    val fileIds = item.optJSONArray("fileIdentifiers")?.let { arr ->
                        (0 until arr.length()).map { arr.getString(it) }
                    } ?: emptyList()

                    if (id.isNotEmpty()) {
                        targetMap[id] = PaperEntry(
                            id = id,
                            name = name,
                            fileIdentifiers = fileIds,
                            regionType = parseRegionType(name)
                        )
                    }
                }
            }
            
            // 喵~ 解析 byFileIdentifier 对象（辅助索引），用于通过 fileIdentifier 反向查找
            val byFileIdentifier = json.optJSONObject("byFileIdentifier")
            if (byFileIdentifier != null) {
                byFileIdentifier.keys().forEach { fileIdentifier ->
                    val entry = byFileIdentifier.getJSONObject(fileIdentifier)
                    val id = entry.optString("id", "")
                    val name = entry.optString("name", "")
                    val fileIds = entry.optJSONArray("fileIdentifiers")?.let { arr ->
                        (0 until arr.length()).map { arr.getString(it) }
                    } ?: emptyList()
                    
                    // 只有当 ID 已经存在于 targetMap 中时才添加 fileIdentifier 索引
                    // 这样可以避免在 byFileIdentifier 中创建重复的 PaperEntry
                    if (id.isNotEmpty() && targetMap.containsKey(id)) {
                        // 添加所有 fileIdentifiers 到索引
                        fileIds.forEach { fid ->
                            if (!targetMap.containsKey(fid)) {
                                // 创建一个基于原条目但使用 fileIdentifier 作为键的副本喵~
                                targetMap[fid] = PaperEntry(
                                    id = id,
                                    name = name,
                                    fileIdentifiers = fileIds,
                                    regionType = parseRegionType(name)
                                )
                            }
                        }
                    }
                }
            }
            
            Log.d(TAG, "加载 $fileName: ${items?.length() ?: 0} 条记录 (items), ${byFileIdentifier?.length() ?: 0} 条记录 (byFileIdentifier)")
        } catch (e: Exception) {
            Log.e(TAG, "加载 $fileName 失败: ${e.message}")
        }
    }

    /**
     * 根据 paperId 查询试卷条目
     * @param paperId 试卷ID
     * @return 试卷条目，如果未找到返回 null
     */
    fun getEntryByPaperId(paperId: String): PaperEntry? {
        // 先查询 C系列
        cSeriesById[paperId]?.let { return it }
        // 再查询 G系列
        gSeriesById[paperId]?.let { return it }
        return null
    }
    
    /**
     * 喵~ 根据 fileIdentifier 查询试卷条目（通过 byFileIdentifier 索引）
     * @param fileIdentifier 文件标识符（MD5 哈希值）
     * @return 试卷条目，如果未找到返回 null
     */
    fun getEntryByFileIdentifier(fileIdentifier: String): PaperEntry? {
        // 先查询 C系列
        cSeriesById[fileIdentifier]?.let { return it }
        // 再查询 G系列
        gSeriesById[fileIdentifier]?.let { return it }
        return null
    }

    /**
     * 根据 paperId 获取地区类型
     * @param paperId 试卷ID
     * @return 地区类型
     */
    fun getRegionType(paperId: String): RegionType {
        return getEntryByPaperId(paperId)?.regionType ?: RegionType.UNKNOWN
    }

    /**
     * 根据 paperId 获取试卷名称
     * @param paperId 试卷ID
     * @return 试卷名称，如果未找到返回 null
     */
    fun getPaperName(paperId: String): String? {
        return getEntryByPaperId(paperId)?.name
    }

    /**
     * 根据 paperId 获取地区标签文本
     * @param paperId 试卷ID
     * @return 地区标签，如 "初中"、"高中" 或 "未知"
     */
    fun getRegionLabel(paperId: String): String {
        return getRegionType(paperId).displayName
    }

    /**
     * 检查是否已初始化
     */
    fun isReady(): Boolean = isInitialized

    /**
     * 获取统计数据
     * 喵~ 现在显示 items 数量和 fileIdentifier 索引数量喵！
     */
    fun getStats(): String {
        // 计算 fileIdentifier 索引数量（不包含 ID 重复的）
        val cFiCount = cSeriesById.count { (k, v) -> k != v.id }
        val gFiCount = gSeriesById.count { (k, v) -> k != v.id }
        return "C系列: ${cSeriesById.count { it.key == it.value.id }}条 (items), ${cFiCount}条 (fileIdentifier) | G系列: ${gSeriesById.count { it.key == it.value.id }}条 (items), ${gFiCount}条 (fileIdentifier)"
    }

    /**
     * 清除索引（用于测试或内存清理）
     * 喵~ 同时清除 items 和 fileIdentifier 索引喵！
     */
    fun clear() {
        cSeriesById.clear()
        gSeriesById.clear()
        cSeriesByFileIdentifier.clear()
        gSeriesByFileIdentifier.clear()
        isInitialized = false
        Log.d(TAG, "BeijingIndexManager 已清除")
    }
}