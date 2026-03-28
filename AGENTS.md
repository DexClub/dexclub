# DexClub 项目规则

## 核心要求

- **语言要求**：所有回复、解释及沟通必须采用 **中文**。
- **文件编码**：项目内所有文件统一使用 **UTF-8 without BOM（UTF-8 无 BOM）** 编码；修改现有文件时必须保持该编码，新创建文件也必须使用该编码。
- **无 Lint / 格式化工具**：项目当前未配置正式 Lint 与自动格式化工具，必须严格手动遵守下文的代码风格与结构规范。
- **代码整理**：如果需要代码整理，应在确保逻辑正确的前提下同时保证代码可读性和可维护性进行，如有必要可以调整代码结构和代码文件结构。

## 项目概览

这是一个面向以下平台的 **Kotlin Multiplatform (KMP)** 项目：

- Android（最低 SDK 24，目标 SDK 36）
- JVM / Desktop（Compose Desktop）

### 模块说明

- `sharedUI`：跨平台共享的通用代码。
- `androidApp`：Android 平台专属应用代码。
- `desktopApp`：桌面端专属应用代码。
- `shadcn-ui-compose`：共享 UI 组件库。
- `dex-engine`：Dex 引擎主库模块，封装 `dexkit + dexlib2 + baksmali + jadx`，供 `sharedUI` 和 CLI 复用。
- `dex-engine/cli`：Dex 引擎命令行模块，负责 `main(args)` 与 fat jar 打包。
- `dex-engine/vendor/DexKit`：DexKit 上游工程目录，通过 `settings.gradle.kts` 中 `includeBuild` + `dependencySubstitution` 与主工程联动。
- `code-view`：独立的 Canvas 自绘代码预览 / 编辑器组件，不依赖 `shadcn-ui-compose`。

## 代码规范

### 包结构

- 遵循反向域名命名法：`io.github.dexclub.feature.subfeature`
- 通用代码位于 `commonMain` 的 `io.github.dexclub.*`
- 平台专属代码位于 `androidMain` / `jvmMain` 的 `io.github.dexclub.*`
- 子目录划分：
    - `app`：应用级代码、UI 界面、ViewModels
    - `core`：核心业务逻辑（工厂类、服务）
    - `database`：数据库（实体、DAOs、Room 数据库）
    - `model`：数据模型
    - `utils`：工具类（`object` 声明）
    - `res`：资源（`StringRes` 等）
    - `compose`：Compose 基础组件
    - `scene`：特定场景 / 屏幕代码

### 导包

- 按库类型分组并在组内按字母排序：
    1. 标准库（Standard library）
    2. `androidx`
    3. `compose`
    4. 项目内部导入（Project imports）
    5. 第三方库
- 禁止使用通配符导入（`import *`）
- 每个导入独占一行
- 示例：
  ```kotlin
  import androidx.compose.foundation.layout.Column
  import androidx.compose.material3.Text
  import io.github.dexclub.database.workspace.entities.WorkspaceEntity
  import io.github.shadcn.ui.compose.Button
  ```

### 格式化

- **缩进**：4 个空格，禁止使用 Tab
- **行宽**：最大 120-140 个字符
- **大括号风格**：K&R 风格（左大括号不换行）
- **尾随逗号**：在函数调用、参数列表和多行声明中使用尾随逗号
- **空格**：运算符前后、逗号后、关键字与括号之间需加空格
- **空行**：顶级声明之间留一个空行，函数 / 类之间留两个空行

### 命名规范

- **类 / 对象**：大驼峰式（PascalCase），如 `WorkspaceEntity`、`ApkFactory`
- **函数 / 方法**：小驼峰式（camelCase），如 `exportSingleDex`、`onShowNewWorkspaceDialog`
- **属性**：小驼峰式（camelCase），如 `workspace`、`newWorkspaceDialog`
- **常量**：真正的常量使用全大写蛇形命名（SCREAMING_SNAKE_CASE）
- **私有幕后属性**：以前缀下划线开头，如 `_newWorkspaceDialog`、`_workspaceItems`
- **Composable 函数**：大驼峰式（PascalCase），如 `HomeScreen`、`AppTitle`
- **UI 组件**：大驼峰式（PascalCase），如 `Button`、`Card`
- **数据类**：大驼峰式；集合类使用复数，如 `WorkspaceItem`、`workspaceItems`

### 类型与模式

- 使用 **data classes** 定义不可变数据模型（包含 `equals` / `hashCode`）
- 使用 **sealed classes** 处理导航状态和受限类型层级
- 在 ViewModel 中使用 **StateFlow** 处理响应式状态
- 使用 **expect / actual** 模式处理平台相关实现
- Room 实体使用 `@Entity` 注解并指定 `tableName`
- Room DAO 接口使用 `@Dao` 注解
- 伴生对象（Companion object）用于工厂方法和常量

### Compose 指南

- 状态提升（State hoisting）至最小公共父节点
- 在 Composable 中使用 `remember` 存储本地状态
- 使用 `viewModel { ViewModelType() }` 获取 ViewModel
- 使用 `collectAsState()` 观察 `StateFlow`
- 参数应具有明确类型以提高可读性
- 使用 `Modifier` 作为第一个可选参数，默认值为 `Modifier = Modifier`
- UI 组件优先使用组合（Composition）而非继承
- 所有组合函数必须添加 `@Composable` 注解

### 数据库指南

- 使用 Room 进行数据库抽象
- 实体使用 `@Entity(tableName = "...")`
- 主键根据实体语义选择；自增场景使用 `@PrimaryKey(autoGenerate = true)`，稳定业务键可使用 `@PrimaryKey`
- DAO 使用 `@Query` 注解编写 SQL
- 针对平台差异化的数据库配置使用 `expect / actual` 模式
- DAO 操作使用 `suspend`
- 路径处理在 `androidMain` / `jvmMain` 中分别处理平台差异

### 错误处理

- 使用 `require()` 进行参数验证，并附带清晰错误消息
- 对于非预期情况，抛出带有描述性消息的异常
- 在 `actual` 实现中处理平台相关错误
- 示例：
  ```kotlin
  require(apk.exists()) { "apk 文件不存在: ${apk.absolutePath}" }
  require(apk.isFile) { "apk 路径必须是一个文件: ${apk.absolutePath}" }
  ```

### 日志记录

- 使用项目内置日志工具：`Any.logger("消息")`
- 平台特定实现在 `Env.kt` 文件中定义
- 保持日志消息简洁且有信息量
- 示例：
  ```kotlin
  logger("singleDexFile 路径: ${singleDexFile.absolutePath}")
  ```

### 注释

- 保持注释最少化，让代码尽量自解释
- 对复杂模块使用包级文档说明
- 仅在“为什么这么做”不明确时添加注释，而不是解释“在做什么”
- 显而易见的代码不要添加行内注释

## 构建、测试与清理

### 常用构建

- **构建所有模块**：`./gradlew build`
- **Android 调试 APK**：`./gradlew :androidApp:assembleDebug`
- **Android 发布 APK**：`./gradlew :androidApp:assembleRelease`
- **桌面应用运行**：`./gradlew :desktopApp:run`
- **桌面应用热重载**：`./gradlew :desktopApp:hotRunJvm --auto`
- **桌面应用打包（当前系统）**：`./gradlew :desktopApp:packageDistributionForCurrentOS`

### 模块编译

- **sharedUI JVM 编译**：`./gradlew :sharedUI:compileKotlinJvm`
- **sharedUI Android 编译**：`./gradlew :sharedUI:compileAndroidMain`
- **shadcn-ui-compose JVM 编译**：`./gradlew :shadcn-ui-compose:compileKotlinJvm`
- **shadcn-ui-compose Android 编译**：`./gradlew :shadcn-ui-compose:compileAndroidMain`
- **code-view-compose JVM 编译**：`./gradlew :code-view-compose:compileKotlinJvm`
- **code-view-compose Android 编译**：`./gradlew :code-view-compose:compileAndroidMain`
- **dex-engine JVM 编译**：`./gradlew :dex-engine:compileKotlinJvm`
- **dex-engine Android 编译**：`./gradlew :dex-engine:compileAndroidMain`
- **dex-engine CLI 编译**：`./gradlew :dex-engine:cli:compileKotlin`
- **dex-engine CLI shadowJar**：`./gradlew :dex-engine:cli:shadowJar`
- **dex-engine CLI fat jar**：`./gradlew :dex-engine:cli:fatJar`
- **KMP 双端快速编译校验（示例：`dex-engine`）**：`./gradlew :dex-engine:compileKotlinJvm :dex-engine:compileAndroidMain`
- **KMP 任务注意**：`:xxx:compileKotlinAndroid` 通常不存在，请使用 `:xxx:compileAndroidMain`

### 测试

- **已配置任务**：当前可运行 `allTests` / `jvmTest`，但业务自动化测试覆盖仍需持续补充
- **运行所有测试**：`./gradlew allTests`
- **运行单个测试**：`./gradlew :sharedUI:jvmTest --tests 类名.方法名`

### 清理与格式化

- **清理项目**：`./gradlew clean`
- **Lint / 格式化说明**：未配置正式 Lint 工具，请严格遵守本文规范

## 技术约束与注意事项

- **编译器选项**：已启用 `freeCompilerArgs.add("-Xexpect-actual-classes")`
- **Java 版本**：21（源码和目标版本均为 21）
- **KSP**：用于 Room 注解处理
- **Compose 编译器**：已启用并配置
- **依赖管理**：使用 `gradle/libs.versions.toml` 中的版本目录
- **配置缓存**：`gradle.properties` 中已启用
- **首次克隆仓库**：执行 `git submodule update --init --recursive` 初始化 `tree-sitter/*` 与 `dex-engine/vendor/DexKit` 目录所需内容
