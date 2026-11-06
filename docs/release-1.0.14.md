# TV ScreenGuard 1.0.14 Release

- English / GitHub display name: TV ScreenGuard; Chinese: 电视防沉迷系统.
- Repository: https://github.com/Elaine-Lynn/tvscreenguard.
- Application ID / Java namespace: `com.tv.screenguard`; versionCode 15.
- GenTV production certificate SHA-256: `8d5b5378f9056880f3036d44bc2f4a48322141fbb8d8bc91951d61a4652d997f`.
- APK SHA-256: `f30e62b447d93b29f268bb11017a2187496f77c547cd441601099992c942a250`; size: 602521 bytes.
- External signing configuration is read in place. No key/password/config copied into this repository.
- R8 minification and resource shrinking enabled; release-only discard removes the QA 30-second string. Release BuildConfig.TIMER_TEST_OPTION=false.

## Validation

31 Release JVM tests, lintRelease, assembleRelease, apksigner verification and standalone project checks passed. APK badging confirms the new identity, localized names and API26 minimum. The release resource table contains no child_lock_test_30_seconds entry.

A dedicated Android TV 16 emulator ran 355 UI checks on the normal Debug build (no QA30 option) using the renamed package and instrumentation. README settings/question screenshots come from that run. The actual signed Release was then installed fresh on the same isolated emulator: install returned Success; launch returned Status: ok; the English parent PIN screen and approved icon were visually checked. This is a startup smoke check, not a full Release playback timer acceptance run.

The physical B3P installation was not changed. Existing Tencent playback/Home results were recorded on the old QA package and must not be represented as acceptance of this new release identity. New identity requires a fresh PIN, managed-app choices and accessibility approval; disable the old app's protection before using the new one. No user data was erased or migrated.

README and promotional graphics approved by the owner. Publication targets Elaine-Lynn/tvscreenguard, tag v1.0.14. See publication-security-audit.md for the public-content review. Distribution ZIP includes APK, README, privacy statement and inherited licences.
