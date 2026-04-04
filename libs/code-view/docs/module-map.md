# code-view 模块地图

## 目的

本页用于回答两个问题：

- `code-view` 这套组件家族由哪些模块组成
- 各模块之间的数据和职责是怎么流动的

它不替代设计文档，只负责建立整体脑图。

## 模块列表

### `code-view-core`

职责：

- 核心公开模型
- 文本、语言、token、annotation、document 等基础类型

当前典型内容：

- `CodeDocument`
- `Cursor`
- `LineSelection`
- `CodeSelection`
- `CodeTokenSpan`
- `CodeAnnotation`

这个模块回答的是：

- 什么是公开数据结构
- UI 层和 runtime 层共享什么基础类型

### `code-view-compose`

职责：

- Compose UI 主入口
- `CodeViewer` / `CodeEditor`
- 布局快照、viewport、Canvas 渲染
- pointer / key / handle / overlay 交互
- 平台输入桥接适配层

这是当前最活跃、变化最多的模块，也是组件行为最集中落地的地方。

### `code-view-language`

职责：

- 语言安装入口
- 语言能力描述
- addon 组装

这里不直接做渲染，而是负责“有哪些语言能力可供 runtime 安装和使用”。

### `code-view-runtime`

职责：

- `CodeDocument` 到 tokens / annotations 的运行时桥接
- 语言 session 管理
- degrade / fallback 决策
- surface controller 缓存

这里回答的是：

- 文本变成什么高亮和注解
- 大文件或超长行时如何降级

### `code-view-tree-sitter`

职责：

- tree-sitter 抽象桥接
- query 加载
- 高亮映射

这是具体语言包和上层语言系统之间的通用桥。

### `code-view-tree-sitter-java`

职责：

- Java 语言安装与 session

### `code-view-tree-sitter-kotlin`

职责：

- Kotlin 语言安装与 session

### `code-view-tree-sitter-smali`

职责：

- Smali 语言安装与 session

### `code-view-bom`

职责：

- 组件依赖版本管理

它不直接参与运行时逻辑，但负责版本一致性。

## 主要依赖方向

整体依赖方向可以简单理解为：

- `core` 提供基础模型
- `language` 和 `tree-sitter-*` 提供语言能力
- `runtime` 把 document 变成 surface 数据
- `compose` 把 surface 数据变成可见 UI 和可交互编辑器

换句话说：

- `core` 是模型底座
- `runtime` 是内容生产层
- `compose` 是展示和交互层

## 主要数据流

### 只读路径

1. `CodeDocument` 提供文本快照
2. `CodeRuntime` 根据 document + addons 产出 tokens / annotations
3. `CodeLayoutSnapshot` 建立文本坐标系
4. `CodeViewerCanvas` 根据 viewport 绘制可见内容
5. annotation hit / context menu 从画布坐标回映射到 offset

### 编辑路径

1. `CodeEditor` 维护编辑态文本与选区
2. 平台输入桥负责 IME / commit / selection update
3. 画布继续作为唯一可见编辑器
4. caret、selection、reveal、input anchor 都围绕编辑态主链更新
5. 文本提交后驱动 runtime 刷新 tokens / annotations

### 语言路径

1. `CodeAddons` 安装语言能力
2. `CodeRuntime` 按 document + addons 获取 surface controller
3. controller 产出 tokens / annotations
4. `compose` 侧消费这些结果并渲染

## 当前高耦合区域

现阶段需要联动理解的高耦合区域主要有三组：

- `selection / caret / reveal`
- `IME / input anchor / composing`
- `viewport / text layout / annotation hit`

这三个区域的特点是：

- 单独看都能成立
- 但真实问题往往出现在它们的交界处

所以后续继续重构时，应优先按能力域推进，而不是零散改单个函数。

## 当前模块边界提醒

### 不要让 `compose` 再长出第二套模型层

当前 `compose` 内部有不少过渡状态，但长期来看：

- 公开模型应尽量收口在 `core`
- `compose` 更适合承接布局、渲染和交互状态

### 不要让 `sharedUI` 继续吃底层职责

`sharedUI` 当前负责页面接线和兜底菜单，但不应继续承担：

- 底层 selection 手势
- 输入桥状态机
- 平台菜单桥接

这些职责应继续留在 `libs/code-view` 内部。

### runtime 负责内容生产，不负责 UI 语义

runtime 可以决定：

- token
- annotation
- degrade / fallback

但不负责：

- caret
- selection
- viewport
- 平台交互

## 关联阅读

- 总览：[`overview.md`](overview.md)
- 总进度：[`status.md`](status.md)
- 布局与坐标：[`design/layout-viewport.md`](design/layout-viewport.md)
- 只读渲染：[`design/viewer-rendering.md`](design/viewer-rendering.md)
- Selection / Caret：[`design/selection-caret.md`](design/selection-caret.md)
- 输入与 IME：[`design/editor-input-ime.md`](design/editor-input-ime.md)
