# sharedUI 重构进度

最后更新：2026-03-13

## 这份文件怎么用

这份文件只给下次会话快速接力用，不重复写完整设计。

- 完整阶段计划、边界原则、目录目标：看 `sharedUI/REFACTOR_PLAN.md`
- 这份文件只回答 6 件事：
  1. 当前阶段做到哪
  2. 哪些边界已经稳定
  3. 最近一轮刚完成什么
  4. 现在真正卡在哪
  5. 下次第一步直接做什么
  6. 哪些事情不要回头做

## 30 秒结论

- 阶段 0：已完成
- 阶段 1：已完成
- 阶段 2：已完成
- 阶段 3：已完成
- 阶段 4：已完成
- 阶段 5：已完成
- 阶段 6：已完成
- 阶段 7：主链已完成
- 阶段 8：已完成

一句话结论：

- 已经从"scene 直接碰数据库 / 运行时 / picker"推进到"scene -> repository / service / runtime owner -> storage"。阶段 8 的死代码、冗余包装、过渡命名清理已全部收尾。
- `WorkspaceUiState` 已对齐为 workspace 场景唯一主聚合状态，`WorkspaceSceneContent` 现只收集单个 `uiState`。
- 工作区清单存储已从 `workspace_index.db` 收口到 `KStore` 驱动的 `workspaces.json`；旧的 workspace index Room repository / database / schema 已移除。
- 本轮继续补掉 app 层残留的 editorsession entity 泄漏：`OpenTabUiModel`、side panel 持久化请求都已改为轻量 scene/core 契约，不再直接暴露或拼装 Room entity。
- 本轮继续把 editorsession 边界从 app 向 core 下压：`EditorSessionRepository`、`OpenTabService`、`WorkspaceInitializer`、`CodeContentService` 已改为消费 core session record，Room entity 现只保留在 `data/editorsession` / `database/editorsession`。
- 本轮继续把 classindex 边界从 core 下压到 data：`WorkspaceClassIndexRepository`、`WorkspaceIndexService` 已改为消费 `WorkspaceIndexedClassRecord`，`ClassesEntity` 现只保留在 `data/classindex` / `database/classindex`。
- `ClassVisualKind` 已迁到 `core/workspace`，scene / core / node 不再从 `database.classindex.entities` 引入这个页面可见枚举。
- `sharedUI/REFACTOR_PLAN.md` 当前只定义到阶段 8；这份进度文件不再使用之前那个不存在的“阶段 9（Gradle 模块化）”说法。

当前最重要的判断：

- 不要再回头拆 editor runtime、DexKit owner、navigation service、search service，这些边界已经收口。
- 不要再回头重做 Workspace UI 主文件拆分，`WorkspaceSidePanel.kt`、`WorkspaceCodePanel.kt`、`WorkspaceSearchDialog.kt`、`CodeViewPane.kt` 已在可维护区间。
- Android / JVM actual 已共享主体布局，不再是"一个成熟、一个占位"的状态。
- `WorkspaceSceneViewModel` 已全文扫描确认：无更多可清理的死函数、冗余派生、过渡命名。

## 已稳定的边界

### 稳定入口

- 导航入口：
  - `app/navigation/Scenes.kt`
  - `app/navigation/WorkspaceRouteArgs.kt`
- 依赖装配入口：
  - `app/di/SharedUiDependencies.kt`
- scene contract：
  - `HomeUiState` / `HomeUiEffect`
  - `WorkspaceUiState` / `WorkspaceUiEffect`
- 首页轻量模型：
  - `app/model/WorkspaceSummary.kt`

### 已抽出的核心能力

这些能力已经稳定，不要回头重拆：

- `core/settings/AppSettingsRepository.kt`
- `core/workspace/WorkspaceRepository.kt`
- `core/workspace/WorkspaceImporter.kt`
- `core/workspace/WorkspaceClassIndexRepository.kt`
- `core/workspace/WorkspaceClassSource.kt`
- `core/workspace/WorkspaceIndexService.kt`
- `core/workspace/WorkspaceInitializer.kt`
- `core/workspace/WorkspaceDexKitRuntime.kt`
- `core/workspace/WorkspaceIndexedClassRecord.kt`
- `core/editor/EditorSessionRepository.kt`
- `core/editor/OpenTabService.kt`
- `core/editor/CodeContentService.kt`
- `core/editor/CodeContentRuntime.kt`
- `core/editor/EditorStateRepository.kt`
- `core/editor/HighlightCoordinator.kt`
- `core/navigation/DeclarationResolver.kt`
- `core/navigation/resolver/DeclarationResolvers.kt`
- `core/navigation/NavigationService.kt`
- `core/search/ClassSearchService.kt`
- `core/search/StringSearchService.kt`
- `core/search/StringSearchLocationResolver.kt`

### Workspace 场景当前稳定状态

- `WorkspaceSceneViewModel` 已接收 `WorkspaceSceneContext`，不再对外暴露 `routeArgs`。
- `WorkspaceSceneContext.kt` 已收口 `WorkspaceRouteArgs -> scene context` 映射，以及 `workspaceId <= 0L` 的归一化。
- `SharedUiDependencies.createWorkspaceSceneViewModel(...)` 已负责 `WorkspaceRouteArgs -> WorkspaceSceneContext` 组装。
- 设置加载 / 保存已通过 `AppSettingsRepository`。
- 类索引访问已通过 `WorkspaceClassIndexRepository`。
- 初始化编排已通过 `WorkspaceInitializer`。
- 类索引检查 / 构建 / 读取已通过 `WorkspaceIndexService`。
- 打开标签页、恢复 session、kind 优先级已通过 `OpenTabService`。
- 代码内容读取、缓存、generation 控制、runtime state 清理已通过 `CodeContentService` + `CodeContentRuntime`。
- 滚动位置、光标、选区、搜索高亮、保存防抖已通过 `EditorStateRepository`。
- `HighlightEngine` 接线、patch merge、viewport / consumer 转发已通过 `HighlightCoordinator`。
- DexKit bridge 的创建、复用、关闭已通过 `WorkspaceDexKitRuntime`。
- 声明跳转目标准备、目标类规范化、目标落点推断已通过 `NavigationService`。
- DexKit 类名搜索、字符串搜索与结果映射已通过 `core/search`。
- 日志导出目录选择仍由 scene callback / 平台层处理，ViewModel 不再直接持有 picker / FileKit。

### UI 与 scene-facing 状态当前稳定状态

- `WorkspaceSceneContent.kt` 已是 workspace 主体的唯一 scene-level 装配点：
  - 统一收集单个 `WorkspaceUiState`
  - 统一装配 header / side / code callback contract
- `WorkspaceUiState.kt` 已升级为 workspace 场景唯一主聚合状态，统一承接 loading / header / side / code 子状态。
- `WorkspaceHeaderUiState.kt` 已承接工作区标题、显示路径、搜索弹窗状态、设置弹窗状态。
- `WorkspaceSettingsUiState.kt` 已收窄为显式 scene-facing 字段，不再直接透传 `AppSettings`。
- `WorkspaceSearchDialogUiState.kt` 已承接搜索弹窗 tab / query / result / error / loading 状态。
- `WorkspaceSidePanelUiState.kt` 已承接 panel state，`buildWorkspaceSidePanelUiState(...)` 已落地。
- `WorkspaceCodePanelUiState.kt` 已承接 code panel page-level state，`buildWorkspaceCodePanelUiState(...)` 已落地。
- `WorkspaceCodePaneUiState` / `buildWorkspaceCodePaneUiStates(...)` 已承接 editor pane 的代码内容与编辑器快照。
- `WorkspaceTabBarUiState` / `WorkspaceTabBarItemUiState` / `buildWorkspaceTabBarUiState(...)` 已承接 tab bar / pager 的 page-level 决策。
- `WorkspaceSidePanel.kt` / `WorkspaceCodePanel.kt` 已回收为纯 `UiState + callbacks` 装配。
- 直接持有 `WorkspaceSceneViewModel` 的 UI 文件已基本收敛到：
  - `WorkspaceScene.kt`
  - `WorkspaceSceneContent.kt`
- `OpenTabUiModel` 已改为轻量 scene-facing model；不再直接持有 `OpenTabEntity` / `OpenTabContentEntity` / `OpenTabPaneEntity`。
- `EditorSessionSidePanelSnapshot` / `EditorSessionSidePanelPersistRequest` 已承接 side panel 读写契约；`WorkspaceSidePanelPersistence.kt` 不再直接拼 `SidePanelStateEntity` / `SidePanelExpandedPathEntity`。
- `EditorSessionModels.kt` 已定义 `EditorSessionTabRecord` / `EditorSessionContentRecord` / `EditorSessionPaneRecord` / `EditorSessionKindPriorityRecord`。
- `EditorSessionRepository` 不再向 core 暴露 editorsession Room entity。
- `RoomEditorSessionRepository` 已退化为纯 record <-> Room entity 映射层。
- `WorkspaceClassIndexRepository` 不再向 core 暴露 `ClassesEntity`。
- `RoomWorkspaceClassIndexRepository` 已退化为 `WorkspaceIndexedClassRecord` <-> `ClassesEntity` 的映射层。
- `ClassVisualKind` 已从 `database` 包迁到 `core/workspace`，scene/core/node 层不再依赖 data/database 命名空间里的视觉枚举。

### `WorkspaceSceneViewModel` 样板整理状态

**现有 helper（均有多处调用，正常保留）：**

- 日志 helper：`logDebug` / `logInfo` / `logWarn` / `logError`（注入 TAG + workspace 上下文，不是纯透传）
- loading / message：`updateLoadingMessage` / `updateThrowableLoadingFailure`
- action failure：`handleTaskFailure`
- task 启动：`launchTask` / `launchLoadingTask` / `launchCancelableLoadingTask`
- task + failure：`launchWarnTask` / `launchErrorTask` / `launchWarnLoadingTask` / `launchErrorLoadingTask` / `launchWarnCancelableLoadingTask`
- 基础：`launchHandledTask`
- IO / cleanup：`runIo` / `runIoCatching` / `runTabSessionMutation` / `runBlockingCleanupStep` / `runCleanupStep`

**已移除的无保留价值过渡代码：**

- `contentKey(...)` / `normalizeSemanticNode(...)` — 无语义过渡 helper
- `emitEffect(...)` — 只被 `emitMessageEffect` 调用一次的单行包装
- `hydrateEditorStates(...)` — 只被调用一次的单行透传
- `buildWorkspaceHeaderUiState(...)` — 纯透传 builder，无字段转换逻辑
- `findTargetClassRecord(...)` (ViewModel 私有) — 单调用，输入已归一化，直接改用 `workspaceIndexService.findByName`
- `NavigationRequest.createdAt` — 死字段，只赋值不读取

## 最近一轮刚完成

阶段 8 全部死代码、冗余包装、遗留命名清理已收尾。最近几轮继续把 storage 实现边界往 data 层压：

- `605020c`
  - `refactor(sharedUI): remove remaining editorsession entity leaks`
  - app 层不再直接碰 `OpenTabEntity` / `OpenTabContentEntity` / `OpenTabPaneEntity` / side panel editorsession entity
- `941c485`
  - `refactor(sharedUI): move editor session records into core`
  - `EditorSessionRepository`、`OpenTabService`、`CodeContentService`、`WorkspaceInitializer` 已切到 core session record
- `e9e8a0b`
  - `refactor(sharedUI): move class index records into core`
  - `WorkspaceClassIndexRepository`、`WorkspaceIndexService` 已切到 `WorkspaceIndexedClassRecord`
- `working tree`
  - `refactor(sharedUI): move app settings storage to kstore`
  - `DefaultAppSettingsRepository` 已改为直接基于 `KStore` 读写 `setting.json`
  - `AppSettingsStore` 过渡对象已移除，`AppSettings` 已拆到独立模型文件
  - 已补 `DefaultAppSettingsRepositoryTest`，覆盖已有值读取、空存储默认值回写、损坏存储回退、保存写回
- `working tree`
  - `refactor(sharedUI): move workspace repository to kstore`
  - `DefaultWorkspaceRepository` 已改为直接基于 `KStore` 读写 `workspaces.json`
  - `RoomWorkspaceRepository`、`WorkspaceIndexDatabase`、`WorkspaceDao`、`WorkspaceEntity` 和对应 schema / 测试已移除
  - 已补 `DefaultWorkspaceRepositoryTest`，覆盖插入、按 id 查询、删除、id 递增
- `working tree`
  - `refactor(sharedUI): align workspace scene state contract`
  - `WorkspaceUiState` 已聚合 header / side / code 子状态，`WorkspaceSceneContent` 改为只消费单个 `uiState`
- `working tree`
  - `refactor(sharedUI): move class visual kind out of database package`
  - `ClassVisualKind` 已迁到 `core/workspace`，`database/classindex/entities` 仅保留 `ClassesEntity -> visualKind` 映射

到这一轮为止：

- app 层无 `WorkspaceEntity` / `ClassesEntity` / editorsession Room entity 泄漏
- core 层无 `ClassesEntity` / editorsession Room entity 泄漏
- Room entity 主要只留在 `data/*` 与 `database/*`
- `ClassVisualKind` 不再作为 `database` 包类型向 scene / core / node 外溢
- 设置存储已完成从手写 `AppSettingsStore` 向 `KStore` 的 data 层收口
- 工作区清单存储已完成从 `workspace_index.db` 向 `KStore` 的 data 层收口

验证已通过：

```bash
./gradlew :sharedUI:compileKotlinJvm --rerun-tasks --no-build-cache
./gradlew :sharedUI:jvmTest --rerun-tasks --no-build-cache
./gradlew :sharedUI:compileAndroidMain --rerun-tasks --no-build-cache
```

## 当前停留点

计划内阶段 0-8 已完成。当前没有必须继续追的 scene/core 边界遗留项。当前状态：

- `WorkspaceSceneViewModel` 全文已确认干净：无死函数、无冗余派生、无过渡命名。
- `WorkspaceSceneViewModel` 的页面主状态出口已收口为单个 `uiState`；其余公开流仅保留 `effects` 与 editor patch stream。
- app 层无 `ClassesEntity` / `WorkspaceEntity` / `OpenTabEntity` / `OpenTabContentEntity` / `OpenTabPaneEntity` / `SidePanelStateEntity` / `SidePanelExpandedPathEntity` 泄漏。
- core 层无 `OpenTabEntity` / `OpenTabContentEntity` / `OpenTabPaneEntity` / `OpenTabKindPriorityEntity` 泄漏。
- core 层无 `ClassesEntity` 泄漏。
- scene / core / node 层无 `database.classindex.entities.ClassVisualKind` 泄漏。
- `WorkspaceSearchModels.kt`、`NavigationModels.kt`、`WorkspaceNavigationPresentation.kt`、`WorkspaceSceneContext.kt` 均干净。
- 无 TODO / FIXME / HACK / TEMP 标记。

如果还要继续做“sharedUI 内部重构”，当前仅剩偏 data 层的可选收尾项，优先级从高到低大致是：

- 文档一致性
  - 若后续继续演进存储实现，记得同步改 `REFACTOR_PLAN.md` 的存储策略建议，而不是只改进度文件

## 下次直接开做

如果下次会话仍然以“继续 sharedUI 重构”为目标，不要先找不存在的“阶段 9”。先按下面顺序判断：

1. 先读：
   - `sharedUI/REFACTOR_PLAN.md`
   - `sharedUI/REFACTOR_PROGRESS.md`
2. 确认本次目标是不是还在“sharedUI 内部重构”
   - 如果不是，直接切去独立任务，不要为了重构而重构
3. 如果还要继续做 sharedUI 内部收尾，优先先做文档一致性
   - 核对 `sharedUI/REFACTOR_PLAN.md` 里的存储策略建议是否要补“工作区清单已切到 KStore”的现状说明
   - 再判断是否还值得继续清理旧命名、旧路径或历史说明
   - 这已经不是 scene/core 主链阻塞，而是文档和实现的一致性问题

不建议下次一上来做：

- Gradle 模块化
- 重新拆 scene / UI 文件
- 重新设计 navigation / search / editor runtime 边界

## 不要回头做

- 不要回头重复拆 search service。
- 不要回头重复拆 navigation service。
- 不要回头重复拆 editor runtime。
- 不要回头重做 Workspace UI 主文件拆分。
- 不要为了"更纯"继续硬拆 `WorkspaceSceneContent.kt`。
- 不要再写“阶段 9（Gradle 模块化）”，除非先在 `REFACTOR_PLAN.md` 里正式定义新阶段和边界。
- 不要先碰数据库兼容 / migration。
- 不要把新逻辑重新堆回 `WorkspaceSceneViewModel`。

## 已有测试覆盖

当前已有逻辑测试覆盖：

- `DefaultWorkspaceRepository`
- `ClassTreeNode.parse()` / `flatten()`
- `SignatureUtils`
- Java / Smali 声明解析
- `WorkspaceSummary -> WorkspaceRouteArgs`
- `WorkspaceSceneContext`（route → scene context 字段透传与非法 workspaceId 归一化）
- `WorkspaceIndexService`
- `WorkspaceInitializer`
- `OpenTabService`
- `CodeContentRuntime`
- `EditorStateRepository`
- `HighlightCoordinator`
- `NavigationService`
- `ClassSearchService`
- `StringSearchService`
- `StringSearchLocationResolver`
- `WorkspaceNavigationPresentation`
- `WorkspaceCodePanelUiState`
- `WorkspaceSearchDialogUiState`
- `WorkspaceSettingsUiState`
- `WorkspaceSidePanelUiState`
- `WorkspaceTabBarUiState`
- `WorkspaceSidePanelPersistence`

## 下次建议先跑的验证

```bash
./gradlew :sharedUI:compileKotlinJvm --rerun-tasks --no-build-cache
./gradlew :sharedUI:jvmTest --rerun-tasks --no-build-cache
```

如果本轮涉及 Android 共享代码或 actual 合同，再补：

```bash
./gradlew :sharedUI:compileAndroidMain --rerun-tasks --no-build-cache
```

## 环境注意事项

- `commonTest` 已存在，但 Android host test 未开启；当前不影响 `compileKotlinJvm`、`compileAndroidMain`、`jvmTest`。
- Kotlin daemon / KSP / build cache 偶发不稳定。
- `tree-sitter` 的 `jvmJar` 偶发 ZIP 写入失败。
- `BUILD SUCCESSFUL` 之后偶发出现 KSP / AWT 尾部异常。
- 遇到这些问题时，优先直接重跑并加：
  - `--rerun-tasks --no-build-cache`
