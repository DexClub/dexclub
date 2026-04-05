# API 规范

## 目的

本册用于记录 `code-view` 当前公开 API 的稳定语义。

它不试图逐个抄录源码签名，而是回答：

- 哪些公开入口是主入口
- 每类参数表达什么语义
- 哪些参数是初始值，哪些参数是持续状态
- 当前公开模型各自负责什么

## 范围

本册覆盖：

- `CodeViewer`
- `CodeEditor`
- `CodeRuntime`
- `CodeAddons`
- 核心公开模型

## 公开主入口

当前公开主入口主要包括：

- `CodeViewer`
- `CodeEditor`
- `CodeRuntime()`
- `CodeAddons.build { ... }`

### `CodeViewer`

`CodeViewer` 当前有三类入口：

- `text + languageId`
- `CodeTextValue`
- `CodeDocument`

其中最完整、最适合业务主路径的入口是：

- `CodeViewer(document, addons, ...)`

前两类入口更适合：

- 简单示例
- 受控外部文本输入
- 不想自己管理 `CodeDocument` 的调用方

### `CodeEditor`

`CodeEditor` 当前有三类入口：

- `initialText + languageId`
- `text + onTextChange + languageId`
- `document`

其中：

- `text + onTextChange` 更接近标准受控 Compose 用法
- `document` 更适合工作区主路径和高级场景
- `initialText` 更接近便捷入口

补充约束：

- `initialText` 重载当前内部会自己维护一份 `text` 状态，再同步到内部 `CodeDocument`
- `text + onTextChange` 重载当前会在 `remember(languageId)` 后持有内部 `CodeDocument`，再通过 `LaunchedEffect(text)` 把外部文本同步进去
- `document` 重载最接近当前真实主路径，因为它不再额外隐藏一层文本容器

## 公开模型语义

### `Cursor`

`Cursor(line, offset)` 用于表达：

- caret 在哪

它不表达：

- 范围选区
- anchor 方向
- 选择模式

### `LineSelection`

`LineSelection` 用于表达：

- 按行坐标保存和恢复的范围选区

它适合：

- 搜索高亮
- 外部选区恢复
- 只读态范围高亮

它不适合：

- 表达完整编辑态活动端

### `CodeSelection`

`CodeSelection(anchorOffset, caretOffset)` 当前更接近内部偏移级选区模型。

当前 API 现实是：

- 它已公开
- 但它还不是最终完整模型

后续计划方向是：

- 升级为带 `mode` 的完整模型

### `CodeViewerCursorTarget`

`CodeViewerCursorTarget(line, offset, token)` 用于表达：

- 一次程序化 reveal 请求

其中 `token` 的语义是：

- 去重用标记
- 相同 token 不应重复触发同一次滚动

### `CodeViewerInteractionOptions`

当前主要用于：

- 注解命中相关交互配置

目前主要公开语义是：

- `annotationTag`

## 参数语义规范

## 初始参数

下面这些参数表达的是初始恢复值，而不是持续真相源：

- `initialFirstVisibleLine`
- `initialScrollOffsetX`

它们的语义是：

- 组件首次进入时尝试恢复到指定位置
- 之后应以内部滚动状态和回调结果为准

## 持续输入参数

下面这些参数表达的是持续控制或持续投影：

- `selection`
- `cursor`
- `searchHighlight`
- `cursorTarget`
- `interactionOptions`
- `textStyle`
- `gutterOptions`
- `contentOptions`
- `decorationOptions`

它们的语义分别是：

- `selection`
  当前范围选区投影
- `cursor`
  当前 caret 投影
- `searchHighlight`
  当前活动搜索命中范围高亮投影
- `cursorTarget`
  一次程序化 reveal 请求
- `textStyle`
  Canvas 文本绘制与测量基准样式
- gutter / content / decoration options
  只读和编辑渲染的结构性配置

补充说明：

- 当前完整页内搜索会话并不是 `CodeEditor` 的公开输入模型
- 上层若需要“搜索框是否显示、查询词、命中计数、上一项 / 下一项”，应在业务层自行维护搜索会话
- 当前 `searchHighlight` 只负责把“当前活动命中”投影给 `code-view`

## 回调语义

### `onScrollChange`

用于回传：

- 当前首可见行
- 当前横向滚动偏移

当前实现语义：

- `firstVisibleLine` 或横向滚动值变化时触发

### `onViewportChange`

用于回传：

- 当前可见起止行

当前实现语义：

- 首可见行或末可见行变化时触发

### `onSelectionChange`

用于回传：

- 当前范围选区投影

当前实现语义：

- 仅在 `CodeEditor` 内部 `fieldValue` 发生变化时触发
- 回传值基于当前编辑态文本布局快照换算得到

### `onCursorChange`

用于回传：

- 当前 caret 投影

当前实现语义：

- 仅在 `CodeEditor` 内部 `fieldValue` 发生变化时触发
- 回传值基于当前编辑态文本布局快照换算得到

### `onAnnotationHit`

用于回传：

- annotation 命中结果

### `onContextMenu`

用于回传：

- 当前上下文菜单调用点
- 命中的 annotation，若存在

### `onTextChange`

`onTextChange` 仅存在于 `CodeEditor` 路线。

当前实现语义：

- 只有在 `composition == null` 且文本相对 `snapshot.text` 发生变化时才触发
- composing 中间态不会立即触发 `onTextChange`

## `readOnly` 语义

`CodeEditor` 当前公开 `readOnly` 参数。

它的规范语义是：

- `readOnly = false`
  表示编辑态
- `readOnly = true`
  表示只读态

并且：

- 编辑态中 caret 必须存在
- 只读态中是否显示 caret 可以由 `cursor` 控制
- `readOnly = true` 时，当前内部 `TextFieldValue` 会被持续同步回 `snapshot.text + externalSelection`

## `scrollPastEnd` 语义

`scrollPastEnd` 的规范语义是：

- 在最后一行下方额外保留若干空白行

它影响：

- 可滚动内容高度
- viewport 裁剪
- caret / cursorTarget reveal 的底部边界

当 `scrollPastEnd <= 0` 时：

- 不额外预留底部空白

## `CodeRuntime` 语义

`CodeRuntime` 当前的公开职责是：

- 为 `document + addons` 返回 surface controller
- 按 `DocumentId` 释放文档相关资源
- 整体关闭 runtime

当前工厂语义：

- `CodeRuntime()` 是一个顶层工厂函数，返回默认 runtime 实现

它不负责：

- 组件交互
- selection / caret
- viewport

## `CodeAddons` 语义

`CodeAddons` 的公开职责是：

- 安装语言能力
- 在构建时校验语言能力冲突

构建方式当前规范为：

```kotlin
CodeAddons.build {
    install(...)
}
```

若出现：

- 相同 familyId + languageId 重复安装
- 不同 familyId 对同一 languageId 的能力冲突

则应抛出冲突异常。

## 当前 API 结论

当前 API 设计的整体方向是：

- `core` 提供稳定公开模型
- `compose` 提供高层入口与 UI 参数
- `runtime` 提供内容生产桥
- `language` 提供语言安装入口

而不是让外部直接操作内部布局、渲染和输入桥细节。

## 当前状态

- 状态：`进行中`
- 当前目标：先收口公开语义，再根据 Selection / Caret 模型重构决定是否升级部分公开模型
- 关联文档：[`state-model-spec.md`](state-model-spec.md)、[`behavior-spec.md`](behavior-spec.md)
