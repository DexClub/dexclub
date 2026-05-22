# Skills

当前仓库在 `skills/` 下维护 Codex skill 的版本化副本。

这些文件的职责是：

- 跟随仓库一起演进、review 与提交
- 作为运行时 skill 的源码来源
- 为 AI / agent 使用 `dexclub` MCP 提供稳定工作流约束

## 当前 skill

- `dexclub-analysis`
  用于通过 `mcp__dexclub__` 分析 APK、Dex、manifest、resources、classes 与 methods，适合黑盒 Android 逆向、功能定位、实现链路追踪与竞品分析。

## 运行前提

`dexclub-analysis` 不是独立运行的本地脚本，它依赖：

- Codex skill 机制
- 在 skill 实际执行时，`mcp__dexclub__` 已在当前 Codex 环境中可用

skill 可以被 Codex 正常发现；但在真正执行前，如果 dexclub MCP 不可用，skill 应停止并提示用户先连接或启动 dexclub MCP server，而不是静默回退到 shell 或 CLI。

## 仓库内 skill 与运行时 skill

仓库中的 `skills/` 目录是**版本化源码**，不一定会被 Codex 自动发现。

如果你希望当前机器上的 Codex 实际触发这些 skill，通常还需要把对应目录同步到：

```text
$CODEX_HOME/skills/
```

在 Windows 上，常见位置类似：

```text
C:\Users\<user>\.codex\skills\
```

例如当前 skill 可以同步到：

```text
C:\Users\<user>\.codex\skills\dexclub-analysis\
```

## 同步方式

最简单的方式是手动复制：

```powershell
Copy-Item -Recurse -Force .\skills\dexclub-analysis C:\Users\<user>\.codex\skills\
```

如果运行时 skill 和仓库内 skill 不一致，真实行为应以 `$CODEX_HOME/skills` 中的副本为准。

## 最小验证

在 Codex 中确认：

1. `dexclub` MCP 已连接
2. 当前会话能看到 `mcp__dexclub__`
3. skill 已被同步到 `$CODEX_HOME/skills`

之后可在一个无上下文新会话里显式要求：

```text
请使用 $dexclub-analysis 分析某个 APK 功能入口。
```

如果 skill 与 MCP 都生效，Codex 应优先：

- 使用 `mcp__dexclub__`
- 先 `open_target_session`
- 首轮优先 `brief=true`，只有确有必要再显式 `fields`
- 先 `inspect` 后 `export`

## 相关文件

- [dexclub-analysis/SKILL.md](D:/Code/My/Github/dexclub-cli/skills/dexclub-analysis/SKILL.md)
- [dexclub-analysis/agents/openai.yaml](D:/Code/My/Github/dexclub-cli/skills/dexclub-analysis/agents/openai.yaml)
- [dexclub-analysis/references/workflow-notes.md](D:/Code/My/Github/dexclub-cli/skills/dexclub-analysis/references/workflow-notes.md)
