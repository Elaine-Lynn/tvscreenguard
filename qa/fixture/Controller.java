package com.tvguard.fixture;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import java.util.ArrayList;
import java.util.List;
/** Temporary emulator fault injector. It can change only the isolated QA service entry. */
public final class Controller extends BroadcastReceiver {
 public void onReceive(Context context,Intent intent){
  String hardware=android.os.Build.HARDWARE;
  if(!(hardware.contains("ranchu")||hardware.contains("goldfish"))){setResultCode(0);return;}
  try {
   if ("playback".equals(intent.getStringExtra("operation"))) {
    android.widget.VideoView video=Player.activeVideo;
    setResultData(new org.json.JSONObject().put("resumed",Player.resumed)
      .put("playing",video!=null&&video.isPlaying()).put("position_ms",video==null?-1:video.getCurrentPosition())
      .put("duration_ms",video==null?-1:video.getDuration()).toString());setResultCode(-1);return;
   }
   String before=Settings.Secure.getString(context.getContentResolver(),Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
   String operation=intent.getStringExtra("operation");
   if(!"read".equals(operation)){
    List<String> services=new ArrayList<>();
    if(before!=null&&!before.isEmpty())for(String s:before.split(":")){
     if(!s.startsWith("com.tv.screenguard/"))services.add(s);
    }
    if("enable".equals(operation))services.add("com.tv.screenguard/com.tv.screenguard.ChildWatchService");
    else if(!"disable".equals(operation))throw new IllegalArgumentException("Unknown test operation");
    String after=android.text.TextUtils.join(":",services);
    if(!Settings.Secure.putString(context.getContentResolver(),Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,after))throw new IllegalStateException("Refused");
    if(!Settings.Secure.putInt(context.getContentResolver(),Settings.Secure.ACCESSIBILITY_ENABLED,services.isEmpty()?0:1))throw new IllegalStateException("Refused");
   }
   setResultData(Settings.Secure.getString(context.getContentResolver(),Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES));setResultCode(-1);
  }catch(Exception error){setResultData(error.getClass().getSimpleName());setResultCode(0);}
 }
}
