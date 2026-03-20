# Code View Canvas 自绘改造进度

## 关联文档

- 主计划文档：`CANVAS_EDITOR_REFACTOR_PLAN.md`
- 当前文档：`CANVAS_EDITOR_REFACTOR_PROGRESS.md`

使用方式：

- 计划文档负责说明“为什么做、要做什么、按什么方案做”
- 当前文档负责说明“做到哪里了、最近确认了什么、下一步做什么”

## 状态约定

- `未开始`：尚未进入该阶段
- `进行中`：已开始推进，但尚未达到阶段验收标准
- `已完成`：达到该阶段在计划文档中定义的验收标准
- `阻塞`：存在外部依赖或决策阻塞，暂时无法推进

## 当前总状态

- 最近更新时间：`2026-03-20`
- 总体状态：`进行中`
- 当前阶段：`阶段 6：性能与稳定性`
- 当前结论：`Cursor` 与 API 先行调整已完成；共享布局快照与 viewport 已接入 `CodeViewer` / `CodeEditor`；`CodeViewer` 已切到 Canvas 自绘并支持 annotation 点击、长按上下文与 desktop 次键上下文；`CodeEditor` 已切到“共享 CodeViewerCanvas + 透明 BasicTextField”输入桥接，并已接通编辑态 caret 自动 reveal；布局快照、坐标解析与 viewport reveal 已有 `commonTest` 覆盖，`sharedUI` 主路径也已开始实际传入 `cursor`；`textStyle` 已确认不是根因；当前已补“平均度量、按行真实宽度测量、编辑态优先绘制 `fieldValue.text`、CJK 行高采样”等修正，但用户最新手测表明 Desktop 下输入汉字时仍存在 caret 横向错位和编辑行抖动，下一步应直接基于 `BasicTextField.onTextLayout` 的真实 `TextLayoutResult` 对齐编辑态绘制与 reveal

## 阶段总览

| 阶段 | 名称 | 状态 | 说明 |
| --- | --- | --- | --- |
| 0 | 基线整理 | 已完成 | 文档整理、API 草案、源码签名调整与模块编译已完成 |
| 1 | 共享布局快照 | 已完成 | 文本分行、坐标映射、selection/cursor 转换、token 切片已完成 |
| 2 | viewport 状态层 | 已完成 | viewport 状态、滚动范围、visible range、cursor reveal 已接入 `CodeViewer` |
| 3 | CodeViewer Canvas 化 | 已完成 | `CodeViewer` 已切 Canvas，自绘、高亮、caret、annotation 命中基础链路已完成 |
| 4 | Viewer 交互与命中细化 | 进行中 | annotation 点击 / 长按上下文已接入，点击定位、横向滚动下命中细化仍待补 |
| 5 | CodeEditor 输入桥接 | 进行中 | 已切换到共享 `CodeViewerCanvas`，编辑态仍待补更细回归与交互验证 |
| 6 | 性能与稳定性 | 进行中 | 已开始拆分文本布局与装饰层，避免 token/annotation 刷新时重复分行 |
| 7 | 回归与收尾 | 进行中 | `code-view-compose` 测试和 `sharedUI` 双端编译已通过，UI 手动回归仍待补 |

## 已确认决议

### 2026-03-20

- 采用 `cursor: Cursor?` 表达 caret 位置和是否绘制，不再只依赖折叠态 `LineSelection`
- `CodeViewer` 的 caret 是否显示完全由 `cursor` 控制
- `CodeEditor` 需要补 `readOnly` 参数
- `CodeEditor(readOnly = false)` 时 caret 强制显示，即使 `cursor == null` 也不允许隐藏
- `CodeEditor(readOnly = true)` 时才允许通过 `cursor: Cursor?` 控制 caret 显示或隐藏
- 第一阶段输入桥接优先采用“固定覆盖式透明 `BasicTextField` + Canvas 自绘”，不采用“输入层跟随选区移动”作为主方案
- `CodeEditor` 参数面需要向 `CodeViewer` 对齐，至少补齐滚动、viewport、selection、cursor、annotation 交互相关参数

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

- 状态：`进行中`
- 已完成：
  - `CodeEditor` 接入 runtime `surface controller`
  - `CodeEditor` 接入 tokens / annotations collect 与 revision 刷新
  - `CodeEditor` 布局快照与 `CodeViewer` 改为同源
  - `CodeEditor` 不再嵌套 `CodeViewer`，改为直接复用 `CodeViewerCanvas`
  - 编辑态透明 `BasicTextField` 已接入共享 scroll 容器
  - 编辑态 `TextFieldValue.selection` 与 `LineSelection` / `Cursor` 互转已接通
  - 输入桥接层已隐藏原生选区背景，避免与 Canvas 选区叠绘
  - `TextFieldValue` 同步时已保留 composing 区域，降低 IME 抖动风险
  - 编辑态已接入 desktop 次键 annotation 上下文命中
  - 编辑态主点击当前明确保留给文本选择 / caret 定位，不触发 annotation 主点击
  - 编辑态 `TextFieldValue` 变化后已通过内部 token 驱动 caret 自动 reveal
  - 为布局快照、点击坐标解析、viewport reveal 补入 `commonTest`
  - 完成 `:code-view-compose:jvmTest`
  - 再次完成 `:code-view-compose:compileKotlinJvm`
  - 再次完成 `:code-view-compose:compileAndroidMain`
- 未完成：
  - 尚未验证 Android / Desktop 的输入法、拖拽选区与长文本编辑体验
  - caret 自动 reveal 逻辑已接通，但尚未做 Android / Desktop 的专项手动回归
  - 若后续需要“编辑态主点击 annotation”，还需要额外设计与文本选择的优先级
- 下一步：
  - 继续补编辑态交互验证，并开始性能与稳定性检查

### 阶段 6：性能与稳定性

- 状态：`进行中`
- 已完成：
  - `CodeLayoutSnapshotFactory` 已拆出 `withDecorations(base, tokens, annotations)` 路径
  - `CodeViewer` / `CodeEditor` 现在会先缓存按文本分行的基础布局，再按 tokens / annotations 做装饰
  - token / annotation 刷新时不再重复重建整份逻辑行布局
  - 每行 token 排序已前移到布局快照构建阶段，移除了绘制阶段的逐帧 `sortedBy`
  - 为该路径补入 `commonTest`
- 未完成：
  - 尚未对超长行和大文本滚动做专项性能基准
  - 尚未评估编辑态高频输入下 `selectionLayoutSnapshot` 临时重建成本
- 下一步：
  - 继续看长文本 / 长行场景下是否需要再拆更细的缓存层
  - 结合主路径回归判断是否需要补专项性能日志或基准
### 阶段 7：回归与收尾

- 状态：`进行中`
- 已完成：
  - 完成 `:code-view-compose:jvmTest`
  - 完成 `:code-view-compose:compileAndroidMain`
  - 完成 `:sharedUI:compileKotlinJvm`
  - 完成 `:sharedUI:compileAndroidMain`
  - `sharedUI` 的 `CodeViewPane` 已开始向 `CodeViewer` 传入 `cursor`
- 未完成：
  - 尚未做 Android / Desktop 的手动交互回归
  - 尚未验证工作区主路径下的实际滚动恢复、搜索高亮恢复与上下文菜单体验
- 下一步：
  - 进入 UI 级手动回归或继续补自动回归

## 最近变更

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
- 下次会话的首个实现方向已固定为：在 `CodeEditor` 中接入 `BasicTextField.onTextLayout`，基于真实 `TextLayoutResult` 驱动 caret、selection 和横向 reveal
- `:sharedUI:compileKotlinJvm` 通过
- `:sharedUI:compileAndroidMain` 通过
- `CodeEditor` 输入桥接后再次通过 `code-view-compose` JVM / Android 编译

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
- 编辑态拖拽选区时，Canvas 自绘选区与透明 `BasicTextField` 的系统选区 handle 是否一致
- 工作区真实页面中的横向滚动、长行显示和 annotation 上下文菜单体验是否符合预期
- Desktop 下已经观察到光标定位不准确和编辑行抖动，需要在显式传入目标 `textStyle` 后复测是否仍然存在
- Desktop 下已经确认问题不由 `textStyle` 本身引起，需要继续复测“平均度量 + 显式多行输入层”后是否仍存在 caret 偏移和编辑行抖动
- Desktop 下的 caret 定位问题已通过“平均度量 + 显式多行输入层”修正，当前剩余重点是确认编辑行抖动是否还存在
- Desktop 下普通点击后的 caret 已经对齐，但汉字输入与 IME composing 下是否仍存在 caret 偏移和抖动还需继续确认
- 当前已针对“汉字输入下 Canvas 画旧文本”和“拉丁样本行高低估 CJK 字形高度”两条路径补修，待继续手测验证
- 用户最终确认：Desktop 下输入汉字时，caret 仍然对不上，而且编辑行仍会抖动
- 下次不再尝试引入其他模块的 `TextField` 封装，直接基于 `BasicTextField.onTextLayout` 处理真实布局

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
- [ ] 拖拽选区时，确认 Canvas 自绘选区与系统 selection handle 位置一致
- [ ] 编辑态右键可跳转符号，确认仍可弹出上下文菜单
- [ ] 编辑态主点击普通文本区域，确认优先表现为文本选择 / caret 定位，而不是 annotation 主点击

### 输入法与平台

- [ ] Desktop 下使用中文输入法连续输入，确认 composing 不抖动、不丢字、不覆盖异常
- [x] Desktop 下点击不同列位置后，caret 已能准确对齐目标列
- [ ] Desktop 下输入汉字时，caret 是否仍然准确跟随 composing / 已提交文本
- [ ] Desktop 下输入汉字时，编辑行是否仍然发生抖动
- [ ] Android 下使用输入法连续输入，确认 composing 和选区行为正常
- [ ] Desktop 下对长行进行横向滚动后继续输入，确认 caret reveal 和文本显示正常
- [ ] Android 下对长文本上下滚动后继续输入，确认 caret reveal 和滚动状态正常

### 结果记录

- [ ] 若发现问题，记录“文件类型 / 平台 / 触发步骤 / 实际结果 / 预期结果”
- [ ] 若全部通过，可将“待确认项”整体标记为已完成，并进入最终收尾阶段

## 下一步建议

1. 继续验证 `CodeEditor` 编辑态下 selection、caret reveal、IME composing 和 desktop 次键上下文行为
2. 在 `CodeEditor` 中接入 `BasicTextField.onTextLayout`，让 caret、selection、横向 reveal 直接使用真实 `TextLayoutResult`
3. 补 UI 级主路径回归，确认工作区中的滚动恢复、搜索高亮和上下文菜单体验

## 下次会话起点

- 先打开本文档，优先查看“待确认项”和“手测清单”
- 本次会话结束时的未解决重点只有两个：`Desktop 中文输入时 caret 横向错位`、`Desktop 中文输入时编辑行抖动`
- 下次会话不要再从 `textStyle`、平均字符宽度或外部 `TextField` 封装重新开始
- 下次实现优先级：
  1. 继续保持 `BasicTextField`，不引入其他模块封装
  2. 在 `CodeEditor` 中接入 `onTextLayout: (TextLayoutResult) -> Unit`
  3. 用真实 `TextLayoutResult` 驱动编辑态 caret、selection 高亮和横向 reveal
  4. 再根据真实行高与 composing 区域表现决定是否还需要额外的 CJK 行高固定策略
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
