package com.zdplayer.fpvplayer.android.usbserial.driver;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.SparseArray;

import com.zdplayer.fpvplayer.android.usbserial.driver.CdcAcmSerialDriver;
import com.zdplayer.fpvplayer.android.usbserial.driver.Cp2102SerialDriver;
import com.zdplayer.fpvplayer.android.usbserial.driver.UsbSerialDriver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public enum UsbSerialProber {
   CDC_ACM_SERIAL {
      public List<com.zdplayer.fpvplayer.android.usbserial.driver.UsbSerialDriver> probe(UsbManager manager, UsbDevice usbDevice) {
         if (!UsbSerialProber.testIfSupported(usbDevice, CdcAcmSerialDriver.getSupportedDevices())) {
            return Collections.emptyList();
         } else {
            UsbDeviceConnection connection = manager.hasPermission(usbDevice) ? manager.openDevice(usbDevice) : null;
            if (connection == null) {
               return Collections.emptyList();
            } else {
               com.zdplayer.fpvplayer.android.usbserial.driver.UsbSerialDriver driver = new CdcAcmSerialDriver(usbDevice, connection);
               return Collections.singletonList(driver);
            }
         }
      }

      public SparseArray<int[]> getSupportedDevices() {
         return CdcAcmSerialDriver.getSupportedDevices();
      }
   },
   SILAB_SERIAL {
      public List<com.zdplayer.fpvplayer.android.usbserial.driver.UsbSerialDriver> probe(UsbManager manager, UsbDevice usbDevice) {
         if (!UsbSerialProber.testIfSupported(usbDevice, Cp2102SerialDriver.getSupportedDevices())) {
            return Collections.emptyList();
         } else {
            UsbDeviceConnection connection = manager.hasPermission(usbDevice) ? manager.openDevice(usbDevice) : null;
            if (connection == null) {
               return Collections.emptyList();
            } else {
               com.zdplayer.fpvplayer.android.usbserial.driver.UsbSerialDriver driver = new Cp2102SerialDriver(usbDevice, connection);
               return Collections.singletonList(driver);
            }
         }
      }

      public SparseArray<int[]> getSupportedDevices() {
         return Cp2102SerialDriver.getSupportedDevices();
      }
   };

   private UsbSerialProber() {
   }

   protected abstract List<com.zdplayer.fpvplayer.android.usbserial.driver.UsbSerialDriver> probe(UsbManager var1, UsbDevice var2);

   protected abstract SparseArray<int[]> getSupportedDevices();

   public static com.zdplayer.fpvplayer.android.usbserial.driver.UsbSerialDriver findFirstDevice(UsbManager usbManager) {
      Iterator var1 = usbManager.getDeviceList().values().iterator();

      while(var1.hasNext()) {
         UsbDevice usbDevice = (UsbDevice)var1.next();
         UsbSerialProber[] var3 = values();
         int var4 = var3.length;

         for(int var5 = 0; var5 < var4; ++var5) {
            UsbSerialProber prober = var3[var5];
            List<com.zdplayer.fpvplayer.android.usbserial.driver.UsbSerialDriver> probedDevices = prober.probe(usbManager, usbDevice);
            if (!probedDevices.isEmpty()) {
               return (com.zdplayer.fpvplayer.android.usbserial.driver.UsbSerialDriver)probedDevices.get(0);
            }
         }
      }

      return null;
   }

   public static com.zdplayer.fpvplayer.android.usbserial.driver.UsbSerialDriver openUsbDevice(UsbManager usbManager, UsbDevice device) {
      UsbSerialProber[] var2 = values();
      int var3 = var2.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         UsbSerialProber prober = var2[var4];
         List<com.zdplayer.fpvplayer.android.usbserial.driver.UsbSerialDriver> probedDevices = prober.probe(usbManager, device);
         if (!probedDevices.isEmpty()) {
            return (com.zdplayer.fpvplayer.android.usbserial.driver.UsbSerialDriver)probedDevices.get(0);
         }
      }

      return null;
   }

   public static List<UsbDevice> getAvailableSupportedDevices(UsbManager usbManager) {
      List<UsbDevice> supportedDevices = new ArrayList();
      Iterator var2 = usbManager.getDeviceList().values().iterator();

      while(true) {
         while(var2.hasNext()) {
            UsbDevice usbDevice = (UsbDevice)var2.next();
            UsbSerialProber[] var4 = values();
            int var5 = var4.length;

            for(int var6 = 0; var6 < var5; ++var6) {
               UsbSerialProber prober = var4[var6];
               if (testIfSupported(usbDevice, prober.getSupportedDevices())) {
                  supportedDevices.add(usbDevice);
                  break;
               }
            }
         }

         return supportedDevices;
      }
   }

   public static List<com.zdplayer.fpvplayer.android.usbserial.driver.UsbSerialDriver> findAllDevices(UsbManager usbManager) {
      List<com.zdplayer.fpvplayer.android.usbserial.driver.UsbSerialDriver> result = new ArrayList();
      Iterator var2 = usbManager.getDeviceList().values().iterator();

      while(var2.hasNext()) {
         UsbDevice usbDevice = (UsbDevice)var2.next();
         result.addAll(probeSingleDevice(usbManager, usbDevice));
      }

      return result;
   }

   public static List<com.zdplayer.fpvplayer.android.usbserial.driver.UsbSerialDriver> probeSingleDevice(UsbManager usbManager, UsbDevice usbDevice) {
      List<com.zdplayer.fpvplayer.android.usbserial.driver.UsbSerialDriver> result = new ArrayList();
      UsbSerialProber[] var3 = values();
      int var4 = var3.length;

      for(int var5 = 0; var5 < var4; ++var5) {
         UsbSerialProber prober = var3[var5];
         List<com.zdplayer.fpvplayer.android.usbserial.driver.UsbSerialDriver> probedDevices = prober.probe(usbManager, usbDevice);
         result.addAll(probedDevices);
      }

      return result;
   }

   /** @deprecated */
   @Deprecated
   public static com.zdplayer.fpvplayer.android.usbserial.driver.UsbSerialDriver acquire(UsbManager usbManager) {
      return findFirstDevice(usbManager);
   }

   /** @deprecated */
   @Deprecated
   public static com.zdplayer.fpvplayer.android.usbserial.driver.UsbSerialDriver acquire(UsbManager usbManager, UsbDevice usbDevice) {
      List<com.zdplayer.fpvplayer.android.usbserial.driver.UsbSerialDriver> probedDevices = probeSingleDevice(usbManager, usbDevice);
      return !probedDevices.isEmpty() ? (UsbSerialDriver)probedDevices.get(0) : null;
   }

   private static boolean testIfSupported(UsbDevice usbDevice, SparseArray<int[]> supportedDevices) {
      int[] supportedProducts = (int[])supportedDevices.get(usbDevice.getVendorId());
      if (supportedProducts == null) {
         return false;
      } else {
         int productId = usbDevice.getProductId();
         int[] var4 = supportedProducts;
         int var5 = supportedProducts.length;

         for(int var6 = 0; var6 < var5; ++var6) {
            int supportedProductId = var4[var6];
            if (productId == supportedProductId) {
               return true;
            }
         }

         return false;
      }
   }

   // $FF: synthetic method
   UsbSerialProber(Object x2) {
      this();
   }
}
