package com.shuaiqiu.fuckets100

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 云端激活/登录页面
 * 喵~ 用于用户登录 ETS100 账号喵！
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudActivationScreen(
    onBack: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val changyanLoginLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            onLoginSuccess()
        }
    }
    
    // 表单状态
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    
    // 登录状态检查
    LaunchedEffect(Unit) {
        if (ETS100AuthManager.isLoggedIn(context)) {
            // 已经登录，直接跳转到云端首页
            onLoginSuccess()
        }
    }
    
    // 登录处理
    fun handleLogin() {
        if (phone.isBlank() || password.isBlank()) {
            Toast.makeText(context, "请输入手机号和密码", Toast.LENGTH_SHORT).show()
            return
        }
        
        scope.launch {
            isLoading = true
            
            try {
                // 获取机器码
                val deviceCode = ETS100AuthManager.getDeviceCode(context)
                Log.d("CloudActivationScreen", "使用机器码: ${deviceCode.take(8)}...")
                
                // 调用登录 API
                Log.d("CloudActivationScreen", "===== 开始登录 =====")
                Log.d("CloudActivationScreen", "手机号: ${phone}")
                Log.d("CloudActivationScreen", "机器码: ${deviceCode}")
                
                val loginResult = ETS100ApiClient.login(phone, password, deviceCode)
                
                loginResult.onSuccess { loginResponse ->
                    Log.i("CloudActivationScreen", "✓ 登录成功！")
                    Log.d("CloudActivationScreen", "Token: ${loginResponse.token}")
                    
                    // 获取父账户 ID
                    Log.d("CloudActivationScreen", "----- 获取父账户ID -----")
                    val ecardResult = ETS100ApiClient.getEcardList(loginResponse.token)
                    
                    ecardResult.onSuccess { parentAccountId ->
                        Log.i("CloudActivationScreen", "✓ 获取父账户ID成功: $parentAccountId")
                        
                        // 保存登录信息
                        ETS100AuthManager.saveLoginInfo(context, phone, loginResponse.token, parentAccountId)
                        ETS100AuthManager.saveLoginMethod(context, ETS100AuthManager.LOGIN_METHOD_PASSWORD)
                        ETS100AuthManager.savePassword(context, password)
                        
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "登录成功！", Toast.LENGTH_SHORT).show()
                            onLoginSuccess()
                        }
                    }.onFailure { e ->
                        Log.e("CloudActivationScreen", "✗ 获取父账户ID失败")
                        Log.e("CloudActivationScreen", "错误信息: ${e.message}")
                        e.stackTrace.forEach { Log.e("CloudActivationScreen", "  at ${it}") }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "获取账户信息失败: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }.onFailure { e ->
                    Log.e("CloudActivationScreen", "✗ 登录失败！")
                    Log.e("CloudActivationScreen", "错误类型: ${e::class.java.simpleName}")
                    Log.e("CloudActivationScreen", "错误信息: ${e.message}")
                    
                    when (e) {
                        is ETS100ApiClient.DeviceBindRequiredException -> {
                            // 喵~ 检测到设备需要绑定，开始自动绑定喵！
                            Log.i("CloudActivationScreen", "检测到设备需要绑定，开始自动绑定...")
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "设备需要绑定，正在自动绑定...", Toast.LENGTH_SHORT).show()
                            }
                            
                            val bindResult = ETS100ApiClient.bindDevice(e.phone, e.password, e.deviceCode)
                            
                            bindResult.onSuccess { bindResponse ->
                                Log.i("CloudActivationScreen", "✓ 设备绑定成功！")
                                Log.d("CloudActivationScreen", "绑定 Token: ${bindResponse.token}")
                                
                                // 绑定成功后，获取父账户 ID
                                val ecardResult = ETS100ApiClient.getEcardList(bindResponse.token)
                                
                                ecardResult.onSuccess { parentAccountId ->
                                    Log.i("CloudActivationScreen", "✓ 获取父账户ID成功: $parentAccountId")
                                    
                                    // 保存登录信息
                                    ETS100AuthManager.saveLoginInfo(context, e.phone, bindResponse.token, parentAccountId)
                                    ETS100AuthManager.saveLoginMethod(context, ETS100AuthManager.LOGIN_METHOD_PASSWORD)
                                    ETS100AuthManager.savePassword(context, e.password)
                                    
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "设备绑定并登录成功！", Toast.LENGTH_SHORT).show()
                                        onLoginSuccess()
                                    }
                                }.onFailure { ecardError ->
                                    Log.e("CloudActivationScreen", "✗ 获取父账户ID失败: ${ecardError.message}")
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "绑定成功但获取账户信息失败: ${ecardError.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }.onFailure { bindError ->
                                Log.e("CloudActivationScreen", "✗ 设备绑定失败: ${bindError.message}")
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "设备绑定失败: ${bindError.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                        else -> {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "登录失败: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
                
                Log.d("CloudActivationScreen", "===== 登录流程结束 =====")
            } catch (e: Exception) {
                Log.e("CloudActivationScreen", "✗ 登录过程发生异常！")
                Log.e("CloudActivationScreen", "异常类型: ${e::class.java.simpleName}")
                Log.e("CloudActivationScreen", "异常信息: ${e.message}")
                e.stackTrace.forEach { Log.e("CloudActivationScreen", "  at ${it}") }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "登录异常: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            
            isLoading = false
        }
    }
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("云端模式", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            // 云端图标
            Icon(
                Icons.Default.Cloud,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(80.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 标题
            Text(
                text = "云端模式",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "在线获取作业和答案",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // 手机号输入
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("手机号") },
                placeholder = { Text("请输入手机号") },
                leadingIcon = {
                    Icon(Icons.Default.Phone, contentDescription = null)
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 密码输入
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("密码") },
                placeholder = { Text("请输入密码") },
                leadingIcon = {
                    Icon(Icons.Default.Lock, contentDescription = null)
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "隐藏密码" else "显示密码"
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { handleLogin() }
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 登录按钮
            Button(
                onClick = { handleLogin() },
                enabled = !isLoading && phone.isNotBlank() && password.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("登 录", style = MaterialTheme.typography.titleMedium)
                }
            }

            TextButton(
                onClick = {
                    Log.d("CloudActivationScreen", "点击讯飞登录，准备启动 ChangyanWebLoginActivity")
                    changyanLoginLauncher.launch(ChangyanWebLoginActivity.createIntent(context))
                },
                enabled = !isLoading
            ) {
                Text(
                    text = "讯飞登录",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 提示信息
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "登录风险提示",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "登录云端模式可能会导致 E听说账号在官方客户端退出登录。请确认当前没有正在使用 E听说，再继续登录。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            softWrap = true
                        )
                    }
                }
            }
        }
    }
}
