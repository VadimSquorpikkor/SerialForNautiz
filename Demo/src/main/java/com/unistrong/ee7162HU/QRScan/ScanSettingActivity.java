package com.unistrong.ee7162HU.QRScan;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.handheldgroup.serialport.SerialPort;
import com.unistrong.ee7162HU.R;
import com.unistrong.player.Player;
import com.unistrong.qrcode.OnScanListener;
import com.unistrong.qrcode.QRScanManagerJNI;
import com.unistrong.qrcode.ScanBroadcastReceiver;
import com.unistrong.qrcode.USBQRscanFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.security.InvalidParameterException;
import java.util.Arrays;

import static com.unistrong.ee7162HU.QRScan.mbs.DEFAULT_MESSAGE_LENGTH;
import static com.unistrong.ee7162HU.QRScan.mbs.getCRCOrder;
import static com.unistrong.ee7162HU.QRScan.mbs.getMessageWithCRC16;

public class ScanSettingActivity extends Activity {
    private static final int HIDE_SERIAL_CTL_MSG = 1;
    private static final int DEVICE_POWER_STATE_CHANGE_MSG = 2;
    private static final int CLEAR_RESULT_MSG = 4;
    private static String TAG = "ACT_QRScan";
    USBQRscanFactory usbScan;
    private Button btnLightSetting,
            btnScan, btnContinuoutStart, btnPowerOnOff,
            btnSetTimeOut;
    private EditText etTimeOutSeconds;
    private TextView mShow;
    private TextView mTvstatus;
    private ScanBroadcastReceiver scanBroadcastReceiver;
    private boolean mWorkingStateFlag = false;
    private boolean mPowerStateFlag = true;
    private Context mCtx;
    private Player mPlayer;


    SerialPort mSerialPort;
    OutputStream mOutputStream;
    InputStream mInputStream;
    private ReadThread mReadThread;
    private BufferedReader mReader;
    private boolean mReadLine = true;

    Handler H = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case 0:
                    byte[] ret = (byte[]) msg.obj;
                    if (ret != null) {
                        mShow.setText("Result:" + new String(ret) + "\nHEX:" + BytesToHex(ret));
                        new Instrumentation().sendStringSync(new String(ret));
                        if (null != mPlayer) {
                            mPlayer.playSound();
                        }
                    }
                    break;
                case DEVICE_POWER_STATE_CHANGE_MSG:
                    boolean on = (boolean) msg.obj;
                    hideSerialCtls(on);
                    btnPowerOnOff.setText(on
                            ? getString(R.string.str_poweron)
                            : getString(R.string.str_poweroff));
                    mTvstatus.setText(on
                            ? getString(R.string.str_poweroff)
                            : getString(R.string.str_poweron));
                    break;
                case CLEAR_RESULT_MSG:
                    mShow.setText("");
                    break;
                case QRScanManagerJNI.QR_OPEN_SUCCESS:
                    mTvstatus.setText(getString(R.string.str_open_success));
                    mWorkingStateFlag = true;
                    break;
                case QRScanManagerJNI.QR_CLOSE_SUCCESS:
                    mTvstatus.setText(getString(R.string.str_close_success));
                    mWorkingStateFlag = false;
                    break;
                case QRScanManagerJNI.QR_OPEN_FAILED:
                    mTvstatus.setText(getString(R.string.str_open_failed));
                    mWorkingStateFlag = false;
                    break;
                case QRScanManagerJNI.QR_QUIT:
                    mTvstatus.setText(getString(R.string.str_exit));
                    break;
                case QRScanManagerJNI.QR_NOT_OPENED:
                    mTvstatus.setText(getString(R.string.str_not_opened));
                    break;
                case QRScanManagerJNI.QR_DEVICE_CLOSE:
                    mTvstatus.setText(getString(R.string.str_close));
                    break;
                case QRScanManagerJNI.QR_SET_TIMEOUT_OUTRANGE:
                    mTvstatus.setText(getString(R.string.str_timeout_outrange));
                    break;
                case QRScanManagerJNI.QR_SET_TIMEOUT_FAILED:
                    mTvstatus.setText(getString(R.string.str_timeout_failed));
                    break;
                case QRScanManagerJNI.QR_SET_TIMEOUT_SUCCESS:
                    mTvstatus.setText(getString(R.string.str_timeout_success));
                    break;
            }
        }
    };
    private int quit = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scan_setting_layout);
        mCtx = ScanSettingActivity.this;

        btnLightSetting = (Button) findViewById(R.id.btn_light_setting);
        btnPowerOnOff = (Button) findViewById(R.id.btn_poweron);
//		btnExit = (Button) findViewById(R.id.btn_exit);
        btnContinuoutStart = (Button) findViewById(R.id.btn_continuous_scan_start);

        mShow = (TextView) findViewById(R.id.tv_show_info);
        mTvstatus = (TextView) findViewById(R.id.tv_status);
        btnScan = (Button) findViewById(R.id.scan_button);
        btnLightSetting.setTag("0");
        btnContinuoutStart.setTag("0");

        etTimeOutSeconds = (EditText) findViewById(R.id.et_timeout);
        btnSetTimeOut = (Button) findViewById(R.id.btn_set_timeout);
        if (null == mPlayer)
            mPlayer = new Player();
        mPlayer.init(mCtx, R.raw.done);
        scanBroadcastReceiver = new ScanBroadcastReceiver(mPlayer, R.raw.done);

        hideSerialCtls(true);

        usbScan = USBQRscanFactory.createInstance();
        btnPowerOnOff.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                if (usbScan == null) usbScan = USBQRscanFactory.createInstance();

                if (mPowerStateFlag) {
                    usbScan.powerOn();
                    mPowerStateFlag = false;
                } else {
                    if (usbScan == null) usbScan = USBQRscanFactory.createInstance();
                    if (mWorkingStateFlag) {
                        usbScan.close();
                        H.sendEmptyMessage(CLEAR_RESULT_MSG);
                        mWorkingStateFlag = false;
                    }
                    usbScan.powerOff();
                    mPowerStateFlag = true;
                }
                H.sendMessage(H.obtainMessage(DEVICE_POWER_STATE_CHANGE_MSG, mPowerStateFlag));

            }
        });

        usbScan.init(new OnScanListener() {

            @Override
            public void scanReport(byte[] result) {
                // TODO Auto-generated method stub
                synchronized (H) {
                    if (null != result && result.length > 0) {
                        H.sendMessage(H.obtainMessage(0, result));
                    }
                }
            }

            @Override
            public void statusReport(int status) {
                // TODO Auto-generated method stub
                synchronized (H) {
                    H.sendEmptyMessage(status);
                }
            }
        });

        btnLightSetting.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                if (btnLightSetting.getTag().equals("0")) {
                    openScanner(true);
                    usbScan.lightSet(true);
                    btnLightSetting.setTag("1");
                    btnLightSetting.setText(getString(R.string.str_light_off));
                    openScanner(false);
                } else {
                    openScanner(true);
                    usbScan.lightSet(false);
                    btnLightSetting.setTag("0");
                    btnLightSetting.setText(getString(R.string.str_light_on));
                    openScanner(false);
                }
            }

        });

        btnScan.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                openScanner(true);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        usbScan.scan_start();
                    }
                }).start();
            }
        });

        btnContinuoutStart.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                if (btnContinuoutStart.getTag().equals("0")) {
                    openScanner(true);
                    usbScan.continuousScan(true);
                    btnContinuoutStart.setTag("1");
                    btnContinuoutStart.setText(getString(R.string.str_continuous_scan_stop));
                } else {
                    usbScan.continuousScan(false);
                    openScanner(false);
                    btnContinuoutStart.setTag("0");
                    btnContinuoutStart.setText(getString(R.string.str_continuous_scan_start));
                }


            }
        });

        btnSetTimeOut.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                if (null != etTimeOutSeconds && "".equals(etTimeOutSeconds.getText())) {
                    return;
                } else {
                    Log.d(TAG, etTimeOutSeconds.getText().toString());
                    String strnum = etTimeOutSeconds.getText().toString();
                    if (null == strnum || "".equals(strnum)) return;
                    int milliseconds = new Integer(strnum);
                    if (0 >= milliseconds || 30000 < milliseconds) {
                        Toast.makeText(getApplicationContext(),
                                getString(R.string.input_out_range_tips), Toast.LENGTH_SHORT).show();
                    } else {
                        openScanner(true);
                        usbScan.timeOutSet(milliseconds);
                        openScanner(false);
                    }
                }
            }
        });

        findViewById(R.id.open_console).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                openConsole();
//                method();
            }
        });

        findViewById(R.id.comm_1).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                method();
            }
        });

        findViewById(R.id.comm_2).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                method2();
            }
        });



        requestPermission();
    }

    private static final int PERMISSION_WRITE_MEMORY = 10;
    private static final int PERMISSION_READ_MEMORY = 11;

    private void requestPermission_() {
        if (Build.VERSION.SDK_INT >= 23) {
            int REQUEST_CODE_CONTACT = 101;
            String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
            // Verify permissions
            for (String str : permissions) {
                if (this.checkSelfPermission(str) != PackageManager.PERMISSION_GRANTED) {
                    //request for access
                    this.requestPermissions(permissions, REQUEST_CODE_CONTACT);
                    return;
                } else initSerialPort();
            }
        }
    }

    private void requestPermission() {
        Log.e(TAG, "♠♠♠♠♠requestPermissions♠♠♠♠♠");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //permission for writing data into external memory
            switch (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                case PackageManager.PERMISSION_DENIED:
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            PERMISSION_WRITE_MEMORY);
                    break;
                case PackageManager.PERMISSION_GRANTED:
                    initSerialPort();
                    isGranted++;
                    Log.e(TAG, "PERMISSION_WRITE_MEMORY GRANTED");
                    break;
            }

            switch (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                case PackageManager.PERMISSION_DENIED:
                    requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            PERMISSION_READ_MEMORY);
                    break;
                case PackageManager.PERMISSION_GRANTED:
                    initSerialPort();
                    isGranted++;
                    Log.e(TAG, "PERMISSION_READ_MEMORY GRANTED");
                    break;
            }

        } else {
            initSerialPort();
        }
    }

    int isGranted = 0;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_WRITE_MEMORY) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                isGranted++;
//                initSerialPort();
            }
        }
        if (isGranted==2)initSerialPort();

    }

    public static final byte READ_DEVICE_ID = 0x11;
    public static final byte READ_STATE_DATA_REGISTERS = 0x07;
    public static final byte ADDRESS = 0x02;

    public static ModbusMessage createModbusMessage(int messageLength, byte address, byte command,
                                                    byte[] data, boolean crcOrder) {
        ByteBuffer buffer = ByteBuffer.allocate(messageLength)
                .put(address)
                .put(command);
        if (data != null) {
            buffer.put(data);
        }

        ModbusMessage message = new ModbusMessage(getMessageWithCRC16(buffer.array(), crcOrder));
        message.mIntegrity = true;
        message.mException = false;
        return message;
    }

    void method() {
        ModbusMessage response = sendMessageWithResponse(createModbusMessage(DEFAULT_MESSAGE_LENGTH, ADDRESS, READ_DEVICE_ID,
                null, getCRCOrder()));
        if (response!=null&&response.getBuffer()!=null&&response.getBuffer().length!=0)Log.e(TAG, "--method: "+response.getBuffer()[0]);
        if (response!=null&&response.getBuffer()!=null&&response.getBuffer().length!=0)Log.e(TAG, "--method: "+response.getBuffer()[1]);
    }

    void method2() {
        ModbusMessage response = sendMessageWithResponse(createModbusMessage(DEFAULT_MESSAGE_LENGTH, ADDRESS, READ_STATE_DATA_REGISTERS,
                null, getCRCOrder()));
    }

    public synchronized ModbusMessage sendMessageWithResponse(ModbusMessage message) {
        //requestMessage = message;
        sendMessage(message);
        return receiveMessage();
    }

    public void sendMessage(ModbusMessage message){

        Log.e(TAG, "♦ ----------------------------");
        Log.e(TAG, "♦ sendMessage адрес: " + message.getBuffer()[0]);
        Log.e(TAG, "♦ sendMessage команда: " + message.getBuffer()[1]);

        try {
            sendMessage2(message.getBuffer());
        } catch (IOException e) {
            e.printStackTrace();
        }

//        try {
//            mSerialPort.getOutputStream().write(message.getBuffer());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        ////receiveMessage();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    void sendMessage2(byte[] message) throws IOException {
        ////////////////mInputStream.skip(mInputStream.available());
        mSerialPort.getOutputStream().write(message);
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        mWorkingStateFlag = false;
        if (usbScan != null) {
            usbScan.close();
            usbScan.powerOff();
            usbScan = null;
        }
    }


    private Intent getUSBScanService() {
        // TODO Auto-generated method stub
        Intent i = new Intent();
        i.setAction("com.unistrong.qrcode.USBScanService");
        i.setPackage(getApplication().getPackageName());
        return i;
    }

    public String BytesToHex(byte[] data) {
        if (null == data) return null;
        int N = data.length;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < N; i++) {
            if (data[i] != 0x0) sb.append(String.format("%02x ", data[i]));
        }
        return sb.toString();
    }

    private void hideSerialCtls(boolean hide) {
        btnLightSetting.setVisibility(hide ? View.GONE : View.VISIBLE);
        btnScan.setVisibility(hide ? View.GONE : View.VISIBLE);
        btnContinuoutStart.setVisibility(hide ? View.GONE : View.VISIBLE);
        etTimeOutSeconds.setVisibility(hide ? View.GONE : View.VISIBLE);
        btnSetTimeOut.setVisibility(hide ? View.GONE : View.VISIBLE);
    }

    private void openScanner(boolean open) {
        if (open == mWorkingStateFlag) return;
        if (open) {
            try {
                Thread.sleep(50);
                usbScan.open();
                usbScan.enableAddKeyValue(0);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {
            if (null != usbScan) {
                if (btnContinuoutStart.getTag().equals("1")) {
                    usbScan.continuousScan(false);
                    btnContinuoutStart.setTag("0");
                    btnContinuoutStart.setText(getString(R.string.str_continuous_scan_start));
                }
                usbScan.close();
            }
            H.sendEmptyMessage(CLEAR_RESULT_MSG);
        }
    }


    void openConsole() {
        startActivity(new Intent(this, ConsoleActivity.class));
    }


    @Override
    public void onResume() {
        super.onResume();
//        initSerialPort();
    }

    private static String getStringFromInputStream(InputStream is) {

        BufferedReader bufferedReader = null;
        StringBuilder stringBuilder = new StringBuilder();

        String line;
        try {
            bufferedReader = new BufferedReader(new InputStreamReader(is));
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return stringBuilder.toString();
    }

    @Override
    public void onPause() {
        //////////////closeSerialPort();
        super.onPause();
    }

    //9600 19200 38400 57600 115200 230400 460800 921600
    void initSerialPort() {
        Log.e(TAG, "♦ ----------------------------------------------");
        mReadLine = true;
        try {
//            mSerialPort = ((PortApplication) getApplication()).getSerialPort();
            mSerialPort = new SerialPort(new File("/dev/ttyHSL1"), 230400, 0);

            mOutputStream = mSerialPort.getOutputStream();
            mInputStream = mSerialPort.getInputStream();


            mReader = new BufferedReader(new InputStreamReader(mSerialPort.getInputStream()));
            mReadThread = new ReadThread();
            mReadThread.start();
        } catch (SecurityException e) {
            //DisplayError(R.string.error_security);
        } catch (IOException e) {
            //DisplayError(R.string.error_unknown);
        } catch (InvalidParameterException e) {
            //DisplayError(R.string.error_configuration);
        }
    }

    void closeSerialPort() {
        if (mReadThread != null) {
            mReadThread.interrupt();
            mReadThread = null;
        }
        try {
            if (mReader != null) {
                mReader.close();
                mReader = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        ((PortApplication) getApplication()).closeSerialPort();
        mSerialPort = null;
    }

    private class ReadThread__ extends Thread {
        @Override
        public void run() {
            super.run();
            String line = "";
            try {
                line = line+mReader.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.e(TAG, ""+line);
            String s = "";
            while(s!=null) {
                //Log.e(TAG, "run: " );
                try {
                    s = mReader.readLine();
                    line+=s;
                    Log.e(TAG, ""+line);
                } catch (IOException e) {
                    Log.e(TAG, "run: E= "+e);
                    e.printStackTrace();
                }

            }
        }
    }

    private class ReadThread___ extends Thread {
        @Override
        public void run() {
            super.run();
            StringBuffer buffer = new StringBuffer();
            buffer.setLength(0);
            while(!isInterrupted()) {
                try {
                    if (mSerialPort == null) return;
                    //Log.e(TAG, "run: " );
                    if (mReader.ready()) {
                        buffer.append((char) mReader.read());
                        Log.e(TAG, "♦ "+buffer);
                        buffer.setLength(0);
                    } else {
                        if (mReadLine) {
                            int carriage = buffer.indexOf("\n");
                            if (carriage != -1) {
//                                onDataReceived(buffer.substring(0, carriage));
                                buffer.delete(0, carriage + 1);
                            }
                        } else {
                            if (buffer.length() > 0) {
//                                onDataReceived(buffer.toString());
                                buffer.setLength(0);
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
            }
        }
    }

//    D/debugTag: Request [1, 17, 44, -64]
//    Response [1, 17, 18, 48, -4, 1, 34, 0, 0, 4, 0, 12, 0, 12, 6, 0, 0, 0, 0, 0, 0, -32, -13]


    //2 17 18 97 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 34 53

    private class ReadThread extends Thread {
        @Override
        public void run() {
            super.run();
            StringBuffer buffer1 = new StringBuffer();
            buffer1.setLength(0);

            int currentPosition = 0;

            while(!isInterrupted()) {
//            while(mInputStream.read()!=0) {
                try {
                    if (mSerialPort == null) return;
                    //////////buffer1.append((char) mReader.read());

                    int x = mInputStream.read();
                    buffer[currentPosition] = (byte) x;
                    Log.e(TAG, "run: "+buffer[currentPosition]);
                    currentPosition++;
//                    Log.e(TAG, "Response " + Arrays.toString(buffer));


//                    receiveMessage();

//                   Log.e(TAG, "mInputStream.available(): "+mInputStream.available());

                    //responseMessage.setIntegrity(CRC16.checkCRC(responseMessage.getBuffer()));

                    ////////////Log.e(TAG, "♦ "+buffer1);
                    ////////////buffer1.setLength(0);
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
            }

            Log.e(TAG, "Response " + Arrays.toString(buffer));
        }
    }



    ModbusMessage receiveMessage() {
        ModbusMessage responseMessage = new ModbusMessage(receiveByteMessage());
        if (responseMessage.getBuffer()!=null)Log.e(TAG, "Response " + Arrays.toString(responseMessage.getBuffer()));
        return responseMessage;
    }


    private final byte[] buffer = new byte[3084];;

    private static final int MESSAGE_DEFAULT_LENGTH = 5;
    private static final int TIMEOUT_DEFAULT = 1500;
    public static final byte DIAGNOSTICS = 0x08;
    public static final byte SEND_CONTROL_SIGNAL = 0x05;
    public static final byte CHANGE_STATE_CONTROL_REGISTER = 0x06;
    public static final byte CHANGE_STATE_CONTROL_REGISTERS = 0x10;
    private static final int MESSAGE_LONG_LENGTH = 8;
    public static final byte READ_STATUS_WORD = 0x07;
    public static final byte READ_ACCUMULATED_SPECTRUM = 0x40;
    public static final byte READ_ACCUMULATED_SPECTRUM_COMPRESSED_REBOOT = 0x41;
    public static final byte READ_ACCUMULATED_SPECTRUM_COMPRESSED = 0x42;
    private static final int MESSAGE_MID_LENGTH = 6;

    public byte[] receiveByteMessage() {
        Log.e(TAG, "receiveByteMessage: ");
        byte[] buffer = this.buffer;
        int currentPosition = 0;
        long startTime = System.currentTimeMillis();
        int totalByte = MESSAGE_DEFAULT_LENGTH;

        //until time passes or we get all the bytes
        while ((System.currentTimeMillis() - startTime < TIMEOUT_DEFAULT) && currentPosition != totalByte) {
            Log.e(TAG, "startTime < TIMEOUT_DEFAULT: "+(System.currentTimeMillis() - startTime < TIMEOUT_DEFAULT));
            Log.e(TAG, "!= totalByte: " + (currentPosition != totalByte));
            try {
                    Log.e(TAG, "♦1♦");
                    //BufferedReader reader = new BufferedReader(new InputStreamReader(mSerialPort.getInputStream()));
                    //Log.e(TAG, "receiveByteMessage: "+reader.readLine());
                //Log.e(TAG, "mInputStream.available(): "+mInputStream.available());
                if (mInputStream.available() > 0) {
                    Log.e(TAG, "♦2♦");
                    int x = mInputStream.read();
                    buffer[currentPosition] = (byte) x;
                    currentPosition++;
                    //position 1 contains contains command byte
                    //We can figure out the number of bytes in message according to command byte
                    if (currentPosition == 3) {
                        if (buffer[1] == DIAGNOSTICS
                                || buffer[1] == SEND_CONTROL_SIGNAL
                                || buffer[1] == CHANGE_STATE_CONTROL_REGISTER
                                || buffer[1] == CHANGE_STATE_CONTROL_REGISTERS) {
                            totalByte = MESSAGE_LONG_LENGTH;
                        } else if (buffer[1] == READ_STATUS_WORD) {
                            totalByte = MESSAGE_DEFAULT_LENGTH;
                        } else {
                            totalByte = (buffer[2] & 255) + MESSAGE_DEFAULT_LENGTH;
                        }
                    } else if (currentPosition == 4 &&
                            (buffer[1] == READ_ACCUMULATED_SPECTRUM
                                    || buffer[1] == READ_ACCUMULATED_SPECTRUM_COMPRESSED_REBOOT
                                    || buffer[1] == READ_ACCUMULATED_SPECTRUM_COMPRESSED)) {
                        //in these command 3 and 4 bytes shows number of data bytes in message
                        int lengthData = BitConverter.toInt16(new byte[]{buffer[3], buffer[2]}, 0);
                        totalByte = lengthData + MESSAGE_MID_LENGTH;
                    }
                }
            } catch (Exception e) {
                return null;
            }
        }
        return Arrays.copyOf(buffer, currentPosition);
    }



}

