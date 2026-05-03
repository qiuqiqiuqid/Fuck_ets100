package com.shuaiqiu.fuckets100

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * Share preview screen with animations
 * For previewing answer HTML and sharing~
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareScreen(
    paper: ETS100AnswerReader.Paper,
    isDarkMode: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    
    // Generate HTML content
    val htmlContent = remember(paper, isDarkMode) {
        AnswerHtmlGenerator.generatePaperHtml(paper, isDarkMode)
    }
    
    Log.d("ShareScreen", "Preview paper: ${paper.title}")
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Share Preview") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // WebView preview area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                loadWithOverviewMode = true
                                useWideViewPort = true
                                builtInZoomControls = false
                                displayZoomControls = false
                            }
                            
                            setBackgroundColor(
                                if (isDarkMode) android.graphics.Color.parseColor("#1a1a1a")
                                else android.graphics.Color.parseColor("#f5f5f5")
                            )
                            
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    Log.d("ShareScreen", "Page loaded")
                                }
                            }
                            
                            loadDataWithBaseURL(
                                null,
                                htmlContent,
                                "text/html",
                                "UTF-8",
                                null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            // Bottom action bar - share button only
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = {
                            shareHtmlFile(context, paper.title, htmlContent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Share Answer")
                    }
                }
            }
        }
    }
}

/**
 * Share HTML file
 */
private fun shareHtmlFile(context: Context, title: String, htmlContent: String) {
    try {
        val filename = "Fe_Answer_${System.currentTimeMillis()}.html"
        val file = File(context.cacheDir, filename)
        
        Log.d("ShareScreen", "Saving HTML to: ${file.absolutePath}")
        
        FileOutputStream(file).use { out ->
            out.write(htmlContent.toByteArray(Charsets.UTF_8))
        }
        
        Log.d("ShareScreen", "HTML file size: ${file.length()} bytes")
        
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        Log.d("ShareScreen", "FileProvider URI: $uri")
        
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/html"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Fe Answer: $title")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(Intent.createChooser(shareIntent, "Share Answer"))
        
    } catch (e: Exception) {
        Log.e("ShareScreen", "Share failed", e)
    }
}
