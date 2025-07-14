package com.zdplayer.fpvplayer.fpvlibrary.video;

import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Environment;
import android.text.TextUtils;
import com.shenyaocn.android.Encoder.AvcEncoder;
import com.shenyaocn.android.OpenH264.ByteArrayOutputStream;
import com.shenyaocn.android.OpenH264.CircularByteBuffer;
import com.shenyaocn.android.OpenH264.Decoder;
import com.zdplayer.fpvplayer.fpvlibrary.bean.Frame;
import com.zdplayer.fpvplayer.fpvlibrary.utils.BusinessUtils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.jetbrains.annotations.Nullable;

public class FPVVideoClient {
   private final ExecutorService executor = Executors.newSingleThreadExecutor();
   private final CircularByteBuffer circularByteBuffer = new CircularByteBuffer(524288);
   private final byte[] H264Header = new byte[]{0, 0, 0, 1};
   private OutputStream snapshotOutStream = null;
   private String snapshotFileName = "";
   private int picWidth = 0;
   private int picHeight = 0;
   private volatile boolean mRun = false;
   private AvcEncoder avWriter = new AvcEncoder();
   private RendererThread rendererThread;
   private final Queue<Frame> frameQueue = new LinkedList();
   private final Object surfaceLock = new Object();
   private boolean isPlaying = false;
   private Decoder swDecoder;
   private Delegate delegate;
   private Runnable runnable = new Runnable() {
      private final byte[] buffer = new byte[2048];
      private final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

      public void run() {
         while(FPVVideoClient.this.circularByteBuffer.available() > this.buffer.length) {
            int count = FPVVideoClient.this.circularByteBuffer.get(this.buffer);
            if (count <= 4) {
               return;
            }

            try {
               this.byteArrayOutputStream.write(this.buffer, 0, count);
               byte[] buffer = this.byteArrayOutputStream.getBuf();
               int bufferCount = this.byteArrayOutputStream.getCount();

               int end;
               byte[] h264s;
               for(int start = BusinessUtils.Find(buffer, bufferCount, 0, FPVVideoClient.this.H264Header); start != -1; start = end) {
                  end = BusinessUtils.Find(buffer, bufferCount, start + FPVVideoClient.this.H264Header.length, FPVVideoClient.this.H264Header);
                  if (end != -1) {
                     h264s = Arrays.copyOfRange(buffer, start, end);
                     FPVVideoClient.this.decodeH264(h264s, h264s.length);
                  }
               }

               end = BusinessUtils.FindR(buffer, bufferCount, FPVVideoClient.this.H264Header);
               h264s = this.byteArrayOutputStream.toByteArray();
               this.byteArrayOutputStream.reset();
               this.byteArrayOutputStream.write(h264s, end, h264s.length - end);
            } catch (Exception var7) {
            }
         }

      }
   };

   public void setDelegate(Delegate delegate) {
      this.delegate = delegate;
   }

   public boolean startRecord(@Nullable String folder, @Nullable String fileName) {
      if (!this.isPlaying) {
         return false;
      } else if (this.avWriter.isOpened()) {
         return true;
      } else {
         String tempFolder = folder;
         if (TextUtils.isEmpty(folder)) {
            tempFolder = Environment.getExternalStorageDirectory().getAbsolutePath() + "/FUAV/video/";
         }

         String tempFileName = fileName;
         if (TextUtils.isEmpty(fileName)) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd.HH.mm.ss", Locale.US);
            tempFileName = "/TDVR_" + format.format(new Date()) + ".mp4";
         }

         String name = tempFolder + tempFileName;
         return this.avWriter.open(name, this.picWidth, this.picHeight);
      }
   }

   public void stopRecord() {
      if (this.avWriter.isOpened()) {
         this.avWriter.close();
         if (this.delegate != null) {
            this.delegate.onStopRecordListener(this.avWriter.getRecordFileName());
         }

      }
   }

   public boolean isRecording() {
      return this.avWriter.isOpened();
   }

   public void captureSnapshot(@Nullable String folder, @Nullable String fileName) {
      if (this.isPlaying) {
         String tempFolder = folder;
         if (TextUtils.isEmpty(folder)) {
            tempFolder = Environment.getExternalStorageDirectory().getAbsolutePath() + "/FUAV/video/";
         }

         String tempFileName = fileName;
         if (TextUtils.isEmpty(fileName)) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd.HH.mm.ss", Locale.US);
            tempFileName = "IPC_" + format.format(new Date()) + ".jpg";
         }

         File path = new File(tempFolder);
         if (!path.exists()) {
            path.mkdirs();
         }

         this.snapshotFileName = tempFolder + tempFileName;

         try {
            this.snapshotOutStream = new FileOutputStream(this.snapshotFileName);
         } catch (Exception var7) {
            var7.printStackTrace();
         }

      }
   }

   public void received(byte[] bytes, int size) {
      if (size > 0) {
         this.circularByteBuffer.put(bytes, 0, size);

         try {
            if (this.circularByteBuffer.available() > 2048) {
               this.executor.execute(this.runnable);
            }
         } catch (Exception var4) {
         }
      }

   }

   public void received(byte[] buffer, int off, int size) {
      this.circularByteBuffer.put(buffer, off, size);

      try {
         if (this.circularByteBuffer.available() > 2048) {
            this.executor.execute(this.runnable);
         }
      } catch (Exception var5) {
      }

   }

   private void decodeH264(byte[] h264, int length) {
      if (this.swDecoder == null) {
         this.swDecoder = new Decoder();
      }

      if (!this.swDecoder.hasCreated()) {
         this.swDecoder.create();
      }

      if (this.swDecoder.decodeI420(h264, length)) {
         int width = this.swDecoder.getFrameWidth();
         int height = this.swDecoder.getFrameHeight();
         byte[] i420 = new byte[width * height * 3 / 2];
         this.swDecoder.getFrameI420(i420);
         this.onI420FrameReceived(i420, width, height, false);
      }

   }

   private void onI420FrameReceived(byte[] i420, int width, int height, boolean hwDecoder) {
      if (this.picWidth != width || this.picHeight != height) {
         this.picWidth = width;
         this.picHeight = height;
         synchronized(this.surfaceLock) {
            if (this.delegate != null) {
               this.delegate.setVideoSize(this.picWidth, this.picHeight);
            }
         }
      }

      if (this.snapshotOutStream != null) {
         try {
            byte[] nv21 = new byte[width * height * 3 / 2];
            BusinessUtils.I420toNV21(i420, nv21, width, height);
            (new YuvImage(nv21, 17, width, height, (int[])null)).compressToJpeg(new Rect(0, 0, width, height), 90, this.snapshotOutStream);
            this.snapshotOutStream.flush();
            this.snapshotOutStream.close();
            if (this.delegate != null) {
               this.delegate.onSnapshotListener(this.snapshotFileName);
            }
         } catch (Exception var14) {
            var14.printStackTrace();
         } finally {
            this.snapshotOutStream = null;
         }
      }

      Frame frame = new Frame();
      System.arraycopy(i420, 0, i420, 0, i420.length);
      frame.frame = i420;
      frame.width = width;
      frame.height = height;
      synchronized(this.frameQueue) {
         this.frameQueue.offer(frame);
         this.frameQueue.notify();
      }
   }

   public void startPlayback() {
      this.isPlaying = true;
      this.mRun = true;
      this.picWidth = 0;
      this.picHeight = 0;
      synchronized(this.surfaceLock) {
         if (this.delegate != null) {
            this.delegate.resetView();
         }
      }

      this.rendererThread = new RendererThread();
      this.rendererThread.start();
   }

   public void stopPlayback() {
      this.isPlaying = false;
      this.mRun = false;
      if (this.rendererThread != null) {
         this.rendererThread.interrupt();
         synchronized(this.frameQueue) {
            this.frameQueue.notify();
         }

         try {
            this.rendererThread.join();
         } catch (InterruptedException var3) {
         }

         this.rendererThread = null;
      }

      this.frameQueue.clear();
      if (this.isRecording()) {
         this.stopRecord();
      }

      if (this.swDecoder != null) {
         this.swDecoder.destroy();
      }

   }

   public void setRun(boolean run) {
      this.mRun = run;
   }

   public boolean isPlaying() {
      return this.isPlaying;
   }

   private class RendererThread extends Thread {
      private RendererThread() {
      }

      public void run() {
         byte queueLen = 3;

         while(true) {
            try {
               if (!this.isInterrupted() && FPVVideoClient.this.mRun) {
                  long sTime = System.currentTimeMillis();
                  Frame frame;
                  synchronized(FPVVideoClient.this.frameQueue) {
                     while(FPVVideoClient.this.frameQueue.size() == 0) {
                        FPVVideoClient.this.frameQueue.wait();
                        if (this.isInterrupted() || !FPVVideoClient.this.mRun) {
                           return;
                        }
                     }

                     frame = (Frame)FPVVideoClient.this.frameQueue.poll();
                  }

                  if (frame != null) {
                     synchronized(FPVVideoClient.this.surfaceLock) {
                        if (FPVVideoClient.this.delegate != null) {
                           FPVVideoClient.this.delegate.renderI420(frame.frame, frame.width, frame.height);
                        }
                     }

                     if (FPVVideoClient.this.avWriter.isOpened()) {
                        FPVVideoClient.this.avWriter.putFrame420(frame.frame, frame.width, frame.height);
                     }
                  }

                  int size = FPVVideoClient.this.frameQueue.size();
                  byte fps;
                  if (size > queueLen) {
                     fps = 35;
                  } else if (size < queueLen) {
                     fps = 15;
                  } else {
                     fps = 25;
                  }

                  long dTime = System.currentTimeMillis() - sTime;
                  long delay = (long)(1000 / fps) - dTime;
                  if (delay > 0L) {
                     Thread.sleep(delay);
                  }
                  continue;
               }
            } catch (InterruptedException var13) {
            }

            return;
         }
      }

      // $FF: synthetic method
      RendererThread(Object x1) {
         this();
      }
   }

   public interface Delegate {
      void onStopRecordListener(String var1);

      void onSnapshotListener(String var1);

      void renderI420(byte[] var1, int var2, int var3);

      void setVideoSize(int var1, int var2);

      void resetView();
   }
}
