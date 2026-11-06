package com.tv.screenguard;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/** Hosts the challenge over the current app. Home also restores the same saved round on launch. */
public final class ChildChallengeActivity extends Activity {
    private static volatile ChildChallengeActivity resumed;
    private ChildLock lock;

    static boolean isResumed() { return resumed != null; }

    private String resumePlayer = "";

    void resumePlayerAfterFinish(String pkg) { resumePlayer = pkg; }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        lock = new ChildLock(this);
    }

    @Override protected void onResume() {
        super.onResume();
        resumed = this;
        if (BuildConfig.TIMER_TEST_OPTION) android.util.Log.i("TVGuardLifecycle", "resume");
        restorePendingChallenge();
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        // A singleTask relaunch may reuse a dismissed dialog host. onResume handles
        // a stopped instance; an already resumed host restores the same saved round.
        if (resumed == this) restorePendingChallenge();
    }

    private void restorePendingChallenge() {
        if (!lock.showIfLocked()) finish();
    }

    @Override protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        // Home/Recents may close the host UI, never the saved question or rest.
        // Finishing also avoids B3P restoring a protected outgoing task over its
        // Home trampoline. Managed re-entry creates a fresh host for the same round.
        if (ChildLock.locked(this)) finish();
    }

    @Override protected void onPause() {
        if (BuildConfig.TIMER_TEST_OPTION) android.util.Log.i("TVGuardLifecycle", "pause");
        if (resumed == this) resumed = null;
        lock.dismiss();
        super.onPause();
    }

    @Override protected void onDestroy() {
        if (resumed == this) resumed = null;
        super.onDestroy();
        if (!resumePlayer.isEmpty()) ChildWatchService.challengeFinished(resumePlayer);
    }

    @Override public void onBackPressed() {
        if (!ChildLock.locked(this)) super.onBackPressed();
    }
}
