package com.tv.screenguard;
import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.lang.reflect.*;
import java.util.*;
/** Emulator-only verification against the unchanged installable APK. */
public final class GuardIntegration extends Instrumentation {
 private int checks; private String phase; private final String pin="593826";
 public void onCreate(Bundle args){super.onCreate(args);phase=args.getString("phase","setup");start();}
 void check(boolean b,String m){if(!b)throw new AssertionError(m);checks++;Bundle out=new Bundle();out.putString("stream","PASS "+m+"\n");sendStatus(0,out);}
 public void onStart(){Bundle out=new Bundle();try{
  check(Build.HARDWARE.contains("ranchu")||Build.HARDWARE.contains("goldfish"),"dedicated emulator only");
  SharedPreferences state=ChildLock.state(getTargetContext());
  if(phase.equals("setup")){
   state.edit().clear().commit();
   Activity page=startActivitySync(new Intent(getTargetContext(),ChildSettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
   runOnMainSync(()->{
    AlertDialog dialog=(AlertDialog)field(page,"passwordDialog");List<EditText> fields=inputs(dialog.getWindow().getDecorView());
    check(fields.size()==2,"first launch creates PIN with confirmation");
    fields.get(0).setText("12");dialog.getButton(-1).performClick();check(!((Boolean)field(page,"authorized")),"short PIN rejected");
    fields.get(0).setText(pin);fields.get(1).setText("123456");dialog.getButton(-1).performClick();check(!((Boolean)field(page,"authorized")),"mismatched PIN rejected");
    fields.get(1).setText(pin);dialog.getButton(-1).performClick();check((Boolean)field(page,"authorized"),"matching PIN opens standalone settings");
    check(state.getInt("minutes",10)==10,"default ten minutes");
    check(ChildWatchInterval.minutes(30,false)==10,"production interval guard rejects unsupported values");
    check(!ChildWatchService.isConnected(),"service initially genuinely disconnected");
    ((Button)field(page,"playerButton")).performClick();
   });waitForIdleSync();capture("app-picker");
   runOnMainSync(()->{AlertDialog dialog=(AlertDialog)field(page,"playerDialog");ListView list=dialog.getListView();check(list.getCount()>=2,"multi-select app list");dialog.getButton(-2).performClick();});
   ChildApps.save(state,Collections.singleton("com.tvguard.fixture"));
   state.edit().putInt("mode",0).putInt(ChildGrade.KEY,1).commit();
   runOnMainSync(()->((Button)field(page,"permissionButton")).performClick());
   long end=SystemClock.elapsedRealtime()+90000;
   while(!ChildWatchService.isConnected()&&SystemClock.elapsedRealtime()<end){
    runOnMainSync(()->{AlertDialog dialog=(AlertDialog)field(page,"serviceDialog");if(dialog!=null&&dialog.isShowing()){List<EditText> f=inputs(dialog.getWindow().getDecorView());if(f.size()==1){f.get(0).setText("5555");dialog.getButton(-1).performClick();}}});
    // Accept only our own isolated emulator's standard system ADB prompt.
    android.view.accessibility.AccessibilityNodeInfo root=getUiAutomation(1).getRootInActiveWindow();
    if(root!=null){if("com.android.systemui".contentEquals(root.getPackageName()))for(android.view.accessibility.AccessibilityNodeInfo n:root.findAccessibilityNodeInfosByViewId("android:id/button1")){if("Allow".contentEquals(n.getText())||"允许".contentEquals(n.getText()))n.performAction(16);n.recycle();}root.recycle();}
    SystemClock.sleep(300);
   }
   check(ChildWatchService.isConnected(),"user-click local ADB path actually binds standalone service");
   SystemClock.sleep(1500);
   runOnMainSync(()->{
    if(!((Boolean)field(page,"authorized"))){AlertDialog dialog=(AlertDialog)field(page,"passwordDialog");for(EditText f:inputs(dialog.getWindow().getDecorView()))f.setText(pin);dialog.getButton(-1).performClick();}
    check(((Button)field(page,"permissionButton")).getText().toString().equals(page.getString(R.string.child_adb_button_enabled)),"enabled label follows real binding");
    ((Button)field(page,"enableButton")).performClick();check(state.getBoolean("enabled",false),"protection enabled");
   });capture("settings-en");
   ActivityMonitor monitor=addMonitor(ChildChallengeActivity.class.getName(),null,false);
   long began=SystemClock.elapsedRealtime();
   getTargetContext().startActivity(new Intent().setClassName("com.tvguard.fixture","com.tvguard.fixture.Player").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
   for(int i=0;i<43 && state.getInt(ChildLock.STAGE,0)==0;i++){
    SystemClock.sleep(15000);
    Bundle progress=new Bundle();progress.putString("stream","TIMER elapsed_ms="+(SystemClock.elapsedRealtime()-began)+" watched_ms="+state.getLong(ChildWatchService.WATCHED,0)+" stage="+state.getInt(ChildLock.STAGE,0)+"\n");sendStatus(0,progress);
   }
   long elapsed=SystemClock.elapsedRealtime()-began;
   check(elapsed>=600000 && elapsed<645000,"default 600000 ms unaccelerated automatic question at "+elapsed+" ms");
   Activity challenge=monitor.getLastActivity();check(challenge!=null&&state.getInt(ChildLock.STAGE,0)==1,"timer opens standalone challenge");
   capture("automatic-question");
   org.json.JSONArray saved=new org.json.JSONArray(state.getString("question_snapshot",""));String answer=saved.getString(saved.getInt(1)+2);
   runOnMainSync(()->{Dialog dialog=(Dialog)field(field(challenge,"lock"),"dialog");View button=find(dialog.getWindow().getDecorView(),answer);check(button!=null&&button.requestFocus(),"answer receives remote focus");});
   sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_CENTER);SystemClock.sleep(2000);
   check(state.getInt(ChildLock.STAGE,0)==0,"remote OK correct answer unlocks");
   org.json.JSONObject first=playback();SystemClock.sleep(2200);org.json.JSONObject second=playback();
   check(first.getBoolean("playing")&&second.getBoolean("playing")&&second.getBoolean("resumed"),"actual VideoView resumes playback");
   check(second.getInt("position_ms")!=first.getInt("position_ms"),"video progress advances "+first.getInt("position_ms")+" to "+second.getInt("position_ms"));
   capture("playback-restored");
   removeMonitor(monitor);

  }else if(phase.equals("answer")){
   check(state.getInt(ChildLock.STAGE,0)==1,"real timer left persisted question");
   Activity challenge=startActivitySync(new Intent(getTargetContext(),ChildChallengeActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
   SystemClock.sleep(500);capture("question");
   org.json.JSONArray saved=new org.json.JSONArray(state.getString("question_snapshot",""));String answer=saved.getString(saved.getInt(1)+2);
   runOnMainSync(()->{Dialog dialog=(Dialog)field(field(challenge,"lock"),"dialog");View button=find(dialog.getWindow().getDecorView(),answer);check(button!=null&&button.requestFocus(),"answer receives remote focus");});
   sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_CENTER);SystemClock.sleep(1800);
   check(state.getInt(ChildLock.STAGE,0)==0,"remote OK correct answer unlocks");
  }
  out.putString("stream","TOTAL "+checks+" passed\n");finish(-1,out);
 }catch(Throwable e){out.putString("stream","FAIL "+e+"\n");finish(0,out);}}
 org.json.JSONObject playback() throws Exception {
  java.util.concurrent.CountDownLatch done=new java.util.concurrent.CountDownLatch(1);java.util.concurrent.atomic.AtomicReference<String> data=new java.util.concurrent.atomic.AtomicReference<>();
  BroadcastReceiver receiver=new BroadcastReceiver(){public void onReceive(Context c,Intent i){data.set(getResultData());done.countDown();}};
  getTargetContext().sendOrderedBroadcast(new Intent("com.tvguard.fixture.PLAYBACK").setClassName("com.tvguard.fixture","com.tvguard.fixture.Controller").putExtra("operation","playback"),null,receiver,new Handler(Looper.getMainLooper()),0,null,null);
  if(!done.await(10,java.util.concurrent.TimeUnit.SECONDS))throw new AssertionError("playback timeout");return new org.json.JSONObject(data.get());
 }
 void capture(String name)throws Exception{android.graphics.Bitmap b=getUiAutomation(1).takeScreenshot();java.io.File dir=getTargetContext().getExternalFilesDir("proof");dir.mkdirs();try(java.io.FileOutputStream f=new java.io.FileOutputStream(new java.io.File(dir,name+".png"))){b.compress(android.graphics.Bitmap.CompressFormat.PNG,100,f);}b.recycle();}
 static Object fieldStatic(Class<?> c,String n){try{Field f=c.getDeclaredField(n);f.setAccessible(true);return f.get(null);}catch(Exception e){throw new AssertionError(e);}}
 static Object field(Object o,String n){try{Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(o);}catch(Exception e){throw new AssertionError(e);}}
 static List<EditText> inputs(View v){List<EditText> list=new ArrayList<>();if(v instanceof EditText)list.add((EditText)v);if(v instanceof ViewGroup g)for(int i=0;i<g.getChildCount();i++)list.addAll(inputs(g.getChildAt(i)));return list;}
 static View find(View v,String text){if(v instanceof TextView t&&t.getText().toString().equals(text)&&v.isClickable())return v;if(v instanceof ViewGroup g)for(int i=0;i<g.getChildCount();i++){View x=find(g.getChildAt(i),text);if(x!=null)return x;}return null;}
}
