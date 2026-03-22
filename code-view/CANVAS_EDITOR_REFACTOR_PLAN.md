# Code View Canvas 自绘改造计划

## 文档关系

- 主计划文档：`CANVAS_EDITOR_REFACTOR_PLAN.md`
- 进度跟踪文档：`CANVAS_EDITOR_REFACTOR_PROGRESS.md`
- 输入锚点规则：`INPUT_ANCHOR_STATE_RULES.md`
- 选区交互规则：`SELECTION_INTERACTION_RULES.md`

两份文档的分工如下：

- 本文档负责记录目标、范围、设计方案、阶段拆分、风险和验收标准
- 进度文档负责记录当前状态、阶段推进情况、已确认决议、最近变更和阻塞项
- 输入锚点规则文档负责记录 IME / composing / commit / 焦点 / 锚点重定位的状态边界
- 选区交互规则文档负责记录 Desktop / Android 在 caret、滚动、长按、手柄与菜单上的分层边界

使用约定：

- 阶段编号必须与进度文档保持一致
- 如果设计方案发生变更，先更新本文档，再同步更新进度文档中的“决议记录”
- 如果只是执行状态变化，只更新进度文档即可
- 如果阶段被拆成更细的子任务，优先写入进度文档，不强制回写到计划文档

## 背景

本计划文档保留了这轮重构从“占位实现”演进到当前架构的背景。下面这组“现状问题”描述的是改造起点，而不是 2026-03-22 的当前代码状态。

改造起点中，`code-view-compose` 的 `CodeViewer` 与 `CodeEditorContent` 曾是占位实现：

- `CodeViewer` 仅使用 `BasicText` 直接显示全文
- `CodeEditorContent` 仅使用 `BasicTextField` 直接编辑全文
- 对外已经暴露的 `initialFirstVisibleLine`、`initialScrollOffsetX`、`selection`、`searchHighlight`、`cursorTarget`、`onScrollChange`、`onViewportChange`、`onAnnotationHit`、`onContextMenu` 等参数，尚未真正接入内部布局和交互逻辑

这意味着当时的实现无法稳定承载以下能力：

- 基于行号的 viewport 恢复与同步
- 行列坐标和全局文本偏移之间的准确映射
- Canvas 级别的高亮、选区、搜索命中、自绘光标
- 面向大文件的可视区裁剪渲染
- 后续编辑态需要的命中测试、输入桥接、自动滚动

因此，这次改造不能只做“把 `BasicText` / `BasicTextField` 替换成 `Canvas`”。必须先补齐一层共享的布局与 viewport 核心，再让 Viewer / Editor 共用。

## 目标

### 主要目标

1. 将 `CodeViewer` 改为基于 `Canvas` 的自绘渲染
2. 将 `CodeEditorContent` 改为“Canvas 显示 + 隐藏 IME host / 平台输入桥接”的编辑模型
3. 让编辑态的 caret、selection、命中与 reveal 全部回到 Canvas 主导，其中横向几何以 `TextLayoutResult` 为准，垂直行框以稳定行度量为准
4. 正式引入 viewport 概念，并让只读态与编辑态共享同一套滚动和可见区定义
5. 将现有公开参数真正接入内部逻辑，保证外部调用方状态可恢复、可同步
6. 建立后续可扩展的代码坐标系统，为折叠、行号栏、诊断波浪线、代码补全定位等能力打基础

### 次要目标

- 将渲染限制在可见区内，避免每帧处理整份文本
- 将语法高亮 token、annotation 命中、选区命中统一到同一套布局模型
- 为大文件降级路径保留扩展位

## 非目标

本轮改造默认不把以下内容作为必须完成项：

- 行号栏、断点栏、折叠 gutter
- 代码补全、悬浮提示、诊断波浪线
- 复杂富文本编辑命令系统
- 多光标、矩形选区
- 软换行布局

默认先按“单行不换行、横向滚动、基于逻辑行渲染”的模型推进。

## 现状问题

### 1. UI 层没有真实布局模型

当前外部状态以 `LineSelection`、首可见行、横向滚动为主，但内部没有：

- `line -> offset`
- `offset -> line / column`
- token 按行切片结果
- 可视区内行范围
- 文本像素坐标和字符坐标映射

这会导致任何选区、点击定位、程序化滚动都缺少稳定基础。

### 2. 只读态与编辑态没有共享坐标体系

如果直接分别实现两个版本：

- Viewer 单独做 Canvas 绘制
- Editor 单独做输入处理

那么后面会出现双份命中测试、双份滚动逻辑、双份选区映射，维护成本高，而且行为容易不一致。

### 3. “透明 BasicTextField 作为编辑主控”存在较高风险

最新实现与手测已经证明，这条路线会把编辑态拆成两套系统：

- `BasicTextField` 内部维护一套文本布局、选区和 composing 语义
- `Canvas` 再维护一套视觉绘制、光标、命中和 reveal 逻辑

主要风险包括：

- composing 文本直接落入正文时，会触发 `TextFieldValue`、`CodeDocument`、Canvas 三条链路的高频同步
- 输入控件内部布局与 Canvas 自绘布局来源不同，容易出现 caret 横向错位
- 点击、拖拽、选区、自动滚动被拆成两套系统，平台差异更难收敛
- Android / Desktop 下的焦点、IME 候选框和删除语义更容易出现抖动或回归

因此当前主方案调整为：

- `Canvas` 负责完整显示，且成为编辑态唯一视觉真相源
- `CodeEditor` 自己管理文本、选区、光标、点击定位、拖拽选区和 reveal
- Desktop 使用一个附着在 AWT 窗口上的专用输入宿主组件承接 IME 会话，并单独处理普通键盘命令
- Android 不再将隐藏的 `0x0 BasicTextField` 视为长期方案，后续改为更底层的平台文本输入桥
- 后续如果需要更精确的 IME 锚点，再考虑在移动端为折叠 caret 追加平台锚点能力

## 核心设计

### 零、公开 API 预调整

在进入 Canvas / viewport 重构前，先补齐公开参数语义，避免底层实现完成后再因 API 不完整返工。

#### 1. 新增 `cursor: Cursor?`

建议新增独立的公开类型：

```kotlin
@CodeViewApi
public data class Cursor(
    val line: Int,
    val offset: Int,
)
```

设计意图：

- `selection: LineSelection?` 只表达范围选区
- `cursor: Cursor?` 只表达 caret 位置与是否绘制
- `cursor == null` 表示不绘制 caret
- `cursor != null` 表示在指定位置绘制 caret

不建议继续复用折叠态 `selection` 作为唯一光标语义，原因是：

- 范围选区和活动 caret 是两个不同概念
- 上层状态当前本来就将 `cursorLine` / `cursorOffset` 与 `selection` 分开保存
- 后续编辑态命中、滚动 reveal、只读态是否显示 caret 都更适合由独立 `cursor` 表达

#### 2. `CodeViewer` 的光标语义

- `CodeViewer` 通过 `cursor: Cursor?` 控制 caret 是否显示
- `cursor == null` 时，不绘制 caret
- `cursor != null` 时，在对应位置绘制 caret
- `cursorTarget` 继续只负责程序化滚动定位，不承担 caret 可见性语义

#### 3. `CodeEditor` 的光标语义

`CodeEditor` 需要先补 `readOnly` 概念，再定义 caret 行为：

- `readOnly = false` 时，caret 强制显示
- `readOnly = false` 时，即使 `cursor == null`，也只表示“位置由内部编辑态决定”，不表示隐藏 caret
- `readOnly = false` 且 `cursor != null` 时，可视为受控 caret 位置
- `readOnly = true` 时，才允许由 `cursor: Cursor?` 控制是否显示 caret

也就是说：

- 编辑态：始终显示 caret
- 只读态：允许隐藏 caret

#### 4. `CodeEditor` 参数面需要与 `CodeViewer` 对齐

当前 `CodeEditor` 参数明显不足，后续至少需要补齐以下能力：

- `textStyle: TextStyle = CodeViewDefaults.CodeTextStyle`
- `readOnly: Boolean = false`
- `selection: LineSelection? = null`
- `cursor: Cursor? = null`
- `searchHighlight: LineSelection? = null`
- `initialFirstVisibleLine: Int = 0`
- `initialScrollOffsetX: Int = 0`
- `cursorTarget: CodeViewerCursorTarget? = null`
- `interactionOptions: CodeViewerInteractionOptions = CodeViewerInteractionOptions()`
- `onSelectionChange: ((LineSelection?) -> Unit)? = null`
- `onCursorChange: ((Cursor?) -> Unit)? = null`
- `onScrollChange: ((firstVisibleLine: Int, scrollOffsetX: Int) -> Unit)? = null`
- `onViewportChange: ((firstVisibleLine: Int, lastVisibleLine: Int) -> Unit)? = null`
- `onAnnotationHit: ((CodeAnnotationHit) -> Unit)? = null`
- `onContextMenu: ((annotationHit: CodeAnnotationHit?, offset: Offset) -> Unit)? = null`

这些参数不要求在 API 调整阶段全部完成实现，但至少要先确定公开语义。

#### 5. 推荐的 V1 API 草案

建议将 `Cursor` 放在 `code-view-core` 的 `text` 包下，与 `LineSelection` 保持同级。

推荐签名如下：

```kotlin
@CodeViewApi
public data class Cursor(
    val line: Int,
    val offset: Int,
)

@CodeViewApi
@Composable
public fun CodeViewer(
    document: CodeDocument,
    addons: CodeAddons,
    modifier: Modifier = Modifier,
    runtime: CodeRuntime = remember { CodeRuntime() },
    textStyle: TextStyle = CodeViewDefaults.CodeTextStyle,
    initialFirstVisibleLine: Int = 0,
    initialScrollOffsetX: Int = 0,
    selection: LineSelection? = null,
    cursor: Cursor? = null,
    searchHighlight: LineSelection? = null,
    cursorTarget: CodeViewerCursorTarget? = null,
    interactionOptions: CodeViewerInteractionOptions = CodeViewerInteractionOptions(),
    onScrollChange: ((firstVisibleLine: Int, scrollOffsetX: Int) -> Unit)? = null,
    onViewportChange: ((firstVisibleLine: Int, lastVisibleLine: Int) -> Unit)? = null,
    onAnnotationHit: ((CodeAnnotationHit) -> Unit)? = null,
    onContextMenu: ((annotationHit: CodeAnnotationHit?, offset: Offset) -> Unit)? = null,
)

@CodeViewApi
@Composable
public fun CodeEditor(
    text: String,
    onTextChange: (String) -> Unit,
    languageId: CodeLanguageId,
    addons: CodeAddons,
    modifier: Modifier = Modifier,
    runtime: CodeRuntime = remember { CodeRuntime() },
    textStyle: TextStyle = CodeViewDefaults.CodeTextStyle,
    readOnly: Boolean = false,
    selection: LineSelection? = null,
    cursor: Cursor? = null,
    searchHighlight: LineSelection? = null,
    initialFirstVisibleLine: Int = 0,
    initialScrollOffsetX: Int = 0,
    cursorTarget: CodeViewerCursorTarget? = null,
    interactionOptions: CodeViewerInteractionOptions = CodeViewerInteractionOptions(),
    onSelectionChange: ((LineSelection?) -> Unit)? = null,
    onCursorChange: ((Cursor?) -> Unit)? = null,
    onScrollChange: ((firstVisibleLine: Int, scrollOffsetX: Int) -> Unit)? = null,
    onViewportChange: ((firstVisibleLine: Int, lastVisibleLine: Int) -> Unit)? = null,
    onAnnotationHit: ((CodeAnnotationHit) -> Unit)? = null,
    onContextMenu: ((annotationHit: CodeAnnotationHit?, offset: Offset) -> Unit)? = null,
)

@CodeViewApi
@Composable
public fun CodeEditor(
    document: CodeDocument,
    addons: CodeAddons,
    modifier: Modifier = Modifier,
    runtime: CodeRuntime = remember { CodeRuntime() },
    textStyle: TextStyle = CodeViewDefaults.CodeTextStyle,
    readOnly: Boolean = false,
    selection: LineSelection? = null,
    cursor: Cursor? = null,
    searchHighlight: LineSelection? = null,
    initialFirstVisibleLine: Int = 0,
    initialScrollOffsetX: Int = 0,
    cursorTarget: CodeViewerCursorTarget? = null,
    interactionOptions: CodeViewerInteractionOptions = CodeViewerInteractionOptions(),
    onTextChange: ((String) -> Unit)? = null,
    onSelectionChange: ((LineSelection?) -> Unit)? = null,
    onCursorChange: ((Cursor?) -> Unit)? = null,
    onScrollChange: ((firstVisibleLine: Int, scrollOffsetX: Int) -> Unit)? = null,
    onViewportChange: ((firstVisibleLine: Int, lastVisibleLine: Int) -> Unit)? = null,
    onAnnotationHit: ((CodeAnnotationHit) -> Unit)? = null,
    onContextMenu: ((annotationHit: CodeAnnotationHit?, offset: Offset) -> Unit)? = null,
)
```

说明：

- `CodeViewer` 与 `CodeEditor` 在滚动、viewport、注解交互参数上尽量对齐
- `CodeEditor(text, onTextChange, ...)` 作为受控文本重载，符合常规 Compose 使用习惯
- `CodeEditor(document, ...)` 作为工作区主路径和高级场景重载，方便直接复用 `CodeDocument`
- `initialText` 形式的非受控重载可以保留给简单示例，但不建议继续扩展太多高级参数

#### 6.1 `textStyle` 约束

- `CodeViewer` 与 `CodeEditor` 都应允许显式传入 `textStyle`
- `Canvas` 测量、正文绘制和透明输入层必须使用同一套 `textStyle`，避免桌面端出现 caret 偏移和编辑抖动
- Desktop 侧已经观察到“光标定位不准确”和“输入时编辑行抖动”，因此 `textStyle` 需要作为首个排查入口保留给上层显式控制
- 若继续沿用“单字符整数宽度”测量，Desktop 侧会积累列宽误差，因此字符宽度和行高应优先使用长样本平均值测量
- 对包含汉字的文本和 IME composing，单纯的固定列宽模型仍然不够，编辑态需要进一步使用按行真实宽度测量的 x 坐标来绘制分段文本、选区、caret 和 reveal
- 当前实现虽然已经不再依赖纯固定字符宽度绘制正文，但自定义 `textStyle` 仍最好保持等宽字体并提供明确 `lineHeight`
- 若只覆盖部分样式属性，未指定部分应回退到 `CodeViewDefaults.CodeTextStyle`

#### 6. `selection` 与 `cursor` 的配套语义

引入 `cursor` 之后，需要同时约束它与 `selection` 的关系：

- 在 `CodeViewer` 与 `CodeEditor(readOnly = true)` 中：
  - `selection == null && cursor == null`
    表示没有外部受控选区，也不绘制 caret
  - `selection == null && cursor != null`
    表示仅绘制 caret，无范围选区
  - `selection != null && selection.isCollapsed && cursor != null`
    表示折叠选区，caret 位置应与折叠选区一致
  - `selection != null && !selection.isCollapsed && cursor != null`
    表示存在范围选区，`cursor` 表示当前活动端点

- 在 `CodeEditor(readOnly = false)` 中：
  - `cursor != null`
    表示受控 caret 位置
  - `cursor == null`
    表示 caret 位置由内部编辑态维护，但 caret 仍然必须显示
  - `selection != null && !selection.isCollapsed`
    表示存在范围选区，若同时传入 `cursor`，则 `cursor` 表示当前活动端点

之所以需要 `cursor` 独立存在，是因为当前 `LineSelection` 并不表达 anchor / caret 方向，只表达范围。后续编辑态如果要支持键盘扩选、拖拽扩选、Shift 方向扩展，就需要一个单独的活动 caret 概念。

#### 7. 回调语义建议

- `onSelectionChange`
  用于回传当前范围选区
- `onCursorChange`
  用于回传当前 caret 位置
- `onSelectionChange` 与 `onCursorChange` 在编辑态应成对更新，避免上层状态割裂
- 若后续发现上层需要更强一致性，可再评估引入 `CodeEditorSelectionState` 之类的组合值对象，但第一阶段不强制

### 一、共享布局层

先建立一层内部布局快照，作为 Viewer / Editor 的共同基础。该层职责：

- 维护文本逻辑行切分
- 建立全局 offset 与 `(line, column)` 的双向映射
- 维护每行文本内容、长度、起始 offset
- 将 `CodeTokenSpan` 按逻辑行切片
- 提供 annotation 的命中区间映射
- 提供 viewport 裁剪后的可见行范围

建议的内部模型示意：

```kotlin
internal data class CodeLayoutSnapshot(
    val text: String,
    val lineStarts: IntArray,
    val lines: List<CodeLineLayout>,
    val maxLineLength: Int,
    val tokensByLine: List<List<CodeLineTokenSpan>>,
    val annotations: List<CodeAnnotationRange>,
)

internal data class CodeLineLayout(
    val lineIndex: Int,
    val startOffset: Int,
    val endOffsetExclusive: Int,
    val content: String,
    val length: Int,
)
```

这一层优先放在 `code-view-compose` 内部实现，先不要急着公开 API。等坐标模型稳定后，再决定是否下沉到 `code-view-core`。

### 布局层需要提供的能力

- `offsetToLineColumn(offset)`
- `lineColumnToOffset(line, column)`
- `clampLineSelection(selection)`
- `toGlobalSelection(lineSelection)`
- `toLineSelection(globalSelection)`
- `findVisibleLines(firstVisibleLine, viewportHeightPx, lineHeightPx)`
- `findOffsetByPosition(x, y, viewport)`
- `findAnnotationHit(offset)`

### 二、视口模型

需要正式定义内部 viewport，而不是把它散落在参数和回调里。

建议最少包含：

```kotlin
internal data class CodeViewportState(
    val firstVisibleLine: Int,
    val horizontalScrollPx: Float,
    val viewportWidthPx: Float,
    val viewportHeightPx: Float,
    val lineHeightPx: Float,
)
```

基于该状态，可以稳定导出：

- `visibleLineCount`
- `lastVisibleLine`
- `contentHeightPx`
- `contentWidthPx`
- 某一行在屏幕中的 `y`
- 某一列在屏幕中的 `x`

### viewport 需要承接的行为

- 初始化恢复 `initialFirstVisibleLine`
- 初始化恢复 `initialScrollOffsetX`
- 用户滚动后的 `onScrollChange`
- 可见行变化后的 `onViewportChange`
- 处理 `cursorTarget` 的程序化 reveal
- 输入后自动将 caret 保持在可见区域

### 滚动模型建议

- 垂直方向先按“整行逻辑滚动”实现，状态继续使用 `firstVisibleLine`
- 水平方向使用像素滚动值 `horizontalScrollPx`
- 如果后续需要更细粒度纵向滚动，可扩展为 `verticalScrollPx`，但第一阶段不强制引入

### 三、Canvas 渲染层

Viewer 与 Editor 都复用同一个渲染器，只是在编辑态额外绘制 caret 与处理输入桥接。

渲染顺序建议固定为：

1. 背景
2. 搜索高亮背景
3. 普通选区背景
4. 当前行或光标行背景（如果需要）
5. 语法高亮文本
6. 光标
7. 调试辅助绘制（可选）

### 文本渲染策略

当前实现采用“按可见行逐行绘制 + 稳定行度量 + 分段文本布局”的策略：

- 每一帧只处理 viewport 内可见行
- 每行先基于 token 颜色与字符脚本分组生成可复用的渲染分段
- 横向点击定位、caret、annotation 命中与横向 reveal 继续复用 `TextLayoutResult`
- 垂直行框不再由当前行瞬时 mixed layout 决定，而是统一使用字体配置级的稳定行度量
- `charWidthPx` 仅保留给行尾 cursor 宽度与少量 fallback 场景

### 文本测量建议

需要明确区分“稳定行框”和“横向几何真值”：

- `lineHeightPx`、`contentHeightPx`、`contentTopPaddingPx` 与共享 `baselinePx` 由字体配置级样本测量统一导出
- `charWidthPx` 只作为少量 fallback 场景的辅助值
- 每个可见逻辑行仍需要可复用的 `TextLayoutResult`，但它主要服务于横向几何，不再直接决定整行垂直行框
- 混排行中的可见文本按渲染分段单独测量与绘制，避免单个 mixed layout 的瞬时 baseline 把整行带偏

后续点击定位、selection、caret、annotation 命中和横向 reveal 都应优先基于可复用的 `TextLayoutResult`，而不是只依赖固定列宽推导。

### 四、输入桥接层

编辑态采用“显示层”和“输入桥接层”分离，但编辑主控不再交给可见输入控件：

- `Canvas` 负责真实视觉输出
- `CodeEditor` 自己管理 `TextFieldValue`、selection、cursor、点击定位、拖拽与 reveal
- Android 改用更底层的平台文本输入桥负责 IME 连接、composing、commit 与平台编辑命令
- Desktop 使用专用输入宿主组件处理 IME 会话、普通输入、删除、粘贴、导航与全选
- 平台输入接入层通过 `expect/actual` bridge 收口，不在公共编辑器中继续堆平台条件分支
- 不引入外部封装的 `TextField` 组件，避免 `code-view` 与其他 UI 模块形成反向依赖

### 当前输入桥接方向

- `CodeEditor` 不再二次嵌套 `CodeViewer`，而是直接复用同一个 `CodeViewerCanvas` 和同一组滚动容器
- `Canvas` 成为正文、selection、caret、命中和横向 reveal 的唯一视觉真相源
- 编辑态仍使用内部 `TextFieldValue` 承接平台输入语义，但 `composition != null` 时不立即写回 `CodeDocument`
- Desktop 的 IME 与键盘路径不依赖隐藏输入框
- Android 当前代码已切到 `AndroidInputHostView + InputConnection` 输入桥，不再依赖隐藏 `BasicTextField`
- Android 后续目标是继续稳固这条低层输入桥，并在必要时再评估 Compose 平台文本输入 session API，而不是回到不可见文本框补丁路线
- 混排渲染当前已切到“稳定行度量 + 分段绘制”路径，`TextLayoutResult` 主要保留给横向几何与命中计算

这样做的好处是：

- composing 不再直接驱动正文和 Canvas 双重刷新
- caret、selection、命中和 reveal 都能复用同一套几何来源
- Viewer / Editor 共用同一套 scroll state，避免双层滚动不同步
- Desktop 行为更接近旧版，平台差异更容易收敛

Android 侧已确认隐藏 `BasicTextField` 方案的结构性问题：

- 删除命令无法稳定映射回真实文档
- 软键盘触发的选择 / 全选等编辑命令会落在宿主自身，而不是回放到真实编辑状态
- 继续补丁化处理会让宿主越来越像“第二套编辑器”，违背当前分层目标

因此 Android 新桥必须满足：

- 只负责 IME 会话、composing、commit 与平台编辑命令接入
- 不拥有整份真实文档，也不成为第二套可编辑文本源
- 能直接承接删除、选区更新、软键盘侧选择 / 全选 / 剪贴板类动作
- 所有真实编辑结果都统一回放到 `CodeEditor` 状态机

### 四点五、Android Overscroll 探索

Android overscroll 当前已确认最终落地方向：正文 stretch 使用平台 overscroll，光标手柄、选区手柄、工具栏与相关 overlay 放入同一 overscroll 视觉层统一变形。

探索原因：

- 代码编辑器的正文、caret、选择手柄、光标手柄、工具栏和输入锚点天然分层，若 overlay 留在系统 stretch 之外，就会出现对不齐或“冲出去再收回”的问题
- AndroidX / AOSP 的平台 stretch 本质上是 `RenderNode + EdgeEffect` 的硬件层变形，不适合在公共 Canvas 层继续用 `scale` 或手工坐标映射去复刻
- 只对手柄额外附加简单平移，或单独做 stretch / unStretch 估算，都会引入抖动、过冲和命中不一致

当前探索目标：

- 正文直接使用平台 overscroll stretch，而不是在编辑器内部再次自绘 stretch
- 光标手柄、选区双手柄、工具栏与相关 overlay 跟正文进入同一 overscroll 视觉层
- overlay 命中与拖拽优先回到基础 scroll 坐标模型，不再额外做一套手工 stretch / unStretch 反算
- 若后续需要扩展，仅考虑在平台 stretch 之上补少量平台专用能力，不再回到 editor-local 自定义 stretch 主方案

当前已知风险：

- 若 overlay 没有进入同一 overscroll 视觉层，就仍然会出现正文和手柄不对齐
- 若后续再次回到公共层的手工 stretch / unStretch 估算，容易重新引入字符抖动、手柄过冲和拖拽错位
- 平台 overscroll 的视觉主导权必须保留在 Android 平台层，公共层只应维护必要的滚动与交互状态

当前该方向已通过用户手测确认正文 stretch、光标手柄与选区双手柄对齐效果，可作为 Android 默认方案继续回归验证。

当前阶段交互约束：

- `CodeViewer` 可以响应 annotation 主点击与上下文命中
- `CodeEditor` 的主点击优先保留给文本选择和 caret 定位
- `CodeEditor` 如需暴露 annotation 交互，当前优先只开放上下文命中，不抢占主点击

### 编辑态 Selection 分层

`CodeEditor` 的 Selection 交互不应再尝试用一套手势同时覆盖 Desktop 和 Android。

推荐约束如下：

- Desktop 走 `mouse-first`：
  - 主点击放置 caret
  - 拖拽直接扩展选区
  - 次键负责上下文菜单
- Android 走 `touch-first`：
  - 单击只放置 caret
  - 普通拖动优先交给滚动
  - 长按进入选区
  - 长按后先按词选中，再打开菜单入口
  - 后续扩选通过 `Selection Handle` 手柄完成

这里的差异不应通过在公共编辑逻辑里直接判断“是否 Android / 是否 Desktop”实现，而应继续通过平台桥接层暴露能力开关收口。

当前更合适的抽象是：

- 是否使用浮动输入锚点
- 是否启用 touch-first 的 Selection 手势

而不是：

- 业务层直接判断平台名

### 不建议作为基础架构的方案

- 全屏透明 `BasicTextField` 作为编辑主控
- 透明输入控件跟随选区或 caret 位置移动

理由：

- 选区是范围，不是点
- 多行选区无法自然映射到一个输入框位置
- 可见输入控件的内部布局与 Canvas 几何容易打架
- 容易引入 IME 候选框、焦点和平台差异问题

### 第二阶段可评估增强

如果后续需要更精确的 IME 候选框定位，可在移动端的“折叠选区”下补平台锚点：

- 平台输入桥仍负责输入
- 但额外维护一个 caret 平台锚点坐标，供输入法候选框定位使用

这应作为增强项，而不是当前基础架构。

### 输入锚点方案补充

当前已确认一个更明确的方向：输入宿主不再承担“透明编辑层”角色，而只承担平台输入桥接角色。

输入锚点的职责边界如下：

- 它不是可见编辑器，画布仍然是唯一可见文本来源
- 它不绑定整份文档文本，只维护独立的 `imeFieldValue`
- 它只负责：
  - 建立平台输入法 / 文本输入会话
  - 承载 `composition != null` 的预输入片段
  - 在 `composition == null` 时产出最终 commit 文本
- 它不负责：
  - 绘制正文
  - 绘制 caret
  - 绘制 selection
  - 命中测试
  - 方向键 / Home / End / 删除 / 粘贴等命令键逻辑

也就是说，输入锚点只是“IME 锚点 + 输入会话宿主”，其余一切都交给画布与编辑状态机。

#### 中文输入法语义

输入锚点需要正确区分 composing 与 commit：

- `composition != null`
  - 说明当前仍处于预输入阶段
  - 画布只显示临时 composing 片段
  - 不将该片段正式写入文档
- `composition == null && text.isNotEmpty()`
  - 说明输入法已经完成一次最终提交
  - 不论提交结果是汉字还是拼音原文，都统一作为 commit 文本写入文档
  - 提交完成后立即清空 `imeFieldValue`

因此：

- 空格确认候选汉字，本质上是一种 commit
- 回车确认拼音原文，本质上也是一种 commit
- 编辑器不需要自行判断“空格”和“回车”的输入法语义差异，只需要根据 `composition` 是否存在来区分预输入和正式提交

#### 命令键与跨行场景

输入锚点方案还必须明确处理“编辑命令导致光标离开当前行”的情况，例如：

- 直接按回车插入换行
- Backspace / Delete 导致跨行合并
- 方向键、Home / End、鼠标点击导致光标跳转
- 选区替换后光标落到新的位置

这里的核心规则是：

- 输入锚点永远服务于“当前画布光标”
- 它不能长期绑定在某次输入开始时的旧位置
- 一旦编辑命令改变了真实文档或真实光标位置，输入锚点必须立即重新定位

更具体地说：

- 普通编辑命令（例如直接回车换行）由编辑状态机处理
- 文档和画布光标更新后，输入锚点在下一帧跟随新的画布光标位置移动
- 如果当前仍存在 composing 片段，而编辑命令会改变文档结构或光标位置，则应优先：
  - 结束当前 composing
  - 或清空 / 重置输入锚点的局部 `imeFieldValue`

也就是说，输入锚点不是“当前这一轮输入的所有者”，而只是“当前光标位置的输入入口”。

#### 输入锚点定位原则

输入锚点如果需要跟随光标，应满足以下条件：

- 跟随的是“画布光标在当前 viewport 中的坐标”，而不是滚动内容内部的普通控件位置
- 它不能成为滚动内容的一部分，否则很容易在获取焦点时触发 `bringIntoView`
- 它不能接管鼠标事件
- 它不能直接接管整套命令键

推荐定位模型：

- 画布继续负责可见内容
- 输入锚点作为独立浮层存在
- 其位置由当前光标的 viewport 坐标驱动
- 命令键仍由编辑状态机处理

此外还需要保证：

- 输入锚点的焦点获取不会触发滚动容器的 `bringIntoView` 抖动
- 输入锚点位置更新不会吞掉鼠标事件
- 输入锚点失焦、输入法取消、候选提交等状态变化都能及时同步回画布状态机

#### 当前实现取舍

桌面端早期探索中曾验证：

- “全屏透明输入宿主”更容易维持文本输入可用性
- “跟随光标的小宿主”更有希望解决候选窗锚点问题

当前阶段已经确认：

- Desktop 专用输入宿主已经可以稳定承接输入
- Desktop 输入宿主的状态机、自动焦点与命令键打断规则已跑通
- 画布侧 `inline composing overlay` 已落地
- `selection + composing` 已按“首次进入 composing 时先真实删除选区”策略实现
- `composing` 期间的可见光标、输入锚点与自动 reveal 已接入 preedit 内部 caret
- Desktop 候选词窗口当前已能跟随输入锚点移动，整体效果达到当前阶段预期
- Desktop `InputMethodEvent` 中的 preedit 前缀不再提前写入正文，整段 preedit 会保留到最终 commit
- Android 隐藏 `BasicTextField` 路线已确认暴露结构性缺陷，不再继续作为长期实现推进

因此后续不再需要围绕“桌面候选窗是否能跟随光标”继续试错，剩余工作应转向边界一致性验证和交互收尾。

#### 仍需重点校验的边界

输入锚点方案在继续落地前，应至少验证以下边界：

- composing 期间直接回车、Backspace、Delete、方向键移动是否会导致状态错乱
- composing 期间鼠标点击其他位置后，旧 composing 是否会被正确结束或丢弃
- 输入锚点重新定位后，候选窗是否仍能稳定跟随
- 输入锚点失焦后，未提交 composing 是否会被平台输入法吞掉或异常保留
- 长行横向滚动、纵向滚动后，输入锚点是否仍能落在正确 viewport 坐标

### 五、交互命中模型

需要统一处理以下命中逻辑：

- 点击定位 caret
- 拖拽更新选区
- 双击选词
- 程序化跳转到指定行列
- annotation 点击
- annotation 右键菜单命中

推荐统一走：

1. 屏幕坐标
2. viewport 坐标系
3. `(line, column)`
4. 全局 offset
5. selection / annotation / token

避免在多个层级重复做坐标换算。

## 模块与文件调整建议

### 重点修改模块

- `code-view/code-view-compose`

### 可能新增的内部文件

- `CodeLayoutSnapshot.kt`
- `CodeViewportState.kt`
- `CodeCoordinateMapper.kt`
- `CodeCanvasRenderer.kt`
- `CodeViewerState.kt`
- `CodeEditorInputBridge.kt`
- `CodeSelectionMapper.kt`

### 预计需要修改的现有文件

- `code-view/code-view-compose/src/commonMain/kotlin/io/github/dexclub/codeview/compose/CodeViewer.kt`
- `code-view/code-view-compose/src/commonMain/kotlin/io/github/dexclub/codeview/compose/CodeEditor.kt`
- `code-view/code-view-compose/src/commonMain/kotlin/io/github/dexclub/codeview/compose/CodeViewerOptions.kt`
- `code-view/code-view-core/src/commonMain/kotlin/io/github/dexclub/codeview/core/text/*`

### 是否需要修改其他模块

第一阶段尽量不改 `code-view-core` 公开结构，优先把布局实现收敛在 `compose` 模块内部。

只有在出现以下情况时，才考虑下沉到 `core`：

- 多个平台 UI 层都需要共享布局模型
- `LineSelection` / `CodeSelection` 的转换能力需要对外公开
- annotation 命中结构需要跨模块复用

## 分阶段实施计划

### 阶段 0：基线整理

目标：

- 清点现有参数的真实使用方式
- 明确首批必须兼容的外部行为
- 先确定 `cursor: Cursor?` 与 `readOnly` 的公开语义

任务：

- 核对 `CodeViewPane` 对 `CodeViewer` 的依赖
- 确定 `selection`、`searchHighlight`、`cursorTarget`、`onScrollChange`、`onViewportChange` 为首批必须落地的能力
- 确定 `CodeViewer` 与 `CodeEditor` 的 caret 语义
- 补齐 `CodeEditor` 的参数设计，至少在文档层明确对齐目标
- 保留公开 API 形状，尽量不破坏上层调用

交付：

- 文档确认
- 变更边界明确
- 公开 API 草案确定

### 阶段 1：共享布局快照

目标：

- 让文本拥有稳定的“行与偏移坐标系”

任务：

- 解析全文并生成 `lineStarts`
- 建立 `offset <-> line/column` 双向映射
- 将 token 切分到每一逻辑行
- 提供选区裁剪与转换工具
- 统一处理空文本、末尾换行、超界 offset

风险点：

- 末尾换行时最后一行边界容易出错
- token 跨行切片需要避免 off-by-one

验收：

- 同一份文本在任意 offset 上都能稳定映射回行列
- `LineSelection` 与全局选区往返转换不丢信息

### 阶段 2：viewport 状态层

目标：

- 让滚动与可见区成为正式状态，而不是临时计算

任务：

- 定义内部 viewport 状态对象
- 建立首可见行和最后可见行计算
- 接入 `initialFirstVisibleLine`
- 接入 `initialScrollOffsetX`
- 对外回调 `onScrollChange`
- 对外回调 `onViewportChange`
- 接入 `cursorTarget` 的 reveal 逻辑

风险点：

- 需要避免 `cursorTarget` 重复触发滚动
- 回调和内部状态之间要避免循环更新

验收：

- 外部恢复滚动后画面正确
- 导航跳转时能自动滚动到目标行
- viewport 变化能稳定回传给上层

### 阶段 3：CodeViewer Canvas 化

目标：

- 先完成只读态渲染闭环

任务：

- 用 `Canvas` 替换 `BasicText`
- 仅绘制可见行
- 按 token 分段绘制颜色和字形样式
- 绘制 `selection`
- 绘制 `searchHighlight`
- 支持基础点击命中 annotation
- 支持右键菜单命中 annotation

风险点：

- token 样式到 Canvas 文本样式的映射需要一致
- 横向滚动后命中坐标必须同步扣除偏移

验收：

- 只读模式下显示效果正确
- 搜索高亮和已有选区可见
- annotation 点击和右键上下文可正常工作

### 阶段 4：Viewer 交互与命中细化

目标：

- 补齐 Viewer 的完整坐标交互

任务：

- 点击空白区时定位到行尾
- 点击文本时按最近列命中
- 统一坐标换算工具
- 处理超长行与横向滚动场景

验收：

- 不同行宽、不同滚动位置下点击结果一致

### 阶段 5：CodeEditor 输入桥接

目标：

- 在不放弃 IME 的前提下，将显示逻辑完全切到 Canvas

任务：

- 引入内部 `TextFieldValue` 状态
- 将编辑态点击、拖拽、selection、caret 与 reveal 逻辑收回 `CodeEditor`
- Android 接入更底层的平台文本输入桥，当前主链为 `AndroidInputHostView + InputConnection`，后续按需要评估 Compose 平台文本输入 session API
- Desktop 接入专用输入宿主组件，统一处理 IME、键盘输入与剪贴板路径
- 将 `TextFieldValue.selection` 与内部选区映射同步
- `composition != null` 时不立即更新 `CodeDocument`
- Canvas 绘制 caret 和选区；横向几何统一使用 `TextLayoutResult`，垂直行框统一使用稳定行度量
- 输入后自动 reveal caret
- 接入基础键盘操作与拖拽选区

需要优先保证的输入路径：

- 普通输入
- 删除 / 退格
- 替换选区
- 粘贴
- 全选

风险点：

- `String` 与 `TextFieldValue` 双状态不同步
- Android 新输入桥需要覆盖删除、选区更新与软键盘编辑动作，平台语义梳理成本较高
- IME composing、退格与 commit 语义需要继续做平台手测
- 编辑后 token 刷新存在异步延迟

验收：

- 文本编辑正确
- 选区替换正确
- 光标始终可见
- 焦点切换不丢输入

### 阶段 6：性能与稳定性

目标：

- 保证大文本场景下仍可用
- 避免 decoration 刷新时重复计算与文本本身无关的布局基础数据

任务：

- 限制每帧只绘制可见行
- 对布局快照做必要缓存
- 将“按文本分行的基础布局”和“按 token / annotation 装饰”拆层缓存
- 将“稳定行度量采样”“渲染分段切分”“按段文本布局”继续保持为独立可缓存层
- 避免高频滚动下重复解析整份文本
- 评估长行场景下的横向滚动成本
- 保持大文件降级路径兼容 runtime 当前策略

验收：

- 普通代码文件滚动平稳
- 大文件下不出现明显卡死

### 阶段 7：回归与收尾

目标：

- 确认新架构与现有工作区主路径兼容
- 为核心非 UI 逻辑补自动回归，避免后续交互重构引入坐标退化

任务：

- 检查 `CodeViewPane` 的滚动恢复
- 检查搜索高亮恢复
- 检查导航跳转 reveal
- 检查 annotation 点击与右键菜单
- 为布局快照、坐标映射、viewport reveal 增补 `commonTest`
- 编译 `code-view-compose` 双端
- 视情况编译主工程使用路径

建议至少在 `code-view` 目录内执行：

- `./gradlew :code-view-compose:compileKotlinJvm`
- `./gradlew :code-view-compose:compileAndroidMain`

如果联动主工程验证，则补：

- `./gradlew :sharedUI:compileKotlinJvm`
- `./gradlew :sharedUI:compileAndroidMain`

## 风险与取舍

### 1. 输入层方案取舍

主张：

- Desktop 保持“专用输入宿主 + Canvas 自管编辑”
- Android 改为“更底层的平台文本输入桥 + Canvas 自管编辑”

原因：

- 工程复杂度仍可控
- 能避免可见输入控件内部布局与 Canvas 布局互相干扰
- 能避免 Android 隐藏 `BasicTextField` 继续吞掉删除和软键盘编辑动作
- 更容易把问题收敛到“状态同步 + 单一几何来源”

代价：

- 需要自己绘制选区和 caret
- 需要自己维护命中与自动滚动
- 需要自己处理 Desktop 键盘输入与移动端 IME 桥接细节

### 2. 是否一次性做完整编辑器

不建议一次性把所有编辑功能堆进去。

建议先后顺序：

1. 只读 Canvas Viewer
2. viewport 与命中测试稳定
3. Editor 输入桥接
4. 输入法与边角行为打磨

原因：

- 这样能把可视化问题和输入问题分开排查
- 避免同时引入绘制、滚动、IME、选区四类问题

### 3. 是否公开布局 API

第一阶段不建议公开。

原因：

- 内部模型还在探索期
- 过早公开会增加后续重构成本

## 验收标准

以下能力全部满足后，可视为本轮改造达标：

- `CodeViewer` 不再依赖 `BasicText` 显示正文
- `CodeEditorContent` 不再依赖 `BasicTextField` 直接负责视觉显示
- `viewport` 成为内部正式状态，并能正确回调外部
- `selection` / `searchHighlight` / `cursorTarget` 都能在 Canvas 模式下工作
- annotation 点击与右键菜单命中正常
- 编辑态下普通输入、删除、替换选区、粘贴可正常工作
- Android / JVM 均可编译通过

## 推荐执行顺序

建议实际落地时按下面顺序推进：

1. 共享布局快照
2. viewport 状态层
3. CodeViewer Canvas 渲染
4. Viewer 命中测试
5. CodeEditor 输入桥接
6. 自动滚动与焦点/IME 修正
7. 双端编译与工作区主路径回归

## 结论

这次改造的关键不是“换个控件”，而是把 `code-view` 从占位 UI 提升为具备独立布局系统的代码视图组件。

在实现路径上，推荐优先采用：

- 共享布局核心
- 正式 viewport 状态
- Canvas 自绘 Viewer
- Desktop 专用输入宿主 + Android 低层平台文本输入桥，统一接到 Canvas 自管编辑

而不是把透明输入控件继续作为编辑主控。输入框跟随选区移动也依然不建议作为首版基础架构，只能作为后续增强项评估。
