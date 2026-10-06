# Needle2 fine-tuning for bapXcut

Research checked 2026-08-28. Refs #2. This is a proposed training/tool contract, not a trained model or implemented Android bridge.

## Exact app and upstream versions

- Published app: `com.getwinharris.bapxcut`, versionName `1.0-beta7`, base versionCode 10, minSdk 26, target/compile SDK 34. ABI outputs: arm64-v8a, armeabi-v7a, x86_64.
- Local AI experiment: same min/target SDK, but app ID `com.aistudio.bapxcut.xyzabc`, universal APK setting and an ARM64 needle executable. Its exact release is unverified; do not equate filename/size to a release pin.
- Verified PyPI package: `cactus-needle==2.0.10`. Its inspected wheel uses engine version `2.0.3`. GitHub main's pyproject still says 2.0.8, so do not silently combine main metadata and released-package versions.
- Model/native files: `Cactus-Compute/needle2`, revision `98fbd955b0347e78059be0c253cc1ffa09b87bc7`. Android ARM64 runner: 14,820,024 bytes, SHA-256 `b6e10164b83ac7e1543684ccad046548a18b8e2e53837a5f6bf7bbc619dfbedb`. Static archive SHA-256 `a1333d28c57a35d33b4a0fb688706573dac56d851dea467771480f745f59f9f6`.
- Published Android folders are arm64, armv7 and riscv64, not Android x86_64. A Linux x86_64 binary is not an Android substitute. Keep AI unavailable on unsupported ABIs until a matching build is verified.
- A device's actual OS/ABI has not been inspected. Compatibility down to API 26, linking requirements and newer page-size requirements still need build/device validation.

## Training contract for existing editing code

Start with the five proposed schemas in `tools.example.json`:

| Tool | Existing implementation target |
| --- | --- |
| trim_main(start_ms, end_ms) | updateMainVideoTrim -> ReplaceUniqueOperationCommand(Trim) |
| set_speed(multiplier) | updateMainVideoSpeed; preserve proxy-generation semantics |
| mute_main() | addMuteAudioOperation |
| add_text(text) | addTextOperation; app supplies current styling, not model guesses |
| undo() | VideoEditingViewModel.undo |

These adapters are not implemented. Validate loaded project, timeline bounds, start < end, allowed speed, text size and undo availability in Kotlin. Model output never supplies raw shell/FFmpeg commands, executable classes, private paths or guessed media IDs. Preview proposed edits and preserve undo history.

`seed.example.jsonl` contains seven format examples only; it is not a sufficient training dataset. Build a reviewed corpus with different phrasing, numbers, tool confusions, unsupported requests and refusals. Each line includes query, full tools, expected answers and optional grounding explanation. Use an independently held-out test set and keep near-duplicate phrasings out of both splits. Measure exact tool/argument accuracy and unsafe-action/refusal rates before and after tuning. Check rendered lengths against the training cap; do not accept silent truncation. [Official training guidance](https://github.com/cactus-compute/needle/blob/main/doc/finetuning.md)

## Reproducible preparation

Train on a computer, not by modifying the Android executable. Prefer a pinned container; no container runtime is configured here. The following are preparation commands to run in an isolated approved Python environment, not commands already executed:

```sh
python -m pip install 'cactus-needle==2.0.10'
```

Optional vendor extras are `cactus-needle[metal]==2.0.10` for Apple GPU or `cactus-needle[gpu]==2.0.10` for CUDA. Record Python, full resolved dependency lock, hardware and seeds. The package's declared version and actual wheel were inspected; no training environment has been installed. [Official package](https://pypi.org/project/cactus-needle/2.0.10/)

Fetch a fixed source snapshot instead of moving main:

```python
from huggingface_hub import snapshot_download
snapshot_download(
    repo_id="Cactus-Compute/needle2",
    revision="98fbd955b0347e78059be0c253cc1ffa09b87bc7",
    allow_patterns=["checkpoints/needle2.pkl", "android-arm64/*", "LICENSE", "config.json"],
    local_dir="vendor/needle2",
)
```

Run this outside the repository, or explicitly exclude vendor/model outputs; do not commit binary downloads or private training data. Verify file hashes. Preserve the upstream license. Downloading a pickled checkpoint requires trusting its publisher; never substitute an untrusted checkpoint.

## Train, export, evaluate

After the reviewed dataset exists at `data/train.jsonl`:

```sh
needle finetune data/train.jsonl \
  --checkpoint vendor/needle2/checkpoints/needle2.pkl \
  --epochs 10 --batch-size 16 --lr 0.0001 \
  --lora-rank 16 --lora-alpha 32 --max-len 1024 --val-split 0.1 \
  --out bapxcut-adapter.pkl
needle build vendor/needle2/checkpoints/needle2.pkl \
  --lora bapxcut-adapter.pkl --bits 2 --out bapxcut-needle2.cact
```

These are a starting configuration, not an optimized recipe. Tune against validation and the independent test set. The base stays frozen during LoRA training; export merges the adapter into a `.cact`. Do not fine-tune the stripped executable. Optional synthetic-data generation contacts a cloud provider and needs credentials; do not enable it or upload prompts without explicit approval. [Official training/export commands](https://github.com/cactus-compute/needle/blob/main/doc/finetuning.md)

Evaluate on the host using the same reviewed schema:

```python
import json
import needle
with open("tools.json") as f:
    tools = json.load(f)
agent = needle.Needle(tools=tools, weights="bapxcut-needle2.cact")
print(agent.complete("Keep the main video from 5 seconds to 12 seconds"))
```

Copy the finalized schema to tools.json first. Run base and tuned evaluations in separate processes: loaded tuned weights are process-global and cannot simply be unloaded. Tuned confidence is not calibrated; the Python wrapper returns None. Native integration must also avoid treating an available numeric confidence as a validated safety gate. [Official API](https://github.com/cactus-compute/needle/blob/main/doc/apis.md)

## Android loading path to implement and verify

Link the pinned Android `libneedle.a` through a JNI shared library packaged in the APK. The upstream `android-arm64/needle.h` exposes `needle_load`, `needle_init`, `needle_complete`, and `needle_reset`. The load sequence below follows the official Python wrapper; it is not a finished JNI implementation:

1. Read tuned `.cact` into native-owned bytes and keep them alive for the engine session.
2. Call `needle_load(bytes, length)`; fail on nonzero result.
3. Call `needle_init(systemFacts, toolsJson, toolIndexPath)`; fail on a negative result.
4. Call `needle_complete(input, maxTokens, outputBuffer, capacity)` off the UI thread, serialize engine access and parse type/function_calls/name/arguments.
5. Validate and preview commands in Kotlin before executing through the existing undo-aware command path.

[Official Android header](https://huggingface.co/Cactus-Compute/needle2/blob/98fbd955b0347e78059be0c253cc1ffa09b87bc7/android-arm64/needle.h) and [official wrapper](https://github.com/cactus-compute/needle/blob/main/needle/__init__.py).

The base distribution embeds weights; the fine-tuned deployment adds an explicit `.cact` weight override. Do not promise the resulting APK still costs only 14 MB: the packaged engine and override may both contribute. Validate archive/engine compatibility by loading and running the actual Android build; format compatibility is version-dependent.

The current local filesDir + setExecutable + ProcessBuilder pattern must be replaced for modern target SDKs: Android 10 restricts execve from writable app home directories for apps targeting API 29+. Package native code through the APK rather than attempting to bypass that restriction. [Android documentation](https://developer.android.com/about/versions/10/behavior-changes-10#execute-permission)

## Completion gates

No trained bapXcut model, native JNI bridge, physical-device inference result or latency/memory claim exists yet. Require format validation, held-out accuracy, corrupt/missing model handling, repeated prompts, cancellation/lifecycle safety, supported ABI/API tests, and the #3 same-device benchmark before shipping. Fine-tuning cannot repair the existing parser/protocol/packaging mismatch by itself.
