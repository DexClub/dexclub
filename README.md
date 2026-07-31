# dexclub

`dexclub` 是一个面向 `dex / apk / Android 资源` 的 Kotlin 多模块工具集。项目通过共享的应用层提供两种入口：适合终端和脚本使用的 CLI，以及供 AI / agent 调用的 HTTP MCP server。

## 主要能力

- 管理以 `.dexclub` 为边界的 target 工作区
- 查询类、方法和字段，并检查方法详情
- 导出 dex、smali 和 Java 证据
- 结构化解析 `AndroidManifest.xml`、`resources.arsc` 和二进制 XML
- 按资源身份、配置、值类型和命中位置检索资源
- 通过有状态 target session 或无状态 workdir 调用 MCP 工具

## 选择入口

| 入口 | 适用场景 | 文档 |
| --- | --- | --- |
| `cli-app` | 本地终端、脚本、人工分析 | [CLI 使用说明](./cli-app/README.md) |
| `mcp-app` | MCP client、AI / agent 集成 | [MCP server 使用说明](./mcp-app/README.md) |

两个入口共享 Dex 查询合同。完整的 matcher、字段和约束见 [Dex 查询合同](./.docs/v4/dex-query-contract.md)。

## 模块结构

根工程直接包含 4 个子项目，并通过 included build 接入 `dexkit-binding`：

| 模块 | 职责 |
| --- | --- |
| `cli-app` | CLI 入口、参数解析和终端渲染 |
| `mcp-app` | Streamable HTTP MCP server 和工具适配 |
| `app-service` | 共享 use case 和 runtime 装配 |
| `domain-core` | 稳定模型、能力边界和底层实现基础 |
| `dexkit-binding` | DexKit 绑定层 |

`gui-app` 是计划中的兼容方向，当前未接入根构建。

## 开始构建

环境要求：JDK 21。

首次拉取后初始化 submodule：

```bash
git submodule update --init --recursive
```

执行常用快速验证：

```bash
./gradlew verifyFast
```

执行完整的结构化验证：

```bash
./gradlew :app-service:testStructured
./gradlew :cli-app:testStructured
./gradlew :mcp-app:testStructured
./gradlew :domain-core:testWorkspace
```

仅编译主要模块：

```bash
./gradlew :app-service:compileKotlin :domain-core:compileKotlinJvm :cli-app:compileKotlin :mcp-app:compileKotlin
```

构建可运行产物：

```bash
./gradlew :cli-app:fatJar
./gradlew :mcp-app:installDist
```

Windows PowerShell 中可将 `./gradlew` 替换为 `.\gradlew.bat`。具体的运行方式和模块测试任务见对应入口文档。

## 文档与维护

- [.docs/v4/index.md](./.docs/v4/index.md)：当前架构边界、模块关系和 v4 文档入口
- [.docs/native-maintenance.md](./.docs/native-maintenance.md)：`dexkit-binding / vendor / Android native` 维护说明
- [skills/README.md](./skills/README.md)：仓库内 skills 的维护说明

`dexkit-binding/vendor/DexKit/` 是 vendored 上游 DexKit，`dexkit-binding/vendor/libcxx-prefab/` 是本地 `libcxx` prefab 仓库。Android SDK / NDK、`cmake` 和 `ninja` 只在 native / Android 维护路径下需要。DexKit 桌面端前提可参考[上游运行说明](https://luckypray.org/DexKit/zh-cn/guide/run-on-desktop.html)。

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
