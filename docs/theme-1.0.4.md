# 1.0.4 settings theme alignment

Settings and parent PIN screens now share the question screen's dark teal surfaces, translucent panels, light text and pale green focus outlines. The preview panel uses the same teacher artwork. Flat rounded buttons replace raised white cards. The fixed global action and remote Up/Down navigation remain in place. App selection and PIN fields also use dark surfaces to preserve contrast.

30 JVM tests, lintDebug and builds passed. English and Chinese UI suites each passed 353 checks. Evidence is in output/validation/dark-1.0.4. A normal build hides the 30-second option; the explicitly enabled QA30 build retains it.

DBB3 Pro installation of 1.0.4-qa30 / code5 returned Success and launch returned Status: ok. PIN, four selected applications and the connected timer service were preserved, with interval_ms=30000. Earlier physical testing confirmed automatic questions over Tencent's home screen only. Actual video playback recovery remains unverified. Existing Home and GenTV were preserved; the dedicated emulator was deleted. Disable ADB debugging when device testing is finished.
