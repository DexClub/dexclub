# 上下文交互与平台菜单

## 目的

本册用于说明 `code-view` 中和上下文动作相关的交互边界。

这里要回答的问题是：

- 桌面右键和移动端长按各自负责什么
- annotation 上下文和 selection 上下文如何分开
- 平台 `SelectionToolbar` 与共享 `CodeContextMenu` 的关系是什么
- 复制、全选、跳转定义这类动作应从哪里进入

## 范围

本册覆盖：

- annotation hit
- Desktop secondary click
- Android long press
- `CodeContextMenu`
- 平台 `SelectionToolbar`
- 复制 / 全选 / 跳转定义等上下文动作入口

本册不覆盖：

- 选区手势本身
- 输入法桥接
- 更高层业务菜单的最终 UI 样式

相关主题请见：

- Selection / Caret：[`selection-caret.md`](selection-caret.md)
- 输入与 IME：[`editor-input-ime.md`](editor-input-ime.md)

## 当前分层

当前上下文交互已经明确分成两条主线：

### 1. annotation 上下文

annotation 上下文用于：

- 跳转定义
- 对符号、引用或注解位置执行次级动作
- 和具体文本选区无关的上下文操作

它的入口当前包括：

- `CodeViewer` 主点击命中
- `CodeViewer` 长按上下文命中
- Desktop 次键上下文命中
- `CodeEditor` 中的 Desktop 次键 annotation 命中

### 2. selection 上下文

selection 上下文用于：

- 复制
- 全选
- 后续可能扩展的剪切、粘贴、分享、搜索等动作

它的入口当前按平台分开：

- Desktop：次键菜单和后续共享菜单
- Android：优先平台 `SelectionToolbar`

annotation 上下文和 selection 上下文不应混成一套规则。

## Desktop

Desktop 当前采用桌面编辑器式语义：

- 主点击优先给文本编辑、caret 定位和 selection
- annotation 主点击只在 `Viewer` 路线中承担直接命中
- 次键负责上下文菜单

当前已经明确的边界是：

- 在 `CodeEditor` 中，主点击当前保留给文本选择 / caret 定位
- 编辑态 annotation 当前优先只开放上下文命中，不抢占主点击
- `Viewer` 中可以直接进行 annotation 命中，因为它没有编辑优先级冲突

这条规则的核心目的很直接：

- 不让“点击文本编辑”和“点击注解跳转”在编辑态互相争抢主手势

## Android

Android 当前采用触摸优先语义：

- 主画布单击优先放置 caret
- 长按进入选区态
- 长按结束后再出现 selection 上下文入口
- 选区稳定后通过平台工具栏和手柄继续完成动作

当前 Android 上下文主路线是：

- 优先走 `PlatformSelectionToolbarBridge`

当前已接通的基础动作是：

- 复制
- 全选

`sharedUI` 中的 `CodeContextMenu` 当前保留为：

- 不支持平台工具栏时的兜底路径

这意味着当前 Android 的设计方向已经很明确：

- selection 上下文尽量平台化
- annotation 与业务动作继续由共享层或上层页面接住

## 当前动作入口

当前已经明确的动作入口可以分成三类：

### 文本选择动作

- 复制
- 全选
- 后续可能扩展的剪切、粘贴

这类动作的触发前提通常是：

- 当前已有有效选区

### 注解 / 符号动作

- 跳转定义
- 打开上下文菜单
- 后续可能扩展的更多符号相关动作

这类动作的触发前提通常是：

- annotation hit 成功

### 平台菜单动作

- Android 平台工具栏
- Desktop 右键菜单

这类动作不是具体业务动作本身，而是动作承载容器。

## 当前问题

### 1. annotation 上下文与文本编辑优先级仍需继续收口

当前在 `Editor` 路线里已经明确：

- 主点击优先服务编辑
- 次键才服务 annotation 上下文

但如果后续要支持更多编辑态注解动作，仍然需要继续明确：

- annotation 命中和文本选区谁先处理
- 命中后是否允许保留现有选区
- 编辑态右键命中对象与菜单内容如何收口

### 2. Android 的平台菜单虽然已接入，但共享兜底路径还在过渡期

当前 Android 已经优先走平台 `SelectionToolbar`，但还没有完全收尾：

- 动作仍只覆盖复制 / 全选
- 不支持平台工具栏时仍依赖共享 `CodeContextMenu`
- annotation 相关动作与 selection 动作仍没有完全拆成独立菜单模型

### 3. 真实工作区回归还没做完

当前最需要继续确认的是：

- Desktop 右键菜单位置是否稳定
- 长行横向滚动后 annotation 命中与菜单位置是否正确
- Android 长按后菜单入口时机是否符合预期
- Android 手柄拖拽期间工具栏的收起与恢复是否稳定

## 平台桥接边界

当前已经确认一个重要原则：

- 不直接按平台名在业务层写分支

平台差异应继续通过桥接层收口，例如：

- `PlatformEditorBridge`
- `PlatformSelectionToolbarBridge`

公共层负责：

- annotation hit
- selection state
- 上下文动作所需的数据

平台层负责：

- 桌面右键菜单的宿主承接
- Android 平台工具栏的调用
- 后续可能的触觉反馈或平台扩展动作

## 与 sharedUI 的关系

当前 `sharedUI` 已经接通：

- 选中文本提取
- “全选”
- `CodeContextMenu` 兜底路径
- 工作区中的 `onSelectionChange` / `onCursorChange` 状态回写

当前这层的角色应理解为：

- 提供页面级动作接线
- 在平台菜单缺位时兜底
- 不承担底层 selection 手势和平台输入桥逻辑

也就是说，后续如果继续完善平台菜单，应该优先补底层桥接和动作模型，而不是让 `sharedUI` 继续吞更多底层职责。

## 当前计划

当前建议的推进顺序：

1. 先完成 Desktop 真实工作区下的右键菜单回归
2. 再完成 Android 长按、平台工具栏与手柄交互回归
3. 再梳理 annotation 上下文和 selection 上下文的动作模型
4. 最后评估 `CodeContextMenu` 是否继续保留为长期兜底

## 验收标准

以下条件满足后，可认为当前阶段的上下文交互基本达标：

- Desktop `Viewer` / `Editor` 中 annotation 次键命中稳定
- Desktop 右键菜单位置、命中对象和动作入口正确
- Android 长按进入选区后的菜单入口时机正确
- Android 平台 `SelectionToolbar` 能稳定承接复制 / 全选
- 手柄拖拽期间工具栏能临时收起，结束后能按当前选区恢复
- 共享 `CodeContextMenu` 只作为兜底，不再承担主路线职责

## 当前状态

- 状态：`已计划`
- 当前目标：收口 annotation 上下文、selection 上下文和平台菜单边界
- 对应总进度：[`../status.md`](../status.md)
