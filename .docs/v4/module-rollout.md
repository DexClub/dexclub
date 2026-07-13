# DexClub V4 模块落地顺序

## 目标

本稿只记录两件事：

- 当前这套模块结构是按什么顺序落地的
- 为什么当时按这个顺序推进

## 当前结论

`v4` 的当前结构已经按下面顺序落地：

1. `app-service`
2. `domain-core`
3. `cli-app`
4. `mcp-app`
5. `dexkit-binding`

补充约束：

- `gui-app` 继续只保留为未来兼容方向

## 为什么是这个顺序

### 1. 先落 `app-service`

因为共享运行时边界当时最先已经长在应用层：

- `cli`
- `mcp`

都已经在直接消费共享装配和运行时逻辑。

先把 `app-service` 抬成正式模块，收益最大：

- 共享执行语义先有正式落点
- 入口层与基础层之间先多出清晰边界

### 2. 再收口 `domain-core`

`app-service` 独立之后，剩余稳定模型、能力边界和底层实现自然收口到：

- `domain-core`

这一步的目标不是重新设计一套新结构，而是先给基础层一个正式名字和稳定位置。

### 3. 然后再改 `cli-app`

入口模块改名应放在共享边界已经成立之后。

这样做的好处是：

- `cli-app` 只是在边界稳定后完成结构对齐
- 不会用模块名变化掩盖职责还没收口的问题

### 4. 再改 `mcp-app`

`mcp-app` 和 `cli-app` 属于同类入口调整，但它同时还带着 MCP 分发、安装和 workflow 路径。

把它放在 `cli-app` 之后，更容易逐步验证：

- 先收一个入口
- 再收另一个入口

### 5. 最后改 `dexkit-binding`

`dexkit-binding` 最后落，是因为它影响范围最大：

- Gradle include
- vendor 路径
- native 路径探测
- workflow
- 打包链

这一步主要是绑定维护链收口，不应抢在应用层和入口层之前。

## 当前状态

这五步已经落地完成。

但要按真实仓库形态理解：

- 根工程最终固定为 `app-service / domain-core / cli-app / mcp-app`
- `dexkit-binding` 最终作为独立工程通过 `includeBuild` 接入

现在真正剩下的动作不是继续造新的迁移阶段，而是：

- 判断当前边界是否已经稳定到足以宣布 `v4` 完成
- 继续收口文档里的现状描述

## 验证基线

围绕这套模块结构，当前至少应保留下面这组验证基线：

- `./gradlew :app-service:testStructured`
- `./gradlew :cli-app:testStructured`
- `./gradlew :mcp-app:testStructured`
- `./gradlew :domain-core:testWorkspace`
- `./gradlew -p dexkit-binding compileKotlinJvm`

如果改动触及 native / Android 维护链，再额外补：

- `./gradlew -p dexkit-binding assembleAndroidMain`
- `./gradlew -p dexkit-binding jvmTest testAndroidHostTest`
