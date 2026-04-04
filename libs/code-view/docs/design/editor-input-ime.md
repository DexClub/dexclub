# Editor 输入与 IME

## 目的

本册用于说明 `CodeEditor` 的输入主链、平台输入桥、IME、input anchor、composing、commit 与焦点规则。

本册的重点不是“如何再选一套输入架构”，而是说明当前已经确认的输入边界、平台差异和后续回归重点。

## 范围

本册覆盖：

- `CodeEditor` 输入主链
- `TextFieldValue` 相关桥接
- Desktop 输入宿主
- Android `InputConnection`
- input anchor
- composing / commit
- 输入法打断规则
- 输入相关 reveal

本册不覆盖：

- 选区手势本身
- 平台工具栏具体 UI 形态
- 业务页菜单逻辑

选区与手势请见：[`selection-caret.md`](selection-caret.md)

## 当前架构

当前 `CodeEditor` 已经切到“Canvas 自管编辑 + 平台输入桥”的主架构。

这里有一条必须保持稳定的总原则：

- 画布是唯一可见编辑器

这意味着：

- 正文、selection、caret、命中、滚动都由画布负责
- 输入宿主只负责 IME 会话、预输入、最终提交与平台编辑命令桥接

## Desktop

Desktop 当前使用专用输入宿主组件：

- 它附着在当前 AWT 窗口上
- 它负责 `InputMethodListener`
- 它负责候选窗定位
- 它负责普通键盘命令和剪贴板路径

当前已经接通：

- 英文输入
- 中文输入
- `inline composing overlay`
- 候选窗跟随输入锚点

当前 Desktop 侧最重要的工作已经不是“能否输入”，而是：

- composing 被命令键打断
- composing 被鼠标点击打断
- 长行横向滚动后继续输入
- 真实工作区中的候选窗与 reveal 体感

## Android

Android 当前使用：

- `AndroidInputHostView + InputConnection`

当前已接通：

- `setComposingText`
- `commitText`
- `deleteSurroundingText`
- `setSelection`
- `performContextMenuAction`

当前已经明确结束的旧路线：

- Android 隐藏 `0x0 BasicTextField` 输入宿主只作为过渡方案
- 这条路线不再继续扩展

当前 Android 侧仍需继续观察的是：

- 不同输入法
- 不同设备
- 删除、候选提交、焦点切换与选区更新的一致性

## 输入宿主的职责边界

输入宿主只负责：

- IME 会话
- `composition`
- 最终 `commit`
- 平台编辑命令接入
- 平台选区变化归一化

输入宿主不负责：

- 真正的可见文本绘制
- 真正的可见 selection
- 真正的 caret 绘制
- 滚动容器主逻辑

换句话说：

- 输入宿主不是第二个编辑器
- 它只是平台输入桥

## 当前状态模型

当前输入语义可以按三个状态理解：

### Idle

- `imeFieldValue.text` 为空
- `imeFieldValue.composition == null`
- 当前没有预输入片段

### Composing

- `imeFieldValue.composition != null`
- `imeFieldValue.text` 是当前预输入片段
- 画布显示临时 composing 内容
- 真实文档尚未正式写入

### CommitReady

- `imeFieldValue.composition == null`
- `imeFieldValue.text.isNotEmpty()`
- 输入法已经给出最终提交内容

此时编辑器应：

1. 将提交文本写入真实文档
2. 更新真实 caret
3. 清空 `imeFieldValue`
4. 让输入锚点跟随新的真实 caret 重新定位

## 输入锚点规则

当前输入锚点的核心原则是：

- 输入锚点永远跟随当前画布 caret

它不能：

- 长期绑定到某次输入开始时的旧位置
- 变成滚动内容的一部分
- 吞掉鼠标事件
- 因为自身焦点变化引起滚动容器抖动

输入锚点的定位需要依赖：

- 当前 caret 的逻辑行列
- 当前 viewport 的横向与纵向滚动
- 稳定行度量
- 当前行可复用的 `TextLayoutResult`

当前额外约束：

- 水平定位可以复用当前行真实 `TextLayoutResult`
- 垂直定位不应直接依赖混排正文的瞬时行框
- Android 键盘顶起期间，reveal 需要同时参考 viewport 收缩和动态 `WindowInsets.ime.bottom`

## 中文输入与 composing 规则

当前所有预输入统一视为 `Composing`：

- 拼音串
- 下划线预编辑文本
- 其他输入法的临时候选片段

处理规则：

- 只在画布上显示临时 composing
- 不正式写入文档

只要满足：

- `composition == null`
- `text.isNotEmpty()`

就视为最终提交。

编辑器不需要区分：

- 空格确认候选
- 回车确认拼音原文

对于编辑主链来说，它们都属于同一种 `commit`。

## 打断规则

### 命令键打断

以下操作会改变真实光标或真实文档：

- 回车换行
- Backspace
- Delete
- 方向键跳转
- Home / End
- 鼠标点击定位
- 选区替换

一旦这些命令发生：

- 输入锚点必须重新跟随新的真实 caret

### composing 期间执行命令

这是输入主链里最需要统一的一段规则。

当前建议仍然是偏稳妥的策略：

1. 若当前存在 `composition`
2. 且用户执行了会改变真实光标或文档结构的命令
3. 先结束当前 composing
4. 再执行真正的编辑命令

当前更稳的处理方式是：

- 命令键打断 composing 时，直接清空 `imeFieldValue`

这样做的理由是：

- 状态更简单
- 锚点同步更稳定
- 更不容易残留旧 composing 挂在旧位置

### 点击与失焦

当用户在 composing 期间点击其他位置：

- 先结束当前 composing
- 再更新真实 caret
- 再重定位输入锚点

当输入宿主失焦时：

- 不允许残留旧 composing
- 失焦时应清空 `imeFieldValue`

## 与 Selection / Caret 的关系

输入主链与 Selection 不是同一套状态，但它们必须保持一致。

当前 Selection / Caret 模型仍有旧假设残留：

- reveal、输入锚点和候选窗在不少地方默认跟随 `selection.end`

这在普通字符级编辑下通常成立，但会限制：

- IDE 式多击选择
- 选区范围和真实 caret 分离
- 更准确的候选窗和 reveal 跟随

因此本册和 [`selection-caret.md`](selection-caret.md) 是联动关系：

- Selection / Caret 完整重构后
- input anchor placement
- Desktop 候选窗位置
- preedit overlay caret
- 编辑态 reveal

都需要切换为显式跟随真实 caret。

## 当前问题

### 1. 主架构已经切稳，但真实页面回归还没做完

当前最大的未完成项不是“输入架构选型”，而是：

- Desktop 工作区真实页面回归
- Android 真机完整回归
- composing 被命令键 / 点击打断时的一致性验证

### 2. Android 兼容性仍需继续观察

虽然 Android 已切到 `AndroidInputHostView + InputConnection`，但仍需要继续验证：

- 不同输入法
- 不同设备
- 删除与候选提交
- 焦点切换
- 异常 `setSelection(...)` 收尾

### 3. 输入锚点与 reveal 还要配合 Selection / Caret 重构

只要真实 caret 还没有和范围正式解耦：

- 输入锚点就还不是最终形态
- reveal 也还不是最终形态

所以当前输入主链虽然已经可用，但还没有完全完成语义收口。

## 当前计划

当前建议的推进顺序：

1. 先完成 Desktop 真实工作区回归
2. 再完成 Android 真机完整回归
3. 再配合 Selection / Caret 重构切换到真实 caret 跟随
4. 最后补 `spec/` 层行为规范和回归矩阵

## 验收标准

### 当前阶段已达成

- Desktop / Android 输入主链都已切到平台桥
- `composition != null` 不再立即写回真实文档
- Android 键盘顶起 reveal 已接通
- Desktop 候选窗跟随已接通

### 当前阶段未完成

以下条件满足后，可认为本册当前阶段达标：

- Desktop 工作区真实页面中文输入稳定
- Desktop composing 被回车、删除、方向键和鼠标点击打断时行为一致
- Desktop 长行横向滚动后继续输入无明显错位或抖动
- Android 在常见输入法与设备上输入、删除、提交、选区更新稳定
- Selection / Caret 重构后输入主链没有引入新回归

## 当前状态

- 状态：`进行中`
- 当前目标：稳住现有平台输入主链，并为 Selection / Caret 模型重构留出接入点
- 对应总进度：[`../status.md`](../status.md)
