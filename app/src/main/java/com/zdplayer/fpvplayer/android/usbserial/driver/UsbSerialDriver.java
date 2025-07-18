package com.zdplayer.fpvplayer.android.usbserial.driver;

import java.io.IOException;

public interface UsbSerialDriver {
   int DATABITS_5 = 5;
   int DATABITS_6 = 6;
   int DATABITS_7 = 7;
   int DATABITS_8 = 8;
   int FLOWCONTROL_NONE = 0;
   int FLOWCONTROL_RTSCTS_IN = 1;
   int FLOWCONTROL_RTSCTS_OUT = 2;
   int FLOWCONTROL_XONXOFF_IN = 4;
   int FLOWCONTROL_XONXOFF_OUT = 8;
   int PARITY_NONE = 0;
   int PARITY_ODD = 1;
   int PARITY_EVEN = 2;
   int PARITY_MARK = 3;
   int PARITY_SPACE = 4;
   int STOPBITS_1 = 1;
   int STOPBITS_1_5 = 3;
   int STOPBITS_2 = 2;

   void open() throws IOException;

   void close() throws IOException;

   int read(byte[] var1, int var2) throws IOException;

   int write(byte[] var1, int var2) throws IOException;

   void setParameters(int var1, int var2, int var3, int var4) throws IOException;

   boolean getCD() throws IOException;

   boolean getCTS() throws IOException;

   boolean getDSR() throws IOException;

   boolean getDTR() throws IOException;

   void setDTR(boolean var1) throws IOException;

   boolean getRI() throws IOException;

   boolean getRTS() throws IOException;

   void setRTS(boolean var1) throws IOException;
}
