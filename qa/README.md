# Emulator QA only

`fixture/Player.java` plays a bundled locally generated MP4 using VideoView; `Controller.java` returns actual playback state/position and provides an emulator-only accessibility fault injector. These classes and their permissions are **not** part of the product APK. Sources and synthetic clip are inherited from the GenTV 3.7.12 emulator validation tools under MIT.

Set `JAVA_HOME` (JDK17) and `ANDROID_HOME`, then run `python3 qa/build-fixtures.py`. Install managed/unmanaged fixtures only in a dedicated disposable Android TV emulator. Build `assembleDebugAndroidTest`. `GuardIntegration` performs UI setup, user-click local ADB with the observed emulator port 5555, a real 10-minute timer and actual VideoView resume; `ChildPinIntegration` checks PIN, multi-selection, previews and remote focus; `GuardPersistenceIntegration` uses clearly synthetic initial state then actual UI wrong answers across three fresh processes (`pending`, `resume`, `rest`). Never run these destructive test-state flows on a user device.

Instrumentation termination kills the target process and can mark its bound accessibility service crashed. This is a test harness effect: explicitly revoke/re-enable the isolated service between suites. The product itself has no such test controls.

`GuardServiceIntegration` additionally uses the instrumentation shell identity (not the product UID) to enable/revoke its own isolated service and checks real multi-app foreground accumulation and UI connection labels. Run suites sequentially, wait for the instrumentation result before starting another suite, and preserve separate logs. `GuardPersistenceIntegration`'s final `rest` phase can also be run after rebooting the dedicated emulator.
