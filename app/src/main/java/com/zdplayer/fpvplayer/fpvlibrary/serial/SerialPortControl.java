package com.zdplayer.fpvplayer.fpvlibrary.serial;

import android.util.Log;
import com.zdplayer.fpvplayer.fpvlibrary.SDKInit;
import com.zdplayer.fpvplayer.fpvlibrary.base.BaseSerialPortControl;
import com.zdplayer.fpvplayer.fpvlibrary.utils.String2ByteArrayUtils;

public class SerialPortControl extends BaseSerialPortControl {
   private SerialPortConnection mSerialPortConnection;

   public SerialPortControl(SerialPortConnection connection) {
      this.mSerialPortConnection = connection;
   }

   public void sendCMDData(String method, String cmd) {
      if (SDKInit.getInstance().isDebug()) {
         Log.d(SDKInit.getInstance().getTAG(), "method=> " + method + ";txt:" + cmd + ";hex: " + String2ByteArrayUtils.encodeHexStr(cmd.getBytes()));
      }

      if (this.mSerialPortConnection != null) {
         this.mSerialPortConnection.sendData(cmd.getBytes());
      }

   }

   public void sendCMDData(String method, byte[] cmd) {
      if (SDKInit.getInstance().isDebug()) {
         Log.d(SDKInit.getInstance().getTAG(), "method=> " + method + ";txt:" + new String(cmd) + ";hex: " + String2ByteArrayUtils.encodeHexStr(cmd));
      }

      if (this.mSerialPortConnection != null) {
         this.mSerialPortConnection.sendData(cmd);
      }

   }
}
