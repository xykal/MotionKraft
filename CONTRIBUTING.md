# Contributing

By contributing you agree that your changes are submitted under the project's license.

## Pull Requests
1. Fork the repository
2. Work on a feature branch
3. Ensure `./gradlew assembleDebug assembleRelease` succeeds locally
4. Open a PR describing the change

PRs from forks can only build debug artifacts. Release signing requires maintainer approval and runs only on the main repository with environment protection.

## Code Style
- Kotlin official style
- No emoji in UI strings or code comments
- All user-facing strings go through `values/strings.xml`
- New features must not break existing export or timeline data formats without a migration
