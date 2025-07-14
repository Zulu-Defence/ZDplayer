package com.zdplayer.fpvplayer;

import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import java.io.File;
import android.net.Uri;
import android.widget.Button;
import android.widget.Toast;
import android.os.Environment;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.zdplayer.fpvplayer.android.usbserial.DeviceFilter;
import com.zdplayer.fpvplayer.android.usbserial.USBMonitor;
import com.zdplayer.fpvplayer.fpvlibrary.enums.PTZAction;
import com.zdplayer.fpvplayer.fpvlibrary.usbserial.UsbSerialConnection;
import com.zdplayer.fpvplayer.fpvlibrary.usbserial.UsbSerialControl;
import com.zdplayer.fpvplayer.fpvlibrary.utils.BusinessUtils;
import com.zdplayer.fpvplayer.fpvlibrary.video.FPVVideoClient;
import com.zdplayer.fpvplayer.fpvlibrary.widget.GLHttpVideoSurface;

import java.util.List;

/**
 * usb串口连接方式
 */
public class UsbSerialActivity extends AppCompatActivity {
    private Context mContext;

    //Usb监视器
    private USBMonitor mUSBMonitor;

    //Usb设备
    private UsbDevice mUsbDevice;

    private GLHttpVideoSurface mPreviewDualVideoView;

    //视频渲染
    private FPVVideoClient mFPVVideoClient;

    //usb连接实例
    private UsbSerialConnection mUsbSerialConnection;

    //摄像头控制
    private UsbSerialControl mUsbSerialControl;

    private Handler mainHanlder = new Handler(Looper.getMainLooper());
    private boolean isRecording = false;
    private View recordingIndicator;
    private Handler blinkHandler = new Handler(Looper.getMainLooper());
    private Runnable blinkRunnable;


    //private Handler mainHanlder = new Handler(Looper.getMainLooper());

    private void scanFile(File file) {
        Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        scanIntent.setData(Uri.fromFile(file));
        sendBroadcast(scanIntent);
    }
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usbserial);
        this.mContext = this;
        initView();
        init();
    }

    public static void start(Context context){
        context.startActivity(new Intent(context,UsbSerialActivity.class));
    }

    /*private void initView(){
        mPreviewDualVideoView = findViewById(R.id.fPVVideoView);
        mPreviewDualVideoView.init();
        findViewById(R.id.btnTest).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mUsbSerialControl.AkeyControl(PTZAction.DOWN);
            }
        });
    }*/
    private void initView() {
        recordingIndicator = findViewById(R.id.recording_indicator);

        mPreviewDualVideoView = findViewById(R.id.fPVVideoView);
        mPreviewDualVideoView.init();
        recordingIndicator.setVisibility(View.GONE);


        Button btnToggleRecord = findViewById(R.id.btn_toggle_record);
        Button btnSnap = findViewById(R.id.btn_snapshot);

        btnToggleRecord.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                if (!isRecording) {
                    startRecording();
                    btnToggleRecord.setText("Stop Recording");
                    isRecording = true;
                } else {
                    stopRecording();
                    btnToggleRecord.setText("Start Recording");
                    isRecording = false;
                }
            }
        });

        btnSnap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takeSnapshot();
            }
        });
    }



    private void startRecording() {
        String timestamp = new SimpleDateFormat("yy-MM-dd-HH-mm-ss", Locale.getDefault()).format(new Date());
        String fileName = "video-" + timestamp + ".mp4";

        // Correctly build the path: Downloads/ZDPlayer/video
        File baseDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File folder = new File(baseDir, "ZDPlayer/video/");

        if (!folder.exists()) {
            folder.mkdirs();  // Ensure directories exist
        }

        //boolean started = mFPVVideoClient.startRecord(folder.getAbsolutePath(), fileName);
        String folderPath = folder.getAbsolutePath();
        if (!folderPath.endsWith("/")) {
            folderPath += "/";
        }
        boolean started = mFPVVideoClient.startRecord(folderPath, fileName);


        if (started) {
            //recordingIndicator.setVisibility(View.VISIBLE);
            Toast.makeText(this, "Recording started:\n" + fileName, Toast.LENGTH_SHORT).show();
            scanFile(new File(folder, fileName));
            startBlinking();
        } else {
            Toast.makeText(this, "Failed to start recording.", Toast.LENGTH_SHORT).show();
        }
    }



    private void stopRecording() {
        if (mFPVVideoClient != null && mFPVVideoClient.isRecording()) {
            mFPVVideoClient.stopRecord();
            //recordingIndicator.setVisibility(View.GONE);
            Toast.makeText(this, "Recording stopped", Toast.LENGTH_SHORT).show();
            stopBlinking();
        } else {
            Toast.makeText(this, "Not recording or client is null", Toast.LENGTH_SHORT).show();
        }
    }
    private void startBlinking() {
        recordingIndicator.setVisibility(View.VISIBLE);
        blinkRunnable = new Runnable() {
            private boolean visible = true;

            @Override
            public void run() {
                visible = !visible;
                recordingIndicator.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
                blinkHandler.postDelayed(this, 500); // 500 ms toggle
            }
        };
        blinkHandler.post(blinkRunnable);
    }

    private void stopBlinking() {
        blinkHandler.removeCallbacks(blinkRunnable);
        recordingIndicator.setVisibility(View.GONE);
    }



    private void takeSnapshot() {
        String timestamp = new SimpleDateFormat("yy-MM-dd-HH-mm-ss", Locale.getDefault()).format(new Date());
        String fileName = "snap-" + timestamp + ".jpg";

        // Base directory: DCIM/ZDPlayer/snap
        File baseDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File folder = new File(baseDir, "ZDPlayer/snap/");

        if (!folder.exists()) {
            folder.mkdirs();
        }

        String folderPath = folder.getAbsolutePath();
        if (!folderPath.endsWith("/")) {
            folderPath += "/";
        }

        mFPVVideoClient.captureSnapshot(folderPath, fileName);
        Toast.makeText(this, "Snapshot saved:\n" + fileName, Toast.LENGTH_SHORT).show();

        // Refresh gallery
        scanFile(new File(folder, fileName));
    }



    private void init(){
        //初始化usb连接
        mUsbSerialConnection = new UsbSerialConnection(mContext);
        mUsbSerialConnection.setDelegate(new UsbSerialConnection.Delegate() {
            @Override
            public void onH264Received(byte[] bytes, int paySize) {
                //视频数据
                if(mFPVVideoClient != null){
                    mFPVVideoClient.received(bytes,4,paySize);
                }
            }

            @Override
            public void onGPSReceived(byte[] bytes) {
                //GPS数据
            }

            @Override
            public void onDataReceived(byte[] bytes) {
                //数传数据
            }

            @Override
            public void onDebugReceived(byte[] bytes) {
                //遥控器数据
            }
        });

        //渲染视频相关
        mFPVVideoClient = new FPVVideoClient();
        mFPVVideoClient.setDelegate(new FPVVideoClient.Delegate() {
            @Override
            public void onStopRecordListener(String fileName) {
                //停止录像回调
            }

            @Override
            public void onSnapshotListener(String fileName) {
                //拍照回调
            }

            //视频相关
            @Override
            public void renderI420(byte[] frame, int width, int height) {
                mPreviewDualVideoView.renderI420(frame,width,height);
            }

            @Override
            public void setVideoSize(int picWidth, int picHeight) {
                mPreviewDualVideoView.setVideoSize(picWidth,picHeight,mainHanlder);
            }

            @Override
            public void resetView() {
                mPreviewDualVideoView.resetView(mainHanlder);
            }
        });

        //FPV控制
        mUsbSerialControl = new UsbSerialControl(mUsbSerialConnection);

        mUSBMonitor = new USBMonitor(mContext,mOnDeviceConnectListener);
        List<DeviceFilter> deviceFilters = DeviceFilter.getDeviceFilters(mContext, R.xml.device_filter);
        mUSBMonitor.setDeviceFilter(deviceFilters);
        mUSBMonitor.register();
    }

    //使用 USBMonitor 处理USB连接回调
    private USBMonitor.OnDeviceConnectListener mOnDeviceConnectListener = new USBMonitor.OnDeviceConnectListener() {
        // USB device attach
        // USB设备插入
        @Override
        public void onAttach(final UsbDevice device) {
            if(deviceHasConnected(device) || mUsbDevice != null){
                return;
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if(device == null){
                            List<UsbDevice> devices = mUSBMonitor.getDeviceList();
                            if(devices.size() == 1){
                                mUSBMonitor.requestPermission(devices.get(0));
                            }
                        }else {
                            mUSBMonitor.requestPermission(device);
                        }

                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            });
        }

        // USB device detach
        // USB设备物理断开
        @Override
        public void onDettach(UsbDevice device) {
            if (!BusinessUtils.deviceIsUartVideoDevice(device)) {
                return;
            }
            if (!deviceHasConnected(device)) {
                return;
            }
            disconnected();
        }

        // USB device has obtained permission
        // USB设备获得权限
        @Override
        public void onConnect(UsbDevice device, USBMonitor.UsbControlBlock var2, boolean var3) {
            if (!BusinessUtils.deviceIsUartVideoDevice(device)) {
                return;
            }
            if (deviceHasConnected(device)) {
                return;
            }

            synchronized (this){
                if (BusinessUtils.deviceIsUartVideoDevice(device)) {
                    try {
                        //打开串口
                        mUsbSerialConnection.openConnection(device);
                        mUsbDevice = device;
                        //开始渲染视频
                        mFPVVideoClient.startPlayback();
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
        }

        // USB device disconnected
        // USB设备关闭连接
        @Override
        public void onDisconnect(UsbDevice device, USBMonitor.UsbControlBlock var2) {
            if (!BusinessUtils.deviceIsUartVideoDevice(device)) {
                return;
            }
            if (!deviceHasConnected(device)) {
                return;
            }
            disconnected();
        }

        // USB device obtained permission failed
        // USB设备权限获取失败
        @Override
        public void onCancel() {

        }
    };

    //关闭连接
    private void disconnected(){
        if(mUsbSerialConnection != null){
            try {
                mUsbSerialConnection.closeConnection();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if(mFPVVideoClient != null){
            mFPVVideoClient.stopPlayback();
        }

        mUsbDevice = null;
    }

    private boolean deviceHasConnected(UsbDevice usbDevice){
        return usbDevice != null && usbDevice == mUsbDevice;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disconnected();
        if(mUSBMonitor != null){
            mUSBMonitor.unregister();
            mUSBMonitor.destroy();
            mUSBMonitor = null;
        }
    }
}
