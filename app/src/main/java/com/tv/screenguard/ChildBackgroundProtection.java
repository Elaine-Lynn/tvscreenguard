package com.tv.screenguard;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import java.util.concurrent.atomic.AtomicBoolean;

/** Scoped DBB3 Pro process-protection adapter; never changes another package or system display/Home. */
final class ChildBackgroundProtection {
    private static final String AUTHORITY = "com.dangbei.dangbeiref.data";
    private static final String PROVIDER_PACKAGE = "com.dangbei.dbdownloadrefs";
    private static final String URI_TEXT = "content://" + AUTHORITY + "/process_protect_list_info";
    private static final String OWNED = "b3p_process_protection_owned";
    private static final AtomicBoolean BUSY = new AtomicBoolean();
    private static volatile boolean protectedNow;

    private ChildBackgroundProtection() {}

    static boolean modelRequiresProtection() { return supportedModel(Build.MODEL, Build.VERSION.SDK_INT); }
    static boolean supportedModel(String model, int sdk) { return sdk == 28 && "DBB3 Pro".equals(model); }
    static boolean isProtected() { return !modelRequiresProtection() || protectedNow; }

    static void ensureWhenEnabled(Context context) {
        if (!modelRequiresProtection() || !BUSY.compareAndSet(false, true)) return;
        Context app = context.getApplicationContext();
        new Thread(() -> {
            try {
                ProviderInfo provider = app.getPackageManager().resolveContentProvider(AUTHORITY, 0);
                if (provider == null || !PROVIDER_PACKAGE.equals(provider.packageName)
                        || !provider.exported || !provider.enabled || provider.applicationInfo == null
                        || (provider.applicationInfo.flags & (ApplicationInfo.FLAG_SYSTEM
                            | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) == 0) {
                    protectedNow = false;
                    return;
                }
                SharedPreferences prefs = ChildLock.state(app);
                boolean enabled = prefs.getBoolean(ChildSettingsActivity.ENABLED, false);
                String pkg = app.getPackageName();
                boolean present = containsSelf(app, pkg);
                if (enabled && !present) {
                    ContentValues values = new ContentValues();
                    values.put("appPackageName", pkg);
                    values.put("appName", app.getString(R.string.app_name));
                    values.put("appVersion", BuildConfig.VERSION_CODE);
                    // Record ownership before insertion so process loss cannot lose the recovery intent.
                    if (!prefs.edit().putBoolean(OWNED, true).commit()) return;
                    app.getContentResolver().insert(Uri.parse(URI_TEXT), values);
                    present = containsSelf(app, pkg);
                } else if (!enabled && prefs.getBoolean(OWNED, false)) {
                    app.getContentResolver().delete(Uri.parse(URI_TEXT), "appPackageName=?", new String[]{pkg});
                    present = containsSelf(app, pkg);
                    if (!present) prefs.edit().remove(OWNED).commit();
                }
                protectedNow = present;
            } catch (RuntimeException failure) {
                protectedNow = false;
                android.util.Log.w("TVGuardBackground", "Scoped process protection unavailable: "
                        + failure.getClass().getSimpleName());
            } finally { BUSY.set(false); }
        }, "TVGuardBackgroundProtection").start();
    }

    private static boolean containsSelf(Context context, String pkg) {
        try (Cursor cursor = context.getContentResolver().query(Uri.parse(URI_TEXT), new String[]{"appPackageName"},
                "appPackageName=?", new String[]{pkg}, null)) {
            if (cursor == null) throw new IllegalStateException("No protection cursor");
            int column = cursor.getColumnIndexOrThrow("appPackageName");
            while (cursor.moveToNext()) if (pkg.equals(cursor.getString(column))) return true;
            return false;
        }
    }
}
