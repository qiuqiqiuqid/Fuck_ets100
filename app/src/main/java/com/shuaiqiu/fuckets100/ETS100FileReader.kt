package com.shuaiqiu.fuckets100

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.util.Log
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.io.File
import java.security.MessageDigest

/**
 * ETS100 文件读取管理器
 * 负责根据ActivationMode选择合适的阅读器访问文件和目录
 * 
 * 使用示例:
 * val reader = ETS100FileReader.getReader(currentMode)
 * val files = reader.listFiles("/path/to/dir")
 * val content = reader.readFile("/path/to/file")
 */
object ETS100FileReader {

    private const val TAG = "ETS100FileReader"

    /**
     * 文件数据类
     */
    data class FileItem(
        val name: String,
        val path: String,
        val isDirectory: Boolean,
        val size: Long,
        val lastModified: Long
    )

    /**
     * ETS100 目录常量
     */
    object Path {
        // 设备基础路径 - ETS_secondary 目录
        const val DEVICE_BASE = "/storage/emulated/0/Android/data/com.ets100.secondary/files/Download/ETS_secondary"
        
        // 子目录名称
        const val BASE_DIR = ""  // 已在 DEVICE_BASE 中包含
        const val DATA_DIR = "data"
        const val RESOURCE_DIR = "resource"
        
        // 文件大小下限
        const val MIN_FILE_SIZE = 50 * 1024L  // 50KB
        
        // 获取data目录路径
        fun getDataDir(): String = "$DEVICE_BASE/$DATA_DIR"
        
        // 获取resource目录路径
        fun getResourceDir(): String = "$DEVICE_BASE/$RESOURCE_DIR"
        
        // 获取ETS_secondary父目录
        fun getETSSecondaryParent(): String = DEVICE_BASE
        
        // ZWC绕过目录 (Android 11+)
        fun getZWCDataDir(): String {
            return ZWCHelper.getDataDir()
        }
    }

    /**
     * MD5 哈希工具
     */
    object MD5 {
        fun hash(input: String): String {
            val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray())
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }

    /**
     * 文件读取器接口
     */
    interface Reader {
        /**
         * 列出目录内容
         */
        fun listFiles(path: String): List<FileItem>
        
        /**
         * 读取文件内容
         */
        fun readFile(path: String): String?
        
        /**
         * 检查文件是否存在
         */
        fun exists(path: String): Boolean
        
        /**
         * 获取文件大小
         */
        fun getFileSize(path: String): Long
        
        /**
         * 获取文件修改时间(Unix timestamp)
         */
        fun getFileModifiedTime(path: String): Long
        
        /**
         * 删除目录（递归删除所有内容）
         * @param path 要删除的目录路径
         * @return true 删除成功，false 失败
         */
        fun deleteDirectory(path: String): Boolean
        
        /**
         * 检查读取器是否可用
         */
        fun isAvailable(): Boolean
        
        /**
         * 获取当前模式
         */
        fun getMode(): ActivationMode
    }

    /**
     * Shizuku 文件读取器
     * 使用 Shizuku API 执行命令，而非 Root 方式
     */
    class ShizukuReader : Reader {
        override fun listFiles(path: String): List<FileItem> {
            return try {
                // 宝贝这里改用 Shizuku 专用命令执行方式喵~
                val lsResult = execShizukuCommand("ls -la \"$path\" 2>/dev/null")
                val items = parseLsOutput(lsResult ?: "")
                
                items.map { item ->
                    if (!item.isDirectory && item.name.isNotEmpty()) {
                        val filePath = "$path/${item.name}"
                        val statResult = execShizukuCommand("stat -c \"%Y\" \"$filePath\" 2>/dev/null")
                        val timestamp = statResult?.trim()?.toLongOrNull() ?: 0L
                        item.copy(lastModified = timestamp)
                    } else {
                        item
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Shizuku listFiles failed", e)
                emptyList()
            }
        }
        
        override fun readFile(path: String): String? {
            return try {
                // 宝贝这里使用 Shizuku 方式读取文件喵~
                execShizukuCommand("cat \"$path\"")
            } catch (e: Exception) {
                Log.e(TAG, "Shizuku readFile failed", e)
                null
            }
        }
        
        override fun exists(path: String): Boolean {
            return try {
                execShizukuCommand("test -e \"$path\" && echo 'exists'")?.contains("exists") == true
            } catch (e: Exception) {
                false
            }
        }
        
        override fun getFileSize(path: String): Long {
            return try {
                val result = execShizukuCommand("stat -c %s \"$path\"")
                result?.trim()?.toLongOrNull() ?: 0L
            } catch (e: Exception) {
                0L
            }
        }
        
        override fun getFileModifiedTime(path: String): Long {
            return try {
                val result = execShizukuCommand("stat -c %Y \"$path\"")
                result?.trim()?.toLongOrNull() ?: 0L
            } catch (e: Exception) {
                0L
            }
        }
        
        override fun isAvailable(): Boolean {
            // 宝贝检查 Shizuku 是否在运行且有权限喵~
            return ShizukuManager.isShizukuRunning() &&
                    ShizukuManager.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        
        override fun getMode(): ActivationMode = ActivationMode.SHIZUKU
        
        override fun deleteDirectory(path: String): Boolean {
            return try {
                // 宝贝使用 rm -rf 命令递归删除目录喵~
                val result = execShizukuCommand("rm -rf \"$path\" 2>/dev/null; echo 'done'")
                result?.contains("done") == true
            } catch (e: Exception) {
                Log.e(TAG, "Shizuku deleteDirectory failed", e)
                false
            }
        }
        
        /**
         * 使用 Shizuku API 执行命令
         * 宝贝这里修复了，之前错误地使用了 RootManager.execAsRoot() 喵！
         */
        private fun execShizukuCommand(cmd: String): String? {
            return try {
                ShizukuManager.execCommand(cmd)
            } catch (e: Exception) {
                Log.e(TAG, "Shizuku execCommand failed: ${e.message}", e)
                null
            }
        }
        
        private fun parseLsOutput(output: String): List<FileItem> {
            val items = mutableListOf<FileItem>()
            val lines = output.split("\n")
            
            for (line in lines) {
                if (line.startsWith("total") || line.isBlank()) continue
                
                val parts = line.trim().split("\\s+".toRegex())
                if (parts.size >= 8) {
                    val isDir = line.startsWith('d')
                    val name = parts[parts.size - 1]
                    val size = parts[4].toLongOrNull() ?: 0L
                    
                    if (name == "." || name == "..") continue
                    
                    items.add(FileItem(
                        name = name,
                        path = "",
                        isDirectory = isDir,
                        size = size,
                        lastModified = 0L
                    ))
                }
            }
            return items
        }
    }

    /**
     * Root 文件读取器
     */
    class RootReader : Reader {
        override fun listFiles(path: String): List<FileItem> {
            return try {
                val lsResult = RootManager.execAsRoot("ls -la \"$path\" 2>/dev/null")
                val items = parseLsOutput(lsResult ?: "")
                
                items.map { item ->
                    if (!item.isDirectory && item.name.isNotEmpty()) {
                        val filePath = "$path/${item.name}"
                        val statResult = RootManager.execAsRoot("stat -c \"%Y\" \"$filePath\" 2>/dev/null")
                        val timestamp = statResult?.trim()?.toLongOrNull() ?: 0L
                        item.copy(lastModified = timestamp)
                    } else {
                        item
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Root listFiles failed", e)
                emptyList()
            }
        }
        
        override fun readFile(path: String): String? {
            return try {
                RootManager.execAsRoot("cat \"$path\"")
            } catch (e: Exception) {
                Log.e(TAG, "Root readFile failed", e)
                null
            }
        }
        
        override fun exists(path: String): Boolean {
            return try {
                RootManager.execAsRoot("test -e \"$path\" && echo 'exists'")?.contains("exists") == true
            } catch (e: Exception) {
                false
            }
        }
        
        override fun getFileSize(path: String): Long {
            return try {
                val result = RootManager.execAsRoot("stat -c %s \"$path\"")
                result?.trim()?.toLongOrNull() ?: 0L
            } catch (e: Exception) {
                0L
            }
        }
        
        override fun getFileModifiedTime(path: String): Long {
            return try {
                val result = RootManager.execAsRoot("stat -c %Y \"$path\"")
                result?.trim()?.toLongOrNull() ?: 0L
            } catch (e: Exception) {
                0L
            }
        }
        
        override fun isAvailable(): Boolean {
            return RootManager.isRootAvailable()
        }
        
        override fun getMode(): ActivationMode = ActivationMode.ROOT
        
        override fun deleteDirectory(path: String): Boolean {
            return try {
                // 宝贝使用 rm -rf 命令递归删除目录喵~
                val result = RootManager.execAsRoot("rm -rf \"$path\" 2>/dev/null; echo 'done'")
                result?.contains("done") == true
            } catch (e: Exception) {
                Log.e(TAG, "Root deleteDirectory failed", e)
                false
            }
        }
        
        private fun parseLsOutput(output: String): List<FileItem> {
            val items = mutableListOf<FileItem>()
            val lines = output.split("\n")
            
            for (line in lines) {
                if (line.startsWith("total") || line.isBlank()) continue
                
                val parts = line.trim().split("\\s+".toRegex())
                if (parts.size >= 8) {
                    val isDir = line.startsWith('d')
                    val name = parts[parts.size - 1]
                    val size = parts[4].toLongOrNull() ?: 0L
                    
                    if (name == "." || name == "..") continue
                    
                    items.add(FileItem(
                        name = name,
                        path = "",
                        isDirectory = isDir,
                        size = size,
                        lastModified = 0L
                    ))
                }
            }
            return items
        }
    }

    /**
     * ZWC 绕过读取器 (Android 11+)
     * 宝贝这里修复了路径转换问题，所有方法都要先转换路径再访问喵！
     */
    class DirectReadReader : Reader {
        
        override fun listFiles(path: String): List<FileItem> {
            return try {
                // 宝贝这里先把路径转换成 ZWC 绕过路径喵~
                val actualPath = toZWCPath(path)
                val dir = File(actualPath)
                if (!dir.exists() || !dir.isDirectory) {
                    Log.w(TAG, "DirectRead listFiles: path not accessible - $actualPath")
                    return emptyList()
                }
                dir.listFiles()?.map { file ->
                    FileItem(
                        name = file.name,
                        path = file.absolutePath,
                        isDirectory = file.isDirectory,
                        size = file.length(),
                        lastModified = file.lastModified()
                    )
                } ?: emptyList()
            } catch (e: Exception) {
                Log.e(TAG, "DirectRead listFiles failed", e)
                emptyList()
            }
        }
        
        override fun readFile(path: String): String? {
            return try {
                // 宝贝这里先把路径转换成 ZWC 绕过路径喵~
                val actualPath = toZWCPath(path)
                File(actualPath).readText()
            } catch (e: Exception) {
                Log.e(TAG, "DirectRead readFile failed", e)
                null
            }
        }
        
        override fun exists(path: String): Boolean {
            // 宝贝这里先把路径转换成 ZWC 绕过路径喵~
            val actualPath = toZWCPath(path)
            return File(actualPath).exists()
        }
        
        override fun getFileSize(path: String): Long {
            return try {
                // 宝贝这里先把路径转换成 ZWC 绕过路径喵~
                val actualPath = toZWCPath(path)
                File(actualPath).length()
            } catch (e: Exception) {
                0L
            }
        }
        
        override fun getFileModifiedTime(path: String): Long {
            return try {
                // 宝贝这里先把路径转换成 ZWC 绕过路径喵~
                val actualPath = toZWCPath(path)
                File(actualPath).lastModified()
            } catch (e: Exception) {
                0L
            }
        }
        
        override fun isAvailable(): Boolean {
            return ZWCHelper.isDirectReadAvailable()
        }
        
        override fun getMode(): ActivationMode = ActivationMode.DIRECT_READ
        
        override fun deleteDirectory(path: String): Boolean {
            return try {
                // 宝贝这里先把路径转换成 ZWC 绕过路径喵~
                val actualPath = toZWCPath(path)
                val dir = File(actualPath)
                if (dir.exists()) {
                    dir.deleteRecursively()
                } else {
                    true  // 目录不存在也算删除成功喵~
                }
            } catch (e: Exception) {
                Log.e(TAG, "DirectRead deleteDirectory failed", e)
                false
            }
        }
        
        /**
         * 将标准路径转换为 ZWC 绕过路径
         * 宝贝这里修复了，之前定义了但没被调用喵！
         */
        private fun toZWCPath(path: String): String {
            val zwcBase = ZWCHelper.getWorkingBypassPath()
            return if (zwcBase != null && path.startsWith(Path.DEVICE_BASE)) {
                // 替换基础路径为 ZWC 绕过路径
                path.replaceFirst(Path.DEVICE_BASE, zwcBase)
            } else if (zwcBase != null) {
                // 路径不包含 DEVICE_BASE，直接拼接
                "$zwcBase/$path"
            } else {
                // 没有可用的 ZWC 路径，返回原始路径
                Log.w(TAG, "toZWCPath: no working bypass path available, using original path")
                path
            }
        }
    }

    /**
     * SAF (Storage Access Framework) 读取器
     * 宝贝这里修复了路径处理问题喵！
     */
    class SAFReader(private val context: Context) : Reader {
        private var treeUri: Uri? = null
        private var treeDocumentId: String? = null
        
        init {
            val prefs = context.getSharedPreferences("fuck_ets100_prefs", Context.MODE_PRIVATE)
            val uriString = prefs.getString("saf_tree_uri", null)
            treeUri = uriString?.let { Uri.parse(it) }
            // 预先获取 tree document ID，避免重复查询
            treeDocumentId = treeUri?.let {
                try {
                    android.provider.DocumentsContract.getTreeDocumentId(it)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to get tree document ID", e)
                    null
                }
            }
        }
        
        override fun listFiles(path: String): List<FileItem> {
            if (treeUri == null || treeDocumentId == null) {
                Log.w(TAG, "SAF listFiles: treeUri or treeDocumentId is null")
                return emptyList()
            }
            return try {
                // 宝贝这里修复了，之前忽略 path 参数，总是用 treeUri 根目录喵！
                val targetUri = if (path == Path.DEVICE_BASE || path.isEmpty()) {
                    // 根目录
                    treeUri!!
                } else {
                    // 指定目录 - 构建对应的 SAF URI
                    pathToSAFUri(path) ?: treeUri!!
                }
                listFilesViaSAF(targetUri)
            } catch (e: Exception) {
                Log.e(TAG, "SAF listFiles failed", e)
                emptyList()
            }
        }
        
        override fun readFile(path: String): String? {
            val uri = pathToSAFUri(path) ?: run {
                Log.w(TAG, "SAF readFile: pathToSAFUri returned null for path: $path")
                return null
            }
            return readFileViaSAF(uri)
        }
        
        override fun exists(path: String): Boolean {
            val uri = pathToSAFUri(path) ?: return false
            return try {
                context.contentResolver.openInputStream(uri)?.close(); true
            } catch (e: Exception) {
                false
            }
        }
        
        override fun getFileSize(path: String): Long {
            val uri = pathToSAFUri(path) ?: return 0L
            return try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    // 宝贝这里使用更准确的文件大小获取方式喵~
                    val fd = context.contentResolver.openFileDescriptor(uri, "r")
                    fd?.use { it.statSize } ?: inputStream.available().toLong()
                } ?: 0L
            } catch (e: Exception) {
                Log.e(TAG, "SAF getFileSize failed", e)
                0L
            }
        }
        
        override fun getFileModifiedTime(path: String): Long {
            // 宝贝 SAF 不容易获取文件修改时间，返回 0 喵~
            return 0L
        }
        
        override fun isAvailable(): Boolean {
            return treeUri != null && treeDocumentId != null
        }
        
        override fun getMode(): ActivationMode = ActivationMode.SAF
        
        override fun deleteDirectory(path: String): Boolean {
            return try {
                // 宝贝使用 SAF 的 deleteDocument API 删除喵~
                val uri = pathToSAFUri(path)
                if (uri != null) {
                    try {
                        DocumentsContract.deleteDocument(context.contentResolver, uri)
                        true
                    } catch (e: Exception) {
                        // 如果 deleteDocument 失败，尝试用另一种方式
                        Log.w(TAG, "SAF deleteDocument failed, trying recursive delete")
                        deleteRecursivelyViaSAF(path)
                    }
                } else {
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "SAF deleteDirectory failed", e)
                false
            }
        }
        
        /**
         * 通过 SAF 递归删除目录
         * 宝贝这是备选方案喵~
         */
        private fun deleteRecursivelyViaSAF(path: String): Boolean {
            return try {
                val uri = pathToSAFUri(path)
                if (uri == null) return false
                
                // 先尝试获取目录的 document ID
                val documentId = try {
                    DocumentsContract.getDocumentId(uri)
                } catch (e: Exception) {
                    // 尝试用 child 方式获取
                    val treeDocId = DocumentsContract.getTreeDocumentId(treeUri!!)
                    "$treeDocId/${path.removePrefix(Path.DEVICE_BASE).removePrefix("/")}"
                }
                
                // 获取孩子 URI
                val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                    treeUri!!,
                    documentId
                )
                
                val cursor = context.contentResolver.query(
                    childrenUri,
                    arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID),
                    null, null, null
                )
                
                cursor?.use { c ->
                    while (c.moveToNext()) {
                        val childDocId = c.getString(0)
                        val childUri = DocumentsContract.buildDocumentUriUsingTree(
                            treeUri!!,
                            childDocId
                        )
                        try {
                            DocumentsContract.deleteDocument(context.contentResolver, childUri)
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to delete child: $childDocId")
                        }
                    }
                }
                true
            } catch (e: Exception) {
                Log.e(TAG, "SAF deleteRecursivelyViaSAF failed", e)
                false
            }
        }
        
        /**
         * 将标准路径转换为 SAF URI
         * 宝贝这里修复了路径处理逻辑喵！
         */
        private fun pathToSAFUri(path: String): Uri? {
            if (treeUri == null || treeDocumentId == null) {
                Log.w(TAG, "pathToSAFUri: treeUri or treeDocumentId is null")
                return null
            }
            
            try {
                // 计算相对路径
                val relativePath = if (path.startsWith(Path.DEVICE_BASE)) {
                    path.removePrefix(Path.DEVICE_BASE).removePrefix("/")
                } else if (path.startsWith("/")) {
                    path.removePrefix("/")
                } else {
                    path
                }
                
                if (relativePath.isEmpty()) {
                    // 相对路径为空，返回 treeUri 本身
                    return treeUri
                }
                
                // 构建 document URI
                // 宝贝注意：SAF 的 document ID 格式通常是 "tree:primary:path/to/dir"
                val documentUri = android.provider.DocumentsContract.buildDocumentUriUsingTree(
                    treeUri!!,
                    "$treeDocumentId:${relativePath.replace("/", "/")}"
                )
                
                Log.d(TAG, "pathToSAFUri: path=$path -> relativePath=$relativePath -> uri=$documentUri")
                return documentUri
            } catch (e: Exception) {
                Log.e(TAG, "pathToSAFUri failed: path=$path", e)
                return null
            }
        }
        
        /**
         * 列出指定 URI 目录的内容
         */
        private fun listFilesViaSAF(targetUri: Uri): List<FileItem> {
            val items = mutableListOf<FileItem>()
            try {
                // 获取 target URI 对应的 document ID
                val targetDocId = android.provider.DocumentsContract.getDocumentId(targetUri)
                
                // 构建子文档 URI
                val childrenUri = android.provider.DocumentsContract.buildChildDocumentsUriUsingTree(
                    treeUri!!,
                    targetDocId
                )
                
                Log.d(TAG, "listFilesViaSAF: querying childrenUri=$childrenUri for docId=$targetDocId")
                
                val cursor = context.contentResolver.query(
                    childrenUri,
                    arrayOf(
                        android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                        android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                        android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE,
                        android.provider.DocumentsContract.Document.COLUMN_SIZE,
                        android.provider.DocumentsContract.Document.COLUMN_LAST_MODIFIED
                    ),
                    null,
                    null,
                    null
                )
                
                cursor?.use {
                    while (it.moveToNext()) {
                        val docId = it.getString(0)
                        val name = it.getString(1)
                        val mimeType = it.getString(2)
                        val size = it.getLong(3)
                        val lastModified = it.getLong(4)
                        
                        val isDir = mimeType == android.provider.DocumentsContract.Document.MIME_TYPE_DIR
                        
                        items.add(FileItem(
                            name = name,
                            path = docId,
                            isDirectory = isDir,
                            size = size,
                            lastModified = lastModified
                        ))
                    }
                }
                
                Log.d(TAG, "listFilesViaSAF: found ${items.size} items")
            } catch (e: Exception) {
                Log.e(TAG, "listFilesViaSAF failed", e)
            }
            return items
        }
        
        private fun readFileViaSAF(uri: Uri): String? {
            return try {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            } catch (e: Exception) {
                Log.e(TAG, "readFileViaSAF failed", e)
                null
            }
        }
    }

    /**
     * 获取指定模式的阅读器
     */
    fun getReader(mode: ActivationMode, context: Context? = null): Reader {
        return when (mode) {
            ActivationMode.SHIZUKU -> ShizukuReader()
            ActivationMode.ROOT -> RootReader()
            ActivationMode.DIRECT_READ -> DirectReadReader()
            ActivationMode.SAF -> SAFReader(context!!)
            else -> throw IllegalArgumentException("Unsupported mode: $mode")
        }
    }

    /**
     * 检查指定模式是否可用
     */
    fun isModeAvailable(mode: ActivationMode, context: Context? = null): Boolean {
        return try {
            getReader(mode, context).isAvailable()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 读取文件内容(快捷方法)
     */
    fun readFile(mode: ActivationMode, path: String, context: Context? = null): String? {
        return try {
            getReader(mode, context).readFile(path)
        } catch (e: Exception) {
            Log.e(TAG, "readFile failed", e)
            null
        }
    }

    /**
     * 列出目录内容(快捷方法)
     */
    fun listFiles(mode: ActivationMode, path: String, context: Context? = null): List<FileItem> {
        return try {
            getReader(mode, context).listFiles(path)
        } catch (e: Exception) {
            Log.e(TAG, "listFiles failed", e)
            emptyList()
        }
    }
    
    /**
     * 删除目录(快捷方法)
     * 宝贝这是便捷方法喵~
     */
    fun deleteDirectory(mode: ActivationMode, path: String, context: Context? = null): Boolean {
        return try {
            getReader(mode, context).deleteDirectory(path)
        } catch (e: Exception) {
            Log.e(TAG, "deleteDirectory failed", e)
            false
        }
    }
}