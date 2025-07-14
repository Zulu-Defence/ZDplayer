package com.zdplayer.fpvplayer.fpvlibrary.widget;

import android.text.TextUtils;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class SystemProperties {
   public static final int PROP_NAME_MAX = 31;
   public static final int PROP_VALUE_MAX = 91;
   private static Map<String, String> cache = new HashMap();

   public static String get(String key) {
      if (key.length() > 31) {
         throw new IllegalArgumentException("key.length > 31");
      } else {
         try {
            return native_get(key);
         } catch (Exception var2) {
            return "";
         }
      }
   }

   public static String get(String key, String def) {
      if (key.length() > 31) {
         throw new IllegalArgumentException("key.length > 31");
      } else {
         try {
            String ret = native_get(key);
            return TextUtils.isEmpty(ret) ? def : ret;
         } catch (Exception var3) {
            return def;
         }
      }
   }

   public static int getInt(String key, int def) {
      if (key.length() > 31) {
         throw new IllegalArgumentException("key.length > 31");
      } else {
         try {
            return Integer.parseInt(native_get(key));
         } catch (Exception var3) {
            return def;
         }
      }
   }

   public static long getLong(String key, long def) {
      if (key.length() > 31) {
         throw new IllegalArgumentException("key.length > 31");
      } else {
         try {
            return Long.parseLong(native_get(key));
         } catch (Exception var4) {
            return def;
         }
      }
   }

   public static boolean getBoolean(String key, boolean def) {
      if (key.length() > 31) {
         throw new IllegalArgumentException("key.length > 31");
      } else {
         try {
            return Boolean.parseBoolean(native_get(key));
         } catch (Exception var3) {
            return def;
         }
      }
   }

   private static String native_get(String key) throws Exception {
      if (cache.containsKey(key)) {
         return (String)cache.get(key);
      } else {
         Process p = Runtime.getRuntime().exec("/system/bin/getprop " + key);
         p.waitFor();
         BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
         String ln = reader.readLine();
         reader.close();
         p.destroy();
         cache.put(key, ln);
         return ln;
      }
   }
}
