# DexClub V4 入口适配层

## 目标

本稿只回答两个问题：

- `cli` 和 `mcp` 各自只负责什么
- 哪些逻辑不能继续留在入口层

这是一份当前状态说明，不是迁移草案；当前稳定边界见 [stabilization.md](./stabilization.md)。

## 总原则

入口层的职责只有一个：

`把入口输入转换成应用层请求，再把应用层结果转换成入口输出`

凡是超出这个范围的共享执行逻辑，都应优先检查是否应该回收进应用层。

## CLI 适配层

### CLI 负责什么

`cli` 负责：

- argv 解析
- 命令与子命令路由
- query file 读取
- 输出格式选择
- text / json 渲染
- stderr / stdout 分流
- 退出码

### CLI 不负责什么

`cli` 不负责：

- 持有正式运行时状态
- 自己维护 session 规则
- 自己维护 dex context 生命周期
- 复制分析前置规则
- 组合多个共享 service 才能完成一次正常命令

### CLI 允许保留的本地逻辑

下面这些逻辑留在 `cli` 是合理的：

- `--query-json / --query-file` 这种 CLI 专属输入形态
- help 文本
- 输出排序中的展示性细节
- 命令别名或帮助命令的组织方式

当前代码里，`cli` 已经把共享宿主协调从 adapter 中抽走：

- `CliTargetWorkspaceRuntime`
- `app-service/session/WorkspaceRuntime`
- `app-service/AppUseCases`

这说明 `cli` 当前更适合：

- 复用无会话 runtime 入口
- 复用共享的 app use case 装配入口
- 暂不强行暴露 session 模型

同时，`cli` 的业务链路也已经接入 `app-service`：

- 已经接入的一批低风险链路
  - `inspect-method`
  - `manifest`
  - `decode-xml`
  - `list-res`
  - `get-res-value`
  - `find-res-values`
  - `find-class`
  - `find-method`
  - `find-field`
  - `find-class-using-strings`
  - `find-method-using-strings`
- 暂时仍保留直连 `services.*` 的链路
  - 无

当前这一轮已经把 `cli` 的主要业务命令都收口到 `app-service`，入口层现在更接近：

- 只负责命令解析、路由和渲染
- 不再直接拼 `services.*`

### CLI 不应再增长的逻辑

下面这些逻辑若继续长在 `cli`，应视为退化：

- workspace / session 恢复细节
- 共享错误分类规则
- 共享缓存策略
- 共享能力前置校验

尤其不应再让每个 `cli` adapter 各自重复：

- `resolve workdir`
- `open workspace`
- 再进入共享能力调用

## MCP 适配层

### MCP 负责什么

`mcp` 负责：

- tool metadata
- schema 定义
- tool 调用参数解析
- 面向 agent 的结构化结果投影
- 协议层错误返回

### MCP 不负责什么

`mcp` 不负责：

- 长期持有共享运行时规则副本
- 自己定义一套独立的上下文生命周期
- 复制 dex / resource 分析执行语义
- 把协议层对象当作内部核心对象

### MCP 允许保留的本地逻辑

下面这些逻辑留在 `mcp` 是合理的：

- 字段投影
- brief / fields 这类协议层裁剪选项
- tool 名称与 schema 组织
- HTTP 或 MCP 协议相关诊断
- 参数摘要、调用日志与协议侧观测

当前代码里，这些职责已经基本形成固定归属，例如：

- `Mcp*ToolCatalog`
- `McpExecutionSupport`
- `McpRequestParsers`
- `McpToolRequestSupport`
- `McpProjectionSupport`
- `McpViewMappings / McpResultMappings`
- `McpToolResponseSupport`

### MCP 不应再增长的逻辑

下面这些逻辑若继续长在 `mcp`，应优先判断是否应下沉：

- session 失效主规则
- handle 生命周期主规则
- dex context 持有与释放主规则
- 非协议层的执行上下文恢复

但需要注意一件事：

当前 `mcp` 里已经不再保留 `McpSessionStore` 与 `McpDexContextRegistry` 的过渡别名，直接使用 `TargetSessionService` 与 `DexContextRegistry`。

它们现在更适合被理解为：

- 过渡期入口宿主态
- 协议侧运行时桥接

而不是继续扩张成新的适配层主规则中心。

## 共享逻辑回收标准

判断一个逻辑是否应该回收到应用层，可以用下面几个问题：

1. 这个逻辑是否同时对 `cli` 和 `mcp` 成立
2. 这个逻辑是否与协议、命令行参数或渲染无关
3. 这个逻辑是否涉及上下文、缓存、生命周期或错误语义
4. 如果未来增加第三个入口，是否还要再实现一遍

如果四个问题里大部分答案是“是”，这个逻辑就不该继续留在入口层。

## 收口判断

按下面方向收口：

- `cli` 继续做纯命令适配
- `mcp` 继续做纯协议适配
- session / handle / dex context / cache 的主规则回收到应用层
- 入口层只保留各自不可共享的表示层逻辑

对 `cli` 来说，当前这句话更准确地应理解为：

- 命令路由仍留在 `cli`
- 无会话的 workspace 宿主协调已经开始下沉
- 暂时不必为了对齐 `mcp` 而硬引入显式 session 模型
- 已有现成 use case 的链路优先接入
- 其余链路继续按当前边界保持一致

对 `mcp` 来说，当前这句话更准确地应理解为：

- 主执行语义已经开始下沉
- 协议层 support 已经稳定
- 剩余运行时桥接后续再按所有权继续处理

## 与未来 GUI 的关系

`v4` 当前不实现 GUI。

但入口适配层现在就应避免继续固化一种前提：

- 只有命令行和 tool 调用两种入口

如果 `cli` 和 `mcp` 都足够薄，那么未来 GUI 只是再接一个新的适配层。

如果 `cli` 和 `mcp` 继续各自持有运行时逻辑，那么未来 GUI 就只能复制已有入口的隐性规则。

## 仍待讨论

下面这些点还可以继续讨论：

- workspace / session / handle / dex context 的正式关系
- `TargetSessionRuntime` 是否继续上升为 `cli / mcp` 共用宿主协调入口
