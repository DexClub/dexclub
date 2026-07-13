# DexClub V4 架构

## 目标

本稿只回答四件事：

- 当前代码应如何按层次理解
- 各层分别负责什么
- 依赖方向应该如何约束
- 如果后续整理模块，应该遵守什么原则

## 当前分层

`v4` 推荐按四层理解：

1. 绑定层
2. 领域层
3. 应用层
4. 入口适配层

## 当前结构映射

当前仓库更适合先按下面这组真实结构理解：

- 根工程子项目
  - `domain-core`
    - 领域模型、能力边界与底层实现
  - `app-service`
    - 应用层
  - `cli-app`
    - CLI 入口适配层
  - `mcp-app`
    - MCP 入口适配层
- 独立组合构建
  - `dexkit-binding`
    - 绑定层

`gui` 只保留为未来兼容方向，当前没有代码模块。

## 各层职责

### 绑定层

绑定层负责对接第三方分析引擎或 native 能力。

绑定层负责：

- 桥接第三方 API
- 处理平台加载问题
- 输出适合上层消费的低层能力接口

绑定层不负责：

- 工作区
- 用例
- 命令
- tool schema
- GUI 状态

### 领域层

领域层负责定义稳定模型和规则。

包括：

- workspace / target / snapshot 相关正式模型
- dex / resource 请求与结果模型
- 能力边界与约束
- 与入口无关的基础规则

领域层负责回答“是什么”。

领域层不负责回答“怎么调度”和“怎么展示”。

### 应用层

应用层负责：

- 组织正式用例入口
- 管理 workspace / session / handle / dex context 生命周期
- 管理缓存复用、长任务、进度、取消
- 统一 `cli / mcp` 共享的执行语义，并为未来 GUI 预留统一接入点

当前代码里，`app-service` 已经承担了这层职责。

### 入口适配层

入口适配层包括：

- `cli`
- `mcp`
- 未来若存在的 GUI 入口

它们当前主要负责：

- 把入口输入转成应用层请求
- 把应用层结果投影成入口可消费的格式
- 处理入口专属交互
- 启动或接收 `app-service` 暴露的 runtime 入口

它们不负责：

- 复制业务规则
- 自定义资源生命周期
- 各自维护一套核心执行流

## 推荐依赖方向

```text
cli-app(adapter) -> app-service runtime / use cases
cli-app(adapter) -> domain-core api models / errors

mcp-app(adapter) -> app-service runtime / use cases
mcp-app(adapter) -> domain-core api models / errors

app-service -> domain-core api / bootstrap
domain-core impl -> dexkit-binding
```

当前还需要单独说明：

- `AppRuntime / SessionAppRuntime` 已经把共享组合根收进 `app-service`
- `createDefaultAppServices()` 已经成为上层消费的默认装配入口，底层实现仍由 `domain-core` 提供
- `cli-app` 与 `mcp-app` 仍直接依赖 `domain-core` 里的稳定模型、错误类型与能力约束
- 这意味着当前入口层已经不再持有共享组合根，但也还没有完全脱离 `domain-core`

更重要的是反向约束：

- `app-service` 不依赖 CLI 参数、MCP schema 或渲染细节
- `domain-core` 的稳定模型与实现基础不依赖入口层
- `dexkit-binding` 不依赖 workspace、session 或入口协议

## 为什么需要应用层

如果没有应用层，就会出现下面几种退化：

1. `cli` 和 `mcp` 各自保存一套执行前后规则
2. session、handle、context cache 只能挂在某个入口里
3. GUI 为了支持后台任务和缓存复用，不得不重新做一套桌面运行时
4. 新入口接入成本取决于对现有入口实现细节的复制程度

应用层存在的意义，就是把这些原本散落在入口里的运行时职责正式收回。

## `v4` 目标结构

`v4` 的结构口径按下面这套表达：

- `cli-app`
- `mcp-app`
- `app-service`
- `domain-core`
- `dexkit-binding`
- `gui-app`

其中要区分两类：

- `cli-app / mcp-app / app-service / domain-core`
  - 当前根工程里的活跃子项目
- `dexkit-binding`
  - 当前通过 `includeBuild` 接入的绑定工程

`gui-app` 只保留为未来兼容方向，当前没有代码模块。

更具体的目标与当前映射见 [module-decision.md](./module-decision.md)。

## 稳定维护期关注点

下面这些点仍可继续讨论，但不再阻止把 `v4` 视为已完成：

- `workspace` 是否仍是三个入口共享的统一执行上下文
- `TargetSessionRuntime` 是否继续上升为 `cli / mcp` 共用的宿主协调入口
