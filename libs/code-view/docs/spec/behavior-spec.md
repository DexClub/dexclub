# 行为规范

## 目的

本册用于定义 `code-view` 当前的组件级交互行为。

这份规范的目标是：

- 让实现、测试和手测对齐到同一套语义
- 把 Desktop 和 Android 的行为差异写清楚
- 避免后续再靠口头约定理解交互

职责边界：

- 本册只定义“应当表现成什么行为”
- 具体状态真相源见 [`state-model-spec.md`](state-model-spec.md)
- 更深入的设计背景与重构方向见 `design/` 层对应主题文档

## 范围

本册覆盖：

- 单击
- 双击
- 三击
- 长按
- 拖拽
- 方向键
- 复制 / 粘贴 / 全选
- reveal
- IME 打断
- 上下文菜单

## 总原则

当前行为规范遵守这几个总原则：

- Desktop 与 Android 不强行统一主手势
- 画布是唯一可见编辑器
- 编辑态主点击优先服务文本编辑，而不是 annotation 主点击
- annotation 上下文与 selection 上下文分开处理
- revealing、输入锚点与真实 caret 应保持一致

## Desktop 行为

### 单击

Desktop 编辑态单击应：

- 放置 caret
- 折叠现有范围选区
- 将主焦点回到编辑主链

如果在只读态：

- 可以按只读规则进行 caret 定位或注解命中

### 双击

Desktop 双击当前规范为：

- 按词选中

当前目标语义：

- 整词选中
- caret 在词尾

### 三击

Desktop 三击当前规范为：

- 按行选中

当前目标语义：

- 整行选中
- caret 回到三击位置

### 拖拽

Desktop 主拖拽当前规范为：

- 直接扩展选区

这条语义不应被移动端滚动优先规则污染。

### 次键

Desktop 次键当前规范为：

- 打开上下文菜单
- annotation 存在时优先把命中对象带给菜单路径

## Android 行为

### 单击

Android 单击当前规范为：

- 只放置 caret
- 不直接进入范围选区
- 主动请求输入焦点与软键盘

如果当前已有非折叠选区并点击选区内：

- 软键盘未显示时，保留选区并重新请求软键盘
- 软键盘已显示时，折叠到点击位置对应的 caret

### 普通拖动

Android 普通拖动当前规范为：

- 优先交给横向 / 纵向滚动

不允许：

- 一滑就直接开始范围选区

### 长按

Android 长按当前规范为：

- 进入选区态
- 首次按词选中
- 若继续按住拖动，可直接扩展首个词级选区
- 松手后再出现菜单入口和手柄

Android 当前不引入：

- 双击选词
- 三击选行

### 手柄

Android 手柄当前规范为：

- 折叠态显示单个光标手柄
- 范围选区显示双手柄
- 手柄拖拽用于精细调整选区或 caret
- 手柄拖拽开始时不主动唤起软键盘
- 手柄拖拽期间平台工具栏临时收起，结束后恢复

## 选择与编辑行为

### 输入替换

当存在范围选区时，直接输入应：

- 用输入内容替换 `effectiveRange`
- 操作完成后折叠为新的 caret
- 若当前仍处于 composing，中间态文本先留在编辑态 `fieldValue.text` 中，不立即写回 `CodeDocument`

### 粘贴

粘贴当前规范为：

- 若存在范围选区，先替换选区
- 若无范围选区，在当前 caret 插入
- 操作完成后 caret 位于插入内容末尾

### 删除

删除当前规范为：

- 若存在范围选区，优先删除选区
- 若无范围选区，按 Backspace / Delete 的方向删除

## reveal 行为

当前 reveal 规范为：

- 程序化 `cursorTarget` 必须把目标位置滚入可视区
- 编辑态输入后 caret 应保持在可视区
- Android 键盘顶起期间 reveal 需要参考动态 IME inset
- `scrollPastEnd` 影响 reveal 的底部可见边界

当前更进一步的目标语义是：

- reveal 跟随真实 caret，而不是简单跟随范围末端

## IME 与 composing 行为

### composing

当前规范为：

- `composition != null` 时显示 preedit
- composing 不立即写回 `CodeDocument`
- commit 后再正式写入文本
- 编辑态可见文本当前优先跟随 `fieldValue.text`，不等待 `CodeDocument` snapshot 追平

### 命令键打断

当前规范为：

- 若 composing 期间执行会改变文本或 caret 的命令
- 先结束当前 composing
- 再执行真正的编辑命令

当前更稳妥的工程策略是：

- 直接清空旧 `imeFieldValue`

### 点击打断

当前规范为：

- composing 期间点击其他位置
- 先结束当前 composing
- 再更新真实 caret
- 再重定位输入锚点

### 提交时机

当前 `CodeEditor` 的正式文本提交时机是：

- `newValue.composition == null`
- 且 `snapshot.text != newValue.text`

只有同时满足这两个条件时，才会：

- `document.update(newValue.text)`
- 触发 `onTextChange`

## 上下文菜单行为

### annotation 上下文

annotation 上下文当前规范为：

- `Viewer` 中可以直接命中注解
- `Editor` 中主点击不抢注解动作
- `Editor` 中 annotation 优先走次键上下文

### selection 上下文

selection 上下文当前规范为：

- Desktop 走右键菜单
- Android 优先平台 `SelectionToolbar`
- `CodeContextMenu` 当前只作为兜底

## 当前不属于必须行为的项

以下能力当前不属于本轮必须行为：

- 多光标
- 矩形选区
- 语法级扩选
- 全量 IDE 风格多击后拖拽扩选细节

## 规范结论

当前应以以下平台行为作为默认标准：

- Desktop：
  - 单击放 caret
  - 双击选词
  - 三击选行
  - 拖拽扩选
  - 次键菜单
- Android：
  - 单击放 caret
  - 普通拖动优先滚动
  - 长按选词
  - 手柄微调
  - 平台工具栏优先

任何实现、测试或回归结果，只要和这套默认标准冲突，都应视为需要重新确认。

## 当前状态

- 状态：`进行中`
- 当前目标：把当前已确认交互语义沉淀成稳定规范，并为 Selection / Caret 完整重构留出升级位
- 关联设计：[`../design/selection-caret.md`](../design/selection-caret.md)、[`../design/context-actions.md`](../design/context-actions.md)、[`../design/editor-input-ime.md`](../design/editor-input-ime.md)
