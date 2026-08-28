# bapXcut Engineering Context & Agent Rules

## Identity and scope
- Product: bapXcut, a local-first Android video editor from the bapX brand.
- Repository: https://github.com/getwinharris/bapXcut; project site: https://bapx.in.
- Android namespace and application ID: `com.getwinharris.bapxcut`.
- Be proactive and concise. Implement, verify and report evidence without overstating completion.

## Start every task
1. Read `.agent/CONTINUITY.md`, relative to the repository rather than filesystem root.
2. Establish the current ISO timestamp with `date -Iseconds` or platform equivalent.
3. State goal, acceptance criteria, constraints and required inspections. Clarify consequential ambiguity.
4. Verify cwd, remote, branch, worktrees and dirty files. A folder without `.git` is not a checkout. Never overwrite or publish unrelated local work to repair that condition.
5. Check recency using upstream vendor sources. Do not use context7.

## Actual architecture
- Kotlin, ViewModels, XML layouts and ViewBinding with Material Components. Compose is not configured in this checkout.
- Reuse existing command, ViewModel and service paths. Do not add parallel frameworks without need.
- Experimental local AI files are not in the published baseline. Do not describe a native engine or command bridge as shipped without inspecting and testing it.
- `map.mmd` is a deterministic lexical file-reference overview, not a complete runtime call graph.

## Branding and compatibility
- Palette: OLED black `#000000`, slate blue `#6463D7`, purple `#875CE9`, slate gray `#545A67`.
- Update names across translations, notifications, exports, release assets, links and store metadata together.
- Preserve upstream copyright, license and historical attribution. Do not replace author credits as branding.
- Application ID, preferences or storage changes require an explicit compatibility note. Never delete old media or silently claim data migration.
- Do not invent funding accounts, store listings, chat communities or translation services for the fork.

## Tooling and secrets
- Prefer containers for build tooling. No repository-specific container image is configured yet.
- If a container runtime is unavailable, state that limitation. Standard-library Python checks may run locally; use GitHub Actions for Android verification until a reproducible container exists.
- Never print or stage secrets. Exclude `.env`, credentials, keystores, local SDK paths, build products and caches. Stage explicit reviewed paths only.
- Use failure-propagating commands (`&&` or fail-fast scripts). Failed checks must block committing.

## Verification and delivery
- After architecture/package changes run `python3 generate_repo_map_mmd.py`.
- Required checks: `python3 -m unittest discover -s tests`, `python3 generate_repo_map_mmd.py --check`, `./gradlew assembleDebug lintDebug testDebugUnitTest`, and `git diff --check`.
- Keep lint failures blocking. Upload lint and test reports even on failure.
- Use Robolectric for view/resource regressions. Add Roborazzi screenshots for substantial visual changes and run import/edit/export CUJs on a device before release. Resource tests are not screenshot or export verification.
- Review staged diffs and secret exposure before committing. Verify remote SHA and CI result after pushing; queued/running is not passed.
- Record meaningful evidence and remaining work in `.agent/CONTINUITY.md`, and user-visible changes in `CHANGELOG.md`. Update these rules when workflow/architecture changes, not for routine edits.
- Distinguish local, committed, pushed, CI-passed and released states. Do not publish a release without authorization.

## Instruction discovery
`AGENTS.md` is canonical. It replaces `Agents.md` to avoid case-only filename collisions on macOS. `GEMINI.md` and `claude.md` point here; do not create a second case variant.

## Git-based delivery
Follow `docs/DEVELOPMENT.md`: issue -> scoped branch -> reviewed commits -> PR -> CI/review -> authorized merge. Do not push feature work directly to main. Link issues and retain source/APK provenance. Benchmark #3 before optimization #5; local AI provenance/integrity is tracked in #2. Release versions/tags require deliberate authorization and must not be bumped for routine commits.
