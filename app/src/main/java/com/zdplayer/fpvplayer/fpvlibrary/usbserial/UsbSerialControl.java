package com.zdplayer.fpvplayer.fpvlibrary.usbserial;

import android.util.Log;
import com.zdplayer.fpvplayer.fpvlibrary.SDKInit;
import com.zdplayer.fpvplayer.fpvlibrary.base.BaseSerialPortControl;
import com.zdplayer.fpvplayer.fpvlibrary.utils.String2ByteArrayUtils;

public class UsbSerialControl extends BaseSerialPortControl {
   private UsbSerialConnection mUsbSerialConnection;

   public UsbSerialControl(UsbSerialConnection connection) {
      this.mUsbSerialConnection = connection;
   }

   public void sendCMDData(String method, String cmd) {
      if (SDKInit.getInstance().isDebug()) {
         Log.d(SDKInit.getInstance().getTAG(), "method=> " + method + "; txt:" + cmd + ";hex: " + String2ByteArrayUtils.encodeHexStr(cmd.getBytes()));
      }

      this.sendVideo(cmd.getBytes());
   }

   public void sendCMDData(String method, byte[] cmd) {
      if (SDKInit.getInstance().isDebug()) {
         Log.d(SDKInit.getInstance().getTAG(), "method=> " + method + ";txt:" + new String(cmd) + ";hex: " + String2ByteArrayUtils.encodeHexStr(cmd));
      }

      this.sendVideo(cmd);
   }

   public void sendDebug(byte[] payload) {
      if (SDKInit.getInstance().isDebug()) {
         Log.d(SDKInit.getInstance().getTAG(), "method:" + String2ByteArrayUtils.encodeHexStr(payload));
      }

      if (this.mUsbSerialConnection != null) {
         this.mUsbSerialConnection.putPayload((byte)-95, payload);
      }

   }

   public void sendCMDData(byte[] payload) {
      if (this.mUsbSerialConnection != null) {
         this.mUsbSerialConnection.putPayload((byte)-93, payload);
      }

   }

   public void sendVideo(byte[] payload) {
      if (this.mUsbSerialConnection != null) {
         this.mUsbSerialConnection.putPayload((byte)-91, payload);
      }

   }

   public void sendGPS(byte[] payload) {
      if (this.mUsbSerialConnection != null) {
         this.mUsbSerialConnection.putPayload((byte)-89, payload);
      }

   }
}
