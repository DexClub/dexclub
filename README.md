# dexclub-cli

`dexclub-cli` 是一个面向 `dex / apk / Android 资源` 的 Kotlin 多模块项目。
它当前同时提供：

- 一套围绕工作区的 CLI，用来做 dex 查询、方法检查、代码导出以及 Android 资源解析
- 一套面向 AI/agent 的 MCP server，用来把同一批能力以工具调用方式暴露给上层编排

当前仓库包含四个核心模块：

- `cli`
  命令行入口，负责参数解析、命令分发、结果渲染与退出码
- `core`
  稳定能力边界，负责 workspace、dex 查询/导出、resource 解析
- `dexkit`
  面向 KMP 的 DexKit 包装层
- `mcp`
  HTTP MCP server，负责把 `core` 能力暴露成 AI 可调用工具面

## 当前能力

- 初始化并管理 `.dexclub` 工作区
- 在同一工作区内列出并切换 active target
- 输出工作区状态与 active target 摘要
- 按 JSON 条件查找类、方法、字段
- 按字符串批量条件查找类和方法
- 查看单个方法的一层关系详情
- 导出单类 dex、smali、Java
- 导出单方法 dex、smali、Java
- 解析 `AndroidManifest.xml`
- 读取 `resources.arsc`
- 解码二进制 XML
- 列出、解析、搜索资源条目
- 通过 MCP 暴露 target session、dex 查询、方法检查、代码导出、manifest 与资源相关工具

## 仓库结构

- `cli/`
  当前命令行模块
- `core/`
  当前能力库模块
- `dexkit/`
  KMP DexKit 包装层
- `mcp/`
  MCP server 模块
- `skills/`
  仓库内维护的 Codex skill 副本
- `dexkit/vendor/DexKit/`
  vendored 上游 DexKit
- `dexkit/vendor/libcxx-prefab/`
  Android 构建链依赖的本地 `libcxx` prefab 仓库
- `.docs/v3/`
  命令面、对象模型、执行流与存储结构设计文档

## 环境要求

开发和本地构建默认要求：

- JDK 21
- Android SDK
- Android NDK `28.2.13676358`
- `cmake` `3.18.1` 和 `3.31.6`
- `ninja`

说明：

- Android SDK / NDK 主要服务于 DexKit native 构建和测试样本生成
- 根构建默认启用 `mavenLocal()`；当前本仓维护链路依赖本地存在 `dev.rikka.ndk.thirdparty:libcxx:1.3.0`
- 当前 README、根构建与 CI 默认以 `NDK 28.2.13676358` 发布 `dexkit/vendor/libcxx-prefab`；vendored 上游 `dexkit-android` 模块自身仍声明 `NDK 26.1.10909125`
- CI 当前会同时安装 `cmake 3.18.1` 和 `3.31.6`；本地若要复现 Android / native 构建链，建议与该组合保持一致
- `core` 的资源命令运行时不要求额外安装 Android SDK
- 桌面端 DexKit 运行前提可参考上游文档：
  [DexKit 桌面端运行说明](https://luckypray.org/DexKit/zh-cn/guide/run-on-desktop.html)

## 初始化仓库

首次拉取后先初始化 submodule：

```bash
git submodule update --init --recursive
```

如果本地还没有发布 `libcxx`，先执行：

```bash
cd dexkit/vendor/libcxx-prefab
./gradlew :cxx:publishToMavenLocal
```

说明：

- 这一步会把 `dev.rikka.ndk.thirdparty:libcxx:1.3.0` 发布到本机 `mavenLocal()`
- 当前仓库的 `dexkit / core / cli / mcp` 构建都会经过根项目仓库解析；如果这里缺失，Android / native 相关构建会直接失败

## 构建与验证

编译 `dexkit`：

```bash
./gradlew :dexkit:compileKotlinJvm
./gradlew :dexkit:assembleAndroidMain
```

编译 `core / cli / mcp`：

```bash
./gradlew :core:compileKotlinJvm :cli:compileKotlin :mcp:compileKotlin
```

运行测试：

```bash
./gradlew :core:jvmTest
./gradlew :cli:test
./gradlew :mcp:test
```

按职责跑 `core` 测试：

```bash
./gradlew :core:testFast
./gradlew :core:testDex
./gradlew :core:testResource
./gradlew :core:testResourceManifest
./gradlew :core:testResourceTable
./gradlew :core:testResourceXml
./gradlew :core:testResourceEntry
./gradlew :core:testResourceValue
./gradlew :core:testWorkspace
./gradlew :core:testStructured
```

说明：

- `:core:testFast` 跑 dex、resource 与 workspace 三组 JVM 测试入口
- `:core:testDex` 跑 dex 分析、查询、`inspectMethod` 与导出测试
- `:core:testResource` 串行跑全部 resource 相关测试
- `:core:testResourceManifest` 跑 manifest 解码与结构化解析测试
- `:core:testResourceTable` 跑 resource table 解码测试
- `:core:testResourceXml` 跑 XML 解码测试
- `:core:testResourceEntry` 跑 resource entry 索引与列举测试
- `:core:testResourceValue` 跑 resource value 解析与搜索测试
- `:core:testWorkspace` 跑 workspace 初始化、默认服务装配与运行时解析测试
- `:core:testStructured` 按拆分后的测试分组串行跑完全部 core JVM 测试入口

推荐验证路径：

- 只改 dex 搜索、inspect 或导出链路时，先跑 `./gradlew :core:testDex`
- 改 manifest 解析链路时，先跑 `./gradlew :core:testResourceManifest`
- 改 resource table / value / search 链路时，先跑 `./gradlew :core:testResourceTable` 和 `./gradlew :core:testResourceValue`
- 改 XML 解码或 resource entry 索引链路时，补跑 `./gradlew :core:testResourceXml` 和 `./gradlew :core:testResourceEntry`
- 改 workspace 状态、target 管理或默认服务装配链路时，先跑 `./gradlew :core:testWorkspace`
- 改动同时跨 dex 与 resource，或准备合并前收口时，最后跑 `./gradlew :core:testStructured`

按命令族跑 `cli` 测试：

```bash
./gradlew :cli:testFast
./gradlew :cli:testResource
./gradlew :cli:testDexQuery
./gradlew :cli:testExport
./gradlew :cli:testStructured
```

说明：

- `:cli:testFast` 只跑 help / workspace / failure rendering 这类快速测试
- `:cli:testResource` 跑 manifest / resources / xml / resource search 相关测试
- `:cli:testDexQuery` 跑 dex 查询与 `inspect-method` 相关测试
- `:cli:testExport` 跑 smali / dex / java 导出相关测试
- `:cli:testStructured` 按拆分后的测试分组串行跑完全部 `cli` 测试入口

推荐验证路径：

- 只改 `cli` 参数解析、帮助文本或错误渲染时，先跑 `./gradlew :cli:testFast`
- 改资源相关链路时，在 `:cli:testFast` 之后补跑 `./gradlew :cli:testResource`
- 改 dex 查询相关链路时，在 `:cli:testFast` 之后补跑 `./gradlew :cli:testDexQuery`
- 改导出相关链路时，在 `:cli:testFast` 之后补跑 `./gradlew :cli:testExport`
- 需要合并前收口或想完整覆盖 `cli` 入口时，最后跑 `./gradlew :cli:testStructured`
- 多个 `:cli:test*` 任务建议串行执行，避免并发写入同一测试结果目录

按职责跑 `mcp` 测试：

```bash
./gradlew :mcp:testFast
./gradlew :mcp:testSession
./gradlew :mcp:testDex
./gradlew :mcp:testResource
./gradlew :mcp:testModels
./gradlew :mcp:testSmoke
./gradlew :mcp:testStructured
```

说明：

- `:mcp:testFast` 跑 session / dex / resource / models 这类不启动 HTTP 服务的单元测试
- `:mcp:testSession` 跑 target session 生命周期、句柄缓存与过期回收测试
- `:mcp:testDex` 跑 dex 查询、`inspect_method`、导出与工作区回退测试
- `:mcp:testResource` 跑 manifest、resource 查询与 XML 解码测试
- `:mcp:testModels` 跑结果映射、字段投影与错误 JSON 渲染测试
- `:mcp:testSmoke` 跑 MCP HTTP 服务器 smoke 测试
- `:mcp:testStructured` 按拆分后的测试分组串行跑完全部 `mcp` 测试入口

推荐验证路径：

- 只改 `McpSessionStore` 或 `open_target_session` 相关链路时，先跑 `./gradlew :mcp:testSession`
- 改 dex 工具或导出链路时，在基础单元测试后补跑 `./gradlew :mcp:testDex`
- 改 manifest、resource 或 XML 相关链路时，在基础单元测试后补跑 `./gradlew :mcp:testResource`
- 改结果映射、字段筛选或错误输出时，补跑 `./gradlew :mcp:testModels`
- 改 HTTP 安装、路由或协议握手时，最后补跑 `./gradlew :mcp:testSmoke`
- 需要合并前收口或想完整覆盖 `mcp` 入口时，最后跑 `./gradlew :mcp:testStructured`

## 打包 CLI

打包 fat jar：

```bash
./gradlew :cli:fatJar
```

生成带启动脚本的分发目录与 zip：

```bash
./gradlew :cli:installShadowDist :cli:shadowDistZip
```

## 打包 MCP

生成 MCP 分发目录：

```bash
./gradlew :mcp:installDist
```

## 运行 CLI

fat jar 入口：

```bash
java -jar cli/build/libs/dexclub-cli-all.jar <command> [args]
```

Windows PowerShell 下推荐优先使用分发脚本：

```powershell
& .\cli\build\install\cli-shadow\bin\cli.ps1 <command> [args]
```

查看帮助：

```bash
java -jar cli/build/libs/dexclub-cli-all.jar --help
java -jar cli/build/libs/dexclub-cli-all.jar help
java -jar cli/build/libs/dexclub-cli-all.jar help find-method
```

Windows PowerShell 如果要传复杂 `--query-json`，优先使用：

```powershell
& .\cli\build\install\cli-shadow\bin\cli.ps1 find-class E:\path\to\workdir --query-json '{"matcher":{"className":{"value":"Sample","matchType":"Contains","ignoreCase":true}}}'
```

说明：

- `cli.ps1` 会比 `cli.bat` 更稳定地保留 PowerShell 参数边界
- 对复杂 JSON 查询，也可以优先使用 `--query-file`

DexKit native 运行提示：

- fat jar 分发场景下，通常只需把 DexKit native 动态库放在 `dexclub-cli-all.jar` 同目录
- 如果不是通过 fat jar 运行，则可以通过 native 文件路径或 native 目录显式指定，也可以通过环境变量传入，或确保其位于 `java.library.path` 可见范围内

## 运行 MCP

当前 `mcp` 模块提供一个基于 HTTP 的 MCP server，默认监听：

- `host = 127.0.0.1`
- `port = 8787`
- `path = /mcp`

Windows PowerShell：

```powershell
$env:DEXCLUB_MCP_PORT="8787"
.\mcp\build\install\mcp\bin\mcp.bat
```

运行参数可通过环境变量调整：

- `DEXCLUB_MCP_HOST`
- `DEXCLUB_MCP_PORT`
- `DEXCLUB_MCP_PATH`
- `DEXCLUB_MCP_TRACE`

说明：

- `mcp` 是面向 Codex / agent 的工具服务，不是交互式 CLI
- 终端默认保留最小运行日志（启动、tool failure、未捕获异常、shutdown）
- 详细轨迹默认写入 `logs/mcp.log`；每次启动会先把旧的 `mcp.log` 归档到 `logs/archive/mcp_<启动时间>.log`，可通过 `DEXCLUB_MCP_TRACE=false` 显式关闭
- `mcp` 默认依赖 DexKit native 动态库位于分发目录的 `lib/` 下

## Skills

仓库当前也维护面向 Codex 的 skill 副本，位于：

- `skills/`

当前主要 skill：

- `dexclub-analysis`
  通过 `mcp__dexclub__` 驱动 APK / Dex / manifest / resources 分析

说明：

- 仓库内 `skills/` 是版本化源码，不一定会被当前机器上的 Codex 自动发现
- 如果需要在本机 Codex 中实际触发，通常还需要同步到 `$CODEX_HOME/skills`
- 具体说明见 [skills/README.md](D:/Code/My/Github/dexclub-cli/skills/README.md)

## 工作区模型

当前命令面遵循两条规则：

- `init` 必须显式传入单文件输入路径
- 其余命令统一在已初始化工作区上执行，`[workdir]` 可省略；省略时默认使用当前目录

当前工作区允许存在多个已初始化 target，但任意时刻只有一个 active target：

- `init <input>`：为该输入创建或刷新 target，并将其设为当前 active target
- `switch <input>`：只在当前工作区内切换到已经初始化过的 target，不创建新 target
- `targets`：列出当前工作区已有 target，并标出当前 active target

受管工作区固定写入：

```text
<workdir>/.dexclub/
```

## 常用命令

初始化工作区：

```bash
java -jar cli/build/libs/dexclub-cli-all.jar init /path/to/app.apk
java -jar cli/build/libs/dexclub-cli-all.jar init /path/to/classes.dex
java -jar cli/build/libs/dexclub-cli-all.jar init /path/to/AndroidManifest.xml
```

查看工作区状态：

```bash
java -jar cli/build/libs/dexclub-cli-all.jar status /path/to/workdir
java -jar cli/build/libs/dexclub-cli-all.jar inspect /path/to/workdir --json
java -jar cli/build/libs/dexclub-cli-all.jar gc /path/to/workdir
```

切换和列出 target：

```bash
java -jar cli/build/libs/dexclub-cli-all.jar switch a.apk
java -jar cli/build/libs/dexclub-cli-all.jar targets /path/to/workdir
java -jar cli/build/libs/dexclub-cli-all.jar targets /path/to/workdir --json
```

Dex 查询：

```bash
java -jar cli/build/libs/dexclub-cli-all.jar find-class /path/to/workdir \
  --query-json '{"matcher":{"className":{"value":"Sample","matchType":"Contains","ignoreCase":true}}}' \
  --json
```

```bash
java -jar cli/build/libs/dexclub-cli-all.jar find-method-using-strings /path/to/workdir \
  --query-file /path/to/query.json
```

方法详情：

```bash
java -jar cli/build/libs/dexclub-cli-all.jar inspect-method /path/to/workdir \
  --descriptor 'Lcom/example/Sample;->name()Ljava/lang/String;' \
  --json
```

代码导出：

```bash
java -jar cli/build/libs/dexclub-cli-all.jar export-class-smali /path/to/workdir \
  --class Lcom/example/Sample; \
  --output /tmp/Sample.smali
```

```bash
java -jar cli/build/libs/dexclub-cli-all.jar export-method-smali /path/to/workdir \
  --method 'Lcom/example/Sample;->name()Ljava/lang/String;' \
  --output /tmp/Sample_method.smali
```

资源命令：

```bash
java -jar cli/build/libs/dexclub-cli-all.jar manifest /path/to/workdir
java -jar cli/build/libs/dexclub-cli-all.jar res-table /path/to/workdir --json
java -jar cli/build/libs/dexclub-cli-all.jar decode-xml /path/to/workdir --path res/layout/activity_main.xml
java -jar cli/build/libs/dexclub-cli-all.jar get-res-value /path/to/workdir --id 0x7f0a0001
```

## CI

仓库当前提供：

- [.github/workflows/build-native.yml](.github/workflows/build-native.yml)
- [.github/workflows/build-cli.yml](.github/workflows/build-cli.yml)
- [.github/workflows/build-mcp.yml](.github/workflows/build-mcp.yml)
- [.github/workflows/build-packages.yml](.github/workflows/build-packages.yml)

职责划分如下：

- `build-native.yml`
  构建 DexKit native 产物
- `build-cli.yml`
  消费 native 产物并打包 CLI 分发物
- `build-mcp.yml`
  消费 native 产物并打包 MCP 分发物
- `build-packages.yml`
  统一驱动 native、CLI、MCP 构建，并在 tag 下统一上传 release 附件

## License

[GNU General Public License v3.0](LICENSE)

```text
Copyright (C) 2024  Gang

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
```
