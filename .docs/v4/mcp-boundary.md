# DexClub V4 MCP 当前边界

## 目标

本稿只回答一个问题：

`结合当前代码，mcp 这一层现在到底收敛到了什么状态`

这里不讨论最终理想结构，只记录已经落地的边界、仍然保留的过渡实现，以及当前不建议继续机械细拆的判断。

## 当前状态概览

经过前几轮收口，`mcp` 现在已经明显比最初更薄，但还没有彻底退化成“只剩 schema 的空壳”。

当前可以把它理解成：

- 一个正式入口层
- 一个结果投影层
- 一个请求解析层
- 一个过渡期运行时桥接层

其中前面三部分已经相对稳定，最后一部分仍然带有过渡性质。

## 已经稳定下来的边界

### 1. tool 目录与注册

当前 `mcp` 已经把工具目录和注册入口拆开：

- `McpSessionToolCatalog`
- `McpDexToolCatalog`
- `McpResourceToolCatalog`
- `McpToolMetadata`

这部分的职责现在比较清楚：

- 组织 tool 名称
- 组织 schema
- 暴露协议层输入属性

它们不再适合继续回流成“大而全的工具实现文件”。

### 2. 请求解析 support

当前 `mcp` 已经把工具请求解析从 tool handler 主流程里拆出：

- `McpExecutionSupport`
- `McpRequestParsers`
- `McpToolRequestSupport`

当前这些 support 主要负责：

- `session_id / workdir` 读取
- `brief / fields / include / include_text` 读取
- 常见必填参数校验
- 字段投影参数校验
- MCP 请求到基础 Kotlin 值的转换

这部分已经属于稳定的入口适配职责。

### 3. 结果模型与结果映射

当前 `mcp` 的结果输出已经明显分层：

- `McpEnvelopeModels`
- `McpViewModels`
- `McpViewMappings`
- `McpResultMappings`
- `McpProjectionSupport`

这意味着：

- 应用层结果不直接等于 MCP 返回结构
- MCP 结果投影已经成为独立边界
- `brief / fields` 这类协议层裁剪能力有固定归属

这部分不建议再继续拆得更碎。

### 4. 错误与响应包装

当前 `mcp` 已经把成功响应和常见错误转换集中到：

- `McpToolResponseSupport`
- `McpExecutionSupport`

现在工具实现已经不再到处散落：

- `try/catch IllegalArgumentException`
- 手写 `"xxx is required"`
- 手写 JSON error 包装

这一层已经接近稳定形态。

## 已经下沉到应用层的内容

当前已经实际落到 `app-service` 的内容，至少包括下面几类：

- dex 查询与导出相关 use case
- resource 查询与解析相关 use case
- `TargetWorkspaceResolver`
- `TargetSessionRuntime`
- 一部分共享 support

这意味着 `mcp` 当前已经不再直接拼装完整 dex / resource 执行语义。

更准确地说：

- `mcp` 负责协议输入
- `app-service` 负责用例执行
- `mcp` 再把结果投影回协议输出

这条主链路已经成立。

## 仍然留在 `mcp` 的过渡实现

当前仍然还通过 `mcp` 使用、但实现已在 `app-service/session` 的内容主要有两类：

- `TargetSessionService`
- `DexContextRegistry`

它们说明一件事：

- 运行时主规则已经开始下沉
- 会话存储与 dex context registry 的具体实现已经不再定义在 `mcp`
- 但这些概念仍然主要服务当前 MCP 宿主流程

当前更适合把这几块理解成：

- `app-service/session` 中的共享运行时组件
- `mcp` 仍在消费的一部分宿主态入口
- 但不应继续扩张为新的“协议私有规则中心”

当前更关键的变化是：

- `TargetSessionRuntime` 已经把 `session store + dex context registry` 的协调逻辑，以及执行上下文恢复和 dex lease 获取，收进应用层
- `mcp` 不再自己维护 `retain / release / drainRemovedSessions` 这类桥接细节

也就是说，现状不是“`mcp` 还很重，所以继续无限细拆”，而是“主链路与宿主协调都已明显下沉，剩下的问题是这些运行时组件未来是否还要继续抽成更通用的入口共享能力”。

## 当前 `mcp` 更适合负责什么

基于当前实现，`mcp` 长期保留下面这些职责是合理的：

- tool metadata 与 schema
- 请求参数解析
- MCP 协议错误表达
- 结果投影与字段裁剪
- 协议侧诊断与调用日志

当前不应再继续往 `mcp` 增长下面这些东西：

- 新的共享运行时主规则
- 新的 dex / resource 执行编排
- 与协议无关的上下文恢复规则
- 入口无关的共享错误语义

同时，当前也不必为了“彻底去掉 `Mcp*` 命名”而强行再做一轮表层迁移。

## 为什么现在不建议继续机械细拆

如果继续顺着文件颗粒度往下拆，收益已经明显下降。

原因主要有三个：

### 1. 主边界已经形成

现在 `mcp` 内部已经能清楚区分：

- catalog
- request support
- tool handlers
- result mapping
- runtime support

继续拆，不会改变主边界，只会增加跳转成本。

### 2. 剩余问题不是“文件太大”，而是“运行时所有权还未最终定型”

现在最值得继续推进的问题不是：

- 要不要再拆一个 helper 文件

而是：

- `TargetSessionRuntime` 后续是否应成为 `cli / mcp` 共用宿主协调入口
- `mcp` 是否还需要保留额外的过渡命名包装
- 协议宿主态和应用层运行时的分界线如何定死

这类问题靠继续切小文件解决不了。

### 3. 当前已经足够支持后续入口演进

以当前边界看：

- `cli` 可以继续向应用层收口
- `mcp` 可以继续新增 tool 而不必复制大量运行时逻辑
- 未来若增加 GUI，也不必直接复制当前 `mcp` 的 tool 内部细节

所以这一轮更应该把文档和边界共识写实，而不是继续追求文件数量变化。

## 仍待讨论

- `TargetSessionRuntime` 是否继续上升为多入口共享宿主协调层
- `cli` 是否继续在 `WorkspaceRuntime` 之上收口，而不是复制 session runtime
- `session / handle / dex context` 的最终所有权是否需要继续从 MCP 命名表面收口
- `app-service` 是否已经足够稳定到可以长期冻结边界

## 结论

当前 `mcp` 的正确理解不是“还没拆完”，而是：

- 主体已经从重入口转成薄入口
- 请求解析与结果映射边界已经稳定
- 宿主协调逻辑也已经开始进入 `app-service/session`

因此，当前重点应转向多入口共享宿主协调边界，而不是继续机械细拆 `mcp`。
