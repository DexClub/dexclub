# DexClub V4 模块结构结论

## 目标

本稿只回答一件事：

`v4` 现在采用什么模块结构

## 结论

`v4` 当前采用下面这套结构口径：

- `cli-app`
- `mcp-app`
- `app-service`
- `domain-core`
- `dexkit-binding`
- `gui-app`

其中要分清两类：

- 前四个是根工程中的活跃子项目
- `dexkit-binding` 是通过 `includeBuild` 接入的独立绑定工程
- `gui-app` 是未来兼容方向，当前不实现

## 当前活跃模块

按目前仓库真实状态，当前结构要分成两层看：

- 根工程子项目
  - `cli-app`
    - CLI 入口与交互适配层
  - `mcp-app`
    - MCP server 入口与协议适配层
  - `app-service`
    - 共享应用层，用来承接 use case、runtime 协调与入口共享执行语义
  - `domain-core`
    - 稳定模型、能力边界与底层实现基础
- 独立组合构建
  - `dexkit-binding`
    - DexKit / native 绑定层

这组结构已经不是“目标映射”，而是当前代码中的正式结构。

## `gui-app` 的位置

`gui-app` 继续保留，但定位必须说清楚：

- 它是 `v4` 的未来兼容边界
- 它不是当前实现项
- 当前文档可以讨论它的接入约束，但不进入实现细节

## 这套结构的含义

这套结构表达的是下面几个边界：

- `cli-app / mcp-app` 继续只负责各自入口适配
- `app-service` 负责共享应用层编排
- `domain-core` 负责稳定能力与实现基础
- `dexkit-binding` 负责桥接 DexKit 与 native 依赖
- `gui-app` 如果未来出现，也应复用 `app-service` 和 `domain-core`

## 对当前仓库的约束

从现在开始，相关说明都应统一按下面两层写法表达：

### 当前实现

写当前代码时，使用：

- 根工程子项目：`cli-app / mcp-app / app-service / domain-core`
- 独立绑定工程：`dexkit-binding`

### 未来兼容

写未来方向时，只额外补：

- `gui-app`

## 当前共识

这份结论落定后，下面这些判断不再摇摆：

- 活跃模块结构已经对齐到 `cli-app / mcp-app / app-service / domain-core / dexkit-binding`
- 但文档表达需要区分“根工程子项目”和“组合构建”
- `dexkit-binding` 作为绑定层命名已经成立
- `gui-app` 继续只作为未来方向保留
- 后续讨论重点不再是“模块名改不改”，而是“当前边界是否已经稳定到足以宣布 `v4` 完成”
