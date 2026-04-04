# code-view 文档索引

## 目的

本目录用于承载 `code-view` 的长期文档体系。

目标不是继续累积“按时间追加”的开发笔记，而是整理出一套可持续维护的组件手册，使读者能够：

- 快速理解 `code-view` 的整体边界与模块结构
- 按主题阅读设计文档，而不是在旧文档里来回跳转
- 通过规范文档理解公开行为、状态模型与验收标准
- 在保留历史信息的前提下，逐步替换旧的重构流水账文档

## 阅读顺序

建议首次阅读顺序：

1. [overview.md](overview.md)
2. [module-map.md](module-map.md)
3. [status.md](status.md)
4. `design/` 下对应主题文档
5. `spec/` 下规范文档

## 目录结构

### 总览层

- [overview.md](overview.md)
  说明 `code-view` 组件家族提供什么能力、当前边界是什么。
- [module-map.md](module-map.md)
  说明各模块职责、依赖方向与主数据流。
- [status.md](status.md)
  作为新的总进度总表，统一记录 `已计划 / 进行中 / 已完成 / 待开始 / 阻塞 / 已废弃`。

### 设计层

- [design/layout-viewport.md](design/layout-viewport.md)
- [design/viewer-rendering.md](design/viewer-rendering.md)
- [design/selection-caret.md](design/selection-caret.md)
- [design/editor-input-ime.md](design/editor-input-ime.md)
- [design/context-actions.md](design/context-actions.md)
- [design/performance-regression.md](design/performance-regression.md)

### 规范层

- [spec/api-spec.md](spec/api-spec.md)
- [spec/behavior-spec.md](spec/behavior-spec.md)
- [spec/state-model-spec.md](spec/state-model-spec.md)
- [spec/test-matrix.md](spec/test-matrix.md)

## 迁移说明

旧的大一统设计文档、进度文档和规则文档内容已经迁入当前体系。

当前策略：

- `docs/` 是后续唯一主入口
- 新增设计、状态和规范时，优先写入本目录
- 不再新增新的根目录重构流水账文档

## 维护规则

- 设计与进度分离，不再在同一篇文档中混写
- 一份文档只回答一类问题
- 新增主题时优先判断应归入现有哪一册，而不是直接新增根目录文档
- 状态变化优先更新 [status.md](status.md)
- 设计决议变化优先更新对应 `design/` 文档
