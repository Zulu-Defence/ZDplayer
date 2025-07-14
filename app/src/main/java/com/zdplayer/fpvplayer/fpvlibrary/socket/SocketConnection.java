package com.zdplayer.fpvplayer.fpvlibrary.socket;

import android.os.Process;
import android.util.Log;
import com.zdplayer.fpvplayer.fpvlibrary.SDKInit;
import com.zdplayer.fpvplayer.fpvlibrary.utils.String2ByteArrayUtils;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.concurrent.LinkedBlockingQueue;

public class SocketConnection {
   private DatagramSocket datagramSocket;
   private Delegate delegate;
   private int readSize = 1024;
   private int localPort;
   private int serverPort;
   private InetAddress serverAddress;
   private ConnectThread mConnectThread;
   private Thread mForwardThread;
   private volatile boolean connect;
   private final LinkedBlockingQueue<byte[]> mPacketsToSend = new LinkedBlockingQueue();
   private final Runnable mSendingTask = new Runnable() {
      public void run() {
         while(true) {
            try {
               if (SocketConnection.this.connect) {
                  byte[] buffer = (byte[])SocketConnection.this.mPacketsToSend.take();
                  DatagramPacket packet = new DatagramPacket(new byte[buffer.length], buffer.length);
                  if (SocketConnection.this.datagramSocket != null && buffer != null) {
                     packet.setData(buffer);
                     SocketConnection.this.datagramSocket.send(packet);
                  }

                  if (SDKInit.getInstance().isDebug()) {
                     Log.d(SDKInit.getInstance().getTAG(), "MessageQueue=> hex: " + String2ByteArrayUtils.encodeHexStr(buffer));
                  }
                  continue;
               }
            } catch (IOException | InterruptedException var6) {
               var6.printStackTrace();
            } finally {
               SocketConnection.this.closeConnection();
            }

            return;
         }
      }
   };

   public void setDelegate(Delegate delegate) {
      this.delegate = delegate;
   }

   public SocketConnection(int localPort, InetAddress serverAddress, int serverPort) {
      this.localPort = localPort;
      this.serverAddress = serverAddress;
      this.serverPort = serverPort;
   }

   public void sendData(byte[] bytes) {
      if (bytes != null && this.connect) {
         if (!this.mPacketsToSend.offer(bytes) && SDKInit.getInstance().isDebug()) {
            Log.d(SDKInit.getInstance().getTAG(), "Unable to send mavlink packet. Packet queue is full!");
         }

      }
   }

   public void openConnection() {
      this.mConnectThread = new ConnectThread();
      this.mConnectThread.start();
      this.connect = true;
      this.mForwardThread = new Thread(this.mSendingTask);
      this.mForwardThread.start();
   }

   public void closeConnection() {
      this.connect = false;
      this.delegate = null;
      if (this.mConnectThread != null) {
         this.mConnectThread.isInterrupted();
         this.mConnectThread = null;
      }

      if (this.mForwardThread != null) {
         this.mForwardThread.interrupt();
         this.mForwardThread = null;
      }

      if (this.mPacketsToSend != null) {
         this.mPacketsToSend.clear();
      }

      if (this.datagramSocket != null) {
         this.datagramSocket.close();
         this.datagramSocket.disconnect();
         this.datagramSocket = null;
      }

   }

   private void received(byte[] bytes, int size) {
      if (this.delegate != null) {
         this.delegate.received(bytes, size);
      }

   }

   class ConnectThread extends Thread {
      public void run() {
         Process.setThreadPriority(-19);
         byte[] bytes = new byte[SocketConnection.this.readSize];
         DatagramPacket packet = new DatagramPacket(bytes, bytes.length);

         try {
            SocketConnection.this.datagramSocket = new DatagramSocket(SocketConnection.this.localPort);
            SocketConnection.this.datagramSocket.connect(SocketConnection.this.serverAddress, SocketConnection.this.serverPort);
         } catch (SocketException var4) {
            var4.printStackTrace();
         }

         if (SocketConnection.this.datagramSocket != null) {
            SocketConnection.this.connect = true;
            if (SocketConnection.this.delegate != null) {
               SocketConnection.this.delegate.connect();
            }

            while(!this.isInterrupted()) {
               try {
                  if (SocketConnection.this.datagramSocket == null) {
                     break;
                  }

                  SocketConnection.this.datagramSocket.receive(packet);
                  if (SocketConnection.this.delegate != null) {
                     SocketConnection.this.delegate.received(packet.getData(), packet.getLength());
                  }
               } catch (IOException var5) {
                  this.interrupt();
                  var5.printStackTrace();
               }
            }

         }
      }
   }

   public interface Delegate {
      void received(byte[] var1, int var2);

      void connect();
   }
}
