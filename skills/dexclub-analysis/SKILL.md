---
name: dexclub-analysis
description: Use when Codex needs to analyze APK, Dex, manifest, resources, classes, or methods through dexclub MCP, especially for black-box Android reverse-engineering, feature location, implementation tracing, and competitor analysis. Requires `mcp__dexclub__`; if dexclub MCP is unavailable, do not use this skill.
---

# DexClub Analysis

## Overview

Use this skill to drive `mcp__dexclub__` as the primary analysis surface for APK/Dex/manifest/resource inspection. Treat dexclub MCP as mandatory: if it is unavailable, stop early and tell the user the skill cannot proceed until dexclub MCP is connected.

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
3. use `brief + fields` to shrink candidates
4. use `inspect_method` to read one-layer facts
5. use `export_*` only when evidence text is actually needed
6. summarize conclusion, evidence, and remaining uncertainty

Keep the workflow recoverable and iterative. Do not implement it as a rigid state machine.

## Session Rules

Always prefer `session-first`.

- open a target session unless the task is clearly a one-shot light query
- after obtaining `session_id`, keep using it
- do not keep redundantly passing `workdir` once `session_id` exists
- prefer `method_handle` and `class_handle` after they are returned by dexclub

If `session_id not found`:

- reopen the target session
- return to the latest useful step and continue

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

Only continue using previous handles when the session is confirmed to still exist. Otherwise, rebuild the session on the MCP path instead of drifting into `workdir` fallback for deep analysis.

## Entry Strategy

In black-box analysis, use this default priority:

1. `find_methods_using_strings`
2. `find_classes_using_strings`
3. `manifest`
4. `find_resource_values` / `get_resource_value` / `list_res`
5. `find_methods`

Do not start with a broad `find_methods` query when stronger anchors already exist.

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
- use the smallest useful `fields`
- do not send irrelevant `include`, `fields`, or `include_text`
- once a handle exists, do not keep repeating full descriptor and source constraints unless disambiguation is needed

The goal is not merely token savings; it is to reduce drift and keep the analysis path controlled.

## Manifest Rules

Default to structured manifest inspection.

- do not set `include_text=true` by default
- only request manifest raw XML when direct XML evidence is needed
- if only certain sections matter, constrain `include`

## Inspect and Export Rules

Default order:

1. locate
2. inspect
3. export

`export_*` is a heavy step. Do not use it as the default first move.

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

Recovery order:

1. determine whether this is context loss or parameter error
2. rebuild session or reacquire handles for context loss
3. narrow `include` / `fields` / parameters for parameter errors
4. retry on the MCP path

Do not immediately fall back to shell or source-code reading because of these errors.

## Output Discipline

When answering the user, distinguish:

- clues
- facts
- evidence
- conclusions

Do not promote a single hit directly into a final conclusion.

Prefer ending each analysis round with:

1. current conclusion
2. key supporting evidence
3. what remains uncertain

## Useful MCP Surface

The core dexclub MCP tools for this skill are:

- `open_target_session`
- `list_target_sessions`
- `get_target_session`
- `close_target_session`
- `diagnose_target_sessions`
- `manifest`
- `list_res`
- `find_resource_values`
- `get_resource_value`
- `find_classes_using_strings`
- `find_methods`
- `find_methods_using_strings`
- `inspect_method`
- `export_class_java`
- `export_class_smali`
- `export_method_java`
- `export_method_smali`

Use `diagnose_target_sessions` when the current session state feels unclear or potentially stale.

## References

For the underlying design constraints and rationale, read these only when needed:

- `../../.docs/v3/42-ai-capability-surface.md`
- `../../.docs/v3/43-mcp-tool-surface.md`
- `../../.docs/v3/44-skill-workflow.md`
- `references/workflow-notes.md`
