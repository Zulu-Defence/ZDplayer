package com.zdplayer.fpvplayer.fpvlibrary.widget;

import android.opengl.GLES20;

import com.zdplayer.fpvplayer.fpvlibrary.widget.GLProgram;
import com.zdplayer.fpvplayer.fpvlibrary.widget.GLTextureView;

import java.nio.ByteBuffer;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class GLHttpVideoRenderer implements com.zdplayer.fpvplayer.fpvlibrary.widget.GLTextureView.Renderer {
   private com.zdplayer.fpvplayer.fpvlibrary.widget.GLTextureView mTargetSurface;
   private com.zdplayer.fpvplayer.fpvlibrary.widget.GLProgram prog = new com.zdplayer.fpvplayer.fpvlibrary.widget.GLProgram(0);
   private int mVideoWidth;
   private int mVideoHeight;
   private ByteBuffer y;
   private ByteBuffer u;
   private ByteBuffer v;
   private int mScreenWidth;
   private int mScreenHeight;
   private int mOldScreenWidth;
   private int mOldScreenHeight;

   public GLHttpVideoRenderer(GLTextureView surface) {
      this.mTargetSurface = surface;
   }

   public void onSurfaceCreated(GL10 gl, EGLConfig config) {
      if (!this.prog.isProgramBuilt()) {
         this.prog.buildProgram();
      }

   }

   public void onSurfaceChanged(GL10 gl, int width, int height) {
      GLES20.glViewport(0, 0, width, height);
      this.mScreenWidth = width;
      this.mScreenHeight = height;
      this.prog.frustumM(width, height);
   }

   public boolean onDrawFrame(GL10 gl) {
      synchronized(this) {
         if (this.y != null) {
            this.y.position(0);
            this.u.position(0);
            this.v.position(0);
            this.prog.buildTextures(this.y, this.u, this.v, this.mVideoWidth, this.mVideoHeight);
            GLES20.glClearColor(0.0F, 0.0F, 0.0F, 1.0F);
            GLES20.glClear(16384);
            this.prog.drawFrame();
         }

         return true;
      }
   }

   public void onSurfaceDestroyed() {
   }

   public void setScale(float scaleFactor) {
      this.prog.scaleFactor = scaleFactor;
   }

   public void setPosition(float x, float y) {
      this.prog.moveFactorX = x;
      this.prog.moveFactorY = y;
   }

   public void update(int w, int h) {
      if (w > 0 && h > 0) {
         if (this.mScreenWidth > 0 && this.mScreenHeight > 0 && (this.mScreenWidth != this.mOldScreenWidth || this.mScreenHeight != this.mOldScreenHeight)) {
            this.mOldScreenWidth = this.mScreenWidth;
            this.mOldScreenHeight = this.mScreenHeight;
            float f1 = 1.0F * (float)this.mScreenHeight / (float)this.mScreenWidth;
            float f2 = 1.0F * (float)h / (float)w;
            if (f1 == f2) {
               this.prog.createBuffers(GLProgram.squareVertices);
            } else {
               float widScale;
               if (f1 < f2) {
                  widScale = f1 / f2;
                  this.prog.createBuffers(new float[]{-widScale, -1.0F, widScale, -1.0F, -widScale, 1.0F, widScale, 1.0F});
               } else {
                  widScale = f2 / f1;
                  this.prog.createBuffers(new float[]{-1.0F, -widScale, 1.0F, -widScale, -1.0F, widScale, 1.0F, widScale});
               }
            }
         }

         if (w != this.mVideoWidth || h != this.mVideoHeight) {
            this.mVideoWidth = w;
            this.mVideoHeight = h;
            int yarraySize = w * h;
            int uvarraySize = yarraySize / 4;
            synchronized(this) {
               this.y = ByteBuffer.allocate(yarraySize);
               this.u = ByteBuffer.allocate(uvarraySize);
               this.v = ByteBuffer.allocate(uvarraySize);
            }
         }
      }

   }

   public void update(byte[] yuv) {
      synchronized(this) {
         int frameSize = yuv.length * 2 / 3;
         this.y.clear();
         this.u.clear();
         this.v.clear();
         this.y.put(yuv, 0, frameSize);
         this.u.put(yuv, frameSize, frameSize / 4);
         this.v.put(yuv, frameSize * 5 / 4, frameSize / 4);
      }

      this.mTargetSurface.requestRender();
   }
}
