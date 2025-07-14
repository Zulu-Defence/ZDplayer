package com.zdplayer.fpvplayer.fpvlibrary.utils;

import android.hardware.usb.UsbDevice;

public class BusinessUtils {
   public static final String PACKAGE_NAME = "com.skydroid.fpvlibrary";

   public static void I420toNV21(byte[] i420, byte[] nv21, int width, int height) {
      int frameSize = width * height;
      int qFrameSize = frameSize / 4;
      System.arraycopy(i420, 0, nv21, 0, frameSize);

      for(int i = 0; i < qFrameSize; ++i) {
         nv21[frameSize + i * 2 + 1] = i420[frameSize + i];
         nv21[frameSize + i * 2] = i420[frameSize + qFrameSize + i];
      }

   }

   public static void I420toYV12(byte[] i420, byte[] yv12, int width, int height) {
      int frameSize = width * height;
//      int i = false;
//      int j = false;
      System.arraycopy(i420, 0, yv12, 0, frameSize);

      for(int j = 0; j < frameSize / 4; ++j) {
         yv12[frameSize * 5 / 4 + j] = i420[frameSize + j];
         yv12[frameSize + j] = i420[frameSize * 5 / 4 + j];
      }

   }

   public static void NV21toI420(byte[] nv21, byte[] i420, int width, int height) {
      int frameSize = width * height;
      int qFrameSize = frameSize / 4;
      System.arraycopy(nv21, 0, i420, 0, frameSize);

      for(int i = 0; i < qFrameSize; ++i) {
         i420[frameSize + i] = nv21[frameSize + i * 2 + 1];
         i420[frameSize + qFrameSize + i] = nv21[frameSize + i * 2];
      }

   }

   public static void I420toNV12(byte[] i420, byte[] nv12, int width, int height) {
      int frameSize = width * height;
      int qFrameSize = frameSize / 4;
      System.arraycopy(i420, 0, nv12, 0, frameSize);

      for(int i = 0; i < qFrameSize; ++i) {
         nv12[frameSize + i * 2 + 1] = i420[frameSize + qFrameSize + i];
         nv12[frameSize + i * 2] = i420[frameSize + i];
      }

   }

   public static void NV21toNV12(byte[] nv21, byte[] nv12, int width, int height) {
      int frameSize = width * height;
//      int i = false;
//      int j = false;
      System.arraycopy(nv21, 0, nv12, 0, frameSize);

      for(int j = 0; j < frameSize / 2; j += 2) {
         nv12[frameSize + j + 1] = nv21[frameSize + j];
         nv12[frameSize + j] = nv21[frameSize + j + 1];
      }

   }

   public static int Find(byte[] buff, int length, int begin, byte[] search) {
      for(int start = begin; start < length - search.length; ++start) {
         if (buff[start] == search[0]) {
            int next;
            for(next = 1; next < search.length && buff[start + next] == search[next]; ++next) {
            }

            if (next == search.length) {
               return start;
            }
         }
      }

      return -1;
   }

   public static int FindR(byte[] buff, int length, byte[] search) {
      for(int start = length - search.length - 1; start >= 0; --start) {
         if (buff[start] == search[0]) {
            int next;
            for(next = 1; next < search.length && buff[start + next] == search[next]; ++next) {
            }

            if (next == search.length) {
               return start;
            }
         }
      }

      return -1;
   }

   public static boolean deviceIsUartVideoDevice(UsbDevice device) {
      if (device == null) {
         return false;
      } else {
         return device.getVendorId() == 4292 && device.getProductId() == 60000;
      }
   }

   public static int getLen(byte h, byte l) {
      return ((h & 255) << 8) + (l & 255);
   }

   public static int hasHeader(byte[] headers, byte[] datas) {
      int count = 0;

      for(int i = 0; i < datas.length; ++i) {
         if (datas[i] == headers[count]) {
            if (count == headers.length - 1) {
               return i - count;
            }

            ++count;
         } else {
            count = 0;
         }
      }

      return -1;
   }

   public static int hasHeader(byte[] headers, byte[] datas, int start) {
      int count = 0;

      for(int i = start; i < datas.length; ++i) {
         if (datas[i] == headers[count]) {
            if (count == headers.length - 1) {
               return i - count;
            }

            ++count;
         } else {
            count = 0;
         }
      }

      return -1;
   }
}
