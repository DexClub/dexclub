# Code View Canvas 自绘改造进度

## 关联文档

- 主计划文档：`CANVAS_EDITOR_REFACTOR_PLAN.md`
- 当前文档：`CANVAS_EDITOR_REFACTOR_PROGRESS.md`
- 输入锚点规则：`INPUT_ANCHOR_STATE_RULES.md`
- 选区交互规则：`SELECTION_INTERACTION_RULES.md`

使用方式：

- 计划文档负责说明“为什么做、要做什么、按什么方案做”
- 当前文档负责说明“做到哪里了、最近确认了什么、下一步做什么”

## 状态约定

- `未开始`：尚未进入该阶段
- `进行中`：已开始推进，但尚未达到阶段验收标准
- `已完成`：达到该阶段在计划文档中定义的验收标准
- `阻塞`：存在外部依赖或决策阻塞，暂时无法推进

## 当前总状态

- 最近更新时间：`2026-03-22`
- 总体状态：`进行中`
- 当前阶段：`阶段 7：回归与收尾`
- 当前结论：`Cursor` 与 API 先行调整已完成；共享布局快照与 viewport 已接入 `CodeViewer` / `CodeEditor`；`CodeViewer` 已切到 Canvas 自绘并支持 annotation 点击、长按上下文与 desktop 次键上下文；`CodeEditor` 已切到“共享 CodeViewerCanvas + 平台专属 IME host + Canvas 自管编辑”的输入模型，编辑态不再依赖全屏透明 `BasicTextField` 作为主控；Android 侧继续使用隐藏 `0x0 BasicTextField` 作为 IME host，Desktop 侧已切换为附着在 AWT 窗口上的专用输入宿主组件；几何链路已统一到按行缓存的真实 `TextLayoutResult`，caret、selection、annotation 命中和横向 reveal 共用同一套来源；Android 触摸优先 Selection、平台 `SelectionToolbar`、单光标手柄、基础双手柄与对齐修正已接通，并已修正“长按选区 / 手柄拖拽在软键盘收起时误唤起 IME”的回归；`code-view-compose` 与 `sharedUI` 的 JVM / Android 编译已再次通过；当前进入 Android 手动回归与交互细节收尾阶段`

## 阶段总览

| 阶段 | 名称 | 状态 | 说明 |
| --- | --- | --- | --- |
| 0 | 基线整理 | 已完成 | 文档整理、API 草案、源码签名调整与模块编译已完成 |
| 1 | 共享布局快照 | 已完成 | 文本分行、坐标映射、selection/cursor 转换、token 切片已完成 |
| 2 | viewport 状态层 | 已完成 | viewport 状态、滚动范围、visible range、cursor reveal 已接入 `CodeViewer` |
| 3 | CodeViewer Canvas 化 | 已完成 | `CodeViewer` 已切 Canvas，自绘、高亮、caret、annotation 命中基础链路已完成 |
| 4 | Viewer 交互与命中细化 | 进行中 | annotation 点击 / 长按上下文已接入，点击定位、横向滚动下命中细化仍待补 |
| 5 | CodeEditor 输入桥接 | 已完成 | 已切换到隐藏 IME host + Canvas 自管编辑，基础键盘、点击和拖拽链路已接通 |
| 6 | 性能与稳定性 | 进行中 | 已统一到按行 `TextLayoutResult` 几何缓存，仍需继续做大文件与长行场景回归 |
| 7 | 回归与收尾 | 进行中 | `code-view-compose` 测试和 `sharedUI` 双端编译已通过，UI 手动回归仍待补 |

## 已确认决议

### 2026-03-20

- 采用 `cursor: Cursor?` 表达 caret 位置和是否绘制，不再只依赖折叠态 `LineSelection`
- `CodeViewer` 的 caret 是否显示完全由 `cursor` 控制
- `CodeEditor` 需要补 `readOnly` 参数
- `CodeEditor(readOnly = false)` 时 caret 强制显示，即使 `cursor == null` 也不允许隐藏
- `CodeEditor(readOnly = true)` 时才允许通过 `cursor: Cursor?` 控制 caret 显示或隐藏
- 第一阶段输入桥接曾优先采用“固定覆盖式透明 `BasicTextField` + Canvas 自绘”，该决议已在 `2026-03-21` 被替换
- `CodeEditor` 参数面需要向 `CodeViewer` 对齐，至少补齐滚动、viewport、selection、cursor、annotation 交互相关参数

### 2026-03-21

- 不再将“固定覆盖式透明 `BasicTextField`”作为编辑态主方案
- `CodeEditor` 的编辑主控正式切回 Canvas / `TextLayoutResult`
- Android 输入桥接改为隐藏的 `0x0 BasicTextField`，仅承接 IME 连接、composing 和 commit
- Desktop 输入桥接不再依赖隐藏 `BasicTextField`，而是改为专用 AWT 输入宿主组件
- 编辑态在 `composition != null` 时不立即写回 `CodeDocument`，避免中文输入抖动
- `CodeViewerCanvas` 的光标、选区、annotation 命中与横向 reveal 统一复用按行 `TextLayoutResult`
- 平台输入接入层已通过 `PlatformEditorBridge` 收口为 `expect/actual`，公共编辑器不再依赖 `isDesktopPlatform()` 做架构分流
- Desktop `composing` 期间会保留整段 preedit overlay，直到平台输入法真正 commit
- Selection 手势差异继续通过 `PlatformEditorBridge` 的能力开关收口，而不是在公共编辑逻辑中直接判断平台名
- Android 编辑态第一阶段改为“点击放置 caret、普通拖动滚动优先、长按选词、手柄扩选”
- `sharedUI` 的代码页已接通选中文本提取和“全选”，长按菜单不再是空壳

## 当前阶段详情

### 阶段 0：基线整理

- 状态：`已完成`
- 已完成：
  - 梳理 `CodeViewer` / `CodeEditorContent` 当前实现现状
  - 梳理 `CodeViewPane` 对滚动、viewport、selection、cursorTarget 的调用依赖
  - 新建主计划文档并整理结构
  - 在主计划文档中补入 `cursor: Cursor?`、`readOnly`、`CodeEditor` 参数对齐要求
  - 在主计划文档中补入一版接近落地的 `CodeViewer` / `CodeEditor` API 草案
  - 新增 `Cursor` 类型到 `code-view-core`
  - 为 `CodeViewer` 补入 `cursor` 参数
  - 为 `CodeEditor` 补入 `readOnly`、`cursor`、滚动/viewport/交互相关参数
  - 为 `CodeEditor` 增加 `document` 重载
  - 完成 `:code-view-compose:compileKotlinJvm`
  - 完成 `:code-view-compose:compileAndroidMain`
- 未完成：
  - 无
- 下一步：
  - 进入阶段 1，开始实现共享布局快照

### 阶段 1：共享布局快照

- 状态：`已完成`
- 进入条件：
  - `Cursor` / `readOnly` / `CodeEditor` 参数面基本定稿
- 已完成：
  - 新增内部 `CodeLayoutSnapshot`
  - 新增内部 `CodeLayoutSnapshotFactory`
  - 新增内部 `CodeLineLayout`
  - 新增内部 `CodeLineTokenSpan`
  - 实现文本逻辑行切分，兼容 `\n` / `\r\n` / `\r`
  - 实现 offset 与 `(line, column)` 的双向映射
  - 实现 `LineSelection` / `CodeSelection` / `Cursor` 的基础转换与裁剪
  - 实现 `CodeTokenSpan` 按逻辑行切片
  - 再次完成 `:code-view-compose:compileKotlinJvm`
  - 再次完成 `:code-view-compose:compileAndroidMain`
- 未完成：
  - 尚未将布局快照接入 `CodeViewer` / `CodeEditor` 的实际渲染与交互链路
- 下一步：
  - 进入阶段 2，开始补 viewport 状态层

### 阶段 2：viewport 状态层

- 状态：`已完成`
- 进入条件：
  - 布局层可以提供稳定的行与偏移映射
- 已完成：
  - 新增内部 `CodeViewportState`
  - 实现可见行数量计算
  - 实现首尾可见行范围计算
  - 实现横向滚动范围裁剪
  - 实现按 `Cursor` reveal 的基础逻辑
  - 将 viewport 状态接入 `CodeViewer`
  - 将 `onScrollChange` / `onViewportChange` 接入 `CodeViewer`
  - 再次完成 `:code-view-compose:compileKotlinJvm`
  - 再次完成 `:code-view-compose:compileAndroidMain`
- 未完成：
  - 尚未接入 `CodeEditor` 真实状态流
- 下一步：
  - 进入阶段 3，推进 `CodeViewer` Canvas 渲染

### 阶段 3：CodeViewer Canvas 化

- 状态：`已完成`
- 已完成：
  - `CodeViewer` 不再使用 `BasicText` 显示正文
  - 引入基于 `Canvas` 的只读绘制路径
  - 实现可见行范围绘制
  - 实现基于 token 的基础颜色绘制
  - 实现 `selection` 背景绘制
  - 实现 `searchHighlight` 背景绘制
  - 实现 `cursor` 基础绘制
  - 实现 `cursorTarget` 驱动的基础 reveal
  - 为 `CodeAnnotation` 补入 `range`，将 annotation 命中基础坐标正式并入布局快照
  - 为 runtime `surface controller` 接入 annotations 流
  - `CodeViewer` 接入 annotation 点击命中
  - `CodeViewer` 接入长按上下文命中
  - 为 `CodeViewer` 接入文档 revision 刷新 `controller.refresh()`
  - 再次完成 `:code-view-compose:compileKotlinJvm`
  - 再次完成 `:code-view-compose:compileAndroidMain`
- 未完成：
  - 尚未实现点击定位、拖拽选区等交互
  - 桌面端 secondary click 语义仍需进一步细化
  - 尚未与主题化样式系统打通
- 下一步：
  - 进入阶段 4，补 Viewer 命中测试与上下文交互

### 阶段 4：Viewer 交互与命中细化

- 状态：`进行中`
- 已完成：
  - `CodeViewer` 已具备 annotation 点击命中
  - `CodeViewer` 已具备长按上下文命中
  - annotation 命中已统一走屏幕坐标 -> 行列 -> 全局 offset -> annotation range
  - `CodeViewer` 已补 desktop 次键上下文触发
  - annotation 命中已排除视口空白区和行尾右侧空白区误命中
- 未完成：
  - 尚未实现点击空白区定位到行尾
  - 尚未实现 Viewer 自身的点击定位 caret / 选区能力
  - 尚未实现 Viewer 自身的双击选词等更细粒度交互
- 下一步：
  - 继续补命中细节和平台触发差异处理

### 阶段 5：CodeEditor 输入桥接

- 状态：`已完成`
- 已完成：
  - `CodeEditor` 接入 runtime `surface controller`
  - `CodeEditor` 接入 tokens / annotations collect 与 revision 刷新
  - `CodeEditor` 布局快照与 `CodeViewer` 改为同源
  - `CodeEditor` 不再嵌套 `CodeViewer`，改为直接复用 `CodeViewerCanvas`
  - 编辑态 `TextFieldValue.selection` 与 `LineSelection` / `Cursor` 互转已接通
  - 编辑态主点击和拖拽选区已回收至 `CodeEditor` 自己处理
  - Android 侧已接入隐藏 `0x0 BasicTextField` 作为 IME host
  - Desktop 侧已接入专用 AWT 输入宿主，负责 `InputMethodListener`、候选窗定位与普通键盘命令
  - `composition != null` 时不再立即更新 `CodeDocument`
  - 编辑态已接入 desktop 次键 annotation 上下文命中
  - 编辑态主点击当前明确保留给文本选择 / caret 定位，不触发 annotation 主点击
  - `code-view-compose` 再次通过 `:compileKotlinJvm`、`:compileAndroidMain` 与 `:jvmTest`
- 未完成：
  - 若后续需要“编辑态主点击 annotation”，还需要额外设计与文本选择的优先级
  - 双击选词、三击选行、长按选词等更细粒度编辑交互仍可继续补强
- 下一步：
  - 进入阶段 6 / 7，继续做性能与回归验证

### 阶段 6：性能与稳定性

- 状态：`进行中`
- 已完成：
  - `CodeLayoutSnapshotFactory` 已拆出 `withDecorations(base, tokens, annotations)` 路径
  - `CodeViewer` / `CodeEditor` 现在会先缓存按文本分行的基础布局，再按 tokens / annotations 做装饰
  - token / annotation 刷新时不再重复重建整份逻辑行布局
  - 每行 token 排序已前移到布局快照构建阶段，移除了绘制阶段的逐帧 `sortedBy`
  - 新增按行缓存的 `CodeLineTextLayoutCache`
  - `CodeViewerCanvas` 的正文绘制、caret、selection、annotation 命中和横向 reveal 已统一复用真实 `TextLayoutResult`
  - 为该路径补入 `commonTest`
- 未完成：
  - 尚未对超长行和大文本滚动做专项性能基准
  - 尚未评估编辑态高频输入下 `selectionLayoutSnapshot` 临时重建成本
  - 尚未验证 Desktop / Android 长文本输入下的真实体感
- 下一步：
  - 继续看长文本 / 长行场景下是否需要再拆更细的缓存层
  - 结合主路径回归判断是否需要补专项性能日志或基准
### 阶段 7：回归与收尾

- 状态：`进行中`
- 已完成：
  - 完成 `:code-view-compose:jvmTest`
  - 完成 `:code-view-compose:compileAndroidMain`
  - 完成 `:code-view-compose:compileKotlinJvm`
  - 完成 `:sharedUI:compileKotlinJvm`
  - 完成 `:sharedUI:compileAndroidMain`
  - `sharedUI` 的 `CodeViewPane` 已开始向 `CodeViewer` 传入 `cursor`
- 未完成：
  - 尚未做 Android / Desktop 的完整手动交互回归
  - 尚未验证工作区主路径下的实际滚动恢复、搜索高亮恢复与上下文菜单体验
  - 尚未确认 Desktop 中文输入、长行输入和拖拽选区在真实页面中的体感
- 下一步：
  - 进入 UI 级手动回归或继续补自动回归

## 最近变更

### 2026-03-22

- Android 编辑态触摸交互已拆分为“单击请求 IME、长按选区不主动唤起软键盘”
- Android 长按选词后若软键盘原本收起，当前会继续保持收起
- Android 双手柄拖拽开始时不再额外触发软键盘弹出
- Android 双手柄拖拽已改为稳定手势会话，不再因手柄位置实时刷新而频繁中断
- Android 平台 `SelectionToolbar` 在手柄拖拽期间会临时收起，结束后再恢复，减少拖拽卡顿与生硬感
- Android 双手柄在交汇后已允许继续反向拖拽，选区不会在交汇点直接丢失
- Android 点击已有选区内部时，当前选区会保留；若平台 `SelectionToolbar` 已被收起，当前会重新触发显示
- Android 编辑态已补单个光标手柄，拖拽时 caret 会跟随移动

### 2026-03-21

- `CodeEditor` 移除“共享 scroll 容器上的全屏透明 `BasicTextField`”主输入模式
- Android 输入路径切换为隐藏 `0x0 BasicTextField` IME host
- Desktop 输入路径已切换为附着在 AWT 窗口上的专用输入宿主组件，不再通过隐藏 `BasicTextField` 承担 IME 会话
- 平台输入接入层抽为 `expect/actual` `PlatformEditorBridge`
- `CodeEditor` 的点击定位、拖拽选区、基础键盘编辑和剪贴板操作已收回自身处理
- `composition != null` 时不再立即 `document.update(...)`
- 新增 `CodeLineTextLayoutCache`
- `CodeViewerCanvas` 的正文绘制、caret、selection、annotation 命中和横向 reveal 已统一走真实 `TextLayoutResult`
- 新增 `code-view-compose` 平台 `Clipboard` / 修饰键 actual
- `CodeEditor.kt` / `CodeViewer.kt` 已做结构瘦身，输入、状态映射、渲染与 annotation 交互拆入独立文件
- `code-view-compose` 内部文件已进一步整理为 `internal/editor` 与 `internal/viewer` 两组子包，根包仅保留对外入口与少量平台桥接
- `CodeViewer` 恢复呼吸光标动画，caret 端点恢复圆角
- `CodeViewer` 多行选区改为连续块状绘制，去掉行间缝隙，并让非结束行延伸到整行剩余宽度
- 用户最新 Desktop 手测反馈：光标呼吸与圆角效果可验收；多行选区的行间隙与行尾延伸效果可验收
- 已明确“输入锚点”方案：Android 使用隐藏 `BasicTextField`，Desktop 使用专用 AWT 宿主；两者都只承载 IME 会话、composing 与 commit，不承担可见编辑层职责
- 已确认桌面端“候选窗跟随光标”是后续独立问题，需要在不破坏当前可用输入链路的前提下单独推进
- 已补充一条关键规则：输入锚点永远跟随“当前画布光标”，一旦回车、删除、点击跳转等命令改变真实光标位置，输入锚点必须重新定位，并在必要时重置当前 composing 状态
- 已新增独立文档 `INPUT_ANCHOR_STATE_RULES.md`，收口输入锚点的状态规则、命令键打断规则与焦点变化规则
- Desktop 专用输入宿主已跑通：自动焦点、英文输入、中文输入、候选窗定位与普通键盘命令已接通
- 画布已接入 `inline composing overlay`，Desktop 中文预输入的拼音与下划线已可见
- 行中间输入时的 preedit 改为内联渲染，后缀文本会右移，不再被遮挡
- `selection + composing` 已调整为更接近 IDEA：首次进入 composing 时先真实删除选区内容，再从选区起点显示 preedit
- `composing` 期间的可见光标、输入锚点与自动 reveal 已接入 preedit 内部 caret，不再固定在首字母，也不会等 commit 后才滚回可视区
- Desktop 候选词窗口当前已能跟随输入锚点移动，整体效果与预期基本一致
- Desktop IME 事件在 `committedCharacterCount > 0` 但 `composition` 尚未结束时，不再把前缀文本提前写入正文，而是继续保留整段 preedit overlay 直到最终 commit
- `CodeEditor` 重构后再次通过 `:code-view-compose:compileKotlinJvm`
- `CodeEditor` 重构后再次通过 `:code-view-compose:compileAndroidMain`
- `CodeEditor` 重构后再次通过 `:code-view-compose:jvmTest`

### 2026-03-20

- 新建主计划文档 `CANVAS_EDITOR_REFACTOR_PLAN.md`
- 为主计划文档补充文档关系、API 预调整、`Cursor` 方案、`CodeEditor` 参数草案
- 新建当前进度文档，用于后续按阶段同步推进状态
- 新增 `Cursor` 类型并完成 `CodeViewer` / `CodeEditor` 的 API 先行调整
- `code-view-compose` JVM / Android 编译通过
- 新增共享布局快照与坐标映射内部实现
- 共享布局核心加入后再次通过 `code-view-compose` JVM / Android 编译
- 新增 viewport 内部状态实现
- viewport 基础模型加入后再次通过 `code-view-compose` JVM / Android 编译
- `CodeViewer` 切换为 Canvas 自绘
- `CodeViewer` 接入布局快照、viewport、基础高亮与 caret 绘制
- `CodeAnnotation` 增加 `range`，runtime surface annotations 流正式接线
- `CodeViewer` 接入 annotation 点击命中与长按上下文命中
- `CodeViewer` Canvas 化后再次通过 `code-view-compose` JVM / Android 编译
- `CodeEditor` 接入 runtime surface controller、tokens / annotations collect 与 revision 刷新
- `CodeEditor` 改为直接复用 `CodeViewerCanvas`，透明输入层与 Viewer 共用同一 scroll 容器
- `CodeEditor` 输入桥接补入 composing 保留与原生选区背景隐藏
- `CodeViewer` 补入 desktop 次键上下文触发与空白区误命中裁剪
- `CodeEditor` 透明输入层补入 desktop 次键 annotation 上下文命中，主点击保留给文本编辑
- `CodeEditor` 补入基于内部 token 的 caret 自动 reveal
- `code-view-compose` 新增 `commonTest`，覆盖布局快照、坐标解析与 viewport reveal
- `:code-view-compose:jvmTest` 通过
- `CodeLayoutSnapshotFactory` 增加基于已有文本布局的 `withDecorations(...)` 路径
- `CodeViewer` / `CodeEditor` 改为复用文本布局基础快照，减少 decoration 刷新时的重复分行
- 每行 token 排序已前移到布局快照构建阶段，减少 Canvas 绘制热路径内的排序开销
- `sharedUI` 的 `CodeViewPane` 开始向 `CodeViewer` 传入 `cursor`
- `CodeViewer` / `CodeEditor` 新增 `textStyle` 参数，并统一接入 Canvas 测量、正文绘制和透明输入层
- `CodeViewerCanvas` 的字符宽度与行高测量改为长样本平均值，降低 Desktop 侧整数舍入误差累积
- `CodeViewerCanvas` 的内容宽高预留改为“额外一列 / 一行”安全边距，降低透明输入层意外换行与抖动风险
- `CodeEditor` 透明 `BasicTextField` 显式声明 `singleLine = false`、`maxLines = Int.MAX_VALUE` 与 `minLines`
- `CodeViewerCanvas` 新增按行真实宽度测量缓存，文本分段绘制、选区、caret 和横向 reveal 不再只依赖 `column * charWidthPx`
- `CodeEditor` 编辑态可见文本改为优先跟随 `fieldValue.text`，不再等待 `CodeDocument` snapshot 追平后才刷新 Canvas
- `CodeViewerCanvas` 的行高测量改为同时采样拉丁字符和 CJK 字符，并取两者较大值，降低汉字输入时的行高抖动
- 明确不引入外部封装 `TextField`，继续直接使用 `BasicTextField`
- 用户最新 Desktop 手测确认：普通点击场景并未彻底结束问题，输入汉字时仍出现 caret 横向错位与编辑行抖动
- 下次会话的首个实现方向已固定为：将编辑主控从透明 `BasicTextField` 收回 `CodeEditor`，并让几何链路统一回到真实 `TextLayoutResult`
- `:sharedUI:compileKotlinJvm` 通过
- `:sharedUI:compileAndroidMain` 通过
- `CodeEditor` 输入桥接后再次通过 `code-view-compose` JVM / Android 编译
- 新增独立文档 `SELECTION_INTERACTION_RULES.md`，收口桌面 / Android 的 Selection 手势模型、平台桥接边界和后续工具栏演进方向
- Android 侧新增 `useTouchSelectionGestures` 能力开关，桌面端保留原拖拽选区语义
- Android 编辑态新增长按选词与基础双手柄，主画布拖动不再直接扩展选区
- 新增 `PlatformSelectionToolbarBridge`，Android 侧优先通过平台工具栏承接复制 / 全选，长按回调菜单降级为兜底路径
- `CodeEditorContent` 的交互与触摸 overlay 装配已拆到独立文件，当前代码组织比初版更适合继续演进
- Android Selection 手柄当前已切到更接近官方构造的实现，并修正了可见层水平错位
- `sharedUI` 的 `CodeViewPane` 已接通 `onSelectionChange` / `onCursorChange`，并补上选中文本提取与“全选”
- 本轮改动再次通过 `:code-view-compose:compileKotlinJvm`
- 本轮改动再次通过 `:code-view-compose:compileAndroidMain`
- 本轮改动再次通过 `:sharedUI:compileKotlinJvm`
- 本轮改动再次通过 `:sharedUI:compileAndroidMain`

## 当前阻塞

- 无硬阻塞
- 当前属于“共享 Canvas 底座已接通，待继续细化命中行为与编辑态回归”的阶段

## 待确认项

- 工作区代码页在只读场景下是否会按 `editorState.cursorLine / cursorOffset` 正确显示 caret
- 导航跳转到定义后，`cursorTarget` 驱动的自动滚动是否总能把目标位置带入可视区
- 搜索命中恢复后，`searchHighlight` 与滚动恢复是否符合预期，没有错位或丢失
- Desktop 下对可跳转符号执行右键时，上下文菜单位置、命中对象和行为是否正确
- 编辑态下连续输入、删除、方向键移动后，caret 自动 reveal 的体感是否正常，是否出现“跑出视口”或抖动
- Android / Desktop 的 IME composing 是否稳定，是否出现选区错乱、输入抖动或文本覆盖异常
- Android 下长按后菜单位置、按词选中范围和双手柄拖拽是否符合预期
- 编辑态拖拽选区时，Canvas 自绘选区范围是否与实际文本替换结果一致
- 工作区真实页面中的横向滚动、长行显示和 annotation 上下文菜单体验是否符合预期
- Desktop 下中文输入、长行输入和横向滚动后的 caret / selection / reveal 是否仍然稳定
- Desktop 下 composing 期间直接回车、删除、方向键或鼠标点击其他位置时，输入锚点是否会正确重定位并清理旧状态
- Android 下隐藏 IME host 的退格、候选提交和焦点切换是否稳定
- Desktop 下调试开关关闭后的最终体感是否仍与当前一致

## 手测清单

### 通用准备

- [ ] 使用当前主工程代码，进入实际工作区页面，而不是单独的组件预览页
- [ ] 至少准备一份普通长度文件和一份包含长行的文件
- [ ] 至少准备一个带可跳转 annotation 的位置，便于验证点击和上下文菜单

### 只读路径

- [ ] 打开代码页后，确认 caret 会显示在上次保存的 `cursorLine / cursorOffset` 位置
- [ ] 关闭并重新打开同一标签页，确认纵向滚动和横向滚动都能恢复
- [ ] 执行一次导航跳转到定义，确认目标位置会自动滚动到可视区内
- [ ] 触发一次搜索命中恢复，确认 `searchHighlight` 显示正确且没有错位
- [ ] 在普通长度文件中右键可跳转符号，确认上下文菜单位置正确、命中对象正确
- [ ] 在长行文件中横向滚动后再右键可跳转符号，确认命中和菜单位置仍然正确
- [ ] 点击行尾右侧空白区，确认不会误触发 annotation
- [ ] 点击视口下方空白区域，确认不会误触发 annotation

### 编辑路径

- [ ] 在编辑态连续输入文本，确认字符插入正确，caret 不会跑出当前可视区
- [ ] 使用退格、Delete、方向键移动 caret，确认自动 reveal 行为正常，没有明显抖动
- [ ] 选中一段文本后直接输入，确认选区替换正确
- [ ] 粘贴多行文本，确认文本内容、选区和 caret 最终位置正确
- [ ] 拖拽选区时，确认 Canvas 自绘选区范围与最终替换结果一致
- [ ] 编辑态右键可跳转符号，确认仍可弹出上下文菜单
- [ ] 编辑态主点击普通文本区域，确认优先表现为文本选择 / caret 定位，而不是 annotation 主点击

### 输入法与平台

- [ ] Desktop 下使用中文输入法连续输入，确认 composing 不抖动、不丢字、不覆盖异常
- [x] Desktop 下点击不同列位置后，caret 已能准确对齐目标列
- [x] Desktop 下光标呼吸节奏、透明度和圆角效果符合预期
- [x] Desktop 下多行选区已经没有行间缝隙，且到行尾的延伸符合预期
- [x] Desktop 下输入汉字时，caret 与 `inline composing overlay` 基本符合预期
- [x] Desktop 下输入汉字时，编辑行不再出现明显抖动
- [x] Desktop 下输入法候选词列表能跟随画布光标位置显示
- [x] Desktop 下 composing 期间当前输入行会及时滚回可视区
- [ ] Desktop 下 composing 过程中直接回车、删除、方向键跳转或鼠标点击其他位置后，输入状态是否仍然正确
- [ ] Android 下使用输入法连续输入，确认 composing 和选区行为正常
- [ ] Android 下普通滑动代码页时，确认优先表现为纵向 / 横向滚动，而不是直接进入选区
- [ ] Android 下长按单词后，确认会出现菜单入口且首个选区范围合理
- [ ] Android 下单击放置 caret 后，确认会出现单个光标手柄，拖拽后 caret 能稳定跟随
- [ ] Android 下拖拽左右手柄时，确认选区范围更新稳定，没有明显跳动
- [ ] Desktop 下对长行进行横向滚动后继续输入，确认 caret reveal 和文本显示正常
- [ ] Android 下对长文本上下滚动后继续输入，确认 caret reveal 和滚动状态正常

### 结果记录

- [ ] 若发现问题，记录“文件类型 / 平台 / 触发步骤 / 实际结果 / 预期结果”
- [ ] 若全部通过，可将“待确认项”整体标记为已完成，并进入最终收尾阶段

## 下一步建议

1. 先做 Android 真机 / 模拟器回归，重点验证长按选词、平台工具栏、双手柄和滚动优先策略
2. 再验证 `CodeEditor` 编辑态下 selection、caret reveal、IME composing 和 desktop 次键上下文行为
3. 根据手测结果决定是否继续补自动滚动、手柄越界和工具栏动作扩展

## 下次会话起点

- 先打开本文档，优先查看“待确认项”和“手测清单”
- 当前代码层面的主架构已经切到“隐藏 IME host + Canvas 自管编辑 + 真实 TextLayoutResult 几何”
- 输入锚点、`inline composing overlay`、`selection + composing`、虚拟 caret 与 reveal 已经全部落地，不需要再回到主架构讨论
- Android `Selection` 当前主链已经具备：
  - 点击放置 caret
  - 普通拖动滚动优先
  - 长按选词
  - 平台 `SelectionToolbar`
  - 基础双手柄
- 下次会话不要再从 `textStyle`、平均字符宽度或“恢复透明 `BasicTextField` 主控”重新开始
- 下次实现优先级：
  1. 先从 Android 工作区真实页面开始回归：
     - 长按选词
     - 平台工具栏是否正常出现
     - 左右手柄是否继续错位或跳动
     - 普通拖动是否稳定保持滚动优先
  2. 再看 Android 长文本、长行和输入法下的 reveal / 滚动 / 选区一致性
  3. 若 Android 主问题基本通过，再回头处理 Desktop 下 composing 被回车 / 删除 / 方向键 / 鼠标点击打断的一致性
- 若本次已经完成手测，下一次会话请直接同步“哪些条目通过、哪些条目失败、失败现象是什么”
- 若还未手测，下一次会话优先从工作区真实页面开始做 UI 级回归，不必再回到底层 API 或 Canvas 架构讨论
- 如果手测发现问题，下一次会话建议直接附上：
  - 平台
  - 文件类型或样例
  - 触发步骤
  - 实际结果
  - 预期结果
- 如果手测基本通过，下一次会话就进入“阶段 7：回归与收尾”，重点做：
  - 清理遗留待确认项
  - 评估是否还需要补少量自动回归
  - 判断是否可以结束本轮重构

## 更新规则

- 每次阶段状态变化时，优先更新当前文档
- 每次出现新的架构决议时，同时更新当前文档和计划文档
- 每次开始代码实现前，先将“当前阶段”切到对应阶段，避免文档进度滞后
