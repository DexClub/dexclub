# Desktop 真实工作区回归记录

## 说明

本文件用于记录本轮 Desktop 真实工作区回归执行结果。

规则：

- 每测完一项立即追加结果，不等待整轮结束
- 只记录执行结论，不在这里展开设计讨论
- 若某项结果导致状态、规范或设计变化，按 `docs/` 规则同步更新对应文档

## 记录

### 1. 初始恢复与重启恢复

- 平台：Desktop
- 场景：初始 caret、selection、纵向 / 横向滚动恢复；重新运行后位置记忆恢复
- 步骤：打开代码页，调整 caret、selection、纵向 / 横向滚动位置；关闭并重新进入相关页面；重新运行应用后再次检查
- 实际结果：初始 caret、selection、滚动位置恢复正常；重新运行后记忆位置也正常
- 预期结果：恢复正常
- 结论：通过
- 文档落点：仅保留在本轮回归记录，暂不需要更新 `status/spec/design`

### 2. 语法高亮

- 平台：Desktop
- 场景：Java / Smali 代码页语法着色
- 步骤：打开真实工作区中的 Java 和 Smali 代码页，观察正文是否存在 token 级语法着色
- 实际结果：Java / Smali 均已显示基础 token 级语法着色；Smali 在修复后复测通过
- 预期结果：Java / Smali 都存在基础 token 高亮
- 结论：通过
- 当前判断：语言解析回退和 Smali 高亮 query 修复已在真实工作区验证通过
- 主题说明：当前亮色主题基线按 IDEA / Android Studio `Light.icls` 的通用语义色对齐，不按 Java 专属色表处理；Java 与 Smali 共用同一套 `keyword / string / number / comment / annotation / field / builtin` 语义色
- 文档落点：先记录在本轮回归记录；若 Smali 修复后仍不符合预期，再同步更新 `status/design`

### 3. 语法高亮编辑后刷新

- 平台：Desktop
- 场景：编辑代码后语法高亮是否跟随新文本刷新
- 步骤：在可编辑代码页中把关键字替换为普通标识符，例如将 `static` 编辑为 `abcd`，观察提交后的 token 颜色是否更新
- 实际结果：旧 token 残留问题已修复；关键字替换为普通标识符后，不再继续沿用旧关键字范围。另已补修 Java 在错误行与中文注释编辑后的高亮问题：随机小写标识符不再整行误判为类型名，插入中文注释后也不会再导致整文件高亮丢失
- 预期结果：文本提交后应重新刷新 tokens / annotations，旧 token 不应残留到新文本上
- 结论：通过
- 当前判断：`DefaultCodeSurfaceController` 的失败路径、`TreeSitterHighlighter` 的旧树复用问题，以及 Java highlights query 在 Unicode 注释场景下的 byte/char 偏移问题都已修复，当前已不再出现旧关键字范围残留或中文注释后整文件掉色
- 文档落点：先记录在本轮回归记录；若后续确认需要新增长期验收项，再同步更新 `spec/test-matrix.md`

### 4. 编辑时可视区闪烁

- 平台：Desktop
- 场景：在可编辑代码页中单字符编辑时，当前可视区域是否出现明显整体闪烁
- 步骤：在关键字内逐字替换，例如将 `int` 逐步改成 `float`、`你好啊`，观察每次输入后当前可视区域文本绘制是否整体闪烁
- 实际结果：调整 `CodeLineTextLayoutCache` 的复用策略后，当前样例下复测未再观察到明显整片闪烁
- 预期结果：编辑后允许触发可视区域重绘，但不应出现用户明显感知到的整片文本闪烁
- 结论：当前样例复测通过，待继续扩大场景确认
- 当前判断：问题主要位于 Compose 渲染缓存层；此前 `CodeViewerCanvas` 会在每次 `layoutSnapshot` 变化时重建整份 `CodeLineTextLayoutCache`，现已改为跨 snapshot 复用并仅失效受影响行，当前看已明显收敛
- 文档落点：先记录在本轮回归记录；若最终需要调整渲染缓存策略，再同步更新 `design/performance-regression.md`

### 5. 启动后 tree-sitter native 崩溃

- 平台：Desktop
- 场景：打开真实工作区后立即进入代码页，后台高亮刷新触发 native parser
- 步骤：启动应用并打开包含 Smali 代码页的真实工作区，观察后台 `DefaultCodeSurfaceController` 刷新期间是否发生 native 崩溃
- 实际结果：出现 `SmaliLanguageSession -> TreeSitterHighlighter -> Parser.parse(...)` 的 native 崩溃，线程栈显示同一语言会话在 `DefaultDispatcher` 上进入 `ktreesitter` JNI
- 预期结果：高亮刷新允许多次触发，但不能并发进入同一个 tree-sitter session，更不能导致 JVM 进程崩溃
- 结论：失败现象已定位并修复，待真实工作区复测
- 当前判断：`DefaultCodeSurfaceController` 在 `init` 启动刷新，同时 `CodeEditor/CodeViewer` 还会在 `snapshot.revision` 上再次调用 `refresh()`；同一个语言会话因此可能并发进入 `TreeSitterHighlighter`。现已在 controller 侧用 `Mutex` 串行化 `refresh()`，并补了并发回归测试
- 文档落点：先记录在本轮回归记录；若后续仍有 session 并发策略调整，再同步更新 `design/performance-regression.md`
