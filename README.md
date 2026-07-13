# dexclub

`dexclub` 是一个面向 `dex / apk / Android 资源` 的 Kotlin 多模块项目，当前同时提供：

- 基于工作区的 CLI，用于 dex 查询、方法检查、代码导出和 Android 资源解析
- 基于 HTTP 的 MCP server，用于把同一批能力暴露给 AI / agent 调用

## 能力概览

- 初始化并管理 `.dexclub` 工作区
- 列出、切换、刷新 active target
- 查询类、方法、字段
- 通过 `inspect_method` 检查方法详情
- 通过 `export_*` 导出 dex、smali、Java 证据
- 解析 `AndroidManifest.xml`
- 读取 `resources.arsc`
- 解码二进制 XML
- 列出、解析、搜索资源条目
- 通过 MCP 暴露 target session、dex、resource 相关工具

## 模块结构

根工程当前直接包含 4 个子项目，并通过 `included build` 接入 `dexkit-binding`：

- `cli-app` CLI 入口
- `mcp-app` MCP server 入口
- `app-service` 共享应用层，负责 use case 和 runtime 装配
- `domain-core` 稳定模型、能力边界与底层实现基础
- `dexkit-binding` DexKit 绑定层
- `gui-app` （计划中）未来兼容方向，当前未接入根构建

## 快速开始

环境要求：

- JDK 21

首次拉取后先初始化 submodule：

```bash
git submodule update --init --recursive
```

常用快速验证：

```bash
./gradlew verifyFast
```

结构化验证：

```bash
./gradlew :app-service:testStructured
./gradlew :cli-app:testStructured
./gradlew :mcp-app:testStructured
./gradlew :domain-core:testWorkspace
```

如果只需要编译主模块：

```bash
./gradlew :app-service:compileKotlin :domain-core:compileKotlinJvm :cli-app:compileKotlin :mcp-app:compileKotlin
```

## 打包与运行

打包 CLI：

```bash
./gradlew :cli-app:fatJar
./gradlew :cli-app:installShadowDist :cli-app:shadowDistZip
```

打包 MCP：

```bash
./gradlew :mcp-app:installDist
```

CLI 示例：

```bash
java -jar cli-app/build/libs/dexclub-all.jar --help
java -jar cli-app/build/libs/dexclub-all.jar init /path/to/app.apk
java -jar cli-app/build/libs/dexclub-all.jar status /path/to/workdir
```

MCP 示例：

```powershell
# PowerShell
$env:DEXCLUB_MCP_HOST="127.0.0.1" # 默认监听本机回环地址
$env:DEXCLUB_MCP_PORT="8787"      # 默认端口
$env:DEXCLUB_MCP_PATH="/mcp"      # 默认 MCP 路径
.\mcp-app\build\install\mcp\bin\mcp.bat
```

常用环境变量：

- `DEXCLUB_MCP_HOST`：监听地址，默认 `127.0.0.1`；如果改成非回环地址，MCP 服务会暴露到局域网或外网，只建议在可信网络使用
- `DEXCLUB_MCP_PORT`：监听端口，默认 `8787`
- `DEXCLUB_MCP_PATH`：HTTP 路径，默认 `/mcp`
- `DEXCLUB_MCP_TRACE`：是否启用详细 trace 诊断，默认 `true`；设为 `false` 后不再写 `logs/mcp.log`，也不记录 HTTP / tool 级详细 trace
- `DEXCLUB_MCP_STDERR`：是否输出运行期控制台 `stderr` 日志，默认 `false`；设为 `true` 后 tool failure、未捕获异常、shutdown 等信息会打印到控制台。启动阶段的提示和监听地址会始终展示
- `DEXCLUB_MCP_SESSION_IDLE_TIMEOUT_MINUTES`：session 空闲超时分钟数，默认 `10`，超时自动释放，避免内存长时间占用
- `DEXCLUB_MCP_MAX_SESSIONS`：同时保留的 session 上限，默认 `5`；超出后按最近最少使用顺序淘汰旧 session，避免多会话长期占用内存
- `DEXCLUB_MCP_MAX_HANDLES_PER_SESSION`：单个 session 内可保留的 `method_handle` / `class_handle` 总数上限，默认 `1000`；超出后按最近最少使用顺序淘汰旧 handle，避免长会话累计过多句柄状态
- `DEXCLUB_MCP_MAX_TRACE_ARCHIVES`：trace 日志归档保留数量，默认 `10`
- `DEXCLUB_DEXKIT_NATIVE_LIBRARY_DIR`：显式指定 DexKit native 动态库所在目录；加载时会按当前平台文件名在该目录下查找库文件

补充说明：

- 通过 `installDist` 生成的脚本启动时，`APP_HOME` 会自动指向分发目录，日志默认写到 `mcp-app/build/install/mcp/bin/logs/`
- 如果直接以 `java -jar` 方式运行且未显式设置 native 路径，DexKit native 库需要位于 jar 同目录，或通过上面的环境变量 / JVM property 提供

## 文档入口

- [.docs/v4/index.md](./.docs/v4/index.md)
  当前结构边界、模块关系和 `v4` 文档入口
- [.docs/native-maintenance.md](./.docs/native-maintenance.md)
  `dexkit-binding / vendor / Android native` 维护说明
- [skills/README.md](./skills/README.md)
  仓库内 skills 的维护说明

## 补充说明

- `dexkit-binding/vendor/DexKit/` 是 vendored 上游 DexKit
- `dexkit-binding/vendor/libcxx-prefab/` 是本地 `libcxx` prefab 仓库
- Android SDK / NDK、`cmake`、`ninja` 只在 native / Android 维护路径下需要
- DexKit 桌面端运行前提可参考上游文档：
  [DexKit 桌面端运行说明](https://luckypray.org/DexKit/zh-cn/guide/run-on-desktop.html)

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
