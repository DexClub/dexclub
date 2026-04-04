# Viewer / 只读渲染

## 目的

本册用于说明 `CodeViewer` 与只读渲染主链。

这里要回答的问题是：

- 只读视图当前如何绘制
- `Viewer` 和 `Editor` 的共享部分在哪里
- gutter、decoration、annotation 命中现在落在哪条链上
- 当前只读态还差哪些回归

## 范围

本册覆盖：

- `CodeViewer`
- `CodeViewerCanvas`
- gutter / line numbers
- content / decoration 配置
- selection / search highlight / cursor 绘制
- annotation hit / context menu
- 只读态滚动与 reveal

本册不覆盖：

- 编辑态输入桥
- 选区手势细节
- 平台菜单实现细节

相关主题请见：

- 输入与 IME：[`editor-input-ime.md`](editor-input-ime.md)
- Selection / Caret：[`selection-caret.md`](selection-caret.md)
- 上下文交互：[`context-actions.md`](context-actions.md)

## 当前架构

当前 `CodeViewer` 已经不再使用 `BasicText` 直接显示全文，而是走共享的 Canvas 渲染主链。

当前只读态主链可以概括为：

1. 文本、token、annotation 先进入共享布局快照
2. viewport 决定当前可见区域
3. `CodeViewerCanvas` 只绘制可见区需要的内容
4. annotation 点击、长按和次键上下文命中都走同一套坐标链

这一条链和 `CodeEditor` 的关系是：

- Viewer / Editor 共用布局快照
- Viewer / Editor 共用 viewport
- Viewer / Editor 共用 `CodeViewerCanvas`
- Editor 只是在此基础上叠加输入、selection state 与平台桥

## 当前绘制模型

当前绘制顺序已经收敛到同一条自绘管线：

- gutter 与行号
- search highlight
- selection 背景
- decoration 背景
- 正文文本
- caret
- annotation 命中相关 overlay

当前几个关键结论已经明确：

- 文本只按可见行绘制，不再整份全文直接交给 Compose 文本控件
- 横向 caret、selection、annotation 命中与 reveal 继续复用 `TextLayoutResult`
- 垂直行框不再直接跟随当前行瞬时 mixed layout，而是基于稳定行度量
- 混排正文按分段绘制，而不是让整行瞬时 layout 同时决定横向和纵向几何

## 当前可配置项

当前只读渲染相关的公开配置已经开始收口，不再继续堆散落参数。

当前已落地的配置方向包括：

- `CodeGutterOptions`
- `CodeContentOptions`
- `CodeDecorationOptions`
- `scrollPastEnd`

它们的职责边界大致是：

- gutter：行号区与左侧辅助区域的显示
- content：正文起始 inset 与内容区布局
- decoration：高亮、当前行、辅助装饰的开关与样式方向
- `scrollPastEnd`：底部额外预留空间与 reveal 裁剪约束

## 当前已完成能力

当前 `Viewer` 侧已经完成的能力包括：

- `CodeViewer` 切到 Canvas 自绘
- 共享布局快照、viewport 与只读渲染链打通
- token 基础颜色绘制
- `selection` 背景绘制
- `searchHighlight` 背景绘制
- `cursor` 基础绘制
- `cursorTarget` 驱动的 reveal
- annotation 点击命中
- 长按上下文命中
- Desktop 次键上下文命中
- 行号 gutter、内容区起始 inset 与 decoration 配置第一版
- `scrollPastEnd` 的可见区裁剪与 reveal 接入

换句话说，`Viewer` 当前已经不是占位实现，而是一条真实可复用的渲染管线。

## 当前问题

### 1. Viewer 的只读交互还没有完全收尾

当前 `Viewer` 已经具备注解命中和上下文入口，但这些点还没有全部收尾：

- 点击空白区定位到行尾仍未明确收口
- 只读态是否需要自己的 caret / 选区交互仍未完全定稿
- 横向滚动后的细粒度命中仍需要真实工作区回归验证

### 2. 真实工作区回归还没完成

当前最需要继续验证的不是基础绘制是否存在，而是业务页中的体感和正确性：

- 关闭并重新打开标签页后的滚动恢复
- 搜索高亮恢复
- 导航跳转后的 `cursorTarget` reveal
- 长行场景下的注解命中和右键菜单位置
- 只读场景下的 caret 位置恢复

### 3. 主题化和更复杂左侧辅助栏仍未进入本轮必须项

当前基础 gutter 已经落地，但这些能力还不属于当前已完成范围：

- 断点栏
- 折叠 gutter
- 诊断标记
- 更复杂的主题化渲染体系

这不是遗漏，而是本轮边界控制。

## 当前命中模型

当前 annotation 命中已经收敛到统一坐标链：

1. 屏幕坐标
2. viewport 坐标
3. 当前逻辑行与列
4. 全局 offset
5. annotation range

这条链已经具备几个关键约束：

- 视口下方空白区不应误命中 annotation
- 行尾右侧空白区不应误命中 annotation
- 横向滚动后的命中需要扣除真实水平偏移

这也是 `Viewer` 和上下文交互之间的主要连接点。

## 与 Editor 的边界

`CodeViewerCanvas` 是共享渲染器，但 `Viewer` 和 `Editor` 的职责仍然需要分开理解。

`Viewer` 负责：

- 只读正文绘制
- 只读态的 caret / selection / highlight 可视化
- annotation 命中
- 只读态滚动与 reveal

`Editor` 在此基础上额外负责：

- 文本编辑
- 输入法桥接
- 手势选区
- 手柄与 composing overlay

因此后续如果要补更多只读交互，不应绕回单独第二套渲染体系，而应继续在共享 Canvas 管线上扩展。

## 当前计划

当前建议的推进顺序：

1. 先完成真实工作区的只读回归
2. 再补 `Viewer` 命中和空白区行为细节
3. 再视需要决定是否让 `Viewer` 增加更多只读交互
4. 最后再评估是否拆更复杂的 decoration / gutter 子系统

## 验收标准

以下条件满足后，可认为当前阶段的 `Viewer` 基本达标：

- 只读态正文、selection、search highlight 与 caret 渲染稳定
- gutter、content inset 与 decoration 配置在真实工作区中表现稳定
- annotation 点击、长按和 Desktop 次键命中稳定
- 横向滚动与长行场景下命中不明显错位
- `cursorTarget`、滚动恢复与搜索高亮恢复在真实工作区中可用

## 当前状态

- 状态：`进行中`
- 当前目标：完成真实工作区下的只读渲染与命中回归
- 对应总进度：[`../status.md`](../status.md)
