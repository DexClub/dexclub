# DexClub V4 第一批代码切入点

> 本稿保留的是 `v4` 第一批切入点的阶段记录。
> 它主要解释“为什么当时这样切”，不是当前仓库的主执行清单。
> 当前正式边界应优先看 [index.md](./index.md)、[stabilization.md](./stabilization.md) 和 [completion.md](./completion.md)。

## 目标

本稿只回答一个问题：

`从当时的早期状态看，v4 第一批代码最值得从哪里切`

这里不讨论最终全量迁移，只讨论第一批最值得动的部分。

## 为什么先写这篇

当时之所以需要这篇，是因为前面的文档虽然已经把下面这些事情说清了：

- 目标结构是什么
- 为什么需要应用层
- 为什么要先回收 `mcp` 里的运行时职责
- 迁移顺序应该怎样排

但如果没有代码切入点，这些结论还停留在原则层。

所以这篇文档当时的任务，是把原则收成：

- 先动哪里
- 后动哪里
- 哪些文件最适合作为第一批切口
- 哪些动作现在不要做

## 当前回看

这篇文档最初记录的是“第一批该从哪里切”。

结合当前代码，它描述的第一批主线已经基本走通：

- 应用层骨架已经出现
- `mcp` 的高频 dex / resource 链路已经开始走 use case
- `mcp` 已经形成较稳定的请求解析与结果投影边界

所以这篇现在更适合被理解成：

- 第一批为什么这样切
- 现在回看，这条切法验证了什么

## 第一批迁移目标

第一批迁移当时只聚焦一件大事：

`把 mcp 中最核心的共享运行时职责回收到正式应用层`

这里的“最核心”当前主要指：

- target session
- handle
- dex context lease / release
- 一部分执行上下文恢复

## 为什么从 `mcp` 开始

因为当时最明显的运行时逻辑就在 `mcp`：

- [TargetSessionService.kt](../../app-service/src/main/kotlin/io/github/dexclub/core/app/session/TargetSessionService.kt:1)
- [McpExecutionSupport.kt](../../mcp-app/src/main/kotlin/io/github/dexclub/mcp/McpExecutionSupport.kt:1)
- [McpApp.kt](../../mcp-app/src/main/kotlin/io/github/dexclub/mcp/McpApp.kt:1)
- [DexContextRegistry.kt](../../app-service/src/main/kotlin/io/github/dexclub/core/app/session/DexContextRegistry.kt:1)

这些逻辑共同说明了一件事：

- `mcp` 当时不只是协议适配层
- 它还在承担共享运行时

所以从这里切，回收价值最高，边界也最容易看清。

## 第一批不要做什么

按当时的风险控制，第一批不建议做下面这些事：

- 不改模块名
- 不大搬目录
- 不全面重写 `cli`
- 不先动 resource 全链路
- 不先改 README
- 不试图一次性做完应用层所有用例

第一批只需要证明一件事：

- 共享运行时职责可以从 `mcp` 收回应用层，而且行为不变

## 第一批切法

这条主线当时拆成四个小步。

## 第一步：先抽应用层骨架

### 目标

在当时的现有结构里先长出一个正式应用层入口，不要求一步到位新模块化。

### 做法

当时可接受的过渡做法，是先在共享层或新目录中引入一个过渡层，例如：

- `core/app`
- `core/usecase`
- `core/runtime`

这些例子只代表当时的过渡切法，不代表当前仓库仍应按这组目录组织。

重点不是名字，而是先有明确边界。

### 第一批至少要有的对象

第一批应先出现下面这类对象：

- `TargetSessionService` 或同等语义入口
- `ExecutionContextResolver` 或同等语义入口
- `DexContextLeaseService` 或同等语义入口

### 当前结果

这一小步已经成立。

当前已经能看到：

- `app-service/dex`
- `app-service/resource`
- `app-service/session`

应用层不再只是空壳。

## 第二步：先迁 session 主规则

### 目标

先把 session 的主生命周期从 `mcp` 内部拿出来。

### 当前切点

优先看：

- [TargetSessionService.kt](../../app-service/src/main/kotlin/io/github/dexclub/core/app/session/TargetSessionService.kt:1)
- [McpApp.kt](../../mcp-app/src/main/kotlin/io/github/dexclub/mcp/McpApp.kt:1)

### 先收什么

先收这些规则：

- open target session
- get / list / close target session
- refresh target session
- session idle timeout
- session overflow eviction

### 暂时可以留在 `mcp` 的东西

- MCP tool 名称
- 返回 JSON 结构
- 协议层错误文案

### 当前结果

从现在回看，这一步更准确地说是“主规则已经收口，但桥接仍未完全消失”：

- 共享 session 相关解析与恢复逻辑已经开始下沉
- 但 `TargetSessionService` 仍然是当前 `mcp` 消费的宿主态组件

## 第三步：再迁 handle 主规则

### 目标

把 handle 从“`mcp` 私有技巧”提升为“应用层可选能力”。

### 当前切点

优先看：

- [TargetSessionService.kt](../../app-service/src/main/kotlin/io/github/dexclub/core/app/session/TargetSessionService.kt:1)
- [McpExecutionSupport.kt](../../mcp-app/src/main/kotlin/io/github/dexclub/mcp/McpExecutionSupport.kt:1)

### 先收什么

先收这些规则：

- method handle put / get
- class handle put / get
- handle 与 session 的绑定关系
- handle 失效条件
- 每 session handle 数量上限

### 这一步的关键判断

这一步不是让所有入口都显式暴露 handle。

而是先把下面这件事做对：

- handle 的主规则属于应用层，不属于协议层

### 当前结果

从现在回看，这一步也已明显推进：

- handle 使用已经收敛到 use case + 结果投影链路
- 但其最终所有权仍和 session store 的归属绑定在一起，还不是最终形态

## 第四步：再迁 dex context 主规则

### 目标

把 dex context 的主持有、复用和释放规则从 `mcp` 收走。

### 当前切点

优先看：

- [DexContextRegistry.kt](../../app-service/src/main/kotlin/io/github/dexclub/core/app/session/DexContextRegistry.kt:1)
- [McpApp.kt](../../mcp-app/src/main/kotlin/io/github/dexclub/mcp/McpApp.kt:1)
- [McpExecutionSupport.kt](../../mcp-app/src/main/kotlin/io/github/dexclub/mcp/McpExecutionSupport.kt:1)
- [DefaultDexSearchExecutor.kt](../../domain-core/src/jvmMain/kotlin/io/github/dexclub/core/impl/dex/DefaultDexSearchExecutor.kt:1)

### 先收什么

先收这些规则：

- 会话关联的 dex context retain / release
- workdir 调用场景下的 context lease
- session 回收时的 context 清理
- 宿主关闭时的集中释放

### 这一步要注意

不要把 `DefaultDexSearchExecutor` 里的内部 cache 直接暴露给入口层。

这一步真正要做的是：

- 统一谁来决定何时申请 lease
- 统一谁来决定何时释放
- 统一入口如何消费这套规则

### 当前结果

从现在回看，这一步同样仍属于过渡完成：

- 主要执行链路已经不再把 dex context 规则散落进 tool 实现
- 但 `DexContextRegistry` 仍作为当前 `mcp` 消费的桥接态实现

## 第一批代码切入顺序

按下面顺序落：

1. 先长应用层骨架
2. 先迁 session
3. 再迁 handle
4. 再迁 dex context
5. 最后才让具体 tool handlers 全量切到新入口

这个顺序的好处是：

- 风险集中
- 行为容易对照
- 每一步都能单独验证

## 第一批建议暂不触碰的文件

如果目标是降低风险，第一批尽量不要先碰下面这些区域：

- `cli` parser 大面积代码
- renderer / output model
- resource 相关 executor
- vendored `DexKit`
- `libcxx-prefab`

这些区域要么不构成当前主要问题，要么改动成本明显更高。

## 第一批验证建议

第一批每个小步完成后，至少应验证：

- `:app-service:testSession`
- `:mcp-app:testStructured`
- `:domain-core:testStructured`

尤其关注下面几类测试：

- session 生命周期
- dex tool 调用
- `inspect_method`
- workdir 回退调用

重点不是多跑，而是确认：

- 行为没有漂移
- 规则只是换位置，没有顺手改语义

## 第一批完成后的状态

第一批完成后，当时并不要求项目已经长成最终 `v4` 结构。

从现在回看，第一批至少已经证明了下面几件事：

- `mcp` 明显变薄
- 应用层开始真实承接共享运行时
- session / handle / dex context 已经不再被视为协议层私有逻辑
- 后续高频 dex use case 能建立在这层新边界上继续迁移

## 当前阅读方式

如果今天再读这篇，更适合把它理解成：

- 为什么第一批先从 `mcp` 运行时切
- 为什么应用层先于模块重整
- 为什么 session / handle / dex context 被当成第一优先级

如果要判断今天的 `v4` 还差什么，不应再把这里的“四步切法”直接当成当前待办，而应优先看：

- [stabilization.md](./stabilization.md)
- [completion.md](./completion.md)
- [module-decision.md](./module-decision.md)

## 仍待讨论

- `TargetSessionService / DexContextRegistry` 的最终归属
- `cli` 是否开始全面走同一批 use case
