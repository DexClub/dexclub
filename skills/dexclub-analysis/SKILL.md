---
name: dexclub-analysis
description: Use when Codex needs to analyze APK, Dex, manifest, resources, classes, or methods through dexclub MCP, especially for black-box Android reverse-engineering, feature location, implementation tracing, and competitor analysis. This skill runs only when `mcp__dexclub__` is available at execution time.
---

# DexClub Analysis

## Overview

Use this skill to drive `mcp__dexclub__` as the primary analysis surface for APK/Dex/manifest/resource inspection. Treat dexclub MCP as a runtime prerequisite: when the skill is actually invoked, if `mcp__dexclub__` is unavailable, stop early and tell the user the skill cannot proceed until dexclub MCP is connected.

## Hard Gate

Before any analysis, confirm that `mcp__dexclub__` is available in the current tool list.

If `mcp__dexclub__` is unavailable:

- stop
- tell the user dexclub MCP is required for this skill
- ask them to configure, start, or reconnect the dexclub MCP server

Do not:

- fall back to shell-based reverse engineering
- read local decompiled output as the primary path
- silently switch to CLI commands

This skill is intentionally MCP-first, not CLI-first.

## Default Workflow

Use the following default order unless the current task clearly justifies a deviation:

1. `open_target_session`
2. choose one lowest-cost entry path
3. use `brief=true` first, and add `fields` only when the next step truly needs explicit projection
4. use `inspect_method` to read one-layer facts
5. use `export_*` only when evidence text is actually needed
6. summarize conclusion, evidence, and remaining uncertainty

Keep the workflow recoverable and iterative. Do not implement it as a rigid state machine.

If the user already provides a high-specificity anchor, prefer the shortest path that can test it before falling back to the full default route.

If the user already provides a full method descriptor such as `Lpkg/Class;->name(args)Ret`, do not start with `find_methods`.

- open a target session
- go directly to `inspect_method` with `descriptor`
- use `export_method_java` or `export_method_smali` with `descriptor` only when implementation text is needed

Treat a full descriptor as a direct object reference, not as a search hint.

When using `open_target_session`:

- pass an absolute filesystem path in `input`
- do not pass a relative path such as `app.apk`
- remember that relative paths are resolved against the dexclub MCP process working directory, not the user's conversational cwd

## Session Rules

Always prefer `target-session-first`.

- open a target session unless the task is clearly a one-shot light query
- when opening a target session, always use an absolute `input` path to an existing file
- after obtaining `session_id`, keep using it
- do not keep redundantly passing `workdir` once `session_id` exists
- prefer `method_handle` and `class_handle` after they are returned by dexclub
- distinguish tool input names from projected result field names:
  - tool inputs use `method_handle` / `class_handle`
  - result fields use `methodHandle` / `classHandle`
- never invent a bare `handle` field name in `fields`

If `session_id not found`:

- reopen the target session
- return to the latest useful step and continue

If `open_target_session` reports that the input does not exist:

- first check whether `input` was passed as a relative path
- retry with an absolute path to the existing APK/dex/resource target
- do not assume the MCP process is running in the same directory as the conversation workspace

If underlying workspace materials changed and you need the same target session to observe the new snapshot:

- call `refresh_target_session`
- treat existing `method_handle` / `class_handle` as invalid after refresh
- reacquire handles through fresh `find_*` or `inspect_*` results

If `method_handle not found` or `class_handle not found`:

- reacquire the object through `find_*` or `inspect_*`
- do not invent or reconstruct handles manually

If analysis is being resumed after:

- restoring a chat
- restarting Codex
- restarting the dexclub MCP server

then do not assume the previous `session_id` is still valid.

First confirm runtime state through:

- `get_target_session`
- `list_target_sessions`
- `diagnose_target_sessions`

Only continue using previous handles when the target session is confirmed to still exist. Otherwise, rebuild the session on the MCP path instead of drifting into `workdir` fallback for deep analysis.

## Entry Strategy

In black-box analysis, use this default priority:

1. `find_methods_using_strings`
2. `find_classes_using_strings`
3. `manifest`
4. `find_resource_values` / `get_resource_value` / `list_res`
5. `find_methods`

Do not start with a broad `find_methods` query when stronger anchors already exist.

Treat `find_methods` as a class-name or method-name driven search first.

- `class_name_contains` and `method_name_contains` are the primary narrowing inputs
- `descriptor_contains` is only a secondary substring filter applied after the primary search
- do not choose `find_methods` when `descriptor_contains` is the only clue
- if the only concrete clue is already a full method descriptor, skip `find_methods` and go straight to `inspect_method`

During the first positioning round, choose one lowest-cost viable path.

Do not simultaneously expand:

- `manifest include_text=true`
- large `list_res`
- broad `find_methods`
- multiple `export_*`

If the current entry path is weak, then backtrack and switch to the next path.

If the current path has already accumulated `2~3` broad searches without producing stronger evidence, stop extending that same branch unless the next query clearly introduces:

- a new string, resource, or manifest clue
- a new caller / callee / annotation / field fact
- a concrete hypothesis to validate
- a materially smaller candidate set

Do not keep broadening the same branch only by swapping near-synonym keywords, nearby class-name fragments, or vague method-name variations.

After `2~3` consecutive narrowing steps inside the same branch, stop for an internal checkpoint before continuing. At that checkpoint, restate:

- what the current branch already established
- what is still unknown
- why the next query is expected to add a new fact

If that explanation cannot be made clearly, backtrack instead of continuing the branch.

## Parameter Discipline

Keep arguments minimal.

Default rules:

- use `brief=true` for `find_*`, `list_res`, and `find_resource_values` unless more detail is required
- for the first target-session-based narrowing call, prefer `brief=true` without explicit `fields`
  - current MCP brief defaults already return compact identifiers
  - only add `fields` when the next step truly depends on specific projected keys
- use the smallest useful `fields`
- do not send irrelevant `include`, `fields`, or `include_text`
- once a handle exists, do not keep repeating full descriptor and source constraints unless disambiguation is needed
- if you do not have `session_id`, do not request `methodHandle` or `classHandle` in `fields`

For dexclub MCP `find_*` calls, do not guess field aliases. Use only exact field names supported by the tool.

- never request `handle` or `name`
  - method results use `methodHandle`, not `handle`
  - class results use `classHandle`, not `handle`
  - method name uses `methodName`, not `name`
  - class name uses `className`, not `name`
- when you do specify `fields`, use only these exact names:
  - `find_methods` / `find_methods_using_strings`:
    - `descriptor`
    - `sourcePath`
    - `sourceEntry`
    - `methodHandle`
    - only when needed: `className`, `methodName`
  - `find_classes_using_strings`:
    - `className`
    - `sourcePath`
    - `sourceEntry`
    - `classHandle`
- when the first target-session-based `find_*` call is only used to get a compact next-step identifier, omitting `fields` under `brief=true` is acceptable
  - current MCP brief defaults already return compact useful identifiers
  - this can reduce projection mistakes
- if you do need to specify `fields`, choose them from the exact supported names above and keep them minimal for the current step
- for `class_name_contains`, use normal dotted class-name fragments such as `com.foo.Bar` or `foo.Bar`
  - do not use descriptor form like `Lcom/foo/Bar;`
  - do not use slash-form fragments like `com/foo/Bar`
- treat `descriptor_contains` as a raw substring filter, not as a direct object reference
  - it does not replace `class_name_contains` or `method_name_contains` as the primary `find_methods` selector
  - do not call `find_methods` with only `descriptor_contains`
  - do not switch to `descriptor_contains=Lpkg/Class;->` when you already intend to target one concrete method or one concrete owner
  - if you already have a full method descriptor, go straight to `inspect_method` or `export_method_*`
- do not synthesize or rewrite method descriptors from class exports, Java text, or obfuscated guesses before export
  - if an exact method needs to be exported, reacquire the concrete descriptor from MCP search or handle-backed results first

The goal is not merely token savings; it is to reduce drift and keep the analysis path controlled.

## Manifest Rules

Default to structured manifest inspection.

- do not set `include_text=true` by default
- only request manifest raw XML when direct XML evidence is needed
- if only certain sections matter, constrain `include`
- if you do specify `include`, use only these exact section names:
  - `uses-sdk`
  - `application`
  - `uses-permissions`
  - `defined-permissions`
  - `uses-features`
  - `queries`
  - `activities`
  - `activity-aliases`
  - `services`
  - `receivers`
  - `providers`

## Inspect and Export Rules

Default order:

1. locate
2. inspect
3. export

`export_*` is a heavy step. Do not use it as the default first move.

Before another export, another broad search, or a switch to a different major branch, identify the concrete uncertainty that step is intended to resolve.

Before exporting, make sure:

- the candidate set is already small enough
- the export has a clear purpose
- the export is not being used as a substitute for continued narrowing

Skill v1 default limits:

- export no more than `1~2` methods in one round
- export no more than `1` class in one round

These are default budgets, not absolute bans.

If there are still too many candidates, continue narrowing first.

If you exceed the default export budget, have an explicit reason. Valid reasons include:

- Java export is incomplete and smali evidence is required
- the new export directly tests a key branch hypothesis
- `inspect_*` only provides one-layer facts and cannot answer the question
- the new export introduces a new evidence type rather than repeating the same kind of implementation

When method exports in the same analysis round are about to move beyond the default budget, especially from the third method export onward, stop and reassess.

At that checkpoint, first state:

- the current working conclusion
- what each exported object already proved
- what exact uncertainty still remains

Only continue exporting if the next export is tightly targeted at that remaining uncertainty.

For resource and smali MCP calls, do not guess optional parameters.

- `find_resource_values` fields:
  - `resourceId`
  - `type`
  - `name`
  - `value`
  - `sourcePath`
  - `sourceEntry`
- `list_res` fields:
  - `resourceId`
  - `type`
  - `name`
  - `filePath`
  - `sourcePath`
  - `sourceEntry`
  - `resolution`
- for `export_method_smali`, omit `mode` unless you explicitly need `class`
  - supported values are only `snippet` and `class`

Do not keep exporting sibling methods or nearby helpers merely because they look related.

When deciding between Java and smali:

- prefer Java first for quick semantic understanding
- switch to smali only when Java is incomplete, misleading, or insufficient for control-flow proof
- do not export both Java and smali for the same method unless there is a concrete reason

When several candidate methods already point to the same owner class:

- prefer one class export over many more sibling method exports
- but do not export the class unless class-level context is likely to answer the remaining question

## Error Recovery

Treat the following as recoverable, not terminal:

- `session_id not found`
- `method_handle not found`
- `class_handle not found`
- unsupported `include` sections
- unsupported `fields`

Recovery order:

1. determine whether this is context loss or parameter error
2. rebuild session or reacquire handles for context loss
3. narrow `include` / `fields` / parameters for parameter errors
4. retry on the MCP path

If the error mentions unsupported `fields`, first check for guessed aliases such as:

- `handle` instead of `methodHandle` / `classHandle`
- `name` instead of `methodName` / `className`

Do not immediately fall back to shell or source-code reading because of these errors.

## Output Discipline

When answering the user, distinguish:

- clues
- facts
- evidence
- conclusions

Do not promote a single hit directly into a final conclusion.

If the user is only asking for the likely implementation location or entry owner, answer once the evidence is sufficient for that narrower question, and state any remaining uncertainty instead of defaulting to a full implementation trace.

Prefer ending each analysis round with:

1. current conclusion
2. key supporting evidence
3. what remains uncertain

## Practical Reminders

In real usage, the most common drift patterns are:

- expanding multiple heavy entry paths in the first round
- repeatedly passing `workdir` after `session_id` already exists
- exporting large text evidence before narrowing candidates
- constructing fake handles instead of reacquiring them from MCP results
- requesting `manifest include_text=true` before structured manifest data proves it is necessary

When the current branch becomes expensive too early, prefer this reset:

1. keep the current `session_id`
2. go back to the strongest clue
3. rerun the smallest useful `find_*` call with `brief=true`
4. add `fields` only when the next step truly needs explicit projection
5. inspect one likely object first
6. export only if evidence text is still necessary

During that reset:

- on the first recovery search, prefer `brief=true` without explicit `fields`
- use dotted class-name fragments for `class_name_contains`
- do not treat `descriptor_contains` as a substitute for a concrete method descriptor
- do not use `descriptor_contains` as the sole selector for `find_methods`
- reacquire exact descriptors from MCP results before export; do not hand-write them from decompiled text

## Useful MCP Surface

The core dexclub MCP tools for this skill are:

- `open_target_session`
- `list_target_sessions`
- `get_target_session`
- `close_target_session`
- `refresh_target_session`
- `diagnose_target_sessions`
- `manifest`
- `list_res`
- `find_resource_values`
- `get_resource_value`
- `decode_xml`
- `find_classes_using_strings`
- `find_methods`
- `find_methods_using_strings`
- `inspect_method`
- `export_class_java`
- `export_class_smali`
- `export_method_java`
- `export_method_smali`

Use `diagnose_target_sessions` when the current session state feels unclear or potentially stale.
