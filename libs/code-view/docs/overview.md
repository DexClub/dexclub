# code-view 总览

## 组件定位

`code-view` 是一组面向 Kotlin Multiplatform / Compose 的代码查看与编辑组件。

它不是单一控件，而是一套组件家族，当前主要服务于 DexClub 的工作区代码页，目标是提供：

- 只读代码查看
- 可编辑代码编辑
- 语言高亮与注解命中
- 跨平台输入桥接
- 可恢复的滚动、选区与 caret 状态

这套组件家族当前主要覆盖两个平台：

- Desktop / JVM
- Android

## 组件家族由什么构成

从外部看，`code-view` 主要由这些部分组成：

- `CodeViewer`
  只读代码查看器
- `CodeEditor`
  Canvas 自管编辑器
- `CodeRuntime`
  document 到 tokens / annotations 的运行时桥接
- `CodeAddons`
  语言能力安装入口
- `core` 文本模型
  包括 `CodeDocument`、`Cursor`、`LineSelection`、`CodeSelection`

从内部看，这套组件依赖几条核心子系统：

- 布局与坐标系统
- viewport 与滚动恢复
- Canvas 渲染
- Selection / Caret / 手势
- 输入与 IME
- 上下文交互与平台菜单
- 性能、降级与回归

## 当前能力边界

当前已经具备的主能力：

- `CodeViewer` 的 Canvas 自绘正文显示
- `CodeEditor` 的 Canvas 自管编辑主链
- 基于布局快照的 offset / line / column 映射
- viewport、`scrollPastEnd`、cursor reveal
- token 高亮与 annotation 命中
- Desktop 专用输入宿主
- Android `InputConnection` 输入桥
- Desktop 双击选词、三击选行第一版
- Android 长按选词与基础手柄

当前仍在继续收尾的能力：

- 真实工作区页面完整回归
- Selection / Caret 模型完整重构
- Desktop / Android 的复杂边界语义对齐
- 长文本、长行与降级策略专项回归

## 当前平台语义

### Desktop

当前 Desktop 走鼠标优先路线：

- 主点击放置 caret
- 拖拽扩展选区
- 双击选词
- 三击选行
- 次键触发上下文菜单

### Android

当前 Android 走触摸优先路线：

- 单击放置 caret
- 普通拖动优先滚动
- 长按选词
- 手柄负责精细调整
- 平台 `SelectionToolbar` 优先

这两条路线不是“实现没统一”，而是产品语义本来就不应统一。

## 当前架构判断

截至 `2026-04-04`，`code-view` 已经不是“占位控件”，而是一套基本成型的编辑器组件家族。

更准确地说，它目前处在：

- 主架构已稳定
- 主要平台路径已打通
- 行为语义仍在继续收口
- 真实业务页面回归还未完成

因此当前最重要的工作不是再换大架构，而是：

- 补规范层文档
- 做真实工作区回归
- 收口 Selection / Caret 模型

## 适合谁看

### 想快速建立脑图

先读：

1. [`module-map.md`](module-map.md)
2. [`status.md`](status.md)
3. [`design/layout-viewport.md`](design/layout-viewport.md)

### 想继续做编辑器交互

先读：

1. [`design/selection-caret.md`](design/selection-caret.md)
2. [`design/editor-input-ime.md`](design/editor-input-ime.md)
3. [`spec/behavior-spec.md`](spec/behavior-spec.md)

### 想理解状态与 API

先读：

1. [`spec/state-model-spec.md`](spec/state-model-spec.md)
2. [`spec/api-spec.md`](spec/api-spec.md)

### 想评估当前推进状态

先读：

1. [`status.md`](status.md)
2. [`design/performance-regression.md`](design/performance-regression.md)

## 阅读原则

这套文档分三层：

- 总览层
  回答“这是什么、有哪些模块、现在做到哪”
- 设计层
  回答“系统怎么拆、每块边界是什么”
- 规范层
  回答“状态真相源是什么、行为应当如何表现、什么算通过”

推荐理解方式是：

- 先总览
- 再设计
- 最后规范

不要一开始就直接从旧的重构流水账文档进入。

## 迁移原则

旧文档里的有效信息会逐步拆进新体系：

- 架构背景与阶段拆分迁到设计层
- 当前推进状态迁到 `status.md`
- 输入锚点规则迁到输入册与状态模型规范
- 选区规则迁到 Selection / Caret 册与行为规范
- API 与验收要求逐步下沉到 `spec/`

这意味着：

- 新体系是未来主入口
- 旧文档短期仍保留为迁移来源
- 文档迁移本身也是当前工作的一部分
