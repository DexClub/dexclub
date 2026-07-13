# DexClub V4 稳定化

## 目标

本稿只回答一件事：

`当前 v4 已经固定了哪些边界，接下来哪些事情先不要再动`

## 已固定边界

- `app-service/AppRuntime` 与 `SessionAppRuntime` 作为共享组合根入口
- `app-service/createDefaultAppServices()` 作为上层默认装配入口
- `app-service/AppUseCases` 作为 `cli / mcp` 共用用例装配入口
- `app-service/session/TargetSessionRuntime` 作为 session 与 dex context 的宿主协调入口
- `cli` 只保留命令适配与渲染
- `mcp` 只保留协议适配、参数解析与结果投影

## 当前不再优先动的事

- 不再继续新增入口层 helper
- 不再继续新增装配层
- 不再继续把 `mcp` 的 tool handler 细拆成更多中间层
- 不再把模块拆分当成当前主线

## 验证基线

每次边界相关改动后，至少验证：

- `./gradlew verifyFast`
- `./gradlew :app-service:testStructured`
- `./gradlew :cli-app:testStructured`
- `./gradlew :mcp-app:testStructured`
- `./gradlew :domain-core:compileKotlinJvm`

## 已落回归

当前已经补上的代表性回归有：

- `AppRuntimeTest`
- `AppUseCases` 共享装配测试
- `CliTargetWorkspaceRuntimeTest`
- `cli inspect-method` 缺参负向测试
- `mcp` HTTP session 工具烟测
- `McpExecutionSupportTest`
- `mcp inspectMethodExecution` 缺参负向测试

## 当前判断

现在更应该做的是：

- 维持边界稳定
- 补代表性回归测试
- 按稳定维护期持续检查边界是否回退
