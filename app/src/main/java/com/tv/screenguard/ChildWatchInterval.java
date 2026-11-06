package com.tv.screenguard;

/** Public viewing intervals; retired short-test preferences migrate to the default. */
final class ChildWatchInterval {
    static long millis(int stored, boolean testSelected, boolean testBuild) {
        return testBuild && testSelected ? 30_000L : minutes(stored, false) * 60_000L;
    }
    static long millis(android.content.SharedPreferences state) {
        return millis(state.getInt(ChildSettingsActivity.MINUTES, 10),
                state.getBoolean(ChildSettingsActivity.TEST_INTERVAL, false), BuildConfig.TIMER_TEST_OPTION);
    }
    static int minutes(int stored, boolean retiredTestSelected) {
        if (retiredTestSelected) return 10;
        switch (stored) {
            case 5: case 10: case 15: case 20: return stored;
            default: return 10;
        }
    }
}
