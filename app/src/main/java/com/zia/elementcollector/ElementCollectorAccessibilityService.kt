package com.zia.elementcollector

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.OutputKeys

open class ElementCollectorAccessibilityService : AccessibilityService() {
    
    companion object {
        private const val TAG = "ElementCollectorService"
        private const val OUTPUT_DIR = "element_collector_output"
    }
    
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var windowManager: WindowManager? = null
    private var floatingButton: View? = null
    private var isFloatingWindowShowing = false
    private var initialX: Int = 0
    private var initialY: Int = 0
    private var initialTouchX: Float = 0f
    private var initialTouchY: Float = 0f
    private var floatingParams: WindowManager.LayoutParams? = null
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "无障碍服务已连接")
        
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK.toInt()
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            notificationTimeout = 100
        }
        serviceInfo = info
        
        // 检查悬浮窗权限并创建悬浮窗
        if (checkOverlayPermission()) {
            createFloatingWindow()
        } else {
            requestOverlayPermission()
        }
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // 不再自动创建悬浮窗，避免重复创建
    }
    
    override fun onInterrupt() {
        Log.d(TAG, "无障碍服务被中断")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        removeFloatingWindow()
    }
    
    private fun createFloatingWindow() {
        try {
            // 如果悬浮窗已经存在，先移除
            if (isFloatingWindowShowing && floatingButton != null) {
                removeFloatingWindow()
            }
            
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            
            // 设置悬浮窗参数
            floatingParams = WindowManager.LayoutParams().apply {
                width = 120
                height = 60
                type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    WindowManager.LayoutParams.TYPE_PHONE
                }
                flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                format = PixelFormat.TRANSLUCENT
                gravity = Gravity.TOP or Gravity.END
                x = 20
                y = 100
            }
            
            // 创建悬浮按钮
            floatingButton = Button(this).apply {
                text = "收集元素"
                textSize = 16f
                setPadding(16, 8, 16, 8)
                setBackgroundColor(android.graphics.Color.parseColor("#CCFFFFFF"))
                setTextColor(android.graphics.Color.BLACK)
                
                // 设置拖拽监听
                setOnTouchListener { _, event ->
                    when (event.action) {
                        android.view.MotionEvent.ACTION_DOWN -> {
                            initialTouchX = event.rawX
                            initialTouchY = event.rawY
                            initialX = floatingParams?.x ?: 0
                            initialY = floatingParams?.y ?: 0
                            true
                        }
                        android.view.MotionEvent.ACTION_MOVE -> {
                            val deltaX = (event.rawX - initialTouchX).toInt()
                            val deltaY = (event.rawY - initialTouchY).toInt()
                            floatingParams?.let { params ->
                                params.x = initialX + deltaX
                                params.y = initialY + deltaY
                                windowManager?.updateViewLayout(floatingButton, params)
                            }
                            true
                        }
                        android.view.MotionEvent.ACTION_UP -> {
                            // 检查是否是点击（移动距离很小）
                            val deltaX = (event.rawX - initialTouchX).toInt()
                            val deltaY = (event.rawY - initialTouchY).toInt()
                            if (Math.abs(deltaX) < 10 && Math.abs(deltaY) < 10) {
                                Log.d(TAG, "悬浮窗按钮被点击")
                                // 显示点击反馈
                                showClickFeedback()
                                serviceScope.launch {
                                    collectPageElements()
                                }
                            }
                            true
                        }
                        else -> false
                    }
                }
            }
            
            // 添加悬浮窗
            floatingParams?.let { params ->
                windowManager?.addView(floatingButton, params)
                isFloatingWindowShowing = true
                
                Log.d(TAG, "悬浮窗创建成功")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "创建悬浮窗失败", e)
            isFloatingWindowShowing = false
            floatingButton = null
        }
    }
    
    private fun checkOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }
    
    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            
            Toast.makeText(this, "请授予悬浮窗权限，然后重新启用无障碍服务", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun removeFloatingWindow() {
        try {
            if (isFloatingWindowShowing && floatingButton != null) {
                windowManager?.removeView(floatingButton)
                floatingButton = null
                isFloatingWindowShowing = false
                Log.d(TAG, "悬浮窗已移除")
            }
        } catch (e: Exception) {
            Log.e(TAG, "移除悬浮窗失败", e)
        }
    }
    
    private suspend fun collectPageElements() {
        try {
            val rootNode = rootInActiveWindow ?: return
            val pageInfo = analyzePageStructure(rootNode)
            
            // 保存到文件
            val filePath = saveToFile(pageInfo)
            
            // 显示预览
            showResultPreview(filePath)
            
            // 输出到日志
            Log.d(TAG, "页面元素收集完成")
            
        } catch (e: Exception) {
            Log.e(TAG, "收集页面元素时出错", e)
        }
    }
    
    private fun analyzePageStructure(rootNode: AccessibilityNodeInfo): Document {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val doc = builder.newDocument()
        
        // 创建根元素
        val rootElement = doc.createElement("PageElements")
        doc.appendChild(rootElement)
        
        // 添加基本信息
        val infoElement = doc.createElement("PageInfo")
        rootElement.appendChild(infoElement)
        
        val timestampElement = doc.createElement("timestamp")
        timestampElement.textContent = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        infoElement.appendChild(timestampElement)
        
        val packageElement = doc.createElement("packageName")
        packageElement.textContent = rootNode.packageName?.toString() ?: ""
        infoElement.appendChild(packageElement)
        
        val classElement = doc.createElement("className")
        classElement.textContent = rootNode.className?.toString() ?: ""
        infoElement.appendChild(classElement)
        
        // 页面层级结构（合并所有元素信息）
        val hierarchyElement = doc.createElement("Hierarchy")
        rootElement.appendChild(hierarchyElement)
        buildElementHierarchy(rootNode, hierarchyElement, doc)
        
        // 统计信息
        val statsElement = doc.createElement("Statistics")
        rootElement.appendChild(statsElement)
        
        val totalElements = countAllElements(rootNode)
        val clickableElements = countClickableElements(rootNode)
        val textElements = countTextElements(rootNode)
        
        val totalElement = doc.createElement("totalElements")
        totalElement.textContent = totalElements.toString()
        statsElement.appendChild(totalElement)
        
        val clickableElement = doc.createElement("clickableElements")
        clickableElement.textContent = clickableElements.toString()
        statsElement.appendChild(clickableElement)
        
        val textElement = doc.createElement("textElements")
        textElement.textContent = textElements.toString()
        statsElement.appendChild(textElement)
        
        return doc
    }
    
    private fun buildElementHierarchy(node: AccessibilityNodeInfo, parentElement: Element, doc: Document) {
        val element = doc.createElement("Element")
        parentElement.appendChild(element)
        
        // 基本信息 - 只添加有值的属性
        element.setAttribute("className", node.className?.toString() ?: "")
        
        // 只添加非空的文本
        val text = node.text?.toString()
        if (!text.isNullOrEmpty()) {
            element.setAttribute("text", text)
        }
        
        // 只添加非空的描述
        val description = node.contentDescription?.toString()
        if (!description.isNullOrEmpty()) {
            element.setAttribute("contentDescription", description)
        }
        
        // 只添加非空的ID
        val viewId = node.viewIdResourceName
        if (!viewId.isNullOrEmpty()) {
            element.setAttribute("viewId", viewId)
        }
        
        // 位置信息 - 只添加有效的尺寸
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        if (bounds.width() > 0 && bounds.height() > 0) {
            element.setAttribute("left", bounds.left.toString())
            element.setAttribute("top", bounds.top.toString())
            element.setAttribute("right", bounds.right.toString())
            element.setAttribute("bottom", bounds.bottom.toString())
            element.setAttribute("width", bounds.width().toString())
            element.setAttribute("height", bounds.height().toString())
            element.setAttribute("centerX", bounds.centerX().toString())
            element.setAttribute("centerY", bounds.centerY().toString())
        }
        
        // 属性信息 - 只添加为true的属性
        if (node.isClickable) element.setAttribute("clickable", "true")
        if (node.isFocusable) element.setAttribute("focusable", "true")
        if (!node.isEnabled) element.setAttribute("enabled", "false")
        if (!node.isVisibleToUser) element.setAttribute("visible", "false")
        if (node.isCheckable) element.setAttribute("checkable", "true")
        if (node.isChecked) element.setAttribute("checked", "true")
        if (node.isScrollable) element.setAttribute("scrollable", "true")
        if (node.isLongClickable) element.setAttribute("longClickable", "true")
        if (node.isPassword) element.setAttribute("password", "true")
        if (node.isSelected) element.setAttribute("selected", "true")
        
        // 动作信息 - 只添加真正有意义的动作
        val meaningfulActions = node.actionList.filter { action ->
            val actionStr = action.toString()
            when {
                // 过滤掉所有无意义的常见动作
                actionStr.contains("ACTION_FOCUS") -> false
                actionStr.contains("ACTION_CLEAR_FOCUS") -> false
                actionStr.contains("ACTION_ACCESSIBILITY_FOCUS") -> false
                actionStr.contains("ACTION_CLEAR_ACCESSIBILITY_FOCUS") -> false
                actionStr.contains("ACTION_SELECT") -> false
                actionStr.contains("ACTION_CLEAR_SELECTION") -> false
                actionStr.contains("ACTION_SHOW_ON_SCREEN") -> false
                actionStr.contains("ACTION_SCROLL_FORWARD") -> false
                actionStr.contains("ACTION_SCROLL_BACKWARD") -> false
                actionStr.contains("ACTION_SCROLL_LEFT") -> false
                actionStr.contains("ACTION_SCROLL_RIGHT") -> false
                actionStr.contains("ACTION_SCROLL_UP") -> false
                actionStr.contains("ACTION_SCROLL_DOWN") -> false
                actionStr.contains("ACTION_PAGE_LEFT") -> false
                actionStr.contains("ACTION_PAGE_RIGHT") -> false
                actionStr.contains("ACTION_PAGE_UP") -> false
                actionStr.contains("ACTION_PAGE_DOWN") -> false
                actionStr.contains("ACTION_CONTEXT_CLICK") -> false
                actionStr.contains("ACTION_SET_PROGRESS") -> false
                actionStr.contains("ACTION_MOVE_WINDOW") -> false
                actionStr.contains("ACTION_SET_TEXT") -> false
                actionStr.contains("ACTION_IME_ENTER") -> false
                actionStr.contains("ACTION_DRAG_START") -> false
                actionStr.contains("ACTION_DRAG_DROP") -> false
                actionStr.contains("ACTION_DRAG_CANCEL") -> false
                actionStr.contains("ACTION_SET_SELECTION") -> false
                actionStr.contains("ACTION_EXPAND") -> false
                actionStr.contains("ACTION_COLLAPSE") -> false
                actionStr.contains("ACTION_DISMISS") -> false
                actionStr.contains("ACTION_SET_INPUT_FOCUS") -> false
                actionStr.contains("ACTION_CLEAR_INPUT_FOCUS") -> false
                actionStr.contains("ACTION_NEXT_AT_MOVEMENT_GRANULARITY") -> false
                actionStr.contains("ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY") -> false
                actionStr.contains("ACTION_NEXT_HTML_ELEMENT") -> false
                actionStr.contains("ACTION_PREVIOUS_HTML_ELEMENT") -> false
                actionStr.contains("ACTION_SCROLL_IN_DIRECTION") -> false
                actionStr.contains("ACTION_COPY") -> false
                actionStr.contains("ACTION_PASTE") -> false
                actionStr.contains("ACTION_CUT") -> false
                actionStr.contains("ACTION_SET_SELECTION") -> false
                actionStr.contains("ACTION_EXTEND_SELECTION") -> false
                actionStr.contains("ACTION_UNDO") -> false
                actionStr.contains("ACTION_REDO") -> false
                actionStr.contains("ACTION_CLEAR_ACCESSIBILITY_FOCUS") -> false
                actionStr.contains("ACTION_LONG_CLICK") -> false
                actionStr.contains("ACTION_CLICK") -> false
                // 只保留真正有意义的自定义动作
                else -> actionStr.contains("ACTION_") && !actionStr.contains("null")
            }
        }
        
        if (meaningfulActions.isNotEmpty()) {
            val actionsElement = doc.createElement("Actions")
            element.appendChild(actionsElement)
            for (action in meaningfulActions) {
                val actionElement = doc.createElement("Action")
                actionElement.textContent = action.toString()
                actionsElement.appendChild(actionElement)
            }
        }
        
        // 子元素
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                buildElementHierarchy(child, element, doc)
                child.recycle()
            }
        }
    }
    
    private fun countAllElements(node: AccessibilityNodeInfo): Int {
        var count = 1
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                count += countAllElements(child)
                child.recycle()
            }
        }
        return count
    }
    
    private fun countClickableElements(node: AccessibilityNodeInfo): Int {
        var count = if (node.isClickable) 1 else 0
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                count += countClickableElements(child)
                child.recycle()
            }
        }
        return count
    }
    
    private fun countTextElements(node: AccessibilityNodeInfo): Int {
        var count = if (node.text?.isNotEmpty() == true) 1 else 0
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                count += countTextElements(child)
                child.recycle()
            }
        }
        return count
    }
    
    private suspend fun saveToFile(doc: Document): String {
        return withContext(Dispatchers.IO) {
            try {
                val outputDir = File(getExternalFilesDir(null), OUTPUT_DIR)
                if (!outputDir.exists()) {
                    outputDir.mkdirs()
                }
                
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "page_elements_$timestamp.xml"
                val file = File(outputDir, fileName)
                
                // 创建XML转换器
                val transformerFactory = TransformerFactory.newInstance()
                val transformer = transformerFactory.newTransformer()
                transformer.setOutputProperty(OutputKeys.INDENT, "yes")
                transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8")
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
                
                // 转换并保存
                val source = DOMSource(doc)
                val result = StreamResult(file)
                transformer.transform(source, result)
                
                Log.d(TAG, "页面元素信息已保存到: ${file.absolutePath}")
                file.absolutePath
                
            } catch (e: Exception) {
                Log.e(TAG, "保存文件时出错", e)
                ""
            }
        }
    }
    
    private fun showClickFeedback() {
        try {
            val button = floatingButton as? Button
            button?.let { btn ->
                // 保存原始文本
                val originalText = btn.text.toString()
                
                // 改变文本和背景色
                btn.text = "收集完成"
                btn.setBackgroundColor(android.graphics.Color.parseColor("#4CAF50")) // 绿色
                btn.setTextColor(android.graphics.Color.WHITE)
                
                // 1秒后恢复
                btn.postDelayed({
                    btn.text = originalText
                    btn.setBackgroundColor(android.graphics.Color.parseColor("#CCFFFFFF")) // 白色半透明
                    btn.setTextColor(android.graphics.Color.BLACK)
                }, 1000)
            }
        } catch (e: Exception) {
            Log.e(TAG, "显示点击反馈时出错", e)
        }
    }
    
    private fun showResultPreview(filePath: String) {
        if (filePath.isEmpty()) return
        
        try {
            val file = File(filePath)
            if (!file.exists()) return
            
            // 读取完整的文件内容
            val content = file.readText()
            
            // 显示预览对话框
            val intent = Intent(this, PreviewActivity::class.java).apply {
                putExtra("file_path", filePath)
                putExtra("preview_content", content)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            
        } catch (e: Exception) {
            Log.e(TAG, "显示预览时出错", e)
            Toast.makeText(this, "预览失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
} 