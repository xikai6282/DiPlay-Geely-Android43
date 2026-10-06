package com.shilapi.xcertplay.media;
import android.app.Activity;
import android.os.Build;
import android.util.Log;
import android.view.*;
import android.widget.FrameLayout;
import java.lang.reflect.*;
/** Direct decoder Surface on API18/19; existing gesture and connection layers remain above it. */
public final class H52LegacySurface extends SurfaceView implements SurfaceHolder.Callback {
 private final Activity host; private Surface attached;
 private H52LegacySurface(Activity activity){super(activity);host=activity;getHolder().addCallback(this);}
 public static void install(Activity host,FrameLayout root,TextureView texture){
  if(Build.VERSION.SDK_INT>=20)return;
  texture.setSurfaceTextureListener(null);texture.setAlpha(0f);
  root.addView(new H52LegacySurface(host),0,new FrameLayout.LayoutParams(-1,-1));
 }
 private Field field(String name)throws Exception{Field f=host.getClass().getDeclaredField(name);f.setAccessible(true);return f;}
 private void call(String name,Class<?>[] types,Object...args)throws Exception{Method m=host.getClass().getDeclaredMethod(name,types);m.setAccessible(true);m.invoke(host,args);}
 private void attach(SurfaceHolder holder){try{
  attached=holder.getSurface();field("currentSurface").set(host,attached);field("currentSurfaceTexture").set(host,null);
  call("attachSurface",new Class<?>[]{Surface.class},attached);
  Log.i("DiPlay-H52-Surface","direct Surface attached valid="+attached.isValid());
 }catch(Exception e){Log.e("DiPlay-H52-Surface","direct Surface attach failed",e);}}
 public void surfaceCreated(SurfaceHolder holder){attach(holder);}
 public void surfaceChanged(SurfaceHolder holder,int format,int width,int height){
  attach(holder);try{call("scheduleDisplaySize",new Class<?>[]{int.class,int.class},width,height);}catch(Exception e){Log.e("DiPlay-H52-Surface","size update failed",e);}
 }
 public void surfaceDestroyed(SurfaceHolder holder){try{
  Object sink=field("sink").get(host);
  if(sink!=null){Method m=sink.getClass().getMethod("clearSurface",int.class,Surface.class);m.invoke(sink,110,attached);m.invoke(sink,111,attached);}
  if(field("currentSurface").get(host)==attached)field("currentSurface").set(host,null);
  attached=null;
 }catch(Exception e){Log.w("DiPlay-H52-Surface","Surface teardown failed",e);}}
}
