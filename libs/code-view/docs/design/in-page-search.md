# 页内搜索

## 目标

本设计文档用于定义代码页内搜索的第一版完整能力。

当前目标不是只在 DexKit 搜索命中后补一个顶部输入框，而是建立一套可持续扩展的页内搜索模型，使后续可以稳定支持：

- DexKit 搜索结果打开后的自动回填
- 当前代码页内的连续搜索
- 上一项 / 下一项切换
- 命中计数
- Java / Smali 双视图下的独立搜索状态

当前文档先定义设计边界、状态归属和交互规则，不直接作为最终验收矩阵。

## 当前背景

当前工作区已经具备：

- DexKit 搜索结果打开代码页
- 程序化定位 `cursor / selection / reveal`
- 当前活动命中 `searchHighlight`
- 其他命中 `inactiveSearchHighlights`

当前链路的特点是：

- DexKit 搜索弹层状态与代码页状态分离
- 工作区上层维护页内搜索会话
- `code-view` 只接收活动命中和其他命中的渲染投影

这意味着当前实现仍由工作区上层拥有搜索真相源，`code-view` 只负责把命中范围稳定画出来。

## 第一版范围

第一版页内搜索包含：

- 代码页顶部搜索框
- 查询词输入与回填
- 当前 pane 内全部命中计算
- 当前命中高亮
- 上一项 / 下一项
- `x / y` 命中计数

第一版不包含：

- 正则搜索
- 区分大小写
- 全词匹配
- 替换
- 搜索历史
- 长期持久化恢复

## 状态归属

页内搜索状态按 `tabId#kind` 维护，而不是只按 `tabId` 维护。

原因：

- 同一个 tab 下，Java 与 Smali 的显示内容和匹配内容可能不同
- mixed 模式下两个 pane 可以同时存在
- 若只按 `tabId` 维护，同一 tab 内不同 kind 会互相覆盖搜索状态

因此：

- `smali` pane 有自己的页内搜索状态
- `java` pane 有自己的页内搜索状态
- mixed 模式下两个 pane 可以同时显示各自的顶部搜索框

## 查询模型

第一版页内搜索状态至少包含：

- `queryText`
- `matchQuery`
- `source`
- `activeMatchIndex`
- `matches`
- `isVisible`

### `queryText`

输入框中显示给用户的文本。

### `matchQuery`

当前 pane 实际拿去搜索的文本。

第一版中，`queryText` 与 `matchQuery` 大多数情况下可以相同，但模型上仍保留两者分离能力，以避免后续功能扩展时返工。

### `source`

表示当前页内搜索状态的来源。第一版先固定为：

- `Manual`
- `DexKitString`
- `DexKitClass`

用途：

- 区分用户手动输入与 DexKit 自动回填
- 为后续行为差异保留边界

## DexKit 回填规则

### 字符串命中

DexKit 字符串命中打开代码页后：

- 定位目标 pane 并高亮当前字符串命中
- 不自动显示顶部搜索框
- `source = DexKitString`
- `queryText = 原始字符串`
- `matchQuery = 原始字符串`

### 类命中

DexKit 类命中打开代码页后：

- 定位目标 pane 并高亮当前类名命中
- 不自动显示顶部搜索框
- `source = DexKitClass`
- Java 与 Smali 各自独立回填

当前约定：

- `smali` pane 显示并匹配 descriptor
- `java` pane 显示并匹配源码侧类名展示文本

第一版先按当前决议使用 Java 侧展示名进行匹配，后续若需要“显示值”和“匹配值”分离，可直接在现有模型上扩展。

## 用户手动输入

用户手动输入时：

- `source = Manual`
- `queryText = 输入框当前值`
- `matchQuery = 输入框当前值`

用户手动修改 DexKit 自动回填内容后，当前 pane 的搜索状态应视为手动搜索状态继续工作，不再强依赖原始 DexKit 搜索上下文。

## mixed 模式

mixed 模式下两个 pane 各自负责各自的搜索：

- 每个 pane 顶部都可以显示自己的搜索框
- 每个 pane 只搜索自己当前文本
- 每个 pane 维护自己的命中列表与当前命中索引

DexKit 回填不是“跨 pane 混合搜索”，而是：

- 将 DexKit 命中结果分别回填到对应 pane 的页内搜索状态
- 两个 pane 的边界保持独立，不共享命中列表

## 命中与高亮

第一版需要明确区分：

- 全部命中
- 当前命中

当前建议：

- 全部命中使用统一弱高亮
- 当前命中使用更强强调色
- `x / y` 计数与 `activeMatchIndex` 保持一致

当前 `code-view` 已采用分层搜索高亮输入：

- `searchHighlight` 表达当前活动命中
- `inactiveSearchHighlights` 表达其他命中
- 工作区上层仍负责维护完整命中结果、当前索引、计数和 reveal

## 编辑后的行为

用户编辑代码后：

- 保留当前 `queryText`
- 保留搜索框可见状态
- 重新计算当前 pane 的全部命中
- 若原 `activeMatchIndex` 越界，则钳制到有效范围

第一版不采用“编辑后自动清空搜索状态”的策略。

## 生命周期

第一版页内搜索状态只保留在内存中，不做长期持久化。

规则：

- 关闭 tab 后清空对应搜索状态
- 应用重启后不恢复页内搜索状态
- 不写入 editor session 长期存储

这样可以降低第一版复杂度，并避免把临时搜索工作态过早固化为长期状态。

## 键盘与基础交互

第一版至少应支持：

- 打开后输入即搜索
- `Enter` 跳到下一项
- `Shift + Enter` 跳到上一项
- `Esc` 关闭当前 pane 搜索框

是否支持 `Ctrl/Cmd + F` 作为统一打开入口，可在实现阶段按平台输入现状决定，但不影响本设计的核心状态模型。

## 文档落点

当前文档负责说明：

- 为什么需要单独的页内搜索模型
- 状态应该归属到哪里
- DexKit 回填与手动输入如何统一
- 第一版范围和边界是什么

待实现稳定后，再同步更新：

- `docs/spec/test-matrix.md`
- `docs/spec/api-spec.md`
- `docs/spec/state-model-spec.md`
