# Continuity

## 2026-08-28 CI publication
- Inspected remote main at `2a96eeca7096ede26c503e78ad881297657b9dd2` from a clean clone.
- GitHub Actions is enabled, but API inspection returned zero workflows and runs before publication.
- Remote CI already enforces lint failures; the local source folder differs and suppresses lint errors.
- Added a manual `workflow_dispatch` trigger while preserving remote action versions and blocking lint.
- Unrelated local app changes and `.env` are excluded from this commit.
- No application architecture changed; repository map regeneration is not applicable.
- Verification: checked the focused workflow diff and preserved existing push and PR triggers. Build results must be checked in GitHub Actions after publication.
