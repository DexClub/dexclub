# DexClub Analysis Workflow Notes

Read this file only when the task needs extra detail beyond `SKILL.md`.

## Focus

This reference exists to remind the agent of the most failure-prone behaviors seen in real usage:

- expanding multiple heavy entry paths in the first round
- repeatedly passing `workdir` after `session_id` already exists
- exporting large text evidence before narrowing candidates
- constructing fake handles instead of reacquiring them from real MCP results
- requesting `manifest include_text=true` before structured manifest data proves it is necessary

## Practical Reminder

When the current path becomes expensive too early, prefer this reset:

1. keep the current `session_id`
2. go back to the strongest clue
3. rerun the smallest `find_*` call with `brief + fields`
4. inspect one likely method
5. export only if that method still needs evidence text
