package com.shuaiqiu.fuckets100

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * ETS100 API 客户端
 * 负责所有网络请求和签名生成喵~
 * 
 * API 文档参考: ets读取/API_DOC.md
 */
object ETS100ApiClient {

    private const val TAG = "ETS100ApiClient"

    // ============================================================================
    // API 配置
    // ============================================================================
    
    object Config {
        const val API_BASE_URL = "https://api.ets100.com"
        const val CDN_BASE_URL = "https://cdn.subject.ets100.com"
        const val PID = "grlx"
        const val SECRET_KEY = "555ffbe95ccf4e9535a110170b445ab8"
        const val TIMEOUT_MS = 30000  // 30秒超时
    }

    // ============================================================================
    // API 端点
    // ============================================================================
    
    object Endpoints {
        const val LOGIN = "/user/login"
        const val ECARD_LIST = "/m/ecard/list"
        const val HOMEWORK_LIST = "/g/homework/list"
    }

    // ============================================================================
    // 请求配置常量
    // ============================================================================
    
    object RequestConfig {
        const val DEFAULT_SN = "test"
        const val DEFAULT_VERSION = "3"
        const val DEFAULT_SYSTEM = "4"
        const val DEFAULT_GLOBAL_CLIENT_VERSION = "5.2.1"
        const val DEFAULT_DEVICE_NAME = "DESKTOP"
        const val DEFAULT_LOCAL_IP = "127.0.0.1"
    }

    // ============================================================================
    // 签名生成
    // ============================================================================
    
    /**
     * 生成 API 签名
     * sign_string = PID + timestamp + content + SECRET_KEY
     * signature = MD5(sign_string)
     */
    fun generateSign(timestamp: Long, bodyBase64: String): String {
        val signString = Config.PID + timestamp.toString() + bodyBase64 + Config.SECRET_KEY
        return md5(signString)
    }

    /**
     * MD5 哈希
     */
    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    // ============================================================================
    // HTTP 请求
    // ============================================================================
    
    /**
     * 发送 POST 请求
     * @param endpoint API 端点
     * @param bodyData 请求体数据（Map）
     * @return 响应 JSON 字符串
     */
    private suspend fun postRequest(endpoint: String, bodyData: Map<String, Any>): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = URL(Config.API_BASE_URL + endpoint)
            val connection = url.openConnection() as HttpURLConnection
            
            connection.apply {
                requestMethod = "POST"
                connectTimeout = Config.TIMEOUT_MS
                readTimeout = Config.TIMEOUT_MS
                doOutput = true
                doInput = true
                setRequestProperty("Host", "api.ets100.com")
                setRequestProperty("User-Agent", "libcurl-agent/1.0")
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                setRequestProperty("Accept", "*/*")
            }

            // 构建请求体
            val timestamp = System.currentTimeMillis() / 1000
            val bodyJson = bodyData.toJson()
            val bodyBase64 = Base64.encodeToString(bodyJson.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            val sign = generateSign(timestamp, bodyBase64)

            // 构建完整的请求数据
            val requestData = buildString {
                append("body=").append(java.net.URLEncoder.encode(bodyBase64, "UTF-8"))
                append("&head=").append(java.net.URLEncoder.encode("""
                    {
                        "version": "1.0",
                        "sign": "$sign",
                        "pid": "${Config.PID}",
                        "time": $timestamp
                    }
                """.trimIndent(), "UTF-8"))
            }

            // 发送请求
            connection.outputStream.use { os ->
                os.write(requestData.toByteArray(Charsets.UTF_8))
            }

            // 读取响应
            val responseCode = connection.responseCode
            val responseBody = connection.inputStream.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { br ->
                    br.readText()
                }
            }

            Log.d(TAG, "POST $endpoint -> $responseCode")
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                Result.success(responseBody)
            } else {
                Log.e(TAG, "HTTP Error: $responseCode, body: $responseBody")
                Result.failure(Exception("HTTP Error: $responseCode"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Request failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ============================================================================
    // API 方法
    // ============================================================================

    /**
     * 用户登录
     * POST /user/login
     * 
     * @param phone 手机号
     * @param password 密码（明文）
     * @param deviceCode 机器码
     * @return 登录结果，包含 token
     */
    suspend fun login(phone: String, password: String, deviceCode: String): Result<LoginResponse> {
        Log.d(TAG, "login: phone=$phone, deviceCode=$deviceCode")
        
        val bodyData = mapOf(
            "r" to "user/login",
            "params" to mapOf(
                "sn" to RequestConfig.DEFAULT_SN,
                "phone" to phone,
                "password" to password,
                "device_code" to deviceCode,
                "device_name" to RequestConfig.DEFAULT_DEVICE_NAME,
                "version" to RequestConfig.DEFAULT_VERSION,
                "local_ip" to RequestConfig.DEFAULT_LOCAL_IP,
                "system" to RequestConfig.DEFAULT_SYSTEM,
                "global_client_version" to RequestConfig.DEFAULT_GLOBAL_CLIENT_VERSION,
                "sign_response" to 1
            )
        )

        return postRequest(Endpoints.LOGIN, bodyData).mapCatching { responseBody ->
            Log.d(TAG, "===== 登录 API 响应 =====")
            Log.d(TAG, "原始响应: $responseBody")
            
            // 解析响应
            val json = responseBody.parseJson()
            val bodyObj = json.optJSONObject("body")
            
            Log.d(TAG, "body 对象: $bodyObj")
            Log.d(TAG, "body 长度: ${bodyObj?.length() ?: "null"}")
            
            val token = bodyObj?.optString("token") ?: ""
            Log.d(TAG, "解析到的 token: ${if (token.isEmpty()) "(空)" else token.take(20) + "..."}")
            
            if (token.isEmpty()) {
                val errorMsg = bodyObj?.optString("msg") ?: "登录失败"
                Log.e(TAG, "登录失败原因: $errorMsg")
                // 打印整个 body 的所有 key-value
                bodyObj?.let { obj ->
                    Log.d(TAG, "----- body 所有字段 -----")
                    obj.keys().forEach { key ->
                        Log.d(TAG, "  $key = ${obj.opt(key)}")
                    }
                }
                throw Exception(errorMsg)
            }
            
            Log.i(TAG, "✓ 登录成功！")
            LoginResponse(token = token)
        }
    }

    /**
     * 获取父账户ID列表
     * POST /m/ecard/list
     * 
     * @param token 登录返回的 token
     * @return 父账户 ID
     */
    suspend fun getEcardList(token: String): Result<String> {
        Log.d(TAG, "getEcardList")
        
        val bodyData = mapOf(
            "r" to "m/ecard/list",
            "params" to mapOf(
                "sn" to RequestConfig.DEFAULT_SN,
                "token" to token,
                "version" to RequestConfig.DEFAULT_VERSION,
                "system" to RequestConfig.DEFAULT_SYSTEM,
                "global_client_version" to RequestConfig.DEFAULT_GLOBAL_CLIENT_VERSION,
                "sign_response" to 1
            )
        )

        return postRequest(Endpoints.ECARD_LIST, bodyData).mapCatching { responseBody ->
            Log.d(TAG, "===== 父账户 API 响应 =====")
            Log.d(TAG, "原始响应: $responseBody")
            
            val json = responseBody.parseJson()
            val bodyObj = json.optJSONObject("body")
            
            Log.d(TAG, "body 对象: $bodyObj")
            Log.d(TAG, "body 长度: ${bodyObj?.length() ?: "null"}")
            
            // 获取第一个账户的 parent_id
            // body 格式: {"0": {"parent_id": "123456"}}
            if (bodyObj != null && bodyObj.length() > 0) {
                Log.d(TAG, "----- 遍历 body 字段 -----")
                val keys = mutableListOf<String>()
                bodyObj.keys().forEach { key -> keys.add(key) }
                Log.d(TAG, "找到 ${keys.size} 个账户")
                
                val firstKey = keys.first()
                Log.d(TAG, "使用第一个账户 key: $firstKey")
                
                val firstAccount = bodyObj.optJSONObject(firstKey)
                Log.d(TAG, "第一个账户内容: $firstAccount")
                
                val parentId = firstAccount?.optString("parent_id") ?: ""
                Log.d(TAG, "parent_id: $parentId")
                
                if (parentId.isEmpty()) {
                    Log.e(TAG, "parent_id 为空！")
                    throw Exception("未找到账户信息")
                }
                
                Log.i(TAG, "✓ 获取父账户ID成功: $parentId")
                parentId
            } else {
                Log.e(TAG, "bodyObj 为空或长度为0")
                throw Exception("未找到账户信息")
            }
        }
    }

    /**
     * 获取作业列表
     * POST /g/homework/list
     * 
     * @param token 登录返回的 token
     * @param parentAccountId 父账户 ID
     * @return 作业列表响应
     */
    suspend fun getHomeworkList(token: String, parentAccountId: String): Result<HomeworkListResponse> {
        Log.d(TAG, "getHomeworkList: parentAccountId=$parentAccountId")
        
        val bodyData = mapOf(
            "r" to "g/homework/list",
            "params" to mapOf(
                "sn" to RequestConfig.DEFAULT_SN,
                "token" to token,
                "parent_account_id" to parentAccountId,
                "limit" to "0",
                "status" to "1",
                "offset" to "0",
                "get_to_do_count" to 1,
                "show_old_homework" to 1,
                "get_all_count" to 1,
                "check_pass" to 1,
                "get_to_overtime_count" to 1,
                "version" to RequestConfig.DEFAULT_VERSION,
                "system" to RequestConfig.DEFAULT_SYSTEM,
                "global_client_version" to RequestConfig.DEFAULT_GLOBAL_CLIENT_VERSION,
                "sign_response" to 1
            )
        )

        return postRequest(Endpoints.HOMEWORK_LIST, bodyData).mapCatching { responseBody ->
            Log.d(TAG, "homework_list response: $responseBody")
            
            val json = responseBody.parseJson()
            val bodyObj = json.optJSONObject("body")
            
            if (bodyObj == null) {
                throw Exception("作业列表获取失败")
            }
            
            val baseUrl = bodyObj.optString("base_url", Config.CDN_BASE_URL)
            val dataArray = bodyObj.optJSONArray("data") ?: org.json.JSONArray()
            
            val homeworks = mutableListOf<HomeworkInfo>()
            for (i in 0 until dataArray.length()) {
                val item = dataArray.getJSONObject(i)
                val name = item.optString("name", "未知作业")
                
                // 解析 struct.contents 获取作业详情
                val struct = item.optJSONObject("struct")
                val contentsArray = struct?.optJSONArray("contents") ?: org.json.JSONArray()
                
                val contents = mutableListOf<HomeworkContent>()
                for (j in 0 until contentsArray.length()) {
                    val content = contentsArray.getJSONObject(j)
                    contents.add(HomeworkContent(
                        groupName = content.optString("group_name", ""),
                        url = content.optString("url", "")
                    ))
                }
                
                homeworks.add(HomeworkInfo(
                    name = name,
                    contents = contents
                ))
            }
            
            HomeworkListResponse(
                baseUrl = baseUrl,
                homeworks = homeworks
            )
        }
    }

    // ============================================================================
    // 数据类
    // ============================================================================
    
    data class LoginResponse(
        val token: String
    )
    
    data class HomeworkListResponse(
        val baseUrl: String,
        val homeworks: List<HomeworkInfo>
    )
    
    data class HomeworkInfo(
        val name: String,
        val contents: List<HomeworkContent>
    )
    
    data class HomeworkContent(
        val groupName: String,
        val url: String
    )

    // ============================================================================
    // JSON 解析工具
    // ============================================================================
    
    /**
     * 简化 JSON 字符串解析
     * 喵~ 使用 org.json 的简单封装喵！
     */
    private fun String.parseJson(): org.json.JSONObject {
        return org.json.JSONObject(this)
    }
    
    /**
     * Map 转 JSON 字符串
     */
    private fun Map<String, Any>.toJson(): String {
        val sb = StringBuilder()
        sb.append("{")
        entries.forEachIndexed { index, (key, value) ->
            if (index > 0) sb.append(",")
            sb.append("\"$key\":")
            when (value) {
                is String -> sb.append("\"${value.escapeJson()}\"")
                is Number -> sb.append(value)
                is Boolean -> sb.append(value)
                is Map<*, *> -> sb.append((value as Map<String, Any>).toJson())
                else -> sb.append("\"${value.toString().escapeJson()}\"")
            }
        }
        sb.append("}")
        return sb.toString()
    }
    
    /**
     * JSON 字符串转 Map
     */
    private fun String.toMapJson(): String {
        return "{$this}"
    }
    
    /**
     * 转义 JSON 特殊字符
     */
    private fun String.escapeJson(): String {
        return this
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}

// 扩展函数：获取 JSON 对象
private fun org.json.JSONObject.keys(): Iterator<String> {
    val keys = mutableListOf<String>()
    for (key in keys()) {
        keys.add(key)
    }
    return keys.iterator()
}