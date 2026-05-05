package com.shuaiqiu.fuckets100

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 云端首页
 * 喵~ 显示作业列表并提供下载解析功能喵！
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudHomeScreen(
    navController: NavHostController,
    onShowAnswer: (ETS100AnswerReader.Paper) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // 作业列表状态
    var homeworkList by remember { mutableStateOf<List<ETS100ApiClient.HomeworkInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // 下载状态（用于显示进度）
    var downloadingHomework by remember { mutableStateOf<String?>(null) }
    
    // 加载作业列表
    fun loadHomeworkList() {
        scope.launch {
            isLoading = true
            errorMessage = null
            
            try {
                // 检查登录状态
                if (!ETS100AuthManager.isLoggedIn(context)) {
                    errorMessage = "未登录，请先登录"
                    isLoading = false
                    return@launch
                }
                
                val token = ETS100AuthManager.getToken(context)
                val parentId = ETS100AuthManager.getParentAccountId(context)
                
                if (token == null || parentId == null) {
                    errorMessage = "登录信息不完整，请重新登录"
                    isLoading = false
                    return@launch
                }
                
                // 调用 API 获取作业列表
                val result = ETS100ApiClient.getHomeworkList(token, parentId)
                
                result.onSuccess { response ->
                    homeworkList = response.homeworks
                    Log.d("CloudHomeScreen", "获取到 ${homeworkList.size} 个作业")
                }.onFailure { e ->
                    errorMessage = e.message ?: "获取作业列表失败"
                    Log.e("CloudHomeScreen", "获取作业列表失败", e)
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "发生错误"
                Log.e("CloudHomeScreen", "加载作业列表异常", e)
            }
            
            isLoading = false
        }
    }
    
    // 初始加载
    LaunchedEffect(Unit) {
        loadHomeworkList()
    }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("云端作业列表", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    // 刷新按钮
                    IconButton(
                        onClick = {
                            isRefreshing = true
                            loadHomeworkList()
                            isRefreshing = false
                        },
                        enabled = !isLoading && !isRefreshing
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    // 加载中
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                errorMessage != null -> {
                    // 错误状态
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { loadHomeworkList() }) {
                            Text("重试")
                        }
                    }
                }
                
                homeworkList.isEmpty() -> {
                    // 空列表
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Inbox,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "暂无作业",
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                
                else -> {
                    // 作业列表
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(homeworkList) { homework ->
                            HomeworkCard(
                                homework = homework,
                                isDownloading = downloadingHomework == homework.name,
                                onDownload = { content ->
                                    scope.launch {
                                        downloadingHomework = homework.name
                                        downloadAndParseHomework(
                                            context = context,
                                            homework = homework,
                                            content = content,
                                            baseUrl = ETS100ApiClient.Config.CDN_BASE_URL,
                                            onSuccess = { paper ->
                                                downloadingHomework = null
                                                onShowAnswer(paper)
                                            },
                                            onError = { error ->
                                                downloadingHomework = null
                                                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                            }
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 作业卡片组件
 */
@Composable
private fun HomeworkCard(
    homework: ETS100ApiClient.HomeworkInfo,
    isDownloading: Boolean,
    onDownload: (ETS100ApiClient.HomeworkContent) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 作业标题
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Book,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = homework.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(Modifier.height(8.dp))
            
            // 题型列表
            Text(
                text = "${homework.contents.size} 个题型",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // 题型标签
            if (homework.contents.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    homework.contents.take(3).forEach { content ->
                        SuggestionChip(
                            onClick = { },
                            label = { Text(content.groupName, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                    if (homework.contents.size > 3) {
                        SuggestionChip(
                            onClick = { },
                            label = { Text("+${homework.contents.size - 3}", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            // 下载按钮
            if (isDownloading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("下载中...", style = MaterialTheme.typography.bodySmall)
                }
            } else {
                // 显示每个题型的下载按钮
                homework.contents.forEachIndexed { index, content ->
                    if (index > 0) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = content.groupName,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (content.url.isNotEmpty()) {
                            Button(
                                onClick = { onDownload(content) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("查看答案", style = MaterialTheme.typography.labelMedium)
                            }
                        } else {
                            Text(
                                text = "无资源",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 下载并解析作业
 * 喵~ 这个函数处理 ZIP 下载、密码生成、解压和解析喵！
 */
private suspend fun downloadAndParseHomework(
    context: android.content.Context,
    homework: ETS100ApiClient.HomeworkInfo,
    content: ETS100ApiClient.HomeworkContent,
    baseUrl: String,
    onSuccess: (ETS100AnswerReader.Paper) -> Unit,
    onError: (String) -> Unit
) {
    withContext(Dispatchers.IO) {
        try {
            Log.d("CloudHomeScreen", "开始下载: ${content.url}")
            
            // 构建下载 URL
            val downloadUrl = if (content.url.startsWith("http")) {
                content.url
            } else {
                baseUrl + content.url
            }
            
            // 下载 ZIP 文件
            val zipData = downloadFile(downloadUrl)
            if (zipData == null) {
                withContext(Dispatchers.Main) {
                    onError("下载失败，请检查网络连接")
                }
                return@withContext
            }
            
            Log.d("CloudHomeScreen", "下载完成，开始生成密码")
            
            // 生成解压密码
            val password = ZipPasswordGenerator.generatePasswordFromBytes(zipData)
            if (password == null) {
                withContext(Dispatchers.Main) {
                    onError("密码生成失败，文件可能已损坏")
                }
                return@withContext
            }
            
            Log.d("CloudHomeScreen", "密码生成成功: ${password.take(8)}...")
            
            // 解压并解析
            // 喵~ 这里简化处理，实际可能需要先将 zipData 写入临时文件再解压
            // 因为 ZipFile 需要文件路径而不是字节数组
            
            withContext(Dispatchers.Main) {
                // 暂时显示一个提示，因为完整的解压解析需要更多处理
                onError("功能开发中，ZIP 解压需要额外实现")
            }
            
        } catch (e: Exception) {
            Log.e("CloudHomeScreen", "下载解析失败", e)
            withContext(Dispatchers.Main) {
                onError("处理失败: ${e.message}")
            }
        }
    }
}

/**
 * 下载文件到字节数组
 * 喵~ 使用 HttpURLConnection 实现喵！
 */
private fun downloadFile(urlString: String): ByteArray? {
    return try {
        val url = java.net.URL(urlString)
        val connection = url.openConnection() as java.net.HttpURLConnection
        
        connection.apply {
            requestMethod = "GET"
            connectTimeout = 30000
            readTimeout = 30000
            setRequestProperty("User-Agent", "Mozilla/5.0")
        }
        
        val responseCode = connection.responseCode
        if (responseCode != java.net.HttpURLConnection.HTTP_OK) {
            Log.e("CloudHomeScreen", "HTTP 错误: $responseCode")
            return null
        }
        
        // 读取数据
        val inputStream = connection.inputStream
        val buffer = java.io.ByteArrayOutputStream()
        val data = ByteArray(4096)
        var count: Int
        while (inputStream.read(data).also { count = it } != -1) {
            buffer.write(data, 0, count)
        }
        inputStream.close()
        buffer.toByteArray()
    } catch (e: Exception) {
        Log.e("CloudHomeScreen", "下载失败: ${e.message}", e)
        null
    }
}