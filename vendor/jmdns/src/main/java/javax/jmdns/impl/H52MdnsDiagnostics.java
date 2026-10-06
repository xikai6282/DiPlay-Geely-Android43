package javax.jmdns.impl;
import java.net.*;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;
public final class H52MdnsDiagnostics {
 private static final AtomicInteger rx=new AtomicInteger(),tx=new AtomicInteger();
 private static Method log;
 static {try{log=Class.forName("android.util.Log").getMethod("i",String.class,String.class);}catch(Exception e){}}
 public static void packet(DatagramPacket p,boolean incoming){
  int n=(incoming?rx:tx).incrementAndGet();if(n>12||log==null)return;
  byte[] b=p.getData();int o=p.getOffset(),len=p.getLength();
  int flags=len>=4?((b[o+2]&255)<<8)|(b[o+3]&255):0;
  try{log.invoke(null,"DiPlay-H52-mDNS",(incoming?"RX":"TX")+" n="+n+" bytes="+len+" response="+((flags&32768)!=0)+" multicast="+p.getAddress().isMulticastAddress()+" family="+(p.getAddress() instanceof Inet4Address?"IPv4":"IPv6"));}catch(Exception e){}
 }
}
