package com.shenyaocn.android.Encoder;

import android.media.MediaCodec;
import android.media.MediaCrypto;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.view.Surface;
import com.zdplayer.fpvplayer.fpvlibrary.utils.BusinessUtils;
import java.io.IOException;
import java.nio.ByteBuffer;

public class AvcEncoder {
   private static final String MIME_TYPE = "video/avc";
   private boolean isYuv420p = true;
   private long lastEncodedVideoTimeStamp = 0;
   private MediaCodec mEncoderVideo;
   private MediaMuxer mMuxer;
   private boolean mMuxerStarted = false;
   private MediaCodec.BufferInfo mVideoBufferInfo;
   private int mVideoTrackIndex = -1;
   private String recordFileName;
   private long startWhenVideo;
   private byte[] yuvTempBuffer;

   public synchronized boolean open(String fileName, int width, int height) {
      this.mMuxerStarted = false;
      try {
         prepareVideoEncoder(width, height);
         this.recordFileName = fileName;
         this.mMuxer = new MediaMuxer(fileName, 0);
      } catch (Exception e) {
         this.mMuxer = null;
         this.mEncoderVideo = null;
         return false;
      }
      return true;
   }

   public String getRecordFileName() {
      return this.recordFileName;
   }

   private void prepareVideoEncoder(int width, int height) throws IOException {
      this.startWhenVideo = 0;
      this.mVideoBufferInfo = new MediaCodec.BufferInfo();
      MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, width, height);
      format.setInteger("color-format", 19);
      format.setInteger("bitrate", 5000000);
      format.setInteger("frame-rate", 30);
      format.setInteger("i-frame-interval", 5);
      MediaCodec createEncoderByType = MediaCodec.createEncoderByType(MIME_TYPE);
      this.mEncoderVideo = createEncoderByType;
      boolean bConf = false;
      try {
         Surface surface = null;
         MediaCrypto mediaCrypto = null;
         createEncoderByType.configure(format, (Surface) null, (MediaCrypto) null, 1);
         bConf = true;
      } catch (Exception e) {
      }
      if (!bConf) {
         format.setInteger("color-format", 21);
         Surface surface2 = null;
         MediaCrypto mediaCrypto2 = null;
         this.mEncoderVideo.configure(format, (Surface) null, (MediaCrypto) null, 1);
         this.isYuv420p = false;
      }
      this.mEncoderVideo.start();
   }

   public synchronized void close() {
      drainEncoder(true);
      releaseEncoder();
      this.mMuxerStarted = false;
   }

   public synchronized void putFrame(byte[] pixelNv21, int width, int height) {
      if (this.mEncoderVideo != null) {
         if (this.isYuv420p) {
            byte[] bArr = this.yuvTempBuffer;
            if (bArr == null || bArr.length != pixelNv21.length) {
               this.yuvTempBuffer = new byte[pixelNv21.length];
            }
            BusinessUtils.NV21toI420(pixelNv21, this.yuvTempBuffer, width, height);
            VideoEncode(this.yuvTempBuffer, System.currentTimeMillis(), false);
         } else {
            byte[] bArr2 = this.yuvTempBuffer;
            if (bArr2 == null || bArr2.length != pixelNv21.length) {
               this.yuvTempBuffer = new byte[pixelNv21.length];
            }
            BusinessUtils.NV21toNV12(pixelNv21, this.yuvTempBuffer, width, height);
            VideoEncode(this.yuvTempBuffer, System.currentTimeMillis(), false);
         }
         drainEncoder(false);
      }
   }

   public synchronized void putFrame420(byte[] pixels420, int width, int height) {
      byte[] pixels4202 = swapYV12toI420(pixels420, width, height);
      if (this.mEncoderVideo != null) {
         if (this.isYuv420p) {
            byte[] bArr = this.yuvTempBuffer;
            if (bArr == null || bArr.length != pixels4202.length) {
               this.yuvTempBuffer = new byte[pixels4202.length];
            }
            BusinessUtils.I420toYV12(pixels4202, this.yuvTempBuffer, width, height);
            VideoEncode(this.yuvTempBuffer, System.currentTimeMillis(), false);
         } else {
            byte[] bArr2 = this.yuvTempBuffer;
            if (bArr2 == null || bArr2.length != pixels4202.length) {
               this.yuvTempBuffer = new byte[pixels4202.length];
            }
            BusinessUtils.I420toNV12(pixels4202, this.yuvTempBuffer, width, height);
            VideoEncode(this.yuvTempBuffer, System.currentTimeMillis(), false);
         }
         drainEncoder(false);
      }
   }

   public byte[] swapYV12toI420(byte[] yv12bytes, int width, int height) {
      byte[] i420bytes = new byte[yv12bytes.length];
      for (int i = 0; i < width * height; i++) {
         i420bytes[i] = yv12bytes[i];
      }
      for (int i2 = width * height; i2 < (width * height) + (((width / 2) * height) / 2); i2++) {
         i420bytes[i2] = yv12bytes[(((width / 2) * height) / 2) + i2];
      }
      for (int i3 = (width * height) + (((width / 2) * height) / 2); i3 < (width * height) + ((((width / 2) * height) / 2) * 2); i3++) {
         i420bytes[i3] = yv12bytes[i3 - (((width / 2) * height) / 2)];
      }
      return i420bytes;
   }

   public boolean isOpened() {
      return this.mMuxer != null;
   }

   private void releaseEncoder() {
      MediaCodec mediaCodec = this.mEncoderVideo;
      if (mediaCodec != null) {
         mediaCodec.stop();
         this.mEncoderVideo.release();
         this.mEncoderVideo = null;
      }
      MediaMuxer mediaMuxer = this.mMuxer;
      if (mediaMuxer != null) {
         mediaMuxer.stop();
         this.mMuxer.release();
         this.mMuxer = null;
      }
   }

   private void VideoEncode(byte[] pixelsNv21, long presentationTimeMs, boolean endOfStream) {
      long duration;
      byte[] bArr = pixelsNv21;
      MediaCodec mediaCodec = this.mEncoderVideo;
      if (mediaCodec != null) {
         try {
            ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();
            int inputBufferIndex = this.mEncoderVideo.dequeueInputBuffer(10000);
            if (inputBufferIndex >= 0) {
               ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
               inputBuffer.clear();
               inputBuffer.put(pixelsNv21);
               long j = this.startWhenVideo;
               if (j == 0) {
                  this.startWhenVideo = System.currentTimeMillis();
                  duration = 0;
               } else {
                  duration = (presentationTimeMs - j) * 1001;
               }
               if (endOfStream) {
                  this.mEncoderVideo.queueInputBuffer(inputBufferIndex, 0, bArr.length, duration, 4);
                  return;
               }
               this.mEncoderVideo.queueInputBuffer(inputBufferIndex, 0, bArr.length, duration, 0);
            }
         } catch (Exception e) {
         }
      }
   }

   private void drainEncoder(boolean endOfStream) {
      if (this.mMuxer != null && this.mEncoderVideo != null) {
         if (endOfStream) {
            VideoEncode(new byte[0], System.currentTimeMillis(), true);
         }
         MediaCodec.BufferInfo bufferInfo = this.mVideoBufferInfo;
         try {
            ByteBuffer[] encoderOutputBuffers = this.mEncoderVideo.getOutputBuffers();
            while (true) {
               int encoderStatus = this.mEncoderVideo.dequeueOutputBuffer(bufferInfo, 1000);
               if (encoderStatus == -1) {
                  if (!endOfStream) {
                     return;
                  }
               } else if (encoderStatus == -3) {
                  encoderOutputBuffers = this.mEncoderVideo.getOutputBuffers();
               } else if (encoderStatus == -2) {
                  if (!this.mMuxerStarted) {
                     this.mVideoTrackIndex = this.mMuxer.addTrack(this.mEncoderVideo.getOutputFormat());
                     this.mMuxer.start();
                     this.lastEncodedVideoTimeStamp = 0;
                     this.mMuxerStarted = true;
                  } else {
                     throw new RuntimeException("format changed twice");
                  }
               } else if (encoderStatus < 0) {
                  continue;
               } else {
                  ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                  if (encodedData != null) {
                     if ((bufferInfo.flags & 2) != 0) {
                        bufferInfo.size = 0;
                     }
                     if (bufferInfo.size != 0) {
                        if (this.mMuxerStarted) {
                           encodedData.position(bufferInfo.offset);
                           encodedData.limit(bufferInfo.offset + bufferInfo.size);
                           long j = bufferInfo.presentationTimeUs;
                           long j2 = this.lastEncodedVideoTimeStamp;
                           if (j < j2) {
                              long j3 = j2 + 100000;
                              this.lastEncodedVideoTimeStamp = j3;
                              bufferInfo.presentationTimeUs = j3;
                           }
                           this.lastEncodedVideoTimeStamp = bufferInfo.presentationTimeUs;
                           if (bufferInfo.presentationTimeUs < 0) {
                              bufferInfo.presentationTimeUs = 0;
                           }
                           this.mMuxer.writeSampleData(this.mVideoTrackIndex, encodedData, bufferInfo);
                        } else {
                           throw new RuntimeException("muxer hasn't started");
                        }
                     }
                     this.mEncoderVideo.releaseOutputBuffer(encoderStatus, false);
                     if ((bufferInfo.flags & 4) != 0) {
                        return;
                     }
                  } else {
                     throw new RuntimeException("encoderOutputBuffer " + encoderStatus + " was null");
                  }
               }
            }
         } catch (Exception e) {
         }
      }
   }
}