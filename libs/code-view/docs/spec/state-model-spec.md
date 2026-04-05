# 状态模型规范

## 目的

本册用于定义 `code-view` 的核心状态模型，以及各状态的真相源边界。

这份规范要回答的问题是：

- 哪些状态是公开模型
- 哪些状态是组件内部状态
- 谁是真相源，谁只是投影视图
- 当前模型有哪些限制，哪些地方已经计划升级

职责边界：

- 本册只回答“状态是什么、谁为准、谁只是投影”
- 具体交互语义见 [`behavior-spec.md`](behavior-spec.md)
- Selection / Caret 的设计拆分与重构方向见 [`../design/selection-caret.md`](../design/selection-caret.md)

## 范围

本册覆盖：

- document
- selection
- caret
- viewport
- in-page search
- composing
- input anchor
- annotations
- tokens

## 术语

### 真相源

真相源指：

- 某个状态最终以谁为准
- 其他表现形式都只是从它投影或同步出来

### 投影视图

投影视图指：

- 为了外部使用、兼容或可视化而派生出的状态
- 它不应反向拥有完整语义

## 当前总原则

当前状态模型应遵守这几个总原则：

- `CodeDocument` 是文本真相源
- `CodeRuntime` 产出的 tokens / annotations 是 surface 内容真相源
- 画布是唯一可见编辑器
- 输入宿主不是第二个编辑器
- `LineSelection` 和 `Cursor` 是重要公开模型，但并不等于完整编辑态真相源

## document

### 角色

`CodeDocument` 是文本内容的真相源。

它负责：

- 文本快照
- revision
- document identity

它不负责：

- 选区
- caret
- viewport
- 平台输入法会话

### 约束

- 只读路径直接以 `CodeDocument` 快照为准
- 编辑路径最终也必须把提交结果回写到 `CodeDocument`
- `composition != null` 时，预输入不应立即写回 `CodeDocument`
- `CodeDocument.update(newText)` 当前会直接生成下一版 snapshot，并将 revision 加 `1`
- `CodeDocument.create(...)` 当前会自动生成递增的内部 `DocumentId`

## tokens / annotations

### 角色

tokens 和 annotations 是 surface 内容真相源，由 runtime 产出。

当前来源链路是：

- `CodeDocument`
- `CodeAddons`
- `CodeRuntime`
- `CodeSurfaceController`

### 约束

- 渲染层不自己生成高亮结果
- 渲染层必须能接住降级后的 plain text / reduced annotations 结果
- tokens / annotations 的刷新不应反向重置文本、selection 或 viewport

## viewport

### 角色

viewport 是显示范围与滚动恢复的真相源。

它至少需要稳定表达：

- 首可见行
- 横向滚动偏移
- 当前可见区范围

### 约束

- `initialFirstVisibleLine` 与 `initialScrollOffsetX` 是初始输入，不是持续真相源
- 真实滚动发生后，应以内部 viewport 状态和回调结果为准
- reveal、滚动恢复、可见区裁剪都应围绕同一套 viewport 状态工作

## in-page search

### 角色

页内搜索当前分成两层状态：

- 工作区上层搜索会话状态
- `CodeViewer / CodeEditor` 的 `searchHighlight` 输入投影

当前真相源不在 `code-view` 内部，而在工作区上层按 `tabId#kind` 维护。

### 当前上层会话状态

当前工作区页内搜索会话至少需要稳定表达：

- `queryText`
- `matchQuery`
- `source`
- `activeMatchIndex`
- `isVisible`

这些状态当前属于代码页工作区集成层，而不是 `CodeDocument` 或 `CodeEditor` 内部真相源。

### `searchHighlight`

`searchHighlight` 当前只表达：

- 当前活动命中的范围投影

它不表达：

- 全部命中列表
- 搜索框是否可见
- 命中来源
- 当前命中索引以外的会话语义

### 当前约束

- mixed 模式下搜索会话必须按 `tabId#kind` 分离维护
- Java 与 Smali 不能共享同一份页内搜索真相源
- 编辑后命中重算属于上层搜索会话职责，不属于 `code-view` core 内部状态
- `searchHighlight` 只是上层活动命中投影到 `code-view` 的单值输入

## selection

### 当前公开模型

当前公开层存在两个选区相关模型：

- `LineSelection`
- `CodeSelection`

### `LineSelection`

`LineSelection` 的定位是：

- UI 层按行保存与恢复的范围投影视图

它适合：

- 搜索高亮
- 工作区状态保存
- 外部受控范围选区

它不适合：

- 表达完整编辑态中的活动 caret
- 表达选择模式

### `CodeSelection`

当前 `CodeSelection` 仍是：

```kotlin
data class CodeSelection(
    val anchorOffset: Int,
    val caretOffset: Int,
)
```

它适合：

- 字符级选区
- 折叠 caret
- 常规扩选

它当前的限制也很明确：

- 只能直接表达 `anchorOffset + caretOffset`
- 真实范围等于简单包围盒
- 还不能正式表达 `mode`

### 当前状态结论

当前完整编辑态内部真正缺的是：

- 真实活动 caret 与范围解耦
- `Character / Word / Line` 三类模式

因此当前规范结论是：

- `LineSelection` 是范围投影视图
- `CodeSelection` 是更接近内部的偏移级模型
- 但现有 `CodeSelection` 仍不是最终完整模型
- 当前编辑态真正直接驱动可见 selection 的仍是 `TextFieldValue.selection`
- `CodeSelection` 当前主要作为 `TextRange` 与布局快照之间的偏移级桥接

## caret

### 公开模型

当前公开 caret 模型是：

- `Cursor(line, offset)`

### 定位

`Cursor` 的职责只有一个：

- 表达 caret 在哪里

它不负责：

- 表达范围选区
- 表达 anchor 方向
- 表达选择模式

### 当前约束

- `CodeViewer` 中，`cursor` 控制 caret 是否显示
- `CodeEditor(readOnly = false)` 中，caret 必须存在，即使外部没传 `cursor`
- 编辑态中 `Cursor` 更接近外部兼容投影，不是内部完整真相源
- 当前编辑态 `effectiveCursor` 是由 `fieldValue.selection -> CodeSelection -> layoutSnapshot.cursorFromSelection(...)` 推导得到

## composing

### 角色

composing 是平台输入法预输入态。

它的真相源当前在输入桥接层的 `imeFieldValue` / 平台输入状态中，而不是 `CodeDocument`。

### 约束

- `composition != null` 时，画布显示临时 preedit
- 预输入不应正式写入 `CodeDocument`
- `composition == null && text.isNotEmpty()` 时，视为一次 commit
- 当 `fieldValue.composition != null` 且 `fieldValue.text != snapshot.text` 时，编辑态当前以 `fieldValue.text` 作为可见文本真相源

## input anchor

### 角色

input anchor 是平台输入会话宿主的几何锚点。

它负责：

- IME 会话位置
- 候选窗定位
- 平台输入桥的焦点与定位

它不负责：

- 文本内容真相源
- selection 真相源
- caret 真相源

### 当前约束

- input anchor 永远跟随当前画布 caret
- 它不是滚动内容的一部分
- 它不能吞掉主画布的交互

## 当前状态依赖关系

可以用下面这组关系理解当前模型：

- `CodeDocument`
  文本真相源
- `CodeRuntime / CodeSurfaceController`
  tokens / annotations 真相源
- 内部 viewport 状态
  可见区与滚动真相源
- 工作区上层页内搜索会话
  `query / source / activeMatchIndex / visible` 真相源
- 编辑态内部 `TextFieldValue`
  当前编辑中文本、selection 与 composing 的直接真相源
- `CodeSelection` 映射
  `TextFieldValue.selection` 和布局快照之间的偏移级桥接
- `LineSelection`
  范围投影视图
- `Cursor`
  caret 投影视图
- `searchHighlight`
  当前活动搜索命中的范围投影视图

## 当前已知限制

当前最重要的模型限制是：

- 真实 caret 还没有正式和范围解耦

这会影响：

- 双击 / 三击语义
- reveal
- input anchor
- preedit caret
- `preferredColumn`

因此本册和 [`../design/selection-caret.md`](../design/selection-caret.md) 是联动的：

- 当前规范承认现有模型是过渡状态
- 下一轮完整模型重构会升级 `CodeSelection`

## 已计划的模型升级

当前已经明确计划的方向是：

- `CodeSelection` 升级为：
  - `anchorOffset`
  - `caretOffset`
  - `mode`

其中 `mode` 至少包括：

- `Character`
- `Word`
- `Line`

升级后的规范目标是：

- `effectiveRange` 用于真实选中范围
- `caretOffset` 用于真实活动 caret
- `LineSelection` 与 `Cursor` 继续作为对外投影

## 规范结论

当前状态模型的规范结论是：

- 文本以 `CodeDocument` 为准
- 高亮和注解以 runtime surface 为准
- 画布是唯一可见编辑器
- `LineSelection` 与 `Cursor` 是外部重要模型，但不是完整编辑态真相源
- 当前 `CodeSelection` 仍是过渡模型，后续应升级为带 `mode` 的完整模型

## 当前状态

- 状态：`已计划`
- 当前目标：在不破坏外部使用方式的前提下，补齐完整 Selection / Caret 状态模型
- 关联设计：[`../design/selection-caret.md`](../design/selection-caret.md)、[`../design/editor-input-ime.md`](../design/editor-input-ime.md)
