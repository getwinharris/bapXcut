# Continuity

## 2026-08-28 CI publication
- Inspected remote main at `2a96eeca7096ede26c503e78ad881297657b9dd2` from a clean clone.
- GitHub Actions is enabled, but API inspection returned zero workflows and runs before publication.
- Remote CI already enforces lint failures; the local source folder differs and suppresses lint errors.
- Added a manual `workflow_dispatch` trigger while preserving remote action versions and blocking lint.
- Unrelated local app changes and `.env` are excluded from this commit.
- No application architecture changed; repository map regeneration is not applicable.
- Verification: checked the focused workflow diff and preserved existing push and PR triggers. Build results must be checked in GitHub Actions after publication.

## 2026-08-28 Branding and workflow publication
- Scope: remote bapXcut branding, package identity, map generation and agent workflow. Unrelated local experimental AI changes and `.env` are excluded.
- Renamed namespace/application ID to `com.getwinharris.bapxcut`, packages, application class, resource keys, app names/translations, export names and CI release artifacts. This is a separate install; no automatic private-data migration is promised.
- Project links target getwinharris/bapXcut and https://bapx.in. Removed unsupported fork-specific store/community/funding claims. Original license and historical attribution remain.
- Applied black/slate-blue/purple palette and replaced inherited hardcoded pink selection accents.
- Corrected actual architecture to XML/ViewBinding, added discoverable AGENTS.md, fail-fast verification guidance, secret ignore rules, deterministic map checking and test-report artifacts.
- Prior CI run 33165126506 passed assemble, lint and unit test steps. New rebrand CI still needs verification after publication.
- Local map generator checks passed; Robolectric onboarding identity/theme coverage added. Device screenshots, export CUJs and signed release validation remain outstanding.
- No local container runtime is available. Use hosted CI for Android verification. A pinned repository build container remains future work.
- Canonical rules renamed from Agents.md to AGENTS.md; case-only duplicate files cannot coexist reliably on macOS. Gemini/Claude pointers use AGENTS.md.
