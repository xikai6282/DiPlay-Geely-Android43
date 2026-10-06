package com.shilapi.xcertplay.media;
import android.bluetooth.*;
import android.content.Context;
import android.hardware.usb.*;
import android.os.Build;
import android.util.Log;
import java.util.*;
/** Temporarily release only the connected phone's A2DP profile during wired CarPlay. */
public final class H52WiredAudioGuard implements BluetoothProfile.ServiceListener {
 private static H52WiredAudioGuard active;
 private final Context context;private final BluetoothAdapter adapter;
 private BluetoothProfile profile;private BluetoothDevice released;private Timer timer;private boolean closed;
 private H52WiredAudioGuard(Context c){context=c.getApplicationContext();adapter=BluetoothAdapter.getDefaultAdapter();}
 public static synchronized void start(Context c){
  if(Build.VERSION.SDK_INT>=20||active!=null||!applePresent(c))return;
  H52WiredAudioGuard g=new H52WiredAudioGuard(c);if(g.adapter==null)return;
  active=g;
  if(!g.adapter.getProfileProxy(g.context,g,BluetoothProfile.A2DP)){active=null;Log.w("DiPlay-H52-Audio","A2DP proxy unavailable");}
 }
 private static boolean applePresent(Context c){UsbManager m=(UsbManager)c.getSystemService(Context.USB_SERVICE);if(m==null)return false;for(UsbDevice d:m.getDeviceList().values())if(d.getVendorId()==0x05ac)return true;return false;}
 public synchronized void onServiceConnected(int kind,BluetoothProfile p){
  if(closed){adapter.closeProfileProxy(kind,p);return;}
  profile=p;timer=new Timer("h52-wired-audio",true);
  timer.schedule(new TimerTask(){public void run(){tick();}},0,1000);
 }
 public synchronized void onServiceDisconnected(int kind){profile=null;}
 private synchronized void tick(){
  if(closed)return;
  if(!applePresent(context)){finish();return;}
  if(profile==null)return;
  try{
   List<BluetoothDevice> peers=profile.getConnectedDevices();
   BluetoothDevice phone=null;
   if(released!=null){for(BluetoothDevice d:peers)if(d.getAddress().equals(released.getAddress()))phone=d;}
   else{for(BluetoothDevice d:peers){BluetoothClass type=d.getBluetoothClass();if(type!=null&&type.getMajorDeviceClass()==BluetoothClass.Device.Major.PHONE){if(phone!=null){Log.w("DiPlay-H52-Audio","Multiple phones; refusing A2DP selection");return;}phone=d;}}}
   if(phone!=null){Object result=profile.getClass().getMethod("disconnect",BluetoothDevice.class).invoke(profile,phone);if(Boolean.TRUE.equals(result)){released=phone;Log.i("DiPlay-H52-Audio","wired A2DP phone disconnected; HFP retained");}else Log.w("DiPlay-H52-Audio","A2DP disconnect not accepted");}
  }catch(Exception e){Log.w("DiPlay-H52-Audio","A2DP guard failed",e);}
 }
 private synchronized void finish(){
  if(closed)return;closed=true;if(timer!=null)timer.cancel();
  if(profile!=null){if(released!=null)try{Object result=profile.getClass().getMethod("connect",BluetoothDevice.class).invoke(profile,released);Log.i("DiPlay-H52-Audio","USB gone: A2DP restoration requested="+result);}catch(Exception e){Log.w("DiPlay-H52-Audio","A2DP restore failed",e);}adapter.closeProfileProxy(BluetoothProfile.A2DP,profile);}
  synchronized(H52WiredAudioGuard.class){if(active==this)active=null;}
 }
}
