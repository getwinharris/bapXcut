# bapXcut

A local-first Android video editor from the **bapX** brand, maintained by **getwinharris**.

[Project site](https://bapx.in) · [Source](https://github.com/getwinharris/bapXcut) · [Issues](https://github.com/getwinharris/bapXcut/issues) · [Releases](https://github.com/getwinharris/bapXcut/releases) · [CI](https://github.com/getwinharris/bapXcut/actions/workflows/ci.yml)

## Editing

Trim and arrange clips, adjust speed, add text, images, audio and subtitles, and export videos locally without a watermark. The app remains in beta; verify important edits and exports on your device.

## Build and verify

Use JDK 17 and Android SDK 34. The Gradle wrapper pins the build tool version.

```sh
git clone https://github.com/getwinharris/bapXcut.git
cd bapXcut
python3 -m unittest discover -s tests
python3 generate_repo_map_mmd.py --check
./gradlew assembleDebug lintDebug testDebugUnitTest
```

CI runs these checks on pushes, pull requests and manual dispatch. Debug APKs are CI artifacts; signed release availability depends on the release workflow and configured signing secrets. There is no claimed F-Droid listing, Weblate project or Discord server for this fork.

## Identity and compatibility

Android package and application ID: `com.getwinharris.bapxcut`. This is a **separate installation**, not an in-place update of the upstream app or earlier locally branded builds. Existing private app data is not migrated automatically. Keep the original app/data and export projects before switching. New exports use bapXcut folders and names; existing media is not moved or deleted.

## Architecture and agent workflow

The committed app uses Kotlin, XML layouts, ViewBinding and ViewModels, not Jetpack Compose. See [map.mmd](map.mmd) for a generated file-reference overview; it is not a complete runtime call graph. The local experimental AI bridge is not part of this published rebrand.

Read [AGENTS.md](AGENTS.md) and [.agent/CONTINUITY.md](.agent/CONTINUITY.md) before editing. Agent changes must be scoped, tested and accompanied by explicit publication and verification status.

## Brand

Project site: https://bapx.in. App palette: OLED black `#000000`, slate blue `#6463D7`, purple `#875CE9`, and slate gray `#545A67`. Existing upstream screenshots and artwork are historical assets and are not presented here as current bapXcut screenshots. Device screenshots and export CUJs still need review before a release.

## Attribution

bapXcut is derived from [LibreCuts by Tharun Birla](https://github.com/tharunbirla/LibreCuts). Original copyright and the MIT license are preserved in [LICENSE](LICENSE). Historical upstream changelog entries remain unchanged. Fork branding does not imply ownership of the original work.
