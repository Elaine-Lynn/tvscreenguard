# Home bypass fix and verification - 1.0.11

## Confirmed cause

On the user's DBB3 Pro / API28, pressing Home starts the original Dangbei Home and then its firmware force-stops multiple applications. Before the fix, this included com.tvguard.app and removed its accessibility service from enabled_accessibility_services. The pending question remained persisted but no process/service remained to intercept a reopened Tencent player. ActivityManager timestamps at 12:35:55.659 and 12:35:56.322 show Home immediately preceding the guard force-stop. This explains the previously undetermined multi-app force-stop observation; no user cleaning action was reported.

## Fix

1. A strictly gated DBB3 Pro/API28 adapter validates the known system provider com.dangbei.dbdownloadrefs and registers only getPackageName() in content://com.dangbei.dangbeiref.data/process_protect_list_info. Existing entries are retained. The app uses ordinary ContentResolver APIs, not broad WRITE_SECURE_SETTINGS, root, a notification listener, a background ADB daemon or desktop changes. It verifies its own entry before reporting background protection ready. App-created entries have ownership recorded and are removed when the parent turns the mode off; pre-existing entries are preserved.
2. Home closes only the transient quiz host; it never clears the saved QUESTION/REST, question snapshot, wrong count or rest deadline. This avoids B3P restoring its protected outgoing task over the Home trampoline.
3. Managed-app re-entry enforces the pending round without another viewing interval. Focused roots are refreshed and paused own-quiz roots rejected. Fresh window events are preferred briefly, then active roots are reconciled because Tencent may start its final HomeActivity after the initial startup activity and cover a question already launched.
4. A singleTask re-entry restores the existing pending UI; protection status distinguishes connected accessibility from unavailable firmware background protection.

## Read-only firmware evidence

DBSystemService.updateDBListForFlag(10005,...) maps to the process-protection URI. SystemUtilsImpl.updateList uses the appPackageName/appName/appVersion schema; the add path performs a ContentResolver insert and the delete path selects appPackageName=?. The original baseline was empty. A scoped trial entry prevented the real Home force-stop. After removing that trial entry, the installed app itself created and verified its own record via the normal application UID. Framework compact DEX was inspected using the device's oatdump after REA's Android engine could not decode it; no firmware was modified. Raw hashes, exported compact DEX, disassembly and provider baselines are private under work/home-bypass. APK static inventory was inspected with REA; static findings alone were not treated as runtime proof.

## Verification

- JDK17/SDK34: 31 JVM tests, lintDebug and QA/normal builds pass.
- Dedicated Android TV16 emulator: 60 checks pass, including two Home/re-entry cycles for QUESTION and two for REST, Home availability, unchanged question/answer order and rest deadline, continuously bound accessibility, required correct answer, actual VideoView resume and advancing progress. Synthetic initial pending state is explicitly separate from real timing evidence.
- Physical DBB3 Pro: original Home remains available. Three consecutive Home -> Tencent cycles preserve the same PID, service approval and exact pending snapshot. Re-entry is observed at 0.240 / 0.235 / 0.230 seconds and the challenge remains in front after a further two-second stability check. The earlier short-lived appearance that Tencent's delayed startup covered was rejected and fixed, not counted as acceptance.
- A correct answer unlocks normally, followed by a fresh real timed question at 30.321 seconds. The three Home cycles above were repeated on that fresh pending round.
- Installed 1.0.11-qa30 / code12 APK was pulled back and its SHA-256 exactly matches the delivered QA APK. PIN, four managed applications, original Home, GenTV and the historical device-admin state are preserved.
- The user also verified the physical remote Home -> desktop -> Tencent path and confirmed that the same pending question returns and cannot be bypassed. Power-cycle and ADB-off acceptance are not claimed by this test. Existing Tencent picture/sound acceptance from the earlier observed round remains separate.

Evidence snapshots: output/validation/home-fix-1.0.11. Private raw captures, device configuration and rollback command: work/home-bypass. The vendor-protection change is reversible by deleting only this app's entry; do not remove any other entries. Testing keeps the labelled 30-second Debug-only option; normal builds remain 5/10/15/20 minutes. Disable ADB debugging after device testing is finished.
