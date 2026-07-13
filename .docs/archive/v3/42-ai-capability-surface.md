# DexClub V3 AI 能力面设计

## 目标

本稿只回答以下 6 件事：

1. 为什么 DexClub 需要单独定义 AI 能力面
2. `core / mcp / skill` 三层分别承担什么职责
3. AI 分析任务在 DexClub 中应如何流转
4. AI 能力面应按什么分析动作分层
5. P0 应先提供哪些能力闭环
6. token 成本在 AI 能力设计中应处于什么位置

本稿讨论的是：

- 面向 AI / skill / agent 的正式能力面
- `core / mcp / skill` 的协作边界
- AI 分析任务的通用动作原语

本稿不讨论：

- CLI 命令继续扩展什么
- deep trace / 方法图 / 路径图
- GUI 形态
- 跨进程缓存或后台服务

## 产品定位

DexClub 当前已有两层稳定边界：

1. `core`
   - 提供稳定分析能力
2. `cli`
   - 提供面向人类用户的终端交互

当调用方从人扩展到 AI / skill / agent 时，仅有 `cli` 已不够。

原因不是：

- CLI 没有能力

而是：

- CLI 的能力组织方式面向人类终端交互，不面向 AI 分析编排

如果继续让上层通过 shell 调 CLI，会带来以下问题：

1. 输入冗长
   - AI 需要拼接命令行和大段 JSON
2. 输出冗余
   - CLI 文本输出更适合人读，不适合 AI 消费
3. 容易脆弱
   - shell、转义、stdout/stderr、文本解析都会增加失败面
4. 缺少会话语义
   - 多轮分析中的上下文和中间对象无法稳定复用
5. 难以编排
   - AI 需要的不只是“调一个命令”，而是一套可组合的分析动作

因此，V3 若要真正服务 AI，应定义一套正式的 AI 能力面。

这套能力面由三层组成：

- `core`
- `mcp`
- `skill`

## 三层职责

### `core`

`core` 是稳定能力层。

它负责：

- 工作区模型
- dex / resource / manifest 相关领域能力
- 稳定 request / result 模型
- 排序、校验、capability gate 等领域规则

它不负责：

- CLI 参数形态
- MCP tool 协议
- AI 编排
- 终端渲染

一句话说：

- `core` 提供原语，但不负责解释 AI 该如何使用这些原语

### `mcp`

`mcp` 是 AI 工具适配层。

它负责：

- tool 注册
- tool 参数解析
- 将 `core` 能力组织成结构化工具面
- 会话态与 handle 管理
- 字段投影、摘要返回、分页等 AI 友好裁剪

它不负责：

- 重新实现静态分析逻辑
- 自己给出领域结论
- 替 skill 决定完整分析策略

一句话说：

- `mcp` 提供可调用的结构化工具，但不替 AI 做分析决策

### `skill`

`skill` 是 AI 分析编排层。

它负责：

- 理解用户问题
- 提出假设
- 选择合适的 tool 与视图
- 决定下一步是继续定位、检查、取证还是回退
- 汇总证据并形成结论

它不负责：

- 直接访问底层 dex 能力
- 自己维护复杂的 shell 脚本封装
- 重新实现 `core` 里的业务规则

一句话说：

- `skill` 是分析员，`mcp` 是工具箱，`core` 是能力底座

## 三层协作关系

依赖方向应保持：

```text
cli -> core
mcp -> core
skill -> MCP tools
```

不应出现：

```text
mcp -> cli
skill -> shell -> cli
core -> mcp
core -> skill
```

这条边界的意义是：

1. `core` 保持稳定，不被上层调用形态反向约束
2. `mcp` 可围绕 AI 需求调整 tool 面，而不破坏 `core`
3. `skill` 可持续进化分析流程，而不必重新设计底层能力

## AI 分析任务流

AI 面对的任务，通常不是“一次调用拿结果”，而是一轮多步分析。

一个通用分析任务通常包含以下阶段：

1. 收集线索
2. 定位候选簇
3. 缩小到对象
4. 检查一层事实
5. 导出原始证据
6. 汇总结论
7. 必要时回退并尝试另一条路径

这里的对象可能是：

- 类
- 方法
- 资源
- 组件

这里的线索可能是：

- 字符串
- 资源名
- manifest 组件
- 类名 / 方法名
- 已知实现片段
- 竞品样本中的局部证据

这意味着 AI 能力面设计的重点，不是“把某个命令搬给 AI”，而是：

- 让 AI 能低摩擦地完成一轮多步分析

## 证据模型

AI 在分析过程中会接触四类不同性质的输出。

### 1. 线索

线索用于回答：

- 从哪里切入
- 哪些命中值得继续看

典型来源：

- `find_*`
- 字符串搜索
- manifest 入口
- 资源搜索

### 2. 事实

事实用于回答：

- 这个对象本身有哪些一层已知信息

典型来源：

- `inspect_method`
- 后续可扩的 `inspect_class` / `inspect_field`

### 3. 原始证据

原始证据用于回答：

- 真实实现到底长什么样
- 是否需要消歧

典型来源：

- `export_*_smali`
- `export_*_java`

### 4. 推断结论

推断结论不是 `mcp` 直接返回的对象，而是由 skill 基于线索、事实和原始证据整理出的分析结果。

因此：

- `mcp` 产出线索、事实、证据
- `skill` 产出结论

如果不区分这四类输出，后续很容易把：

- 某个命中

误当成：

- 已经验证过的结论

## 分析动作分层

AI 能力面应按以下 5 类分析动作组织，而不是按某几个具体场景组织。

### 1. 定位

回答：

- 从哪里切入
- 哪些类/方法/资源/组件值得继续看

典型原语：

- 按字符串找类
- 按字符串找方法
- 按名称找类/方法/字段
- 看 manifest 组件入口
- 按资源名找资源

### 2. 检查

回答：

- 这个对象本身是什么
- 它具备哪些一层事实

典型原语：

- `inspect_method`
- 后续可扩 `inspect_class`
- 后续可扩 `inspect_field`

### 3. 取证

回答：

- 原始实现长什么样
- 是否需要回到底层文本进一步确认

典型原语：

- `export_method_smali`
- `export_class_smali`
- `export_method_java`
- `export_class_java`

不同视图的定位应明确区分：

- `java`
  - 更偏语义视图
  - 更适合快速理解高层结构与主要逻辑
- `smali`
  - 更偏原始证据视图
  - 更适合取证、消歧与确认真实实现

这里不应预设固定顺序，例如：

- 一律先看 `java`
- 一律先看 `smali`

更合理的原则是：

- 由 skill 根据当前分析目标直接选择更合适的视图
- 必要时再切换到另一种视图补证

### 4. 关联

回答：

- 这个点和哪些对象存在局部关联
- 如何把零散命中串成一个小范围结构

典型原语：

- 类包含哪些方法
- 某方法属于哪个类、哪个 source
- 某资源被哪些类或方法引用
- 某组件对应哪些实现类

这里强调的是：

- 受控的一层关联

而不是：

- deep trace
- 方法大图
- 路径搜索

### 5. 会话

回答：

- 如何减少重复传参
- 如何减少同一轮分析中的上下文损耗
- 如何在多轮分析中保留中间判断对象

典型原语：

- target session
- method handle
- class handle
- 分页
- 字段投影
- summary/detail 两段式

## AI 能力面与 CLI 的职责区分

同一能力在 CLI 与 AI 能力面上的职责应明确区分。

例如 `inspect-method`：

- CLI
  - 面向人类阅读
  - 保留终端友好输出
  - 可接受更接近手工操作的参数形态
- AI 能力面
  - 面向结构化消费
  - 默认返回稳定对象
  - 默认支持摘要、字段投影与 handle 复用

因此：

- AI 能力面不应追求与 CLI 命令一一镜像

## P0 设计目标

P0 的目标不是：

- 把所有 CLI 命令搬进 MCP

而是：

- 让 skill 已经拥有一套可完成完整分析闭环的基础原语集合

这个闭环至少应支持：

1. 找入口
2. 找候选簇
3. 看对象一层事实
4. 拉原始证据
5. 在多轮分析中保留上下文

## P0 能力闭环

### A. 定位原语

P0 至少建议提供：

1. `find_methods`
2. `find_methods_using_strings`
3. `find_classes_using_strings`
4. `manifest`
5. `find_resource_values`
6. `get_resource_value`
7. `list_res`

理由：

- 字符串是混淆样本里非常稳定的切入口
- 类级定位通常比方法级定位更适合作为第一步
- Android 功能入口经常依赖组件与资源，而不是单个方法名

### B. 检查原语

P0 至少建议提供：

1. `inspect_method`

第一版 `inspect_method` 应直接复用当前已稳定的五类详情：

- `using-fields`
- `callers`
- `invokes`
- `strings`
- `annotations`

`inspect_class` 与 `inspect_field` 不进入 P0 必须项，但后续可以补。

### C. 取证原语

P0 至少建议提供：

1. `export_method_smali`
2. `export_class_smali`
3. `export_method_java`
4. `export_class_java`

理由：

- 单方法证据适合局部确认
- 整类证据适合看初始化、字段、回调、内部 helper 等上下文
- 很多复杂问题不是单个方法能解释清楚的
- `java` 与 `smali` 各自提供不同层次的证据视图

### D. 关联原语

P0 不建议单独引入“图”能力，但应保留最低限度的局部关联信息来源：

- `inspect_method` 中的一层关系
- source / class / method 的归属关系
- manifest / resource 到实现对象的对应关系

第一版可以不单独做 `relate_*` tool，但应确保上述信息能从现有 tool 组合得到。

### E. 会话原语

P0 至少建议提供：

1. `open_target_session`
2. `method_handle`
3. `class_handle`

以及以下通用能力：

- `limit`
- `offset`
- `fields`
- `brief`

理由：

- skill 不应每轮重复传递完整 descriptor
- 会话态不只是为了省 token
- 也为了减少状态丢失和重复判断

AI 能力面在设计上应默认遵循：

- `session-first`

也就是：

- 先建立 session
- 后续尽量围绕 session 与 handle 展开

`workdir` 直接调用路径仍可保留，但应只作为：

- 启动阶段
- 退化场景
- 尚未持有 session 时的 fallback

## P0 推荐工具面

如果把上面的原语落成具体 tool，P0 推荐至少覆盖：

1. `open_target_session`
2. `find_methods_using_strings`
3. `find_classes_using_strings`
4. `find_methods`
5. `inspect_method`
6. `export_method_smali`
7. `export_class_smali`
8. `export_method_java`
9. `export_class_java`
10. `manifest`
11. `find_resource_values`
12. `get_resource_value`
13. `list_res`

说明：

- 这不是要求所有 CLI 命令都搬进 MCP
- 而是要求 skill 在最小范围内，已经有一套完整分析动作可用
- `find_methods` 仍属于 P0 能力面
- 但实现批次上可以后于字符串入口和类级入口落地

## 会话与上下文模型

AI 分析任务不是单次调用，而是多轮状态推进。

因此会话模型除了减少 token 外，还有两个作用：

1. 保留分析中间态
2. 减少同一轮分析中的状态漂移

建议会话中至少能承载：

- 当前 target
- 最近一次候选结果集
- `methodHandle -> descriptor/source`
- `classHandle -> descriptor/source`
- 待继续下钻的对象引用

如果后续涉及对比分析，会话模型还应允许多个 target 并存，而不是只允许一轮分析只绑定一个对象世界。

这里应明确：

- session 不是可选附属能力
- 而是 AI 能力面的默认上下文承载方式

无 session 的直接调用路径仍然有效，但不应成为主要设计中心。

### 并发约束

AI 侧可能会并发调用 MCP tool。

因此 AI 能力面设计不能默认假设：

- 所有调用永远串行发生

但这不等于实现层必须默认支持：

- 同一 target
- 同一 session
- 同一底层资源

上的真正并发执行。

对于 Dex 相关能力，第一原则应是：

- 先假设共享底层资源需要明确并发控制

在缺少明确并发安全证明前，不应把：

- 同一 target / session 上的 dex 查询

直接设计成无约束并发执行。

更稳的理解是：

- 协议层允许并发调用
- 实现层应显式定义并发策略

例如：

- 哪些 tool 可以安全并发
- 哪些 tool 需要基于 session 或 target 串行化
- 哪些共享资源需要互斥保护

## token 约束

AI 能力设计必须考虑 token 成本，但 token 不是唯一目标。

应明确遵守以下原则：

1. 分析有效性优先于 token 优化
2. 结论可靠性优先于 token 优化
3. token 成本是重要约束，但不是唯一目标

也就是说：

- 不应为了减少一次返回体，就强迫 skill 先走一个并不合适的视图
- 不应为了少传一点文本，就把分析步骤拆成更多轮低效往返

在两种方案分析价值接近时，再优先选择 token 成本更低的那一个。

在上述前提下，仍建议遵守以下输出约束：

### 1. 默认返回摘要

例如 `inspect_method` 可先返回：

- `counts.callers`
- `counts.invokes`
- `counts.usingFields`
- `counts.strings`
- `counts.annotations`

在需要时再显式返回详情列表。

### 2. 支持字段投影

列表型 tool 应支持只返回必要字段，例如：

- `fields=["descriptor"]`
- `fields=["descriptor","sourcePath","sourceEntry"]`

### 3. 默认小批量

所有列表型 tool 默认都应有较小的返回窗口。

### 4. 不默认返回大文本

smali / java / dex 这类大对象必须显式请求。

### 5. 不把 CLI 文案搬进 AI 能力面

AI 能力面不应返回：

- help 文案
- usage 文案
- 面向终端的状态提示语

### 6. 尽量通过 handle 复用对象引用

同一轮分析里，后续步骤应优先基于：

- `method_handle`
- `class_handle`

继续展开，而不是每次重传完整对象。

## 与现有基础设施的关系

AI 能力面 P0 应直接站在当前 `core` 之上。

已有且应复用的基础包括：

- 工作区模型
- `Services`
- `DexAnalysisService`
- `ResourceService`
- target 级 `DexKitBridge` 进程内复用
- `inspect-method`
- `find-method`
- `find-class-using-strings`
- `find-method-using-strings`
- `manifest`
- `find-res-values`
- `get-res-value`
- `list-res`
- `export-method-smali`
- `export-class-smali`
- `export-method-java`
- `export-class-java`

这意味着 AI 能力面 P0 的重点不是“新增静态分析能力”，而是：

- 重组输入输出
- 形成 AI 友好的能力面
- 形成可供 skill 编排的分析原语集合

## P1 以后再讨论的内容

以下内容暂不进入 AI 能力面 P0：

- 所有 CLI 命令的一比一镜像
- `trace-method`
- 路径搜索、调用图、方法图
- `inspect-class`
- `inspect-field`
- `param-annotations`
- `opCodes`
- `DexKitCacheBridge` 接入
- 跨进程缓存
- 后台服务/守护进程

这些内容不是没有价值，而是当前阶段应先保证：

- 三层边界清楚
- P0 原语闭环成立
- skill 已经能完成真实多步分析

## 推荐推进顺序

建议按以下顺序推进：

1. 新增 `mcp/` 模块并打通最小 server
2. 落 `open_target_session`
3. 落 `inspect_method`
4. 落 `find_methods_using_strings`
5. 落 `find_classes_using_strings`
6. 落 `export_method_smali`
7. 落 `export_class_smali`
8. 落 `export_method_java`
9. 落 `export_class_java`
10. 落 `manifest`
11. 落 `find_resource_values / get_resource_value / list_res`
12. 再回头补 `find_methods`

原因：

- `inspect_method` 是当前最成熟的单点能力
- 字符串定位 + 类级导出，最接近真实分析起手式
- `java` 与 `smali` 都是正式证据视图，不应把 `java` 视为可有可无的后补项
- Android 上下文入口应尽早进入 AI 能力面，而不是只做 dex 侧能力
- `find_methods` 虽然重要，但对 skill 而言未必比字符串和类级入口更早产生价值

## 一句话结论

DexClub 后续若要真正服务 AI / skill，应补的是：

`一套由 core 提供原语、由 mcp 提供结构化工具面、由 skill 负责任务编排的 AI 能力体系`

而不是继续把 CLI 当作 AI 的主调用入口，也不是只做少量方法级 tool。
