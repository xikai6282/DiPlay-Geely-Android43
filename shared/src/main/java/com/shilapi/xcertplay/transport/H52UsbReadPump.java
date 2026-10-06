package com.shilapi.xcertplay.transport;
import android.hardware.usb.*;
import android.os.Build;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.IdentityHashMap;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
/** API18 reads on direct buffers keep blocking USB waits outside Dalvik JNI critical sections. */
public final class H52UsbReadPump {
 private static final IdentityHashMap<UsbDeviceConnection,HashMap<Integer,Pump>> pumps=new IdentityHashMap<>();
 public static int read(UsbDeviceConnection c, UsbEndpoint ep, byte[] target,int offset,int length,int timeout) {
  if(Build.VERSION.SDK_INT>=26) return c.bulkTransfer(ep,target,offset,length,timeout);
  Pump p;
  synchronized(pumps) {
   HashMap<Integer,Pump> endpoints=pumps.get(c);
   if(endpoints==null){endpoints=new HashMap<>();pumps.put(c,endpoints);}
   p=endpoints.get(ep.getAddress());
   if(p==null){p=new Pump(c,ep);endpoints.put(ep.getAddress(),p);p.start();}
  }
  return p.read(target,offset,length,timeout);
 }
 private static final class Pump extends Thread {
  final UsbDeviceConnection connection; final UsbEndpoint endpoint;
  final ArrayBlockingQueue<byte[]> packets=new ArrayBlockingQueue<>(64);
  final String devicePath; volatile boolean ended; volatile RuntimeException failure;
  byte[] remaining; int position;
  Pump(UsbDeviceConnection c,UsbEndpoint ep){
   super("h52-usb-read-"+ep.getAddress());setDaemon(true);connection=c;endpoint=ep;
   String path=null;try{String candidate=new File("/proc/self/fd/"+c.getFileDescriptor()).getCanonicalPath();if(candidate.startsWith("/dev/bus/usb/"))path=candidate;}catch(Exception ignored){}
   devicePath=path;
  }
  void checkDevice(){
   if(connection.getFileDescriptor()<0 || (devicePath!=null&&!new File(devicePath).exists()))throw new IllegalStateException("USB device detached");
   if(failure!=null)throw failure;
   if(ended&&packets.isEmpty()&&remaining==null)throw new IllegalStateException("USB read request ended");
  }
  synchronized int read(byte[] target,int offset,int length,int timeout){
   if(length<=0)return 0;
   checkDevice();
   if(remaining==null){try{remaining=packets.poll(Math.max(1,timeout),TimeUnit.MILLISECONDS);position=0;}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException("USB read interrupted",e);}}
   checkDevice(); if(remaining==null)return -1;
   int count=Math.min(length,remaining.length-position);System.arraycopy(remaining,position,target,offset,count);position+=count;
   if(position==remaining.length){remaining=null;position=0;}return count;
  }
  public void run(){
   UsbRequest request=new UsbRequest();
   try{
    if(!request.initialize(connection,endpoint))throw new IllegalStateException("USB request initialize failed");
    ByteBuffer buffer=ByteBuffer.allocateDirect(16384);
    while(true){
     checkDevice(); buffer.clear();
     if(!request.queue(buffer,buffer.capacity()))throw new IllegalStateException("USB request queue failed");
     UsbRequest complete=connection.requestWait();
     if(complete!=request)throw new IllegalStateException("USB device detached or request replaced");
     int count=buffer.position(); if(count<=0){checkDevice();continue;}
     byte[] data=new byte[count];buffer.flip();buffer.get(data);while(!packets.offer(data,100,TimeUnit.MILLISECONDS))checkDevice();
    }
   }catch(InterruptedException e){Thread.currentThread().interrupt();failure=new IllegalStateException("USB pump interrupted",e);}
   catch(RuntimeException e){failure=e;}
   finally{ended=true;request.cancel();request.close();synchronized(pumps){HashMap<Integer,Pump> endpoints=pumps.get(connection);if(endpoints!=null&&endpoints.get(endpoint.getAddress())==this){endpoints.remove(endpoint.getAddress());if(endpoints.isEmpty())pumps.remove(connection);}}}
  }
 }
}
