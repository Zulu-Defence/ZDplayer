package com.shenyaocn.android.OpenH264;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

public class ByteArrayOutputStream extends OutputStream {
   protected byte[] buf;
   protected int count;

   public ByteArrayOutputStream() {
      this(32);
   }

   public byte[] getBuf() {
      return this.buf;
   }

   public int getCount() {
      return this.count;
   }

   public ByteArrayOutputStream(int size) {
      if (size < 0) {
         throw new IllegalArgumentException("Negative initial size: " + size);
      } else {
         this.buf = new byte[size];
      }
   }

   public synchronized void write(int b) {
      int newcount = this.count + 1;
      if (newcount > this.buf.length) {
         this.buf = Arrays.copyOf(this.buf, Math.max(this.buf.length << 1, newcount));
      }

      this.buf[this.count] = (byte)b;
      this.count = newcount;
   }

   public synchronized void write(byte[] b, int off, int len) {
      if (off >= 0 && off <= b.length && len >= 0 && off + len <= b.length && off + len >= 0) {
         if (len != 0) {
            int newcount = this.count + len;
            if (newcount > this.buf.length) {
               this.buf = Arrays.copyOf(this.buf, Math.max(this.buf.length << 1, newcount));
            }

            System.arraycopy(b, off, this.buf, this.count, len);
            this.count = newcount;
         }
      } else {
         throw new IndexOutOfBoundsException();
      }
   }

   public synchronized void writeTo(OutputStream out) throws IOException {
      out.write(this.buf, 0, this.count);
   }

   public synchronized void reset() {
      this.count = 0;
   }

   public synchronized byte[] toByteArray() {
      return Arrays.copyOf(this.buf, this.count);
   }

   public synchronized int size() {
      return this.count;
   }

   public synchronized String toString() {
      return new String(this.buf, 0, this.count);
   }

   public synchronized String toString(String charsetName) throws UnsupportedEncodingException {
      return new String(this.buf, 0, this.count, charsetName);
   }

   /** @deprecated */
   @Deprecated
   public synchronized String toString(int hibyte) {
      return new String(this.buf, hibyte, 0, this.count);
   }

   public void close() throws IOException {
   }
}
