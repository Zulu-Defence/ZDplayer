package com.zdplayer.fpvplayer.fpvlibrary.serial;

import android.serialport.SerialPort;
import android.util.Log;
import com.zdplayer.fpvplayer.fpvlibrary.SDKInit;
import com.zdplayer.fpvplayer.fpvlibrary.utils.String2ByteArrayUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.LinkedBlockingQueue;

public class SerialPortConnection {
   private SerialPort mSerialPort;
   private int baudrate;
   private String path;
   private int flags;
   private int parity;
   private int dataBits;
   private int stopBits;
   private int flowCon;
   private int fifoSize;
   private int readSize;
   private InputStream inputStream;
   private OutputStream outputStream;
   private volatile boolean connect;
   private ReadThread mReadThread;
   private Thread mForwardThread;
   private final LinkedBlockingQueue<byte[]> mPacketsToSend;
   private final Runnable mSendingTask;
   private Delegate delegate;

   private SerialPortConnection(String path, int baudrate, int dataBits, int parity, int stopBits, int flowCon, int fifoSize, int readSize, int flags) {
      this.readSize = 2048;
      this.mPacketsToSend = new LinkedBlockingQueue();
      this.mSendingTask = new Runnable() {
         public void run() {
            while(true) {
               try {
                  if (SerialPortConnection.this.connect) {
                     byte[] buffer = (byte[])SerialPortConnection.this.mPacketsToSend.take();
                     OutputStream outputStream = SerialPortConnection.this.outputStream;
                     if (outputStream == null || buffer == null) {
                        continue;
                     }

                     try {
                        outputStream.write(buffer);
                     } catch (IOException var13) {
                        var13.printStackTrace();
                     }

                     if (SDKInit.getInstance().isDebug()) {
                        Log.d(SDKInit.getInstance().getTAG(), "MessageQueue=> hex: " + String2ByteArrayUtils.encodeHexStr(buffer));
                     }
                     continue;
                  }
               } catch (InterruptedException var14) {
                  var14.printStackTrace();
               } finally {
                  try {
                     SerialPortConnection.this.closeConnection();
                  } catch (IOException var12) {
                     var12.printStackTrace();
                  }

               }

               return;
            }
         }
      };
      this.path = path;
      this.baudrate = baudrate;
      this.dataBits = dataBits;
      this.stopBits = stopBits;
      this.parity = parity;
      this.flags = flags;
      this.flowCon = flowCon;
      this.fifoSize = fifoSize;
      this.readSize = readSize;
   }

   public void openConnection() throws Exception {
      SerialPort serialPort = SerialPort.newBuilder(this.path, this.baudrate).flags(this.flags).parity(this.parity).dataBits(this.dataBits).stopBits(this.stopBits).flowCon(this.flowCon).fifoSize(this.fifoSize).build();
      this.mSerialPort = serialPort;
      InputStream inputStream = serialPort.getInputStream();
      OutputStream outputStream = serialPort.getOutputStream();
      this.inputStream = inputStream;
      this.outputStream = outputStream;
      this.connect = true;
      this.mForwardThread = new Thread(this.mSendingTask);
      this.mForwardThread.start();
      if (inputStream != null) {
         this.mReadThread = new ReadThread();
         this.mReadThread.start();
      }

      Delegate delegate = this.delegate;
      if (delegate != null) {
         delegate.connect();
      }

   }

   public void closeConnection() throws IOException {
      this.connect = false;
      this.delegate = null;
      this.mPacketsToSend.clear();
      Thread forwardThread = this.mForwardThread;
      if (forwardThread != null && forwardThread.isAlive()) {
         forwardThread.interrupt();
         this.mForwardThread = null;
      }

      Thread readThread = this.mReadThread;
      if (readThread != null) {
         readThread.interrupt();
         this.mReadThread = null;
      }

      OutputStream outputStream = this.outputStream;
      if (outputStream != null) {
         outputStream.close();
         this.outputStream = null;
      }

      InputStream inputStream = this.inputStream;
      if (inputStream != null) {
         inputStream.close();
         this.inputStream = null;
      }

      SerialPort serialPort = this.mSerialPort;
      if (serialPort != null) {
         serialPort.close();
         this.mSerialPort = null;
      }

   }

   public void sendData(byte[] bytes) {
      if (bytes != null && this.connect) {
         if (!this.mPacketsToSend.offer(bytes) && SDKInit.getInstance().isDebug()) {
            Log.d(SDKInit.getInstance().getTAG(), "Unable to send mavlink packet. Packet queue is full!");
         }

      }
   }

   private void received(byte[] bytes, int size) {
      Delegate delegate = this.delegate;
      if (delegate != null) {
         delegate.received(bytes, size);
      }

   }

   public boolean isConnection() {
      return this.connect;
   }

   public void setDelegate(Delegate delegate) {
      this.delegate = delegate;
   }

   public static Builder newBuilder(String devicePath, int baudrate) {
      return new Builder(devicePath, baudrate);
   }

   // $FF: synthetic method
   SerialPortConnection(String x0, int x1, int x2, int x3, int x4, int x5, int x6, int x7, int x8, Object x9) {
      this(x0, x1, x2, x3, x4, x5, x6, x7, x8);
   }

   public static final class Builder {
      private String path;
      private int baudrate;
      private int dataBits;
      private int parity;
      private int stopBits;
      private int flags;
      private int flowCon;
      private int fifoSize;
      private int readSize;

      private Builder(String path, int baudrate) {
         this.dataBits = 8;
         this.parity = 0;
         this.stopBits = 1;
         this.flags = 0;
         this.flowCon = 0;
         this.fifoSize = -1;
         this.readSize = 2048;
         this.path = path;
         this.baudrate = baudrate;
      }

      public Builder dataBits(int dataBits) {
         this.dataBits = dataBits;
         return this;
      }

      public Builder parity(int parity) {
         this.parity = parity;
         return this;
      }

      public Builder stopBits(int stopBits) {
         this.stopBits = stopBits;
         return this;
      }

      public Builder flags(int flags) {
         this.flags = flags;
         return this;
      }

      public Builder flowCon(int flowCon) {
         this.flowCon = flowCon;
         return this;
      }

      public Builder fifoSize(int fifoSize) {
         this.fifoSize = fifoSize;
         return this;
      }

      public Builder readSize(int readSize) {
         this.readSize = readSize;
         return this;
      }

      public SerialPortConnection build() {
         return new SerialPortConnection(this.path, this.baudrate, this.dataBits, this.parity, this.stopBits, this.flowCon, this.fifoSize, this.readSize, this.flags);
      }

      // $FF: synthetic method
      Builder(String x0, int x1, Object x2) {
         this(x0, x1);
      }
   }

   public interface Delegate {
      void received(byte[] var1, int var2);

      void connect();
   }

   class ReadThread extends Thread {
      public void run() {
         while(!this.isInterrupted()) {
            try {
               InputStream inputStream = SerialPortConnection.this.inputStream;
               if (inputStream == null) {
                  return;
               }

               if (inputStream.available() != 0) {
                  byte[] buffer = new byte[SerialPortConnection.this.readSize];
//                  int sizex = false;
                  int size = inputStream.read(buffer);
                  if (size > 0) {
                     byte[] data = new byte[size];
                     System.arraycopy(buffer, 0, data, 0, data.length);
                     SerialPortConnection.this.received(data, size);
                  }
               }
            } catch (IOException var5) {
               var5.printStackTrace();
            }
         }

      }
   }
}
