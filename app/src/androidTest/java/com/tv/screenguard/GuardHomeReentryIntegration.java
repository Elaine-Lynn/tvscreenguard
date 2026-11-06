package com.tv.screenguard;

import android.app.Activity;
import android.app.Dialog;
import android.app.Instrumentation;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.View;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

/** Dedicated-emulator regression. Synthetic initial QUESTION; actual Home, app re-entry and answer UI.
 * Does not simulate or claim to repair a vendor force-stop / revoked accessibility permission.
 */
public final class GuardHomeReentryIntegration extends Instrumentation {
    private static final String FIXTURE = "com.tvguard.fixture";
    private SharedPreferences state;
    private Object boundService;
    private int checks;

    @Override public void onCreate(Bundle arguments) { super.onCreate(arguments); start(); }

    private void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
        checks++;
        Bundle progress = new Bundle();
        progress.putString("stream", "PASS " + message + "\n");
        sendStatus(0, progress);
    }

    @Override public void onStart() {
        Bundle result = new Bundle();
        String before = null, global = null, updated = null;
        int status = Activity.RESULT_OK;
        try {
            check(Build.HARDWARE.contains("ranchu") || Build.HARDWARE.contains("goldfish"),
                    "dedicated emulator required before modifying any state");
            check(getTargetContext().getPackageName().equals("com.tv.screenguard"), "expected isolated target package");
            getTargetContext().getPackageManager().getApplicationInfo(FIXTURE, 0);
            state = ChildLock.state(getTargetContext());
            check(state.edit().clear().commit(), "fresh emulator-only test state");
            check(ChildApps.save(state, Collections.singleton(FIXTURE)), "real video fixture selected");
            before = shell("settings get secure enabled_accessibility_services");
            global = shell("settings get secure accessibility_enabled");
            check(before.matches("[A-Za-z0-9_.$/:]+")
                            && (global.equals("null") || global.equals("0") || global.equals("1")),
                    "baseline settings support exact plain-command restoration");
            updated = ChildServiceSettings.add(before, getTargetContext().getPackageName());
            check(before.equals(shell("settings get secure enabled_accessibility_services"))
                            && global.equals(shell("settings get secure accessibility_enabled")),
                    "accessibility baseline remained stable before harness grant");
            // UiAutomation executes command tokens, not a shell script. Values here
            // are validated component lists with no spaces or shell interpretation.
            shell("settings put secure enabled_accessibility_services " + updated);
            check(updated.equals(shell("settings get secure enabled_accessibility_services")),
                    "isolated service appended while retaining other entries");
            shell("settings put secure accessibility_enabled 1");
            check(updated.equals(shell("settings get secure enabled_accessibility_services"))
                            && "1".equals(shell("settings get secure accessibility_enabled")),
                    "test harness grant verified from actual settings");
            await(ChildWatchService::isConnected, 8000, "service did not bind");
            boundService = fieldStatic(ChildWatchService.class, "connected");
            launchFixture();
            await(() -> playbackUnchecked().optBoolean("resumed"), 6000, "video fixture did not resume");

            seedPendingQuestion(771);
            await(this::challengeReady, 4000, "initial pending question was not enforced");
            String snapshot = state.getString("question_snapshot", "");
            check(!snapshot.isEmpty(), "pending question snapshot exists");
            ChildChallengeActivity originalHost = currentChallenge();
            getTargetContext().startActivity(new Intent(getTargetContext(), ChildChallengeActivity.class)
                    .putExtra("guard_qa_reentry_probe", 771L).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            await(() -> receivedReentryProbe(771L) && challengeReady(), 3000,
                    "singleTask relaunch did not deliver its intent and restore dialog");
            check(currentChallenge() == originalHost
                            && snapshot.equals(state.getString("question_snapshot", "")),
                    "singleTask relaunch reuses the host and exact pending round");
            for (int cycle = 1; cycle <= 2; cycle++) {
                homeAndReturn(ChildLock.QUESTION, snapshot, 0, cycle);
            }
            chooseAnswer(true);
            await(() -> state.getInt(ChildLock.STAGE, 0) == 0 && !ChildChallengeActivity.isResumed(),
                    4000, "correct answer did not dismiss pending restriction");
            JSONObject first = playback();
            SystemClock.sleep(1200);
            JSONObject second = playback();
            check(first.getBoolean("playing") && second.getBoolean("playing") && second.getBoolean("resumed"),
                    "actual VideoView resumes after required correct answer");
            check(first.getInt("position_ms") != second.getInt("position_ms"), "actual playback progress advances");
            assertBound("after correct answer");

            seedPendingQuestion(772);
            await(this::challengeReady, 4000, "second pending question was not enforced");
            for (int wrong = 1; wrong <= 3; wrong++) {
                chooseAnswer(false);
                final int count = wrong;
                await(() -> state.getInt(ChildLock.WRONG, 0) == count, 2000, "wrong answer was not committed");
            }
            check(state.getInt(ChildLock.STAGE, 0) == ChildLock.REST, "three actual UI wrong answers enter REST");
            long deadline = state.getLong("rest_elapsed_end", -1);
            check(deadline > SystemClock.elapsedRealtime(), "rest uses a future monotonic deadline");
            String restSeed = Long.toString(state.getLong(ChildLock.SEED, -1));
            for (int cycle = 1; cycle <= 2; cycle++) {
                homeAndReturn(ChildLock.REST, null, deadline, cycle);
                check(restSeed.equals(Long.toString(state.getLong(ChildLock.SEED, -1)))
                                && state.getInt(ChildLock.WRONG, 0) == 3,
                        "REST preserves round parameters across Home/re-entry " + cycle);
            }
            assertBound("through all QUESTION and REST Home cycles");
            result.putString("stream", "TOTAL " + checks + " checks passed\n");
        } catch (Throwable failure) {
            result.putString("stream", "FAIL " + failure + "\n");
            status = Activity.RESULT_CANCELED;
        } finally {
            // Only the disposable emulator was accepted above. Never clear a real user's PIN.
            if (state != null) {
                ChildChallengeActivity challenge = currentChallenge();
                state.edit().clear().commit();
                if (challenge != null) onMain(challenge::finish);
            }
            if (before != null && global != null && updated != null) {
                try {
                    // Never overwrite an intervening edit, including removal by the system.
                    if (updated.equals(shell("settings get secure enabled_accessibility_services"))
                            && "1".equals(shell("settings get secure accessibility_enabled"))) {
                        restoreSetting("enabled_accessibility_services", before);
                        restoreSetting("accessibility_enabled", global);
                        if (!before.equals(shell("settings get secure enabled_accessibility_services"))
                                || !global.equals(shell("settings get secure accessibility_enabled"))) {
                            throw new java.io.IOException("Accessibility baseline restoration did not verify");
                        }
                    }
                } catch (Exception cleanupFailure) {
                    status = Activity.RESULT_CANCELED;
                    result.putString("stream", result.getString("stream", "") + "FAIL cleanup: " + cleanupFailure + "\n");
                }
            }
        }
        finish(status, result);
    }

    private boolean challengeReady() {
        ChildChallengeActivity current = currentChallenge();
        if (current == null) return false;
        java.util.concurrent.atomic.AtomicBoolean ready = new java.util.concurrent.atomic.AtomicBoolean();
        onMain(() -> {
            Dialog dialog = (Dialog) field(field(current, "lock"), "dialog");
            ready.set(ChildChallengeActivity.isResumed() && dialog != null && dialog.isShowing());
        });
        return ready.get();
    }

    private boolean receivedReentryProbe(long probe) {
        java.util.concurrent.atomic.AtomicBoolean delivered = new java.util.concurrent.atomic.AtomicBoolean();
        onMain(() -> {
            ChildChallengeActivity current = currentChallenge();
            delivered.set(current != null && current.getIntent().getLongExtra("guard_qa_reentry_probe", 0) == probe);
        });
        return delivered.get();
    }

    private void seedPendingQuestion(long seed) {
        check(state.edit().putBoolean(ChildSettingsActivity.ENABLED, true)
                .putInt(ChildLock.STAGE, ChildLock.QUESTION).putInt(ChildLock.LEVEL, 1)
                .putBoolean(ChildLock.WORDS, false).putLong(ChildLock.SEED, seed)
                .putInt(ChildLock.WRONG, 0).putString(ChildWatchService.PLAYBACK, FIXTURE)
                .remove("question_snapshot").remove(ChildWatchService.WATCHED).commit(),
                "synthetic pending QUESTION setup committed");
    }

    private void homeAndReturn(int stage, String snapshot, long restDeadline, int cycle) throws Exception {
        assertBound("before Home " + stage + "/" + cycle);
        shell("input keyevent 3");
        await(() -> !ChildChallengeActivity.isResumed(), 4000, "Home did not leave the challenge");
        SystemClock.sleep(1800); // Expire the normal launch throttle while confirming Home remains available.
        check(!ChildChallengeActivity.isResumed(), "Home remains available while pending " + stage + "/" + cycle);
        check(state.getInt(ChildLock.STAGE, 0) == stage, "Home does not clear pending stage " + stage + "/" + cycle);
        assertBound("while Home visible " + stage + "/" + cycle);
        long began = SystemClock.elapsedRealtime();
        launchFixture();
        await(this::challengeReady, 3000, "managed re-entry did not reassert existing restriction");
        long elapsed = SystemClock.elapsedRealtime() - began;
        check(elapsed < 3000, "pending re-entry requires no new interval (" + elapsed + " ms)");
        check(state.getInt(ChildLock.STAGE, 0) == stage, "managed re-entry preserves stage " + stage + "/" + cycle);
        if (snapshot != null) check(snapshot.equals(state.getString("question_snapshot", "")),
                "exact question and answer order survive Home/re-entry " + cycle);
        if (restDeadline > 0) check(restDeadline == state.getLong("rest_elapsed_end", -1),
                "REST deadline is not restarted by Home/re-entry " + cycle);
        SystemClock.sleep(500);
        check(!playback().getBoolean("resumed"), "managed player is interrupted by restored challenge");
        assertBound("after re-entry " + stage + "/" + cycle);
    }

    private void chooseAnswer(boolean correct) throws Exception {
        await(() -> !state.getString("question_snapshot", "").isEmpty(), 2000, "question snapshot not ready");
        JSONArray saved = new JSONArray(state.getString("question_snapshot", ""));
        int index = saved.getInt(1);
        String label = saved.getString(2 + (correct ? index : (index + 1) % 4));
        setInTouchMode(false);
        onMain(() -> {
            ChildChallengeActivity challenge = currentChallenge();
            if (challenge == null) throw new AssertionError("challenge is not resumed");
            Dialog dialog = (Dialog) field(field(challenge, "lock"), "dialog");
            View choice = GuardIntegration.find(dialog.getWindow().getDecorView(), label);
            check(choice != null && choice.requestFocus(), "actual answer button receives remote focus");
        });
        SystemClock.sleep(150);
        sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_CENTER);
        SystemClock.sleep(250);
    }

    private void assertBound(String phase) {
        check(ChildWatchService.isConnected() && fieldStatic(ChildWatchService.class, "connected") == boundService,
                "same accessibility binding remains alive " + phase);
    }

    private void launchFixture() {
        getTargetContext().startActivity(new Intent().setClassName(FIXTURE, FIXTURE + ".Player")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    private static ChildChallengeActivity currentChallenge() {
        return (ChildChallengeActivity) fieldStatic(ChildChallengeActivity.class, "resumed");
    }

    private JSONObject playbackUnchecked() {
        try { return playback(); } catch (Exception failure) { return new JSONObject(); }
    }

    private JSONObject playback() throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<String> data = new AtomicReference<>();
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) { data.set(getResultData()); done.countDown(); }
        };
        getTargetContext().sendOrderedBroadcast(new Intent("com.tvguard.fixture.PLAYBACK")
                .setClassName(FIXTURE, FIXTURE + ".Controller").putExtra("operation", "playback"), null,
                receiver, new Handler(Looper.getMainLooper()), 0, null, null);
        if (!done.await(5, TimeUnit.SECONDS)) throw new AssertionError("fixture playback timeout");
        return new JSONObject(data.get());
    }

    private void await(BooleanSupplier condition, long timeout, String message) {
        long deadline = SystemClock.elapsedRealtime() + timeout;
        while (SystemClock.elapsedRealtime() < deadline) {
            if (condition.getAsBoolean()) return;
            SystemClock.sleep(50);
        }
        throw new AssertionError(message);
    }

    private void onMain(Runnable action) {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        runOnMainSync(() -> { try { action.run(); } catch (Throwable error) { failure.set(error); } });
        if (failure.get() != null) throw new AssertionError(failure.get());
    }

    private void restoreSetting(String key, String baseline) throws Exception {
        if (baseline.equals("null")) shell("settings delete secure " + key);
        else shell("settings put secure " + key + " " + baseline);
    }

    private String shell(String command) throws Exception {
        try (android.os.ParcelFileDescriptor descriptor = getUiAutomation(1).executeShellCommand(command);
             FileInputStream input = new FileInputStream(descriptor.getFileDescriptor())) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[2048];
            int count;
            while ((count = input.read(buffer)) != -1) bytes.write(buffer, 0, count);
            return new String(bytes.toByteArray(), StandardCharsets.UTF_8).trim();
        }
    }

    private static Object field(Object target, String name) {
        try { Field field = target.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(target); }
        catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
    }

    private static Object fieldStatic(Class<?> target, String name) {
        try { Field field = target.getDeclaredField(name); field.setAccessible(true); return field.get(null); }
        catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
    }
}
