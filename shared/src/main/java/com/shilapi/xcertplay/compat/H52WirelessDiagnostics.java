package com.shilapi.xcertplay.compat;
import android.util.Log;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
public final class H52WirelessDiagnostics {
 private static final AtomicInteger rx=new AtomicInteger(),tx=new AtomicInteger();
 public static int channel(int observed,int configured) {
  if(observed>0)return observed;
  final AtomicReference<Process> process=new AtomicReference<Process>();
  FutureTask<Integer> task=new FutureTask<Integer>(new Callable<Integer>() {public Integer call() throws Exception {
   Process p=new ProcessBuilder("su","-c","cat /data/misc/wifi/hostapd.conf").start();process.set(p);
   try {BufferedReader r=new BufferedReader(new InputStreamReader(p.getInputStream(),"UTF-8"));
    for(int n=0;n<64;n++){String line=r.readLine();if(line==null)break;if(line.startsWith("channel=")){int c=Integer.parseInt(line.substring(8).trim());return c>0&&c<=196?c:0;}}
    return 0;
   }finally{p.destroy();}
  }});
  Thread t=new Thread(task,"diplay-ap-channel");t.setDaemon(true);t.start();
  try{int c=task.get(1500,TimeUnit.MILLISECONDS);if(c>0){Log.i("DiPlay-H52-Wireless","active AP channel="+c+" source=hostapd");return c;}}
  catch(Exception e){task.cancel(true);}finally{Process p=process.get();if(p!=null)p.destroy();}
  return configured;
 }
 public static void packet(DatagramPacket p,boolean incoming){
  int n=(incoming?rx:tx).incrementAndGet();if(n>12)return;
  byte[] b=p.getData();int o=p.getOffset(),len=p.getLength();
  int flags=len>=4?((b[o+2]&255)<<8)|(b[o+3]&255):0;
  Log.i("DiPlay-H52-mDNS",(incoming?"RX":"TX")+" n="+n+" bytes="+len+" response="+((flags&32768)!=0)+" multicast="+p.getAddress().isMulticastAddress()+" family="+(p.getAddress() instanceof Inet4Address?"IPv4":"IPv6"));
 }
}
