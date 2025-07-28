package com.zia.elementcollector

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.marvin.talkback.TalkBackService
import com.zia.elementcollector.ui.theme.ElementCollectorTheme
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ElementCollectorTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var isAccessibilityEnabled by remember { mutableStateOf(false) }
    var hasOverlayPermission by remember { mutableStateOf(false) }
    var outputFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    
    // 检查服务状态和权限
    LaunchedEffect(Unit) {
        isAccessibilityEnabled = isAccessibilityServiceEnabled(context)
        hasOverlayPermission = checkOverlayPermission(context)
        outputFiles = loadOutputFiles(context)
    }
    
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "元素收集器",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "无障碍服务状态",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isAccessibilityEnabled) "已启用" else "未启用",
                            color = if (isAccessibilityEnabled) MaterialTheme.colorScheme.primary 
                                   else MaterialTheme.colorScheme.error
                        )
                        
                        Button(
                            onClick = {
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                context.startActivity(intent)
                            }
                        ) {
                            Text("打开无障碍设置")
                        }
                    }
                }
            }
        }
        
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "悬浮窗权限状态",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (hasOverlayPermission) "已授权" else "未授权",
                            color = if (hasOverlayPermission) MaterialTheme.colorScheme.primary 
                                   else MaterialTheme.colorScheme.error
                        )
                        
                        Button(
                            onClick = {
                                requestOverlayPermission(context)
                            }
                        ) {
                            Text("授权悬浮窗权限")
                        }
                    }
                }
            }
        }
        
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "使用说明",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Text(
                        text = "1. 点击'打开无障碍设置'按钮\n" +
                              "2. 在无障碍设置中找到'ElementCollector'\n" +
                              "3. 启用该无障碍服务\n" +
                              "4. 点击'授权悬浮窗权限'按钮\n" +
                              "5. 授予悬浮窗权限\n" +
                              "6. 返回应用，将出现悬浮窗按钮\n" +
                              "7. 点击悬浮窗按钮手动收集当前页面元素\n" +
                              "8. 收集的数据将保存到设备存储中",
                        fontSize = 14.sp
                    )
                }
            }
        }
        
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "输出文件",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                    
                    if (outputFiles.isEmpty()) {
                        Text(
                            text = "暂无输出文件",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        outputFiles.forEach { file ->
                            Text(
                                text = file.name,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun isAccessibilityServiceEnabled(context: android.content.Context): Boolean {
    val accessibilityEnabled = Settings.Secure.getInt(
        context.contentResolver,
        Settings.Secure.ACCESSIBILITY_ENABLED, 0
    )
    
    if (accessibilityEnabled == 1) {
        val service = "${context.packageName}/${TalkBackService::class.java.name}"
        val settingValue = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return settingValue?.contains(service) == true
    }
    return false
}

private fun checkOverlayPermission(context: android.content.Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        Settings.canDrawOverlays(context)
    } else {
        true
    }
}

private fun requestOverlayPermission(context: android.content.Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
        context.startActivity(intent)
    }
}

private fun loadOutputFiles(context: android.content.Context): List<File> {
    val outputDir = File(context.getExternalFilesDir(null), "element_collector_output")
    return if (outputDir.exists()) {
        outputDir.listFiles()?.filter { it.extension == "xml" }?.sortedByDescending { it.lastModified() } ?: emptyList()
    } else {
        emptyList()
    }
}