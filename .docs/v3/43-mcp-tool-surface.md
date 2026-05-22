# DexClub V3 MCP Tool 面设计

## 目标

本稿只回答以下 5 件事：

1. MCP P0 应暴露哪些 tool
2. tool 输入输出应遵循什么统一约定
3. session / handle / brief / fields / page 应如何统一
4. 哪些 tool 返回线索，哪些返回事实，哪些返回原始证据
5. 哪些内容暂不进入 P0

本稿不重复讨论：

- 为什么需要 AI 能力面
- `core / mcp / skill` 的总职责划分
- skill 工作流如何编排

这些内容见：

- `42-ai-capability-surface.md`

## 总体原则

### 1. MCP tool 不等于 CLI 命令镜像

MCP tool 的设计目标是：

- 让 AI 低摩擦调用

而不是：

- 按 CLI 现有命令名一比一复制

因此：

- CLI 参数二选一形态
- 面向终端的帮助/错误文案
- 面向人类阅读的渲染结构

都不应直接搬进 MCP。

### 2. tool 应围绕分析动作组织

P0 tool 应围绕：

- 定位
- 检查
- 取证
- 会话

这四类动作展开。

### 3. 输出先结构化，再考虑可读性

MCP 的返回对象应优先满足：

- 字段稳定
- 结构稳定
- 易于 skill 继续消费

而不是优先贴近 CLI text 输出。

### 4. 线索、事实、证据要区分

tool 不应混淆以下输出层级：

- 线索
- 事实
- 原始证据

skill 再基于这些对象形成结论。

### 5. transport 优先选择 `Streamable HTTP`

当前 `dexclub` 的 MCP server 应优先以：

- `Streamable HTTP`

方式暴露，而不是依赖本地 `stdio` 进程握手。

原因是：

- Codex 侧已支持通过 `url` 注册 MCP server
- `Streamable HTTP` 更贴近官方推荐路径
- 可以规避不同 `stdio` framing 实现之间的兼容性问题

当前推荐的运行方式是：

- 先单独启动 `mcp` 进程
- 再在 Codex 配置中通过 `url` 连接

当前实现进一步收敛为：

- 优先使用 `stateless Streamable HTTP`
- 主要走 `POST /mcp`
- 由 DexClub 自己的 `open_target_session` 管理业务会话

当前最小运行参数约定：

- `DEXCLUB_MCP_HOST`
- `DEXCLUB_MCP_PORT`
- `DEXCLUB_MCP_PATH`
- `DEXCLUB_MCP_TRACE`

其中：

- 终端默认保留最小运行日志
- 详细 HTTP / tool 轨迹默认写入 `logs/mcp.log`
- `logs/mcp.log` 始终表示当前 MCP 进程日志
- 每次启动会先把旧的 `mcp.log` 归档到 `logs/archive/mcp_<启动时间>.log`
- `DEXCLUB_MCP_TRACE=false` 时，可显式关闭详细轨迹文件
- 启动日志会明确打印最终监听的 `host:port/path`

这样可以避免将 transport 层 session 与 DexClub 自身 session 语义混在一起。

而不是通过：

- `command`
- `args`

直接拉起本地 `stdio` 进程

## P0 Tool 分组

P0 建议至少提供以下 4 组 tool。

### A. 会话

- `open_target_session`
- `list_target_sessions`
- `get_target_session`
- `close_target_session`
- `diagnose_target_sessions`

### B. 定位

- `find_methods_using_strings`
- `find_classes_using_strings`
- `find_methods`
- `manifest`
- `find_resource_values`
- `get_resource_value`
- `list_res`

### C. 检查

- `inspect_method`

### D. 取证

- `export_method_smali`
- `export_class_smali`
- `export_method_java`
- `export_class_java`

## 通用输入约定

### 1. `session`

所有 P0 tool 优先接受：

- `session`

表示当前 AI 分析会话。

若未提供 `session`，则可以退化接受：

- `workdir`

但这应视为无状态调用路径。

MCP tool 面在设计上应默认遵循：

- `session-first`

也就是：

- 主要调用路径围绕 session 与 handle 展开
- `workdir` 直接调用只作为 fallback 或启动路径

P0 当前已落地的最小口径是：

- `session` 仍是主路径
- 多数 P0 tool 已支持 `workdir` fallback
- `method_handle / class_handle`
  - 仅在 `session` 路径下有效
- `workdir` fallback 不返回、也不接受 handle
- 一个常驻 MCP server 可同时持有多个不同工作区/目标的 target session
- 切换工作区的推荐方式是：
  - 再次调用 `open_target_session`
  - 取得新的 `session_id`
  - 必要时通过 `list_target_sessions / get_target_session / close_target_session`
    管理这些并存 session
- 长时间未访问的空闲 session 会被自动回收，并一并清理该 session 下的 `method_handle / class_handle`
- 因此 `session_id`、`method_handle`、`class_handle` 都应被视为运行时上下文，不保证长期稳定有效

### 2. 对象定位优先级

方法相关 tool 建议统一接受以下二选一：

- `method_handle`
- `descriptor`

类相关 tool 建议统一接受以下二选一：

- `class_handle`
- `descriptor`

优先级应明确：

1. 若提供 handle，则优先按 handle 解析
2. 若未提供 handle，则再按 descriptor 解析

### 3. 分页参数

所有返回列表的 tool 建议统一支持：

- `offset`
- `limit`

规则：

- `offset` 为非负整数
- `limit` 为正整数
- 未指定时使用较小默认窗口

### 4. 字段投影

所有列表型 tool 建议统一支持：

- `fields`

它表示：

- 返回结果项中需要保留哪些字段

例如：

- `["descriptor"]`
- `["descriptor","sourcePath","sourceEntry"]`

### 5. 摘要模式

适合做摘要输出的 tool 建议统一支持：

- `brief`

语义：

- `brief=true`
  - 返回计数、少量核心字段或截断后的样本
- `brief=false`
  - 返回完整结构

### 6. P0 默认调用策略

P0 tool 的默认使用顺序应尽量收敛为：

1. 先 `open_target_session`
2. 先用 `find_*` / `list_res` 的 `brief=true` 与 `fields` 缩小候选
3. 再用 `inspect_method` 看一层事实
4. 最后才调用 `export_*` 获取大段文本证据

这条策略的目标是：

- 降低 token 成本
- 避免在候选很多时直接批量导出文本
- 让 skill 更稳定地围绕 handle 继续下钻

额外约束：

- `manifest` 默认应先只取结构化字段
- 只有在确实需要原始证据时才传 `include_text=true`
- `method_handle / class_handle` 必须来自同一 session 内前序 dexclub 结果
- 不应手工构造占位 handle

### 7. include 语义

对存在可选 section 的 detail tool，建议统一使用：

- `include`

而不是让每个 tool 发明不同命名。

例如 `inspect_method`：

- `include=["callers","invokes","strings"]`

## 通用输出约定

### 1. 列表型输出

所有列表型 tool 建议统一返回：

```json
{
  "items": [],
  "total": 0,
  "offset": 0,
  "limit": 20,
  "hasMore": false
}
```

约束：

- `items` 为当前页
- `total` 为总命中数
- `hasMore` 表示是否还有后续页

### 2. detail 型输出

detail 型 tool 建议直接返回单对象，不套 `items`。

例如：

- `inspect_method`
- `manifest`
- `get_resource_value`

### 3. 证据型输出

导出类 tool 建议返回：

- 对象标识
- 证据文本
- 证据视图类型

推荐形态：

```json
{
  "descriptor": "Lfoo/Bar;->baz()V",
  "view": "smali",
  "text": "..."
}
```

### 4. handle 返回

凡是返回候选对象列表的 tool，建议允许同时返回 handle。

例如结果项可包含：

- `methodHandle`
- `classHandle`

是否默认返回 handle，可由 `fields` 或 tool 默认策略决定。

P0 当前已落地的最小口径是：

- `find_methods`
- `find_methods_using_strings`
- `find_classes_using_strings`

这三类列表结果可返回：

- `methodHandle`
- `classHandle`

并且以下 tool 已接受 handle 输入：

- `inspect_method`
- `export_method_java`
- `export_method_smali`
- `export_class_java`
- `export_class_smali`

## Handle 与 Session 约定

### `open_target_session`

目标：

- 打开一个绑定工作区/target 的分析会话

建议输入：

- `workdir`
- 可选 target 标识

建议输出：

- `session`
- `target`
- `capabilities`

### `list_target_sessions`

目标：

- 列出当前 MCP 进程内所有已打开的 target session

建议输出：

- `total`
- `items`

每个 item 至少包含：

- `sessionId`
- `createdAt`
- `workspace`

### `get_target_session`

目标：

- 读取单个 target session 当前绑定的工作区与 active target 信息

建议输入：

- `session_id`

### `close_target_session`

目标：

- 关闭一个 target session
- 同时清理该 session 下缓存的 `method_handle / class_handle`

建议输入：

- `session_id`

### `diagnose_target_sessions`

目标：

- 提供当前 MCP 进程内 session/handle 的运行态摘要
- 降低“空闲自动回收”带来的黑盒感

建议输出：

- `now`
- `idleTimeoutSeconds`
- `sessionCount`
- `methodHandleCount`
- `classHandleCount`
- `sessions`

每个 session 至少包含：

- `sessionId`
- `createdAt`
- `lastAccessedAt`
- `expiresAt`
- `workspace`

说明：

- 这是感知型诊断 tool，不承担管理动作
- 它用于帮助用户或 skill 理解当前 server 内部状态

### `method_handle`

`method_handle` 表示：

- 某个会话中已解析或已命中的方法对象引用

它不要求跨进程稳定，也不要求跨会话稳定。

### `class_handle`

`class_handle` 表示：

- 某个会话中已解析或已命中的类对象引用

它同样只要求在当前会话内有效。

### 生命周期约束

P0 不要求：

- handle 跨进程持久化
- handle 跨会话复用

P0 只要求：

- handle 在当前 session 内可稳定复用

## 定位类 Tool

实现批次上，建议先落：

- `find_methods_using_strings`
- `find_classes_using_strings`

再落：

- `find_methods`

也就是说：

- `find_methods` 仍属于 P0 能力面
- 但实现顺序可以后于字符串入口和类级入口

### `find_methods_using_strings`

目标：

- 用字符串作为锚点定位方法候选

建议输入：

- `session` 或 `workdir`
- `contains_any_strings`
- `contains_all_strings`
- `offset`
- `limit`
- `fields`
- `brief`

P0 不建议一开始就暴露复杂 DexKit 风格查询对象。

优先提供：

- AI 容易直接填写的短参数

### `find_methods`

目标：

- 按名称或 descriptor 相关条件找方法候选

建议输入：

- `session` 或 `workdir`
- `class_name_contains`
- `method_name_contains`
- `descriptor_contains`
- `offset`
- `limit`
- `fields`
- `brief`

建议输出：

- 列表型输出

每个 item 推荐至少可包含：

- `descriptor`
- `sourcePath`
- `sourceEntry`
- `methodHandle`

补充约定：

- P0 当前可先落最小短参数形态：
  - `class_name_contains`
  - `method_name_contains`
  - `descriptor_contains`
- 其中：
  - `class_name_contains`
  - `method_name_contains`
  - 应优先下沉为底层查询条件
- `descriptor_contains`
  - 可以在 MCP 层作为附加过滤
  - 不要求 P0 一开始就扩展 `core` 的查询对象模型

### `find_classes_using_strings`

目标：

- 用字符串作为锚点定位类候选

建议输入与 `find_methods_using_strings` 对称：

- `session` 或 `workdir`
- `contains_any_strings`
- `contains_all_strings`
- `offset`
- `limit`
- `fields`
- `brief`

每个 item 推荐至少可包含：

- `className`
- `sourcePath`
- `sourceEntry`
- `classHandle`

### `manifest`

目标：

- 返回当前 target 的 manifest 结构化信息

建议输入：

- `session` 或 `workdir`
- 可选 `include`
- 可选 `include_text`

建议输出：

- 单对象

P0 应至少允许 skill 拿到：

- package 名
- versionCode / versionName / sharedUserId
- uses-sdk
- application 信息
- uses-permission / permission
- uses-feature
- activity / service / receiver / provider
- queries
- intent-filter
- metadata

补充约定：

- `manifest` 默认应返回结构化主视图
- `include=["activities","activity-aliases"]`
  - 表示只返回指定 section
- 基础识别字段
  - 如 `packageName / versionCode / versionName / sharedUserId`
  - 不应被 `include` 裁掉
- `include_text=true` 时，允许附带原始 manifest XML 作为取证兜底

### `find_resource_values`

目标：

- 按资源值搜索资源候选

建议输入：

- `session` 或 `workdir`
- `type`
- `value`
- `contains`
- `ignore_case`
- `offset`
- `limit`
- `fields`
- `brief`

补充约定：

- 当前能力面应直接对齐现有 `core` 真实能力
- `find_resource_values` 的主语是资源值命中，不是资源名命中
- 当前建议至少支持：
  - `type=string`
  - `type=integer`
  - `type=bool`
  - `type=color`

### `get_resource_value`

目标：

- 将资源 id 或 type/name 解析为结构化资源值

建议输入：

- `session` 或 `workdir`
- `resource_id`
  或
- `type` + `name`

建议输出：

- 单对象

### `list_res`

目标：

- 返回资源条目列表

建议输入：

- `session` 或 `workdir`
- `type`
- `offset`
- `limit`
- `fields`
- `brief`

补充约定：

- `list_res` 的主语应是资源条目
- 默认返回资源身份与来源映射，不默认展开资源值

## 检查类 Tool

### `inspect_method`

目标：

- 返回单个方法的一层事实

建议输入：

- `session` 或 `workdir`
- `method_handle` 或 `descriptor`
- `include`
- `brief`

P0 `include` 建议支持：

- `using-fields`
- `callers`
- `invokes`
- `strings`
- `annotations`

建议输出：

- `method`
- 可选 `counts`
- `usingFields`
- `callers`
- `invokes`
- `strings`
- `annotations`

其中：

- `brief=true`
  - 应优先返回 `counts`
  - 详情列表可省略或截断
- `brief=false`
  - 返回完整 detail

## 取证类 Tool

### `export_method_smali`

目标：

- 返回单方法的 smali 证据文本

建议输入：

- `session` 或 `workdir`
- `method_handle` 或 `descriptor`
- 可选 `source_path`
- 可选 `source_entry`

建议输出：

- `descriptor`
- `view="smali"`
- `text`

补充约定：

- `source_path` / `source_entry`
  - 用于在同名或同签名对象存在歧义时收窄导出目标
- `export_method_smali`
  - P0 应允许 `mode`
  - 当前建议至少支持：
    - `snippet`
    - `class`

### `export_class_smali`

目标：

- 返回整类的 smali 证据文本

建议输入：

- `session` 或 `workdir`
- `class_handle` 或 `descriptor`
- 可选 `source_path`
- 可选 `source_entry`

建议输出：

- `descriptor`
- `view="smali"`
- `text`

### `export_method_java`

目标：

- 返回单方法的 Java 语义视图

建议输入：

- `session` 或 `workdir`
- `method_handle` 或 `descriptor`
- 可选 `source_path`
- 可选 `source_entry`

建议输出：

- `descriptor`
- `view="java"`
- `text`

### `export_class_java`

目标：

- 返回整类的 Java 语义视图

建议输入：

- `session` 或 `workdir`
- `class_handle` 或 `descriptor`
- 可选 `source_path`
- 可选 `source_entry`

建议输出：

- `descriptor`
- `view="java"`
- `text`

## 视图选择约束

`java` 与 `smali` 都应是正式视图。

规则应明确：

1. 不预设固定顺序
2. 不强迫 skill 为了省 token 先走某一种视图
3. 由 skill 根据当前目标选择更合适的证据视图

通常可理解为：

- `java`
  - 偏语义视图
- `smali`
  - 偏原始证据视图

但最终选择权应留给 skill。

## token 与返回体约束

MCP tool 设计必须考虑 token 成本，但不应把“压缩返回体”当成唯一目标。

因此：

- `brief`
  - 用于减少不必要展开
- `fields`
  - 用于减少无关字段
- `limit`
  - 用于控制单轮结果规模

但不应为了减少一轮返回体，而破坏：

- 事实完整性
- 证据充分性
- 分析步骤合理性

## 并发约束

AI 侧可能并发调用多个 tool。

因此 tool 面设计应允许：

- 并发请求进入

但这不等于实现层必须默认支持：

- 同一 session
- 同一 target
- 同一共享底层资源

上的无约束并发执行。

P0 设计阶段应保留并发策略显式化的空间，而不是默认假设所有 dex 查询都可安全并发。

## 暂不进入 P0 的内容

以下内容暂不进入 P0：

- `inspect_class`
- `inspect_field`
- deep trace tool
- compare / diff 类 tool
- graph / path 类 tool
- 复杂 workflow 型单体 tool
- 跨进程持久 handle
- 后台守护进程

## 一句话结论

MCP P0 不应追求“把 CLI 搬过去”，而应提供：

`一组结构稳定、可会话复用、适合 skill 编排的分析原语 tool`
