# `inspect-method` 命令设计

## 目标

本命令用于查看单个方法的关系型详情。

它和现有 `find-method` 的职责区分如下：

- `find-method`：负责按查询条件搜索方法，返回命中列表
- `inspect-method`：负责针对一个已知方法描述符返回详情信息

当前版本覆盖以下五类方法详情：

- `using-fields`
- `callers`
- `invokes`
- `strings`
- `annotations`

本命令不负责：

- 再次做条件搜索
- 展开类详情或字段详情
- 返回参数注解、`opCodes` 等更丰富结果

## CLI 约定

建议命令形式：

```bash
cli inspect-method [workdir] --descriptor <method-descriptor> [--include <sections>] [--json]
```

参数约定：

- `workdir`
  - 可选，沿用现有 CLI 约定
- `--descriptor`
  - 必填
  - 值为完整方法描述符，例如 `Lfoo/Bar;->baz(I)V`
- `--include`
  - 可选
  - 用逗号分隔需要返回的详情块
- `--json`
  - 可选
  - 返回结构化 JSON

补充约束：

- `--descriptor` 在当前工作区内必须唯一命中
- 如果同一描述符出现在多个 dex 源中，命令直接报错，而不是返回数组

## `--include` 规则

当前允许的值为：

- `using-fields`
- `callers`
- `invokes`
- `strings`
- `annotations`

规则如下：

- 未传 `--include` 时，默认返回全部五块
- 传入 `--include` 时，只返回指定块
- 未指定的块在 JSON 中直接省略
- 已指定但没有结果时，返回空数组 `[]`

## JSON 输出结构

`--json` 输出为单个对象，而不是数组。

推荐结构如下：

```json
{
  "method": {
    "className": "foo.Bar",
    "methodName": "baz",
    "descriptor": "Lfoo/Bar;->baz(I)V",
    "sourcePath": "base.apk",
    "sourceEntry": "classes.dex"
  },
  "usingFields": [
    {
      "usingType": "Read",
      "field": {
        "className": "foo.Bar",
        "fieldName": "count",
        "descriptor": "Lfoo/Bar;->count:I",
        "sourcePath": "base.apk",
        "sourceEntry": "classes.dex"
      }
    }
  ],
  "callers": [
    {
      "className": "foo.Caller",
      "methodName": "run",
      "descriptor": "Lfoo/Caller;->run()V",
      "sourcePath": "base.apk",
      "sourceEntry": "classes2.dex"
    }
  ],
  "invokes": [
    {
      "className": "foo.Helper",
      "methodName": "work",
      "descriptor": "Lfoo/Helper;->work()V",
      "sourcePath": "base.apk",
      "sourceEntry": "classes.dex"
    }
  ],
  "strings": [
    "dexclub-needle-string"
  ],
  "annotations": [
    "@foo.Marker(value = \"baz\")"
  ]
}
```

字段规则：

- 顶层固定包含 `method`
- `method` 结构沿用现有 `MethodHitView` 风格
- `usingFields` 使用 camelCase，与 `--include using-fields` 对应
- `callers` / `invokes` 返回现有 `MethodHitView` 风格的列表
- `strings` / `annotations` 返回字符串列表
- `usingFields[*].field` 返回现有 `FieldHitView` 风格的对象
- `usingFields[*].usingType` 直接使用 `Read` / `Write`

## 命名约定

CLI 参数和值使用 kebab-case：

- `inspect-method`
- `using-fields`

JSON 字段使用 camelCase：

- `usingFields`
- `sourcePath`
- `sourceEntry`

## 第一版边界

第一版只定义并实现：

- `inspect-method`
- `--descriptor`
- `--include`
- `--json`
- `using-fields` / `callers` / `invokes`
- `strings`
- `annotations`

第一版暂不实现：

- `inspect-class`
- `inspect-field`
- `opCodes`
- `param-annotations`
- 更复杂的嵌套详情展开
- `--source-path` / `--source-entry` 这类歧义消解参数

## 实现约束

实现时应保持以下分层：

- `dexkit`
  - 负责提供底层 `getMethodUsingFields` / `getMethodCallers` / `getMethodInvokes`
  - 以及 `getMethodUsingStrings` / `getMethodAnnotations`
- `core`
  - 负责聚合为稳定的方法详情模型
- `cli`
  - 负责参数解析、输出模型和文本/JSON 渲染

本命令不应绕过 `core` 直接在 CLI 层拼装 DexKit 结果。
