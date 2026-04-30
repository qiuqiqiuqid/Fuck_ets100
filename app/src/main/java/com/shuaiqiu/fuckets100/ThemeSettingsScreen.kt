package com.shuaiqiu.fuckets100

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsScreen(
    navController: NavHostController,
    onThemeChanged: ((AppTheme) -> Unit)? = null
) {
    val currentTheme = ThemeManager.getSavedTheme()
    var selectedTheme by remember { mutableStateOf(currentTheme) }
    
    val monetThemes = ThemeManager.getMonetThemes()
    val monoThemes = ThemeManager.getMonoThemes()
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("主题设置", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = { 
                    IconButton(onClick = { navController.popBackStack() }) { 
                        Icon(Icons.Default.Palette, null) 
                    } 
                }
            )
        }
    ) { p ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(p)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(16.dp))
            
            // 莫奈系列
            Text(
                "莫奈系列", 
                style = MaterialTheme.typography.titleMedium, 
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Text(
                "多彩配色，随心切换",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(monetThemes) { theme ->
                    ThemeCard(
                        theme = theme,
                        isSelected = selectedTheme == theme,
                        onClick = {
                            selectedTheme = theme
                            ThemeManager.saveTheme(theme)
                            onThemeChanged?.invoke(theme)
                        }
                    )
                }
            }
            
            Spacer(Modifier.height(24.dp))
            
            // 黑白系列
            Text(
                "黑白系列", 
                style = MaterialTheme.typography.titleMedium, 
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Text(
                "简约灰度，高对比度",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(monoThemes) { theme ->
                    ThemeCard(
                        theme = theme,
                        isSelected = selectedTheme == theme,
                        onClick = {
                            selectedTheme = theme
                            ThemeManager.saveTheme(theme)
                            onThemeChanged?.invoke(theme)
                        }
                    )
                }
            }
            
            Spacer(Modifier.height(32.dp))
            
            // 预览区域
            Text(
                "主题预览", 
                style = MaterialTheme.typography.titleMedium, 
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(Modifier.height(12.dp))
            
            ThemePreviewCard(theme = selectedTheme, modifier = Modifier.padding(horizontal = 16.dp))
            
            Spacer(Modifier.height(88.dp))
        }
    }
}

@Composable
fun ThemeCard(
    theme: AppTheme,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(100.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                theme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 主色调预览圆
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(theme.primary)
            ) {
                if (isSelected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = theme.onPrimary,
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.Center)
                    )
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            Text(
                theme.displayName,
                style = MaterialTheme.typography.labelMedium,
                color = if (isSelected) theme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun ThemePreviewCard(theme: AppTheme, modifier: Modifier = Modifier) {
    // 动态创建一个临时 ColorScheme 用于预览
    val previewColorScheme = lightColorScheme(
        primary = theme.primary,
        onPrimary = theme.onPrimary,
        primaryContainer = theme.primaryContainer,
        surface = theme.surface,
        onSurface = theme.onSurface,
        onSurfaceVariant = theme.onSurfaceVariant,
        outlineVariant = theme.outlineVariant,
        error = theme.error,
        errorContainer = theme.errorContainer,
        secondary = theme.secondary,
        secondaryContainer = theme.secondaryContainer
    )
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = theme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 标题预览
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(theme.primary)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "Fe 终端",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = theme.onSurface
                    )
                    Text(
                        "应用预览",
                        style = MaterialTheme.typography.bodySmall,
                        color = theme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            // 按钮预览
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = theme.primary,
                        contentColor = theme.onPrimary
                    )
                ) {
                    Text("主要按钮")
                }
                
                OutlinedButton(onClick = { }) {
                    Text("次要按钮")
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            // 状态指示器
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(theme.primaryContainer),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // 模拟进度
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(0.7f)
                        .background(theme.primary)
                )
            }
        }
    }
}
