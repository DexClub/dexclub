# sharedUI 移动端实现计划

最后更新：2026-03-21

## 结论

- `sharedUI` 目前不是“没有 Android 端”，而是“Android 平台能力已接通，但主界面仍按桌面交互组织”。
- 2026-03-21 已本地验证 `./gradlew :sharedUI:compileKotlinJvm :sharedUI:compileAndroidMain` 可以通过。
- 最合适的实现路线不是复制一套 Android 专属业务逻辑，而是复用现有 `commonMain` 业务层，在 `commonMain` 新增自适应 UI，并在 `androidMain` 补齐少量平台行为。

## 范围

- 本计划只覆盖当前项目中的 Android 移动端。
- 第一优先级是竖屏手机可用。
- 平板和横屏尽量复用同一套自适应布局，不单独做第二套页面。
- 不包含 iOS。
- 不重写 `HomeSceneViewModel`、`WorkspaceSceneViewModel`、`core/*`、`data/*`。
- 首版不追求和桌面端交互完全一致，允许在手机上对双栏、右键、悬浮菜单做降级。

## 现状梳理

### 已经可复用的部分

- 导航与入口已经有 Android actual：
  - `sharedUI/src/androidMain/kotlin/io/github/dexclub/app/App.android.kt`
  - `sharedUI/src/androidMain/kotlin/io/github/dexclub/app/scene/workspace/WorkspaceScene.android.kt`
  - `androidApp/src/main/kotlin/io/github/dexclub/android/MainActivity.kt`
- Android 平台文件、存储、日志能力已经存在：
  - `sharedUI/src/androidMain/kotlin/io/github/dexclub/Env.android.kt`
  - `sharedUI/src/androidMain/kotlin/io/github/dexclub/database/Database.android.kt`
  - `sharedUI/src/androidMain/kotlin/io/github/dexclub/data/settings/AppSettingsStoreFactory.android.kt`
  - `sharedUI/src/androidMain/kotlin/io/github/dexclub/data/workspace/WorkspaceStoreFactory.android.kt`
  - `sharedUI/src/androidMain/kotlin/io/github/dexclub/compat/FileKitCompat.android.kt`
  - `sharedUI/src/androidMain/kotlin/io/github/dexclub/Logger.android.kt`
- 业务层已经与平台解耦，可直接复用：
  - `sharedUI/src/commonMain/kotlin/io/github/dexclub/app/scene/home/HomeSceneViewModel.kt`
  - `sharedUI/src/commonMain/kotlin/io/github/dexclub/app/scene/workspace/WorkspaceSceneViewModel.kt`
  - `sharedUI/src/commonMain/kotlin/io/github/dexclub/app/di/SharedUiDependencies.kt`
  - `sharedUI/src/commonMain/kotlin/io/github/dexclub/core/*`
  - `sharedUI/src/commonMain/kotlin/io/github/dexclub/data/*`
- `code-view` 已经有 Android 源集并参与编译，说明代码阅读器不是移动端的零起点。

### 当前业务主链

- 首页主链：
  - 加载工作区列表
  - 导入 apk/dex 创建工作区
  - 打开已有工作区
  - 删除工作区
- Workspace 主链：
  - `WorkspaceRouteArgs` 转 `WorkspaceSceneContext`
  - `WorkspaceInitializer.bootstrap(...)` 初始化类索引和会话
  - 恢复侧边栏状态和已打开标签页
  - 按需导出 Java / Smali 代码
  - 类名搜索、字符串搜索
  - 定义跳转
  - 编辑器滚动位置、光标、选区和设置持久化
  - 工作区日志导出

### 当前真正缺口

- `HomeScene.kt` 和 `WorkspaceList.kt` 仍是桌面居中布局，且列表宽度固定为 `350.dp`。
- `NewWorkspaceDialog.kt`、`DeleteWorkspaceDialog.kt`、`WorkspaceSettingsDialog.kt`、`WorkspaceSearchDialog.kt` 仍偏桌面对话框范式。
- `WorkspaceSceneContent.kt` 固定为“左侧类树 + 中间拖拽分割条 + 右侧代码区”的桌面双栏结构。
- `ClassTreeList.kt` 使用常驻横纵滚动条，这更像桌面交互。
- `CodeViewPager.kt` 在 `OpenTabMode.MIXED` 下固定双 Pane 并排显示，不适合手机窄屏。
- `CodeContextMenu.kt` 依赖指针位置弹出上下文菜单，手机上语义不稳定。
- `WorkspaceTabBarItem.kt` 同时兼容右键与长按；右键分支对 Android 没价值。
- `App.android.kt` 里 `NavDisplay.onBack` 目前是空实现，物理返回没有真正接入页面栈。

## 推荐实现方向

- 不复制一套 Android 专属 `WorkspaceScene`。
- 继续保持 ViewModel、service、repository 共享。
- 在 `commonMain` 增加“紧凑布局 / 宽屏布局”的自适应层。
- 让桌面端和 Android 宽屏继续走现有双栏模型。
- 让 Android 手机走紧凑模型，对以下交互做降级：
  - 不显示拖拽分割条
  - 不显示常驻滚动条
  - 不在窄屏下并排展示双 Pane
  - 不依赖右键菜单

## 目标形态

### Home 页面

- 手机端改为顶部起始布局，不再垂直居中。
- 工作区列表宽度改为填充可用宽度，并保留一个合理的最大宽度。
- “新建工作区”和“打开工作区”改为更适合手机的按钮布局。
- 新建、删除确认类弹层优先改为自适应大弹窗；如果体验不够，再切为全屏页或底部面板。

### Workspace 页面

- 手机端使用单主内容区，不再同时显示类树和代码区。
- 类树改为抽屉或模态侧栏。
- 顶部栏保留工作区标题，并承载返回、搜索、设置、更多操作。
- 代码区保持主舞台，标签页仍可横向滚动。
- `OpenTabMode.MIXED` 在紧凑模式下只显示当前激活 Pane，通过按钮或菜单切换 Java / Smali / Mixed 的当前可见内容。
- 搜索弹层在手机端优先做大尺寸弹窗或全屏搜索页，而不是桌面式中等宽度对话框。

## 具体落地方案

### 阶段 1：建立自适应布局基线

- 在 `commonMain` 新增布局模式定义，例如：
  - `WorkspaceLayoutMode.Compact`
  - `WorkspaceLayoutMode.Medium`
  - `WorkspaceLayoutMode.Expanded`
- 基于 `BoxWithConstraints` 或等价方式在 scene 内部判定布局模式。
- 将 `WorkspaceSceneContent.kt` 拆成：
  - 共享状态装配层
  - `Compact` 布局实现
  - `Expanded` 布局实现
- 将拖拽分割条逻辑只保留在 `Expanded`。

### 阶段 2：先把 Home 做成可用手机页

- 改造 `HomeScene.kt` 的整体布局，让内容顶对齐并支持窄屏。
- 改造 `WorkspaceList.kt`，移除固定 `350.dp` 宽度。
- 改造 `NewWorkspaceDialog.kt` 和 `DeleteWorkspaceDialog.kt`，让宽度按屏幕自适应。
- 这一阶段完成后，移动端至少应该能稳定完成：
  - 查看工作区列表
  - 新建工作区
  - 点击列表进入工作区
  - 删除工作区

### 阶段 3：完成 Workspace 紧凑布局

- 为 `WorkspaceSceneContent.kt` 增加 `Compact` 版本。
- 将 `WorkspaceSidePanel.kt` 放入抽屉或模态侧栏。
- 保留现有 `WorkspaceCodePanel.kt` 作为主体，但对紧凑模式增加单 Pane 展示策略。
- 在 `CodeViewPager.kt` 中把紧凑模式下的 `MIXED` 改为“只展示 active pane”。
- `ClassTreeList.kt` 在紧凑模式下去掉 `HorizontalScrollbar` 和 `VerticalScrollbar`。

### 阶段 4：改造移动端交互

- Android 返回行为接入：
  - `App.android.kt` 的 `NavDisplay.onBack`
  - 必要时补 `BackHandler`
- `CodeContextMenu.kt` 做自适应：
  - 宽屏继续用现有上下文菜单
  - 紧凑模式改为更稳妥的菜单或操作面板
- `WorkspaceTabBarItem.kt` 中保留长按，弱化或移除 Android 侧无意义的右键语义。
- `WorkspaceSearchDialog.kt` 改为更适合手机的尺寸策略。
- `WorkspaceSettingsDialog.kt` 改为更适合手机的尺寸策略。

### 阶段 5：回归与补边角

- 验证旋转屏幕后布局切换是否稳定。
- 验证从搜索结果进入代码后，抽屉、标签、滚动位置是否正常。
- 验证代码查看器在 Android 上的点击、长按、定义跳转、滚动是否可用。
- 验证日志导出目录选择在 Android SAF 下是否顺畅。

## 建议新增或重点改造的文件

- 建议新增：
  - `sharedUI/src/commonMain/kotlin/io/github/dexclub/app/scene/workspace/WorkspaceLayoutMode.kt`
  - `sharedUI/src/commonMain/kotlin/io/github/dexclub/app/scene/workspace/WorkspaceSceneCompact.kt`
  - `sharedUI/src/commonMain/kotlin/io/github/dexclub/app/scene/workspace/WorkspaceSceneExpanded.kt`
- 重点改造：
  - `sharedUI/src/commonMain/kotlin/io/github/dexclub/app/scene/home/HomeScene.kt`
  - `sharedUI/src/commonMain/kotlin/io/github/dexclub/app/scene/home/WorkspaceList.kt`
  - `sharedUI/src/commonMain/kotlin/io/github/dexclub/app/scene/home/NewWorkspaceDialog.kt`
  - `sharedUI/src/commonMain/kotlin/io/github/dexclub/app/scene/home/DeleteWorkspaceDialog.kt`
  - `sharedUI/src/commonMain/kotlin/io/github/dexclub/app/scene/workspace/WorkspaceSceneContent.kt`
  - `sharedUI/src/commonMain/kotlin/io/github/dexclub/app/scene/workspace/ClassTreeList.kt`
  - `sharedUI/src/commonMain/kotlin/io/github/dexclub/app/scene/workspace/CodeViewPager.kt`
  - `sharedUI/src/commonMain/kotlin/io/github/dexclub/app/scene/workspace/CodeContextMenu.kt`
  - `sharedUI/src/commonMain/kotlin/io/github/dexclub/app/scene/workspace/WorkspaceSearchDialog.kt`
  - `sharedUI/src/commonMain/kotlin/io/github/dexclub/app/scene/workspace/WorkspaceSettingsDialog.kt`
  - `sharedUI/src/androidMain/kotlin/io/github/dexclub/app/App.android.kt`

## 明确不建议的做法

- 不建议把 `WorkspaceSceneViewModel` 再拆出一份 Android 版本。
- 不建议把大部分页面直接复制到 `androidMain` 维护两套 UI。
- 不建议在首版手机适配时强保留桌面交互细节，例如拖拽分栏、右键菜单、并排双代码窗格。
- 不建议为了移动端去重写 `WorkspaceInitializer`、`OpenTabService`、`NavigationService` 这类已经稳定的共享能力。

## 验收标准

- Android 手机上可完成从首页到工作区的完整主链。
- 类树在手机上可打开、滚动、展开、点击进入类。
- 代码区在手机上可浏览并切换标签。
- Java / Smali 至少可以稳定切换查看。
- 搜索、定义跳转、日志导出在 Android 上可完成基本闭环。
- 物理返回键行为符合预期。
- `./gradlew :sharedUI:compileKotlinJvm :sharedUI:compileAndroidMain` 持续通过。

## 风险与注意事项

- `code-view` 虽已能参与 Android 编译，但触摸选择、长按菜单、软键盘行为仍需真机或模拟器验证。
- `HomeSceneViewModel.onOpen(...)` 的“打开已有工作区”在 Android 上不是主路径，首版应优先保证“工作区列表点击进入”这条主路径顺滑。
- 如果现有 `ContextMenu` 在 Android 上体验明显不佳，应果断改为更简单的操作面板，而不是继续强行对齐桌面。
- 如果 `MIXED` 模式在手机上体验不稳定，首版允许只暴露“当前激活视图切换”，暂不提供真正的双 Pane 同屏。

## 推荐执行顺序

1. 先做 `Home` 页面移动端适配，尽快打通手机端进入工作区的链路。
2. 再做 `Workspace` 紧凑布局，把类树挪入抽屉，把代码区做成单主舞台。
3. 然后处理 `MIXED`、搜索弹层、设置弹层、上下文菜单这类手机交互。
4. 最后补 Android 返回行为、旋转屏、日志导出和回归验证。
