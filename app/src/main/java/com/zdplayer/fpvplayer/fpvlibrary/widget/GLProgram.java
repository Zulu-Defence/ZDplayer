package com.zdplayer.fpvplayer.fpvlibrary.widget;

import android.opengl.GLES20;
import android.opengl.Matrix;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class GLProgram {
   private int _program;
   public final int mWinPosition;
   private int _textureI;
   private int _textureII;
   private int _textureIII;
   private int _tIindex;
   private int _tIIindex;
   private int _tIIIindex;
   private float[] _vertices;
   private int _uMVPMatrixHandle = -1;
   private int _positionHandle = -1;
   private int _coordHandle = -1;
   private int _yhandle = -1;
   private int _uhandle = -1;
   private int _vhandle = -1;
   private int _ytid = -1;
   private int _utid = -1;
   private int _vtid = -1;
   private ByteBuffer _vertice_buffer;
   private ByteBuffer _coord_buffer;
   private int _video_width = -1;
   private int _video_height = -1;
   private boolean isProgBuilt = false;
   private float[] mMVPMatrix = new float[16];
   private float[] mProjMatrix = new float[16];
   private float[] mScaleMatrix = new float[16];
   private float[] mMoveMatrix = new float[16];
   private float[] mVMatrix = new float[16];
   public float scaleFactor = 1.0F;
   public float moveFactorX = 0.0F;
   public float moveFactorY = 0.0F;
   static float[] squareVertices = new float[]{-1.0F, -1.0F, 1.0F, -1.0F, -1.0F, 1.0F, 1.0F, 1.0F};
   static float[] squareVertices1 = new float[]{-1.0F, 0.0F, 0.0F, 0.0F, -1.0F, 1.0F, 0.0F, 1.0F};
   static float[] squareVertices2 = new float[]{0.0F, -1.0F, 1.0F, -1.0F, 0.0F, 0.0F, 1.0F, 0.0F};
   static float[] squareVertices3 = new float[]{-1.0F, -1.0F, 0.0F, -1.0F, -1.0F, 0.0F, 0.0F, 0.0F};
   static float[] squareVertices4 = new float[]{0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F};
   private static float[] coordVertices = new float[]{0.0F, 1.0F, 1.0F, 1.0F, 0.0F, 0.0F, 1.0F, 0.0F};
   private static final String VERTEX_SHADER = "uniform mat4 uMVPMatrix;\nattribute vec4 vPosition;\nattribute vec2 a_texCoord;\nvarying vec2 tc;\nvoid main() {\ngl_Position = uMVPMatrix * vPosition;\ntc = a_texCoord;\n}\n";
   private static final String FRAGMENT_SHADER = "precision mediump float;\nuniform sampler2D tex_y;\nuniform sampler2D tex_u,tex_v;\nvarying vec2 tc;\nvoid main(void) {\n  float r,g,b,y,u,v;\n  mediump vec4 txl,ux,vx;  y=texture2D(tex_y,tc).r;\n  u=texture2D(tex_u,tc).r;\n  v=texture2D(tex_v,tc).r;\n  y=1.1643*(y-0.0625);\n  u=u-0.5;\n  v=v-0.5;\n  r=y+1.5958*v;\n  g=y-0.39173*u-0.81290*v;\n  b=y+2.017*u;\n  gl_FragColor=vec4(r,g,b,1.0);\n}\n";

   public GLProgram(int position) {
      if (position >= 0 && position <= 4) {
         this.mWinPosition = position;
         this.setup(this.mWinPosition);
         float ratio = 1.3333334F;
         Matrix.frustumM(this.mProjMatrix, 0, -ratio, ratio, -1.0F, 1.0F, 3.0F, 7.0F);
      } else {
         throw new RuntimeException("Index can only be 0 to 4");
      }
   }

   public void setup(int position) {
      switch(this.mWinPosition) {
      case 0:
      default:
         this._vertices = squareVertices;
         this._textureI = 33984;
         this._textureII = 33985;
         this._textureIII = 33986;
         this._tIindex = 0;
         this._tIIindex = 1;
         this._tIIIindex = 2;
         break;
      case 1:
         this._vertices = squareVertices1;
         this._textureI = 33984;
         this._textureII = 33985;
         this._textureIII = 33986;
         this._tIindex = 0;
         this._tIIindex = 1;
         this._tIIIindex = 2;
         break;
      case 2:
         this._vertices = squareVertices2;
         this._textureI = 33987;
         this._textureII = 33988;
         this._textureIII = 33989;
         this._tIindex = 3;
         this._tIIindex = 4;
         this._tIIIindex = 5;
         break;
      case 3:
         this._vertices = squareVertices3;
         this._textureI = 33990;
         this._textureII = 33991;
         this._textureIII = 33992;
         this._tIindex = 6;
         this._tIIindex = 7;
         this._tIIIindex = 8;
         break;
      case 4:
         this._vertices = squareVertices4;
         this._textureI = 33993;
         this._textureII = 33994;
         this._textureIII = 33995;
         this._tIindex = 9;
         this._tIIindex = 10;
         this._tIIIindex = 11;
      }

   }

   public boolean isProgramBuilt() {
      return this.isProgBuilt;
   }

   public void buildProgram() {
      if (this._program <= 0) {
         this._program = this.createProgram("uniform mat4 uMVPMatrix;\nattribute vec4 vPosition;\nattribute vec2 a_texCoord;\nvarying vec2 tc;\nvoid main() {\ngl_Position = uMVPMatrix * vPosition;\ntc = a_texCoord;\n}\n", "precision mediump float;\nuniform sampler2D tex_y;\nuniform sampler2D tex_u,tex_v;\nvarying vec2 tc;\nvoid main(void) {\n  float r,g,b,y,u,v;\n  mediump vec4 txl,ux,vx;  y=texture2D(tex_y,tc).r;\n  u=texture2D(tex_u,tc).r;\n  v=texture2D(tex_v,tc).r;\n  y=1.1643*(y-0.0625);\n  u=u-0.5;\n  v=v-0.5;\n  r=y+1.5958*v;\n  g=y-0.39173*u-0.81290*v;\n  b=y+2.017*u;\n  gl_FragColor=vec4(r,g,b,1.0);\n}\n");
      }

      this._uMVPMatrixHandle = GLES20.glGetUniformLocation(this._program, "uMVPMatrix");
      this.checkGlError("glGetUniformLocation uMVPMatrix");
      if (this._uMVPMatrixHandle == -1) {
         throw new RuntimeException("Could not get uniform location for uMVPMatrix");
      } else {
         this._positionHandle = GLES20.glGetAttribLocation(this._program, "vPosition");
         this.checkGlError("glGetAttribLocation vPosition");
         if (this._positionHandle == -1) {
            throw new RuntimeException("Could not get attribute location for vPosition");
         } else {
            this._coordHandle = GLES20.glGetAttribLocation(this._program, "a_texCoord");
            this.checkGlError("glGetAttribLocation a_texCoord");
            if (this._coordHandle == -1) {
               throw new RuntimeException("Could not get attribute location for a_texCoord");
            } else {
               this._yhandle = GLES20.glGetUniformLocation(this._program, "tex_y");
               this.checkGlError("glGetUniformLocation tex_y");
               if (this._yhandle == -1) {
                  throw new RuntimeException("Could not get uniform location for tex_y");
               } else {
                  this._uhandle = GLES20.glGetUniformLocation(this._program, "tex_u");
                  this.checkGlError("glGetUniformLocation tex_u");
                  if (this._uhandle == -1) {
                     throw new RuntimeException("Could not get uniform location for tex_u");
                  } else {
                     this._vhandle = GLES20.glGetUniformLocation(this._program, "tex_v");
                     this.checkGlError("glGetUniformLocation tex_v");
                     if (this._vhandle == -1) {
                        throw new RuntimeException("Could not get uniform location for tex_v");
                     } else {
                        this.isProgBuilt = true;
                     }
                  }
               }
            }
         }
      }
   }

   public void frustumM(int width, int height) {
      float ratio = (float)width / (float)height;
      Matrix.frustumM(this.mProjMatrix, 0, -ratio, ratio, -1.0F, 1.0F, 3.0F, 7.0F);
   }

   public void buildTextures(Buffer y, Buffer u, Buffer v, int width, int height) {
      boolean videoSizeChanged = width != this._video_width || height != this._video_height;
      if (videoSizeChanged) {
         this._video_width = width;
         this._video_height = height;
      }

      int[] textures;
      if (this._ytid < 0 || videoSizeChanged) {
         if (this._ytid >= 0) {
            GLES20.glDeleteTextures(1, new int[]{this._ytid}, 0);
            this.checkGlError("glDeleteTextures");
         }

         textures = new int[1];
         GLES20.glGenTextures(1, textures, 0);
         this.checkGlError("glGenTextures");
         this._ytid = textures[0];
      }

      GLES20.glBindTexture(3553, this._ytid);
      this.checkGlError("glBindTexture");
      GLES20.glTexImage2D(3553, 0, 6409, this._video_width, this._video_height, 0, 6409, 5121, y);
      this.checkGlError("glTexImage2D");
      GLES20.glTexParameterf(3553, 10241, 9728.0F);
      GLES20.glTexParameterf(3553, 10240, 9729.0F);
      GLES20.glTexParameteri(3553, 10242, 33071);
      GLES20.glTexParameteri(3553, 10243, 33071);
      if (this._utid < 0 || videoSizeChanged) {
         if (this._utid >= 0) {
            GLES20.glDeleteTextures(1, new int[]{this._utid}, 0);
            this.checkGlError("glDeleteTextures");
         }

         textures = new int[1];
         GLES20.glGenTextures(1, textures, 0);
         this.checkGlError("glGenTextures");
         this._utid = textures[0];
      }

      GLES20.glBindTexture(3553, this._utid);
      GLES20.glTexImage2D(3553, 0, 6409, this._video_width / 2, this._video_height / 2, 0, 6409, 5121, u);
      GLES20.glTexParameterf(3553, 10241, 9728.0F);
      GLES20.glTexParameterf(3553, 10240, 9729.0F);
      GLES20.glTexParameteri(3553, 10242, 33071);
      GLES20.glTexParameteri(3553, 10243, 33071);
      if (this._vtid < 0 || videoSizeChanged) {
         if (this._vtid >= 0) {
            GLES20.glDeleteTextures(1, new int[]{this._vtid}, 0);
            this.checkGlError("glDeleteTextures");
         }

         textures = new int[1];
         GLES20.glGenTextures(1, textures, 0);
         this.checkGlError("glGenTextures");
         this._vtid = textures[0];
      }

      GLES20.glBindTexture(3553, this._vtid);
      GLES20.glTexImage2D(3553, 0, 6409, this._video_width / 2, this._video_height / 2, 0, 6409, 5121, v);
      GLES20.glTexParameterf(3553, 10241, 9728.0F);
      GLES20.glTexParameterf(3553, 10240, 9729.0F);
      GLES20.glTexParameteri(3553, 10242, 33071);
      GLES20.glTexParameteri(3553, 10243, 33071);
   }

   public void drawFrame() {
      if (this._vertice_buffer != null && this._coord_buffer != null) {
         GLES20.glUseProgram(this._program);
         this.checkGlError("glUseProgram");
         Matrix.setIdentityM(this.mScaleMatrix, 0);
         Matrix.scaleM(this.mScaleMatrix, 0, this.scaleFactor * ((float)this._video_width * 1.0F / (float)this._video_height), this.scaleFactor, this.scaleFactor);
         Matrix.setIdentityM(this.mMoveMatrix, 0);
         Matrix.translateM(this.mMoveMatrix, 0, this.moveFactorX, this.moveFactorY, 0.0F);
         float[] mTempMatrix = new float[16];
         Matrix.multiplyMM(mTempMatrix, 0, this.mScaleMatrix, 0, this.mMoveMatrix, 0);
         Matrix.setLookAtM(this.mVMatrix, 0, 0.0F, 0.0F, 3.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F);
         Matrix.multiplyMM(this.mMVPMatrix, 0, this.mVMatrix, 0, mTempMatrix, 0);
         Matrix.multiplyMM(this.mMVPMatrix, 0, this.mProjMatrix, 0, this.mMVPMatrix, 0);
         GLES20.glUniformMatrix4fv(this._uMVPMatrixHandle, 1, false, this.mMVPMatrix, 0);
         GLES20.glVertexAttribPointer(this._positionHandle, 2, 5126, false, 8, this._vertice_buffer);
         this.checkGlError("glVertexAttribPointer mPositionHandle");
         GLES20.glEnableVertexAttribArray(this._positionHandle);
         GLES20.glVertexAttribPointer(this._coordHandle, 2, 5126, false, 8, this._coord_buffer);
         this.checkGlError("glVertexAttribPointer maTextureHandle");
         GLES20.glEnableVertexAttribArray(this._coordHandle);
         GLES20.glActiveTexture(this._textureI);
         GLES20.glBindTexture(3553, this._ytid);
         GLES20.glUniform1i(this._yhandle, this._tIindex);
         GLES20.glActiveTexture(this._textureII);
         GLES20.glBindTexture(3553, this._utid);
         GLES20.glUniform1i(this._uhandle, this._tIIindex);
         GLES20.glActiveTexture(this._textureIII);
         GLES20.glBindTexture(3553, this._vtid);
         GLES20.glUniform1i(this._vhandle, this._tIIIindex);
         GLES20.glDrawArrays(5, 0, 4);
         GLES20.glFinish();
         GLES20.glDisableVertexAttribArray(this._positionHandle);
         GLES20.glDisableVertexAttribArray(this._coordHandle);
      }
   }

   public int createProgram(String vertexSource, String fragmentSource) {
      int vertexShader = this.loadShader(35633, vertexSource);
      int pixelShader = this.loadShader(35632, fragmentSource);
      int program = GLES20.glCreateProgram();
      if (program != 0) {
         GLES20.glAttachShader(program, vertexShader);
         this.checkGlError("glAttachShader");
         GLES20.glAttachShader(program, pixelShader);
         this.checkGlError("glAttachShader");
         GLES20.glLinkProgram(program);
         int[] linkStatus = new int[1];
         GLES20.glGetProgramiv(program, 35714, linkStatus, 0);
         if (linkStatus[0] != 1) {
            GLES20.glDeleteProgram(program);
            program = 0;
         }
      }

      return program;
   }

   private int loadShader(int shaderType, String source) {
      int shader = GLES20.glCreateShader(shaderType);
      if (shader != 0) {
         GLES20.glShaderSource(shader, source);
         GLES20.glCompileShader(shader);
         int[] compiled = new int[1];
         GLES20.glGetShaderiv(shader, 35713, compiled, 0);
         if (compiled[0] == 0) {
            GLES20.glDeleteShader(shader);
            shader = 0;
         }
      }

      return shader;
   }

   void createBuffers(float[] vert) {
      this._vertice_buffer = ByteBuffer.allocateDirect(vert.length * 4);
      this._vertice_buffer.order(ByteOrder.nativeOrder());
      this._vertice_buffer.asFloatBuffer().put(vert);
      this._vertice_buffer.position(0);
      if (this._coord_buffer == null) {
         this._coord_buffer = ByteBuffer.allocateDirect(coordVertices.length * 4);
         this._coord_buffer.order(ByteOrder.nativeOrder());
         this._coord_buffer.asFloatBuffer().put(coordVertices);
         this._coord_buffer.position(0);
      }

   }

   private void checkGlError(String op) {
      int error;
      if ((error = GLES20.glGetError()) != 0) {
         throw new RuntimeException(op + ": glError " + error);
      }
   }
}
