# DexClub CLI

`cli-app` 是 dexclub 的命令行入口，用于管理本地 target 工作区，以及执行 Dex 查询、方法检查、代码导出和 Android 资源分析。

## 构建与运行

要求 JDK 21，并在仓库根目录执行：

```bash
./gradlew :cli-app:fatJar
java -jar cli-app/build/libs/dexclub-all.jar --help
```

需要完整分发包时执行：

```bash
./gradlew :cli-app:installShadowDist :cli-app:shadowDistZip
```

Windows PowerShell 中可将 `./gradlew` 替换为 `.\gradlew.bat`。

## 基本流程

CLI 以工作区为调用边界。先用一个输入文件初始化工作区，再在同一工作区中执行查询：

```bash
java -jar cli-app/build/libs/dexclub-all.jar init /path/to/app.apk
java -jar cli-app/build/libs/dexclub-all.jar status /path/to/workdir
java -jar cli-app/build/libs/dexclub-all.jar find-method /path/to/workdir \
  --query-json '{"matcher":{"name":{"value":"onCreate","matchType":"Equals"}}}'
```

除 `init` 外，命令只消费已经初始化的工作区，不会隐式创建 `.dexclub`。可选的 `[workdir]` 省略时直接使用当前目录，CLI 不会向父目录搜索工作区。

## 命令目录

工作区生命周期：

| 命令 | 作用 |
| --- | --- |
| `init <input>` | 用单个输入文件初始化工作区并设为 active target |
| `switch <input>` | 切换到当前工作区中已初始化的 target |
| `targets [workdir]` | 列出 target 并标记 active target |
| `status [workdir]` | 只读检查工作区状态和问题 |
| `inspect [workdir]` | 查看 active target 摘要和能力 |
| `refresh [workdir]` | 显式重建 active target snapshot |
| `gc [workdir]` | 删除 active target 中可安全重建的派生状态 |

Dex 分析：

| 命令组 | 命令 |
| --- | --- |
| 查询 | `find-class`、`find-method`、`find-field` |
| 检查 | `inspect-method` |
| 类导出 | `export-class-dex`、`export-class-smali`、`export-class-java` |
| 方法导出 | `export-method-dex`、`export-method-smali`、`export-method-java` |

Android 资源：

| 命令 | 作用 |
| --- | --- |
| `manifest` | 解析 manifest，可按组件名称和类型过滤 |
| `res-table` | 查看资源表摘要 |
| `list-res` | 按资源身份、类型、路径和配置列出条目 |
| `get-res-value` | 解析指定资源及其配置变体 |
| `find-res-values` | 按 decoded/raw/reference/bag 等目标搜索资源值 |
| `decode-xml` | 解码二进制 XML |

运行 `--help` 或 `<command> --help` 可查看当前版本的完整参数和输出约定。

## Dex 查询输入

`find-class`、`find-method` 和 `find-field` 接受完整的结构化查询：

```bash
java -jar cli-app/build/libs/dexclub-all.jar find-method \
  --query-json '{"matcher":{"name":{"value":"onCreate","matchType":"Equals"}}}'
```

较大的查询可以放入 JSON 文件：

```bash
java -jar cli-app/build/libs/dexclub-all.jar find-class \
  --query-file ./queries/activity.json
```

查询根对象、递归 matcher 和不支持字段见 [Dex 查询合同](../.docs/v4/dex-query-contract.md)。不要仅凭 DexKit 的原生 API 形状推断 CLI 参数。

资源命令同样支持结构化筛选。例如搜索包含 `example.com` 的字符串资源：

```bash
java -jar cli-app/build/libs/dexclub-all.jar find-res-values \
  --query-json '{"resourceType":"string","value":"example.com","contains":true}'
```

支持结构化输出的命令可使用 `--json`，具体以命令帮助为准。

## Native 库

Dex 查询依赖 DexKit native 动态库。优先使用生成的完整分发包；自定义运行环境可以通过以下任一方式显式指定动态库目录：

- 环境变量 `DEXCLUB_DEXKIT_NATIVE_LIBRARY_DIR`
- JVM property `dexclub.dexkit.native.library.dir`

直接运行 fat jar 且未指定目录时，需要确保当前平台的 native 库位于可被运行时找到的位置。

调试 CLI 的未处理异常时，可设置 `DEXCLUB_CLI_DEBUG_STACKTRACE=true` 输出 stack trace。

## 测试

```bash
./gradlew :cli-app:testFast
./gradlew :cli-app:testStructured
```

需要定点验证时可使用 `testHelp`、`testWorkspace`、`testFailureRendering`、`testResource`、`testDexQuery` 和 `testExport`。

返回[项目 README](../README.md)。
