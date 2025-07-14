package com.zdplayer.fpvplayer.fpvlibrary.widget;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLDebugHelper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;

import com.zdplayer.fpvplayer.fpvlibrary.widget.SystemProperties;

import java.io.Writer;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL;
import javax.microedition.khronos.opengles.GL10;

public class GLTextureView extends TextureView implements SurfaceTextureListener {
   private static final String TAG = "GLTextureView";
   private static final boolean LOG_ATTACH_DETACH = false;
   private static final boolean LOG_THREADS = false;
   private static final boolean LOG_PAUSE_RESUME = false;
   private static final boolean LOG_SURFACE = false;
   private static final boolean LOG_RENDERER = false;
   private static final boolean LOG_RENDERER_DRAW_FRAME = false;
   private static final boolean LOG_EGL = false;
   public static final int RENDERMODE_WHEN_DIRTY = 0;
   public static final int RENDERMODE_CONTINUOUSLY = 1;
   public static final int DEBUG_CHECK_GL_ERROR = 1;
   public static final int DEBUG_LOG_GL_CALLS = 2;
   private static final GLThreadManager sGLThreadManager = new GLThreadManager();
   private final WeakReference<GLTextureView> mThisWeakRef = new WeakReference(this);
   private GLThread mGLThread;
   private Renderer mRenderer;
   private boolean mDetached;
   private EGLConfigChooser mEGLConfigChooser;
   private EGLContextFactory mEGLContextFactory;
   private EGLWindowSurfaceFactory mEGLWindowSurfaceFactory;
   private GLWrapper mGLWrapper;
   private int mDebugFlags;
   private int mEGLContextClientVersion;
   private boolean mPreserveEGLContextOnPause;

   public GLTextureView(Context context) {
      super(context);
      this.init();
   }

   public GLTextureView(Context context, AttributeSet attrs) {
      super(context, attrs);
      this.init();
   }

   protected void finalize() throws Throwable {
      try {
         if (this.mGLThread != null) {
            this.mGLThread.requestExitAndWait();
         }
      } finally {
         super.finalize();
      }

   }

   private void init() {
      super.setSurfaceTextureListener(this);
   }

   public void setGLWrapper(GLWrapper glWrapper) {
      this.mGLWrapper = glWrapper;
   }

   public void setDebugFlags(int debugFlags) {
      this.mDebugFlags = debugFlags;
   }

   public int getDebugFlags() {
      return this.mDebugFlags;
   }

   public void setPreserveEGLContextOnPause(boolean preserveOnPause) {
      this.mPreserveEGLContextOnPause = preserveOnPause;
   }

   public boolean getPreserveEGLContextOnPause() {
      return this.mPreserveEGLContextOnPause;
   }

   public void setRenderer(Renderer renderer) {
      this.checkRenderThreadState();
      if (this.mEGLConfigChooser == null) {
         this.mEGLConfigChooser = new SimpleEGLConfigChooser(true);
      }

      if (this.mEGLContextFactory == null) {
         this.mEGLContextFactory = new DefaultContextFactory();
      }

      if (this.mEGLWindowSurfaceFactory == null) {
         this.mEGLWindowSurfaceFactory = new DefaultWindowSurfaceFactory();
      }

      this.mRenderer = renderer;
      this.mGLThread = new GLThread(this.mThisWeakRef);
      this.mGLThread.start();
   }

   public void setEGLContextFactory(EGLContextFactory factory) {
      this.checkRenderThreadState();
      this.mEGLContextFactory = factory;
   }

   public void setEGLWindowSurfaceFactory(EGLWindowSurfaceFactory factory) {
      this.checkRenderThreadState();
      this.mEGLWindowSurfaceFactory = factory;
   }

   public void setEGLConfigChooser(EGLConfigChooser configChooser) {
      this.checkRenderThreadState();
      this.mEGLConfigChooser = configChooser;
   }

   public void setEGLConfigChooser(boolean needDepth) {
      this.setEGLConfigChooser(new SimpleEGLConfigChooser(needDepth));
   }

   public void setEGLConfigChooser(int redSize, int greenSize, int blueSize, int alphaSize, int depthSize, int stencilSize) {
      this.setEGLConfigChooser(new ComponentSizeChooser(redSize, greenSize, blueSize, alphaSize, depthSize, stencilSize));
   }

   public void setEGLContextClientVersion(int version) {
      this.checkRenderThreadState();
      this.mEGLContextClientVersion = version;
   }

   public void setRenderMode(int renderMode) {
      if (this.mGLThread != null) {
         this.mGLThread.setRenderMode(renderMode);
      }

   }

   public int getRenderMode() {
      return this.mGLThread.getRenderMode();
   }

   public void requestRender() {
      this.mGLThread.requestRender();
   }

   /** @deprecated */
   @Deprecated
   public void setSurfaceTextureListener(SurfaceTextureListener listener) {
      Log.e("GLTextureView", "setSurfaceTextureListener preserved, setRenderer() instead?");
   }

   public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
      this.mGLThread.surfaceCreated();
      this.onSurfaceTextureSizeChanged(surface, width, height);
   }

   public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
      this.mGLThread.onWindowResize(width, height);
   }

   public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
      this.mGLThread.surfaceDestroyed();
      if (null != this.mRenderer) {
         this.mRenderer.onSurfaceDestroyed();
      }

      return true;
   }

   public void onSurfaceTextureUpdated(SurfaceTexture surface) {
   }

   public void onPause() {
      this.mGLThread.onPause();
   }

   public void onResume() {
      this.mGLThread.onResume();
   }

   public void queueEvent(Runnable r) {
      this.mGLThread.queueEvent(r);
   }

   protected void onAttachedToWindow() {
      super.onAttachedToWindow();
      if (this.mDetached && this.mRenderer != null) {
         int renderMode = 1;
         if (this.mGLThread != null) {
            renderMode = this.mGLThread.getRenderMode();
         }

         this.mGLThread = new GLThread(this.mThisWeakRef);
         if (renderMode != 1) {
            this.mGLThread.setRenderMode(renderMode);
         }

         this.mGLThread.start();
      }

      this.mDetached = false;
   }

   protected void onDetachedFromWindow() {
      if (this.mGLThread != null) {
         this.mGLThread.requestExitAndWait();
      }

      this.mDetached = true;
      super.onDetachedFromWindow();
   }

   private void checkRenderThreadState() {
      if (this.mGLThread != null) {
         throw new IllegalStateException("setRenderer has already been called for this instance.");
      }
   }

   private static class GLThreadManager {
      private static String TAG = "GLThreadManager";
      private boolean mGLESVersionCheckComplete;
      private int mGLESVersion;
      private boolean mGLESDriverCheckComplete;
      private boolean mMultipleGLESContextsAllowed;
      private boolean mLimitedGLESContexts;
      private static final int kGLES_20 = 131072;
      private static final String kMSM7K_RENDERER_PREFIX = "Q3Dimension MSM7500 ";
      private GLThread mEglOwner;

      private GLThreadManager() {
      }

      public synchronized void threadExiting(GLThread thread) {
         thread.mExited = true;
         if (this.mEglOwner == thread) {
            this.mEglOwner = null;
         }

         this.notifyAll();
      }

      public boolean tryAcquireEglContextLocked(GLThread thread) {
         if (this.mEglOwner != thread && this.mEglOwner != null) {
            this.checkGLESVersion();
            if (this.mMultipleGLESContextsAllowed) {
               return true;
            } else {
               if (this.mEglOwner != null) {
                  this.mEglOwner.requestReleaseEglContextLocked();
               }

               return false;
            }
         } else {
            this.mEglOwner = thread;
            this.notifyAll();
            return true;
         }
      }

      public void releaseEglContextLocked(GLThread thread) {
         if (this.mEglOwner == thread) {
            this.mEglOwner = null;
         }

         this.notifyAll();
      }

      public synchronized boolean shouldReleaseEGLContextWhenPausing() {
         return this.mLimitedGLESContexts;
      }

      public synchronized boolean shouldTerminateEGLWhenPausing() {
         this.checkGLESVersion();
         return !this.mMultipleGLESContextsAllowed;
      }

      public synchronized void checkGLDriver(GL10 gl) {
         if (!this.mGLESDriverCheckComplete) {
            this.checkGLESVersion();
            String renderer = gl.glGetString(7937);
            if (this.mGLESVersion < 131072) {
               this.mMultipleGLESContextsAllowed = !renderer.startsWith("Q3Dimension MSM7500 ");
               this.notifyAll();
            }

            this.mLimitedGLESContexts = !this.mMultipleGLESContextsAllowed;
            this.mGLESDriverCheckComplete = true;
         }

      }

      private void checkGLESVersion() {
         if (!this.mGLESVersionCheckComplete) {
            this.mGLESVersion = SystemProperties.getInt("ro.opengles.version", 0);
            if (this.mGLESVersion >= 131072) {
               this.mMultipleGLESContextsAllowed = true;
            }

            this.mGLESVersionCheckComplete = true;
         }

      }

      // $FF: synthetic method
      GLThreadManager(Object x0) {
         this();
      }
   }

   static class LogWriter extends Writer {
      private StringBuilder mBuilder = new StringBuilder();

      public void close() {
         this.flushBuilder();
      }

      public void flush() {
         this.flushBuilder();
      }

      public void write(char[] buf, int offset, int count) {
         for(int i = 0; i < count; ++i) {
            char c = buf[offset + i];
            if (c == '\n') {
               this.flushBuilder();
            } else {
               this.mBuilder.append(c);
            }
         }

      }

      private void flushBuilder() {
         if (this.mBuilder.length() > 0) {
            Log.v("GLTextureView", this.mBuilder.toString());
            this.mBuilder.delete(0, this.mBuilder.length());
         }

      }
   }

   static class GLThread extends Thread {
      private boolean mShouldExit;
      private boolean mExited;
      private boolean mRequestPaused;
      private boolean mPaused;
      private boolean mHasSurface;
      private boolean mSurfaceIsBad;
      private boolean mWaitingForSurface;
      private boolean mHaveEglContext;
      private boolean mHaveEglSurface;
      private boolean mFinishedCreatingEglSurface;
      private boolean mShouldReleaseEglContext;
      private int mWidth = 0;
      private int mHeight = 0;
      private int mRenderMode = 1;
      private boolean mRequestRender = true;
      private boolean mRenderComplete;
      private ArrayList<Runnable> mEventQueue = new ArrayList();
      private boolean mSizeChanged = true;
      private EglHelper mEglHelper;
      private WeakReference<GLTextureView> mGLTextureViewWeakRef;

      GLThread(WeakReference<GLTextureView> glTextureViewWeakRef) {
         this.mGLTextureViewWeakRef = glTextureViewWeakRef;
      }

      public void run() {
         this.setName("GLThread " + this.getId());

         try {
            this.guardedRun();
         } catch (Exception var5) {
         } finally {
            GLTextureView.sGLThreadManager.threadExiting(this);
         }

      }

      private void stopEglSurfaceLocked() {
         if (this.mHaveEglSurface) {
            this.mHaveEglSurface = false;
            this.mEglHelper.destroySurface();
         }

      }

      private void stopEglContextLocked() {
         if (this.mHaveEglContext) {
            this.mEglHelper.finish();
            this.mHaveEglContext = false;
            GLTextureView.sGLThreadManager.releaseEglContextLocked(this);
         }

      }

      private void guardedRun() throws InterruptedException {
         this.mEglHelper = new EglHelper(this.mGLTextureViewWeakRef);
         this.mHaveEglContext = false;
         this.mHaveEglSurface = false;
         boolean var32 = false;

         try {
            var32 = true;
            GL10 gl = null;
            boolean createEglContext = false;
            boolean createEglSurface = false;
            boolean createGlInterface = false;
            boolean lostEglContext = false;
            boolean sizeChanged = false;
            boolean wantRenderNotification = false;
            boolean doRenderNotification = false;
            boolean askedToReleaseEglContext = false;
            int w = 0;
            int h = 0;
            Runnable event = null;

            label431:
            while(true) {
               while(true) {
                  synchronized(GLTextureView.sGLThreadManager) {
                     while(true) {
                        if (this.mShouldExit) {
                           var32 = false;
                           break label431;
                        }

                        if (!this.mEventQueue.isEmpty()) {
                           event = (Runnable)this.mEventQueue.remove(0);
                           break;
                        }

                        boolean pausing = false;
                        if (this.mPaused != this.mRequestPaused) {
                           pausing = this.mRequestPaused;
                           this.mPaused = this.mRequestPaused;
                           GLTextureView.sGLThreadManager.notifyAll();
                        }

                        if (this.mShouldReleaseEglContext) {
                           this.stopEglSurfaceLocked();
                           this.stopEglContextLocked();
                           this.mShouldReleaseEglContext = false;
                           askedToReleaseEglContext = true;
                        }

                        if (lostEglContext) {
                           this.stopEglSurfaceLocked();
                           this.stopEglContextLocked();
                           lostEglContext = false;
                        }

                        if (pausing && this.mHaveEglSurface) {
                           this.stopEglSurfaceLocked();
                        }

                        if (pausing && this.mHaveEglContext) {
                           GLTextureView view = (GLTextureView)this.mGLTextureViewWeakRef.get();
                           boolean preserveEglContextOnPause = view == null ? false : view.mPreserveEGLContextOnPause;
                           if (!preserveEglContextOnPause || GLTextureView.sGLThreadManager.shouldReleaseEGLContextWhenPausing()) {
                              this.stopEglContextLocked();
                           }
                        }

                        if (pausing && GLTextureView.sGLThreadManager.shouldTerminateEGLWhenPausing()) {
                           this.mEglHelper.finish();
                        }

                        if (!this.mHasSurface && !this.mWaitingForSurface) {
                           if (this.mHaveEglSurface) {
                              this.stopEglSurfaceLocked();
                           }

                           this.mWaitingForSurface = true;
                           this.mSurfaceIsBad = false;
                           GLTextureView.sGLThreadManager.notifyAll();
                        }

                        if (this.mHasSurface && this.mWaitingForSurface) {
                           this.mWaitingForSurface = false;
                           GLTextureView.sGLThreadManager.notifyAll();
                        }

                        if (doRenderNotification) {
                           wantRenderNotification = false;
                           doRenderNotification = false;
                           this.mRenderComplete = true;
                           GLTextureView.sGLThreadManager.notifyAll();
                        }

                        if (this.readyToDraw()) {
                           if (!this.mHaveEglContext) {
                              if (askedToReleaseEglContext) {
                                 askedToReleaseEglContext = false;
                              } else if (GLTextureView.sGLThreadManager.tryAcquireEglContextLocked(this)) {
                                 try {
                                    this.mEglHelper.start();
                                 } catch (RuntimeException var38) {
                                    GLTextureView.sGLThreadManager.releaseEglContextLocked(this);
                                    throw var38;
                                 }

                                 this.mHaveEglContext = true;
                                 createEglContext = true;
                                 GLTextureView.sGLThreadManager.notifyAll();
                              }
                           }

                           if (this.mHaveEglContext && !this.mHaveEglSurface) {
                              this.mHaveEglSurface = true;
                              createEglSurface = true;
                              createGlInterface = true;
                              sizeChanged = true;
                           }

                           if (this.mHaveEglSurface) {
                              if (this.mSizeChanged) {
                                 sizeChanged = true;
                                 w = this.mWidth;
                                 h = this.mHeight;
                                 wantRenderNotification = true;
                                 createEglSurface = true;
                                 this.mSizeChanged = false;
                              }

                              this.mRequestRender = false;
                              GLTextureView.sGLThreadManager.notifyAll();
                              break;
                           }
                        }

                        GLTextureView.sGLThreadManager.wait();
                     }
                  }

                  if (event != null) {
                     event.run();
                     event = null;
                  } else {
                     if (createEglSurface) {
                        if (!this.mEglHelper.createSurface()) {
                           synchronized(GLTextureView.sGLThreadManager) {
                              this.mFinishedCreatingEglSurface = true;
                              this.mSurfaceIsBad = true;
                              GLTextureView.sGLThreadManager.notifyAll();
                              continue;
                           }
                        }

                        synchronized(GLTextureView.sGLThreadManager) {
                           this.mFinishedCreatingEglSurface = true;
                           GLTextureView.sGLThreadManager.notifyAll();
                        }

                        createEglSurface = false;
                     }

                     if (createGlInterface) {
                        gl = (GL10)this.mEglHelper.createGL();
                        GLTextureView.sGLThreadManager.checkGLDriver(gl);
                        createGlInterface = false;
                     }

                     GLTextureView view;
                     if (createEglContext) {
                        view = (GLTextureView)this.mGLTextureViewWeakRef.get();
                        if (view != null) {
                           view.mRenderer.onSurfaceCreated(gl, this.mEglHelper.mEglConfig);
                        }

                        createEglContext = false;
                     }

                     if (sizeChanged) {
                        view = (GLTextureView)this.mGLTextureViewWeakRef.get();
                        if (view != null) {
                           view.mRenderer.onSurfaceChanged(gl, w, h);
                        }

                        sizeChanged = false;
                     }

                     boolean needSwap = false;
                     view = (GLTextureView)this.mGLTextureViewWeakRef.get();
                     if (view != null) {
                        needSwap = view.mRenderer.onDrawFrame(gl);
                     }

                     if (needSwap) {
                        int swapError = this.mEglHelper.swap();
                        switch(swapError) {
                        case 12288:
                           break;
                        case 12302:
                           lostEglContext = true;
                           break;
                        default:
                           EglHelper.logEglErrorAsWarning("GLThread", "eglSwapBuffers", swapError);
                           synchronized(GLTextureView.sGLThreadManager) {
                              this.mSurfaceIsBad = true;
                              GLTextureView.sGLThreadManager.notifyAll();
                           }
                        }
                     }

                     if (wantRenderNotification) {
                        doRenderNotification = true;
                     }
                  }
               }
            }
         } finally {
            if (var32) {
               synchronized(GLTextureView.sGLThreadManager) {
                  this.stopEglSurfaceLocked();
                  this.stopEglContextLocked();
               }
            }
         }

         synchronized(GLTextureView.sGLThreadManager) {
            this.stopEglSurfaceLocked();
            this.stopEglContextLocked();
         }
      }

      public boolean ableToDraw() {
         return this.mHaveEglContext && this.mHaveEglSurface && this.readyToDraw();
      }

      private boolean readyToDraw() {
         return !this.mPaused && this.mHasSurface && !this.mSurfaceIsBad && this.mWidth > 0 && this.mHeight > 0 && (this.mRequestRender || this.mRenderMode == 1);
      }

      public void setRenderMode(int renderMode) {
         if (0 <= renderMode && renderMode <= 1) {
            synchronized(GLTextureView.sGLThreadManager) {
               this.mRenderMode = renderMode;
               GLTextureView.sGLThreadManager.notifyAll();
            }
         } else {
            throw new IllegalArgumentException("renderMode");
         }
      }

      public int getRenderMode() {
         synchronized(GLTextureView.sGLThreadManager) {
            return this.mRenderMode;
         }
      }

      public void requestRender() {
         synchronized(GLTextureView.sGLThreadManager) {
            this.mRequestRender = true;
            GLTextureView.sGLThreadManager.notifyAll();
         }
      }

      public void surfaceCreated() {
         synchronized(GLTextureView.sGLThreadManager) {
            this.mHasSurface = true;
            this.mFinishedCreatingEglSurface = false;
            GLTextureView.sGLThreadManager.notifyAll();

            while(this.mWaitingForSurface && !this.mFinishedCreatingEglSurface && !this.mExited) {
               try {
                  GLTextureView.sGLThreadManager.wait();
               } catch (InterruptedException var4) {
                  Thread.currentThread().interrupt();
               }
            }

         }
      }

      public void surfaceDestroyed() {
         synchronized(GLTextureView.sGLThreadManager) {
            this.mHasSurface = false;
            GLTextureView.sGLThreadManager.notifyAll();

            while(!this.mWaitingForSurface && !this.mExited) {
               try {
                  GLTextureView.sGLThreadManager.wait();
               } catch (InterruptedException var4) {
                  Thread.currentThread().interrupt();
               }
            }

         }
      }

      public void onPause() {
         synchronized(GLTextureView.sGLThreadManager) {
            this.mRequestPaused = true;
            GLTextureView.sGLThreadManager.notifyAll();

            while(!this.mExited && !this.mPaused) {
               try {
                  GLTextureView.sGLThreadManager.wait();
               } catch (InterruptedException var4) {
                  Thread.currentThread().interrupt();
               }
            }

         }
      }

      public void onResume() {
         synchronized(GLTextureView.sGLThreadManager) {
            this.mRequestPaused = false;
            this.mRequestRender = true;
            this.mRenderComplete = false;
            GLTextureView.sGLThreadManager.notifyAll();

            while(!this.mExited && this.mPaused && !this.mRenderComplete) {
               try {
                  GLTextureView.sGLThreadManager.wait();
               } catch (InterruptedException var4) {
                  Thread.currentThread().interrupt();
               }
            }

         }
      }

      public void onWindowResize(int w, int h) {
         synchronized(GLTextureView.sGLThreadManager) {
            this.mWidth = w;
            this.mHeight = h;
            this.mSizeChanged = true;
            this.mRequestRender = true;
            this.mRenderComplete = false;
            GLTextureView.sGLThreadManager.notifyAll();

            while(!this.mExited && !this.mPaused && !this.mRenderComplete && this.ableToDraw()) {
               try {
                  GLTextureView.sGLThreadManager.wait();
               } catch (InterruptedException var6) {
                  Thread.currentThread().interrupt();
               }
            }

         }
      }

      public void requestExitAndWait() {
         synchronized(GLTextureView.sGLThreadManager) {
            this.mShouldExit = true;
            GLTextureView.sGLThreadManager.notifyAll();

            while(!this.mExited) {
               try {
                  GLTextureView.sGLThreadManager.wait();
               } catch (InterruptedException var4) {
                  Thread.currentThread().interrupt();
               }
            }

         }
      }

      public void requestReleaseEglContextLocked() {
         this.mShouldReleaseEglContext = true;
         GLTextureView.sGLThreadManager.notifyAll();
      }

      public void queueEvent(Runnable r) {
         if (r == null) {
            throw new IllegalArgumentException("r must not be null");
         } else {
            synchronized(GLTextureView.sGLThreadManager) {
               this.mEventQueue.add(r);
               GLTextureView.sGLThreadManager.notifyAll();
            }
         }
      }
   }

   private static class EglHelper {
      private WeakReference<GLTextureView> mGLTextureViewWeakRef;
      EGL10 mEgl;
      EGLDisplay mEglDisplay;
      EGLSurface mEglSurface;
      EGLConfig mEglConfig;
      EGLContext mEglContext;

      public EglHelper(WeakReference<GLTextureView> glTextureViewWeakRef) {
         this.mGLTextureViewWeakRef = glTextureViewWeakRef;
      }

      public void start() {
         this.mEgl = (EGL10)EGLContext.getEGL();
         this.mEglDisplay = this.mEgl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
         if (this.mEglDisplay == EGL10.EGL_NO_DISPLAY) {
            throw new RuntimeException("eglGetDisplay failed");
         } else {
            int[] version = new int[2];
            if (!this.mEgl.eglInitialize(this.mEglDisplay, version)) {
               throw new RuntimeException("eglInitialize failed");
            } else {
               GLTextureView view = (GLTextureView)this.mGLTextureViewWeakRef.get();
               if (view == null) {
                  this.mEglConfig = null;
                  this.mEglContext = null;
               } else {
                  this.mEglConfig = view.mEGLConfigChooser.chooseConfig(this.mEgl, this.mEglDisplay);
                  this.mEglContext = view.mEGLContextFactory.createContext(this.mEgl, this.mEglDisplay, this.mEglConfig);
               }

               if (this.mEglContext == null || this.mEglContext == EGL10.EGL_NO_CONTEXT) {
                  this.mEglContext = null;
                  this.throwEglException("createContext");
               }

               this.mEglSurface = null;
            }
         }
      }

      public boolean createSurface() {
         if (this.mEgl == null) {
            throw new RuntimeException("egl not initialized");
         } else if (this.mEglDisplay == null) {
            throw new RuntimeException("eglDisplay not initialized");
         } else if (this.mEglConfig == null) {
            throw new RuntimeException("mEglConfig not initialized");
         } else {
            this.destroySurfaceImp();
            GLTextureView view = (GLTextureView)this.mGLTextureViewWeakRef.get();
            if (view != null) {
               this.mEglSurface = view.mEGLWindowSurfaceFactory.createWindowSurface(this.mEgl, this.mEglDisplay, this.mEglConfig, view.getSurfaceTexture());
            } else {
               this.mEglSurface = null;
            }

            if (this.mEglSurface != null && this.mEglSurface != EGL10.EGL_NO_SURFACE) {
               if (!this.mEgl.eglMakeCurrent(this.mEglDisplay, this.mEglSurface, this.mEglSurface, this.mEglContext)) {
                  logEglErrorAsWarning("EGLHelper", "eglMakeCurrent", this.mEgl.eglGetError());
                  return false;
               } else {
                  return true;
               }
            } else {
               int error = this.mEgl.eglGetError();
               if (error == 12299) {
                  Log.e("EglHelper", "createWindowSurface returned EGL_BAD_NATIVE_WINDOW.");
               }

               return false;
            }
         }
      }

      GL createGL() {
         GL gl = this.mEglContext.getGL();
         GLTextureView view = (GLTextureView)this.mGLTextureViewWeakRef.get();
         if (view != null) {
            if (view.mGLWrapper != null) {
               gl = view.mGLWrapper.wrap(gl);
            }

            if ((view.mDebugFlags & 3) != 0) {
               int configFlags = 0;
               Writer log = null;
               if ((view.mDebugFlags & 1) != 0) {
                  configFlags |= 1;
               }

               if ((view.mDebugFlags & 2) != 0) {
                  log = new LogWriter();
               }

               gl = GLDebugHelper.wrap(gl, configFlags, log);
            }
         }

         return gl;
      }

      public int swap() {
         return !this.mEgl.eglSwapBuffers(this.mEglDisplay, this.mEglSurface) ? this.mEgl.eglGetError() : 12288;
      }

      public void destroySurface() {
         this.destroySurfaceImp();
      }

      private void destroySurfaceImp() {
         if (this.mEglSurface != null && this.mEglSurface != EGL10.EGL_NO_SURFACE) {
            this.mEgl.eglMakeCurrent(this.mEglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
            GLTextureView view = (GLTextureView)this.mGLTextureViewWeakRef.get();
            if (view != null) {
               view.mEGLWindowSurfaceFactory.destroySurface(this.mEgl, this.mEglDisplay, this.mEglSurface);
            }

            this.mEglSurface = null;
         }

      }

      public void finish() {
         if (this.mEglContext != null) {
            GLTextureView view = (GLTextureView)this.mGLTextureViewWeakRef.get();
            if (view != null) {
               view.mEGLContextFactory.destroyContext(this.mEgl, this.mEglDisplay, this.mEglContext);
            }

            this.mEglContext = null;
         }

         if (this.mEglDisplay != null) {
            this.mEgl.eglTerminate(this.mEglDisplay);
            this.mEglDisplay = null;
         }

      }

      private void throwEglException(String function) {
         throwEglException(function, this.mEgl.eglGetError());
      }

      public static void throwEglException(String function, int error) {
         String message = formatEglError(function, error);
         throw new RuntimeException(message);
      }

      public static void logEglErrorAsWarning(String tag, String function, int error) {
         Log.w(tag, formatEglError(function, error));
      }

      public static String formatEglError(String function, int error) {
         return function + " failed: " + eglGetErrorString(error);
      }

      static String eglGetErrorString(int error) {
         switch(error) {
         case 12288:
            return "EGL_SUCCESS";
         case 12289:
            return "EGL_NOT_INITIALIZED";
         case 12290:
            return "EGL_BAD_ACCESS";
         case 12291:
            return "EGL_BAD_ALLOC";
         case 12292:
            return "EGL_BAD_ATTRIBUTE";
         case 12293:
            return "EGL_BAD_CONFIG";
         case 12294:
            return "EGL_BAD_CONTEXT";
         case 12295:
            return "EGL_BAD_CURRENT_SURFACE";
         case 12296:
            return "EGL_BAD_DISPLAY";
         case 12297:
            return "EGL_BAD_MATCH";
         case 12298:
            return "EGL_BAD_NATIVE_PIXMAP";
         case 12299:
            return "EGL_BAD_NATIVE_WINDOW";
         case 12300:
            return "EGL_BAD_PARAMETER";
         case 12301:
            return "EGL_BAD_SURFACE";
         case 12302:
            return "EGL_CONTEXT_LOST";
         default:
            return "0x" + Integer.toHexString(error);
         }
      }
   }

   private class SimpleEGLConfigChooser extends ComponentSizeChooser {
      public SimpleEGLConfigChooser(boolean withDepthBuffer) {
         super(8, 8, 8, 0, withDepthBuffer ? 16 : 0, 0);
      }
   }

   private class ComponentSizeChooser extends BaseConfigChooser {
      private int[] mValue = new int[1];
      protected int mRedSize;
      protected int mGreenSize;
      protected int mBlueSize;
      protected int mAlphaSize;
      protected int mDepthSize;
      protected int mStencilSize;

      public ComponentSizeChooser(int redSize, int greenSize, int blueSize, int alphaSize, int depthSize, int stencilSize) {
         super(new int[]{12324, redSize, 12323, greenSize, 12322, blueSize, 12321, alphaSize, 12325, depthSize, 12326, stencilSize, 12344});
         this.mRedSize = redSize;
         this.mGreenSize = greenSize;
         this.mBlueSize = blueSize;
         this.mAlphaSize = alphaSize;
         this.mDepthSize = depthSize;
         this.mStencilSize = stencilSize;
      }

      public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display, EGLConfig[] configs) {
         EGLConfig[] var4 = configs;
         int var5 = configs.length;

         for(int var6 = 0; var6 < var5; ++var6) {
            EGLConfig config = var4[var6];
            int d = this.findConfigAttrib(egl, display, config, 12325, 0);
            int s = this.findConfigAttrib(egl, display, config, 12326, 0);
            if (d >= this.mDepthSize && s >= this.mStencilSize) {
               int r = this.findConfigAttrib(egl, display, config, 12324, 0);
               int g = this.findConfigAttrib(egl, display, config, 12323, 0);
               int b = this.findConfigAttrib(egl, display, config, 12322, 0);
               int a = this.findConfigAttrib(egl, display, config, 12321, 0);
               if (r == this.mRedSize && g == this.mGreenSize && b == this.mBlueSize && a == this.mAlphaSize) {
                  return config;
               }
            }
         }

         return configs.length > 0 ? configs[0] : null;
      }

      private int findConfigAttrib(EGL10 egl, EGLDisplay display, EGLConfig config, int attribute, int defaultValue) {
         return egl.eglGetConfigAttrib(display, config, attribute, this.mValue) ? this.mValue[0] : defaultValue;
      }
   }

   private abstract class BaseConfigChooser implements EGLConfigChooser {
      protected int[] mConfigSpec;

      public BaseConfigChooser(int[] configSpec) {
         this.mConfigSpec = this.filterConfigSpec(configSpec);
      }

      public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
         int[] num_config = new int[1];
         if (!egl.eglChooseConfig(display, this.mConfigSpec, (EGLConfig[])null, 0, num_config)) {
            throw new IllegalArgumentException("eglChooseConfig failed");
         } else {
            int numConfigs = num_config[0];
            if (numConfigs <= 0) {
               throw new IllegalArgumentException("No configs match configSpec");
            } else {
               EGLConfig[] configs = new EGLConfig[numConfigs];
               if (!egl.eglChooseConfig(display, this.mConfigSpec, configs, numConfigs, num_config)) {
                  throw new IllegalArgumentException("eglChooseConfig#2 failed");
               } else {
                  EGLConfig config = this.chooseConfig(egl, display, configs);
                  if (config == null) {
                     throw new IllegalArgumentException("No config chosen");
                  } else {
                     return config;
                  }
               }
            }
         }
      }

      abstract EGLConfig chooseConfig(EGL10 var1, EGLDisplay var2, EGLConfig[] var3);

      private int[] filterConfigSpec(int[] configSpec) {
         if (GLTextureView.this.mEGLContextClientVersion != 2 && GLTextureView.this.mEGLContextClientVersion != 3) {
            return configSpec;
         } else {
            int len = configSpec.length;
            int[] newConfigSpec = new int[len + 2];
            System.arraycopy(configSpec, 0, newConfigSpec, 0, len - 1);
            newConfigSpec[len - 1] = 12352;
            if (GLTextureView.this.mEGLContextClientVersion == 2) {
               newConfigSpec[len] = 4;
            } else {
               newConfigSpec[len] = 64;
            }

            newConfigSpec[len + 1] = 12344;
            return newConfigSpec;
         }
      }
   }

   public interface EGLConfigChooser {
      EGLConfig chooseConfig(EGL10 var1, EGLDisplay var2);
   }

   private static class DefaultWindowSurfaceFactory implements EGLWindowSurfaceFactory {
      private DefaultWindowSurfaceFactory() {
      }

      public EGLSurface createWindowSurface(EGL10 egl, EGLDisplay display, EGLConfig config, Object nativeWindow) {
         EGLSurface result = null;

         try {
            result = egl.eglCreateWindowSurface(display, config, nativeWindow, (int[])null);
         } catch (IllegalArgumentException var7) {
            Log.e("GLTextureView", "eglCreateWindowSurface", var7);
         }

         return result;
      }

      public void destroySurface(EGL10 egl, EGLDisplay display, EGLSurface surface) {
         egl.eglDestroySurface(display, surface);
      }

      // $FF: synthetic method
      DefaultWindowSurfaceFactory(Object x0) {
         this();
      }
   }

   public interface EGLWindowSurfaceFactory {
      EGLSurface createWindowSurface(EGL10 var1, EGLDisplay var2, EGLConfig var3, Object var4);

      void destroySurface(EGL10 var1, EGLDisplay var2, EGLSurface var3);
   }

   private class DefaultContextFactory implements EGLContextFactory {
      private int EGL_CONTEXT_CLIENT_VERSION;

      private DefaultContextFactory() {
         this.EGL_CONTEXT_CLIENT_VERSION = 12440;
      }

      public EGLContext createContext(EGL10 egl, EGLDisplay display, EGLConfig config) {
         int[] attrib_list = new int[]{this.EGL_CONTEXT_CLIENT_VERSION, GLTextureView.this.mEGLContextClientVersion, 12344};
         return egl.eglCreateContext(display, config, EGL10.EGL_NO_CONTEXT, GLTextureView.this.mEGLContextClientVersion != 0 ? attrib_list : null);
      }

      public void destroyContext(EGL10 egl, EGLDisplay display, EGLContext context) {
         if (!egl.eglDestroyContext(display, context)) {
            Log.e("DefaultContextFactory", "display:" + display + " context: " + context);
            EglHelper.throwEglException("eglDestroyContex", egl.eglGetError());
         }

      }

      // $FF: synthetic method
      DefaultContextFactory(Object x1) {
         this();
      }
   }

   public interface EGLContextFactory {
      EGLContext createContext(EGL10 var1, EGLDisplay var2, EGLConfig var3);

      void destroyContext(EGL10 var1, EGLDisplay var2, EGLContext var3);
   }

   public interface Renderer {
      void onSurfaceCreated(GL10 var1, EGLConfig var2);

      void onSurfaceChanged(GL10 var1, int var2, int var3);

      boolean onDrawFrame(GL10 var1);

      void onSurfaceDestroyed();
   }

   public interface GLWrapper {
      GL wrap(GL var1);
   }
}
