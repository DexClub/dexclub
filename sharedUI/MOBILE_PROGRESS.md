# sharedUI 移动端实现进度

最后更新：2026-03-21

对应计划：`sharedUI/MOBILE_PLAN.md`

## 30 秒结论

- 移动端第一批基础改造已完成。
- 当前状态已经从“Android 只是在编译层面接通”推进到“首页与 Workspace 主链具备手机端布局基础”。
- `Workspace` 已具备 `Compact / Expanded` 自适应布局骨架。
- Android 返回行为已接入。
- `./gradlew :sharedUI:compileKotlinJvm :sharedUI:compileAndroidMain` 已通过。
- 还没有做真机 / 模拟器交互回归，`CodeContextMenu` 等移动端细节还需要继续收尾。

## 本轮完成内容

### 1. 建立了 Workspace 自适应布局基线

- 新增 `WorkspaceLayoutMode`，统一定义 `Compact / Medium / Expanded` 三档布局模式。
- `WorkspaceSceneContent` 不再直接内嵌桌面双栏逻辑，而是变成状态装配入口。
- 新增：
  - `sharedUI/src/commonMain/kotlin/io/github/dexclub/app/scene/workspace/WorkspaceLayoutMode.kt`
  - `sharedUI/src/commonMain/kotlin/io/github/dexclub/app/scene/workspace/WorkspaceSceneCompact.kt`
  - `sharedUI/src/commonMain/kotlin/io/github/dexclub/app/scene/workspace/WorkspaceSceneExpanded.kt`
- `WorkspaceSceneContent` 现在会根据可用宽度切换布局实现：
  - 窄屏走 `Compact`
  - 宽屏继续走 `Expanded`

### 2. Workspace 手机端已切到抽屉类树 + 单主代码区

- 手机端不再默认常驻左侧类树。
- 类树已改为 `ModalNavigationDrawer` 形态。
- 顶部栏已增加：
  - 返回
  - 打开类树抽屉
  - 工作区标题
  - 更多动作菜单
- 点击类节点后会关闭抽屉并回到代码区。

涉及文件：

- `sharedUI/src/commonMain/kotlin/io/github/dexclub/app/scene/workspace/WorkspaceSceneCompact.kt`
- `sharedUI/src/commonMain/kotlin/io/github/dexclub/app/scene/workspace/SideHeaderBar.kt`

### 3. 代码区已做第一轮紧凑布局降级

- `WorkspaceCodePanel` 增加布局模式参数。
- `CodeViewPager` 在 `Compact` 下不再把 `MIXED` 模式双 Pane 并排展示。
- 紧凑布局下只展示当前激活 Pane，避免窄屏双栏压缩。
- 侧边树在紧凑布局下关闭常驻横纵滚动条。

涉及文件：

- `sharedUI/src/commonMain/kotlin/io/github/dexclub/app/scene/workspace/WorkspaceCodePanel.kt`
- `sharedUI/src/commonMain/kotlin/io/github/dexclub/app/scene/workspace/CodeViewPager.kt`
- `sharedUI/src/commonMain/kotlin/io/github/dexclub/app/scene/workspace/ClassTreeList.kt`

### 4. Home 页面已做成手机可用布局

- 首页不再固定桌面居中布局。
- 工作区列表移除了固定 `350.dp` 宽度。
- 新建 / 打开工作区操作改成更适合手机的按钮布局。
- 首页内容区改为按屏宽自适应的单列内容容器。

涉及文件：

- `sharedUI/src/commonMain/kotlin/io/github/dexclub/app/scene/home/HomeScene.kt`
- `sharedUI/src/commonMain/kotlin/io/github/dexclub/app/scene/home/WorkspaceList.kt`

### 5. 相关弹层已做响应式宽度调整

- `NewWorkspaceDialog`
- `DeleteWorkspaceDialog`
- `WorkspaceSettingsDialog`
- `WorkspaceSearchDialog`

都已改为基于屏幕宽度计算对话框尺寸，而不是继续写死桌面宽度。

涉及文件：

- `sharedUI/src/commonMain/kotlin/io/github/dexclub/app/scene/home/NewWorkspaceDialog.kt`
- `sharedUI/src/commonMain/kotlin/io/github/dexclub/app/scene/home/DeleteWorkspaceDialog.kt`
- `sharedUI/src/commonMain/kotlin/io/github/dexclub/app/scene/workspace/WorkspaceSettingsDialog.kt`
- `sharedUI/src/commonMain/kotlin/io/github/dexclub/app/scene/workspace/WorkspaceSearchDialog.kt`

### 6. Android 返回链路已接通

- `sharedUI` 的 Android 源集新增 `activity-compose` 依赖。
- Android 入口已接入 `BackHandler`。
- `NavDisplay.onBack` 已开始回收 `backStack`。
- `WorkspaceScene` 在 Android 侧不再忽略 `onBackPressed`。
- 同时补了 `systemBarsPadding()`，避免内容顶到系统栏。

涉及文件：

- `sharedUI/build.gradle.kts`
- `sharedUI/src/androidMain/kotlin/io/github/dexclub/app/App.android.kt`
- `sharedUI/src/androidMain/kotlin/io/github/dexclub/app/scene/workspace/WorkspaceScene.android.kt`
- `sharedUI/src/jvmMain/kotlin/io/github/dexclub/app/scene/workspace/WorkspaceScene.jvm.kt`

## 对计划阶段的映射

### 阶段 1：建立自适应布局基线

- 状态：已完成

完成项：

- 布局模式抽象已建立
- `WorkspaceSceneContent` 已拆为装配层 + 两套布局实现
- 宽屏拖拽分栏逻辑已保留在 `Expanded`

### 阶段 2：先把 Home 做成可用手机页

- 状态：已完成

完成项：

- 首页响应式布局
- 列表宽度自适应
- 新建 / 删除相关弹层响应式

### 阶段 3：完成 Workspace 紧凑布局

- 状态：主链已完成

完成项：

- 抽屉类树已落地
- 代码区已作为主舞台保留
- `MIXED` 模式已做窄屏降级
- 紧凑布局关闭常驻滚动条

未完成：

- 还没有继续细化 `MIXED` 模式下的显式 Java / Smali 切换入口

### 阶段 4：改造移动端交互

- 状态：部分完成

已完成：

- Android 返回行为接入
- 搜索 / 设置弹层第一轮尺寸适配

未完成：

- `CodeContextMenu` 还没有针对移动端重新设计
- Tab 长按 / 菜单交互还没有做手机端专项优化
- 代码查看器的触摸交互还没有做真机验证

### 阶段 5：回归与补边角

- 状态：未开始

## 当前停留点

当前代码已经具备继续做第二批移动端收尾的基础，暂时不需要回头重拆以下边界：

- `WorkspaceLayoutMode`
- `WorkspaceSceneContent` 作为装配入口
- `WorkspaceSceneCompact / Expanded` 的分层
- `HomeScreen` 继续保留在 `commonMain` 做自适应
- Android 返回逻辑继续在 `App.android.kt` 收口

当前最主要的未完成项是：

- `CodeContextMenu` 的移动端交互重设计
- `MIXED` 模式在手机端的显式视图切换入口
- Tab 相关长按 / 菜单体验优化
- 搜索结果进入代码后的手势与焦点体验回归
- 真机 / 模拟器验证

## 已验证内容

已通过：

```bash
./gradlew :sharedUI:compileKotlinJvm :sharedUI:compileAndroidMain
```

说明：

- 当前验证范围是编译通过。
- 还没有做 Android 模拟器或真机交互验证。

## 风险与注意事项

- `code-view` 虽然已经可参与 Android 编译，但长按、选区、上下文菜单、软键盘行为仍可能有移动端特有问题。
- `WorkspaceSceneCompact` 目前优先保证结构正确，后续还需要在交互细节上继续打磨。
- `MIXED` 模式虽然已经做了窄屏降级，但“如何让用户明确切换当前显示的 kind”还需要补 UI。
- 当前进度不包含视觉细节打磨，仍以主链可用和结构可维护为优先。

## 下次直接开做

建议按下面顺序继续：

1. 处理 `CodeContextMenu` 的移动端交互方案，避免继续沿用桌面语义。
2. 给紧凑布局下的 `MIXED` 模式补显式切换入口。
3. 优化 Tab 长按菜单和顶部操作在手机端的触达路径。
4. 跑一轮 Android 模拟器 / 真机验证，重点看：
   - 首页新建工作区
   - 列表进入工作区
   - 抽屉类树打开 / 关闭 / 选类
   - 标签切换
   - 搜索与定义跳转
   - 返回键行为
