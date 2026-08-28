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
- Prior CI run 33165126506 passed assemble, lint and unit test steps. Rebrand CI 33165620835 passed for source commit 8bd9b6a.
- Local map generator checks passed; Robolectric onboarding identity/theme coverage added. Device screenshots, export CUJs and signed release validation remain outstanding.
- No local container runtime is available. Use hosted CI for Android verification. A pinned repository build container remains future work.
- Canonical rules renamed from Agents.md to AGENTS.md; case-only duplicate files cannot coexist reliably on macOS. Gemini/Claude pointers use AGENTS.md.

### Verified outcome
- Published rebrand/map/workflow in `87f983c`, locale regression protection in `8bd9b6a`.
- https://github.com/getwinharris/bapXcut/actions/runs/33165620835 completed successfully: five Python map/branding checks, assembleDebug, lintDebug and testDebugUnitTest.
- Downloaded unit test artifact: BrandingTest ran two tests and ExampleUnitTest ran one; zero failures, errors or skips. APK, lint and unit test reports were uploaded.
- Verified GitHub homepage is https://bapx.in and repository description identifies bapXcut.
- Remaining old text: upstream attribution and historical changelog. `src/images/featureGraphic.png` visibly retains upstream name/pink UI; historical screenshots and artwork need replacement before release promotion.
- Remaining workflow improvements: pinned Android build container; reviewed Roborazzi baselines and device import/edit/export CUJs; reconcile the separate non-Git local experimental source folder before publishing its AI work.
- This outcome update changes documentation only; the application and tests are identical to the passing CI commit. No signed release was created.

## 2026-08-28 Needle2 fine-tuning research
- Refs #2. Added docs/ai/NEEDLE2_FINETUNING.md, five proposed tool schemas and seven seed format examples on docs/2-needle2-finetuning. No training, engine execution or Android integration was performed.
- Verified app minSdk 26/target 34, local ARM64 binary, package cactus-needle 2.0.10 and its engine 2.0.3 setting, HF revision 98fbd955b0347e78059be0c253cc1ffa09b87bc7, Android file hashes and needle_load C API.
- Train LoRA on checkpoint, export tuned .cact, load before init via native bridge; check format/engine compatibility on device. Base weights are embedded; tuned override is an additional file.
- Existing writable filesDir execution conflicts with Android 10+ target-29+ restrictions. Recommend APK-packaged JNI integration; API-26 compatibility and supported ABIs still require verification.
- Tuned confidence is not calibrated. Documented tool/argument evaluation and safety validation are required, not just training loss.
- Validated JSON structure/tool names/required arguments for seed examples. They are not a sufficient dataset; no accuracy claims.
