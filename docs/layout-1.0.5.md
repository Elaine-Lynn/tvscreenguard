# 1.0.5 aligned settings panels

Both settings panels now use framed ScrollView viewports with exactly matching top/bottom bounds. The left groups share one frame, with spacing reduced enough to show the Chinese service hint. The teacher artwork is placed in an independent upper-right header column, so neither Chinese nor English headings can overlap it. The fixed global bottom action and remote navigation remain unchanged.

30 JVM tests, lintDebug and builds passed. Chinese and English UI suites each passed 355 checks, including exact vertical panel bounds and upper-right artwork geometry. Screenshots and raw results are in output/validation/equal-1.0.5. Normal builds hide the test interval; the QA30 package retains it.

The 1.0.5-qa30 / code6 update preserves the box's PIN and managed applications. Existing GenTV and Home remain unchanged. Actual video pause/resume acceptance remains outstanding from the earlier test. Disable ADB debugging when physical-device testing is finished.
