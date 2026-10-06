package com.shilapi.xcertplay.media;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
public final class H52VideoCapabilities {
 private static Boolean hevc;
 public static synchronized boolean supportsHevc() {
  if(hevc!=null)return hevc;
  boolean found=false;
  try{for(int i=0;i<MediaCodecList.getCodecCount();i++){
   MediaCodecInfo codec=MediaCodecList.getCodecInfoAt(i);
   if(codec.isEncoder())continue;
   for(String type:codec.getSupportedTypes())if("video/hevc".equalsIgnoreCase(type))found=true;
  }}catch(RuntimeException ignored){}
  hevc=found;return found;
 }
}
