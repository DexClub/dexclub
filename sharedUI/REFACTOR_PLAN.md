# sharedUI 重构计划

## 范围

本计划只针对 `sharedUI` 模块内部重构。

本计划明确不做以下事情：
- 不拆 `sharedUI` 为多个 Gradle 模块
- 不调整 Android / Desktop 的应用入口模块结构
- 不在第一阶段引入新的 DI 框架
- 不在第一阶段大规模改动 UI 行为和交互设计

## 额外前提

本次重构允许破坏性调整，并接受以下前提：
- 不以兼容现有本地数据库为目标；必要时可直接删除旧数据库文件并重建
- `workspace_index.db`、`classes.db`、`editor_session.db` 都允许在重构期间直接重建
- Room schema 可以重新建立为 `version = 1`，本次重构不要求维护 migration path
- `sharedUI/schemas/*/1.json` 可以在设计稳定后直接覆盖为新的基线
- 不需要为了兼容旧 schema 保留过渡表、过渡字段、过渡 DAO 或迁移测试
- 最终持久化拓扑不以现有 `workspace_index.db`、`classes.db`、`editor_session.db`、`setting.json` 为边界
- 可以按职责合并、拆分、重命名数据库与配置文件；数据库数量允许增加或减少
- 可以将部分状态从 Room 迁移到 `KStore`，也可以反向将多个零散文件收敛到 SQLite / Room

本计划重点解决以下问题：
- `WorkspaceSceneViewModel` 职责过多，文件过大，已演变为 God Object
- `WorkspaceSidePanel.kt`、`WorkspaceCodePanel.kt`、`HomeScene.kt` 体积过大，UI 与业务状态耦合过深
- `WorkspaceSceneViewModel` 对外暴露过多离散 `StateFlow`，页面层需要手动拼接状态，导致状态边界模糊
- `ViewModel` 直接依赖 Room、FileKit、DexFactory、DexKit、设置存储，缺少中间层
- `HomeScene.kt`、`WorkspaceScene.kt` 直接在 Composable 中创建 ViewModel，缺少统一依赖装配入口
- `NavigationModels.kt` 文件名与职责不匹配，导航模型、解析器、文本分析逻辑混在一起
- `WorkspaceSceneViewModel` 仍直接触发文件选择、导出目录等 `FileKit` 交互，UI side effect 边界不干净
- 导航层直接传递 `WorkspaceEntity`，界面状态与数据库实体耦合较深
- 搜索结果、类树节点等页面模型仍直接暴露 `ClassesEntity` / `WorkspaceEntity`
- 资源创建与关闭职责分散，数据库、DexKit、highlight runtime 的生命周期边界不清晰
- 目前缺少最小回归测试，后续拆分风险偏高

## 重构目标

重构完成后，`sharedUI` 应满足以下目标：
- 保持单 Gradle 模块，但模块内分层清晰
- 依赖通过单一装配入口集中创建，避免在 Composable 中直接拼装复杂依赖
- `ViewModel` 只负责状态聚合、事件分发、生命周期协调
- 业务逻辑下沉到 repository / service / coordinator
- 持久化实现层只保留具体存储实现（Room / `KStore` 等），不再承载页面业务编排
- 选择器、launcher 等平台 UI 交互留在 Composable / 平台层，ViewModel 只消费结果
- 场景层优先暴露聚合后的 `UiState` 与少量 `UiEffect`，而不是持续增加离散 `StateFlow`
- 大型 UI 文件拆成多个小组件文件，单文件职责明确
- 导航、搜索、代码加载、高亮、定义跳转分别可独立维护
- 页面模型、导航参数、搜索结果不再直接暴露 Room entity
- 数据库、DexKit、缓存和高亮引擎的创建与关闭归属明确
- 明确区分“持久源数据”和“可再生缓存 / 会话数据”，避免缓存库结构反向约束上层设计
- 按数据形态选择存储实现：结构化检索优先 SQLite / Room，轻量配置与聚合快照优先 `KStore`
- `Nav3` / `NavKey` / serialization 相关代码收口在 `app/navigation`，scene/domain 层不直接感知导航框架细节
- Android / JVM actual 实现继续遵循同一套 scene contract
- 后续如需再做 Gradle 模块化，可以直接按现有边界继续外提

## 当前重点问题

### P0

- `sharedUI/src/commonMain/kotlin/io/github/dexclub/app/scene/workspace/WorkspaceSceneViewModel.kt`
  - 当前承担初始化、类索引、标签页、代码加载、高亮、滚动状态、编辑状态、定义跳转、字符串搜索、设置持久化、DexKit 生命周期、日志导出等职责
- `sharedUI/src/commonMain/kotlin/io/github/dexclub/app/scene/workspace/WorkspaceSidePanel.kt`
  - 同时承担标题栏、设置弹窗、搜索弹窗、树节点绘制、滚动同步
- `sharedUI/src/commonMain/kotlin/io/github/dexclub/app/scene/workspace/WorkspaceCodePanel.kt`
  - 同时承担 TabBar、Pager、CodeView、右键菜单、选择与复制逻辑

### P1

- `sharedUI/src/commonMain/kotlin/io/github/dexclub/app/scene/workspace/NavigationModels.kt`
  - 模型、resolver、文本扫描工具函数混在一个文件中
- `sharedUI/src/commonMain/kotlin/io/github/dexclub/app/Scenes.kt`
  - 直接耦合 `Nav3` 的 `NavKey` 与 `WorkspaceEntity`，后续应收口到 `app/navigation`
- `sharedUI/src/commonMain/kotlin/io/github/dexclub/app/scene/home/HomeSceneViewModel.kt`
  - 直接处理工作区导入、目录操作、数据库写入、文件选择器调用
- `sharedUI/src/commonMain/kotlin/io/github/dexclub/app/scene/home/HomeScene.kt`
- `sharedUI/src/commonMain/kotlin/io/github/dexclub/app/scene/workspace/WorkspaceScene.kt`
  - 直接在 Composable 中创建 ViewModel，后续 repository / service 注入缺少稳定入口
- `sharedUI/src/commonMain/kotlin/io/github/dexclub/settings/AppSettingsStore.kt`
  - 目前是直接静态访问，后续应通过仓储封装给上层使用

### P2

- `sharedUI/src/commonMain/kotlin/io/github/dexclub/database/*`
  - Database 实例生命周期管理分散，后续应通过 provider / repository 收口
- `workspace_index.db`、`classes.db`、`editor_session.db`
  - 当前没有清晰区分用户数据、缓存和会话状态，schema 容易被历史结构反向绑住
- `sharedUI/src/commonMain/kotlin/io/github/dexclub/settings/AppSettingsStore.kt`
  - 当前仍手写 `setting.json` 读写，但项目已经依赖 `kotlinx.serialization` + `KStore`，存储抽象没有统一
- `sharedUI/src/commonMain/kotlin/io/github/dexclub/app/Scenes.kt`
  - 导航对象直接持有 `WorkspaceEntity`
- `sharedUI/src/commonMain/kotlin/io/github/dexclub/app/scene/workspace/WorkspaceSearchModels.kt`
- `sharedUI/src/commonMain/kotlin/io/github/dexclub/node/ClassTreeNode.kt`
  - 页面模型直接持有 `ClassesEntity`，后续应引入更轻的 scene/domain model
- `sharedUI/src/androidMain/kotlin/io/github/dexclub/app/scene/workspace/WorkspaceScene.android.kt`
- `sharedUI/src/jvmMain/kotlin/io/github/dexclub/app/scene/workspace/WorkspaceScene.jvm.kt`
  - 平台 actual 当前演进程度差异较大，重构时应优先固化共享 contract

## 目录目标

重构后，建议 `sharedUI/src/commonMain/kotlin/io/github/dexclub/` 逐步收敛为以下结构：

```text
io/github/dexclub/
├─ app/
│  ├─ compose/
│  ├─ di/
│  │  └─ SharedUiDependencies.kt
│  ├─ navigation/
│  │  └─ Scenes.kt
│  ├─ model/
│  ├─ res/
│  └─ scene/
│     ├─ home/
│     │  ├─ HomeScene.kt
│     │  ├─ HomeSceneViewModel.kt
│     │  ├─ HomeUiState.kt
│     │  ├─ HomeUiEffect.kt
│     │  ├─ WorkspaceList.kt
│     │  ├─ NewWorkspaceDialog.kt
│     │  └─ DeleteWorkspaceDialog.kt
│     └─ workspace/
│        ├─ WorkspaceScene.kt
│        ├─ WorkspaceSceneViewModel.kt
│        ├─ WorkspaceUiState.kt
│        ├─ WorkspaceUiEffect.kt
│        ├─ side/
│        │  ├─ WorkspaceSidePanel.kt
│        │  ├─ SideHeaderBar.kt
│        │  ├─ WorkspaceSearchDialog.kt
│        │  ├─ WorkspaceSettingsDialog.kt
│        │  └─ ClassTreeList.kt
│        ├─ code/
│        │  ├─ WorkspaceCodePanel.kt
│        │  ├─ WorkspaceTabBar.kt
│        │  ├─ CodeViewPane.kt
│        │  ├─ CodeViewPager.kt
│        │  └─ CodeContextMenu.kt
│        └─ navigation/
│           ├─ NavigationModels.kt
│           └─ NavigationUiEvent.kt
├─ core/
│  ├─ workspace/
│  │  ├─ WorkspaceRepository.kt
│  │  ├─ WorkspaceImporter.kt
│  │  ├─ WorkspaceInitializer.kt
│  │  ├─ WorkspaceIndexService.kt
│  │  └─ WorkspaceDexKitRuntime.kt
│  ├─ editor/
│  │  ├─ EditorSessionRepository.kt
│  │  ├─ OpenTabService.kt
│  │  ├─ CodeContentService.kt
│  │  ├─ CodeContentRuntime.kt
│  │  ├─ HighlightCoordinator.kt
│  │  └─ EditorStateRepository.kt
│  ├─ navigation/
│  │  ├─ NavigationService.kt
│  │  ├─ DeclarationResolver.kt
│  │  └─ resolver/
│  │     ├─ JavaDeclarationResolver.kt
│  │     └─ SmaliDeclarationResolver.kt
│  ├─ search/
│  │  ├─ ClassSearchService.kt
│  │  └─ StringSearchService.kt
│  └─ settings/
│     └─ AppSettingsRepository.kt
├─ data/
│  ├─ settings/
│  ├─ workspace/
│  ├─ classindex/
│  └─ editorsession/
├─ lang/
├─ compat/
├─ node/
└─ utils/
```

说明：
- 第一阶段不要求一次性建完所有目录
- 以“先抽服务，再迁移调用，再收缩 ViewModel”为主
- 现有 `settings/AppSettingsStore.kt` 可以先保留；`core/settings/AppSettingsRepository.kt` 作为上层入口，后续再迁移到 `data/settings`
- `app/di/SharedUiDependencies.kt` 只是示意名称，也可以用 scene factory / provider 实现同样职责
- 可以先定义 scene-facing model / route args 基线，再回头调整 Room entity；不用为了兼容旧 schema 保留过渡层
- 示例中的 `data/` 表示存储实现层；若某部分继续使用 Room，可在 `data/.../room` 下承接 `database / dao / entity`
- 若某部分迁移到 `KStore`，则在 `data/.../store` 或等价目录放置实现，不要求强行模拟 Room 结构

## 分阶段计划

## 存储策略建议

基于当前代码和依赖，建议优先按“数据形态”而不是按“现有文件名”来设计持久化：

- 应用设置：
  - 推荐从手写 `setting.json` 迁移为 `KStore` 驱动的 `AppSettingsRepository`
  - 原因：字段少、结构稳定、天然适合 `kotlinx.serialization`，不需要 Room
  - 截至 2026-03-13：已落地为 `KStore`
- 工作区清单：
  - 当前只有 `getAll()` / `insert()` / `delete()` 级别操作，可评估是否从 `workspace_index.db` 降为 `KStore`
  - 若后续需要分页、筛选、排序、索引约束，再继续保留或回到 Room
  - 截至 2026-03-13：已落地为 `KStore` 驱动的 `workspaces.json`
- 类索引：
  - 仍建议优先保留在 SQLite / Room 一侧，或收敛为单个 workspace cache db 中的一个子域
  - 原因：存在批量写入、名称查找、简单检索与后续建索引空间
- 编辑器会话：
  - 当前数据更像一个聚合快照，可评估是否用 `KStore` 持久化 `EditorSessionSnapshot`
  - 若后续仍需要事务更新、唯一约束和细粒度查询，再保留为 Room

推荐决策顺序：
- 先定义 `Repository` / `Store` 接口和 domain model
- 再决定底层使用 Room、`KStore` 或混合实现
- 先保证 `ViewModel` 与存储技术解耦，再决定最终文件拓扑
- 不为了延续旧文件名而保留不必要的数据库

额外建议：
- 导航参数保持“小而稳定”，优先只放 `id`、名称、初始展示模式等可序列化轻量字段
- 不在导航参数中传递大型对象、数据库实体、代码内容或运行时缓存
- 编辑器持久化只保存基础快照字段，不持有 `CodeViewState` 等 UI 组件状态对象

## 阶段 0：建立安全网

目标：
- 在大规模拆分前，先补最小回归测试和人工验证清单

任务：
- 新建 `sharedUI/src/commonTest` 与 `sharedUI/src/jvmTest`
- 在 `sharedUI/build.gradle.kts` 中补齐 `commonTest` / `jvmTest` 依赖
- 先补纯逻辑测试，避免一开始写 UI 测试
- 不为旧数据库兼容补 migration test，测试重点放在行为回归和 repository / resolver contract
- 为 repository / store contract 预留 fake / in-memory 实现，避免测试直接绑定 Room 或文件系统
- 为 Java / Smali 解析、字符串定位准备文本级 fixture / testdata
- 为后续 DexKit / repository / resolver 拆分预留 fake / stub 注入点，第一阶段不强求真实 DexKit 集成测试
- 优先覆盖以下能力：
- `WorkspaceEntity.dexsAbsolutePathList`
- `ClassTreeNode.parse()` 与 `flatten()`
- `SignatureUtils`
- `NavigationModels.kt` 中可独立抽测的声明定位逻辑
- Java / Smali 参数解析、方法签名匹配、字符串命中定位

完成标准：
- 至少有一组可以运行的 `jvmTest`
- 测试目录、依赖和样例数据准备完成，后续新增逻辑测试不需要再补基础设施
- 后续对导航和搜索逻辑的改动可由测试兜底

## 阶段 1：先建立应用层边界，不改 UI 行为

目标：
- 先把 `ViewModel` 对底层实现的直接依赖收口

任务：
- 先梳理存储分类矩阵：哪些属于 source of truth、cache、session、config
- 新建 `core/workspace/WorkspaceRepository.kt`
- 新建 `core/editor/EditorSessionRepository.kt`
- 新建 `core/settings/AppSettingsRepository.kt`
- 建立 `HomeUiState` / `HomeUiEffect` 与 `WorkspaceUiState` / `WorkspaceUiEffect` 的最小 contract
- 场景层公开状态优先收口为 `StateFlow<UiState>` + 单独 `UiEffect` 通道，避免继续暴露更多离散 Flow
- 为 repository 增加明确的存储契约命名，避免接口名直接绑定现有 `xxxDatabase`
- 约定统一依赖装配入口（例如 `SharedUiDependencies` / scene factory）
- 先定义轻量路由参数和 scene-facing model 基线（如 `WorkspaceRouteArgs`、`WorkspaceSummary`）
- 收口 `Nav3` 相关类型到 `app/navigation`，避免 `scene` / `core` 层直接依赖 `NavKey`
- `HomeSceneViewModel` 不再直接 `WorkspaceIndexDatabase.open()`
- `WorkspaceSceneViewModel` 不再直接 `ClassIndexDatabase.open()`、`EditorSessionDatabase.open()`、`AppSettingsStore.load()`、`AppSettingsStore.save()`
- `HomeScene.kt`、`WorkspaceScene.kt` 不再在 Composable 中直接拼装复杂依赖
- 明确数据库、DexKit、highlight runtime 的 owner 与关闭时机
- 优先把 `AppSettingsStore` 收口为 `AppSettingsRepository`，并评估是否直接切到 `KStore`
- 明确 `FileKit`、目录选择、导出路径选择这类 side effect 通过 `UiEffect` 或 scene callback 触发
- 先采用手动构造依赖，不引入 DI 框架

建议原则：
- repository 只负责持久化访问与简单映射
- service 负责业务编排
- `ViewModel` 只依赖接口或明确职责的实现类
- repository 接口不暴露 Room / `KStore` / 文件路径细节
- `UiState` 承担页面消费契约，避免组件横向订阅过多原子 Flow

完成标准：
- `HomeSceneViewModel` 与 `WorkspaceSceneViewModel` 中直接访问数据库 companion object 的代码显著减少
- 设置加载与保存从静态调用迁移到 repository
- 不再新增直接向 scene 层暴露 `WorkspaceEntity` 的入口
- 至少完成一处“接口不变但底层存储实现可替换”的落地验证，推荐优先在设置存储完成
- `WorkspaceSceneViewModel` 不再直接调用 `FileKit` 或目录选择 API
- ViewModel 构造参数来源清晰，后续新增 service 不需要继续在 Composable 内联拼装

## 阶段 2：拆 Home 场景

目标：
- 先处理较小场景，形成拆分模板

任务：
- 从 `HomeScene.kt` 中拆出：
- `WorkspaceList.kt`
- `NewWorkspaceDialog.kt`
- `DeleteWorkspaceDialog.kt`
- 新建 `HomeUiState.kt`
- 新建 `HomeUiEffect.kt`
- 将文件选择器触发与 `PickerResultLauncher` 放回 `HomeScene.kt`
- `HomeSceneViewModel` 收敛为：
- 页面状态
- 工作区列表刷新
- 调用 `WorkspaceImporter`
- 调用 `WorkspaceRepository`
- 不再直接持有 `openDirectoryPickerCompat()` / `FileKit.openFilePicker()` 等 UI 平台调用
- 逐步把 `WorkspaceEntity` 映射为首页消费的轻量 `WorkspaceSummary`

建议新增类：
- `core/workspace/WorkspaceImporter.kt`
- `core/workspace/WorkspaceCreationResult.kt`

完成标准：
- `HomeScene.kt` 只保留页面装配
- `HomeSceneViewModel.kt` 主要负责状态与事件，不再直接组织完整导入流程
- 首页场景默认以单个 `HomeUiState` 和少量 `HomeUiEffect` 对外提供契约
- 文件选择器逻辑从 ViewModel 中移出，UI / 平台边界更清晰

## 阶段 3：拆 Workspace 初始化与类索引

目标：
- 从 `WorkspaceSceneViewModel` 中先剥离初始化流程

任务：
- 抽出 `core/workspace/WorkspaceInitializer.kt`
- 抽出 `core/workspace/WorkspaceIndexService.kt`
- 结合阶段 1 的存储分类，决定类索引是继续独立为 `classes.db`，还是并入更通用的 workspace cache storage
- 视情况新增轻量类树 / 类索引页面模型，避免 `ClassTreeNode.ClassNode` 继续直接持有 `ClassesEntity`
- 将以下职责迁出 `WorkspaceSceneViewModel`：
- `initialization()`
- `hasCompleteClassIndexData()`
- `buildClassesIndex()`
- `queryClassesIndex()`
- DexKit 预热
- 类树构建与恢复流程
- 明确 `WorkspaceInitializer` / `WorkspaceIndexService` 对 DexFactory、DexKit 预热、ClassIndexDatabase 的持有与释放边界

建议新增模型：
- `WorkspaceBootstrapResult`
- `WorkspaceIndexState`

完成标准：
- `WorkspaceSceneViewModel` 不再直接处理类索引构建细节
- 初始化逻辑收口到一个可单测的 service
- 类树构建输出尽量依赖轻量模型，而不是直接回传 Room entity

## 阶段 4：拆标签页、代码内容、编辑状态

目标：
- 从 Workspace 主 ViewModel 中拆掉最重的编辑器运行时逻辑

任务：
- 抽出 `core/editor/OpenTabService.kt`
- 抽出 `core/editor/CodeContentService.kt`
- 抽出 `core/editor/CodeContentRuntime.kt`
- 抽出 `core/editor/EditorStateRepository.kt`
- 抽出 `core/editor/HighlightCoordinator.kt`
- 决定编辑器会话是继续保留 `EditorSessionDatabase`，还是收敛为 `KStore` 驱动的会话快照存储
- 明确 `EditorSessionDatabase` 或会话 store、`CodeContentCache`、highlight registration 的持有与释放位置
- 将编辑器持久化 contract 固定为基础快照字段，避免 `CodeView` 组件状态对象渗透进 repository
- 将以下职责迁出 `WorkspaceSceneViewModel`：
- 打开标签页
- 切换标签页
- 标签页布局模式切换
- 代码内容读取与缓存
- 代码高亮 patch 分发
- 滚动位置、光标、选区的持久化

阶段 4 建议继续按以下子顺序推进：
- 先落 `CodeContentService`，承接文件读取、缓存、语言 session、plainLines 和初始高亮注册准备
- 再抽 `EditorStateRepository`，承接滚动位置、光标、选区和保存防抖
- 最后补 `HighlightCoordinator`，承接 patch merge、viewport 事件转发与 `HighlightEngine` 生命周期协调

截至 2026-03-11：
- `CodeContentService` 已落地
- `CodeContentRuntime` 已落地
- `EditorStateRepository` 已落地
- `HighlightCoordinator` 已落地
- `loadParsedContentsForTab(...)` 的 generation 控制、代码内容快照写回、runtime state 清理已迁到 `CodeContentRuntime`
- 阶段 4 主链已闭合；剩余工作主要转入阶段 5 / 6 的 DexKit runtime owner、side effect 和 UI 文件拆分

建议保留在 ViewModel 的内容：
- 单个 `StateFlow<WorkspaceUiState>`
- 少量 `UiEffect` / 一次性事件通道
- 用户事件入口

完成标准：
- `WorkspaceSceneViewModel.kt` 去掉大量围绕 tab/content/highlight 的私有函数
- `CodeContentCache` 可以视情况迁移到 `core/editor`，不再放在 `database/editorsession`
- 编辑器会话的存储实现与页面逻辑解耦；如后续需要从 Room 切到 `KStore` 不再影响 `ViewModel`
- 页面与 `code-view` 组件的耦合收敛在 `WorkspaceCodePanel` 一侧，不再倒灌到仓储层
- Workspace 场景默认以单个 `WorkspaceUiState` 和少量 `WorkspaceUiEffect` 对外提供契约
- 编辑器运行时状态在关闭页面时仍能按既有行为持久化和释放

## 阶段 5：拆定义跳转、搜索、设置

目标：
- 将文本分析和语义解析从页面状态层彻底剥离

任务：
- `NavigationModels.kt` 拆分为：
- `app/scene/workspace/navigation/NavigationModels.kt`
- `app/navigation/Scenes.kt` 或等价文件
- `core/navigation/DeclarationResolver.kt`
- `core/navigation/resolver/JavaDeclarationResolver.kt`
- `core/navigation/resolver/SmaliDeclarationResolver.kt`
- `core/navigation/NavigationService.kt`
- 抽出搜索能力：
- `core/search/ClassSearchService.kt`
- `core/search/StringSearchService.kt`
- `WorkspaceSearchModels.kt` 改为页面消费的轻量模型，不再直接暴露 `ClassesEntity`
- 抽出 `core/workspace/WorkspaceDexKitRuntime.kt`
- 收口设置能力到阶段 1 已建立的 `core/settings/AppSettingsRepository.kt`
- 如设置相关编排继续膨胀，再补 `WorkspaceSettingsService.kt`
- 将 Workspace 中残留的导出目录选择、日志导出等 UI side effect 从 ViewModel 中迁出

截至 2026-03-11：
- `DeclarationResolver` 契约已迁到 `core/navigation`
- Java / Smali resolver 已迁到 `core/navigation/resolver`
- `app/scene/workspace/NavigationModels.kt` 已收缩为 scene runtime 模型
- `core/search/ClassSearchService.kt` 已落地
- `core/search/StringSearchService.kt` 已落地
- `core/navigation/NavigationService.kt` 已落地
- `core/search/StringSearchLocationResolver.kt` 已落地
- `WorkspaceSearchModels.kt` 已改为轻量 scene model，不再直接暴露 `ClassesEntity`
- `WorkspaceSceneViewModel` 中 DexKit 搜索与结果映射已迁移到 search service
- `WorkspaceSceneViewModel` 中声明跳转与搜索结果跳转的目标准备、目标落点推断已迁移到 navigation/search service
- `WorkspaceDexKitRuntime` 已落地，DexKit bridge 的创建、复用、关闭不再留在 ViewModel
- `WorkspaceUiEffect.ShowMessage` 已落地
- `WorkspaceScene.jvm.kt` 已通过 scene callback 接管日志导出目录选择
- `WorkspaceSceneViewModel` 已移除 `exportCurrentWorkspaceLogs(...)` 这类直接持有 picker / FileKit 的入口
- 阶段 5 可视为完成；后续只在阶段 6 / 7 随 UI 拆分顺手清理少量 scene glue code

建议规则：
- `app/.../NavigationModels.kt` 只保留页面需要消费的模型
- `app/navigation/*` 只承接 Nav3 路由、入参和返回路径，不承接业务逻辑
- resolver 文件只负责声明定位
- search service 负责 DexKit 搜索和结果映射
- repository / service 返回 domain model，是否映射成 UI model 由 scene 层决定

完成标准：
- `WorkspaceSceneViewModel` 不再包含大段 Java / Smali 文本扫描算法
- `NavigationModels.kt` 文件名与内容重新匹配
- 搜索结果模型不再直接夹带 Room entity
- Workspace 相关 `FileKit` / 导出逻辑不再留在 ViewModel

## 阶段 6：拆 Workspace UI 文件

目标：
- 把 UI 与状态编排彻底分开

任务：
- 从 `WorkspaceSidePanel.kt` 拆出：
- `SideHeaderBar.kt`
- `WorkspaceSearchDialog.kt`
- `WorkspaceSettingsDialog.kt`
- `ClassTreeList.kt`
- `ClassTreeRow.kt`
- 从 `WorkspaceCodePanel.kt` 拆出：
- `WorkspaceTabBar.kt`
- `WorkspaceTabBarItem.kt`
- `CodeViewPane.kt`
- `CodeViewPager.kt`
- `CodeContextMenu.kt`
- `WorkspaceCodePanel.kt` 最终只保留装配逻辑

截至 2026-03-11：
- `WorkspaceSidePanel.kt` 已收缩到约 31 行，只保留 `WorkspaceSidePanel(...)` 装配
- `WorkspaceSidePanel.kt` 相关 UI 已拆到 `SideHeaderBar.kt`、`WorkspaceSettingsDialog.kt`、`WorkspaceSearchDialog.kt`、`ClassTreeList.kt`、`ClassTreeRow.kt`
- `WorkspaceCodePanel.kt` 已收缩到约 55 行，只保留 `WorkspaceCodePanel(...)` 装配
- `WorkspaceCodePanel.kt` 相关 UI 已拆到 `WorkspaceTabBar.kt`、`WorkspaceTabBarItem.kt`、`CodeViewPane.kt`、`CodeViewPager.kt`、`CodeContextMenu.kt`
- `WorkspaceSceneContent.kt` 已落地，JVM / Android actual 现已共享主体布局、loading 覆盖层和 effect 收口
- JVM / Android actual 都已通过 scene callback 接管日志导出目录选择；阶段 6 的前置 scene contract 已够用，不需要再回头修改阶段 5 主边界
- `WorkspaceSearchDialog.kt` 已继续拆到 `WorkspaceSearchTabs.kt`、`WorkspaceSearchResultCards.kt`，当前约 319 行
- `CodeViewPane.kt` 已继续拆到 `CodeViewNavigateContext.kt`、`CodeViewPaneSelectionUtils.kt`、`CodeViewPaneStyle.kt`，当前约 294 行
- `ClassTreeNode.ClassNode` 已改为轻量节点，不再直接持有 `ClassesEntity`
- `ClassTreeNode.parse(...)` 已改为消费轻量 `ClassTreeClassItem`
- `WorkspaceIndexedClassRecord.kt` 已落地
- `OpenTabService` / `NavigationService` / `WorkspaceSceneViewModel` 已改为消费轻量类记录，不再直接依赖 `ClassesEntity`
- `:sharedUI:compileKotlinJvm --rerun-tasks --no-build-cache` 已通过
- `:sharedUI:compileAndroidMain --rerun-tasks --no-build-cache` 已通过
- `:sharedUI:jvmTest --rerun-tasks --no-build-cache` 已通过

当前可直接利用的切点：
- `WorkspaceSidePanel.kt` 已有 `SideHeaderBar`、`WorkspaceSettingsDialog`、`WorkspaceSearchDialog`、`WorkspaceSearchTabs`、`WorkspaceClassSearchResultCard`、`WorkspaceStringSearchResultCard`、`WorkspaceSearchStateText`、`SideTreeRow`、`SideLazyColumn`
- `WorkspaceCodePanel.kt` 已有 `TabBarItem`、`HeaderTabBar`、`CodeViewPane`、`CodeViewPage`、`CodeViewPager`

建议切分顺序：
- 第一轮主拆分已完成：`WorkspaceSidePanel.kt` / `WorkspaceCodePanel.kt` 已分别收缩为装配文件
- `WorkspaceScene.kt`、`WorkspaceScene.jvm.kt`、`WorkspaceScene.android.kt` 的主体 contract 已完成第一轮收口
- `CodeViewPane.kt` 的 helper 已继续贴近使用点整理，阶段 6 的主要 UI 细化工作可视为完成
- 阶段 7 的 scene-facing 解耦主链已基本完成；下一步优先转入阶段 8 的命名、过渡代码和 ViewModel 收缩收尾
- 二次细化仍以“文件边界更清晰”为目标，不重写行为

阶段 6 约束：
- 第一刀只做职责切开，不同时改 UI 行为、导航行为或状态结构
- 第一刀允许继续向拆出的 Composable 传 `WorkspaceSceneViewModel`；不要为了“纯 props”一次性引入几十个参数
- 不要在拆 UI 的同时回头重做 `WorkspaceUiState` / `WorkspaceUiEffect` 契约
- 不要把 Android actual 对齐工作提前到 UI 文件尚未拆完之前
- 不要把新的平台 side effect、数据库访问或 DexKit 生命周期代码重新塞回 UI 文件

完成标准：
- 主要 UI 文件长度控制在 200-350 行附近
- 单个 Composable 文件职责单一，便于以后继续改样式或交互
- `WorkspaceSidePanel.kt` 与 `WorkspaceCodePanel.kt` 只保留装配和少量 page-level glue code
- 拆出的文件即使暂时继续接收 `WorkspaceSceneViewModel`，也不再新增平台 picker、存储实现或 runtime owner 逻辑
- 截至 2026-03-11，上述完成标准已基本满足，阶段 6 可视为完成

## 阶段 7：导航模型与实体解耦

目标：
- 清理阶段 1 之后仍残留的 entity 暴露点，完成 scene model 与 Room entity 解耦

任务：
- 若阶段 1 尚未完成，则将 `Scenes.WorkScene(val entity: WorkspaceEntity)` 改为更轻的导航参数
- 优先考虑：
- `workspaceId`
- `workspaceName`
- 或 `WorkspaceRouteArgs`
- 页面进入后由 repository 读取详情
- 梳理 `WorkspaceSearchModels.kt`、类树节点、列表项等 scene-facing model，逐步移除对 `ClassesEntity` / `WorkspaceEntity` 的直接暴露

完成标准：
- 导航对象不直接传递 Room entity
- 页面模型与持久化层边界更清晰

## 阶段 8：收尾与清理

目标：
- 统一命名、删除过渡代码、补充文档

任务：
- 清理无用扩展与旧 helper
- 清理过渡期保留的重复逻辑
- 核对包名是否符合职责
- 核对 import 分组与排序
- 核对 Android / JVM actual 是否仍遵循同一套 scene contract
- 在 schema 设计稳定后，清理并重新导出 `sharedUI/schemas/*/1.json`，将当前结构固化为新的 `version = 1` 基线
- 若部分状态已迁移到 `KStore`，同步清理旧的 `.db` / `.json` 命名、路径和废弃适配代码
- 补充 `sharedUI` 的模块说明文档

截至 2026-03-11：
- `WorkspaceSceneViewModel` 中导航失败 / 超时 / 不支持提示已统一改走 `WorkspaceUiEffect.ShowMessage`
- `WorkspaceSceneContent.kt`、`WorkspaceSettingsDialog.kt` 已开始直接消费 `WorkspaceUiState`
- `WorkspaceUiState.kt` 已升级为 workspace 场景唯一主聚合状态，`headerUiState` / `sidePanelUiState` / `codePanelUiState` 已作为子状态收口到其中
- `WorkspaceSceneContent.kt` 已改为只收集单个 `uiState`，不再从 `WorkspaceSceneViewModel` 并行订阅多组 page-level `StateFlow`
- `WorkSidePanel(...)`、`WorkCodePanel(...)` 这类过渡命名已回收为与文件名一致的 `Workspace*`
- `WorkspaceNavigationPresentation.kt` 已开始承接 target kind / reveal plan 这类 scene-facing 纯页面决策
- `WorkspaceSidePanelPersistence.kt` 已开始承接侧边栏状态恢复 / 持久化映射
- `WorkspaceSidePanelUiState` / `WorkspaceCodePanelUiState` 已开始承接侧边栏 / 代码面板的 scene-facing 子状态
- `WorkspaceCodePaneUiState` / `buildWorkspaceCodePaneUiStates(...)` 已开始承接 editor pane 的代码内容与编辑器快照映射
- `EditorStateRepository.getContentStateSnapshot(...)` 已落地，editor pane 所需的滚动 / 光标 / 选区 / 搜索高亮读取不再散落在 scene / UI 层
- `CodeViewPane.kt` / `CodeViewPager.kt` 已开始直接消费 pane `UiState`，不再通过 `WorkspaceSceneViewModel` 暴露一组 `getScrollOffsetY(...)` / `getSelection(...)` 风格的读取桥接
- `WorkspaceTabBarUiState` / `WorkspaceTabBarItemUiState` 已开始承接 tab bar / pager 的 page-level 决策
- `WorkspaceCodePanel.kt` 已开始接管 pager 选中页同步，`WorkspaceTabBar.kt` 已收回为 tab bar 渲染 / 事件装配
- UI 文件已开始优先消费子 `UiState`，旧的零散 flow 出口正在收窄
- `WorkspaceSceneViewModel` 中目标 tab 加载与 reveal 的重复流程已开始通过共用 helper 收口
- `WorkspaceSceneViewModel` 中一批 loading / search 页面样板已开始通过共用 helper 收口
- `WorkspaceSceneViewModel` 中 session mutation 与 tabs 刷新的重复流程已开始通过共用 helper 收口
- `WorkspaceSceneViewModel` 中 cleanup / fire-and-forget 保存样板已开始通过共用 helper 收口
- `WorkspaceSceneViewModel` 中 IO / Result 处理样板已开始通过 `runIo(...)` / `runIoCatching(...)` 收口
- `ClassVisualKind` 已迁到 `core/workspace`，scene / core / node 层不再从 `database.classindex.entities` 引入该枚举
- 截至 2026-03-13，阶段 8 已完成；`WorkspaceSceneViewModel` 体量仍偏大，但剩余主要是页面编排体量问题，不再是边界泄漏或状态出口未收口问题

完成标准：
- 主要能力有固定入口
- 页面、业务、持久化、平台兼容层边界清晰
- `WorkspaceSceneViewModel` 的页面主状态出口默认收口为单个 `StateFlow<WorkspaceUiState>`；除 `WorkspaceUiEffect` 和 editor patch stream 外不再并行暴露多组 page-level `StateFlow`
- scene / core / node 层不再依赖 `database` 包中的 `ClassVisualKind`

## 推荐实施顺序

推荐严格按以下顺序进行：

补充说明：
- `Scenes.WorkScene` 的导航入参解耦建议并入阶段 1 一起做，阶段 7 主要处理剩余页面模型 / 搜索结果 / 类树节点中的 entity 泄露点

1. 阶段 0：安全网
2. 阶段 1：repository / service 边界
3. 阶段 2：Home 场景
4. 阶段 3：Workspace 初始化与类索引
5. 阶段 4：标签页、代码内容、编辑状态
6. 阶段 5：定义跳转、搜索、设置
7. 阶段 6：Workspace UI 文件拆分
8. 阶段 7：导航解耦
9. 阶段 8：收尾

不要倒序做。

原因：
- 先拆 UI 而不先拆业务，会把大文件拆成很多个仍然高度耦合的小文件
- 先改导航对象而不先建立 repository，容易造成页面入口代码来回改
- 先不明确存储分类就拆 repository，容易把“旧数据库名”错误固化进新抽象
- 先做 Gradle 模块化只会把当前耦合放大成跨模块耦合

## 每阶段验收标准

每完成一个阶段，都应检查以下事项：
- `sharedUI` JVM 编译通过
- `sharedUI` Android 编译通过
- 已有 `jvmTest` 通过
- 关键页面行为未回退
- 关键资源在页面关闭后能正常释放
- 新增文件职责明确，命名与内容一致
- 没有继续把新逻辑堆回 `WorkspaceSceneViewModel`

## 建议人工验证清单

在自动化测试仍不完整的阶段，建议每阶段至少回归以下路径：
- 新建工作区（导入 dex / apk）
- 打开已有工作区
- 类树展开、滚动、恢复状态
- 打开 / 切换 / 关闭标签页
- Java / Smali 代码加载与高亮
- 定义跳转
- 类名搜索、字符串搜索及结果定位
- 修改设置并重启后验证设置恢复
- 关闭页面后再次进入，确认编辑会话和资源释放无异常

建议每阶段至少执行：

```bash
./gradlew :sharedUI:compileKotlinJvm
./gradlew :sharedUI:compileAndroidMain
./gradlew :sharedUI:jvmTest
```

## 分提交建议

建议按以下粒度提交，而不是一次性大改：

1. `test(sharedUI): add shared test infra for tree, navigation, and search regression`
2. `refactor(sharedUI): define storage contracts and migrate app settings to repository`
3. `refactor(sharedUI): introduce scene contracts and lightweight navigation args`
4. `refactor(sharedUI): extract home workspace import flow`
5. `refactor(sharedUI): extract workspace initialization and class index services`
6. `refactor(sharedUI): extract editor session, tab, and code content services`
7. `refactor(sharedUI): extract navigation, search, and workspace side effects`
8. `refactor(sharedUI): split workspace ui files and finalize scene-facing models`

## 约束

重构期间应持续遵守以下约束：
- 不做 Gradle 模块化
- 不引入新的重量级框架
- 允许破坏性数据库调整；不为兼容旧本地库保留迁移脚本、过渡表和过渡字段
- 不把现有 `workspace_index.db`、`classes.db`、`editor_session.db`、`setting.json` 视为必须保留的最终形态
- 不做无测试兜底的大面积逻辑搬运
- 不在一个提交里同时改动 UI、业务、数据库模型三条主线
- 单个新类必须有明确职责说明
- 新增 repository / service 时，优先先写接口或清晰的职责边界，再迁移旧代码

## 第一批建议落地项

如果从下一次改动开始正式进入重构，建议第一批只做这些事情：

1. 建立 `sharedUI/src/jvmTest`
2. 在 `sharedUI/build.gradle.kts` 补齐测试依赖，并准备基础 fixture
3. 给 `ClassTreeNode`、导航声明解析、字符串搜索辅助函数补测试
4. 梳理 source of truth / cache / session / config 的最小存储分类
5. 新建 `core/settings/AppSettingsRepository.kt`，优先落地 `KStore` 版实现
6. 新建 `core/workspace/WorkspaceRepository.kt`
7. 新建 `core/editor/EditorSessionRepository.kt`
8. 定义 `WorkspaceRouteArgs` / `WorkspaceSummary` 与 `HomeUiState` / `HomeUiEffect`、`WorkspaceUiState` / `WorkspaceUiEffect` 基线
9. 建立最小依赖装配入口
10. 把 `HomeSceneViewModel` 先改成通过 repository 工作，并移出文件选择器调用

这批完成后，再进入 Workspace 主流程拆分。
