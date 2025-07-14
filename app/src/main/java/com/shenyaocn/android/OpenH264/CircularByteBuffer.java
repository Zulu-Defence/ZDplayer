package com.shenyaocn.android.OpenH264;

public class CircularByteBuffer {
   private final byte[] buffer;
   private final int capacity;
   private int available;
   private int idxGet;
   private int idxPut;

   public CircularByteBuffer() {
      this(8192);
   }

   public CircularByteBuffer(int capacity) {
      this.capacity = capacity;
      this.buffer = new byte[this.capacity];
   }

   public synchronized void clear() {
      this.idxGet = this.idxPut = this.available = 0;
   }

   public synchronized int get() {
      if (this.available == 0) {
         return -1;
      } else {
         byte value = this.buffer[this.idxGet];
         this.idxGet = (this.idxGet + 1) % this.capacity;
         --this.available;
         return value;
      }
   }

   public int get(byte[] dst) {
      return this.get(dst, 0, dst.length);
   }

   public synchronized int get(byte[] dst, int off, int len) {
      if (this.available == 0) {
         return 0;
      } else {
         int limit = this.idxGet < this.idxPut ? this.idxPut : this.capacity;
         int count = Math.min(limit - this.idxGet, len);
         System.arraycopy(this.buffer, this.idxGet, dst, off, count);
         this.idxGet += count;
         if (this.idxGet == this.capacity) {
            int count2 = Math.min(len - count, this.idxPut);
            if (count2 > 0) {
               System.arraycopy(this.buffer, 0, dst, off + count, count2);
               this.idxGet = count2;
               count += count2;
            } else {
               this.idxGet = 0;
            }
         }

         this.available -= count;
         return count;
      }
   }

   public synchronized boolean put(byte value) {
      if (this.available == this.capacity) {
         return false;
      } else {
         this.buffer[this.idxPut] = value;
         this.idxPut = (this.idxPut + 1) % this.capacity;
         ++this.available;
         return true;
      }
   }

   public int put(byte[] src) {
      return this.put(src, 0, src.length);
   }

   public synchronized int put(byte[] src, int off, int len) {
      if (this.available == this.capacity) {
         return 0;
      } else {
         int limit = this.idxPut < this.idxGet ? this.idxGet : this.capacity;
         int count = Math.min(limit - this.idxPut, len);
         System.arraycopy(src, off, this.buffer, this.idxPut, count);
         this.idxPut += count;
         if (this.idxPut == this.capacity) {
            int count2 = Math.min(len - count, this.idxGet);
            if (count2 > 0) {
               System.arraycopy(src, off + count, this.buffer, 0, count2);
               this.idxPut = count2;
               count += count2;
            } else {
               this.idxPut = 0;
            }
         }

         this.available += count;
         return count;
      }
   }

   public synchronized int peek() {
      return this.available > 0 ? this.buffer[this.idxGet] : -1;
   }

   public synchronized int skip(int count) {
      if (count > this.available) {
         count = this.available;
      }

      this.idxGet = (this.idxGet + count) % this.capacity;
      this.available -= count;
      return count;
   }

   public int capacity() {
      return this.capacity;
   }

   public synchronized int available() {
      return this.available;
   }

   public synchronized int free() {
      return this.capacity - this.available;
   }
}
