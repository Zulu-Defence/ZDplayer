package com.shenyaocn.android.OpenH264;

import java.nio.ByteBuffer;

public class Decoder {
   private long id = 0;

   private native long createDecoder();

   private native boolean decodeFrameI420(long j, byte[] bArr, int i);

   private native void destroyDecoder(long j);

   private native int getDecodedFrameHeight(long j);

   private native void getDecodedFrameI420(long j, byte[] bArr);

   private native int getDecodedFrameWidth(long j);

   public static final native void nativeI420BuffertoI420(ByteBuffer byteBuffer, int i, int i2, byte[] bArr, int i3, int i4);

   public static final native void nativeNV12BuffertoI420(ByteBuffer byteBuffer, int i, int i2, byte[] bArr, int i3, int i4);

   public boolean hasCreated() {
      return this.id != 0;
   }

   public void create() {
      this.id = createDecoder();
   }

   public void finalize() {
      destroy();
   }

   public synchronized void destroy() {
      long j = this.id;
      if (j != 0) {
         destroyDecoder(j);
      }
      this.id = 0;
   }

   public synchronized int getFrameWidth() {
      long j = this.id;
      if (j == 0) {
         return 0;
      }
      return getDecodedFrameWidth(j);
   }

   public synchronized int getFrameHeight() {
      long j = this.id;
      if (j == 0) {
         return 0;
      }
      return getDecodedFrameHeight(j);
   }

   public synchronized boolean decodeI420(byte[] h264, int h264Length) {
      long j = this.id;
      if (j == 0) {
         return false;
      }
      return decodeFrameI420(j, h264, h264Length);
   }

   public synchronized void getFrameI420(byte[] i420) {
      long j = this.id;
      if (j != 0) {
         getDecodedFrameI420(j, i420);
      }
   }

   static {
      System.loadLibrary("openh264");
      System.loadLibrary("openh264jni");
   }
}