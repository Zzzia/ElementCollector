# ElementCollector - Android UI信息采集

一个基于Android无障碍服务的页面元素收集工具，能够自动获取和分析页面上的所有UI元素信息，并输出为xml。
（支持Wechat）

## 功能特性

- 🔍 **手动元素收集**: 通过悬浮窗按钮手动触发收集页面上的所有UI元素
- 📊 **层级结构分析**: 提供完整的页面元素层级结构
- 📝 **详细信息输出**: 包含元素位置、属性、文本内容等详细信息
- 💾 **XML格式保存**: 将收集的数据以XML格式保存到设备存储
- 🎯 **元素分类**: 自动识别和分类不同类型的元素（按钮、输入框、文本等）
- 🎛️ **悬浮窗控制**: 提供可拖拽的悬浮窗按钮，随时手动收集当前页面元素
- 📱 **实时预览**: 收集完成后自动弹出预览界面，支持完整内容滚动查看
- 🔄 **状态监控**: 实时监控无障碍服务和悬浮窗权限状态
- 📋 **文件管理**: 文件列表支持点击预览，显示详细信息
- 🎨 **视觉反馈**: 点击悬浮窗后显示"收集完成"反馈，1秒后自动恢复

## 主要组件

### 1. ElementCollectorAccessibilityService
无障碍服务核心类，负责：
- 创建可拖拽的悬浮窗按钮
- 手动触发元素收集
- 分析元素层级结构
- 保存数据到文件
- 显示点击反馈动画
- 自动弹出预览界面

### 2. PreviewActivity
预览界面，提供：
- 文件详细信息显示
- 完整的XML内容预览
- 支持滚动查看长内容
- 分享文件路径功能

### 3. MainActivity
主界面，提供：
- 无障碍服务状态显示
- 悬浮窗权限状态监控
- 实时状态刷新（每2秒）
- 输出文件列表（可点击预览）
- 使用说明和操作指导

### 4. TalkBackService
继承自ElementCollectorAccessibilityService，提供：
- 无障碍服务集成
- 可扩展的功能接口

## 界面展示

### 主界面
![主界面](screenshot/MainActivity.jpeg)

主界面显示：
- 无障碍服务状态（已启用/未启用）
- 悬浮窗权限状态
- 输出文件列表（可点击预览）
- 使用说明和操作指导

### 预览界面
![预览界面](screenshot/preview.jpeg)

预览界面功能：
- 文件详细信息（路径、大小、创建时间）
- 完整的XML内容预览（支持滚动）
- 分享和关闭按钮

## 使用方法

### 1. 安装应用
```bash
# 编译并安装应用
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 2. 启用无障碍服务
1. 打开应用
2. 点击"打开无障碍设置"按钮
3. 在无障碍设置中找到"ElementCollector"
4. 启用该无障碍服务
5. 返回应用

### 3. 授权悬浮窗权限
1. 点击"授权悬浮窗权限"按钮
2. 在系统设置中授予悬浮窗权限
3. 返回应用

### 4. 开始收集
启用服务后，应用会：
- 显示可拖拽的悬浮窗按钮
- 点击按钮手动收集当前页面的所有元素信息
- 自动弹出预览界面
- 保存数据到设备存储

### 5. 查看结果
- 收集完成后自动显示预览
- 在主界面点击文件列表中的任意文件进入预览
- 支持分享文件路径

## 输出数据格式

收集的数据以XML格式保存，提供最简洁的层级结构展示：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<PageElements>
  <PageInfo>
    <timestamp>2024-01-01 12:00:00</timestamp>
    <packageName>com.example.app</packageName>
    <className>com.example.app.MainActivity</className>
  </PageInfo>
  
  <Hierarchy>
    <Element className="android.widget.FrameLayout" 
             left="0" top="0" right="1080" bottom="1920" 
             width="1080" height="1920" centerX="540" centerY="960">
      <Element className="android.widget.LinearLayout" 
               left="0" top="0" right="1080" bottom="1920" 
               width="1080" height="1920" centerX="540" centerY="960">
        <Element className="android.widget.TextView" text="Hello World!" 
                 viewId="text_hello" left="50" top="100" right="1030" bottom="150" 
                 width="980" height="50" centerX="540" centerY="125"/>
        <Element className="android.widget.Button" text="Click Me" 
                 viewId="button_click" left="50" top="200" right="300" bottom="250" 
                 width="250" height="50" centerX="175" centerY="225" 
                 clickable="true" focusable="true"/>
        <Element className="android.widget.EditText" contentDescription="Input field" 
                 viewId="edit_input" left="50" top="300" right="1030" bottom="350" 
                 width="980" height="50" centerX="540" centerY="325" 
                 clickable="true" focusable="true"/>
      </Element>
    </Element>
  </Hierarchy>
  
  <Statistics>
    <totalElements>5</totalElements>
    <clickableElements>3</clickableElements>
    <textElements>2</textElements>
  </Statistics>
</PageElements>
```

## 元素信息详情

每个元素包含以下关键信息：

- **基本信息**: 类名、文本内容（仅非空）、内容描述（仅非空）、资源ID（仅非空）
- **位置信息**: 屏幕坐标、尺寸、中心点（仅有效尺寸）
- **属性信息**: 仅包含为true或有意义的属性（如clickable、focusable、checkable等）

**优化特点**：
- 只输出有意义的属性，避免冗余信息
- 移除无意义的Actions节点（过滤掉所有常见动作）
- 移除冗余的depth和path属性（XML层级已能体现）
- 合并层级结构和元素列表，避免重复
- 文件大小最小化，信息最关键

## 文件存储

收集的数据保存在：
```
/storage/emulated/0/Android/data/com.zia.elementcollector/files/element_collector_output/
```

文件名格式：`page_elements_YYYYMMDD_HHMMSS.xml`

## 权限要求

- `BIND_ACCESSIBILITY_SERVICE`: 无障碍服务权限
- `SYSTEM_ALERT_WINDOW`: 系统悬浮窗权限

## 技术实现

- **无障碍服务**: 使用Android AccessibilityService API
- **协程**: 使用Kotlin协程处理异步操作
- **XML处理**: 使用Java内置XML处理库
- **UI框架**: 使用Jetpack Compose构建现代化界面
- **悬浮窗**: 使用WindowManager实现可拖拽悬浮窗
- **状态监控**: 实时监控服务状态和权限变化
- **文件预览**: 支持完整内容滚动查看
- **视觉反馈**: 动画效果提升用户体验

## 开发环境

- Android Studio Hedgehog | 2023.1.1
- Kotlin 1.9.0
- Android Gradle Plugin 8.1.0
- Target SDK: 34
- Minimum SDK: 24

## 注意事项

1. 需要用户手动启用无障碍服务
2. 需要授权悬浮窗权限才能显示悬浮窗按钮
3. 某些系统应用可能无法收集元素信息
4. 收集的数据量可能较大，注意存储空间
5. 悬浮窗支持拖拽移动，可调整到合适位置
6. 建议在测试环境中使用

## 许可证

MIT License 