from pathlib import Path
import subprocess,zipfile,os
r=Path(__file__).resolve().parent/'fixture';sdk=Path(os.environ.get('ANDROID_HOME',str(Path.home()/'Library/Android/sdk')));bt=sdk/'build-tools/34.0.0';platform=sdk/'platforms/android-34/android.jar';java=Path(os.environ['JAVA_HOME'])/'bin'
def run(*args):subprocess.run([str(x) for x in args],check=True)
run(bt/'aapt2','compile','--dir',r/'res','-o',r/'resources.zip')
run(java/'javac','--release','8','-cp',platform,'-d',r/'classes',r/'Player.java',r/'Observer.java',r/'Controller.java')
run(bt/'d8','--min-api','26','--lib',platform,'--output',r/'dex',*[str(p) for p in (r/'classes').rglob('*.class')])
for name,pkg in [('managed','com.tvguard.fixture'),('unmanaged','com.tvguard.unmanagedfixture')]:
 manifest=f'''<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="{pkg}">
 <uses-sdk android:minSdkVersion="26" android:targetSdkVersion="32"/>
 <uses-permission android:name="android.permission.WRITE_SECURE_SETTINGS"/>
 <application android:debuggable="true" android:label="TVGuard {name} video QA" android:theme="@android:style/Theme.Material.NoActionBar">
 <activity android:name="com.tvguard.fixture.Player" android:exported="true"><intent-filter><action android:name="android.intent.action.MAIN"/><category android:name="android.intent.category.LEANBACK_LAUNCHER"/></intent-filter></activity>
 <service android:name="com.tvguard.fixture.Observer" android:label="TVGuard QA Observer" android:exported="true" android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"><intent-filter><action android:name="android.accessibilityservice.AccessibilityService"/></intent-filter><meta-data android:name="android.accessibilityservice" android:resource="@xml/observer"/></service>
 <receiver android:name="com.tvguard.fixture.Controller" android:exported="true"/>
 </application></manifest>'''
 p=r/(name+'.xml');p.write_text(manifest)
 run(bt/'aapt2','link','-I',platform,'--manifest',p,'-o',r/(name+'-base.apk'),r/'resources.zip')
 with zipfile.ZipFile(r/(name+'-base.apk'),'a') as z:
  for member in (r/'dex').glob('*.dex'):z.write(member,member.name)
 run(bt/'zipalign','-f','4',r/(name+'-base.apk'),r/(name+'-aligned.apk'))
 run(bt/'apksigner','sign','--ks',Path.home()/'.android/debug.keystore','--ks-key-alias','androiddebugkey','--ks-pass','pass:android','--out',r.parent/(name+'.apk'),r/(name+'-aligned.apk'))
