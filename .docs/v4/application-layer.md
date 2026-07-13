# DexClub V4 应用层

## 目标

本稿只回答下面几个问题：

- 为什么 `v4` 必须补应用层
- 应用层到底承接哪些职责
- 哪些职责必须从 `cli` 和 `mcp` 收回
- 应用层的接口形态应如何理解

这是一份当前状态说明，不是规划草案；当前稳定边界见 [stabilization.md](./stabilization.md)。

## 为什么必须有应用层

当前问题已不是“是否需要应用层”，而是共享运行时边界还需要继续稳定。

现在已经能看到几类职责开始散落在入口中：

- `cli` 在做命令适配之外的执行编排
- `mcp` 在做协议适配之外的 session / handle / context 管理
- 默认装配和运行时组织仍然更接近“库直接给入口调用”，而不是“正式用例层”

但和最初相比，当前代码已经有一个关键变化：

- `app-service` 已经开始承接 dex / resource use case
- `TargetWorkspaceResolver` 这类共享上下文恢复逻辑已经开始从 `mcp` 中移出
- `WorkspaceRuntime` 已经开始承接无会话的 workspace 打开路径
- `TargetSessionRuntime` 已经开始承接带会话的宿主协调路径，并进一步收口执行上下文恢复与 dex lease 获取

所以现在讨论的重点是：

- 应用层已经承担了哪些职责
- 哪些桥接还留在入口层
- 哪些边界还需要继续下沉

如果继续这样演进，会出现几个结果：

- 新能力一加，`cli` 和 `mcp` 都要各写一遍入口编排
- 资源生命周期只能挂在某个入口内部
- 共享能力边界会越来越像“很多 service 方法”，而不像稳定用例
- 未来若需要第三个入口，只能复制现有入口的隐性规则

应用层的作用，就是把这些共享执行语义正式收口。

## 应用层负责什么

应用层当前应负责下面这些职责：

- 用例入口
- 执行上下文恢复
- workspace 生命周期管理
- session 生命周期管理
- handle 生命周期管理
- dex context 生命周期管理
- 缓存复用策略
- 长任务与取消边界
- 统一错误语义

它不负责：

- CLI 参数解析
- MCP schema 暴露
- 文本渲染
- HTTP 协议
- GUI 组件状态

## 应用层不是领域层

领域层负责定义稳定模型和规则。

应用层负责组织这些模型和规则进入可执行用例。

区别可以简单理解为：

- 领域层回答“这是什么、规则是什么”
- 应用层回答“这次操作怎么执行、上下文怎么恢复、结果怎么返回”

如果没有这层区分，就会把大量运行期职责继续塞进 `domain-core service` 或入口适配层。

## 应用层的核心职责

### 1. 用例入口

应用层不应继续暴露成一串只按底层能力分类的 service 方法。

它应围绕正式用例组织主入口，例如：

- 打开工作区
- 初始化工作区
- 切换 target
- 执行 dex 查询
- 检查方法详情
- 导出 class / method 产物
- 解析 manifest / resources / xml

重点不是方法名，而是：

- 用例应该完整表达执行语义
- 入口层不需要自己拼前置动作

### 2. 执行上下文恢复

应用层需要统一处理下面这些事情：

- 根据 workdir 恢复 workspace
- 根据 session id 恢复会话态
- 根据 handle 恢复被引用对象
- 根据 snapshot / fingerprint 选择复用还是重建 dex context

这些逻辑不应长期分别挂在 `cli` 和 `mcp`。

结合当前实现，这一层已经开始出现两个不同粒度的入口：

- `WorkspaceRuntime`
  - 负责无会话场景下的 `workdir -> workspace`
- `TargetSessionRuntime`
  - 负责带会话场景下的 session store、dex context registry、执行上下文恢复与宿主协调

这两个入口当前不是竞争关系，而是分阶段落地：

- `cli` 先复用 `WorkspaceRuntime`
- `mcp` 继续复用 `TargetSessionRuntime`，并让它承担更完整的执行上下文恢复职责

### 3. 生命周期管理

应用层应正式持有这些生命周期：

- workspace
- target session
- method / class handle
- dex context
- task

这不代表所有状态都必须持久化。

它表达的是：

- 谁创建
- 谁持有
- 谁释放
- 什么时候失效
- 什么时候需要显式刷新

### 4. 缓存复用

当前 dex context、物料扫描结果、导出前置上下文这类资源，都不是简单的“算完即丢”。

应用层需要统一定义：

- 哪些上下文允许复用
- 复用键是什么
- 复用的作用域是什么
- 何时主动释放
- 何时因 session 或 fingerprint 变化失效

否则入口层会各自长出缓存规则。

### 5. 长任务与取消边界

`v4` 当前不实现 GUI，但应用层设计不能默认所有操作都是瞬时、同步、无取消的。

应用层至少应预留下面这些边界：

- 哪些操作可能耗时
- 如何表达任务状态
- 如何表达取消能力
- 如何表达进度或阶段

当前即便不做完整任务系统，也不应把接口设计成彻底排斥这些能力。

## 哪些职责必须从入口收回

### 从 `cli` 收回

- 执行前上下文拼装
- 与分析能力绑定的前置校验
- 跨命令共享的运行时规则

`cli` 应保留：

- 参数解析
- help / usage
- 输出渲染
- 退出码

结合当前实现，可以进一步明确：

- `cli` 可以保留命令式路由
- 但 `workdir -> open workspace` 这类宿主协调已不应散落在各 adapter 中

### 从 `mcp` 收回

- target session 的核心生命周期规则
- handle 的核心生命周期规则
- dex context 的核心生命周期规则
- 与协议无关的共享执行语义

`mcp` 应保留：

- tool schema
- 参数解析
- 协议层结果投影
- 协议层错误表达

结合当前实现，可以进一步补一条：

- 请求裁剪选项与协议层结果裁剪规则

## 接口形态建议

应用层对外暴露的不是“万能门面”，也不是“底层 service 原样上抛”。

更合适的方向是：

- 少量正式 use case 入口
- 明确 request / result 模型
- 明确同步调用与长任务调用的边界
- 明确需要上下文的调用与无状态调用的边界

当前先不在本稿里定具体类名，但需要先定下面几个原则：

- 入口层不直接组合多个底层 service 才能完成一次正常操作
- 应用层返回的是稳定结果，不是入口专用格式
- 应用层错误不带 CLI 或 MCP 文案

## 与现有 `domain-core` 的关系

应用层不是为了替换掉 `domain-core` 里所有内容。

更准确地说：

- `domain-core` 中稳定的领域模型和基础能力可以保留
- `domain-core` 中一部分运行时组织和默认装配需要上移或重组
- 当前挂在 `mcp` 内部的一部分运行时职责，也需要回收到应用层

当前已经落地的阶段更接近下面这种状态：

- `app-service` 已经不是纯规划概念
- `mcp` 到 `app-service` 的 use case 调用链已经成立
- `cli` 已经开始通过 `WorkspaceRuntime` 复用共享 workspace 打开路径
- `mcp` 已经开始通过 `TargetSessionRuntime` 复用共享宿主协调逻辑，并不再自己拼装上下文恢复与 lease 获取
- `cli` 中 resource、inspect-method 与 dex 查询链路已经开始直接复用 `app-service` use case
- `cli` 中的导出链路、`inspect` 与 `res-table` 也已经开始直接复用 `app-service` use case
- `cli` 的 workspace 命令链路也已经收口到 `app-service`

所以 `v4` 的重点不是“推翻重写”，而是重新划清共享领域能力和共享运行时能力。

## 仍待讨论

下面这些点还可以继续讨论：

- 应用层是一个模块，还是拆成多个子模块
- `WorkspaceRuntime / TargetSessionRuntime` 是否继续演进成更统一的宿主协调层
- handle 是否对所有入口可见，还是只作为某些入口的内部能力
- dex context cache 的作用域是 workspace、target 还是 session
- 长任务接口是否首版就进入正式 API
