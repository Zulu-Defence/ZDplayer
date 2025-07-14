package com.zdplayer.fpvplayer.fpvlibrary.usbserial;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Process;
import android.util.Log;
import com.shenyaocn.android.OpenH264.CircularByteBuffer;
import com.zdplayer.fpvplayer.android.usbserial.driver.UsbSerialDriver;
import com.zdplayer.fpvplayer.android.usbserial.driver.UsbSerialProber;
import com.zdplayer.fpvplayer.fpvlibrary.SDKInit;
import com.zdplayer.fpvplayer.fpvlibrary.utils.String2ByteArrayUtils;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

public class UsbSerialConnection {
   private UsbSerialDriver usbSerialDriver;
   private Context context;
   private boolean connect;
   private byte[] buffer = new byte[2048];
   private Queue<byte[]> payloadQueue = new LinkedList();
   private ReadThread mReadThread;
   private Delegate delegate;

   public UsbSerialConnection(Context context) {
      this.context = context;
   }

   public void openConnection(UsbDevice device) throws Exception {
      UsbManager manager = (UsbManager)this.context.getSystemService("usb");
      UsbSerialDriver serialDriver = UsbSerialProber.openUsbDevice(manager, device);
      int baudRate = 4000000;
      serialDriver.open();
      serialDriver.setParameters(baudRate, 8, 1, 0);
      this.usbSerialDriver = serialDriver;
      this.mReadThread = new ReadThread();
      this.mReadThread.start();
      this.connect = true;
   }

   public void closeConnection() throws Exception {
      this.connect = false;
      this.delegate = null;
      if (this.usbSerialDriver != null) {
         this.usbSerialDriver.close();
         this.usbSerialDriver = null;
      }

      if (this.mReadThread != null) {
         this.mReadThread.interrupt();
         this.mReadThread = null;
      }

   }

   public boolean isConnection() {
      return this.connect;
   }

   public void putPayload(byte type, byte[] payload) {
      CircularByteBuffer circularByteBuffer = new CircularByteBuffer(Math.max(payload.length, 256));
      circularByteBuffer.put(payload);
      int maxPayload = 250;
      byte[] buff = new byte[maxPayload + 1];

      while(true) {
         int length = circularByteBuffer.get(buff, 1, maxPayload);
         if (length <= 0) {
            return;
         }

         buff[0] = type;
         synchronized(this.payloadQueue) {
            this.payloadQueue.offer(Arrays.copyOf(buff, length + 1));
         }
      }
   }

   public void setDelegate(Delegate delegate) {
      this.delegate = delegate;
   }

   public interface Delegate {
      void onH264Received(byte[] var1, int var2);

      void onGPSReceived(byte[] var1);

      void onDataReceived(byte[] var1);

      void onDebugReceived(byte[] var1);
   }

   class ReadThread extends Thread {
      private byte[] req = new byte[5];

      public void run() {
         Process.setThreadPriority(-19);

         try {
            while(!this.isInterrupted() && UsbSerialConnection.this.connect) {
               this.readUartData();
            }
         } catch (Exception var2) {
            var2.printStackTrace();
            UsbSerialConnection.this.connect = false;
         }

      }

      private void readUartData() throws Exception {
         int packSize = 512;
         int timeout = 20;
         if (UsbSerialConnection.this.usbSerialDriver != null) {
            this.req[0] = -1;
            this.req[1] = (byte)(packSize >> 8);
            this.req[2] = (byte)packSize;
            this.req[3] = 85;
            this.req[4] = 0;
            byte[] payload;
            synchronized(UsbSerialConnection.this.payloadQueue) {
               payload = (byte[])UsbSerialConnection.this.payloadQueue.poll();
            }

            byte[] buff;
            if (payload == null) {
               buff = new byte[this.req.length + 1];
               System.arraycopy(this.req, 0, buff, 0, this.req.length);
               buff[5] = -91;
               UsbSerialConnection.this.usbSerialDriver.write(buff, 1000);
               if (SDKInit.getInstance().isDebug()) {
                  Log.d(SDKInit.getInstance().getTAG(), "MessageQueue=> hex: " + String2ByteArrayUtils.encodeHexStr(buff));
               }
            } else {
               this.req[4] = (byte)(payload.length - 1);
               buff = new byte[this.req.length + payload.length];
               System.arraycopy(this.req, 0, buff, 0, this.req.length);
               System.arraycopy(payload, 0, buff, this.req.length, payload.length);
               UsbSerialConnection.this.usbSerialDriver.write(buff, 1000);
               if (SDKInit.getInstance().isDebug()) {
                  Log.d(SDKInit.getInstance().getTAG(), "MessageQueue=> hex: " + String2ByteArrayUtils.encodeHexStr(buff));
               }
            }

            long start = System.currentTimeMillis();
            int count = UsbSerialConnection.this.usbSerialDriver.read(UsbSerialConnection.this.buffer, timeout);
            if (count != packSize || UsbSerialConnection.this.buffer[0] != -1) {
               long delay = System.currentTimeMillis() - start;
               if (delay < (long)timeout) {
                  try {
                     Thread.sleep((long)timeout - delay);
                  } catch (Exception var11) {
                  }
               }

               if (count == -1) {
                  return;
               }

               byte[] tempArray = new byte[UsbSerialConnection.this.buffer.length];
               System.arraycopy(UsbSerialConnection.this.buffer, 0, tempArray, 0, count);
               int c = UsbSerialConnection.this.usbSerialDriver.read(UsbSerialConnection.this.buffer, 40);
               if (c + count != packSize) {
                  return;
               }

               System.arraycopy(UsbSerialConnection.this.buffer, 0, tempArray, count, c);
               System.arraycopy(tempArray, 0, UsbSerialConnection.this.buffer, 0, c + count);
            }

            int paySize = (UsbSerialConnection.this.buffer[1] & 255) << 8 | UsbSerialConnection.this.buffer[2] & 255;
            if (paySize > 508) {
               paySize = 508;
            }

            if (UsbSerialConnection.this.delegate != null) {
               if (UsbSerialConnection.this.buffer[3] == -91) {
                  UsbSerialConnection.this.delegate.onH264Received(UsbSerialConnection.this.buffer, paySize);
               } else {
                  byte[] recv = new byte[paySize];
                  System.arraycopy(UsbSerialConnection.this.buffer, 4, recv, 0, paySize);
                  switch(UsbSerialConnection.this.buffer[3]) {
                  case -95:
                     UsbSerialConnection.this.delegate.onDebugReceived(recv);
                     break;
                  case -93:
                     UsbSerialConnection.this.delegate.onDataReceived(recv);
                     break;
                  case -89:
                     UsbSerialConnection.this.delegate.onGPSReceived(recv);
                  }
               }
            }
         }

      }
   }
}
