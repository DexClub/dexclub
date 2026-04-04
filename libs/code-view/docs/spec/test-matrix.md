# 测试矩阵

## 目的

本册用于定义 `code-view` 当前的验证矩阵。

它要回答的问题是：

- 哪些内容应该靠自动测试覆盖
- 哪些内容必须靠手测
- Desktop 和 Android 分别至少要验证什么
- 什么时候可以认为一个主题“已完成”

## 范围

本册覆盖：

- `commonTest`
- `jvmTest`
- compile 校验
- Desktop 手测
- Android 真机 / 模拟器手测

## 总原则

当前测试策略遵守这几个原则：

- 纯逻辑优先自动测试
- 平台体感优先手测
- 页面级问题不能只靠组件级单测判断
- compile 校验是最低门槛，不等于行为通过

## 验证层级

当前建议把验证分成四层：

### 1. 模型与纯函数测试

适合覆盖：

- 布局快照
- 坐标映射
- `LineSelection / CodeSelection / Cursor` 转换
- viewport reveal
- token / annotation 切片
- 行渲染分段
- 输入桥中的纯状态机逻辑
- 多击计数与选区纯逻辑

当前建议任务：

- `:code-view-compose:jvmTest`

### 2. 编译校验

适合覆盖：

- 公开 API 没有编译断裂
- KMP 双端主链没有明显结构性破坏

当前建议最少执行：

- `:code-view-compose:compileKotlinJvm`
- `:code-view-compose:compileAndroidMain`

若需要验证主工程联动：

- `:sharedUI:compileKotlinJvm`
- `:sharedUI:compileAndroidMain`

### 3. 组件级手测

适合覆盖：

- 组件行为是否符合规范
- 但不一定进入完整工作区页面

主要用于：

- 新交互语义刚接入时的快速确认

### 4. 真实工作区手测

适合覆盖：

- 页面级滚动恢复
- 导航跳转
- 搜索高亮恢复
- 右键菜单
- 平台工具栏
- IME 体感
- overscroll

这是当前最关键、也最不能缺的一层。

## 当前自动测试基线

当前自动测试应至少覆盖这些主题：

| 主题 | 当前建议 |
| --- | --- |
| 布局快照 | 必测 |
| offset / line / column 映射 | 必测 |
| viewport reveal | 必测 |
| token / annotation 切片 | 必测 |
| 渲染分段 | 必测 |
| Selection 纯逻辑 | 必测 |
| 多击计数 | 必测 |
| 输入桥状态机纯逻辑 | 建议持续补 |

## 当前编译校验基线

每次涉及 `libs/code-view` 结构变化时，至少应通过：

- `:code-view-compose:compileKotlinJvm`
- `:code-view-compose:compileAndroidMain`

每次涉及工作区接线或 `sharedUI` 集成时，至少应额外通过：

- `:sharedUI:compileKotlinJvm`
- `:sharedUI:compileAndroidMain`

## Desktop 手测矩阵

当前 Desktop 至少应覆盖这些场景：

### 只读路径

- 打开代码页后 caret 位置恢复
- 关闭并重新打开标签页后的纵向 / 横向滚动恢复
- `cursorTarget` 导航跳转 reveal
- 搜索高亮恢复
- annotation 次键命中与菜单位置
- 长行横向滚动后的 annotation 命中

### 编辑路径

- 连续输入
- Backspace / Delete
- 方向键移动
- 选区替换
- 粘贴多行文本
- 拖拽选区
- 编辑态右键 annotation
- 双击选词
- 三击选行

### 输入法路径

- 中文输入连续 composing
- composing 被回车打断
- composing 被删除打断
- composing 被方向键打断
- composing 被鼠标点击其他位置打断
- 长行横向滚动后继续输入
- 候选窗跟随 caret

## Android 手测矩阵

当前 Android 至少应覆盖这些场景：

### 编辑与输入

- 连续输入
- 退格 / 删除
- 候选提交
- 焦点切换
- 选区更新
- 不同输入法差异

### 触摸与选择

- 单击放 caret
- 普通拖动滚动优先
- 长按选词
- 长按后菜单入口时机
- 单个光标手柄拖拽
- 双手柄拖拽
- 交汇后反向拖拽

### 平台与滚动

- 平台 `SelectionToolbar`
- 长文本上下滚动后继续输入
- 键盘顶起 reveal
- overscroll 下正文 stretch
- overscroll 下手柄对齐
- 边界拖动后的回弹体感

## 结果记录规范

手测若发现问题，记录至少应包含：

- 平台
- 文件类型或样例
- 触发步骤
- 实际结果
- 预期结果

不要只写“有 bug”。

## 当前达标标准

一个主题只有同时满足下面几类条件，才能从“进行中”转成“已完成”：

- 核心纯逻辑已有自动测试覆盖
- 相关模块编译校验通过
- Desktop / Android 对应平台手测已完成
- 真实工作区主路径没有明显回归

## 当前状态

- 状态：`进行中`
- 当前目标：把现有零散手测和自动测试要求整理成统一矩阵，并逐步沉淀到持续维护流程
- 关联文档：[`../status.md`](../status.md)、[`../design/performance-regression.md`](../design/performance-regression.md)
