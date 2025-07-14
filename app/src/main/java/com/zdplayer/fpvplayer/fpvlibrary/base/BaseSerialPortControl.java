package com.zdplayer.fpvplayer.fpvlibrary.base;

import com.zdplayer.fpvplayer.fpvlibrary.enums.PTZAction;
import com.zdplayer.fpvplayer.fpvlibrary.enums.Sizes;
import java.text.SimpleDateFormat;
import java.util.Locale;

public abstract class BaseSerialPortControl {
   protected int lastExposureTime = 8192;
   protected int lastExposureMode = 0;
   protected boolean LEDOn = false;
   protected boolean isReCord = false;

   public boolean toggleReCord() {
      if (this.isReCord) {
         this.atAzP(1);
         this.isReCord = false;
      } else {
         this.atAzP(0);
         this.isReCord = true;
      }

      return this.isReCord;
   }

   public void toggleReCord(boolean reCord) {
      if (!reCord) {
         this.atAzP(1);
      } else {
         this.atAzP(0);
      }

      this.isReCord = reCord;
   }

   public boolean getReCord() {
      return this.isReCord;
   }

   public void snapshot() {
      this.atAzP(2);
   }

   public void atAzP(int p) {
      String cmd = String.format(Locale.ENGLISH, "AT+AZ -p%d\r\n", p);
      this.sendCMDData("setTime", cmd);
   }

   public void setTime(long timestamp_ms) {
      SimpleDateFormat form = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      String cmd = String.format(Locale.ENGLISH, "AT+DATE -s%s\r\n", form.format(timestamp_ms));
      this.sendCMDData("setTime", cmd);
   }

   public void calibrateTripod() {
      String cmd = "AT+ANGLE -C1\r\n";
      this.sendCMDData("calibrateTripod", cmd);
   }

   public void controlDirection(PTZAction action) {
      String cmd = "";
      switch(action) {
      case LEFT:
         cmd = "AT+ANGLE -Z0\r\n";
         break;
      case TOP:
         cmd = "AT+ANGLE -X1\r\n";
         break;
      case RIGHT:
         cmd = "AT+ANGLE -Z1\r\n";
         break;
      case BOTTOM:
         cmd = "AT+ANGLE -X0\r\n";
      }

      this.sendCMDData("controlDirection", cmd);
   }

   public void flip(boolean flip) {
      String cmd = flip ? "AT+VIDEO -x1 -y1\n" : "AT+VIDEO -x0 -y0\n";
      this.sendCMDData("flip", cmd);
   }

   public void AkeyControl(PTZAction action) {
      String cmd = "";
      switch(action) {
      case UP:
         cmd = "AT+ANGLE -P180\r\n";
         break;
      case FRONT:
         cmd = "AT+ANGLE -P90\r\n";
         break;
      case DOWN:
         cmd = "AT+ANGLE -P0\r\n";
      }

      this.sendCMDData("AkeyControl", cmd);
   }

   public void setResolution(Sizes sizes) {
      String str = "";
      switch(sizes) {
      case Size_320x240:
         str = "AT+VIDEO -m0 -p1 -f15 -b300 -e1 -g8\r\n";
         break;
      case Size_640x480:
         str = "AT+VIDEO -m0 -p2 -f15 -b600 -e1 -g8\n";
         break;
      case Size_640x480_900k:
         str = "AT+VIDEO -m0 -p2 -f15 -b900 -e1 -g8\n";
         break;
      case Size_1280x720_1500k:
         str = "AT+VIDEO -m0 -p2 -f15 -b1200 -e1 -g8\n";
      }

      String cmd = str + String.format(Locale.US, "AT+AE -o%d -i1 -h1 -r0\r\nAT+AEM -a%d -b0 -c0 -d0 -e%d -f1024 -g1024 -h1024\r\n", this.lastExposureMode, this.lastExposureMode, this.lastExposureTime);
      this.sendCMDData("setResolution", cmd);
   }

   public void setExposureTime(int time, boolean auto) {
      int a = auto ? 0 : 1;
      String cmd = String.format(Locale.US, "AT+AE -o%d -i1 -h1 -r0\r\nAT+AEM -a%d -b0 -c0 -d0 -e%d -f1024 -g1024 -h1024\r\n", a, a, time);
      this.sendCMDData("setExposureTime", cmd);
      this.lastExposureTime = time;
      this.lastExposureMode = a;
   }

   public void setCSC(int brightness, int contrast, int hue, int saturation) {
      String cmd = String.format(Locale.US, "AT+CSC -l%d -c%d -h%d -s%d\r\n", brightness, contrast, hue, saturation);
      this.sendCMDData("setCSC", cmd);
   }

   public void toggleLED() {
      this.LEDOn = !this.LEDOn;
      String cmd = this.LEDOn ? "AT+LED -e1\r\n" : "AT+LED -e0\r\n";
      this.sendCMDData("toggleLED", cmd);
   }

   public void toggleLED(boolean offON) {
      this.LEDOn = offON;
      String cmd = offON ? "AT+LED -e1\r\n" : "AT+LED -e0\r\n";
      this.sendCMDData("toggleLED", cmd);
   }

   public void switchCamera() {
      String cmd = "AT+SWITCH -e1\r\n";
      this.sendCMDData("switchCamera", cmd);
   }

   public void switch2FrontCamera() {
      String cmd = "AT+SWITCH -d0\r\n";
      this.sendCMDData("switch2FrontCamera", cmd);
   }

   public void switch2AttachedCamera() {
      String cmd = "AT+SWITCH -d1\r\n";
      this.sendCMDData("switch2AttachedCamera", cmd);
   }

   public abstract void sendCMDData(String var1, String var2);

   public abstract void sendCMDData(String var1, byte[] var2);
}
