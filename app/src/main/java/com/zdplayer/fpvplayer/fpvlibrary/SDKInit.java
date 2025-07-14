package com.zdplayer.fpvplayer.fpvlibrary;

public class SDKInit {
   private final String TAG = "SkydroidLog";
   private static SDKInit instance = new SDKInit();
   private boolean debug;

   private SDKInit() {
   }

   public static SDKInit getInstance() {
      return instance;
   }

   public void init(boolean debug) {
      this.debug = debug;
   }

   public Boolean isDebug() {
      return this.debug;
   }

   public String getTAG() {
      return "SkydroidLog";
   }
}
