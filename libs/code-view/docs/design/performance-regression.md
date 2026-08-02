# 性能、降级与回归

## 目的

本册用于说明 `code-view` 当前的性能策略、降级边界与回归要求。

这里要回答的问题是：

- 当前已经做了哪些缓存和热路径优化
- 大文件、长行和高频输入时有哪些边界
- 当前回归重点在哪里
- 自动测试和手测应该分别覆盖什么

## 范围

本册覆盖：

- layout cache
- token / annotation refresh
- large file degrade
- long lines degrade
- `commonTest` / `jvmTest`
- Desktop / Android 手测范围

本册不覆盖：

- 具体平台输入桥实现细节
- 单个交互主题的详细规则

相关主题请见：

- Viewer / 只读渲染：[`viewer-rendering.md`](viewer-rendering.md)
- Selection / Caret：[`selection-caret.md`](selection-caret.md)
- 输入与 IME：[`editor-input-ime.md`](editor-input-ime.md)

## 当前性能策略

当前 `code-view` 的性能思路已经不是“直接把整份文本交给一个文本控件”，而是把渲染和装饰拆成几个层次：

1. 先缓存按文本分行的基础布局
2. 再在基础布局上叠 token / annotation 装饰
3. 只绘制 viewport 内可见行
4. 横向几何复用 `TextLayoutResult`
5. 垂直行框改为稳定行度量

这套思路的核心目标是：

- 避免 decoration 刷新时反复重建与文本本身无关的基础布局
- 避免每帧都做整份文本级计算
- 把横向几何精度和纵向稳定性拆开处理

## 当前已完成优化

当前已经明确落地的优化包括：

- `CodeLayoutSnapshotFactory` 已拆出 `withDecorations(base, tokens, annotations)` 路径
- 文本分行基础布局会先缓存，再叠 token / annotation 装饰
- token / annotation 刷新时不再重复重建整份逻辑行布局
- 每行 token 排序前移到布局快照构建阶段，移除绘制热路径中的逐帧排序
- 新增按行缓存的 `CodeLineTextLayoutCache`
- `CodeViewerCanvas` 的横向 caret、selection、annotation 命中与横向 reveal 统一复用 `TextLayoutResult`
- 垂直行框切到稳定行度量
- 混排正文改为按 token / 脚本分段绘制
- 稳定行度量、渲染分段和 renderer 职责已拆到独立文件
- 高亮 / 注解刷新已切到后台线程
- `TreeSitterHighlighter` 已缓存 highlights `Query`，避免每次刷新重复编译 query
- tree-sitter capture 的 UTF-8 byte -> char offset 转换已改为单次顺序扫描，避免每个 token 都从头扫描全文
- `CodeLayoutSnapshotFactory` 构建 `tokensByLine` 时会优先复用有序 token 输入，按行提示推进定位，只在检测到乱序行时补排序
- 编辑态已避免按输入全量切行字符串、全量测所有行宽

这些优化说明当前已经不再停留在“先跑起来再说”的阶段，而是在主动压热路径。

## 当前降级边界

`code-view` 的性能问题不只来自绘制，还来自语言高亮和 surface controller。

当前降级路线的原则是：

- 大文件和超长行不强求完整高亮
- runtime 可以按既定策略降级
- Viewer / Editor 侧必须兼容 token / annotation 变少甚至退成 plain text 的情况

也就是说：

- 渲染层不独自决定“是否降级”
- 但渲染层必须能稳定接住 runtime 的降级结果

## 当前主要风险

### 1. 长行与长文本的真实体感还没有完全验证

虽然当前渲染链已经切到更合理的结构，但这些点还没有完全做实：

- 超长行横向滚动的体感
- Desktop 长文本输入中的 reveal 稳定性
- Android 长文本输入和滚动后的体感

### 2. 编辑态高频输入仍可能有临时快照重建成本

当前仍需继续观察：

- 高频输入下 `selectionLayoutSnapshot` 的临时重建成本
- 输入、selection、token 更新叠加时的主路径开销

### 3. 真实工作区回归仍是当前最重要的验证手段

当前很多风险不是单个纯函数测试能直接覆盖的，而是要在真实工作区里验证：

- 滚动恢复
- 搜索高亮恢复
- annotation 命中
- IME composing
- overscroll

## 当前回归重点

当前回归顺序已经明确为：

1. 先 Desktop
2. 再 Android

当前 Desktop 侧最需要继续验证：

- 中文输入与 composing 是否稳定
- composing 被回车、删除、方向键和鼠标点击打断后的状态
- 长行横向滚动后继续输入
- 关闭并重新打开标签页后的纵向 / 横向滚动恢复
- 搜索高亮恢复
- annotation 右键菜单位置和命中

当前 Android 侧最需要继续验证：

- 连续输入与 composing
- 普通拖动滚动优先
- 长按选词
- 单个光标手柄与双手柄拖拽
- 不同输入法下的删除、候选提交和选区更新
- 平台 overscroll 下正文 stretch、手柄对齐和边界回弹

## 自动测试与手测分工

当前比较合理的分工是：

### 自动测试

自动测试优先覆盖：

- 布局快照
- 坐标映射
- viewport reveal
- token / annotation 切片
- 行渲染分段
- 输入桥中的纯状态机逻辑
- 多击计数和选区纯逻辑

这类逻辑具有：

- 稳定
- 可重复
- 不依赖真实平台输入法

### 手测

手测优先覆盖：

- 真实工作区页面
- 真实滚动容器
- IME
- 平台工具栏
- 右键菜单
- overscroll
- 长行体感

这类问题如果只靠单元测试，很难捕捉真实退化。

## 当前测试现状

当前已经明确通过过的任务包括：

- `:code-view-compose:jvmTest`
- `:code-view-compose:compileKotlinJvm`
- `:code-view-compose:compileAndroidMain`
- `:sharedUI:compileKotlinJvm`
- `:sharedUI:compileAndroidMain`

当前已有测试方向已经覆盖到：

- 布局快照
- 坐标解析
- viewport reveal
- 渲染分段
- 输入桥部分纯函数逻辑
- 多击与选区纯逻辑

但它还没有覆盖：

- 真实工作区页面级回归
- 真实 IME 体感
- 平台菜单与 overscroll 体感

## 当前计划

当前建议的推进顺序：

1. 继续完成 Desktop 工作区真实页面回归
2. 再完成 Android 真机和输入法差异回归
3. 根据手测结果决定是否补专项自动回归
4. 最后补 `spec/test-matrix.md`，把回归矩阵正式化

## 验收标准

以下条件满足后，可认为当前阶段的性能、降级与回归工作基本达标：

- 基础布局、装饰刷新与渲染热路径没有明显重复计算
- 长文本和长行场景下没有明显卡死或严重错位
- Desktop / Android 真实工作区主路径回归完成
- 自动测试覆盖核心纯逻辑，手测覆盖平台体感和页面级风险
- runtime 降级结果能被 Viewer / Editor 稳定接住

## 当前状态

- 状态：`进行中`
- 当前目标：完成真实工作区回归，并继续观察长文本、长行与高频输入性能边界
- 对应总进度：[`../status.md`](../status.md)
