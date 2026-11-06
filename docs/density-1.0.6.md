# 1.0.6 physical-box density correction

The DBB3 Pro reports 1920x1080 at 240dpi, unlike the previous 320dpi emulator. A direct screenrecord frame and actual view hierarchy confirmed large empty lower panel areas caused by fixed dp/sp content inside full-height panels.

Settings now scale dp and sp together using screenHeightDp / 540 (clamped to 1.0-1.4). This changes only application layout, not the system display density. Equal panel bounds, the upper-right teacher, fixed bottom action and managed-app settings are preserved.

30 JVM tests, lintDebug and QA/normal builds passed. Actual 1.0.6-qa30 installation returned Success; opening returned Status: ok. The user entered their PIN locally. A new 1920x1080 screenrecord frame was visually inspected on the physical box and confirms larger controls/text and substantially reduced blank lower areas, with no clipped bottom guidance. This is physical-device evidence, not an emulator screenshot. The system density remains unchanged.

The physical capture is output/validation/density-1.0.6/box-settings-real.png. Private before/after view hierarchies and captures are in work/box-qa30. No actual-video recovery claim is added by this layout check. Disable ADB debugging after device testing.
