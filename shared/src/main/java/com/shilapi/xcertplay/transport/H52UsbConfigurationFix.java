package com.shilapi.xcertplay.transport;

import android.hardware.usb.*;
import android.util.Log;
import java.util.*;
import com.shilapi.xcertplay.compat.LegacyUsbNative;

/** API18 flattens every configuration into UsbDevice.interfaces, in descriptor order. */
public final class H52UsbConfigurationFix {
    static int u(byte[] b, int i) { return b[i] & 255; }
    public static UsbDeviceLayout read(UsbDevice device, byte[] raw, Integer active) {
        int offset=0, config=0, flat=0, best=0, score=-1;
        Map<Integer,List<UsbInterfaceView>> groups=new LinkedHashMap<>();
        Map<Integer,Integer> scores=new HashMap<>();
        while(offset+2<=raw.length) {
            int len=u(raw,offset), type=u(raw,offset+1);
            if(len<2 || offset+len>raw.length) throw new IllegalArgumentException("Invalid USB descriptor");
            if(type==2 && len>=9) { config=u(raw,offset+5); groups.put(config,new ArrayList<UsbInterfaceView>()); scores.put(config,0); }
            if(type==4 && len>=9) {
                if(flat>=device.getInterfaceCount()) throw new IllegalArgumentException("USB interface map incomplete");
                UsbInterface p=device.getInterface(flat++);
                int id=u(raw,offset+2), alt=u(raw,offset+3), cls=u(raw,offset+5), sub=u(raw,offset+6), proto=u(raw,offset+7);
                if(p.getId()!=id || p.getInterfaceClass()!=cls || p.getInterfaceSubclass()!=sub || p.getInterfaceProtocol()!=proto || p.getEndpointCount()!=u(raw,offset+4))
                    throw new IllegalArgumentException("USB flattened interface mismatch");
                groups.get(config).add(new UsbInterfaceView(p,alt));
                int s=scores.get(config);
                if(cls==255 && sub==254 && proto==2) s|=4;
                if(cls==2 && sub==13) s|=2;
                if(cls==255 && sub==253 && proto==1) s|=1;
                scores.put(config,s);
            }
            offset+=len;
        }
        if(flat!=device.getInterfaceCount()) throw new IllegalArgumentException("USB descriptor/interface count mismatch");
        for(Integer id:groups.keySet()) {
            int s=scores.get(id);
            if((s&6)==6 && s>score) { best=id; score=s; }
        }
        if(best==0) best=active!=null && groups.containsKey(active)?active:(groups.isEmpty()?0:groups.keySet().iterator().next());
        Log.i("xcertplay-usb","H52 isolated configuration target="+best+" active="+active+" flattened="+flat);
        return new UsbDeviceLayout(best,groups.containsKey(best)?groups.get(best):Collections.<UsbInterfaceView>emptyList());
    }

    private static String readText(java.io.File path) throws java.io.IOException {
        java.io.BufferedReader reader=new java.io.BufferedReader(new java.io.FileReader(path));
        try { String line=reader.readLine(); return line==null?"":line.trim(); } finally { reader.close(); }
    }
    public static boolean select(UsbDevice device, UsbDeviceConnection connection, int target) {
        byte[] current=new byte[1];
        if(connection.controlTransfer(128,8,0,0,current,1,2000)!=1) throw new IllegalStateException("Cannot confirm active Apple USB configuration");
        int active=u(current,0);
        if(active==target) return true;
        int result=LegacyUsbNative.INSTANCE.configure(connection.getFileDescriptor(),target);
        if(result==-16) {
            if(device.getVendorId()!=0x05ac || target<1 || target>255) throw new IllegalStateException("Refusing non-Apple configuration transition");
            try {
                java.io.File[] devices=new java.io.File("/sys/bus/usb/devices").listFiles();
                java.io.File selected=null;
                if(devices!=null) for(java.io.File path:devices) {
                    if(!path.getName().matches("[0-9]+-[0-9.]+")) continue;
                    if(!"05ac".equals(readText(new java.io.File(path,"idVendor")))) continue;
                    int bus=Integer.parseInt(readText(new java.io.File(path,"busnum")));
                    int dev=Integer.parseInt(readText(new java.io.File(path,"devnum")));
                    String name=String.format(java.util.Locale.US,"/dev/bus/usb/%03d/%03d",bus,dev);
                    if(!name.equals(device.getDeviceName())) continue;
                    if(selected!=null) throw new IllegalStateException("Ambiguous Apple USB path");
                    selected=new java.io.File(path,"bConfigurationValue");
                }
                if(selected==null) throw new IllegalStateException("Apple USB sysfs path unavailable");
                Process process=new ProcessBuilder("su","-c","echo "+target+" > "+selected.getPath()).redirectErrorStream(true).start();
                long deadline=System.nanoTime()+5000000000L;
                int exit=-1;
                while(System.nanoTime()<deadline) {
                    try { exit=process.exitValue(); break; } catch(IllegalThreadStateException pending) { Thread.sleep(25); }
                }
                process.destroy();
                if(exit!=0) throw new IllegalStateException("H52 USB configuration root helper failed: "+exit);
                result=0;
                Log.i("xcertplay-usb","H52 Apple-only sysfs configuration transition target="+target+" completed");
            } catch(Exception e) { throw new IllegalStateException("H52 USB configuration transition unavailable",e); }
        }
        Arrays.fill(current,(byte)0);
        int n=connection.controlTransfer(128,8,0,0,current,1,2000);
        Log.i("xcertplay-usb","H52 configuration transition target="+target+" result="+result+" actual="+(n==1?u(current,0):-1));
        if(result!=0 || n!=1 || u(current,0)!=target) throw new IllegalStateException("H52 CarPlay configuration transition failed: "+result);
        return true;
    }
}
