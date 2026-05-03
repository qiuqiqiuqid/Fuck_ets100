package com.shuaiqiu.fuckets100

import android.content.Context
import android.util.Log
import org.json.JSONObject

/**
 * 资源索引管理器
 * 负责解析 resource-*.json 文件，根据 paperId 查询试卷名称和地区信息
 * 
 * 喵~ 支持 C系列（初中）和 G系列（高中）的资源索引喵！
 */
object ResourceIndexManager {

    private const val TAG = "ResourceIndexManager"

    /**
     * 地区类型
     */
    enum class RegionType(val displayName: String) {
        MIDDLE_SCHOOL("初中"),
        HIGH_SCHOOL("高中"),
        UNKNOWN("未知")
    }

    /**
     * 资源条目数据类
     */
    data class ResourceEntry(
        val id: String,           // 资源ID（如 "376927"）
        val name: String,         // 资源名称（如 "(新教材)7年级(上)U1A"）
        val fileIdentifiers: List<String>,  // 文件标识符列表
        val regionType: RegionType  // 地区类型
    )

    // C系列资源索引（初中）
    private var cSeriesIndex: MutableMap<String, ResourceEntry> = mutableMapOf()
    
    // G系列资源索引（高中）
    private var gSeriesIndex: MutableMap<String, ResourceEntry> = mutableMapOf()
    
    // 喵~ C系列资源索引（初中）- 按 fileIdentifier 索引
    private var cSeriesByFileIdentifier: MutableMap<String, ResourceEntry> = mutableMapOf()
    
    // 喵~ G系列资源索引（高中）- 按 fileIdentifier 索引
    private var gSeriesByFileIdentifier: MutableMap<String, ResourceEntry> = mutableMapOf()
    
    // 是否已初始化
    private var isInitialized = false

    /**
     * 从名称中解析地区类型
     * C系列格式: "(新教材)7年级(上)U1A" - 包含"年级"
     * G系列格式: "(新)单元测试题1" - 不包含"年级"
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
     * 初始化资源索引
     * 需要在 Application 启动时调用
     */
    fun initialize(context: Context) {
        if (isInitialized) {
            Log.d(TAG, "ResourceIndexManager 已初始化，跳过")
            return
        }

        Log.i(TAG, "初始化 ResourceIndexManager...")
        
        // 从 assets 加载 C系列 和 G系列 资源索引
        loadResourceIndex(context, "resource-C1.json", cSeriesIndex)
        loadResourceIndex(context, "resource-C2.json", cSeriesIndex)
        loadResourceIndex(context, "resource-C3.json", cSeriesIndex)
        
        loadResourceIndex(context, "resource-G1.json", gSeriesIndex)
        loadResourceIndex(context, "resource-G2.json", gSeriesIndex)
        loadResourceIndex(context, "resource-G3.json", gSeriesIndex)

        isInitialized = true
        Log.i(TAG, "ResourceIndexManager 初始化完成: C系列=${cSeriesIndex.size}条, G系列=${gSeriesIndex.size}条")
    }

    /**
     * 从 assets 加载资源索引文件
     * 喵~ 同时解析 byId 和 byFileIdentifier 两部分喵！
     */
    private fun loadResourceIndex(context: Context, fileName: String, targetMap: MutableMap<String, ResourceEntry>) {
        try {
            val jsonString = context.assets.open("etsresource/$fileName").bufferedReader().use { it.readText() }
            val json = JSONObject(jsonString)
            
            // 解析 byId 对象（主索引）
            val byId = json.optJSONObject("byId")
            if (byId != null) {
                byId.keys().forEach { key ->
                    val entry = byId.getJSONObject(key)
                    val name = entry.optString("name", "")
                    val fileIds = entry.optJSONArray("file_identifiers")?.let { arr ->
                        (0 until arr.length()).map { arr.getString(it) }
                    } ?: emptyList()

                    targetMap[key] = ResourceEntry(
                        id = key,
                        name = name,
                        fileIdentifiers = fileIds,
                        regionType = parseRegionType(name)
                    )
                }
            }
            
            // 喵~ 解析 byFileIdentifier 对象（辅助索引），用于通过 fileIdentifier 反向查找
            val byFileIdentifier = json.optJSONObject("byFileIdentifier")
            if (byFileIdentifier != null) {
                byFileIdentifier.keys().forEach { fileIdentifier ->
                    val paperId = byFileIdentifier.getString(fileIdentifier)
                    // 如果 paperId 存在于主索引中，则添加 fileIdentifier 索引喵~
                    targetMap[paperId]?.let { entry ->
                        targetMap[fileIdentifier] = entry
                    }
                }
            }
            
            Log.d(TAG, "加载 $fileName: ${byId?.length() ?: 0} 条记录 (byId), ${byFileIdentifier?.length() ?: 0} 条记录 (byFileIdentifier)")
        } catch (e: Exception) {
            Log.e(TAG, "加载 $fileName 失败: ${e.message}")
        }
    }

    /**
     * 根据 paperId 查询资源条目
     * @param paperId 试卷ID
     * @return 资源条目，如果未找到返回 null
     */
    fun getEntryByPaperId(paperId: String): ResourceEntry? {
        // 先查询 C系列
        cSeriesIndex[paperId]?.let { return it }
        // 再查询 G系列
        gSeriesIndex[paperId]?.let { return it }
        return null
    }
    
    /**
     * 喵~ 根据 fileIdentifier 查询资源条目（通过 byFileIdentifier 索引）
     * @param fileIdentifier 文件标识符（MD5 哈希值）
     * @return 资源条目，如果未找到返回 null
     */
    fun getEntryByFileIdentifier(fileIdentifier: String): ResourceEntry? {
        // 先查询 C系列
        cSeriesIndex[fileIdentifier]?.let { return it }
        // 再查询 G系列
        gSeriesIndex[fileIdentifier]?.let { return it }
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
     * 喵~ 现在显示 byId 数量和 byFileIdentifier 索引数量喵！
     */
    fun getStats(): String {
        val cByIdCount = cSeriesIndex.count { (k, v) -> k == v.id }
        val gByIdCount = gSeriesIndex.count { (k, v) -> k == v.id }
        val cFiCount = cSeriesIndex.size - cByIdCount
        val gFiCount = gSeriesIndex.size - gByIdCount
        return "C系列: ${cByIdCount}条 (byId), ${cFiCount}条 (fileIdentifier) | G系列: ${gByIdCount}条 (byId), ${gFiCount}条 (fileIdentifier)"
    }

    /**
     * 清除索引（用于测试或内存清理）
     * 喵~ 同时清除 byId 和 fileIdentifier 索引喵！
     */
    fun clear() {
        cSeriesIndex.clear()
        gSeriesIndex.clear()
        cSeriesByFileIdentifier.clear()
        gSeriesByFileIdentifier.clear()
        isInitialized = false
        Log.d(TAG, "ResourceIndexManager 已清除")
    }
}