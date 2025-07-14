package com.zdplayer.fpvplayer.android.usbserial.driver;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;

import com.zdplayer.fpvplayer.android.usbserial.driver.UsbSerialDriver;

import java.io.IOException;

abstract class CommonUsbSerialDriver implements UsbSerialDriver {
   public static final int DEFAULT_READ_BUFFER_SIZE = 16384;
   public static final int DEFAULT_WRITE_BUFFER_SIZE = 16384;
   protected final UsbDevice mDevice;
   protected final UsbDeviceConnection mConnection;
   protected final Object mReadBufferLock = new Object();
   protected final Object mWriteBufferLock = new Object();
   protected byte[] mReadBuffer;
   protected byte[] mWriteBuffer;

   public CommonUsbSerialDriver(UsbDevice device, UsbDeviceConnection connection) {
      this.mDevice = device;
      this.mConnection = connection;
      this.mReadBuffer = new byte[16384];
      this.mWriteBuffer = new byte[16384];
   }

   public final UsbDevice getDevice() {
      return this.mDevice;
   }

   public final void setReadBufferSize(int bufferSize) {
      synchronized(this.mReadBufferLock) {
         if (bufferSize != this.mReadBuffer.length) {
            this.mReadBuffer = new byte[bufferSize];
         }
      }
   }

   public final void setWriteBufferSize(int bufferSize) {
      synchronized(this.mWriteBufferLock) {
         if (bufferSize != this.mWriteBuffer.length) {
            this.mWriteBuffer = new byte[bufferSize];
         }
      }
   }

   public abstract void open() throws IOException;

   public abstract void close() throws IOException;

   public abstract int read(byte[] var1, int var2) throws IOException;

   public abstract int write(byte[] var1, int var2) throws IOException;

   public abstract void setParameters(int var1, int var2, int var3, int var4) throws IOException;

   public abstract boolean getCD() throws IOException;

   public abstract boolean getCTS() throws IOException;

   public abstract boolean getDSR() throws IOException;

   public abstract boolean getDTR() throws IOException;

   public abstract void setDTR(boolean var1) throws IOException;

   public abstract boolean getRI() throws IOException;

   public abstract boolean getRTS() throws IOException;

   public abstract void setRTS(boolean var1) throws IOException;
}
