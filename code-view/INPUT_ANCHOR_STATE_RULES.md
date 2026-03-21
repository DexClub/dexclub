# Input Anchor 状态规则

## 目的

本文档用于明确 `code-view-compose` 中“输入锚点”方案的状态边界，避免后续在 Desktop / Android 的 IME 处理上继续边实现边试错。

这里的“输入锚点”特指：

- 一个透明的输入宿主
- 只负责 IME 会话、`composition` 与最终 `commit`
- 不负责正文绘制、selection、caret、命令键或滚动

当前平台形态：

- Android：隐藏的 `0x0 BasicTextField`
- Desktop：附着在 AWT 窗口上的透明 `JComponent` 输入宿主

## 核心原则

### 1. 画布是唯一可见编辑器

- 正文、caret、selection、命中、滚动都由画布负责
- 输入锚点不可见，不承担视觉编辑器职责

### 2. 输入锚点只维护独立的 `imeFieldValue`

- 它不绑定整份文档文本
- 它只承载本次输入片段
- commit 完成后必须立即清空
- Desktop 下即使 `InputMethodEvent` 带有 `committedCharacterCount`，只要 composing 尚未结束，整段 preedit 仍继续保留在 `imeFieldValue`

### 3. 输入锚点永远跟随“当前画布光标”

- 它不能长期绑定到某次输入开始时的旧位置
- 一旦真实光标变化，输入锚点必须重新定位

### 4. 命令键不归输入锚点

- 回车换行
- Backspace / Delete
- 方向键
- Home / End
- 复制 / 粘贴 / 全选

这些都由编辑状态机处理。

输入锚点只负责：

- 平台输入法会话
- `composition != null` 的预输入
- `composition == null` 的最终提交

## 状态定义

### Idle

空闲态：

- `imeFieldValue.text` 为空
- `imeFieldValue.composition == null`
- 当前没有预输入片段

### Composing

预输入态：

- `imeFieldValue.composition != null`
- `imeFieldValue.text` 为当前预输入片段
- 画布显示临时 composing 文本
- 文档正文尚未正式写入

### CommitReady

提交态：

- `imeFieldValue.composition == null`
- `imeFieldValue.text.isNotEmpty()`
- 输入法已经给出最终提交内容

此时编辑器应：

1. 将 `imeFieldValue.text` 写入真实文档
2. 更新真实光标
3. 清空 `imeFieldValue`
4. 输入锚点跟随新的画布光标重新定位

## 中文输入法语义

### 预输入

例如：

- 拼音串
- 带下划线的临时文本

统一视为 `Composing`。

处理规则：

- 只在画布上显示临时 composing
- 不正式写入文档

### 最终提交

例如：

- 空格确认汉字候选
- 回车确认拼音原文

对编辑器来说都是同一种事情：

- 只要 `composition == null && text.isNotEmpty()`，就视为最终 `commit`

编辑器不需要自行区分“空格确认汉字”和“回车确认拼音”的差异。

## 命令键打断规则

### 普通编辑命令

以下操作会改变真实光标或真实文档：

- 回车换行
- Backspace
- Delete
- 方向键跳转
- Home / End
- 鼠标点击定位
- 选区替换

规则：

- 一旦执行这些命令，输入锚点必须重新跟随新的真实光标定位

### composing 期间执行命令

这是最需要统一的部分。

默认规则建议：

1. 如果当前 `composition != null`
2. 且用户执行了会改变真实光标或真实文档结构的命令
3. 先结束当前 composing
4. 再执行编辑命令

“结束当前 composing”优先级建议：

- 首选：让输入法自己提交最终结果
- 若平台无法稳定做到：直接丢弃当前 composing，并清空 `imeFieldValue`

对于当前工程，建议先采用更稳的策略：

- 命令键打断 composing 时，直接清空 `imeFieldValue`
- 不保留旧 composing 状态

原因：

- 状态更简单
- 更容易保证画布光标与输入锚点重新同步
- 避免出现“旧 composing 还挂在旧位置”的问题

## 点击与焦点变化规则

### 点击其他位置

当用户在 composing 期间点击其他位置：

- 先结束当前 composing
- 再更新真实光标
- 输入锚点移动到新的真实光标位置

### 输入锚点失焦

当输入锚点失焦时：

- 不允许残留无主的旧 composing 状态

建议策略：

- 失焦时清空 `imeFieldValue`
- 画布中的临时 composing 同步消失

## 定位规则

### 输入锚点位置

输入锚点位置必须由以下信息共同决定：

- 当前真实光标所在逻辑行
- 当前真实光标所在列
- 当前 viewport 的横向滚动
- 当前 viewport 的纵向滚动
- 当前行的真实 `TextLayoutResult`

### 坐标系

输入锚点应该跟随：

- 画布光标在 **viewport 中的坐标**

而不是：

- 文档全局坐标
- 滚动内容中的普通布局位置

### 禁止事项

- 输入锚点不能成为滚动内容的一部分
- 输入锚点不能吞掉鼠标事件
- 输入锚点获取焦点不能触发滚动容器的 `bringIntoView` 抖动

## 平台策略

### Desktop

当前重点目标：

- 保证英文输入、中文输入、方向键、命令键都稳定
- 保证候选窗跟随输入锚点移动

当前已确认：

- Desktop 输入锚点已切换为 AWT 窗口附属宿主，不再依赖隐藏 `BasicTextField`
- 候选窗当前已能跟随输入锚点移动
- `composing` 期间的可见光标、输入锚点与自动 reveal 已跟随 preedit 内部 caret
- 输入宿主组件本体固定为透明 `1x1`，真实候选窗定位仅依赖 `getTextLocation()`

因此 Desktop 侧后续重点不再是“候选窗能否跟随”，而是“命令键打断 / 点击打断 / 长行滚动”等边界一致性。

### Android

当前重点目标：

- 保证 IME 会话稳定
- 保证 composing / commit 正常
- 保证隐藏输入宿主不与画布编辑状态打架

## 当前建议实现顺序

1. 先确保主输入链路稳定
2. 明确 composing 被命令键 / 点击 / 失焦打断时的统一清理规则
3. 再处理输入锚点跟随画布光标定位
4. 最后单独处理 Desktop 候选窗精确跟随

## 当前未决项

- composing 被打断时，是否需要“尝试提交”而不是直接丢弃
- Desktop 下 composing 被打断时的最终策略是否需要进一步贴近 IDEA / 平台默认行为
