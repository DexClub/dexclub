# DexClub V4 完成标准

## 目标

本稿只回答三件事：

- `v4` 什么时候才算完成
- 当前仓库为什么可以按完成状态理解
- 进入稳定维护期后还要盯什么

这是一份完成判定文档，不替代 [migration.md](./migration.md) 的迁移背景，也不替代 [checklist.md](./checklist.md) 的边界自查。

## 当前结论

按当前仓库真实状态，当前更准确的判断是：

- `v4` 已完成
- 当前阶段进入稳定维护期，而不是继续保留迁移时态

这里的“已完成”指的是：

- 活跃模块结构已经稳定
- 共享运行时边界已经收口
- README、`.docs/v4/`、Gradle 与 CI 已经按当前结构表达
- 后续重点应转向稳定维护、验证和功能演进，而不是继续把模块迁移当主线

## 什么才算完成

只有下面几类条件同时成立，才适合把当前状态描述成“`v4` 已完成”。

### 1. 模块结构已经稳定

需要满足：

- 根工程子项目固定为 `cli-app / mcp-app / app-service / domain-core`
- `dexkit-binding` 固定为通过 `includeBuild` 接入的独立绑定工程
- `gui-app` 明确保留为未来兼容边界，而不是当前实现项
- 代码、Gradle、README、CI 与打包路径都按这套结构表达

### 2. 入口层已经收口

需要满足：

- `cli-app` 主要负责参数解析、命令分发、结果渲染与退出码
- `mcp-app` 主要负责 tool schema、请求解析、结果投影与协议层错误表达
- 新的共享执行语义不再继续长在入口层

### 3. 应用层边界已经稳定

需要满足：

- `app-service` 已经成为 `cli-app / mcp-app` 的正式共享用例入口
- `workspace / session / handle / dex context` 的归属已经清楚
- 默认装配入口已经以 `app-service` 为上层消费入口收口
- 高频 dex / resource / workspace 链路已经围绕正式 use case 收口

### 4. 基础层口径已经稳定

需要满足：

- `domain-core` 的职责已经清楚表达为稳定模型、能力边界与底层实现基础
- `dexkit-binding` 的职责已经清楚表达为 DexKit / native 绑定层
- native / vendor 复杂度没有继续泄漏到 `app-service`
- `cli-app / mcp-app` 的运行逻辑没有继续扩张到直接承载底层实现细节

### 5. 文档与交付已经收口

需要满足：

- README、`.docs/v4/`、workflow、打包路径与运行前提相互一致
- 文档能明确区分“当前事实”和“未来方向”
- 不再把“迁移过程”写成“当前状态”

## 当前已经成立的部分

按上面的标准看，下面这些事实已经成立：

- `app-service` 已经作为正式模块落地
- `domain-core` 模块名已经落地
- `cli-app` 模块名已经落地
- `mcp-app` 模块名已经落地
- `dexkit-binding` 已经以独立绑定工程形态落地
- README、workflow、native 维护文档已经切到当前模块结构
- `AppRuntime / SessionAppRuntime` 已经成为共享组合根入口
- `AppUseCases` 已经成为共享装配入口
- `WorkspaceRuntime / TargetSessionRuntime` 已开始承接共享宿主协调职责
- `createDefaultAppServices()` 已经成为上层消费的默认装配入口
- `cli-app` 现有命令适配链路已经通过 `app-service` 复用主要 use case
- `mcp-app` 已经围绕 `catalog / request parsing / result mapping / runtime bridge` 形成较稳定边界

## 为什么当前可以宣布完成

当前仍然存在的，不是“必须继续推进的结构改动”，而是稳定维护期内的正常约束：

- `domain-core` 继续承载稳定模型、能力边界与实现基础
- `app-service` 继续承载默认装配入口、共享 runtime 与 use case
- `cli-app / mcp-app` 仍可直接依赖部分 `domain-core` 稳定模型与错误类型
- `cli-app` 打包仍直接消费 DexKit vendor/native 产物

这些点不再视为 `v4` 未完成的理由，而是当前正式边界的一部分。

## 建议的判定方式

当前更适合用下面三问持续检查稳定维护期，而不是继续做迁移判定：

1. 这次改动是否把共享运行时职责重新散回入口层
2. 这次改动是否让 `app-service / domain-core` 的边界变得更模糊
3. 这次改动是否让普通验证路径变重或让文档和代码重新分叉

## 当前建议

基于目前状态，当前最合理的动作是：

1. 维持当前边界，不再把模块迁移当主线
2. 优先使用快速验证基线，例如 `./gradlew verifyFast`
3. 在边界相关改动后再使用结构化验证基线兜底
