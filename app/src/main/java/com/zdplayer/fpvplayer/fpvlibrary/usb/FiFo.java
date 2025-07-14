package com.zdplayer.fpvplayer.fpvlibrary.usb;

public class FiFo {
   private int FIFO_SIZE;
   private byte[] buffer;
   private int front = 0;
   private int tail = 0;
   private boolean isEmpty = true;
   private boolean isFull = false;
   private Object lock = new Object();

   public FiFo() {
      this.FIFO_SIZE = 4194304;
      this.buffer = new byte[this.FIFO_SIZE];
   }

   public FiFo(int fifoSize) {
      this.FIFO_SIZE = fifoSize;
      this.buffer = new byte[this.FIFO_SIZE];
   }

   public int read(byte[] data, int length) {
      synchronized(this.lock) {
         int bufSize = this.getActualSize();
         if (length >= 1 && !this.isEmpty && bufSize != 0) {
            int count;
            if (bufSize > length) {
               count = length;
               this.isEmpty = false;
            } else {
               count = bufSize;
               this.isEmpty = true;
            }

            if (this.isFull) {
               this.isFull = false;
            }

            if (this.tail > this.front) {
               System.arraycopy(this.buffer, this.front, data, 0, count);
               this.front += count;
            } else {
               if (count > this.FIFO_SIZE - this.front) {
                  System.arraycopy(this.buffer, this.front, data, 0, this.FIFO_SIZE - this.front);
                  System.arraycopy(this.buffer, 0, data, this.FIFO_SIZE - this.front, count - (this.FIFO_SIZE - this.front));
               } else {
                  System.arraycopy(this.buffer, this.front, data, 0, count);
               }

               this.front = this.front + count >= this.FIFO_SIZE ? this.front + count - this.FIFO_SIZE : this.front + count;
            }

            return count;
         } else {
            return 0;
         }
      }
   }

   public int write(byte[] data, int length) {
      synchronized(this.lock) {
//         int count = false;
         int bufSize = this.getActualSize();
         if (length >= 1 && !this.isFull) {
            int count;
            if (this.FIFO_SIZE - bufSize > length) {
               count = length;
               this.isFull = false;
            } else {
               count = this.FIFO_SIZE - bufSize;
               this.isFull = true;
            }

            if (this.isEmpty) {
               this.isEmpty = false;
            }

            if (this.tail >= this.front) {
               if (this.FIFO_SIZE - this.tail >= count) {
                  System.arraycopy(data, 0, this.buffer, this.tail, count);
                  this.tail = this.tail + count >= this.FIFO_SIZE ? 0 : this.tail + count;
               } else {
                  System.arraycopy(data, 0, this.buffer, this.tail, this.FIFO_SIZE - this.tail);
                  System.arraycopy(data, this.FIFO_SIZE - this.tail, this.buffer, 0, count - (this.FIFO_SIZE - this.tail));
                  this.tail = this.tail + count - this.FIFO_SIZE;
               }
            } else {
               System.arraycopy(data, 0, this.buffer, this.tail, count);
               this.tail = this.tail + count >= this.FIFO_SIZE ? this.tail + count - this.FIFO_SIZE : this.tail + count;
            }

            return count;
         } else {
            this.isFull = true;
            return 0;
         }
      }
   }

   public int getActualSize() {
      synchronized(this.lock) {
         if (this.isEmpty) {
            return 0;
         } else {
            return this.tail >= this.front ? this.tail - this.front : this.FIFO_SIZE - (this.front - this.tail);
         }
      }
   }
}
