# Skills

`skills/` 目录保存本仓库维护的 Codex skill 源码。

## 当前包含

- `dexclub-analysis`
  通过 `mcp__dexclub__` 分析 APK、Dex、manifest、resources、classes 与 methods，适合黑盒 Android 逆向、功能定位、实现链路追踪与竞品分析。

## 运行前提

当前 skill 依赖：

- Codex skill 机制
- skill 执行时当前环境已提供 `mcp__dexclub__`

如果 dexclub MCP 不可用，skill 应直接停止，并提示用户先连接或启动 dexclub MCP server，而不是静默回退到 shell 或 CLI。

## 同步到运行时

仓库中的 `skills/` 是源码目录，不一定会被当前机器上的 Codex 自动发现。

如果要在本机实际触发 skill，通常还需要把对应目录同步到：

```text
$CODEX_HOME/skills/
```

Windows 上常见位置类似：

```text
C:\Users\<user>\.codex\skills\
```

例如：

```powershell
Copy-Item -Recurse -Force .\skills\dexclub-analysis C:\Users\<user>\.codex\skills\
```

如果运行时副本和仓库内源码不一致，真实行为应以 `$CODEX_HOME/skills` 中的副本为准。

本仓库中的 `skills/` 源码是权威维护来源；但在未同步到 `$CODEX_HOME/skills` 之前，当前机器上的实际运行行为仍以运行时副本为准。

## 最小验证

确认下面三件事：

1. `dexclub` MCP 已连接
2. 当前会话能看到 `mcp__dexclub__`
3. `dexclub-analysis` 已同步到 `$CODEX_HOME/skills`

然后可在一个无上下文新会话里显式要求：

```text
请使用 $dexclub-analysis 分析某个 APK 功能入口。
```

如果 skill 与 MCP 都生效，Codex 应优先：

- 使用 `mcp__dexclub__`
- 先 `open_target_session`
- `open_target_session.input` 使用绝对路径，不要传相对路径
- 首轮优先 `brief=true`，只有确有必要再显式 `fields`
- `find_methods` 只有在已有 `class_name_contains` 或 `method_name_contains` 这类主过滤线索时才作为入口
- 先 `inspect_method` 后 `export_*`

## 相关文件

- [dexclub-analysis/SKILL.md](dexclub-analysis/SKILL.md)
- [dexclub-analysis/agents/openai.yaml](dexclub-analysis/agents/openai.yaml)
