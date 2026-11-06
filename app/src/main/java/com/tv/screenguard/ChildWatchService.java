package com.tv.screenguard;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.Random;
import java.util.Set;
import java.util.Collections;

/** Times the configured foreground app while the screen is interactive. */
public final class ChildWatchService extends AccessibilityService {
    private static volatile ChildWatchService connected;
    static boolean isConnected() {
        return connected != null;
    }

    static final String WATCHED = "watched_ms";
    static final String PLAYBACK = "resume_player";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private SharedPreferences state;
    private PowerManager power;
    private String foreground = "";
    private long lastWindowEventAt;
    private Set<String> targets = Collections.emptySet();
    private ChildWatchClock clock;
    private long lastOpen;
    private ChildLock overlay;
    private android.media.AudioFocusRequest overlayFocus;
    private boolean overlayUnavailable;
    private String overlayFallbackReason = "none";


    @Override protected void onServiceConnected() {
        connected = this;
        foreground = "";
        lastWindowEventAt = 0;
        state = ChildLock.state(this);
        power = getSystemService(PowerManager.class);
        targets = ChildApps.read(state);
        ChildBackgroundProtection.ensureWhenEnabled(this);
        clock = new ChildWatchClock(SystemClock.elapsedRealtime(), state.getLong(WATCHED, 0));
        if (state.getBoolean(ChildSettingsActivity.ENABLED, false)) updateActiveWindow();
        handler.removeCallbacks(tick);
        handler.post(tick);
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (state == null || !state.getBoolean(ChildSettingsActivity.ENABLED, false)
                || event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                || event.getPackageName() == null) return;
        // Window events identify the new owner even when the cached active root still
        // belongs to the translucent challenge that just closed (seen on DBB3 Pro).
        if (!ChildLock.locked(this) && event.getClassName() != null
                && ChildChallengeActivity.class.getName().contentEquals(event.getClassName())) return;
        if (BuildConfig.TIMER_TEST_OPTION) android.util.Log.i("TVGuardWindow", "owner=" + event.getPackageName()
                + " class=" + event.getClassName() + " window=" + event.getWindowId());
        lastWindowEventAt = SystemClock.elapsedRealtime();
        setForeground(event.getPackageName().toString());
        // Re-entry must enforce the pending round, not wait for a new viewing budget.
        // Do this before a vendor's stale focused-window cache can replace the event.
        enforcePendingChallenge(SystemClock.elapsedRealtime());
    }

    private boolean updateActiveWindow() {
        // Give a fresh window event time to settle; then reconcile missed vendor
        // events (Tencent opens its home screen after a startup trampoline).
        if (lastWindowEventAt != 0 && SystemClock.elapsedRealtime() - lastWindowEventAt < 500) return true;
        AccessibilityNodeInfo active = getRootInActiveWindow();
        if (active != null) {
            try { if (acceptRoot(active)) return true; }
            finally { active.recycle(); }
        }
        for (android.view.accessibility.AccessibilityWindowInfo window : getWindows()) {
            try {
                if (!window.isFocused() || !window.isActive()) continue;
                AccessibilityNodeInfo root = window.getRoot();
                if (root != null) {
                    try { if (acceptRoot(root)) return true; }
                    finally { root.recycle(); }
                }
            } finally { window.recycle(); }
        }
        return false;
    }

    private boolean acceptRoot(AccessibilityNodeInfo root) {
        if (!root.refresh() || root.getPackageName() == null) return false;
        String owner = root.getPackageName().toString();
        // A paused/dismissed translucent host may remain refreshable in B3P's
        // cache. It is never evidence that the quiz is actually still visible.
        if (owner.equals(getPackageName()) && ChildLock.locked(this)
                && !ChildChallengeActivity.isResumed()) return false;
        setForeground(owner);
        return true;
    }

    private void setForeground(String pkg) {
        if (!foreground.equals(pkg)) {
            account(SystemClock.elapsedRealtime());
            if (targets.contains(foreground)) saveProgress();
            foreground = pkg;
        }
    }

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            targets = ChildApps.read(state);
            boolean interactive = power.isInteractive();
            boolean enabled = state.getBoolean(ChildSettingsActivity.ENABLED, false);
            if (enabled && interactive) updateActiveWindow();
            long now = SystemClock.elapsedRealtime();
            account(now);
            if (!enabled) {
                clock.reset(now);
            } else if (targets.contains(foreground) && interactive) {
                if (ChildLock.locked(ChildWatchService.this)) {
                    enforcePendingChallenge(now);
                } else {
                    long interval = ChildWatchInterval.millis(state);
                    if (clock.elapsed() >= interval) startChallenge(now);
                    else if (clock.elapsed() - state.getLong(WATCHED, 0) >= 10_000L) saveProgress();
                }
            }
            handler.postDelayed(this, 500);
        }
    };

    private final Runnable pendingChallenge = () -> enforcePendingChallenge(SystemClock.elapsedRealtime());

    private void enforcePendingChallenge(long now) {
        if (state == null || power == null || !state.getBoolean(ChildSettingsActivity.ENABLED, false)
                || !power.isInteractive() || !targets.contains(foreground) || !ChildLock.locked(this)
                || ChildChallengeActivity.isResumed()) {
            handler.removeCallbacks(pendingChallenge);
            return;
        }
        long delay = Math.max(0, 1_500L - (now - lastOpen));
        handler.removeCallbacks(pendingChallenge);
        if (delay == 0) openChallenge(now);
        else handler.postDelayed(pendingChallenge, delay);
    }

    private void account(long now) {
        if (clock == null) return;
        boolean counting = state.getBoolean(ChildSettingsActivity.ENABLED, false)
                && power.isInteractive() && targets.contains(foreground) && !ChildLock.locked(this);
        clock.advance(now, counting);
    }

    private void startChallenge(long now) {
        int mode = state.getInt(ChildSettingsActivity.MODE, 2);
        boolean words = mode == 1 || mode == 2 && state.getInt(ChildLock.ROUND, 0) % 2 != 0;
        int level = ChildGrade.read(state);
        if (state.edit().putInt(ChildLock.STAGE, ChildLock.QUESTION).putInt(ChildLock.WRONG, 0)
                .putBoolean(ChildLock.WORDS, words).putInt(ChildLock.LEVEL, level)
                .putLong(ChildLock.SEED, new Random().nextLong()).putString(PLAYBACK, foreground)
                .remove(WATCHED).commit()) {
            clock.reset(now);
            openChallenge(now);
        }
    }

    private void openChallenge(long now) {
        lastOpen = now;
        if (overlay != null) return;
        android.app.admin.DevicePolicyManager policies = getSystemService(android.app.admin.DevicePolicyManager.class);
        boolean managedDevice = policies != null && policies.isDeviceOwnerApp(getPackageName());
        // Enable only after this exact firmware/player combination passes pause,
        // silence, remote-control and recovery checks. DBB3 Pro + Tencent did not.
        if (state.getBoolean("overlay_verified", false) && !managedDevice && !overlayUnavailable
                && state.getInt(ChildLock.STAGE, 0) == ChildLock.QUESTION) {
            try {
                android.media.AudioManager audio = getSystemService(android.media.AudioManager.class);
                overlayFocus = new android.media.AudioFocusRequest.Builder(
                        android.media.AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                        .setAudioAttributes(new android.media.AudioAttributes.Builder()
                                .setUsage(android.media.AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH).build())
                        .setOnAudioFocusChangeListener(change -> {
                            if (change < 0 && overlay != null) fallbackChallenge("audio_focus_loss:" + change);
                        }, handler).build();
                if (audio.requestAudioFocus(overlayFocus) != android.media.AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    fallbackChallenge("audio_focus_denied");
                    return;
                }
                audio.dispatchMediaKeyEvent(new android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN,
                        android.view.KeyEvent.KEYCODE_MEDIA_PAUSE));
                audio.dispatchMediaKeyEvent(new android.view.KeyEvent(android.view.KeyEvent.ACTION_UP,
                        android.view.KeyEvent.KEYCODE_MEDIA_PAUSE));
                overlay = new ChildLock(this, () -> fallbackChallenge("rest_or_parent_entry"));
                overlay.showIfLocked();
                return;
            } catch (android.view.WindowManager.BadTokenException | SecurityException | IllegalStateException failure) {
                android.util.Log.w("GenTVChildLock", "Overlay unavailable; using activity", failure);
                fallbackChallenge("window_error:" + failure.getClass().getSimpleName());
                return;
            }
        }
        openActivityChallenge();
    }

    private void releaseOverlay() {
        if (overlay != null) { overlay.dismiss(); overlay = null; }
        if (overlayFocus != null) {
            getSystemService(android.media.AudioManager.class).abandonAudioFocusRequest(overlayFocus);
            overlayFocus = null;
        }
    }

    private void fallbackChallenge(String reason) {
        overlayFallbackReason = reason;
        android.util.Log.i("GenTVChildLock", "Overlay fallback: " + reason);
        overlayUnavailable = true;
        // Keep the audio focus until the fallback activity has taken the foreground.
        if (overlay != null) { overlay.dismiss(); overlay = null; }
        openActivityChallenge();
        handler.postDelayed(this::releaseOverlay, 500);
    }

    private void openActivityChallenge() {
        startActivity(new Intent(this, ChildChallengeActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    void overlayFinished(String player) {
        releaseOverlay();
        challengeFinished(player);
    }

    static void challengeFinished(String player) {
        ChildWatchService service = connected;
        if (service == null || player.isEmpty()) return;
        service.handler.post(() -> {
            if (service != connected || ChildLock.locked(service)) return;
            service.clock.reset(SystemClock.elapsedRealtime());
            service.setForeground(player);
        });
    }

    private void saveProgress() {
        if (state != null && state.getBoolean(ChildSettingsActivity.ENABLED, false)
                && !ChildLock.locked(this)) state.edit().putLong(WATCHED, clock.elapsed()).apply();
    }

    @Override protected void dump(java.io.FileDescriptor fd, java.io.PrintWriter writer, String[] args) {
        if (state == null) { writer.println("ChildWatch: not connected"); return; }
        writer.println("ChildWatch enabled=" + state.getBoolean(ChildSettingsActivity.ENABLED, false)
                + " interactive=" + power.isInteractive()
                + " foreground=" + foreground + " managed=" + targets.contains(foreground)
                + " targets=" + targets.size()
                + " watched_ms=" + clock.elapsed()
                + " interval_ms=" + (ChildWatchInterval.millis(state))
                + " stage=" + state.getInt(ChildLock.STAGE, 0)
                + " background_protected=" + ChildBackgroundProtection.isProtected()
                + " overlay=" + (overlay != null) + " overlay_fallback=" + overlayUnavailable + " fallback_reason=" + overlayFallbackReason);
    }

    @Override public boolean onUnbind(Intent intent) {
        if (connected == this) connected = null;
        saveProgress();
        handler.removeCallbacks(tick);
        handler.removeCallbacks(pendingChallenge);
        releaseOverlay();
        return super.onUnbind(intent);
    }

    @Override public void onInterrupt() {}

    @Override public void onDestroy() {
        if (connected == this) connected = null;
        releaseOverlay();
        handler.removeCallbacks(tick);
        handler.removeCallbacks(pendingChallenge);
        saveProgress();
        super.onDestroy();
    }
}
