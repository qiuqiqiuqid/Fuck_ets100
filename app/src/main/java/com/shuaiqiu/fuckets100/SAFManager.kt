package com.shuaiqiu.fuckets100

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

/**
 * SAF (Storage Access Framework) 管理器
 * 用于管理 SAF 存储访问框架的文件夹选择和文件列表获取
 */
object SAFManager {

    private const val TAG = "SAFManager"
    
    // 默认目标路径 - Android/data 目录
    const val DEFAULT_TARGET_PATH = "/storage/emulated/0/Android/data"
    
    // Preference keys
    private const val PREFS_NAME = "fe_settings"
    private const val KEY_SAF_DIRECTORY_URI = "saf_directory_uri"
    private const val KEY_SAF_DIRECTORY_NAME = "saf_directory_name"
    
    private lateinit var prefs: SharedPreferences
    private lateinit var contentResolver: ContentResolver
    
    // 文件夹选择回调
    private var onFolderSelectedCallback: (() -> Unit)? = null
    
    // 初始选择的 URI
    private var initialUri: Uri? = null
    
    /**
     * 设置文件夹选择完成后的回调
     */
    fun setOnFolderSelectedCallback(callback: (() -> Unit)?) {
        onFolderSelectedCallback = callback
    }
    
    /**
     * 请求选择文件夹（使用默认目标路径）
     */
    fun requestFolderSelection() {
        requestFolderSelection(DEFAULT_TARGET_PATH)
    }
    
    /**
     * 请求选择特定目标路径的文件夹
     * @param targetPath 目标文件夹路径
     */
    fun requestFolderSelection(targetPath: String) {
        // 将目标路径转换为 SAF URI
        initialUri = pathToSAFUri(targetPath)
        onFolderSelectedCallback?.invoke()
    }
    
    /**
     * 获取初始选择的 URI，用于 SAF picker
     */
    fun getInitialUri(): Uri? {
        return initialUri
    }
    
    /**
     * 清除初始选择的 URI
     */
    fun clearInitialUri() {
        initialUri = null
    }
    
    /**
     * 将目标路径转换为 SAF URI
     * 路径格式: /storage/emulated/0/Android/data/com.ets100.secondary/files/Download/ETS_secondary
     * 转换为: content://com.android.externalstorage.documents/tree/primary:Android/data/com.ets100.secondary/files/Download/ETS_secondary
     */
    private fun pathToSAFUri(path: String): Uri? {
        return try {
            // 移除 /storage/emulated/0/ 前缀
            val relativePath = path.removePrefix("/storage/emulated/0/")
            
            // 构建 document ID 格式: primary:Android/data/com.ets100.secondary/files/Download/ETS_secondary
            val documentId = "primary:$relativePath"
            
            // 使用 DocumentsContract 构建 tree URI
            DocumentsContract.buildDocumentUri(
                "com.android.externalstorage.documents",
                documentId
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to convert path to SAF URI: $path", e)
            null
        }
    }
    
    /**
     * 初始化管理器
     */
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        contentResolver = context.contentResolver
    }
    
    /**
     * 检查 SAF 存储访问是否已配置且可访问
     */
    fun isConfigured(): Boolean {
        val uri = getSavedUri() ?: return false
        // 验证 URI 是否仍然可以访问
        return try {
            contentResolver.openInputStream(uri)?.close()
            true
        } catch (e: Exception) {
            Log.w(TAG, "SAF URI no longer accessible: $e")
            false
        }
    }
    
    /**
     * 检查保存的目录是否是 Android/data 下的正确目录
     * @return true 如果目录是 /storage/emulated/0/Android/data 下的
     */
    fun isCorrectDirectory(): Boolean {
        val uri = getSavedUri() ?: return false
        val docId = try { DocumentsContract.getTreeDocumentId(uri) } catch (e: Exception) { return false }
        // 检查 document ID 是否以 "primary:Android/data" 开头
        return docId.startsWith("primary:Android/data") || docId == "primary:Android/data"
    }
    
    /**
     * 获取保存的目录 URI
     */
    fun getSavedUri(): Uri? {
        val uriString = prefs.getString(KEY_SAF_DIRECTORY_URI, null) ?: return null
        return try {
            Uri.parse(uriString)
        } catch (e: Exception) {
            Log.e(TAG, "Invalid saved URI: $uriString", e)
            null
        }
    }
    
    /**
     * 获取保存的目录名称
     */
    fun getSavedDirectoryName(): String? {
        return prefs.getString(KEY_SAF_DIRECTORY_NAME, null)
    }
    
    /**
     * 保存选择的目录
     * @param uri Document tree URI
     * @param displayName 显示名称
     */
    fun saveDirectory(uri: Uri, displayName: String?) {
        prefs.edit {
            putString(KEY_SAF_DIRECTORY_URI, uri.toString())
            putString(KEY_SAF_DIRECTORY_NAME, displayName ?: extractDisplayName(uri))
        }
        
        // 尝试获取持久化 URI 权限
        try {
            // 获取 READ 和 WRITE 权限
            val readPerm = Intent.FLAG_GRANT_READ_URI_PERMISSION
            val writePerm = Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            contentResolver.takePersistableUriPermission(uri, readPerm or writePerm)
            Log.i(TAG, "Successfully taken persistable URI permission")
        } catch (e: SecurityException) {
            Log.w(TAG, "Could not take persistable permission (may already have it): $e")
        }
    }
    
    /**
     * 从 URI 中提取显示名称
     */
    private fun extractDisplayName(uri: Uri): String {
        var displayName = "已授权的目录"
        
        try {
            // 尝试从 document URI 获取显示名称
            val cursor: android.database.Cursor? = contentResolver.query(
                uri,
                arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null, null, null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        val result = it.getString(nameIndex)
                        if (!result.isNullOrEmpty()) {
                            displayName = result
                            return displayName
                        }
                    }
                }
            }
            
            // 如果获取失败，从 tree URI 中提取目录名
            val docId = DocumentsContract.getTreeDocumentId(uri)
            val parts = docId.split(":")
            if (parts.size >= 2) {
                displayName = parts[1]
            } else if (parts.isNotEmpty()) {
                displayName = parts[0]
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not extract display name: $e")
        }
        
        return displayName
    }
    
    /**
     * 清除保存的目录信息
     */
    fun clearDirectory() {
        // 释放持久化 URI 权限
        getSavedUri()?.let { uri ->
            try {
                val readPerm = Intent.FLAG_GRANT_READ_URI_PERMISSION
                val writePerm = Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                contentResolver.releasePersistableUriPermission(uri, readPerm or writePerm)
            } catch (e: Exception) {
                Log.w(TAG, "Could not release permission: $e")
            }
        }
        
        prefs.edit {
            remove(KEY_SAF_DIRECTORY_URI)
            remove(KEY_SAF_DIRECTORY_NAME)
        }
    }
    
    /**
     * 列出授权目录下的文件
     * @return 文件信息列表，返回 Pair(文件名, 是否为目录)
     */
    fun listFiles(): List<FileInfo> {
        val uri = getSavedUri() ?: return emptyList()
        
        val files = mutableListOf<FileInfo>()
        
        try {
            // 构建查询子文档的 URI
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                uri,
                DocumentsContract.getTreeDocumentId(uri)
            )
            
            val cursor: android.database.Cursor? = contentResolver.query(
                childrenUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                    DocumentsContract.Document.COLUMN_SIZE,
                    DocumentsContract.Document.COLUMN_LAST_MODIFIED
                ),
                null, null,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME + " ASC"
            )
            
            cursor?.use {
                val idIndex = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIndex = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIndex = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeIndex = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)
                val dateIndex = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                
                while (it.moveToNext()) {
                    val documentId = it.getString(idIndex)
                    val name = it.getString(nameIndex)
                    val mimeType = it.getString(mimeIndex)
                    val size = it.getLong(sizeIndex)
                    val lastModified = it.getLong(dateIndex)
                    
                    val isDirectory = DocumentsContract.Document.MIME_TYPE_DIR == mimeType
                    
                    files.add(FileInfo(
                        name = name,
                        isDirectory = isDirectory,
                        size = size,
                        lastModified = lastModified,
                        documentUri = DocumentsContract.buildDocumentUriUsingTree(uri, documentId)
                    ))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error listing files: $e", e)
        }
        
        return files
    }
    
    /**
     * 文件信息数据类
     */
    data class FileInfo(
        val name: String,
        val isDirectory: Boolean,
        val size: Long,
        val lastModified: Long,
        val documentUri: Uri
    ) {
        /**
         * 获取可读的文件大小字符串
         */
        fun getReadableSize(): String {
            return when {
                size < 1024 -> "$size B"
                size < 1024 * 1024 -> "${size / 1024} KB"
                size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)} MB"
                else -> "${size / (1024 * 1024 * 1024)} GB"
            }
        }
        
        /**
         * 获取文件扩展名
         */
        fun getExtension(): String {
            val dotIndex = name.lastIndexOf('.')
            return if (dotIndex > 0 && dotIndex < name.length - 1) {
                name.substring(dotIndex + 1).lowercase()
            } else {
                ""
            }
        }
    }
}

/**
 * Composable 状态管理 - SAF 状态
 */
@Composable
fun rememberSAFState(): SAFState {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var isConfigured by remember { mutableStateOf(SAFManager.isConfigured()) }
    var directoryName by remember { mutableStateOf(SAFManager.getSavedDirectoryName()) }
    
    // 生命周期监听
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isConfigured = SAFManager.isConfigured()
                directoryName = SAFManager.getSavedDirectoryName()
            }
        }
        
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    return SAFState(
        isConfigured = isConfigured,
        directoryName = directoryName,
        files = if (isConfigured) SAFManager.listFiles() else emptyList()
    )
}

/**
 * SAF 状态数据类
 */
data class SAFState(
    val isConfigured: Boolean,
    val directoryName: String?,
    val files: List<SAFManager.FileInfo>
)
