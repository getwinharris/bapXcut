# Git workflow and version provenance

## Work queue

- #2: verify the local needle/model artifact and safely integrate AI only after provenance, integrity and device validation.
- #3: compare original and modified APKs on one physical Android device with identical media and settings.
- #4: adopt this workflow and PR evidence requirements.
- #5: optimize only bottlenecks measured in #3; do not start speculative optimization first.

## Issue to reviewed change

1. Create or select an issue with acceptance criteria, dependencies and verification requirements.
2. Confirm a real Git checkout, correct remote, clean/understood status and current main. Preserve unrelated local work; the separate experimental source folder is not automatically authoritative.
3. Create a branch from current main, such as `docs/4-git-workflow`, `test/3-apk-benchmark` or `perf/5-preview-jank`. Scope each branch to its issue.
4. Make focused commits using `type: description` (for example `test: add reproducible preview benchmark`). Stage explicit reviewed paths; never commit secrets, SDK paths, caches or private media.
5. Run required checks, review staged changes and push the branch. Open a PR linking the issue and attach evidence. Use `Closes #N` only when all acceptance criteria are satisfied; otherwise use `Refs #N`.
6. Wait for CI, review and merge authorization. Do not push feature changes directly to main, auto-merge your own PR, or represent a queued run as passed. Do not rewrite shared branch history; use a follow-up commit unless coordinated otherwise.
7. After merge, verify the merged SHA and CI outcome. Retain benchmark baseline SHAs, fixture hashes and result artifacts. Clean up branches only after checking ownership and other worktrees.

Required checks: `python3 -m unittest discover -s tests`, `python3 generate_repo_map_mmd.py --check`, `./gradlew assembleDebug lintDebug testDebugUnitTest`, and `git diff --check`. Regenerate the map before checking when architecture changes. CI success is not a physical-device benchmark or release certification.

## Repository enforcement

At the 2026-08-28 inspection, the classic main branch-protection endpoint reported "Branch not protected". This does not establish whether organization/repository rulesets impose other controls. Review rulesets and propose required PR reviews and the existing CI check before enabling enforcement. Do not silently change repository-wide access controls. This PR documents the workflow; it does not impose protection settings.

## Version control versus release versions

Every candidate is identified by a full Git commit SHA and APK SHA-256. Also record application ID, ABI, build type, Gradle/JDK/SDK versions and build flags. For benchmarks, use the same toolchain/build type/settings on both commits. Do not compare a release original with a debug candidate or infer speed from CI build duration.

`versionName` is the human-facing release version; `versionCode` is the Android upgrade ordering value. Do not bump either for every development commit. For a deliberate release, inspect all published codes and reserve a strictly higher non-overlapping range: the current ABI scheme adds offsets 0, 1 and 2 to the base versionCode. Never reset or reuse codes. Keep that scheme stable unless a separate migration is reviewed.

Release tags use `v<versionName>`; the existing release workflow checks the match. Create tags/releases only with explicit authorization and after signing/device checks. Record source commit, APK hashes, supported ABIs and compatibility in release notes. The new application ID installs separately and does not migrate prior private app data automatically.

## Benchmark protocol

Follow #3 before #5. Record at least ten measured runs per scenario with warmup and thermal controls; separate cold and warm startup. Alternate APK order and use identical media hashes/edit recipes/export settings. Collect startup, preview frame-time/jank, idle/peak memory, export time, output size and quality/correctness evidence. Preserve raw results and failures, report distribution/variance, and do not claim improvement without a repeatable difference and intact quality guardrails.

No physical device benchmark has been run yet. ADB and Docker were not found on PATH during this task; device access and reproducible tooling remain prerequisites, not completed work.
