# DexClub MCP Server

`mcp-app` 通过 stdio 或 Streamable HTTP MCP 暴露 dexclub 的 target session、Dex 分析和 Android 资源能力，适合接入 MCP client、AI 或自动化 agent。

## 构建与启动

要求 JDK 21。在仓库根目录生成应用分发目录：

```bash
./gradlew :mcp-app:installDist
```

默认启动 HTTP 服务，兼容已有启动方式：

```bash
./mcp-app/build/install/mcp/bin/mcp
```

Windows PowerShell：

```powershell
.\gradlew.bat :mcp-app:installDist
.\mcp-app\build\install\mcp\bin\mcp.bat
```

默认端点是 `http://127.0.0.1:8787/mcp`。MCP client 的具体配置格式由 client 决定，将其 Streamable HTTP endpoint 指向该地址即可。

服务本身不提供面向公网的认证边界。默认只监听回环地址；将 `DEXCLUB_MCP_HOST` 改为非回环地址时，只应部署在可信网络或已有访问控制的代理之后。

### stdio 与 Codex

本机 MCP client 可以直接启动 stdio 进程，无需预先启动 HTTP 服务：

```bash
./mcp-app/build/install/mcp/bin/mcp --transport=stdio
```

Windows PowerShell：

```powershell
.\mcp-app\build\install\mcp\bin\mcp.bat --transport=stdio
```

支持 `--transport=stdio`、`--transport stdio` 和显式的 `--transport=http`。省略参数时保持 HTTP 模式，`--help` 显示启动参数。

在 Codex 的 `config.toml` 中配置命令型 MCP。下面的路径应替换为本机分发包的实际绝对路径：

```toml
[mcp_servers.dexclub]
command = "cmd.exe"
args = ["/d", "/c", "D:/Tools/dexclub/bin/mcp.bat", "--transport=stdio"]
startup_timeout_sec = 60
```

同一服务配置使用 `command` / `args` 后移除原来的 `url` 字段，重新建立 Codex 会话以加载配置。工具、版本校验和配套 skill 合同在两种传输下保持一致。

stdio 模式不监听端口；`stdout` 专用于逐行 MCP JSON-RPC，启动信息、日志及普通控制台输出转入 `stderr`。客户端关闭 stdin 后，进程关闭 MCP session 并释放 target runtime。`DEXCLUB_MCP_HOST`、`DEXCLUB_MCP_PORT` 和 `DEXCLUB_MCP_PATH` 只影响 HTTP 入口；stdio 采用本机文件访问策略。

## Target 定位方式

需要 target 的工具支持两种调用方式：

| 方式 | 适用场景 |
| --- | --- |
| `session_id` | 推荐用于连续分析。先调用 `open_target_session`，复用 runtime，并可继续使用返回的 `method_handle` / `class_handle` |
| `workdir` | 适合独立的无状态调用。直接指定已初始化的 `.dexclub` 工作区 |

`open_target_session` 的 `input` 可以是 APK、Dex 等受支持输入。相对路径按 MCP server 进程的当前目录解析。

Session 快照中的 `inventoryCounts` 统计当前 target 识别出的顶层物料，不展开归档内容。例如 APK 输入通常表现为 `apkCount=1`，其中的 `classes.dex`、`AndroidManifest.xml` 和 `resources.arsc` 不会分别增加其他计数；实际可用能力以同一快照中的 `capabilities` 为准。

## 工具目录

Target session：

- `open_target_session`
- `list_target_sessions`
- `get_target_session`
- `refresh_target_session`
- `close_target_session`
- `diagnose_target_sessions`

Dex 分析：

- `find_classes`、`find_methods`、`find_fields`
- `inspect_method`
- `export_method_java`、`export_method_smali`
- `export_class_java`、`export_class_smali`

Android 资源：

- `manifest`
- `list_res`
- `find_resource_values`
- `get_resource_value`
- `decode_xml`

工具的输入 schema 由 server 直接发布，应以 client 中显示的 schema 为准。

## 查询与结果控制

`find_classes`、`find_methods` 和 `find_fields` 通过 `query_file` 读取服务端可访问的绝对路径 JSON 文件。文件中的 `query` 对应完整的 `FindClassQuery`、`FindMethodQuery` 和 `FindFieldQuery`。文件 envelope、版本要求、递归 matcher、字段定义和明确排除项见 [Dex 查询合同](../.docs/v4/dex-query-contract.md)。

Dex 查询支持 `offset`、`limit`、`brief` 和 `fields` 控制分页与投影。默认 `limit` 为 50，最大为 200。连续调用中应优先保留并复用查询返回的 handle，避免依赖展示文本重新定位实体。

资源工具保留资源配置变体和 typed value。`find_resource_values` 可区分 `decoded_value`、`raw_data`、`reference`、`bag_key` 或 `any` 命中位置；`get_resource_value` 可按资源 ID 或 `resource_type + name` 定位，并通过 `include_all_variants` 返回配置变体。

## 配置

| 环境变量 | 默认值 | 作用 |
| --- | --- | --- |
| `DEXCLUB_MCP_HOST` | `127.0.0.1` | HTTP 监听地址 |
| `DEXCLUB_MCP_PORT` | `8787` | HTTP 监听端口 |
| `DEXCLUB_MCP_PATH` | `/mcp` | MCP HTTP 路径 |
| `DEXCLUB_MCP_TRACE` | `true` | 记录 HTTP / tool 级详细 trace；设为 `false` 后不写 `logs/mcp.log` |
| `DEXCLUB_MCP_STDERR` | `false` | 将 tool failure、未捕获异常和 shutdown 等运行期信息输出到 `stderr` |
| `DEXCLUB_MCP_SESSION_IDLE_TIMEOUT_MINUTES` | `10` | session 空闲释放时间，必须为正数 |
| `DEXCLUB_MCP_MAX_SESSIONS` | `5` | session 上限，超出后按最近最少使用顺序淘汰 |
| `DEXCLUB_MCP_MAX_HANDLES_PER_SESSION` | `1000` | 单个 session 的 method/class handle 总上限 |
| `DEXCLUB_MCP_MAX_TRACE_ARCHIVES` | `10` | trace 日志归档保留数量 |
| `DEXCLUB_DEXKIT_NATIVE_LIBRARY_DIR` | 未设置 | 显式指定 DexKit native 动态库目录 |

也可以使用 JVM property `dexclub.dexkit.native.library.dir` 指定 native 目录。

通过 `installDist` 脚本启动时，运行目录以分发包的 `bin` 为基准，trace 默认写入 `mcp-app/build/install/mcp/bin/logs/mcp.log`。启动提示和监听地址始终输出；详细运行期控制台日志由 `DEXCLUB_MCP_STDERR` 控制。

## 测试

```bash
./gradlew :mcp-app:testFast
./gradlew :mcp-app:testStructured
```

需要定点验证时可使用 `testSession`、`testDex`、`testResource`、`testModels` 和 `testSmoke`。`testSmoke` 包含 HTTP 和 stdio 启动、协议路径及 stdio EOF 退出验证，因此不属于 `testFast`。

返回[项目 README](../README.md)。
