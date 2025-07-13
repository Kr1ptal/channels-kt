# Changelog

All notable changes to this project will be documented in this file.

## [1.0.0]

### Bug Fixes

- Close `forEachSuspend` on coroutine cancellation
- Close notifications channel in `forEachSuspend`

### Features

- Add initial implementation of `Channel`
- Make `Channel` accept only non-null elements and remove `Iterable` interface
- Implement `map`, `mapNotNull`, `filter` operators
- Implement `ChangeNotifier`
- Remove `iterator` functions from `Channel`
- Move code to `channels-core` submodule
- Add `channels-coroutine` submodule
- Update `README.md`
- Add tests (#6)
- Re-add `forEachAsync` functionality
- Replace `ChangeNotifier` with `BlockingStrategy`
- Add coroutine-friendly NotificationHandle integration (#21)
- Reduce usage of java imports (#26)
- Jreleaser library publishing (#27)
- Update readme

### Miscellaneous Tasks

- Add github actions
- Bump org.gradle.toolchains.foojay-resolver-convention
- Bump ktlint-tool from 1.3.1 to 1.5.0 (#3)
- Bump kotlin from 2.1.0 to 2.1.10 (#5)
- Bump junit from 5.11.4 to 5.12.0 (#7)
- Bump io.mockk:mockk-jvm from 1.13.16 to 1.13.17 (#8)
- Bump junit from 5.12.0 to 5.12.1 (#9)
- Bump kotlin from 2.1.10 to 2.1.20 (#10)
- Bump org.gradle.toolchains.foojay-resolver-convention from 0.9.0 to 0.10.0 (#11)
- Bump org.jetbrains.kotlinx:kotlinx-coroutines-core from 1.10.1 to 1.10.2 (#12)
- Bump junit from 5.12.1 to 5.12.2 (#13)
- Bump io.mockk:mockk-jvm from 1.13.17 to 1.14.2 (#15)
- Bump junit from 5.12.2 to 5.13.1 (#20)
- Bump ktlint-tool from 1.5.0 to 1.6.0 (#18)
- Bump kotlin from 2.1.20 to 2.1.21 (#16)
- Bump io.mockk:mockk-jvm from 1.14.2 to 1.14.4 (#22)
- Bump junit from 5.13.1 to 5.13.3 (#25)
- Bump kotlin from 2.1.21 to 2.2.0 (#24)

### Refactor

- Rename package `operation` -> `operator`
- Rename `tryPoll` -> `poll` to keep the naming consistent with `Queue` interface
- Remove delegating channel sender/receiver

### Misc

- Add `README.md`
- Clarify README.md
- Add license to ThreadHints.kt
- Remove `fromEachAsync` example from README.md
- Optimize gradle project setup with `buildSrc` and `libs.versions.toml`
- Format project with ktlint
- Update gradle to v8.12.1
