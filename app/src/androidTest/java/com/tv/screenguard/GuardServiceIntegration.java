package com.tv.screenguard;
import android.app.*;import android.content.*;import android.os.*;import android.widget.*;import java.util.*;
/** Emulator harness authorises via shell, explicitly distinct from the self-ADB acceptance suite. */
public final class GuardServiceIntegration extends Instrumentation {
 int checks;String pin="825937";
 public void onCreate(Bundle b){super.onCreate(b);start();}
 void check(boolean b,String m){if(!b)throw new AssertionError(m);checks++;Bundle out=new Bundle();out.putString("stream","PASS "+m+"\n");sendStatus(0,out);}
 String shell(String cmd)throws Exception{try(java.io.FileInputStream f=new java.io.FileInputStream(getUiAutomation(1).executeShellCommand(cmd).getFileDescriptor())){java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();byte[] b=new byte[2048];int n;while((n=f.read(b))!=-1)out.write(b,0,n);return new String(out.toByteArray(),java.nio.charset.StandardCharsets.UTF_8).trim();}}
 Activity open(){Activity p=startActivitySync(new Intent(getTargetContext(),ChildSettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));runOnMainSync(()->{AlertDialog d=(AlertDialog)GuardIntegration.field(p,"passwordDialog");for(EditText f:GuardIntegration.inputs(d.getWindow().getDecorView()))f.setText(pin);d.getButton(-1).performClick();});return p;}
 void launch(String pkg){getTargetContext().startActivity(new Intent().setClassName(pkg,"com.tvguard.fixture.Player").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}
 long watched(){Object service=GuardIntegration.fieldStatic(ChildWatchService.class,"connected");return ((ChildWatchClock)GuardIntegration.field(service,"clock")).elapsed();}
 public void onStart(){Bundle result=new Bundle();try{
  check(Build.HARDWARE.contains("ranchu")||Build.HARDWARE.contains("goldfish"),"dedicated emulator required");
  android.content.SharedPreferences state=ChildLock.state(getTargetContext());state.edit().clear().commit();Activity page=open();
  check(!ChildWatchService.isConnected(),"initially disconnected");
  String component="com.tv.screenguard/com.tv.screenguard.ChildWatchService";
  shell("settings put secure enabled_accessibility_services "+component);shell("settings put secure accessibility_enabled 1");
  for(int i=0;i<40&&!ChildWatchService.isConnected();i++)SystemClock.sleep(200);
  check(ChildWatchService.isConnected(),"shell test-harness grant results in real binding");SystemClock.sleep(1400);
  runOnMainSync(()->check(((TextView)GuardIntegration.field(page,"protectionStatus")).getText().toString().equals(page.getString(R.string.child_service_ready)),"connected service distinct from enabled mode"));
  ChildApps.save(state,Collections.singleton("com.tvguard.fixture"));state.edit().putBoolean("enabled",true).commit();
  launch("com.tvguard.fixture");SystemClock.sleep(4000);long first=watched();check(first>=2500,"actual managed foreground accumulates");
  launch("com.tvguard.unmanagedfixture");SystemClock.sleep(1000);long paused=watched();SystemClock.sleep(2500);check(watched()-paused<600,"actual unselected foreground pauses timer");
  ChildApps.save(state,new HashSet<>(Arrays.asList("com.tvguard.fixture","com.tvguard.unmanagedfixture")));SystemClock.sleep(3000);long second=watched();check(second>paused+2000,"second selected application shares cumulative budget");
  launch("com.tvguard.fixture");SystemClock.sleep(2500);check(watched()>second+1500,"switching selected apps preserves cumulative timer");
  getTargetContext().startActivity(new Intent(getTargetContext(),ChildSettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
  for(int i=0;i<40&&!((Boolean)GuardIntegration.field(page,"resumed"));i++)SystemClock.sleep(200);
  runOnMainSync(()->{AlertDialog d=(AlertDialog)GuardIntegration.field(page,"passwordDialog");for(EditText f:GuardIntegration.inputs(d.getWindow().getDecorView()))f.setText(pin);d.getButton(-1).performClick();});
  Activity status=page;SystemClock.sleep(1200);
  runOnMainSync(()->check(((TextView)GuardIntegration.field(status,"protectionStatus")).getText().toString().equals(status.getString(R.string.child_service_active)),"actual active status shown"));
  shell("settings delete secure enabled_accessibility_services");
  for(int i=0;i<40&&ChildWatchService.isConnected();i++)SystemClock.sleep(200);
  check(!ChildWatchService.isConnected(),"revocation clears real connection");SystemClock.sleep(1400);
  runOnMainSync(()->{check(((TextView)GuardIntegration.field(status,"protectionStatus")).getText().toString().equals(status.getString(R.string.child_service_disconnected)),"revoked service never claims active protection");check(((Button)GuardIntegration.field(status,"permissionButton")).isEnabled(),"activation control restored after revocation");});
  result.putString("stream","TOTAL "+checks+" passed\n");finish(-1,result);
 }catch(Throwable e){result.putString("stream","FAIL "+e+"\n");finish(0,result);}}
}
