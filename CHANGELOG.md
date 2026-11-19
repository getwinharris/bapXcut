# Changelog

## Unreleased
### Changed
- Allow CI to be started manually from GitHub Actions, retaining push and pull request triggers and blocking lint checks.
- Document project engineering rules and the scope of this CI change.

### bapXcut branding and workflow
- Rebrand app names, translations, package namespace, export paths, metadata and release artifacts for bapXcut/getwinharris.
- Set the project site to https://bapx.in and use its documented bapX app palette.
- Android application ID is now `com.getwinharris.bapxcut`: installs separately from upstream and older local builds; private data is not automatically migrated.
- Preserve upstream license/history while removing misleading fork-specific service links.
- Add deterministic repository mapping, CI freshness checks, Robolectric onboarding regressions, agent discovery aliases, accurate architecture guidance and explicit verification/publication rules.

### Development workflow
- Document issue-linked branches, review-gated pull requests, benchmark evidence and release/APK provenance.
- Add a PR template covering verification, compatibility, risks and remaining work.
