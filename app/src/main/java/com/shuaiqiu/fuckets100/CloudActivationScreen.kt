package com.shuaiqiu.fuckets100

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import androidx.navigation.NavHostController
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
    navController: NavHostController,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
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
                val loginResult = ETS100ApiClient.login(phone, password, deviceCode)
                
                loginResult.onSuccess { loginResponse ->
                    Log.d("CloudActivationScreen", "登录成功，获取 token: ${loginResponse.token.take(8)}...")
                    
                    // 获取父账户 ID
                    val ecardResult = ETS100ApiClient.getEcardList(loginResponse.token)
                    
                    ecardResult.onSuccess { parentAccountId ->
                        Log.d("CloudActivationScreen", "获取父账户ID成功: $parentAccountId")
                        
                        // 保存登录信息
                        ETS100AuthManager.saveLoginInfo(context, phone, loginResponse.token, parentAccountId)
                        
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "登录成功！", Toast.LENGTH_SHORT).show()
                            onLoginSuccess()
                        }
                    }.onFailure { e ->
                        Log.e("CloudActivationScreen", "获取父账户ID失败", e)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "获取账户信息失败: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }.onFailure { e ->
                    Log.e("CloudActivationScreen", "登录失败", e)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "登录失败: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("CloudActivationScreen", "登录异常", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "登录异常: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            
            isLoading = false
        }
    }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("云端模式", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
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
                shape = RoundedCornerShape(12.dp)
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
                shape = RoundedCornerShape(12.dp)
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
                    Icon(Icons.Default.Login, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("登 录", style = MaterialTheme.typography.titleMedium)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 提示信息
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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
                    Column {
                        Text(
                            text = "登录说明",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "使用 ETS100 账号登录，账号为手机号。登录后可以在线查看作业列表和答案。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}