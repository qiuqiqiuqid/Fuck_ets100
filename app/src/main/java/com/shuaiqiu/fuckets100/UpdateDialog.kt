package com.shuaiqiu.fuckets100

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext

/**
 * 更新弹窗组件
 * 宝贝用于显示版本更新提示喵~
 *
 * @param status 更新状态，包含所有需要的信息
 * @param onDismiss 弹窗关闭回调
 */
@Composable
fun UpdateDialog(
    status: com.shuaiqiu.fuckets100.UpdateStatus,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    
    Log.d("UpdateDialog", "显示更新弹窗: message=${status.message}, isForce=${status.isForce}")
    
    AlertDialog(
        onDismissRequest = {
            // 强制更新时不允许关闭弹窗喵~
            if (!status.isForce) {
                onDismiss()
            }
        },
        title = {
            Text(text = if (status.isForce) "🔴 强制更新" else "🆙 发现新版本")
        },
        text = {
            Text(text = status.message)
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // 打开更新链接
                    openUpdateUrl(context, status.updateUrl)
                    // 如果不是强制更新，关闭弹窗
                    if (!status.isForce) {
                        onDismiss()
                    }
                }
            ) {
                Text("立即更新")
            }
        },
        dismissButton = {
            if (!status.isForce) {
                TextButton(onClick = onDismiss) {
                    Text("稍后再说")
                }
            }
        }
    )
}

/**
 * 打开更新链接
 * 宝贝使用浏览器打开更新地址喵~
 */
private fun openUpdateUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    } catch (e: Exception) {
        Log.e("UpdateDialog", "打开更新链接失败: $url", e)
    }
}
