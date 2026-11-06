package com.tv.screenguard;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.provider.Settings;
/** Small platform bridge; no launcher or desktop dependency. */
final class GuardPlatform {
 static int boot(Context context) {
  return Settings.Global.getInt(context.getContentResolver(), Settings.Global.BOOT_COUNT, -1);
 }
 static Intent intent(PackageManager pm, String pkg) {
  Intent intent = pm.getLeanbackLaunchIntentForPackage(pkg);
  return intent != null ? intent : pm.getLaunchIntentForPackage(pkg);
 }
}
