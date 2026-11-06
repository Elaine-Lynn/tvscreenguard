# Tencent physical 30-second test (2026-11-06)

Device: DBB3 Pro / Android9. App: TV Viewing Guard 1.0.6-qa30. Player: com.ktcp.video DetailCoverActivity. The actual player progress UI showed an episode at 00:23:47; this was not a home-screen-only acceptance.

A fresh round after restoring playback reported interval_ms=30000. Polling at approximately 0.5-second intervals first detected stage=1 at 30.361 seconds. The visible automatic question was 369 x (1 + 30%) = ?. Tencent PID21928 had one active audio track before the question; during the question it had zero tracks and the output thread was in standby. The verified native answer button 4797/10 was tapped. Stage returned to zero, Tencent DetailCoverActivity returned to the foreground and its audio track became active again. The next round accumulated normally.

The video layer itself is black in screenrecord captures. These frames do not establish that the physical TV is black, nor does an active audio track prove audible sound. The user subsequently confirmed normal picture and sound before the question, pause and silence while the question was shown, and restoration of both picture and sound after the correct answer. This observed physical-device round therefore passes automatic 30-second question, interruption and playback recovery acceptance. This does not resolve the separate preliminary system force-stop event or establish compatibility with other players/firmware.

At 12:22:17 during preparation, the system force-stopped Tencent, the guard app and several other packages and removed the guard accessibility service. The user reported no manual remote operation or cleaning. Cause is undetermined; do not attribute it to Tencent. That preparation round is excluded. Recovery reopened the app and restored only its previously enabled accessibility component while preserving current other entries; no broad WRITE_SECURE_SETTINGS permission was granted to the product app.

Raw timelines, captures, audio diagnostics and recovery state are in private work/tencent-playback/. Parent PIN and key material were not exported. Disable ADB debugging after device testing.
