package com.zia.elementcollector

import android.view.accessibility.AccessibilityNodeInfo

/**
 * 元素分析工具类
 * 提供各种元素分析功能
 */
object ElementAnalyzer {
    
    /**
     * 查找所有可点击的元素
     */
    fun findClickableElements(rootNode: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        val clickableElements = mutableListOf<AccessibilityNodeInfo>()
        findElementsByProperty(rootNode, { it.isClickable }, clickableElements)
        return clickableElements
    }
    
    /**
     * 查找所有文本元素
     */
    fun findTextElements(rootNode: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        val textElements = mutableListOf<AccessibilityNodeInfo>()
        findElementsByProperty(rootNode, { it.text?.isNotEmpty() == true }, textElements)
        return textElements
    }
    
    /**
     * 查找所有输入框元素
     */
    fun findInputElements(rootNode: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        val inputElements = mutableListOf<AccessibilityNodeInfo>()
        findElementsByProperty(rootNode, { 
            it.className?.toString()?.contains("EditText") == true 
        }, inputElements)
        return inputElements
    }
    
    /**
     * 查找所有按钮元素
     */
    fun findButtonElements(rootNode: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        val buttonElements = mutableListOf<AccessibilityNodeInfo>()
        findElementsByProperty(rootNode, { 
            it.className?.toString()?.contains("Button") == true || 
            it.isClickable 
        }, buttonElements)
        return buttonElements
    }
    
    /**
     * 根据ID查找元素
     */
    fun findElementById(rootNode: AccessibilityNodeInfo, viewId: String): AccessibilityNodeInfo? {
        return findElementByProperty(rootNode) { 
            it.viewIdResourceName == viewId 
        }
    }
    
    /**
     * 根据文本内容查找元素
     */
    fun findElementByText(rootNode: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        return findElementByProperty(rootNode) { 
            it.text?.toString() == text 
        }
    }
    
    /**
     * 根据内容描述查找元素
     */
    fun findElementByContentDescription(rootNode: AccessibilityNodeInfo, description: String): AccessibilityNodeInfo? {
        return findElementByProperty(rootNode) { 
            it.contentDescription?.toString() == description 
        }
    }
    
    /**
     * 获取元素的完整路径
     */
    fun getElementPath(node: AccessibilityNodeInfo): String {
        val path = mutableListOf<String>()
        var current: AccessibilityNodeInfo? = node
        
        while (current != null) {
            val className = current.className?.toString() ?: "Unknown"
            val text = current.text?.toString() ?: ""
            val viewId = current.viewIdResourceName ?: ""
            
            val elementInfo = when {
                viewId.isNotEmpty() -> "$className[@id='$viewId']"
                text.isNotEmpty() -> "$className[@text='$text']"
                else -> className
            }
            
            path.add(elementInfo)
            current = current.parent
        }
        
        return path.reversed().joinToString("/")
    }
    
    /**
     * 获取元素的层级深度
     */
    fun getElementDepth(node: AccessibilityNodeInfo): Int {
        var depth = 0
        var current: AccessibilityNodeInfo? = node.parent
        
        while (current != null) {
            depth++
            current = current.parent
        }
        
        return depth
    }
    
    /**
     * 检查元素是否在屏幕可见区域内
     */
    fun isElementVisible(node: AccessibilityNodeInfo): Boolean {
        val bounds = android.graphics.Rect()
        node.getBoundsInScreen(bounds)
        
        // 检查元素是否有有效的尺寸
        if (bounds.width() <= 0 || bounds.height() <= 0) {
            return false
        }
        
        // 检查元素是否可见
        return node.isVisibleToUser
    }
    
    /**
     * 获取元素的屏幕坐标
     */
    fun getElementBounds(node: AccessibilityNodeInfo): android.graphics.Rect {
        val bounds = android.graphics.Rect()
        node.getBoundsInScreen(bounds)
        return bounds
    }
    
    /**
     * 获取元素的中心点坐标
     */
    fun getElementCenter(node: AccessibilityNodeInfo): android.graphics.Point {
        val bounds = getElementBounds(node)
        return android.graphics.Point(
            bounds.centerX(),
            bounds.centerY()
        )
    }
    
    /**
     * 递归查找满足条件的元素
     */
    private fun findElementsByProperty(
        node: AccessibilityNodeInfo,
        predicate: (AccessibilityNodeInfo) -> Boolean,
        result: MutableList<AccessibilityNodeInfo>
    ) {
        if (predicate(node)) {
            result.add(node)
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                findElementsByProperty(child, predicate, result)
            }
        }
    }
    
    /**
     * 递归查找第一个满足条件的元素
     */
    private fun findElementByProperty(
        node: AccessibilityNodeInfo,
        predicate: (AccessibilityNodeInfo) -> Boolean
    ): AccessibilityNodeInfo? {
        if (predicate(node)) {
            return node
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                val result = findElementByProperty(child, predicate)
                if (result != null) {
                    return result
                }
            }
        }
        
        return null
    }
} 