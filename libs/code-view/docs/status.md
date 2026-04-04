# code-view 总进度

本页是 `code-view` 文档体系中的单一进度入口。

用途只有两个：

- 说明每个能力域当前做到哪一步
- 给后续设计文档、规范文档和回归工作提供统一导航

这里不记录完整实现细节，也不保留长时间线流水账。具体设计请进入 `design/`，具体行为契约请进入 `spec/`。

## 状态定义

- `待开始`：还没有进入实际推进
- `已计划`：目标和方向已明确，但尚未开始实现
- `进行中`：已经开始推进，但还没有达到当前验收标准
- `已完成`：达到当前阶段验收标准，后续仅做维护或增量修正
- `阻塞`：存在明确阻塞，短期内无法推进
- `已废弃`：路线已放弃，不再继续

## 当前结论

截至 `2026-04-04`，`code-view` 当前状态可以概括为：

- 主架构已经切到“Canvas 自管渲染与编辑 + 平台输入桥”
- 共享布局快照、viewport、Viewer Canvas、自绘 Editor 主链都已接通
- Desktop 当前保留鼠标优先语义，并已接通双击选词与三击选行第一版
- Android 当前保留触摸优先语义：单击放 caret、普通拖动优先滚动、长按选词
- 当前最需要继续推进的不是再换架构，而是：
  - Selection / Caret 模型完整重构
  - Desktop 真实工作区回归
  - Android 真机完整回归
  - 新文档体系迁移

## 总表

| 主题 | 状态 | 当前目标 | 最近更新 | 下一步 |
| --- | --- | --- | --- | --- |
| 布局与坐标系统 | 已完成 | 维护稳定坐标基础 | 2026-04-04 | 只做增量修正 |
| Viewer / 只读渲染 | 进行中 | 完成真实工作区回归 | 2026-04-04 | 验证滚动恢复、搜索高亮、注解命中与右键菜单 |
| Selection / Caret / 手势 | 已计划 | 启动完整模型重构 | 2026-04-04 | 先固化模型与平台语义 |
| Editor 输入与 IME | 进行中 | 稳定 Desktop / Android 输入主链 | 2026-04-04 | 继续做边界回归 |
| 上下文交互与平台菜单 | 已计划 | 收口桌面右键与移动端长按菜单模型 | 2026-04-04 | 梳理触发、动作和平台桥边界 |
| 性能、降级与回归 | 进行中 | 长文本与大文件专项回归 | 2026-04-04 | 补测试矩阵与手测结论 |
| 文档体系迁移 | 进行中 | 建立新的 docs 小册并迁移旧文档 | 2026-04-04 | 先迁移核心三册与总进度 |

## 当前优先级

### P0

- 文档体系迁移
- Selection / Caret 模型重构设计收口
- Desktop 工作区真实页面回归

### P1

- Android 真机完整回归
- 上下文交互与平台菜单整理

### P2

- 长文本与长行性能专项验证
- `spec/` 层文档补全

## 主题入口

- 布局与坐标系统：[`design/layout-viewport.md`](design/layout-viewport.md)
- Viewer / 只读渲染：[`design/viewer-rendering.md`](design/viewer-rendering.md)
- Selection / Caret / 手势：[`design/selection-caret.md`](design/selection-caret.md)
- Editor 输入与 IME：[`design/editor-input-ime.md`](design/editor-input-ime.md)
- 上下文交互与平台菜单：[`design/context-actions.md`](design/context-actions.md)
- 性能、降级与回归：[`design/performance-regression.md`](design/performance-regression.md)

## 当前计划

### 已完成

- 新的 `docs/` 小册骨架已建立
- `overview.md`、`module-map.md`、`index.md` 已作为新入口落地
- Desktop 编辑态已补双击选词与三击选行第一版
- Android 已稳定在“单击放 caret + 滚动优先 + 长按选词”的路线
- `code-view-compose` 当前已再次通过 JVM / Android 编译和 `jvmTest`

### 进行中

- 旧文档向新小册迁移
- Desktop 工作区真实页面回归准备
- Desktop / Android 输入边界回归

### 已计划

- Selection / Caret 完整模型重构
- 上下文交互与平台菜单整理
- `spec/` 行为契约和测试矩阵补全

## 迁移状态

旧的大一统文档与规则文档内容已经并入当前 `docs/` 体系。

当前原则：

- 新文档是唯一主入口
- 设计、进度、规范继续分层维护
- 后续不再回到大一统流水账结构

## 维护规则

- 总体状态变化优先更新本页
- 单个主题的实现边界与设计更新到对应 `design/` 文档
- 公开行为要求逐步沉淀到 `spec/` 文档
- 若某个主题进入真实实现，先更新本页状态，再开始代码工作
- 若真实工作区回归发现需要调整或重构，先更新本页的状态、当前目标或下一步，再进入较大改动
- 若问题已经上升为行为规范、状态模型或验证基线变化，不能只更新本页，必须同步更新对应 `spec/` 或 `design/` 文档
