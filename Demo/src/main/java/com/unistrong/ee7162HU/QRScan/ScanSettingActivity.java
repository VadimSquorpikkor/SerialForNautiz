package com.unistrong.ee7162HU.QRScan;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.unistrong.ee7162HU.R;
import com.unistrong.player.Player;
import com.unistrong.qrcode.OnScanListener;
import com.unistrong.qrcode.QRScanManagerJNI;
import com.unistrong.qrcode.ScanBroadcastReceiver;
import com.unistrong.qrcode.USBQRscanFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.InvalidParameterException;

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

}

