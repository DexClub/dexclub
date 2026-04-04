# 布局与坐标系统

## 目的

本册用于说明 `code-view` 的文本分行、offset / line / column 映射、viewport、reveal 与相关基础坐标系统。

它回答的是：

- 代码文本如何被切成稳定的逻辑行
- 坐标如何在 offset、行列、屏幕位置之间转换
- viewport 和 reveal 建立在什么基础上
- 为什么 Viewer 和 Editor 必须共用这套底层

## 范围

本册覆盖：

- `CodeLayoutSnapshot`
- `CodeLayoutSnapshotFactory`
- `CodeLineLayout`
- `CodeLineTextLayoutCache`
- `CodeViewportState`
- offset / line / column 双向映射
- `scrollPastEnd`
- reveal 基础逻辑

本册不覆盖：

- 平台输入桥
- 具体手势交互
- 上下文菜单动作

## 当前架构

当前 `code-view` 的布局与坐标系统已经是 Viewer / Editor 的共享底座。

当前主链可以概括为：

1. 文本先进入 `CodeLayoutSnapshotFactory`
2. 生成基于逻辑行的 `CodeLayoutSnapshot`
3. snapshot 提供 offset、行列和范围裁剪能力
4. viewport 在 snapshot 基础上计算可见区与 reveal
5. Viewer / Editor 共用这套结果做绘制、命中和状态投影

这套底座存在的原因很直接：

- 不再依赖某个文本控件内部的临时布局
- 不让 Viewer / Editor 各自维护一套坐标系统
- 让滚动恢复、selection、cursor、annotation hit 和 reveal 建立在同一份几何基础上

## 当前布局模型

当前布局系统的几个关键概念是：

- 逻辑行
- 全局文本 offset
- 行内 offset
- 行级 token / annotation 装饰
- 当前可见 viewport

当前设计选择是：

- 先按逻辑行切分全文
- 再在逻辑行上叠 token / annotation
- 横向几何继续复用 `TextLayoutResult`
- 垂直行框使用稳定行度量

## 当前已完成能力

当前这套底座已经完成的能力包括：

- 文本逻辑行切分，兼容 `\n` / `\r\n` / `\r`
- `offset <-> (line, column)` 双向映射
- `LineSelection` / `CodeSelection` / `Cursor` 的基础转换与裁剪
- token 按逻辑行切片
- 基于文本基础布局再叠 decoration 的 `withDecorations(...)` 路径
- viewport 首尾可见行计算
- 横向滚动范围裁剪
- 基于 `Cursor` / `cursorTarget` 的 reveal 基础逻辑
- `scrollPastEnd` 对可见区裁剪和 reveal 的接入

换句话说，当前布局层已经不是设计概念，而是实际运行中的底座。

## 当前坐标原则

当前坐标系统应遵守这些原则：

- 全局 offset 是文本级唯一坐标
- 行列坐标是 UI 层更容易使用的投影
- 屏幕命中要先回到 viewport，再回到行列和 offset
- `LineSelection` 和 `Cursor` 都是从布局快照投影出来的 UI 友好表示

这意味着：

- 不能在不同子系统里各自发明一套 offset / line / column 映射
- 命中、selection、cursor、annotation 都必须回到同一套 snapshot

## 当前 viewport 模型

viewport 当前负责：

- 首可见行
- 横向滚动偏移
- 可见起止行
- 程序化 reveal

当前约束是：

- `initialFirstVisibleLine` 和 `initialScrollOffsetX` 只是初始输入
- 真正运行后，以内部 viewport 状态和回调结果为准
- `scrollPastEnd` 不只是视觉留白，也会影响 reveal 底边

## 当前问题

### 1. 真实工作区回归还没做完

布局层本身已经接通，但这些页面级场景还需要继续确认：

- 滚动恢复
- 搜索高亮恢复
- 导航跳转 reveal
- 长行横向滚动下的命中和右键菜单位置

### 2. 更复杂的布局能力还没进入本轮

当前仍未纳入本轮必须项的有：

- 软换行
- 折叠布局
- 矩形选区几何
- 更复杂的 gutter 几何分层

这不是遗漏，而是当前边界控制。

## 与其他系统的关系

### 与 Viewer

- Viewer 基于布局快照和 viewport 做只读绘制与 annotation hit

### 与 Editor

- Editor 在同一份布局快照上叠加 selection、caret、输入和 composing

### 与 Selection / Caret

- `LineSelection`、`Cursor` 和 `CodeSelection` 的基础映射都依赖布局快照

### 与输入锚点

- input anchor 的 fallback caret 定位也依赖布局快照投影

## 当前计划

当前建议的推进顺序：

1. 继续完成真实工作区下的滚动与 reveal 回归
2. 再评估是否需要补更多坐标与布局纯函数测试
3. Selection / Caret 模型重构时，继续复用同一套布局底座
4. 后续若做更复杂布局能力，再在这一层扩展

## 验收标准

以下条件满足后，可认为当前阶段的布局与坐标系统基本达标：

- `offset <-> line / column` 映射稳定
- `LineSelection` / `Cursor` 投影稳定
- viewport 可见范围计算稳定
- `cursorTarget` 与输入后 reveal 稳定
- `scrollPastEnd` 在滚动与 reveal 上表现一致
- Viewer / Editor 继续共用同一套底座，没有重新分叉

## 当前状态

- 状态：`已完成`
- 当前目标：保持共享布局与坐标底座稳定，为上层交互和回归继续提供统一基础
- 对应总进度：[`../status.md`](../status.md)
