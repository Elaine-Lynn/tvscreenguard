package com.tv.screenguard;
import android.app.*;
import android.os.*;
import android.content.*;
import android.widget.*;
import android.view.*;
/** Three separate instrumentation processes prove question/rest survive process loss. Synthetic states are explicit. */
public final class GuardPersistenceIntegration extends Instrumentation {
 String phase; int checks;
 public void onCreate(Bundle args){super.onCreate(args);phase=args.getString("phase","pending");start();}
 void check(boolean b,String m){if(!b)throw new AssertionError(m);checks++;Bundle p=new Bundle();p.putString("stream","PASS "+m+"\n");sendStatus(0,p);}
 public void onStart(){Bundle result=new Bundle();try{
  check(Build.HARDWARE.contains("ranchu")||Build.HARDWARE.contains("goldfish"),"emulator only");
  SharedPreferences state=ChildLock.state(getTargetContext());
  SharedPreferences proof=getTargetContext().getSharedPreferences("persistence_proof",0);
  if(phase.equals("pending"))state.edit().putBoolean("enabled",false).putInt(ChildLock.STAGE,1).putInt(ChildLock.LEVEL,1).putLong(ChildLock.SEED,771).putBoolean(ChildLock.WORDS,false).remove("question_snapshot").putInt(ChildLock.WRONG,0).commit();
  ActivityMonitor monitor=addMonitor(ChildChallengeActivity.class.getName(),null,false);
  getTargetContext().startActivity(new Intent(getTargetContext(),ChildSettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
  Activity activity=monitor.waitForActivityWithTimeout(6000);check(activity!=null,"standalone entry restores pending challenge");SystemClock.sleep(900);
  if(phase.equals("pending")){
   String snapshot=state.getString("question_snapshot","");check(!snapshot.isEmpty(),"pending question snapshot persisted");proof.edit().putString("question",snapshot).commit();
  }else if(phase.equals("resume")){
   check(proof.getString("question","").equals(state.getString("question_snapshot","")),"exact question and options survive new process");
   for(int i=0;i<3;i++){
    org.json.JSONArray saved=new org.json.JSONArray(state.getString("question_snapshot",""));int correct=saved.getInt(1);String wrong=saved.getString(2+(correct+1)%4);
    runOnMainSync(()->{Dialog dialog=(Dialog)GuardIntegration.field(GuardIntegration.field(activity,"lock"),"dialog");View button=GuardIntegration.find(dialog.getWindow().getDecorView(),wrong);check(button!=null&&button.requestFocus(),"wrong option focusable");});
    sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_CENTER);SystemClock.sleep(400);
   }
   check(state.getInt(ChildLock.STAGE,0)==2,"three UI wrong answers enter rest");check(state.getLong("rest_remaining",0)>0,"rest checkpoint saved");
  }else{
   check(state.getInt(ChildLock.STAGE,0)==2,"rest survives another process restart");
   check(state.getLong("rest_remaining",0)>0&&state.getLong("rest_remaining",0)<=300000,"rest duration remains bounded");
  }
  android.graphics.Bitmap bitmap=getUiAutomation(1).takeScreenshot();java.io.File dir=getTargetContext().getExternalFilesDir("proof");dir.mkdirs();try(java.io.FileOutputStream f=new java.io.FileOutputStream(new java.io.File(dir,"persistence-"+phase+".png"))){bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,f);}bitmap.recycle();
  if(phase.equals("rest")){state.edit().remove(ChildLock.STAGE).remove("question_snapshot").commit();runOnMainSync(activity::finish);}
  removeMonitor(monitor);result.putString("stream","TOTAL "+checks+" passed\n");finish(-1,result);
 }catch(Throwable e){result.putString("stream","FAIL "+e+"\n");finish(0,result);}}
}
