package com.zdplayer.fpvplayer.android.usbserial.utils;

import android.util.Log;
import com.zdplayer.fpvplayer.android.usbserial.driver.UsbSerialDriver;
import java.io.IOException;
import java.nio.ByteBuffer;

public class SerialInputOutputManager implements Runnable {
   private static final String TAG = SerialInputOutputManager.class.getSimpleName();
   private static final boolean DEBUG = true;
   private static final int READ_WAIT_MILLIS = 200;
   private static final int BUFSIZ = 4096;
   private final UsbSerialDriver mDriver;
   private final ByteBuffer mReadBuffer;
   private final ByteBuffer mWriteBuffer;
   private State mState;
   private Listener mListener;

   public SerialInputOutputManager(UsbSerialDriver driver) {
      this(driver, (Listener)null);
   }

   public SerialInputOutputManager(UsbSerialDriver driver, Listener listener) {
      this.mReadBuffer = ByteBuffer.allocate(4096);
      this.mWriteBuffer = ByteBuffer.allocate(4096);
      this.mState = State.STOPPED;
      this.mDriver = driver;
      this.mListener = listener;
   }

   public synchronized void setListener(Listener listener) {
      this.mListener = listener;
   }

   public synchronized Listener getListener() {
      return this.mListener;
   }

   public void writeAsync(byte[] data) {
      synchronized(this.mWriteBuffer) {
         this.mWriteBuffer.put(data);
      }
   }

   public synchronized void stop() {
      if (this.getState() == State.RUNNING) {
         Log.i(TAG, "Stop requested");
         this.mState = State.STOPPING;
      }

   }

   private synchronized State getState() {
      return this.mState;
   }

   public void run() {
      synchronized(this) {
         if (this.getState() != State.STOPPED) {
            throw new IllegalStateException("Already running.");
         }

         this.mState = State.RUNNING;
      }

      Log.i(TAG, "Running ..");

      while(true) {
         boolean var14 = false;

         label141: {
            try {
               var14 = true;
               if (this.getState() == State.RUNNING) {
                  this.step();
                  continue;
               }

               Log.i(TAG, "Stopping mState=" + this.getState());
               var14 = false;
            } catch (Exception var18) {
               Log.w(TAG, "Run ending due to exception: " + var18.getMessage(), var18);
               Listener listener = this.getListener();
               if (listener != null) {
                  listener.onRunError(var18);
                  var14 = false;
                  break label141;
               }

               var14 = false;
               break label141;
            } finally {
               if (var14) {
                  synchronized(this) {
                     this.mState = State.STOPPED;
                     Log.i(TAG, "Stopped.");
                  }
               }
            }

            synchronized(this) {
               this.mState = State.STOPPED;
               Log.i(TAG, "Stopped.");
               break;
            }
         }

         synchronized(this) {
            this.mState = State.STOPPED;
            Log.i(TAG, "Stopped.");
            break;
         }
      }

   }

   private void step() throws IOException {
      int len = this.mDriver.read(this.mReadBuffer.array(), 200);
      if (len > 0) {
         Log.d(TAG, "Read data len=" + len);
         Listener listener = this.getListener();
         if (listener != null) {
            byte[] data = new byte[len];
            this.mReadBuffer.get(data, 0, len);
            listener.onNewData(data);
         }

         this.mReadBuffer.clear();
      }

      byte[] outBuff = null;
      synchronized(this.mWriteBuffer) {
         if (this.mWriteBuffer.position() > 0) {
            len = this.mWriteBuffer.position();
            outBuff = new byte[len];
            this.mWriteBuffer.rewind();
            this.mWriteBuffer.get(outBuff, 0, len);
            this.mWriteBuffer.clear();
         }
      }

      if (outBuff != null) {
         Log.d(TAG, "Writing data len=" + len);
         this.mDriver.write(outBuff, 200);
      }

   }

   public interface Listener {
      void onNewData(byte[] var1);

      void onRunError(Exception var1);
   }

   private static enum State {
      STOPPED,
      RUNNING,
      STOPPING;
   }
}
