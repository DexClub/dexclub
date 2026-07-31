# Dex Query JSON Contract

CLI 和 MCP 共用 `domain-core` 中的三个公共 root DTO：

- `FindClassQuery`
- `FindMethodQuery`
- `FindFieldQuery`

它们复用 `dexkit-binding` 的完整 `Matcher*` 结构，并映射到 binding `FindClass`、`FindMethod`、`FindField` 执行。公共合同不包含 `searchInClasses`、`searchInMethods`、`searchInFields`，即使传入空数组也会按未知字段拒绝。

CLI 通过 `--query-json` 或 `--query-file` 传入 JSON 文本：

```bash
cli find-class --query-json '{"matcher":{"className":{"value":"com.example","matchType":"Contains"}}}'
```

MCP 暴露三个对应工具：

- `find_classes`
- `find_methods`
- `find_fields`

MCP 的 `query` 是必填 JSON object，不是 JSON 字符串。tool schema 从公共 DTO serializer descriptor 生成，以 JSON Schema `$defs/$ref` 表达递归 Matcher 结构，并对普通 object 使用 `additionalProperties: false`。

三个 MCP 工具都支持 `offset`、`limit`、`brief`、`fields`。`limit` 默认 50，最大 200；class 和 method 在使用 `session_id` 时可投影 handle，field 不提供 handle。

`BatchFind*` 仍保留在 binding 层供进程内调用，但不作为 domain、CLI 或 MCP 公共能力。完整 `Find*` 已能表达相同查询语义，入口层不再维护仅用于缓存优化的 batch 命令。
