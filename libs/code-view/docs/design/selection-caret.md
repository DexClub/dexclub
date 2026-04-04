# Selection / Caret / 手势

## 目的

本册用于说明 `code-view` 的选区、caret 与手势系统。

这里要回答的问题是：

- 平台之间的交互语义如何分层
- 当前 selection / caret 的真实模型是什么
- 当前模型有哪些限制
- 下一轮完整重构要解决什么问题

职责边界：

- 本册聚焦 Selection / Caret 的设计边界、平台分层和重构方向
- 当前可执行交互规则以 [`../spec/behavior-spec.md`](../spec/behavior-spec.md) 为准
- 当前状态真相源定义以 [`../spec/state-model-spec.md`](../spec/state-model-spec.md) 为准

## 范围

本册覆盖：

- `selection` 模型
- `caret` 模型
- Desktop 鼠标语义
- Android touch-first 语义
- 双击选词
- 三击选行
- 长按选词
- 手柄拖拽
- 选择模式与后续重构方向

本册不覆盖：

- 复杂命令系统
- 多光标
- 矩形选区
- 语法级扩选到语句或代码块

## 当前平台语义

## Desktop

Desktop 当前走 `mouse-first`：

- 主点击放置 caret
- 按下后拖拽直接扩展选区
- 双击按词选中
- 三击按行选中
- 次键触发上下文菜单

这一套语义偏向传统桌面编辑器和 IDE。

当前已经接通的行为：

- 双击选词第一版
- 三击选行第一版
- 拖拽扩选

当前还未完全对齐的点：

- 三击选行后，caret 还没有完全达到“整行选中，但 caret 回到点击位置”的最终语义
- 多击后拖拽按词 / 按行扩选仍未进入本轮必须项

## Android

Android 当前走 `touch-first`：

- 单击只放置 caret
- 普通拖动优先交给滚动
- 长按进入选区
- 长按后先按词选中
- 长按手势结束后再显示手柄或平台工具栏
- 手柄拖拽负责精细调整选区

这一套语义明确不和 Desktop 统一。

当前已经明确的边界：

- Android 不引入双击选词
- Android 不引入三击选行
- Android 的主画布拖动不再直接承担范围选区

这不是能力缺失，而是平台选择。

## 当前模型

当前公开层与编辑主链主要有三种表示：

- `LineSelection`
  适合外层按范围保存与恢复
- `Cursor`
  适合表达可见 caret 位置
- `CodeSelection`
  当前主要表达 `anchorOffset + caretOffset`

当前真正的问题不在“有没有选区”，而在“当前模型默认 caret 就是选区末端”。

这个假设对这些场景是够用的：

- 折叠 caret
- 普通字符级拖拽选区
- Shift + 方向键扩选

但对这些场景已经不够：

- 双击选词
- 三击选行
- 选区范围和真实活动 caret 分离
- reveal、输入锚点和候选窗跟随真实 caret

## 当前问题

### 1. 范围和活动 caret 还没有正式解耦

当前编辑态很多地方仍默认：

- `caret == selection.end`

这会直接限制：

- 三击选行后 caret 的落点
- 后续按词 / 按行扩选
- 输入锚点与 reveal 的准确跟随

### 2. 当前缺少正式的 `selection mode`

虽然交互上已经出现：

- `Character`
- `Word`
- `Line`

但当前内部还没有统一模式模型。

结果是：

- 一部分规则留在 pointer input
- 一部分规则留在文本替换逻辑
- 一部分规则仍建立在旧的 `selection.end` 假设上

### 3. 这个问题已经影响多个子系统

Selection / Caret 不只是手势层问题，还直接影响：

- caret 绘制
- caret reveal
- `preferredColumn`
- 输入锚点定位
- Desktop 候选窗定位
- Android IME 选区归一化
- `onSelectionChange` / `onCursorChange`

所以这里不适合继续打局部补丁。

## 当前已确认规则

当前已经确认的产品语义是：

- Desktop 要保留双击选词和三击选行
- Android 只保留长按选词，不做双击 / 三击

当前已经确认的行为目标是：

- Desktop 双击选词后，整词选中，caret 在词尾
- Desktop 三击选行后，整行选中，caret 回到点击位置
- Android 长按后仍按词选中，并保持滚动优先与手柄微调路线

## 重构方向

下一轮重构目标不是只修三击选行，而是把 Selection / Caret 模型整体理顺。

推荐方向：

- 升级 `CodeSelection`
- 正式引入 `CodeSelectionMode`
- 用新模型统一表达：
  - `anchorOffset`
  - `caretOffset`
  - `mode`
- 用 `effectiveRange` 作为真实选中范围

推荐最小模型：

```kotlin
enum class CodeSelectionMode {
    Character,
    Word,
    Line,
}

data class CodeSelection(
    val anchorOffset: Int,
    val caretOffset: Int,
    val mode: CodeSelectionMode = CodeSelectionMode.Character,
)
```

这里最关键的变化不是多了一个枚举，而是正式承认：

- 真实 caret 是一回事
- 选中范围是另一回事

## 目标语义

### Character

字符级模式用于：

- 普通点击
- 普通拖拽
- Shift + 方向键
- 手柄拖拽

当前行为可以基本保持现状。

### Word

按词模式用于：

- Desktop 双击选词
- Android 长按首个词级选中
- 后续双击后拖拽按词扩选

这时 `effectiveRange` 由词边界扩展得到，而 reveal、输入锚点和可见 caret 继续跟随真实 `caretOffset`。

### Line

按行模式用于：

- Desktop 三击选行
- 后续“选中当前行”命令
- 后续三击后按行扩选

这时整行范围由 `mode` 展开，但 caret 仍可停在点击位置。

这正是旧模型做不到、但新模型天然能够表达的场景。

## 与其他状态的关系

### `LineSelection`

`LineSelection` 继续保留，但只作为范围投影视图：

- 适合工作区恢复
- 适合搜索高亮
- 适合只读态范围回写

它不应继续承担完整编辑态真相源职责。

### `Cursor`

`Cursor` 继续保留，但只投影真实 caret：

- `Cursor` 只回答“caret 在哪里”
- 不回答“当前范围怎么扩展”

### 输入锚点与 reveal

Selection / Caret 重构后，以下能力都应改为跟随真实 caret，而不是选区末端：

- input anchor placement
- Desktop 候选窗定位
- preedit overlay caret
- `preferredColumn`
- caret reveal

## 当前计划

当前建议的推进顺序是：

1. 先固化 Selection / Caret 完整模型
2. 再升级 `core + compose` 中的核心状态与映射
3. 先对齐 Desktop 双击 / 三击语义
4. 再回 Android 验证长按、手柄和 IME 没有回归

当前暂不纳入本轮必须项：

- 多光标
- 矩形选区
- 所有 IDE 级多击拖拽细节一次做全
- 语法级结构扩选

## 验收标准

以下条件满足后，可认为本册对应的下一轮重构达标：

- Desktop 双击选词后，整词选中，caret 在词尾
- Desktop 三击选行后，整行选中，caret 在三击位置
- 非最后一行按行选中时，可以包含行尾换行，但 caret 不被强制推到下一行
- reveal 与输入锚点跟随真实 caret，而不是范围末端
- Android 长按选词、手柄与平台工具栏路线无回归
- `onSelectionChange` / `onCursorChange` 的投影结果与新模型保持一致

## 当前状态

- 状态：`已计划`
- 当前目标：完成 Selection / Caret 完整模型重构设计并进入实现
- 对应总进度：[`../status.md`](../status.md)
