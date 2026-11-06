package com.tvguard.fixture;
import android.app.Activity;
import android.os.Bundle;
import android.net.Uri;
import android.widget.VideoView;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.util.Log;
public class Player extends Activity {
    private VideoView video;
    static VideoView activeVideo;
    static boolean resumed;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        FrameLayout root=new FrameLayout(this);
        video=new VideoView(this);activeVideo=video;root.addView(video,new FrameLayout.LayoutParams(-1,-1));
        TextView title=new TextView(this);title.setText("TVGuard local video playback test");title.setTextSize(24);title.setTextColor(-1);root.addView(title);
        setContentView(root);
        video.setVideoURI(Uri.parse("android.resource://"+getPackageName()+"/raw/clip"));
        video.setOnPreparedListener(player->{player.setLooping(true);video.start();Log.i("TVGuardVideoTest","PLAYING");});
    }
    @Override public void onResume(){super.onResume();resumed=true;if(video!=null){video.start();Log.i("TVGuardVideoTest","RESUME");}}
    @Override public void onPause(){resumed=false;if(video!=null)video.pause();Log.i("TVGuardVideoTest","PAUSED");super.onPause();}
}
