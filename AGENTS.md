## Communication Guidelines

Default to Chinese in user-facing responses for this repository unless the user explicitly asks for another language. Keep explanations direct, technical, and concise, and preserve code, commands, paths, and API names in their original form when needed.

- When the user raises a question, concern, proposal discussion, or rhetorical challenge, first analyze, clarify, and discuss instead of immediately modifying code. Start implementation only after the user explicitly asks to execute, fix, modify, organize, or confirms a proposed approach.

- When the user asks for code or logic inspection/cleanup/review intent, inspect the dirty worktree first. Only widen the scope to the whole project when the worktree is already clean or the user explicitly asks for a broader pass. During inspection/cleanup, avoid over-abstraction and avoid unnecessary cross-module entanglement or nested indirection; keep readability and maintainability as first-order constraints.

- When the user asks to inspect an interface chain, call chain, data flow, or other end-to-end logic, follow the complete chain before giving conclusions. Do not stop at a local module, partial page, or a single layer and then generalize from that incomplete view.

- Do not make extra changes just because they seem reasonable. Stay within the explicitly requested scope unless the user clearly asks for additional adjustments or confirms them after discussion.

## Commit \& Pull Request Guidelines

Do not create git commits proactively after making or verifying changes. Commit only when the user explicitly asks to commit or confirms that the current changes should be committed.

## Debugging Guidelines

When diagnosing unclear runtime behavior, add targeted temporary logs when useful for joint debugging instead of guessing from code alone. Keep those logs scoped, easy to remove, and avoid logging secrets or personally identifiable data.
