package com.zdplayer.fpvplayer.android.usbserial;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.content.res.Resources.NotFoundException;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbInterface;
import android.text.TextUtils;
import android.util.Log;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public final class DeviceFilter {
   private static final String TAG = "DeviceFilter";
   public final int mVendorId;
   public final int mProductId;
   public final int mClass;
   public final int mSubclass;
   public final int mProtocol;
   public final String mManufacturerName;
   public final String mProductName;
   public final String mSerialNumber;

   public DeviceFilter(int var1, int var2, int var3, int var4, int var5, String var6, String var7, String var8) {
      this.mVendorId = var1;
      this.mProductId = var2;
      this.mClass = var3;
      this.mSubclass = var4;
      this.mProtocol = var5;
      this.mManufacturerName = var6;
      this.mProductName = var7;
      this.mSerialNumber = var8;
   }

   public DeviceFilter(UsbDevice var1) {
      this.mVendorId = var1.getVendorId();
      this.mProductId = var1.getProductId();
      this.mClass = var1.getDeviceClass();
      this.mSubclass = var1.getDeviceSubclass();
      this.mProtocol = var1.getDeviceProtocol();
      this.mManufacturerName = null;
      this.mProductName = null;
      this.mSerialNumber = null;
   }

   public static List<DeviceFilter> getDeviceFilters(Context var0, int var1) {
      XmlResourceParser var2 = var0.getResources().getXml(var1);
      ArrayList var3 = new ArrayList();

      try {
         for(int var4 = var2.getEventType(); var4 != 1; var4 = var2.next()) {
            if (var4 == 2) {
               DeviceFilter var5 = read(var0, var2);
               if (var5 != null) {
                  var3.add(var5);
               }
            }
         }
      } catch (XmlPullParserException var6) {
         Log.d("DeviceFilter", "XmlPullParserException", var6);
      } catch (IOException var7) {
         Log.d("DeviceFilter", "IOException", var7);
      }

      return Collections.unmodifiableList(var3);
   }

   private static final int getAttributeInteger(Context var0, XmlPullParser var1, String var2, String var3, int var4) {
      int var5 = var4;

      try {
         String var6 = var1.getAttributeValue(var2, var3);
         if (!TextUtils.isEmpty(var6) && var6.startsWith("@")) {
            String var12 = var6.substring(1);
            int var8 = var0.getResources().getIdentifier(var12, (String)null, var0.getPackageName());
            if (var8 > 0) {
               var5 = var0.getResources().getInteger(var8);
            }
         } else {
            byte var7 = 10;
            if (var6 != null && var6.length() > 2 && var6.charAt(0) == '0' && (var6.charAt(1) == 'x' || var6.charAt(1) == 'X')) {
               var7 = 16;
               var6 = var6.substring(2);
            }

            var5 = Integer.parseInt(var6, var7);
         }
      } catch (NotFoundException var9) {
         var5 = var4;
      } catch (NumberFormatException var10) {
         var5 = var4;
      } catch (NullPointerException var11) {
         var5 = var4;
      }

      return var5;
   }

   private static final String getAttributeString(Context var0, XmlPullParser var1, String var2, String var3, String var4) {
      String var5;
      try {
         var5 = var1.getAttributeValue(var2, var3);
         if (var5 == null) {
            var5 = var4;
         }

         if (!TextUtils.isEmpty(var5) && var5.startsWith("@")) {
            String var6 = var5.substring(1);
            int var7 = var0.getResources().getIdentifier(var6, (String)null, var0.getPackageName());
            if (var7 > 0) {
               var5 = var0.getResources().getString(var7);
            }
         }
      } catch (NotFoundException var8) {
         var5 = var4;
      } catch (NumberFormatException var9) {
         var5 = var4;
      } catch (NullPointerException var10) {
         var5 = var4;
      }

      return var5;
   }

   public static DeviceFilter read(Context var0, XmlPullParser var1) {
      int var2 = -1;
      int var3 = -1;
      int var4 = -1;
      int var5 = -1;
      int var6 = -1;
      String var7 = null;
      String var8 = null;
      String var9 = null;
      boolean var10 = false;

      try {
         for(int var12 = var1.getEventType(); var12 != 1; var12 = var1.next()) {
            String var11 = var1.getName();
            if (!TextUtils.isEmpty(var11) && var11.equalsIgnoreCase("usb-device")) {
               if (var12 == 2) {
                  var10 = true;
                  var2 = getAttributeInteger(var0, var1, (String)null, "vendor-id", -1);
                  if (var2 == -1) {
                     var2 = getAttributeInteger(var0, var1, (String)null, "vendorId", -1);
                     if (var2 == -1) {
                        var2 = getAttributeInteger(var0, var1, (String)null, "venderId", -1);
                     }
                  }

                  var3 = getAttributeInteger(var0, var1, (String)null, "product-id", -1);
                  if (var3 == -1) {
                     var3 = getAttributeInteger(var0, var1, (String)null, "productId", -1);
                  }

                  var4 = getAttributeInteger(var0, var1, (String)null, "class", -1);
                  var5 = getAttributeInteger(var0, var1, (String)null, "subclass", -1);
                  var6 = getAttributeInteger(var0, var1, (String)null, "protocol", -1);
                  var7 = getAttributeString(var0, var1, (String)null, "manufacturer-name", (String)null);
                  if (TextUtils.isEmpty(var7)) {
                     var7 = getAttributeString(var0, var1, (String)null, "manufacture", (String)null);
                  }

                  var8 = getAttributeString(var0, var1, (String)null, "product-name", (String)null);
                  if (TextUtils.isEmpty(var8)) {
                     var8 = getAttributeString(var0, var1, (String)null, "product", (String)null);
                  }

                  var9 = getAttributeString(var0, var1, (String)null, "serial-number", (String)null);
                  if (TextUtils.isEmpty(var9)) {
                     var9 = getAttributeString(var0, var1, (String)null, "serial", (String)null);
                  }
               } else if (var12 == 3 && var10) {
                  return new DeviceFilter(var2, var3, var4, var5, var6, var7, var8, var9);
               }
            }
         }
      } catch (Exception var13) {
         var13.printStackTrace();
      }

      return null;
   }

   private boolean matches(int var1, int var2, int var3) {
      return (this.mClass == -1 || var1 == this.mClass) && (this.mSubclass == -1 || var2 == this.mSubclass) && (this.mProtocol == -1 || var3 == this.mProtocol);
   }

   public boolean matches(UsbDevice var1) {
      if (this.mVendorId != -1 && var1.getVendorId() != this.mVendorId) {
         return false;
      } else if (this.mProductId != -1 && var1.getProductId() != this.mProductId) {
         return false;
      } else if (this.matches(var1.getDeviceClass(), var1.getDeviceSubclass(), var1.getDeviceProtocol())) {
         return true;
      } else {
         int var2 = var1.getInterfaceCount();

         for(int var3 = 0; var3 < var2; ++var3) {
            UsbInterface var4 = var1.getInterface(var3);
            if (this.matches(var4.getInterfaceClass(), var4.getInterfaceSubclass(), var4.getInterfaceProtocol())) {
               return true;
            }
         }

         return false;
      }
   }

   public boolean matches(DeviceFilter var1) {
      if (this.mVendorId != -1 && var1.mVendorId != this.mVendorId) {
         return false;
      } else if (this.mProductId != -1 && var1.mProductId != this.mProductId) {
         return false;
      } else if (var1.mManufacturerName != null && this.mManufacturerName == null) {
         return false;
      } else if (var1.mProductName != null && this.mProductName == null) {
         return false;
      } else if (var1.mSerialNumber != null && this.mSerialNumber == null) {
         return false;
      } else if (this.mManufacturerName != null && var1.mManufacturerName != null && !this.mManufacturerName.equals(var1.mManufacturerName)) {
         return false;
      } else if (this.mProductName != null && var1.mProductName != null && !this.mProductName.equals(var1.mProductName)) {
         return false;
      } else {
         return this.mSerialNumber != null && var1.mSerialNumber != null && !this.mSerialNumber.equals(var1.mSerialNumber) ? false : this.matches(var1.mClass, var1.mSubclass, var1.mProtocol);
      }
   }

   public boolean equals(Object var1) {
      if (this.mVendorId != -1 && this.mProductId != -1 && this.mClass != -1 && this.mSubclass != -1 && this.mProtocol != -1) {
         if (!(var1 instanceof DeviceFilter)) {
            if (!(var1 instanceof UsbDevice)) {
               return false;
            } else {
               UsbDevice var3 = (UsbDevice)var1;
               return var3.getVendorId() == this.mVendorId && var3.getProductId() == this.mProductId && var3.getDeviceClass() == this.mClass && var3.getDeviceSubclass() == this.mSubclass && var3.getDeviceProtocol() == this.mProtocol;
            }
         } else {
            DeviceFilter var2 = (DeviceFilter)var1;
            if (var2.mVendorId == this.mVendorId && var2.mProductId == this.mProductId && var2.mClass == this.mClass && var2.mSubclass == this.mSubclass && var2.mProtocol == this.mProtocol) {
               if (var2.mManufacturerName != null && this.mManufacturerName == null || var2.mManufacturerName == null && this.mManufacturerName != null || var2.mProductName != null && this.mProductName == null || var2.mProductName == null && this.mProductName != null || var2.mSerialNumber != null && this.mSerialNumber == null || var2.mSerialNumber == null && this.mSerialNumber != null) {
                  return false;
               } else {
                  return (var2.mManufacturerName == null || this.mManufacturerName == null || this.mManufacturerName.equals(var2.mManufacturerName)) && (var2.mProductName == null || this.mProductName == null || this.mProductName.equals(var2.mProductName)) && (var2.mSerialNumber == null || this.mSerialNumber == null || this.mSerialNumber.equals(var2.mSerialNumber));
               }
            } else {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   public int hashCode() {
      return (this.mVendorId << 16 | this.mProductId) ^ (this.mClass << 16 | this.mSubclass << 8 | this.mProtocol);
   }

   public String toString() {
      return "DeviceFilter[mVendorId=" + this.mVendorId + ",mProductId=" + this.mProductId + ",mClass=" + this.mClass + ",mSubclass=" + this.mSubclass + ",mProtocol=" + this.mProtocol + ",mManufacturerName=" + this.mManufacturerName + ",mProductName=" + this.mProductName + ",mSerialNumber=" + this.mSerialNumber + "]";
   }
}
