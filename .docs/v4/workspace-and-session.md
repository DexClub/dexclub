# DexClub V4 Workspace 与 Session

## 目标

本稿只回答下面几个问题：

- `workspace` 是什么
- `session` 是什么
- `handle` 是什么
- `dex context` 是什么
- 这几个概念之间是什么关系
- 哪些是应用层正式概念，哪些只是入口层表现

## 先给结论

先把这几个概念拆开理解：

- `workspace`
  - 正式领域概念
- `target session`
  - 正式应用层概念
- `handle`
  - 依赖入口形态的可选应用层能力
- `dex context`
  - 应用层持有的运行时资源

最重要的结论是：

- 不要把 `workspace` 和 `session` 混成一个东西
- 不要把 `handle` 当成领域对象
- 不要把 `dex context` 暴露成入口层主模型

## Workspace

### 定义

`workspace` 是用户可感知的正式执行上下文。

它负责承接：

- `.dexclub` 状态根
- 当前 active target
- target snapshot
- 当前 workdir 绑定关系

### 性质

`workspace` 具有下面几个特征：

- 可以持久化
- 可以显式打开
- 可以显式刷新
- 可以被多个入口共同理解
- 不依赖某个具体协议

### 结论

`workspace` 应继续保留为 `v4` 的正式概念。

原因很简单：

- 它已经是当前产品的统一执行上下文
- `cli` 和 `mcp` 都天然需要它
- 未来若接入 GUI，也不需要重新发明另一套目标绑定模型

## Target Session

### 定义

`target session` 不是工作区本身。

它表示：

- 在某个 `workspace` 之上打开的一段运行期会话
- 这段会话允许持有可复用的运行时资源
- 这段会话可以关联临时引用、缓存和上下文

### 为什么需要

`workspace` 解决的是正式执行上下文问题。

但 `workspace` 不负责表达：

- 谁正在使用这次运行时
- 当前运行时资源是否应继续复用
- 某些临时引用何时失效
- 某些缓存何时可以集中释放

这些都更像 `session` 的职责。

### 性质

`target session` 应具备下面几个特征：

- 依附于某个 `workspace`
- 可创建、可关闭、可过期
- 可持有运行时资源
- 不要求持久化到 `.dexclub`
- 不要求对所有入口都暴露相同表现形式

### 结论

`target session` 应成为应用层正式概念。

但它不一定要求所有入口都显式暴露“session”这个词。

例如：

- `mcp` 可能显式暴露 session id
- `cli` 可能完全走短生命周期无状态调用

这不影响应用层内部仍然使用统一 session 模型组织运行时。

## Handle

### 定义

`handle` 是某个运行期对象的稳定引用令牌。

它通常用于：

- 在一次会话内引用 class
- 在一次会话内引用 method
- 避免调用方反复传完整 descriptor 和 source locator

### 性质

`handle` 不是领域对象。

它更接近一种运行期引用机制。

它具备下面几个特征：

- 依赖 session 存在
- 具有失效条件
- 只在某个作用域内有效
- 更偏向入口友好的引用形式

### 结论

`handle` 不应进入稳定领域核心模型。

当前更合适的理解是：

- 它属于应用层能力
- 某些入口可能显式暴露它
- 某些入口可能完全不需要它

也就是说：

- `handle` 是正式能力，但不是所有入口都必须看见的正式概念

## Dex Context

### 定义

`dex context` 表示一组已打开、可复用的 dex 分析运行时资源。

它可能包含：

- DexKit bridge
- 已解析的 source 集
- 与 target 绑定的缓存上下文

### 性质

`dex context` 不是领域模型，也不是入口对象。

它本质上是应用层运行时资源。

它具备下面几个特征：

- 创建成本高
- 可复用
- 有明确释放时机
- 与 target / snapshot / fingerprint 紧密相关

### 结论

`dex context` 应由应用层持有和管理。

入口层不应：

- 直接创建
- 直接缓存
- 直接决定释放规则

绑定层也不应持有“会话级别的资源生命周期主规则”。

## 四者关系

按下面关系理解：

```text
workspace
  -> 表达正式执行上下文

target session
  -> 依附于 workspace
  -> 表达一段运行期会话

handle
  -> 依附于 session
  -> 表达某个运行期对象的可复用引用

dex context
  -> 由应用层按 workspace / target / fingerprint / session 等规则持有
  -> 为 session 或执行过程提供底层运行时资源
```

这里故意不把 `dex context` 简单写死为“只依附 session”。

因为它的真实作用域还需要继续讨论。

## 作用域判断

### Workspace 作用域

适合放在 `workspace` 作用域里的东西：

- `.dexclub` 状态
- active target
- snapshot
- capability 相关正式状态

### Session 作用域

适合放在 `session` 作用域里的东西：

- handle
- 某些临时引用
- 某些运行期缓存
- 某些集中释放点

### 非正式公开作用域

不适合直接公开成领域作用域的东西：

- DexKit bridge
- 内部 context cache
- 绑定层细节对象

## 哪些概念对哪些入口可见

可见范围如下：

### 对 `cli`

`cli` 默认只显式消费：

- workspace
- 用例 request / result

`cli` 不一定需要显式暴露：

- session
- handle
- dex context

### 对 `mcp`

`mcp` 当前更可能显式消费：

- workspace
- session
- handle

但它仍不应直接消费：

- dex context 内部对象

### 对未来 GUI

GUI 未来若接入，更可能需要：

- workspace
- session

是否需要显式暴露 `handle`，到时再看具体交互模型决定。

## 失效与释放

先按下面原则理解：

### Workspace

`workspace` 不因为一次调用完成就失效。

它的失效更像：

- workdir 不再可用
- 状态损坏
- target 被切换或刷新后上下文变化

### Session

`session` 可以因为下面原因失效：

- 主动关闭
- 超时过期
- 显式刷新后被替换
- 宿主进程重启

### Handle

`handle` 应至少在下面情况失效：

- 对应 session 失效
- 对应引用对象已不再属于当前会话上下文
- 上下文刷新导致引用基础变化

### Dex Context

`dex context` 应至少在下面情况考虑释放或重建：

- 作用域关闭
- snapshot / fingerprint 变化
- 显式清理缓存
- 资源压力或宿主关闭

## 仍待讨论

下面这些点还需要继续讨论：

- session 是否对 `cli` 也应成为显式概念
- handle 是否只保留在 `mcp` 这类入口
- dex context 的复用主键到底是什么
- dex context 的作用域更接近 target 还是 session
- refresh target 时，session 是原地更新还是重建
