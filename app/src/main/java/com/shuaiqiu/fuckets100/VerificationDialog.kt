package com.shuaiqiu.fuckets100

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun VerificationDialog(
    status: UpdateStatus,
    onVerified: () -> Unit
) {
    var inputCode by remember(status.verificationCode) { mutableStateOf("") }
    var errorText by remember(status.verificationCode) { mutableStateOf("") }
    val title = status.verificationTitle.ifBlank { "\u542f\u52a8\u9a8c\u8bc1" }
    val message = status.verificationMessage.ifBlank {
        "\u8bf7\u8f93\u5165\u9a8c\u8bc1\u7801\u7ee7\u7eed\u4f7f\u7528\u3002"
    }

    AlertDialog(
        onDismissRequest = {},
        title = { Text(title) },
        text = {
            Column {
                Text(message)
                OutlinedTextField(
                    value = inputCode,
                    onValueChange = {
                        inputCode = it
                        errorText = ""
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    singleLine = true,
                    label = { Text("\u9a8c\u8bc1\u7801") },
                    visualTransformation = PasswordVisualTransformation(),
                    isError = errorText.isNotBlank(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    supportingText = {
                        if (errorText.isNotBlank()) {
                            Text(errorText)
                        }
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val normalizedInput = inputCode.trim()
                    if (normalizedInput == status.verificationCode) {
                        SettingsManager.saveLocalVerificationCode(normalizedInput)
                        onVerified()
                    } else {
                        errorText = "\u9a8c\u8bc1\u7801\u4e0d\u6b63\u786e\uff0c\u8bf7\u91cd\u65b0\u8f93\u5165"
                    }
                }
            ) {
                Text("\u9a8c\u8bc1")
            }
        }
    )
}
