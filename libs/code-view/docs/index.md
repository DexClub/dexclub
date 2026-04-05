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
- [design/in-page-search.md](design/in-page-search.md)
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
- 每次新的 `code-view` 会话，第一步先查看 [status.md](status.md)；如存在进行中主题或可能中断的迹象，必要时同步检查当前工作区
- 每次会话结束前都应回看 [status.md](status.md) 并更新当前状态，避免留下未记录的半完成状态
- 任何主题发生状态扭转时，都必须立即更新 [status.md](status.md)，不能只在对话或提交信息里说明
- 将主题标记为 `已完成` 前，应确认当前阶段目标、对应文档更新和必要验证都已落地
- 将主题标记为 `阻塞` 时，应同步记录阻塞原因、当前停点和解除条件
- 每次涉及实现、修复或回归的会话结束前，都应回写本次验证情况
- 状态变化优先更新 [status.md](status.md)
- 设计决议变化优先更新对应 `design/` 文档
- 行为契约变化优先更新 `spec/behavior-spec.md`
- 状态模型变化优先更新 `spec/state-model-spec.md`
- 验证基线、手测矩阵和记录要求变化优先更新 `spec/test-matrix.md`

## 回归问题文档落点

真实工作区回归、组件手测或自动测试发现问题后，先判断问题属于哪一类，再决定写入位置。

- “现在做到哪一步、下一步改什么”写入 [status.md](status.md)
- “组件应该表现成什么行为”写入 `spec/behavior-spec.md`
- “哪个状态是真相源、模型应如何升级”写入 `spec/state-model-spec.md`
- “准备怎么改、边界和取舍是什么”写入对应 `design/` 文档
- “以后至少要怎么测、结果要怎么记”写入 `spec/test-matrix.md`

额外规则：

- 一次性的缺陷明细优先放 PR、issue 或任务单，不直接堆进 `docs/`
- 只有当某个问题已经上升为长期规则、设计决议、验收条件或持续回归项时，才沉淀进 `docs/`
- 不允许只修代码不补文档；只要问题导致规范、设计或验证基线发生变化，必须同步更新对应文档
