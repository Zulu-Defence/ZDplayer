package com.zdplayer.fpvplayer.fpvlibrary.socket;

import android.util.Log;
import com.zdplayer.fpvplayer.fpvlibrary.SDKInit;
import com.zdplayer.fpvplayer.fpvlibrary.base.BaseSerialPortControl;
import com.zdplayer.fpvplayer.fpvlibrary.utils.String2ByteArrayUtils;

public class SocketControl extends BaseSerialPortControl {
   private SocketConnection mSocketConnection;

   public SocketControl(SocketConnection connection) {
      this.mSocketConnection = connection;
   }

   public void sendCMDData(String method, String cmd) {
      if (SDKInit.getInstance().isDebug()) {
         Log.d(SDKInit.getInstance().getTAG(), "method=> " + method + ";txt:" + cmd + ";hex: " + String2ByteArrayUtils.encodeHexStr(cmd.getBytes()));
      }

      if (this.mSocketConnection != null) {
         this.mSocketConnection.sendData(cmd.getBytes());
      }

   }

   public void sendCMDData(String method, byte[] cmd) {
      if (SDKInit.getInstance().isDebug()) {
         Log.d(SDKInit.getInstance().getTAG(), "method=> " + method + ";txt:" + new String(cmd) + ";hex: " + String2ByteArrayUtils.encodeHexStr(cmd));
      }

      if (this.mSocketConnection != null) {
         this.mSocketConnection.sendData(cmd);
      }

   }
}
