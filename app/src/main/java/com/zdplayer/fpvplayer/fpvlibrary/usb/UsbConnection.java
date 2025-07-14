package com.zdplayer.fpvplayer.fpvlibrary.usb;

import android.content.Context;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import com.zdplayer.fpvplayer.fpvlibrary.SDKInit;
import com.zdplayer.fpvplayer.fpvlibrary.usb.FiFo;
import com.zdplayer.fpvplayer.fpvlibrary.utils.String2ByteArrayUtils;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.LinkedBlockingQueue;

public class UsbConnection {
   private Context context;
   private boolean connect;
   private int readSize = 2048;
   private InputStream inputStream;
   private OutputStream outputStream;
   private ParcelFileDescriptor mFileDescriptor;
   private ReadThread mReadThread;
   private DealThread mDealThread;
   private VideoThread mVideoThread;
   private Thread mForwardThread;
   private com.zdplayer.fpvplayer.fpvlibrary.usb.FiFo mDataFiFo;
   private com.zdplayer.fpvplayer.fpvlibrary.usb.FiFo mVideoFiFo;
   private Object lock = new Object();
   private final LinkedBlockingQueue<byte[]> mPacketsToSend = new LinkedBlockingQueue();
   private static final int ARLINK_CTRL_DATA = 0;
   private static final int ARLINK_STREAM_DATA = 1;
   private static final int ArlinkRxProtocolHEADER = 0;
   private static final int ArlinkRxProtocolDataLen = 1;
   private static final int ArlinkRxProtocolCheckSum = 2;
   private static final int ArlinkRxProtocolDataBuffer = 3;
   private static final int ARLINK_STREAM_LEN_POS = 14;
   private static final int ARLINK_CTRL_LEN_POS = 8;
   private static final int ARLINK_STREAM_CHECKSUM_POS = 16;
   private static final int ARLINK_CTRL_CHECKSUM_POS = 10;
   private static final int ARLINK_USR_DATA_MAX_LEN = 16384;
   private byte[] buffer = new byte[2048];
   ArlinkRxFiFo arlinkRxFiFo = new ArlinkRxFiFo();
   private int streamHeaderCount = 16;
   private int ctrlHeaderCount = 10;
   private byte[] headerStream = new byte[]{-1, -91, 90, -1};
   private byte[] headerCtrlStream = new byte[]{-1, 90};
   private Delegate delegate;
   private final Runnable mSendingTask = new Runnable() {
      public void run() {
         while(true) {
            try {
               if (UsbConnection.this.connect) {
                  byte[] buffer = (byte[])UsbConnection.this.mPacketsToSend.take();
                  if (UsbConnection.this.outputStream == null || buffer == null) {
                     continue;
                  }

                  try {
                     UsbConnection.this.outputStream.write(buffer);
                  } catch (IOException var12) {
                     var12.printStackTrace();
                  }

                  if (SDKInit.getInstance().isDebug()) {
                     Log.d(SDKInit.getInstance().getTAG(), "MessageQueue=> hex: " + String2ByteArrayUtils.encodeHexStr(buffer));
                  }
                  continue;
               }
            } catch (InterruptedException var13) {
               var13.printStackTrace();
            } finally {
               try {
                  UsbConnection.this.closeConnection();
               } catch (IOException var11) {
                  var11.printStackTrace();
               }

            }

            return;
         }
      }
   };

   public UsbConnection(Context context) {
      this.context = context;
   }

   public void setDelegate(Delegate delegate) {
      this.delegate = delegate;
   }

   public void openConnection(UsbAccessory accessory) throws Exception {
      UsbManager manager = (UsbManager)this.context.getSystemService("usb");
      this.mFileDescriptor = manager.openAccessory(accessory);
      if (this.mFileDescriptor != null && this.mFileDescriptor.getFileDescriptor() != null) {
         this.inputStream = new FileInputStream(this.mFileDescriptor.getFileDescriptor());
         this.outputStream = new FileOutputStream(this.mFileDescriptor.getFileDescriptor());
         this.connect = true;
         this.mForwardThread = new Thread(this.mSendingTask);
         this.mForwardThread.start();
         this.mDataFiFo = new com.zdplayer.fpvplayer.fpvlibrary.usb.FiFo();
         this.mVideoFiFo = new FiFo(1048576);
         if (this.inputStream != null) {
            this.mReadThread = new ReadThread();
            this.mReadThread.start();
            this.mDealThread = new DealThread();
            this.mDealThread.start();
            this.mVideoThread = new VideoThread();
            this.mVideoThread.start();
         }

         if (this.delegate != null) {
            this.delegate.connect();
         }

      } else {
         if (SDKInit.getInstance().isDebug()) {
            Log.d(SDKInit.getInstance().getTAG(), "Open Error！");
         }

      }
   }

   public void closeConnection() throws IOException {
      this.connect = false;
      this.delegate = null;
      if (this.mPacketsToSend != null) {
         this.mPacketsToSend.clear();
      }

      if (this.mReadThread != null) {
         this.mReadThread.interrupt();
         this.mReadThread = null;
      }

      if (this.mDealThread != null) {
         this.mDealThread.interrupt();
         this.mDealThread = null;
      }

      if (this.mVideoThread != null) {
         this.mVideoThread.interrupt();
         this.mVideoThread = null;
      }

      if (this.outputStream != null) {
         this.outputStream.close();
         this.outputStream = null;
      }

      if (this.inputStream != null) {
         this.inputStream.close();
         this.inputStream = null;
      }

      if (this.mFileDescriptor != null) {
         this.mFileDescriptor.close();
         this.mFileDescriptor = null;
      }

   }

   public void sendData(byte[] bytes) {
      if (bytes != null && this.connect) {
         if (!this.mPacketsToSend.offer(bytes) && SDKInit.getInstance().isDebug()) {
            Log.d(SDKInit.getInstance().getTAG(), "Unable to send mavlink packet. Packet queue is full!");
         }

      }
   }

   private void arlinkRxPacketDataAnalyze(byte[] rxData, int length) {
      ArlinkRxFiFoHeader arlinkFiFoHeader = this.arlinkRxFiFo.headerIns;
      int i = 0;
//      byte byteData = false;
      boolean var6 = false;

      while(true) {
         int j;
         label47:
         do {
            while(true) {
               while(length > 0) {
                  byte byteData = rxData[i++];
                  --length;
                  if (arlinkFiFoHeader.findHeader == 1) {
                     if (arlinkFiFoHeader.dataLen <= 16384 && arlinkFiFoHeader.dataLen != 0) {
                        if (arlinkFiFoHeader.dataLen - arlinkFiFoHeader.dataBufferIndex > length + 1) {
                           --i;
                           System.arraycopy(rxData, i, this.arlinkRxFiFo.data, arlinkFiFoHeader.dataBufferIndex, length + 1);
                           arlinkFiFoHeader.dataBufferIndex += length + 1;
                           i += length + 1;
                           length = 0;
                        } else {
                           --i;
                           j = arlinkFiFoHeader.dataLen - arlinkFiFoHeader.dataBufferIndex;
                           System.arraycopy(rxData, i, this.arlinkRxFiFo.data, arlinkFiFoHeader.dataBufferIndex, j);
                           arlinkFiFoHeader.dataBufferIndex += j;
                           i += j;
                           length -= j - 1;
                        }
                        continue label47;
                     }

                     arlinkFiFoHeader.dataBufferIndex = 0;
                     arlinkFiFoHeader.revingData = 0;
                     arlinkFiFoHeader.findHeader = 0;
                  } else {
                     this.arlinkProtocolFindHeader(byteData);
                  }
               }

               return;
            }
         } while(arlinkFiFoHeader.dataBufferIndex != arlinkFiFoHeader.dataLen);

         if (arlinkFiFoHeader.arlinkPacketType != 0) {
            if (arlinkFiFoHeader.arlinkPacketType == 1) {
               this.mVideoFiFo.write(this.arlinkRxFiFo.data, arlinkFiFoHeader.dataLen);
            }
         } else {
            int checkSum = 0;

            for(j = 0; j < arlinkFiFoHeader.dataLen; ++j) {
               checkSum += this.arlinkRxFiFo.data[j] & 255;
               checkSum &= 65535;
            }

            if (checkSum == arlinkFiFoHeader.checkSum) {
               byte[] userDataArray = new byte[arlinkFiFoHeader.dataLen + 10];
               System.arraycopy(this.arlinkRxFiFo.headerIns.headerBuffer, 0, userDataArray, 0, 10);
               System.arraycopy(this.arlinkRxFiFo.data, 0, userDataArray, 10, arlinkFiFoHeader.dataLen);
               if (this.delegate != null) {
                  this.delegate.ctrlData(userDataArray, userDataArray.length);
               }
            }
         }

         arlinkFiFoHeader.dataBufferIndex = 0;
         arlinkFiFoHeader.revingData = 0;
         arlinkFiFoHeader.findHeader = 0;
      }
   }

   private int arlinkProtocolFindHeader(byte data) {
      int retValue = 0;
      int checkSum;
      int i;
      switch(this.arlinkRxFiFo.headerIns.rxState) {
      case 0:
         if (this.arlinkRxFiFo.headerIns.headerBufferIndex >= this.headerStream.length) {
            this.arlinkRxFiFo.headerIns.headerBufferIndex = 0;
         }

         this.arlinkRxFiFo.headerIns.headerBuffer[this.arlinkRxFiFo.headerIns.headerBufferIndex++] = data;
         boolean isEqual = this.ArraysEqual(this.arlinkRxFiFo.headerIns.headerBuffer, this.headerStream, this.arlinkRxFiFo.headerIns.headerBufferIndex);
         if (!isEqual && this.arlinkRxFiFo.headerIns.headerBufferIndex <= this.headerCtrlStream.length) {
            isEqual = this.ArraysEqual(this.arlinkRxFiFo.headerIns.headerBuffer, this.headerCtrlStream, this.arlinkRxFiFo.headerIns.headerBufferIndex);
         }

         if (!isEqual) {
            for(i = 1; i < this.arlinkRxFiFo.headerIns.headerBufferIndex; ++i) {
               this.arlinkRxFiFo.headerIns.headerBuffer[i - 1] = this.arlinkRxFiFo.headerIns.headerBuffer[i];
            }

            --this.arlinkRxFiFo.headerIns.headerBufferIndex;
            return retValue;
         }

         if (this.arlinkRxFiFo.headerIns.headerBufferIndex == this.headerStream.length && this.ArraysEqual(this.arlinkRxFiFo.headerIns.headerBuffer, this.headerStream, this.arlinkRxFiFo.headerIns.headerBufferIndex)) {
            this.arlinkRxFiFo.headerIns.rxState = 1;
            this.arlinkRxFiFo.headerIns.dataLenIndex = 0;
            this.arlinkRxFiFo.headerIns.arlinkPacketType = 1;
         } else if (this.arlinkRxFiFo.headerIns.headerBufferIndex == this.headerCtrlStream.length && this.ArraysEqual(this.arlinkRxFiFo.headerIns.headerBuffer, this.headerCtrlStream, this.arlinkRxFiFo.headerIns.headerBufferIndex)) {
            this.arlinkRxFiFo.headerIns.rxState = 1;
            this.arlinkRxFiFo.headerIns.dataLenIndex = 0;
            this.arlinkRxFiFo.headerIns.arlinkPacketType = 0;
         }
         break;
      case 1:
         this.arlinkRxFiFo.headerIns.headerBuffer[this.arlinkRxFiFo.headerIns.headerBufferIndex++] = data;
         if (this.arlinkRxFiFo.headerIns.arlinkPacketType == 0) {
            if (this.arlinkRxFiFo.headerIns.headerBufferIndex == 8) {
               this.arlinkRxFiFo.headerIns.rxState = 2;
               this.arlinkRxFiFo.headerIns.dataLen = ((this.arlinkRxFiFo.headerIns.headerBuffer[this.arlinkRxFiFo.headerIns.headerBufferIndex - 1] & 255) << 8) + (this.arlinkRxFiFo.headerIns.headerBuffer[this.arlinkRxFiFo.headerIns.headerBufferIndex - 2] & 255);
               i = this.arlinkRxFiFo.headerIns.dataLen;
               checkSum = this.arlinkRxFiFo.headerIns.headerBuffer[this.arlinkRxFiFo.headerIns.headerBufferIndex - 3] & 255;
               i = this.arlinkRxFiFo.headerIns.headerBuffer[this.arlinkRxFiFo.headerIns.headerBufferIndex - 4] & 255;
               if (i > 600 || checkSum > 1 || i > 1) {
                  Log.e("ArtosynApp", "###################### dataLength > 600 || Cur > 1 || Sum > 1 ???????????????????");
                  this.arlinkRxFiFo.headerIns.rxState = 0;
                  this.arlinkRxFiFo.headerIns.headerBufferIndex = 0;
               }
            }
         } else if (this.arlinkRxFiFo.headerIns.arlinkPacketType == 1) {
            if (this.arlinkRxFiFo.headerIns.headerBufferIndex == 14) {
               this.arlinkRxFiFo.headerIns.rxState = 2;
               this.arlinkRxFiFo.headerIns.dataLen = ((this.arlinkRxFiFo.headerIns.headerBuffer[this.arlinkRxFiFo.headerIns.headerBufferIndex - 1] & 255) << 8) + (this.arlinkRxFiFo.headerIns.headerBuffer[this.arlinkRxFiFo.headerIns.headerBufferIndex - 2] & 255);
            }
         } else {
            Log.e("ArtosynApp", "ERROR in ArlinkRxProtocolDataLen !!!!!");
         }
         break;
      case 2:
         this.arlinkRxFiFo.headerIns.headerBuffer[this.arlinkRxFiFo.headerIns.headerBufferIndex++] = data;
         if (this.arlinkRxFiFo.headerIns.arlinkPacketType == 0) {
            if (this.arlinkRxFiFo.headerIns.headerBufferIndex == 10) {
               this.arlinkRxFiFo.headerIns.checkSum = ((this.arlinkRxFiFo.headerIns.headerBuffer[this.arlinkRxFiFo.headerIns.headerBufferIndex - 1] & 255) << 8) + (this.arlinkRxFiFo.headerIns.headerBuffer[this.arlinkRxFiFo.headerIns.headerBufferIndex - 2] & 255);
               retValue = 1;
               this.arlinkRxFiFo.headerIns.findHeader = 1;
               this.arlinkRxFiFo.headerIns.dataBufferIndex = 0;
               this.arlinkRxFiFo.headerIns.rxState = 0;
               this.arlinkRxFiFo.headerIns.headerBufferIndex = 0;
            }
         } else if (this.arlinkRxFiFo.headerIns.arlinkPacketType == 1 && this.arlinkRxFiFo.headerIns.headerBufferIndex == 16) {
            this.arlinkRxFiFo.headerIns.checkSum = ((this.arlinkRxFiFo.headerIns.headerBuffer[this.arlinkRxFiFo.headerIns.headerBufferIndex - 1] & 255) << 8) + (this.arlinkRxFiFo.headerIns.headerBuffer[this.arlinkRxFiFo.headerIns.headerBufferIndex - 2] & 255);
            byte[] packData = this.arlinkRxFiFo.headerIns.headerBuffer;
            checkSum = 0;

            for(i = 0; i < packData.length - 2; ++i) {
               checkSum += packData[i] & 255;
               checkSum &= 65535;
            }

            if (checkSum == this.arlinkRxFiFo.headerIns.checkSum) {
               retValue = 1;
               this.arlinkRxFiFo.headerIns.findHeader = 1;
               this.arlinkRxFiFo.headerIns.dataBufferIndex = 0;
            } else {
               Log.e("ArtosynApp", "ERROR in ArlinkRxProtocolCheckSum 0 !!!!!");
            }

            this.arlinkRxFiFo.headerIns.rxState = 0;
            this.arlinkRxFiFo.headerIns.headerBufferIndex = 0;
         }
         break;
      default:
         Log.e("ArtosynApp", "Default run in Data Transfer !!!!!");
         this.arlinkRxFiFo.headerIns.rxState = 0;
         this.arlinkRxFiFo.headerIns.headerBufferIndex = 0;
      }

      return retValue;
   }

   private boolean ArraysEqual(byte[] array1, byte[] array2, int length) {
//      int i = false;

      int i;
      for(i = 0; i < length && array1[i] == array2[i]; ++i) {
      }

      return i == length;
   }

   class VideoThread extends Thread {
      public void run() {
         super.run();
         byte[] buf = new byte[1024];

         while(!this.isInterrupted()) {
            int readBuffer = UsbConnection.this.mVideoFiFo.read(buf, buf.length);
            if (UsbConnection.this.delegate != null) {
               UsbConnection.this.delegate.streamData(buf, readBuffer);
            }
         }

      }
   }

   class ArlinkRxFiFo {
      ArlinkRxFiFoHeader headerIns = UsbConnection.this.new ArlinkRxFiFoHeader();
      byte[] data = new byte[131072];
   }

   class DealThread extends Thread {
      public void run() {
         super.run();

         while(!this.isInterrupted()) {
            int readBuffer = UsbConnection.this.mDataFiFo.read(UsbConnection.this.buffer, 2048);
            UsbConnection.this.arlinkRxPacketDataAnalyze(UsbConnection.this.buffer, readBuffer);
         }

      }
   }

   class ReadThread extends Thread {
      public void run() {
         byte[] buffer = new byte[UsbConnection.this.readSize];
         boolean var2 = false;

         while(!this.isInterrupted()) {
            try {
               if (UsbConnection.this.inputStream == null) {
                  return;
               }

               int size = UsbConnection.this.inputStream.read(buffer);
               if (size > 0) {
                  if (UsbConnection.this.delegate != null) {
                     UsbConnection.this.delegate.received(buffer, size);
                  }

                  UsbConnection.this.mDataFiFo.write(buffer, size);
               } else {
                  try {
                     Thread.sleep(5L);
                  } catch (InterruptedException var4) {
                     this.interrupt();
                  }
               }
            } catch (IOException var5) {
               var5.printStackTrace();
            }
         }

      }
   }

   class ArlinkRxFiFoHeader {
      int findHeader = 0;
      int revingData = 0;
      int dataLenIndex = 0;
      int headerBufferIndex = 0;
      int rxState = 0;
      byte[] headerBuffer = new byte[16];
      int arlinkPacketType = 0;
      int dataLen = 0;
      int dataBufferIndex = 0;
      int checkSum = 0;
   }

   public interface Delegate {
      void received(byte[] var1, int var2);

      void streamData(byte[] var1, int var2);

      void ctrlData(byte[] var1, int var2);

      void connect();
   }
}
