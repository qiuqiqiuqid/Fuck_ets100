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
import java.io.FileInputStream
import java.io.InputStream
import java.nio.file.Files

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
    private const val LIST_FIELD_SEPARATOR = "\t"

    private fun shellQuote(value: String): String {
        return "'" + value.replace("'", "'\"'\"'") + "'"
    }

    private fun buildFastListCommand(path: String): String {
        val quotedPath = shellQuote(path)
        return """
            dir=$quotedPath
            [ -d "${'$'}dir" ] || exit 0
            for f in "${'$'}dir"/* "${'$'}dir"/.[!.]* "${'$'}dir"/..?*; do
              [ -e "${'$'}f" ] || continue
              name=${'$'}{f##*/}
              if [ "${'$'}name" = "." ] || [ "${'$'}name" = ".." ]; then continue; fi
              if [ -d "${'$'}f" ]; then type=d; else type=f; fi
              size=${'$'}(stat -c %s "${'$'}f" 2>/dev/null || echo 0)
              mtime=${'$'}(stat -c %Y "${'$'}f" 2>/dev/null || echo 0)
              mtime_text=${'$'}(stat -c %y "${'$'}f" 2>/dev/null | tr '\t' ' ')
              printf '%s\t%s\t%s\t%s\t%s\n' "${'$'}type" "${'$'}size" "${'$'}mtime" "${'$'}mtime_text" "${'$'}name"
            done
        """.trimIndent()
    }

    private fun buildFastContentFolderCommand(path: String): String {
        val quotedPath = shellQuote(path)
        return """
            dir=$quotedPath
            [ -d "${'$'}dir" ] || exit 0
            for f in "${'$'}dir"/* "${'$'}dir"/.[!.]* "${'$'}dir"/..?*; do
              [ -d "${'$'}f" ] || continue
              [ -f "${'$'}f/content.json" ] || continue
              name=${'$'}{f##*/}
              if [ "${'$'}name" = "." ] || [ "${'$'}name" = ".." ]; then continue; fi
              size=${'$'}(stat -c %s "${'$'}f/content.json" 2>/dev/null || echo 0)
              mtime=${'$'}(stat -c %Y "${'$'}f" 2>/dev/null || echo 0)
              mtime_text=${'$'}(stat -c %y "${'$'}f" 2>/dev/null | tr '\t' ' ')
              printf '%s\t%s\t%s\t%s\t%s\n' "d" "${'$'}size" "${'$'}mtime" "${'$'}mtime_text" "${'$'}name"
            done
        """.trimIndent()
    }

    private fun buildDirectorySizeCommand(path: String): String {
        val quotedPath = shellQuote(path)
        return "[ -d $quotedPath ] && du -sk $quotedPath 2>/dev/null | cut -f1 || echo 0"
    }

    internal fun parseDirectorySizeKib(output: String?): Long = output
        ?.lineSequence()
        ?.firstOrNull()
        ?.trim()
        ?.substringBeforeAnyWhitespace()
        ?.toLongOrNull()
        ?.coerceAtLeast(0L)
        ?.coerceAtMost(Long.MAX_VALUE / 1024L)
        ?.times(1024L)
        ?: 0L

    private fun String.substringBeforeAnyWhitespace(): String =
        indexOfFirst { it.isWhitespace() }
            .takeIf { it >= 0 }
            ?.let { substring(0, it) }
            ?: this

    private fun parseFastListOutput(output: String): List<FileItem> {
        return output.lineSequence()
            .map { it.trimEnd() }
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val parts = line.split(LIST_FIELD_SEPARATOR, limit = 5)
                if (parts.size < 4) return@mapNotNull null
                val name = if (parts.size >= 5) parts[4] else parts[3]
                if (name == "." || name == "..") return@mapNotNull null
                FileItem(
                    name = name,
                    path = "",
                    isDirectory = parts[0] == "d",
                    size = parts[1].toLongOrNull() ?: 0L,
                    lastModified = parseShellStatTimeMillis(
                        secondsText = parts[2],
                        fullTimeText = parts.getOrNull(3).takeIf { parts.size >= 5 }
                    )
                )
            }
            .toList()
    }

    private fun parseShellStatTimeMillis(secondsText: String, fullTimeText: String?): Long {
        val secondsMillis = (secondsText.toLongOrNull() ?: 0L) * 1000L
        val text = fullTimeText?.trim().orEmpty()
        if (text.isEmpty()) return secondsMillis

        val match = Regex(
            """^(\d{4}-\d{2}-\d{2})[ T](\d{2}:\d{2}:\d{2})(?:\.(\d+))?\s+([+-]\d{4}).*$"""
        ).find(text) ?: return secondsMillis

        return try {
            val baseText = "${match.groupValues[1]} ${match.groupValues[2]} ${match.groupValues[4]}"
            val baseMillis = java.text.SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss Z",
                java.util.Locale.US
            ).parse(baseText)?.time ?: secondsMillis
            val fraction = match.groupValues.getOrNull(3).orEmpty()
            val millis = fraction.padEnd(3, '0').take(3).toLongOrNull() ?: 0L
            baseMillis + millis
        } catch (e: Exception) {
            secondsMillis
        }
    }

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

        fun openInputStream(path: String): InputStream?

        fun isSymbolicLink(path: String): Boolean
        
        /**
         * 检查文件是否存在
         */
        fun exists(path: String): Boolean
        
        /**
         * 获取文件大小
         */
        fun getFileSize(path: String): Long

        /**
         * 获取目录中全部文件的总字节数。
         * 目录不存在或无法读取时返回 0。
         */
        fun getDirectorySize(path: String): Long
        
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

    interface ContentFolderReader {
        fun listContentFolders(path: String): List<FileItem>
    }

    /**
     * Shizuku 文件读取器
     * 使用 Shizuku API 执行命令，而非 Root 方式
     */
    class ShizukuReader : Reader, ContentFolderReader {
        override fun listFiles(path: String): List<FileItem> {
            return try {
                parseFastListOutput(execShizukuCommand(buildFastListCommand(path)) ?: "")
            } catch (e: Exception) {
                Log.e(TAG, "Shizuku listFiles failed", e)
                emptyList()
            }
        }

        override fun listContentFolders(path: String): List<FileItem> {
            return try {
                parseFastListOutput(execShizukuCommand(buildFastContentFolderCommand(path)) ?: "")
            } catch (e: Exception) {
                Log.e(TAG, "Shizuku listContentFolders failed", e)
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

        override fun openInputStream(path: String): InputStream? =
            ShizukuManager.openCommandInputStream("cat ${shellQuote(path)}")

        override fun isSymbolicLink(path: String): Boolean =
            execShizukuCommand("test -L ${shellQuote(path)} && echo link")?.trim() == "link"
        
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

        override fun getDirectorySize(path: String): Long = try {
            parseDirectorySizeKib(execShizukuCommand(buildDirectorySizeCommand(path)))
        } catch (e: Exception) {
            Log.e(TAG, "Shizuku getDirectorySize failed", e)
            0L
        }
        
        override fun getFileModifiedTime(path: String): Long {
            return try {
                // 宝贝需要先转义路径中的引号喵~ 参考 EAuxiliary 的实现
                val sanitizedPath = path.replace("\"", "\\\"")
                
                Log.d(TAG, "╔═══ Shizuku 时间获取调试 ═══")
                Log.d(TAG, "║ 原始路径: $path")
                Log.d(TAG, "║ 转义路径: $sanitizedPath")
                
                // 方式1：使用 stat -c %Y 获取 Unix 时间戳（秒级）
                val statCmd = "stat -c %Y \"$sanitizedPath\""
                Log.d(TAG, "║ 执行命令1: $statCmd")
                val statResult = execShizukuCommand(statCmd)
                Log.d(TAG, "║ stat -c %Y 结果: '$statResult'")
                var timestamp = statResult?.trim()?.toLongOrNull() ?: 0L
                Log.d(TAG, "║ 解析时间戳(秒): $timestamp")
                
                // 方式2：如果 stat 失败，尝试 ls -ld 备选方案
                if (timestamp == 0L) {
                    val lsCmd = "ls -ld \"$sanitizedPath\""
                    Log.d(TAG, "║ 执行命令2 (备选): $lsCmd")
                    val lsResult = execShizukuCommand(lsCmd)
                    Log.d(TAG, "║ ls -ld 结果: $lsResult")
                    
                    // 解析 ls -ld 输出中的时间
                    // 格式: drwxrwxr-x 3 u0_a263 u0_a263 4096 2024-12-01 15:30 path
                    lsResult?.let { output ->
                        val lines = output.trim().split("\n")
                        if (lines.isNotEmpty()) {
                            val line = lines[0].trim()
                            val parts = line.split("\\s+".toRegex())
                            Log.d(TAG, "║ ls -ld 解析 parts 数: ${parts.size}")
                            if (parts.size >= 8) {
                                // parts[5]=月份, parts[6]=日期, parts[7]=时间或年份
                                val monthStr = parts[5]
                                val dayStr = parts[6]
                                val timeStr = parts[7]
                                Log.d(TAG, "║ 月份: $monthStr, 日期: $dayStr, 时间: $timeStr")
                                
                                val month = parseMonth(monthStr)
                                val day = dayStr.toIntOrNull() ?: 1
                                
                                var year = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                                val hour: Int
                                val minute: Int
                                
                                if (timeStr.contains(":")) {
                                    // 格式是时间 (HH:MM)，表示当前年份
                                    val timeParts = timeStr.split(":")
                                    hour = timeParts[0].toIntOrNull() ?: 0
                                    minute = timeParts[1].toIntOrNull() ?: 0
                                } else {
                                    // 格式是年份
                                    year = timeStr.toIntOrNull() ?: year
                                    hour = 0
                                    minute = 0
                                }
                                
                                Log.d(TAG, "║ 解析后: 年=$year, 月=$month, 日=$day, 时=$hour, 分=$minute")
                                
                                try {
                                    val calendar = java.util.Calendar.getInstance()
                                    calendar.set(year, month - 1, day, hour, minute, 0)
                                    // ls -ld 返回的时间已经是可读的，不需要除以 1000
                                    timestamp = calendar.timeInMillis / 1000
                                    Log.d(TAG, "║ 计算时间戳(秒): $timestamp")
                                } catch (e: Exception) {
                                    Log.e(TAG, "║ 解析 ls -ld 时间失败", e)
                                }
                            } else {
                                Log.d(TAG, "║ parts 数量不足，无法解析时间")
                            }
                        }
                    }
                }
                
                // 重要：转换为毫秒级，与 Java File.lastModified() 保持一致！
                val timestampMs = timestamp * 1000
                Log.d(TAG, "║ 最终时间戳(毫秒): $timestampMs")
                Log.d(TAG, "╚═══ 时间获取结束 ═══")
                return timestampMs
            } catch (e: Exception) {
                Log.e(TAG, "getFileModifiedTime failed: path=$path", e)
                0L
            }
        }
        
        /**
         * 解析月份字符串为数字
         * 宝贝这是备选方案喵~
         */
        private fun parseMonth(monthStr: String): Int {
            return when (monthStr.lowercase()) {
                "Jan" -> 1
                "Feb" -> 2
                "Mar" -> 3
                "Apr" -> 4
                "May" -> 5
                "Jun" -> 6
                "Jul" -> 7
                "Aug" -> 8
                "Sep" -> 9
                "Oct" -> 10
                "Nov" -> 11
                "Dec" -> 12
                else -> 1
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
    class RootReader : Reader, ContentFolderReader {
        override fun listFiles(path: String): List<FileItem> {
            return try {
                parseFastListOutput(RootManager.execAsRoot(buildFastListCommand(path)) ?: "")
            } catch (e: Exception) {
                Log.e(TAG, "Root listFiles failed", e)
                emptyList()
            }
        }

        override fun listContentFolders(path: String): List<FileItem> {
            return try {
                parseFastListOutput(RootManager.execAsRoot(buildFastContentFolderCommand(path)) ?: "")
            } catch (e: Exception) {
                Log.e(TAG, "Root listContentFolders failed", e)
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

        override fun openInputStream(path: String): InputStream? =
            RootManager.openInputStreamAsRoot("cat ${shellQuote(path)}")

        override fun isSymbolicLink(path: String): Boolean =
            RootManager.execAsRoot("test -L ${shellQuote(path)} && echo link")?.trim() == "link"
        
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

        override fun getDirectorySize(path: String): Long = try {
            parseDirectorySizeKib(RootManager.execAsRoot(buildDirectorySizeCommand(path)))
        } catch (e: Exception) {
            Log.e(TAG, "Root getDirectorySize failed", e)
            0L
        }
        
        override fun getFileModifiedTime(path: String): Long {
            return try {
                val sanitizedPath = path.replace("\"", "\\\"")
                val result = RootManager.execAsRoot("stat -c %Y \"$sanitizedPath\"")
                val timestamp = result?.trim()?.toLongOrNull() ?: 0L
                // 重要：转换为毫秒级，与 Java File.lastModified() 保持一致！
                timestamp * 1000
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
    class DirectReadReader : Reader, ContentFolderReader {
        
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

        override fun listContentFolders(path: String): List<FileItem> {
            return try {
                val actualPath = toZWCPath(path)
                val dir = File(actualPath)
                if (!dir.exists() || !dir.isDirectory) {
                    return emptyList()
                }
                dir.listFiles()
                    ?.filter { it.isDirectory && File(it, "content.json").isFile }
                    ?.map { file ->
                        FileItem(
                            name = file.name,
                            path = file.absolutePath,
                            isDirectory = true,
                            size = File(file, "content.json").length(),
                            lastModified = file.lastModified()
                        )
                    }
                    ?: emptyList()
            } catch (e: Exception) {
                Log.e(TAG, "DirectRead listContentFolders failed", e)
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

        override fun openInputStream(path: String): InputStream? {
            return runCatching { FileInputStream(toZWCPath(path)) }
                .onFailure { Log.e(TAG, "DirectRead openInputStream failed: $path", it) }
                .getOrNull()
        }

        override fun isSymbolicLink(path: String): Boolean =
            runCatching { Files.isSymbolicLink(File(toZWCPath(path)).toPath()) }.getOrDefault(false)
        
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

        override fun getDirectorySize(path: String): Long = try {
            File(toZWCPath(path)).walkTopDown()
                .filter { it.isFile }
                .sumOf { it.length().coerceAtLeast(0L) }
        } catch (e: Exception) {
            Log.e(TAG, "DirectRead getDirectorySize failed", e)
            0L
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
     * 获取指定模式的阅读器
     */
    fun getReader(mode: ActivationMode, context: Context? = null): Reader {
        return when (mode) {
            ActivationMode.SHIZUKU -> ShizukuReader()
            ActivationMode.ROOT -> RootReader()
            ActivationMode.DIRECT_READ -> DirectReadReader()
            else -> throw IllegalArgumentException("Unsupported mode: $mode")
        }
    }

    /**
     * 检查指定模式是否可用
     * @param forceReadMode 如果为 true，在 DIRECT_READ 模式下跳过权限检查
     */
    fun isModeAvailable(mode: ActivationMode, context: Context? = null, forceReadMode: Boolean = false): Boolean {
        return try {
            // 强执读取模式：跳过权限检查，直接认为模式可用
            if (forceReadMode && mode == ActivationMode.DIRECT_READ) {
                Log.d(TAG, "isModeAvailable: 强执模式，跳过权限检查")
                return true
            }
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
