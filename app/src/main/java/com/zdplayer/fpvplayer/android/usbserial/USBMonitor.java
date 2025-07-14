package com.zdplayer.fpvplayer.android.usbserial;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import com.zdplayer.fpvplayer.android.usbserial.DeviceFilter;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class USBMonitor {
   private static final boolean DEBUG = false;
   private static final String TAG = "USBMonitor";
   private static final String ACTION_USB_PERMISSION_BASE = "com.serenegiant.USB_PERMISSION.";
   private final String ACTION_USB_PERMISSION = "com.serenegiant.USB_PERMISSION." + this.hashCode();
   public static final String ACTION_USB_DEVICE_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED";
   private final ConcurrentHashMap<UsbDevice, UsbControlBlock> mCtrlBlocks = new ConcurrentHashMap();
   private final WeakReference<Context> mWeakContext;
   private final UsbManager mUsbManager;
   private final OnDeviceConnectListener mOnDeviceConnectListener;
   private PendingIntent mPermissionIntent = null;
   private List<DeviceFilter> mDeviceFilters = new ArrayList();
   private final Handler mHandler = new Handler();
   private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
      public void onReceive(Context context, Intent intent) {
         String action = intent.getAction();
         UsbDevice device;
         if (USBMonitor.this.ACTION_USB_PERMISSION.equals(action)) {
            synchronized(USBMonitor.this) {
               device = (UsbDevice)intent.getParcelableExtra("device");
               if (intent.getBooleanExtra("permission", false)) {
                  if (device != null) {
                     USBMonitor.this.processConnect(device);
                  }
               } else {
                  USBMonitor.this.processCancel(device);
               }
            }
         } else {
            UsbDevice usbDevice;
            if ("android.hardware.usb.action.USB_DEVICE_ATTACHED".equals(action)) {
               usbDevice = (UsbDevice)intent.getParcelableExtra("device");
               USBMonitor.this.processAttach(usbDevice);
            } else if ("android.hardware.usb.action.USB_DEVICE_DETACHED".equals(action)) {
               usbDevice = (UsbDevice)intent.getParcelableExtra("device");
               if (usbDevice != null) {
                  device = null;
                  UsbControlBlock var8 = (UsbControlBlock)USBMonitor.this.mCtrlBlocks.remove(usbDevice);
                  if (var8 != null) {
                     var8.close();
                  }

                  USBMonitor.this.mDeviceCounts = 0;
                  USBMonitor.this.processDettach(usbDevice);
               }
            }
         }

      }
   };
   private volatile int mDeviceCounts = 0;
   private final Runnable mDeviceCheckRunnable = new Runnable() {
      public void run() {
         List var1 = USBMonitor.this.getDeviceList();
         int var2 = var1.size();
         if (var2 != USBMonitor.this.mDeviceCounts && var2 > USBMonitor.this.mDeviceCounts) {
            USBMonitor.this.mDeviceCounts = var2;
            if (USBMonitor.this.mOnDeviceConnectListener != null) {
               USBMonitor.this.mOnDeviceConnectListener.onAttach((UsbDevice)null);
            }
         }

         USBMonitor.this.mHandler.postDelayed(this, 2000L);
      }
   };

   public USBMonitor(Context var1, OnDeviceConnectListener var2) {
      this.mWeakContext = new WeakReference(var1);
      this.mUsbManager = (UsbManager)var1.getSystemService("usb");
      this.mOnDeviceConnectListener = var2;
   }

   public void destroy() {
      this.unregister();
      Set var1 = this.mCtrlBlocks.keySet();
      if (var1 != null) {
         try {
            Iterator var3 = var1.iterator();

            while(var3.hasNext()) {
               UsbDevice var4 = (UsbDevice)var3.next();
               UsbControlBlock var2 = (UsbControlBlock)this.mCtrlBlocks.remove(var4);
               var2.close();
            }
         } catch (Exception var5) {
            Log.e("USBMonitor", "destroy:", var5);
         }

         this.mCtrlBlocks.clear();
      }

   }

   public synchronized void register() {
      if (this.mPermissionIntent == null) {
         Context context = (Context)this.mWeakContext.get();
         if (context != null) {
            this.mPermissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(this.ACTION_USB_PERMISSION), 33554432);
            IntentFilter intentFilter = new IntentFilter(this.ACTION_USB_PERMISSION);
            intentFilter.addAction("android.hardware.usb.action.USB_DEVICE_DETACHED");
            context.registerReceiver(this.mUsbReceiver, intentFilter);
         }

         this.mDeviceCounts = 0;
         this.mHandler.postDelayed(this.mDeviceCheckRunnable, 1000L);
      }

   }

   public synchronized void unregister() {
      if (this.mPermissionIntent != null) {
         Context var1 = (Context)this.mWeakContext.get();
         if (var1 != null) {
            var1.unregisterReceiver(this.mUsbReceiver);
         }

         this.mPermissionIntent = null;
      }

      this.mDeviceCounts = 0;
      this.mHandler.removeCallbacks(this.mDeviceCheckRunnable);
   }

   public synchronized boolean isRegistered() {
      return this.mPermissionIntent != null;
   }

   public void setDeviceFilter(DeviceFilter var1) {
      this.mDeviceFilters.clear();
      this.mDeviceFilters.add(var1);
   }

   public void setDeviceFilter(List<DeviceFilter> var1) {
      this.mDeviceFilters.clear();
      this.mDeviceFilters.addAll(var1);
   }

   public int getDeviceCount() {
      return this.getDeviceList().size();
   }

   public List<UsbDevice> getDeviceList() {
      return this.getDeviceList(this.mDeviceFilters);
   }

   public List<UsbDevice> getDeviceList(List<DeviceFilter> var1) {
      HashMap var2 = this.mUsbManager.getDeviceList();
      ArrayList var3 = new ArrayList();
      if (var2 != null) {
         Iterator var4 = var1.iterator();

         label28:
         while(var4.hasNext()) {
            DeviceFilter var5 = (DeviceFilter)var4.next();
            Iterator var6 = var2.values().iterator();

            while(true) {
               UsbDevice var7;
               do {
                  if (!var6.hasNext()) {
                     continue label28;
                  }

                  var7 = (UsbDevice)var6.next();
               } while(var5 != null && !var5.matches(var7));

               var3.add(var7);
            }
         }
      }

      return var3;
   }

   public List<UsbDevice> getDeviceList(DeviceFilter var1) {
      HashMap var2 = this.mUsbManager.getDeviceList();
      ArrayList var3 = new ArrayList();
      if (var2 == null) {
         return var3;
      } else {
         Iterator var4 = var2.values().iterator();

         while(true) {
            UsbDevice var5;
            do {
               if (!var4.hasNext()) {
                  return var3;
               }

               var5 = (UsbDevice)var4.next();
            } while(var1 != null && !var1.matches(var5));

            var3.add(var5);
         }
      }
   }

   public Iterator<UsbDevice> getDevices() {
      Iterator var1 = null;
      HashMap var2 = this.mUsbManager.getDeviceList();
      if (var2 != null) {
         var1 = var2.values().iterator();
      }

      return var1;
   }

   public final void dumpDevices() {
      HashMap var1 = this.mUsbManager.getDeviceList();
      if (var1 != null) {
         Set var2 = var1.keySet();
         if (var2 != null && var2.size() > 0) {
            StringBuilder var3 = new StringBuilder();
            Iterator var4 = var2.iterator();

            while(var4.hasNext()) {
               String var5 = (String)var4.next();
               UsbDevice var6 = (UsbDevice)var1.get(var5);
               int var7 = var6 != null ? var6.getInterfaceCount() : 0;
               var3.setLength(0);

               for(int var8 = 0; var8 < var7; ++var8) {
                  var3.append(String.format("interface%d:%s", var8, var6.getInterface(var8).toString()));
               }

               Log.i("USBMonitor", "key=" + var5 + ":" + var6 + ":" + var3.toString());
            }
         } else {
            Log.i("USBMonitor", "no device");
         }
      } else {
         Log.i("USBMonitor", "no device");
      }

   }

   public boolean hasPermission(UsbDevice var1) {
      return var1 != null && this.mUsbManager.hasPermission(var1);
   }

   public synchronized void requestPermission(UsbDevice var1) {
      if (this.mPermissionIntent != null) {
         if (var1 != null) {
            if (this.mUsbManager.hasPermission(var1)) {
               this.processConnect(var1);
            } else {
               this.mUsbManager.requestPermission(var1, this.mPermissionIntent);
            }
         } else {
            this.processCancel(var1);
         }
      } else {
         this.processCancel(var1);
      }

   }

   private final void processConnect(final UsbDevice var1) {
      this.mHandler.post(new Runnable() {
         public void run() {
            UsbControlBlock var1x = (UsbControlBlock)USBMonitor.this.mCtrlBlocks.get(var1);
            boolean var2;
            if (var1x == null) {
               var1x = new UsbControlBlock(USBMonitor.this, var1);
               USBMonitor.this.mCtrlBlocks.put(var1, var1x);
               var2 = true;
            } else {
               var2 = false;
            }

            if (USBMonitor.this.mOnDeviceConnectListener != null) {
               USBMonitor.this.mOnDeviceConnectListener.onConnect(var1, var1x, var2);
            }

         }
      });
   }

   private final void processCancel(UsbDevice var1) {
      if (this.mOnDeviceConnectListener != null) {
         this.mHandler.post(new Runnable() {
            public void run() {
               USBMonitor.this.mOnDeviceConnectListener.onCancel();
            }
         });
      }

   }

   private final void processAttach(final UsbDevice var1) {
      if (this.mOnDeviceConnectListener != null) {
         this.mHandler.post(new Runnable() {
            public void run() {
               USBMonitor.this.mOnDeviceConnectListener.onAttach(var1);
            }
         });
      }

   }

   private final void processDettach(final UsbDevice var1) {
      if (this.mOnDeviceConnectListener != null) {
         this.mHandler.post(new Runnable() {
            public void run() {
               USBMonitor.this.mOnDeviceConnectListener.onDettach(var1);
            }
         });
      }

   }

   public interface OnDeviceConnectListener {
      void onAttach(UsbDevice var1);

      void onDettach(UsbDevice var1);

      void onConnect(UsbDevice var1, UsbControlBlock var2, boolean var3);

      void onDisconnect(UsbDevice var1, UsbControlBlock var2);

      void onCancel();
   }

   public static final class UsbControlBlock {
      private final WeakReference<USBMonitor> mWeakMonitor;
      private final WeakReference<UsbDevice> mWeakDevice;
      protected UsbDeviceConnection mConnection;
      private final SparseArray<UsbInterface> mInterfaces = new SparseArray();
      private final int mBusNum;
      private final int mDevNum;

      public UsbControlBlock(USBMonitor var1, UsbDevice var2) {
         this.mWeakMonitor = new WeakReference(var1);
         this.mWeakDevice = new WeakReference(var2);
         this.mConnection = var1.mUsbManager.openDevice(var2);
         String var3 = var2.getDeviceName();
         String[] var4 = !TextUtils.isEmpty(var3) ? var3.split("/") : null;
         int var5 = 0;
         int var6 = 0;
         if (var4 != null) {
            var5 = Integer.parseInt(var4[var4.length - 2]);
            var6 = Integer.parseInt(var4[var4.length - 1]);
         }

         this.mBusNum = var5;
         this.mDevNum = var6;
         if (this.mConnection == null) {
            Log.e("USBMonitor", "could not connect to device " + var3);
         }

      }

      public UsbDevice getDevice() {
         return (UsbDevice)this.mWeakDevice.get();
      }

      public String getDeviceName() {
         UsbDevice var1 = (UsbDevice)this.mWeakDevice.get();
         return var1 != null ? var1.getDeviceName() : "";
      }

      public UsbDeviceConnection getUsbDeviceConnection() {
         return this.mConnection;
      }

      public synchronized int getFileDescriptor() {
         return this.mConnection != null ? this.mConnection.getFileDescriptor() : -1;
      }

      public byte[] getRawDescriptors() {
         return this.mConnection != null ? this.mConnection.getRawDescriptors() : null;
      }

      public int getVenderId() {
         UsbDevice var1 = (UsbDevice)this.mWeakDevice.get();
         return var1 != null ? var1.getVendorId() : 0;
      }

      public int getProductId() {
         UsbDevice var1 = (UsbDevice)this.mWeakDevice.get();
         return var1 != null ? var1.getProductId() : 0;
      }

      public synchronized String getSerial() {
         return this.mConnection != null ? this.mConnection.getSerial() : null;
      }

      public int getBusNum() {
         return this.mBusNum;
      }

      public int getDevNum() {
         return this.mDevNum;
      }

      public USBMonitor getUSBMonitor() {
         return (USBMonitor)this.mWeakMonitor.get();
      }

      public synchronized UsbInterface open(int var1) {
         UsbDevice var2 = (UsbDevice)this.mWeakDevice.get();
         UsbInterface var3 = null;
         var3 = (UsbInterface)this.mInterfaces.get(var1);
         if (var3 == null) {
            var3 = var2.getInterface(var1);
            if (var3 != null) {
               synchronized(this.mInterfaces) {
                  this.mInterfaces.append(var1, var3);
               }
            }
         }

         return var3;
      }

      public void close(int var1) {
         UsbInterface var2 = null;
         synchronized(this.mInterfaces) {
            var2 = (UsbInterface)this.mInterfaces.get(var1);
            if (var2 != null) {
               this.mInterfaces.delete(var1);
               this.mConnection.releaseInterface(var2);
            }

         }
      }

      private UsbControlBlock(UsbControlBlock var1) {
         USBMonitor var2 = var1.getUSBMonitor();
         UsbDevice var3 = var1.getDevice();
         if (var3 == null) {
            throw new IllegalStateException("device may already be removed");
         } else {
            this.mConnection = var2.mUsbManager.openDevice(var3);
            if (this.mConnection == null) {
               throw new IllegalStateException("device may already be removed or have no permission");
            } else {
               this.mWeakMonitor = new WeakReference(var2);
               this.mWeakDevice = new WeakReference(var3);
               this.mBusNum = var1.mBusNum;
               this.mDevNum = var1.mDevNum;
            }
         }
      }

      public synchronized void close() {
         if (this.mConnection != null) {
            int var1 = this.mInterfaces.size();

            for(int var4 = 0; var4 < var1; ++var4) {
               int var2 = this.mInterfaces.keyAt(var4);
               UsbInterface var3 = (UsbInterface)this.mInterfaces.get(var2);
               this.mConnection.releaseInterface(var3);
            }

            this.mConnection.close();
            this.mConnection = null;
            USBMonitor var6 = (USBMonitor)this.mWeakMonitor.get();
            if (var6 != null) {
               if (var6.mOnDeviceConnectListener != null) {
                  UsbDevice var5 = (UsbDevice)this.mWeakDevice.get();
                  var6.mOnDeviceConnectListener.onDisconnect(var5, this);
               }

               var6.mCtrlBlocks.remove(this.getDevice());
            }
         }

      }

      public UsbControlBlock clone() throws CloneNotSupportedException {
         try {
            UsbControlBlock ctrlblock = new UsbControlBlock(this);
            return ctrlblock;
         } catch (IllegalStateException var2) {
            throw new CloneNotSupportedException(var2.getMessage());
         }
      }
   }
}
